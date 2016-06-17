package fiji.plugin.trackmate.action;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import ij.plugin.frame.SyncWindows;
import net.imglib2.RealPoint;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.traverse.GraphIterator;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * Author: Robert Haase, Scientific Computing Facility, MPI-CBG,
 * rhaase@mpi-cbg.de
 *
 * Date: June 2016
 */
public class CloseGapsByLinearInterpolationActionTest
{
	@Test
	public void testIfGapsInLinearTracksAreClosed()
	{
		TrackMate trackmate = new TrackMate();

		// Build a linear track with no division but a gap between spots
		final Model model = trackmate.getModel();
		model.beginUpdate();

		Spot spot0 = createSpot( 0, 0, 0 );
		Spot spot1 = createSpot( 1, 1, 0 );
		Spot spot2 = createSpot( 2, 2, 0 );
		Spot spot5 = createSpot( 5, 5, 0 );

		model.addSpotTo( spot0, 0 );
		model.addSpotTo( spot1, 1 );
		model.addSpotTo( spot2, 2 );
		model.addSpotTo( spot5, 5 );

		model.addEdge( spot0, spot1, 1.0 );
		model.addEdge( spot1, spot2, 1.0 );
		model.addEdge( spot2, spot5, 1.0 );

		model.endUpdate();

		// close gap
		CloseGapsByLinearInterpolationAction cgblia = new CloseGapsByLinearInterpolationAction();
		cgblia.execute( trackmate );

		TrackModel trackModel = model.getTrackModel();

		// Check if positions were interpolated in the right way
		GraphIterator< Spot, DefaultWeightedEdge > spots = trackModel.getDepthFirstIterator( spot0, true );

		double[][] referencePositions =
				{
						{ 0, 0 },
						{ 1, 1 },
						{ 2, 2 },
						{ 3, 3 },
						{ 4, 4 },
						{ 5, 5 }
				};

		checkPositions( spots, referencePositions );
	}

	@Test
	public void testIfGapsInDividingTracksAreClosed()
	{
		TrackMate trackmate = new TrackMate();

		// Build a linear track with a division and a gap between spots
		final Model model = trackmate.getModel();
		model.beginUpdate();

		Spot spot0 = createSpot( 0, 0, 0 );
		Spot spot1 = createSpot( 1, 1, 0 );
		Spot spot2 = createSpot( 2, 2, 0 );
		Spot spot5a = createSpot( 5, 5, 0 );
		Spot spot5b = createSpot( 8, 8, 0 );

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
		CloseGapsByLinearInterpolationAction cgblia = new CloseGapsByLinearInterpolationAction();
		cgblia.execute( trackmate );

		TrackModel trackModel = model.getTrackModel();

		// Check if positions were interpolated in the right way
		GraphIterator< Spot, DefaultWeightedEdge > spots = trackModel.getDepthFirstIterator( spot0, true );

		double[][] referencePositions =
				{
						{ 0, 0 },
						{ 1, 1 },
						{ 2, 2 },
						{ 4, 4 },
						{ 6, 6 },
						{ 8, 8 },
						{ 3, 3 },
						{ 4, 4 },
						{ 5, 5 }
				};

		checkPositions( spots, referencePositions );
	}

	private void checkPositions( GraphIterator< Spot, DefaultWeightedEdge > spots, double[][] referencePositions )
	{

		double tolerance = 0.00001;

		int count = 0;
		while ( spots.hasNext() )
		{
			Spot spot = spots.next();

			assertTrue( "Position X is as expected ", Math.abs( referencePositions[ count ][ 0 ] - spot.getDoublePosition( 0 ) ) < tolerance );
			assertTrue( "Position Y is as expected ", Math.abs( referencePositions[ count ][ 1 ] - spot.getDoublePosition( 1 ) ) < tolerance );

			count++;
		}

	}

	private Spot createSpot( double x, double y, double z )
	{
		Spot newSpot = new Spot( x, y, z, 1.0, 1.0 );

		newSpot.getFeatures().put( Spot.POSITION_T, 1.0 );
		return newSpot;
	}
}
