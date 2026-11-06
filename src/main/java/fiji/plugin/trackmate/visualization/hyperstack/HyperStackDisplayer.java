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

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.WrappedActionMap;
import org.scijava.ui.behaviour.util.WrappedInputMap;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.SelectionChangeEvent;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.GuiModel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.UpdateListener;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.ViewUtils;
import fiji.plugin.trackmate.visualization.hyperstack.behaviours.HyperStackDisplayerActions;
import fiji.plugin.trackmate.visualization.hyperstack.behaviours.ImagePlusBehavioursAdapter;
import fiji.plugin.trackmate.visualization.hyperstack.behaviours.SelectSpotsWithRoiListener;
import fiji.plugin.trackmate.visualization.hyperstack.behaviours.SpotEditBehaviours;
import fiji.plugin.trackmate.visualization.ui.KeyConfigContexts;
import fiji.plugin.trackmate.visualization.ui.TrackMateKeymapManager;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;

public class HyperStackDisplayer extends AbstractTrackMateModelView
{

	protected final ImagePlus imp;

	protected final SpotOverlay spotOverlay;

	protected final TrackOverlay trackOverlay;

	public static final String KEY = "HYPERSTACKDISPLAYER";

	/*
	 * CONSTRUCTORS
	 */

	public HyperStackDisplayer( final GuiModel guiModel )
	{
		super( guiModel );
		if ( null != guiModel.getSettings().imp )
			this.imp = guiModel.getSettings().imp;
		else
			this.imp = ViewUtils.makeEmptyImagePlus( guiModel.getModel() );

		final DisplaySettings displaySettings = guiModel.getDisplaySettings();
		this.spotOverlay = new SpotOverlay( guiModel.getModel(), imp, guiModel.getDisplaySettings() );
		this.trackOverlay = new TrackOverlay( guiModel.getModel(), imp, guiModel.getDisplaySettings() );

		final UpdateListener refresher = () -> refresh();
		displaySettings.listeners().add( refresher );
		onClose( () -> displaySettings.listeners().remove( refresher ) );
	}

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
		trackOverlay.setHighlight( guiModel.getSelectionModel().getEdgeSelection() );
		spotOverlay.setSpotSelection( guiModel.getSelectionModel().getSpotSelection() );
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

		imp.getWindow().addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				close();
			}
		} );

		addOverlay( spotOverlay );
		addOverlay( trackOverlay );
		imp.updateAndDraw();

		/*
		 * UI behaviours and actions
		 */

		try
		{
			final Model model = guiModel.getModel();
			final SelectionModel selectionModel = guiModel.getSelectionModel();
			final TrackMateKeymapManager keymapManager = guiModel.getKeymapManager();
			final ImagePlusBehavioursAdapter adapter = new ImagePlusBehavioursAdapter( imp, keymapManager, new String[] { KeyConfigContexts.HYPERSTACK_DISPLAYER, KeyConfigContexts.TRACKMATE } );
			SpotEditBehaviours.install( adapter.behaviours(), model, selectionModel, imp );
			HyperStackDisplayerActions.install( adapter.actions(), guiModel, imp );
			// Select spots with freehand ROI.
			SelectSpotsWithRoiListener.install( model, selectionModel, imp );
			// Global actions.
			final Actions globalActions = guiModel.getGlobalActions();
			adapter.keybindings().addActionMap( "global", new WrappedActionMap( globalActions.getActionMap() ) );
			adapter.keybindings().addInputMap( "global", new WrappedInputMap( globalActions.getInputMap() ) );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
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

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public Window getWindow()
	{
		return imp.getWindow();
	}
}
