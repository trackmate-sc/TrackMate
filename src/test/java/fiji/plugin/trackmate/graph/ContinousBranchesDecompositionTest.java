package fiji.plugin.trackmate.graph;

import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.junit.Before;
import org.junit.Test;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.FastLAPTracker;
import fiji.plugin.trackmate.tracking.FastLAPTrackerFactory;
import fiji.plugin.trackmate.tracking.TrackerKeys;

public class ContinousBranchesDecompositionTest
{

	private static final int N_TP = 10;

	private static final int N_SPOTS = 20;

	private static final double WIDTH = 50;

	private Model model;

	private ContinousBranchesDecomposition splitter;

	@Before
	public void setUp() throws Exception
	{
		// Create spots
		final Random ran = new Random();
		final SpotCollection sc = new SpotCollection();
		for ( int t = 0; t < N_TP; t++ )
		{
			for ( int i = 0; i < N_SPOTS; i++ )
			{
				final double x = ran.nextDouble() * WIDTH;
				final double y = ran.nextDouble() * WIDTH;
				final double z = ran.nextDouble() * WIDTH;
				final Spot spot = new Spot( x, y, z, 1d, -1d );
				sc.add( spot, Integer.valueOf( t ) );
			}
		}

		model = new Model();
		model.setSpots( sc, false );

		// Track
		final Map< String, Object > settings = new FastLAPTrackerFactory().getDefaultSettings();
		settings.put( TrackerKeys.KEY_ALLOW_TRACK_MERGING, true );
		settings.put( TrackerKeys.KEY_ALLOW_TRACK_SPLITTING, true );

		final FastLAPTracker tracker = new FastLAPTracker( sc, settings );
		if ( !tracker.checkInput() || !tracker.process() )
		{
			fail( tracker.getErrorMessage() );
		}

		final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph = tracker.getResult();
		model.setTracks( graph, false );
	}

	@Test
	public void testBehavior()
	{
		splitter = new ContinousBranchesDecomposition( model );
		if ( !splitter.checkInput() || !splitter.process() )
		{
			fail( splitter.getErrorMessage() );
		}

		// Inspect branches
		final Collection< List< Spot >> branches = splitter.getBranches();

		for ( final List< Spot > branch : branches )
		{
			if ( branch.size() == 0 )
			{
				fail( "Found a branch made of 0 spots." );
			}

			final Iterator< Spot > it = branch.iterator();
			Spot previous = it.next();
			while ( it.hasNext() )
			{
				final Spot spot = it.next();
				if ( spot.diffTo( previous, Spot.FRAME ) != 1d )
				{
					fail( "Spots " + spot + " and " + previous + " are not separated by exactly one frame." );
				}
				previous = spot;
			}
		}

		// Test unicity
		final Set< Spot > allSpots = new HashSet< Spot >();
		for ( final List< Spot > branch : branches )
		{
			allSpots.addAll( branch );
		}
		for ( final Spot spot : allSpots )
		{
			boolean foundOnce = false;
			for ( final List< Spot > branch : branches )
			{
				if ( branch.contains( spot ) )
				{
					if ( foundOnce )
					{
						fail( "The spot " + spot + " belongs to at least two branches. One of them is " + branch );
					}
					foundOnce = true;
				}
			}
		}
	}

	@Test
	public void testReconstruction()
	{
		splitter = new ContinousBranchesDecomposition( model );
		if ( !splitter.checkInput() || !splitter.process() )
		{
			fail( splitter.getErrorMessage() );
		}

		final Collection< List< Spot >> branches = splitter.getBranches();
		final Collection< List< Spot >> links = splitter.getLinks();

		final FromContinuousBranches builder = new FromContinuousBranches( branches, links );
		if ( !builder.checkInput() || !builder.process() )
		{
			fail( builder.getErrorMessage() );
		}

		final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph = builder.getResult();

		// Are all the source spots in the reconstructed graph?
		final Set< Spot > allSourceSpots = new HashSet< Spot >();
		for ( final Integer trackID : model.getTrackModel().trackIDs( true ) )
		{
			allSourceSpots.addAll( model.getTrackModel().trackSpots( trackID ) );
		}

		for ( final Spot spot : allSourceSpots )
		{
			if ( !graph.containsVertex( spot ) )
			{
				fail( "The reconstructed graph misses one spot that was in the original model: " + spot );
			}
		}

		// Are all the source edges in the reconstructed graph?
		final Set< DefaultWeightedEdge > allSourceEdges = new HashSet< DefaultWeightedEdge >();
		for ( final Integer trackId : model.getTrackModel().trackIDs( true ) )
		{
			allSourceEdges.addAll( model.getTrackModel().trackEdges( trackId ) );
		}

		for ( final DefaultWeightedEdge edge : allSourceEdges )
		{
			final Spot source = model.getTrackModel().getEdgeSource( edge );
			final Spot target = model.getTrackModel().getEdgeTarget( edge );
			if ( !graph.containsEdge( source, target ) )
			{
				fail( "The reconstructed graph misses one edge that was in the original model: " + edge );
			}
		}

		// Is the reconstructed graph made of edges that were in the source
		// model?
		for ( final DefaultWeightedEdge edge : graph.edgeSet() )
		{
			final Spot source = graph.getEdgeSource( edge );
			final Spot target = graph.getEdgeTarget( edge );
			if ( !graph.containsEdge( source, target ) )
			{
				fail( "The reconstructed graph has an edge that was not in the original model: " + edge );
			}
		}
	}
}