/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2025 TrackMate developers.
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
package fiji.plugin.trackmate.gui.editor.labkit.model;

import bdv.tools.InitializeViewerState;
import bdv.util.Affine3DHelpers;
import bdv.viewer.ViewerPanel;
import bdv.viewer.ViewerState;
import bdv.viewer.animate.SimilarityTransformAnimator;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;
import sc.fiji.labkit.pixel_classification.RevampUtils;
import sc.fiji.labkit.ui.models.TransformationModel;

public class TMTransformationModel extends TransformationModel
{

	private ViewerPanel viewerPanel;

	private final boolean isTimeSeries;

	public TMTransformationModel( final boolean isTimeSeries )
	{
		super( isTimeSeries );
		this.isTimeSeries = isTimeSeries;
	}

	@Override
	public void initialize( final ViewerPanel viewerPanel )
	{
		this.viewerPanel = viewerPanel;
	}

	@Override
	public void transformToShowInterval( Interval interval, final AffineTransform3D sourceTransform )
	{
		if ( viewerPanel == null )
			return;
		if ( isTimeSeries )
		{
			final int lastDim = interval.numDimensions() - 1;
			final long meanTimePoint = ( interval.min( lastDim ) + interval.max( lastDim ) ) / 2;
			if ( viewerPanel != null )
				viewerPanel.setTimepoint( ( int ) meanTimePoint );
			interval = RevampUtils.removeLastDimension( interval );
		}

		final int width = viewerPanel.getWidth();
		final int height = viewerPanel.getHeight();
		final double cX = width / 2.;
		final double cY = height / 2.;

		final AffineTransform3D c = calculateCurrentTransform( viewerPanel.state(), cX, cY );
		final AffineTransform3D t = calculateTargetTransform( interval, sourceTransform, cX, cY );
		viewerPanel.setTransformAnimator( new SimilarityTransformAnimator( c, t, cX, cY, 300 ) );
	}

	public void resetView()
	{
		final int width = viewerPanel.getWidth();
		final int height = viewerPanel.getHeight();
		final double cX = width / 2.;
		final double cY = height / 2.;

		// Source
		final AffineTransform3D c = calculateCurrentTransform( viewerPanel.state(), cX, cY );
		final AffineTransform3D t = InitializeViewerState.initTransform( width, height, false, viewerPanel.state() );
		t.set( t.get( 0, 3 ) - cX, 0, 3 );
		t.set( t.get( 1, 3 ) - cY, 1, 3 );

		// Run
		viewerPanel.setTransformAnimator( new SimilarityTransformAnimator( c, t, cX, cY, 300 ) ); // FIXME
	}

	private static AffineTransform3D calculateCurrentTransform( final ViewerState state, final double cX, final double cY )
	{
		final AffineTransform3D c = new AffineTransform3D();
		state.getViewerTransform( c );
		c.set( c.get( 0, 3 ) - cX, 0, 3 );
		c.set( c.get( 1, 3 ) - cY, 1, 3 );
		return c;
	}

	private static AffineTransform3D calculateTargetTransform(
			final Interval interval,
			final AffineTransform3D sourceTransform,
			final double cX, final double cY )
	{
		final double sX0 = interval.min( 0 );
		final double sX1 = interval.max( 0 );
		final double sY0 = interval.min( 1 );
		final double sY1 = interval.max( 1 );
		final double sX = ( sX0 + sX1 ) / 2;
		final double sY = ( sY0 + sY1 ) / 2;
		final double sZ = 0; // 2D only

		final double[][] m = new double[ 3 ][ 4 ];

		// rotation
		final double[] qSource = new double[ 4 ];
		final double[] qViewer = new double[ 4 ];
		Affine3DHelpers.extractApproximateRotationAffine( sourceTransform, qSource, 2 );
		LinAlgHelpers.quaternionInvert( qSource, qViewer );
		LinAlgHelpers.quaternionToR( qViewer, m );

		// translation
		final double[] centerSource = new double[] { sX, sY, sZ };
		final double[] centerGlobal = new double[ 3 ];
		final double[] translation = new double[ 3 ];
		sourceTransform.apply( centerSource, centerGlobal );
		LinAlgHelpers.quaternionApply( qViewer, centerGlobal, translation );
		LinAlgHelpers.scale( translation, -1, translation );
		LinAlgHelpers.setCol( 3, translation, m );

		final AffineTransform3D t = new AffineTransform3D();
		t.set( m );

		// scale
		final double[] pSource = new double[] { sX1 + 0.5, sY1 + 0.5, sZ };
		final double[] pGlobal = new double[ 3 ];
		final double[] pScreen = new double[ 3 ];
		sourceTransform.apply( pSource, pGlobal );
		t.apply( pGlobal, pScreen );
		final double scaleX = cX / pScreen[ 0 ];
		final double scaleY = cY / pScreen[ 1 ];
		final double scale;
		final boolean zoomedIn = true;
		if ( zoomedIn )
			scale = Math.max( scaleX, scaleY );
		else
			scale = Math.min( scaleX, scaleY );
		t.scale( scale );

		// window center offset
		t.set( t.get( 0, 3 ) + cX - 0.5, 0, 3 );
		t.set( t.get( 1, 3 ) + cY - 0.5, 1, 3 );

		t.set( t.get( 0, 3 ) - cX, 0, 3 );
		t.set( t.get( 1, 3 ) - cY, 1, 3 );
		return t;
	}
}
