/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.visualization.bvv;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.joml.Matrix4f;

import bdv.viewer.animate.TranslationAnimator;
import bvv.core.VolumeViewerPanel;
import bvv.core.util.MatrixMath;
import bvv.vistools.BvvHandle;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import ij.ImagePlus;
import net.imglib2.RealLocalizable;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.Type;

public class TrackMateBVV< T extends Type< T > > extends AbstractTrackMateModelView
{

	private static final String KEY = "BIGVOLUMEVIEWER";

	private final ImagePlus imp;

	private BvvHandle handle;

	private final Map< Spot, StupidMesh > meshMap;

	public TrackMateBVV( final Model model, final SelectionModel selectionModel, final ImagePlus imp, final DisplaySettings displaySettings )
	{
		super( model, selectionModel, displaySettings );
		this.imp = imp;
		this.meshMap = new HashMap<>();
		final Iterable< Spot > it = model.getSpots().iterable( true );
		it.forEach( s -> meshMap.computeIfAbsent( s, BVVUtils::createMesh ) );
		updateColor();
		displaySettings.listeners().add( this::updateColor );
		selectionModel.addSelectionChangeListener( e -> refresh() );
	}

	/**
	 * Returns the {@link BvvHandle} that contains this view. Returns
	 * <code>null</code> if this view has not been rendered yet.
	 * 
	 * @return the BVV handle, or <code>null</code>.
	 */
	public BvvHandle getBvvHandle()
	{
		return handle;
	}

	@Override
	public void render()
	{
		this.handle = BVVUtils.createViewer( imp );
		final VolumeViewerPanel viewer = handle.getViewerPanel();
		viewer.setRenderScene( ( gl, data ) -> {
			if ( displaySettings.isSpotVisible() )
			{
				final Matrix4f pvm = new Matrix4f( data.getPv() );
				final Matrix4f view = MatrixMath.affine( data.getRenderTransformWorldToScreen(), new Matrix4f() );
				final Matrix4f vm = MatrixMath.screen( data.getDCam(), data.getScreenWidth(), data.getScreenHeight(), new Matrix4f() ).mul( view );

				final int t = data.getTimepoint();
				final Iterable< Spot > it = model.getSpots().iterable( t, true );
				it.forEach( s -> meshMap.computeIfAbsent( s, BVVUtils::createMesh ).draw( gl, pvm, vm, selectionModel.getSpotSelection().contains( s ) ) );
			}
		} );
	}

	@Override
	public void refresh()
	{
		if ( handle != null )
			handle.getViewerPanel().requestRepaint();
	}

	@Override
	public void clear()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void centerViewOn( final Spot spot )
	{
		if ( handle == null )
			return;

		final VolumeViewerPanel panel = handle.getViewerPanel();
		panel.setTimepoint( spot.getFeature( Spot.FRAME ).intValue() );

		final AffineTransform3D c = panel.state().getViewerTransform();
		final double[] translation = getTranslation( c, spot, panel.getWidth(), panel.getHeight() );
		if ( translation != null )
		{
			final TranslationAnimator animator = new TranslationAnimator( c, translation, 300 );
			animator.setTime( System.currentTimeMillis() );
			panel.setTransformAnimator( animator );
		}
	}

	/**
	 * Returns a translation vector that will put the specified position at the
	 * center of the panel when used with a TranslationAnimator.
	 * 
	 * @param t
	 *            the viewer panel current view transform.
	 * @param target
	 *            the position to focus on.
	 * @param width
	 *            the width of the panel.
	 * @param height
	 *            the height of the panel.
	 * @return a new <code>double[]</code> array with 3 elements containing the
	 *         translation to use.
	 */
	private static final double[] getTranslation( final AffineTransform3D t, final RealLocalizable target, final int width, final int height )
	{
		final double[] pos = new double[ 3 ];
		final double[] vPos = new double[ 3 ];
		target.localize( pos );
		t.apply( pos, vPos );

		final double dx = width / 2 - vPos[ 0 ] + t.get( 0, 3 );
		final double dy = height / 2 - vPos[ 1 ] + t.get( 1, 3 );
		final double dz = -vPos[ 2 ] + t.get( 2, 3 );

		return new double[] { dx, dy, dz };
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public void modelChanged( final ModelChangeEvent event )
	{
		switch ( event.getEventID() )
		{
		case ModelChangeEvent.SPOTS_FILTERED:
		case ModelChangeEvent.SPOTS_COMPUTED:
		case ModelChangeEvent.TRACKS_VISIBILITY_CHANGED:
		case ModelChangeEvent.TRACKS_COMPUTED:
			refresh();
			break;
		case ModelChangeEvent.MODEL_MODIFIED:
		{
			for ( final Spot spot : event.getSpots() )
			{
				final StupidMesh mesh = BVVUtils.createMesh( spot );
				meshMap.put( spot, mesh );
			}
			updateColor();
			refresh();
			break;
		}
		}
	}

	private void updateColor()
	{
		final FeatureColorGenerator< Spot > spotColorGenerator = FeatureUtils.createSpotColorGenerator( model, displaySettings );
		for ( final Entry< Spot, StupidMesh > entry : meshMap.entrySet() )
		{
			final StupidMesh sm = entry.getValue();
			if ( sm == null )
				continue;

			final Color color = spotColorGenerator.color( entry.getKey() );
			final float alpha = ( float ) displaySettings.getSpotTransparencyAlpha();
			sm.setColor( color, alpha );
			sm.setSelectionColor( displaySettings.getHighlightColor(), alpha );
		}
		refresh();
	}
}
