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
package fiji.plugin.trackmate.visualization.hyperstack;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.SelectionChangeEvent;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.ViewUtils;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;

public class HyperStackDisplayer extends AbstractTrackMateModelView
{

	protected final ImagePlus imp;

	protected SpotOverlay spotOverlay;

	protected TrackOverlay trackOverlay;

	private SpotEditTool editTool;

	public static final String KEY = "HYPERSTACKDISPLAYER";

	/*
	 * CONSTRUCTORS
	 */

	public HyperStackDisplayer( final Model model, final SelectionModel selectionModel, final ImagePlus imp, final DisplaySettings displaySettings )
	{
		super( model, selectionModel, displaySettings );
		if ( null != imp )
			this.imp = imp;
		else
			this.imp = ViewUtils.makeEmpytImagePlus( model );

		this.spotOverlay = createSpotOverlay( displaySettings );
		this.trackOverlay = createTrackOverlay( displaySettings );
		displaySettings.listeners().add( () -> refresh() );
	}

	public HyperStackDisplayer( final Model model, final SelectionModel selectionModel, final DisplaySettings displaySettings )
	{
		this( model, selectionModel, null, displaySettings );
	}

	/*
	 * PROTECTED METHODS
	 */

	/**
	 * Hook for subclassers. Instantiate here the overlay you want to use for
	 * the spots.
	 * 
	 * @param displaySettings
	 *            the display settings.
	 * @return the spot overlay
	 */
	protected SpotOverlay createSpotOverlay( final DisplaySettings displaySettings )
	{
		return new SpotOverlay( model, imp, displaySettings );
	}

	/**
	 * Hook for subclassers. Instantiate here the overlay you want to use for
	 * the spots.
	 * 
	 * @param displaySettings
	 *            the display settings.
	 * @return the track overlay
	 */
	protected TrackOverlay createTrackOverlay( final DisplaySettings displaySettings )
	{
		return new TrackOverlay( model, imp, displaySettings );
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Exposes the {@link ImagePlus} on which the model is drawn by this view.
	 *
	 * @return the ImagePlus used in this view.
	 */
	public ImagePlus getImp()
	{
		return imp;
	}

	@Override
	public void modelChanged( final ModelChangeEvent event )
	{
		switch ( event.getEventID() )
		{
		case ModelChangeEvent.MODEL_MODIFIED:
		case ModelChangeEvent.SPOTS_FILTERED:
		case ModelChangeEvent.SPOTS_COMPUTED:
		case ModelChangeEvent.TRACKS_VISIBILITY_CHANGED:
		case ModelChangeEvent.TRACKS_COMPUTED:
			refresh();
			break;
		}
	}

	@Override
	public void selectionChanged( final SelectionChangeEvent event )
	{
		// Highlight selection
		trackOverlay.setHighlight( selectionModel.getEdgeSelection() );
		spotOverlay.setSpotSelection( selectionModel.getSpotSelection() );
		// Center on last spot
		super.selectionChanged( event );
		// Redraw
		imp.updateAndDraw();
	}

	@Override
	public void centerViewOn( final Spot spot )
	{
		final int frame = spot.getFeature( Spot.FRAME ).intValue();
		final double dz = imp.getCalibration().pixelDepth;
		final long z = Math.round( spot.getFeature( Spot.POSITION_Z ) / dz ) + 1;
		imp.setPosition( imp.getC(), ( int ) z, frame + 1 );
	}

	@Override
	public void render()
	{
		clear();
		imp.setOpenAsHyperStack( true );
		if ( !imp.isVisible() )
			imp.show();

		addOverlay( spotOverlay );
		addOverlay( trackOverlay );
		imp.updateAndDraw();
		registerEditTool();
	}

	@Override
	public void refresh()
	{
		if ( null != imp )
			imp.updateAndDraw();
	}

	@Override
	public void clear()
	{
		Overlay overlay = imp.getOverlay();
		if ( overlay == null )
		{
			overlay = new Overlay();
			imp.setOverlay( overlay );
		}
		overlay.clear();
		refresh();
	}

	public void addOverlay( final Roi overlay )
	{
		imp.getOverlay().add( overlay );
	}

	public SelectionModel getSelectionModel()
	{
		return selectionModel;
	}

	/*
	 * PRIVATE METHODS
	 */

	private void registerEditTool()
	{
		editTool = SpotEditTool.getInstance();
		if ( !SpotEditTool.isLaunched() )
			editTool.run( "" );

		editTool.register( this );
	}

	@Override
	public String getKey()
	{
		return KEY;
	}
}
