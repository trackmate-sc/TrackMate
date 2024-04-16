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
package fiji.plugin.trackmate.action.fit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.util.EverythingDisablerAndReenabler;
import fiji.plugin.trackmate.util.Threads;
import ij.ImagePlus;

public class SpotFitterController
{

	private final TrackMate trackmate;

	private final SelectionModel selectionModel;

	private final SpotFitterPanel gui;

	private final Logger logger;

	private final Map< Spot, double[] > undo;

	public SpotFitterController( final TrackMate trackmate, final SelectionModel selectionModel, final Logger logger )
	{
		this.trackmate = trackmate;
		this.selectionModel = selectionModel;
		this.logger = logger;
		this.undo = new HashMap<>();

		final Settings settings = trackmate.getSettings();
		final List< String > fits = getAvailableFits( DetectionUtils.is2D( settings.imp ) );
		final List< String > docs = getDocs( DetectionUtils.is2D( settings.imp ) );
		this.gui = new SpotFitterPanel( fits, docs, settings.imp.getNChannels() );

		gui.btnFit.addActionListener( e -> fit() );
		gui.btnUndo.addActionListener( e -> undo() );
	}

	private void undo()
	{
		final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( gui, new Class[] { JLabel.class } );
		disabler.disable();
		Threads.run( "SpotFitterControllerUndoThread", () ->
		{
			try
			{
				logger.log( "Undoing last fit.\n" );
				logger.setStatus( "Undoing" );

				int progress = 0;
				for ( final Spot spot : undo.keySet() )
				{
					final double[] arr = undo.get( spot );
					spot.putFeature( Spot.POSITION_X, arr[ 0 ] );
					spot.putFeature( Spot.POSITION_Y, arr[ 1 ] );
					spot.putFeature( Spot.POSITION_Z, arr[ 2 ] );
					spot.putFeature( Spot.RADIUS, arr[ 3 ] );
					logger.setProgress( ( double ) progress++ / undo.size() );
				}
				logger.setProgress( 0. );
				// Recompute features.
				trackmate.computeSpotFeatures( true );
				trackmate.computeEdgeFeatures( true );
				trackmate.computeTrackFeatures( true );
				logger.log( "Undoing done.\n" );

				// Notify changes happened.
				trackmate.getModel().getModelChangeListener().forEach( l -> l.modelChanged( new ModelChangeEvent( this, ModelChangeEvent.MODEL_MODIFIED ) ) );
			}
			finally
			{
				disabler.reenable();
			}
		} );
	}

	private void fit()
	{
		final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( gui, new Class[] { JLabel.class } );
		disabler.disable();
		Threads.run( "SpotFitterControllerFitterThread", () ->
		{
			try
			{
				final ImagePlus imp = trackmate.getSettings().imp;
				// 1-based to 0-based.
				final int channel = gui.getSelectedChannel() - 1;
				final int index = gui.getSelectedFitIndex();
				final SpotFitter fitter;
				// Stupid and harsh:
				if ( DetectionUtils.is2D( imp ) )
				{
					if ( index == 0 )
						fitter = new SpotGaussianFitter2D( imp, channel );
					else if ( index == 1 )
						fitter = new SpotGaussianFitter2DFixedRadius( imp, channel );
					else
						throw new IllegalArgumentException( "Index points to an unknown fit model: " + index );
				}
				else
				{
					if ( index == 0 )
						fitter = new SpotGaussianFitter3D( imp, channel );
					else if ( index == 1 )
						fitter = new SpotGaussianFitter3DFixedRadius( imp, channel );
					else
						throw new IllegalArgumentException( "Index points to an unknown fit model: " + index );
				}
				fitter.setNumThreads( trackmate.getNumThreads() );

				// Get spots to fit.
				final Iterable< Spot > spots;
				if ( gui.rdbtnAll.isSelected() )
					spots = trackmate.getModel().getSpots().iterable( true );
				else if ( gui.rdbtnSelection.isSelected() )
					spots = selectionModel.getSpotSelection();
				else
				{
					selectionModel.selectTrack(
							selectionModel.getSpotSelection(),
							selectionModel.getEdgeSelection(), 0 );
					spots = selectionModel.getSpotSelection();
				}

				// Prepare the undo array.
				undo.clear();
				for ( final Spot spot : spots )
				{
					undo.put( spot, new double[] {
							spot.getDoublePosition( 0 ),
							spot.getDoublePosition( 1 ),
							spot.getDoublePosition( 2 ),
							spot.getFeature( Spot.RADIUS ).doubleValue()
					} );
				}

				// Perform fit.
				fitter.process( spots, logger );

				// Recompute features.
				trackmate.computeSpotFeatures( true );
				trackmate.computeEdgeFeatures( true );
				trackmate.computeTrackFeatures( true );

				// Notify changes happened.
				trackmate.getModel().getModelChangeListener().forEach( l -> l.modelChanged( new ModelChangeEvent( this, ModelChangeEvent.MODEL_MODIFIED ) ) );
			}
			finally
			{
				disabler.reenable();
			}
		} );
	}

	public void show()
	{
		if ( gui.getParent() != null && gui.getParent().isVisible() )
			return;

		final JFrame frame = new JFrame( "TrackMate spot fitting" );
		frame.setIconImage( Icons.SPOT_ICON.getImage() );
		frame.setSize( 300, 300 );
		frame.getContentPane().add( gui );
		GuiUtils.positionWindow( frame, trackmate.getSettings().imp.getCanvas() );
		frame.setVisible( true );
	}

	private List< String > getDocs( final boolean is2d )
	{
		final List< String > docs = new ArrayList<>();
		if ( is2d )
		{
			docs.add(
					"<html>Fit a 2D circular Gaussian on each spot, "
							+ "allowing for the radius to vary.</html>" );
			docs.add( "<html>Fit a 2D circular Gaussian on each spot, "
					+ "but blocking its sigma value. The radius of the spot"
					+ "is not updated.</html>" );
		}
		else
		{
			docs.add(
					"<html>Fit a 3D anisotropic Gaussian on each spot. The Z sigma can be different "
							+ "from the sigmas in X and Y, which are forced to be identical. The radius of "
							+ "the spot is set from the sigma in XY.</html>" );
			docs.add(
					"<html>Fit a 3D anisotropic Gaussian on each spot. We only allow X, Y, Z and the "
							+ "amplitude to adjust in the fit. The spot radius is left unchanged.</html>" );
		}
		return docs;
	}

	private List< String > getAvailableFits( final boolean is2d )
	{
		final List< String > fits = new ArrayList<>();
		if ( is2d )
		{
			fits.add( "Gaussian 2D" );
			fits.add( "Gaussian 2D with fixed radius" );
		}
		else
		{
			fits.add( "Elliptical orthogonal Gaussian 3D" );
			fits.add( "Gaussian 3D with fixed radius" );
		}
		return fits;
	}
}
