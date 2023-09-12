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

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.detection.semiauto.SemiAutoTracker;
import fiji.plugin.trackmate.util.ModelTools;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.util.Threads;
import fiji.plugin.trackmate.util.TrackNavigator;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.FreehandRoi;
import ij.gui.ImageCanvas;

public class ModelEditActions
{

	/**
	 * Fall back default radius when the settings does not give a default radius
	 * to use.
	 */
	static final double FALL_BACK_RADIUS = 5.;

	private static final double COARSE_STEP = 2;

	private static final double FINE_STEP = 0.2f;

	private final Model model;

	private final SelectionModel selectionModel;

	private final Logger logger;

	private final ImagePlus imp;

	private Spot quickEditedSpot;

	private double previousRadius = FALL_BACK_RADIUS;

	private FreehandRoi roiedit;

	private final TrackNavigator trackNavigator;

	public ModelEditActions( final ImagePlus imp, final Model model, final SelectionModel selectionModel, final Logger logger )
	{
		this.imp = imp;
		this.model = model;
		this.selectionModel = selectionModel;
		this.logger = logger;
		this.trackNavigator = new TrackNavigator( model, selectionModel );
	}

	private Spot makeSpot( Point mouseLocation )
	{
		final ImageCanvas canvas = imp.getCanvas();
		if ( mouseLocation == null )
		{
			mouseLocation = MouseInfo.getPointerInfo().getLocation();
			SwingUtilities.convertPointFromScreen( mouseLocation, canvas );
		}
		final double[] calibration = TMUtils.getSpatialCalibration( imp );
		return new SpotBase(
				( -0.5 + canvas.offScreenXD( mouseLocation.x ) ) * calibration[ 0 ],
				( -0.5 + canvas.offScreenYD( mouseLocation.y ) ) * calibration[ 1 ],
				( imp.getSlice() - 1 ) * calibration[ 2 ],
				FALL_BACK_RADIUS,
				-1. );
	}

	private Spot getSpotAtMouseLocation()
	{
		final Spot clickLocation = makeSpot( null );
		final int frame = imp.getFrame() - 1;
		return model.getSpots().getSpotAt( clickLocation, frame, true );
	}

	private void updateStatusBar( final Spot spot, final String units )
	{
		if ( null == spot )
			return;
		String statusString = "";
		if ( null == spot.getName() || spot.getName().equals( "" ) )
			statusString = String.format( Locale.US, "Spot ID%d, x = %.1f, y = %.1f, z = %.1f, r = %.1f %s", spot.ID(), spot.getFeature( Spot.POSITION_X ), spot.getFeature( Spot.POSITION_Y ), spot.getFeature( Spot.POSITION_Z ), spot.getFeature( Spot.RADIUS ), units );
		else
			statusString = String.format( Locale.US, "Spot %s, x = %.1f, y = %.1f, z = %.1f, r = %.1f %s", spot.getName(), spot.getFeature( Spot.POSITION_X ), spot.getFeature( Spot.POSITION_Y ), spot.getFeature( Spot.POSITION_Z ), spot.getFeature( Spot.RADIUS ), units );
		IJ.showStatus( statusString );
	}

	public final void deleteSpotSelection()
	{
		final ArrayList< Spot > spotSelection = new ArrayList<>( selectionModel.getSpotSelection() );
		final ArrayList< DefaultWeightedEdge > edgeSelection = new ArrayList<>( selectionModel.getEdgeSelection() );
		model.beginUpdate();
		try
		{
			selectionModel.clearSelection();
			for ( final DefaultWeightedEdge edge : edgeSelection )
			{
				model.removeEdge( edge );
				logger.log( "Removed edge " + edge + ".\n" );
			}
			for ( final Spot spot : spotSelection )
			{
				model.removeSpot( spot );
				logger.log( "Removed spot " + spot + ".\n" );
			}
		}
		finally
		{
			model.endUpdate();
		}
	}

	public void semiAutoTracking( final double qualityThreshold, final double distanceTolerance, final int nFrames )
	{
		logger.log( "Semi-automatic tracking.\n" );
		@SuppressWarnings( "rawtypes" )
		final SemiAutoTracker autotracker = new SemiAutoTracker( model, selectionModel, imp, logger );
		autotracker.setParameters( qualityThreshold, distanceTolerance, nFrames );
		autotracker.setNumThreads( Prefs.getThreads() / 2 );
		Threads.run( "TrackMate semi-automated tracking thread", () -> {
			final boolean ok = autotracker.checkInput() && autotracker.process();
			if ( !ok )
				logger.error( autotracker.getErrorMessage() );
		} );
	}

	public void addSpot( final boolean autoLinkingmode )
	{
		final double radius = previousRadius;
		final Spot newSpot = makeSpot( null );
		final double dt = imp.getCalibration().frameInterval;
		final int frame = imp.getFrame() - 1;
		newSpot.putFeature( Spot.POSITION_T, frame * dt );
		newSpot.putFeature( Spot.FRAME, Double.valueOf( frame ) );
		newSpot.putFeature( Spot.RADIUS, radius );
		newSpot.putFeature( Spot.QUALITY, -1d );

		model.beginUpdate();
		try
		{
			model.addSpotTo( newSpot, frame );
			logger.log( "Added spot " + newSpot + " to frame " + frame + ".\n" );
		}
		finally
		{
			model.endUpdate();
		}

		/*
		 * If we are in auto-link mode, we create an edge with spot in
		 * selection, if there is just one and if it is in a previous frame
		 */
		if ( autoLinkingmode )
		{
			final Set< Spot > spotSelection = selectionModel.getSpotSelection();
			if ( spotSelection.size() == 1 )
			{
				final Spot source = spotSelection.iterator().next();
				if ( newSpot.diffTo( source, Spot.FRAME ) != 0 )
				{
					model.beginUpdate();
					try
					{
						model.addEdge( source, newSpot, -1 );
						logger.log( "Created a link between " + source + " and " + newSpot + ".\n" );
					}
					finally
					{
						model.endUpdate();
					}
				}
			}
			selectionModel.clearSpotSelection();
			selectionModel.addSpotToSelection( newSpot );
		}
	}

	public void deleteSpot()
	{
		final Spot target = getSpotAtMouseLocation();
		if ( null == target )
			return;

		selectionModel.removeSpotFromSelection( target );
		model.beginUpdate();
		try
		{
			model.removeSpot( target );
			logger.log( "Removed spot " + target + ".\n" );
		}
		finally
		{
			model.endUpdate();
		}
	}

	public void startMoveSpot()
	{
		if ( null == quickEditedSpot )
			quickEditedSpot = getSpotAtMouseLocation();
	}

	public void moveSpot( final Point mouseLocation )
	{
		if ( quickEditedSpot == null )
			return;

		final ImageCanvas canvas = imp.getCanvas();
		final double[] calibration = TMUtils.getSpatialCalibration( imp );
		final double x = ( -0.5 + canvas.offScreenXD( mouseLocation.x ) ) * calibration[ 0 ];
		final double y = ( -0.5 + canvas.offScreenYD( mouseLocation.y ) ) * calibration[ 1 ];
		final double z = ( imp.getSlice() - 1 ) * calibration[ 2 ];

		quickEditedSpot.putFeature( Spot.POSITION_X, x );
		quickEditedSpot.putFeature( Spot.POSITION_Y, y );
		quickEditedSpot.putFeature( Spot.POSITION_Z, z );
		imp.updateAndDraw();
	}

	public void endMoveSpot()
	{
		if ( null == quickEditedSpot )
			return;
		model.beginUpdate();
		try
		{
			model.updateFeatures( quickEditedSpot );
		}
		finally
		{
			model.endUpdate();
		}
		quickEditedSpot = null;
	}

	public void changeSpotRadius( final boolean increase, final boolean fast )
	{
		final Spot target = getSpotAtMouseLocation();
		if ( null == target )
			return;

		final double radius = target.getFeature( Spot.RADIUS );
		final int factor = ( increase ) ? -1 : 1;
		final double dx = imp.getCalibration().pixelWidth;

		final double newRadius = ( fast )
				? radius + factor * dx * COARSE_STEP
				: radius + factor * dx * FINE_STEP;

		if ( newRadius <= dx )
			return;

		// Store new value of radius for next spot creation.
		previousRadius = newRadius;

		// Actually scale the spot.
		target.scale( radius / newRadius );

		model.beginUpdate();
		try
		{
			model.updateFeatures( target );
			logger.log( String.format( Locale.US, "Changed spot " + target + " radius to %.1f " + model.getSpaceUnits() + ".\n", radius ) );
		}
		finally
		{
			model.endUpdate();
		}
	}

	public void toggleLink()
	{
		final Set< Spot > selectedSpots = selectionModel.getSpotSelection();
		if ( selectedSpots.size() == 2 )
		{
			final Iterator< Spot > it = selectedSpots.iterator();
			final Spot sourceTmp = it.next();
			final Spot targetTmp = it.next();

			final Spot source = sourceTmp.diffTo( targetTmp, Spot.FRAME ) < 0 ? sourceTmp : targetTmp;
			final Spot target = sourceTmp.diffTo( targetTmp, Spot.FRAME ) < 0 ? targetTmp : sourceTmp;

			if ( model.getTrackModel().containsEdge( source, target ) )
			{
				/*
				 * Remove it
				 */
				model.beginUpdate();
				try
				{
					model.removeEdge( source, target );
					logger.log( "Removed edge between " + source + " and " + target + ".\n" );
				}
				finally
				{
					model.endUpdate();
				}

			}
			else
			{
				/*
				 * Create a new link
				 */
				final int ts = source.getFeature( Spot.FRAME ).intValue();
				final int tt = target.getFeature( Spot.FRAME ).intValue();

				if ( tt != ts )
				{
					model.beginUpdate();
					try
					{
						model.addEdge( source, target, -1 );
						logger.log( "Created an edge between " + source + " and " + target + ".\n" );
					}
					finally
					{
						model.endUpdate();
					}
					/*
					 * To emulate a kind of automatic linking, we put the last
					 * spot to the selection, so several spots can be tracked in
					 * a row without having to de-select one
					 */
					final Spot single = ( tt > ts ) ? target : source;
					selectionModel.clearSpotSelection();
					selectionModel.addSpotToSelection( single );
				}
				else
				{
					logger.error( "Cannot create an edge between two spots belonging to the same frame.\n" );
				}
			}

		}
		else
		{
			logger.error( "Expected selection to contain 2 spots, found " + selectedSpots.size() + ".\n" );
		}
	}

	public void stepInTime( final boolean forward, final int stepwiseTimeBrowsing )
	{
		// Stepwise time browsing.
		final int currentT = imp.getT() - 1;
		final int prevStep = ( currentT / stepwiseTimeBrowsing ) * stepwiseTimeBrowsing;
		int tp;
		if ( forward )
		{
			tp = prevStep + stepwiseTimeBrowsing;
		}
		else
		{
			if ( currentT == prevStep )
				tp = currentT - stepwiseTimeBrowsing;
			else
				tp = prevStep;
		}
		imp.setT( tp + 1 );
	}

	public void select( final Point point, final boolean addToSelection, final boolean canClearSelection )
	{
		// If no target, we clear selection
		final Spot target = getSpotAtMouseLocation();
		if ( null == target )
		{
			if ( canClearSelection )
			{
				selectionModel.clearSelection();
				logger.log( "Cleared selection.\n" );
			}
			roiedit = null;
			imp.setRoi( roiedit );
		}
		else
		{
			updateStatusBar( target, imp.getCalibration().getUnits() );
			if ( addToSelection )
			{
				if ( selectionModel.getSpotSelection().contains( target ) )
					selectionModel.removeSpotFromSelection( target );
				else
					selectionModel.addSpotToSelection( target );
			}
			else
			{
				selectionModel.clearSpotSelection();
				selectionModel.addSpotToSelection( target );
			}
		}
	}

	public void roiEdit( final MouseEvent e )
	{
		if ( null == roiedit )
		{
			if ( !IJ.spaceBarDown() )
			{
				roiedit = new FreehandRoi( e.getX(), e.getY(), imp )
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected void handleMouseUp( final int screenX, final int screenY )
					{
						type = FREEROI;
						super.handleMouseUp( screenX, screenY );
					}
				};
				imp.setRoi( roiedit );
			}
		}
		else
		{
			roiedit.mouseDragged( e );
		}
	}

	public void selectInRoi( final MouseEvent e )
	{
		if ( null != roiedit )
		{
			Threads.run( "SpotEditTool roiedit processing", () -> {
				roiedit.mouseReleased( e );
				final int frame = imp.getFrame() - 1;

				final Iterator< Spot > it;
				if ( IJ.shiftKeyDown() )
					it = model.getSpots().iterator( true );
				else
					it = model.getSpots().iterator( frame, true );

				final Collection< Spot > added = new ArrayList<>();
				final double calibration[] = TMUtils.getSpatialCalibration( imp );

				while ( it.hasNext() )
				{
					final Spot spot = it.next();
					final double x = spot.getFeature( Spot.POSITION_X );
					final double y = spot.getFeature( Spot.POSITION_Y );
					// In pixel units
					final int xp = ( int ) ( x / calibration[ 0 ] + 0.5f );
					final int yp = ( int ) ( y / calibration[ 1 ] + 0.5f );

					if ( null != roiedit && roiedit.contains( xp, yp ) )
						added.add( spot );
				}

				if ( !added.isEmpty() )
				{
					selectionModel.addSpotToSelection( added );
					if ( added.size() == 1 )
						logger.log( "Added one spot to selection.\n" );
					else
						logger.log( "Added " + added.size() + " spots to selection.\n" );
				}
				roiedit = null;
			} );
		}
	}

	public void selectTrackDownward()
	{
		ModelTools.selectTrackDownward( selectionModel );
	}

	public void selectTrackUpward()
	{
		ModelTools.selectTrackUpward( selectionModel );
	}

	public void selectTrack()
	{
		ModelTools.selectTrack( selectionModel );
	}

	public void navigateToChild()
	{
		trackNavigator.nextInTime();
	}

	public void navigateToParent()
	{
		trackNavigator.previousInTime();
	}

	public void navigateToNextSibling()
	{
		trackNavigator.nextSibling();
	}

	public void navigateToPreviousSibling()
	{
		trackNavigator.previousSibling();
	}

	public void navigateToNextTrack()
	{
		trackNavigator.nextTrack();
	}

	public void navigateToPreviousTrack()
	{
		trackNavigator.previousTrack();
	}
}
