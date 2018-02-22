package fiji.plugin.trackmate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.Test;

public class TrackModelTest
{

	private static final int N_TRACKS = 3;

	private static final int DEPTH = 5;

	@Test
	public void testBuildingTracks()
	{
		final TrackModel model = new TrackModel();
		for ( int i = 0; i < N_TRACKS; i++ )
		{
			Spot previous = null;
			for ( int j = 0; j < DEPTH; j++ )
			{
				final Spot spot = new Spot( 0d, 0d, 0d, 1d, -1d );
				model.addSpot( spot );
				if ( null != previous )
				{
					model.addEdge( previous, spot, 1 );
				}
				previous = spot;
			}
		}

		// The must be N_TRACKS visible tracks in total
		assertEquals( N_TRACKS, model.nTracks( false ) );
		assertEquals( N_TRACKS, model.nTracks( true ) );
		// They must be made of DEPTH spots and DEPTH-1 edges
		for ( final Integer id : model.trackIDs( true ) )
		{
			assertEquals( DEPTH, model.trackSpots( id ).size() );
			assertEquals( DEPTH - 1, model.trackEdges( id ).size() );
		}
	}

	@Test
	public void testConnectingTracks()
	{
		// Build segments
		final TrackModel model = new TrackModel();
		final List< Spot > trackEnds = new ArrayList< >();
		final List< Spot > trackStarts = new ArrayList< >();
		for ( int i = 0; i < N_TRACKS; i++ )
		{
			Spot previous = null;
			Spot spot = null;
			for ( int j = 0; j < DEPTH; j++ )
			{
				spot = new Spot( 0d, 0d, 0d, 1d, -1d );
				model.addSpot( spot );
				if ( null != previous )
				{
					model.addEdge( previous, spot, 1 );
				}
				else
				{
					trackStarts.add( spot );
				}
				previous = spot;
			}
			trackEnds.add( spot );
		}
		// Connect segments
		for ( int i = 0; i < trackStarts.size() - 1; i++ )
		{
			final Spot end = trackEnds.get( i );
			final Spot start = trackStarts.get( i + 1 );
			model.addEdge( end, start, 2 );
		}

		// There must be one visible track in total
		assertEquals( 1, model.nTracks( false ) );
		assertEquals( 1, model.nTracks( true ) );
		// It must be long
		final Integer id = model.trackIDs( true ).iterator().next();
		assertEquals( N_TRACKS * DEPTH, model.trackSpots( id ).size() );
		assertEquals( N_TRACKS * DEPTH - 1, model.trackEdges( id ).size() );
	}

	@Test
	public void testBreakingTracksBySpots()
	{
		// Build 1 long track
		final TrackModel model = new TrackModel();
		final List< Spot > trackBreaks = new ArrayList< >();
		Spot previous = null;
		for ( int i = 0; i < N_TRACKS; i++ )
		{
			for ( int j = 0; j < DEPTH; j++ )
			{
				final Spot spot = new Spot( 0d, 0d, 0d, 1d, -1d );
				model.addSpot( spot );
				if ( null != previous )
				{
					model.addEdge( previous, spot, 1 );
				}
				previous = spot;
			}
			trackBreaks.add( previous );
		}
		// Break it
		for ( final Spot spot : trackBreaks )
		{
			model.removeSpot( spot );
		}

		// There must be N_TRACKS visible tracks in total
		assertEquals( N_TRACKS, model.nTracks( false ) );
		assertEquals( N_TRACKS, model.nTracks( true ) );
		// They must be DEPTH-1 long in spots
		for ( final Integer id : model.trackIDs( false ) )
		{
			assertEquals( DEPTH - 1, model.trackSpots( id ).size() );
			assertEquals( DEPTH - 2, model.trackEdges( id ).size() );
		}
	}

	@Test
	public void testBreakingTracksByEdges()
	{
		// Build 1 long track
		final TrackModel model = new TrackModel();
		final List< DefaultWeightedEdge > trackBreaks = new ArrayList< >();
		Spot previous = new Spot( 0d, 0d, 0d, 1d, -1d );
		model.addSpot( previous );
		for ( int i = 0; i < N_TRACKS; i++ )
		{
			DefaultWeightedEdge edge = null;
			for ( int j = 0; j < DEPTH; j++ )
			{
				final Spot spot = new Spot( 0d, 0d, 0d, 1d, -1d );
				model.addSpot( spot );
				edge = model.addEdge( previous, spot, 1 );
				previous = spot;
			}
			trackBreaks.add( edge );
		}
		// Break it
		for ( final DefaultWeightedEdge edge : trackBreaks )
		{
			model.removeEdge( edge );
		}

		// There must be N_TRACKS visible tracks in total
		assertEquals( N_TRACKS, model.nTracks( false ) );
		assertEquals( N_TRACKS, model.nTracks( true ) );
		// They must be DEPTH long in spots
		for ( final Integer id : model.trackIDs( false ) )
		{
			assertEquals( DEPTH, model.trackSpots( id ).size() );
			assertEquals( DEPTH - 1, model.trackEdges( id ).size() );
		}
	}

	@Test
	public void testVisibility()
	{
		final TrackModel model = new TrackModel();
		for ( int i = 0; i < N_TRACKS; i++ )
		{
			Spot previous = null;
			for ( int j = 0; j < DEPTH; j++ )
			{
				final Spot spot = new Spot( 0d, 0d, 0d, 1d, -1d );
				model.addSpot( spot );
				if ( null != previous )
				{
					model.addEdge( previous, spot, 1 );
				}
				previous = spot;
			}
		}
		// Make some of them invisible
		final Set< Integer > toHide = new HashSet< >( N_TRACKS );
		for ( final Integer id : model.trackIDs( true ) )
		{
			if ( new Random().nextBoolean() )
			{
				toHide.add( id );
			}
		}
		for ( final Integer id : toHide )
		{
			model.setVisibility( id, false );
		}

		// Test if visibility is reported correctly
		assertEquals( N_TRACKS - toHide.size(), model.nTracks( true ) );
		for ( final Integer id : model.trackIDs( false ) )
		{
			if ( toHide.contains( id ) )
			{
				assertEquals( false, model.isVisible( id ) );
			}
			else
			{
				assertEquals( true, model.isVisible( id ) );
			}
		}
	}

	@Test
	public void testVisibilityMerge()
	{
		final TrackModel model = new TrackModel();
		for ( int i = 0; i < 2; i++ )
		{
			Spot previous = null;
			for ( int j = 0; j < DEPTH; j++ )
			{
				final Spot spot = new Spot( 0d, 0d, 0d, 1d, -1d );
				model.addSpot( spot );
				if ( null != previous )
				{
					model.addEdge( previous, spot, 1 );
				}
				previous = spot;
			}
		}
		// Make one of them invisible
		final Integer toHide = model.trackIDs( true ).iterator().next();
		model.setVisibility( toHide, false );

		// Get the id of the other one
		final Set< Integer > ids = new HashSet< >( model.trackIDs( false ) );
		ids.remove( toHide );
		final Integer shown = ids.iterator().next();

		// Connect the two
		final Spot source = model.trackSpots( shown ).iterator().next();
		final Spot target = model.trackSpots( toHide ).iterator().next();
		model.addEdge( source, target, 1 );

		// Test if visibility is reported correctly
		assertEquals( 1, model.nTracks( false ) );
		final Integer id = model.trackIDs( false ).iterator().next();
		assertTrue( model.isVisible( id ) );
	}

}
