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
package fiji.plugin.trackmate.action;

import static org.junit.Assert.assertEquals;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.traverse.GraphIterator;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.action.closegaps.CloseGapsByLinearInterpolation;

/**
 * Author: Robert Haase, Scientific Computing Facility, MPI-CBG,
 * rhaase@mpi-cbg.de
 *
 * Date: June 2016
 */
public class CloseGapsByLinearInterpolationActionTest
{
//	@Test
	public void testIfGapsInLinearTracksAreClosed()
	{
		final TrackMate trackmate = new TrackMate();

		// Build a linear track with no division but a gap between spots
		final Model model = trackmate.getModel();
		model.beginUpdate();

		final Spot spot0 = createSpot( 0, 0, 0 );
		final Spot spot1 = createSpot( 1, 1, 0 );
		final Spot spot2 = createSpot( 2, 2, 0 );
		final Spot spot5 = createSpot( 5, 5, 0 );

		model.addSpotTo( spot0, 0 );
		model.addSpotTo( spot1, 1 );
		model.addSpotTo( spot2, 2 );
		model.addSpotTo( spot5, 5 );

		model.addEdge( spot0, spot1, 1.0 );
		model.addEdge( spot1, spot2, 1.0 );
		model.addEdge( spot2, spot5, 1.0 );

		model.endUpdate();

		// close gap
		final CloseGapsByLinearInterpolation cgblia = new CloseGapsByLinearInterpolation();
		cgblia.execute( trackmate, Logger.VOID_LOGGER );

		final TrackModel trackModel = model.getTrackModel();

		// Check if positions were interpolated in the right way
		final GraphIterator< Spot, DefaultWeightedEdge > spots = trackModel.getDepthFirstIterator( spot0, true );

		final double[][] referencePositions = { { 0, 0 }, { 1, 1 }, { 2, 2 }, { 3, 3 }, { 4, 4 }, { 5, 5 } };

		checkPositions( spots, referencePositions );
	}

//	@Test
	public void testIfGapsInDividingTracksAreClosed()
	{
		final TrackMate trackmate = new TrackMate();

		// Build a linear track with a division and a gap between spots
		final Model model = trackmate.getModel();
		model.beginUpdate();

		final Spot spot0 = createSpot( 0, 0, 0 );
		final Spot spot1 = createSpot( 1, 1, 0 );
		final Spot spot2 = createSpot( 2, 2, 0 );
		final Spot spot5a = createSpot( 5, 5, 0 );
		final Spot spot5b = createSpot( 8, 8, 0 );

		model.addSpotTo( spot0, 0 );
		model.addSpotTo( spot1, 1 );
		model.addSpotTo( spot2, 2 );
		model.addSpotTo( spot5a, 5 );
		model.addSpotTo( spot5b, 5 );

		model.addEdge( spot0, spot1, 1.0 );
		model.addEdge( spot1, spot2, 1.0 );
		model.addEdge( spot2, spot5a, 1.0 );
		model.addEdge( spot2, spot5b, 1.0 );

		model.endUpdate();

		// close gaps
		final CloseGapsByLinearInterpolation cgblia = new CloseGapsByLinearInterpolation();
		cgblia.execute( trackmate, Logger.VOID_LOGGER );

		final TrackModel trackModel = model.getTrackModel();

		// Check if positions were interpolated in the right way
		final GraphIterator< Spot, DefaultWeightedEdge > spots = trackModel.getDepthFirstIterator( spot0, true );

		final double[][] referencePositions = { { 0, 0 }, { 1, 1 }, { 2, 2 }, { 4, 4 }, { 6, 6 }, { 8, 8 }, { 3, 3 }, { 4, 4 }, { 5, 5 } };

		checkPositions( spots, referencePositions );
	}

//	@Test
	public void testIfGapsInDividingBackwardsTracksAreClosed()
	{
		final TrackMate trackmate = new TrackMate();

		// Build a linear track with a division and a gap between spots
		final Model model = trackmate.getModel();
		model.beginUpdate();

		final Spot spot0 = createSpot( 0, 0, 0 );
		final Spot spot1 = createSpot( 1, 1, 0 );
		final Spot spot2 = createSpot( 2, 2, 0 );
		final Spot spot5a = createSpot( 5, 5, 0 );
		final Spot spot5b = createSpot( 8, 8, 0 );

		model.addSpotTo( spot0, 0 );
		model.addSpotTo( spot1, 1 );
		model.addSpotTo( spot2, 2 );
		model.addSpotTo( spot5a, -1 );
		model.addSpotTo( spot5b, -1 );

		model.addEdge( spot0, spot1, 1.0 );
		model.addEdge( spot1, spot2, 1.0 );
		model.addEdge( spot5a, spot2, 1.0 );
		model.addEdge( spot5b, spot2, 1.0 );

		model.endUpdate();

		// close gaps
		final CloseGapsByLinearInterpolation cgblia = new CloseGapsByLinearInterpolation();
		cgblia.execute( trackmate, Logger.VOID_LOGGER );

		final TrackModel trackModel = model.getTrackModel();

		// Check if positions were interpolated in the right way
		final GraphIterator< Spot, DefaultWeightedEdge > spots = trackModel.getDepthFirstIterator( spot0, false );

		final double[][] referencePositions = { { 0, 0 }, { 1, 1 }, { 2, 2 }, { 3, 3 }, { 4, 4 }, { 5, 5 }, { 4, 4 }, { 6, 6 }, { 8, 8 } };

		checkPositions( spots, referencePositions );
	}

	private void checkPositions( final GraphIterator< Spot, DefaultWeightedEdge > spots, final double[][] referencePositions )
	{

		final double tolerance = 0.00001;
		int count = 0;
		while ( spots.hasNext() )
		{
			final Spot spot = spots.next();
			assertEquals( "Position X is not as expected.", referencePositions[ count ][ 0 ], spot.getDoublePosition( 0 ), tolerance );
			assertEquals( "Position Y is not as expected.", referencePositions[ count ][ 1 ], spot.getDoublePosition( 1 ), tolerance );
			count++;
		}
	}

	private Spot createSpot( final double x, final double y, final double z )
	{
		final Spot newSpot = new SpotBase( x, y, z, 1.0, 1.0 );
		newSpot.getFeatures().put( Spot.POSITION_T, 1.0 );
		return newSpot;
	}
}
