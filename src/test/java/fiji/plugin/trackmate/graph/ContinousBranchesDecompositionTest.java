package fiji.plugin.trackmate.graph;

import static org.junit.Assert.fail;
import ij.ImageJ;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.junit.Before;
import org.junit.Test;
import org.scijava.util.AppUtils;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.FastLAPTracker;
import fiji.plugin.trackmate.tracking.FastLAPTrackerFactory;
import fiji.plugin.trackmate.tracking.TrackerKeys;
import fiji.plugin.trackmate.visualization.PerEdgeFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class ContinousBranchesDecompositionTest
{

	private static final int N_TP = 20;

	private static final int N_SPOTS = 10;

	private static final double WIDTH = 50;

	private Model model;

	private ContinousBranchesDecomposition splitter;

	@Before
	public void setUp()
	{
		final File file = new File( AppUtils.getBaseDirectory( TrackMate.class ), "samples/FailCase_02.xml" );
		final TmXmlReader reader = new TmXmlReader( file );
		if ( reader.isReadingOk() )
		{
			model = reader.getModel();
		}
		else
		{
			System.err.println( reader.getErrorMessage() );
		}
	}

	// @Before
	public void setUp1() throws Exception
	{
		// Create spots
		final Random ran = new Random( 1l );
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

	// @Test
	public final void testBehavior()
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
	}

	@Test
	public void testReconstruction()
	{
		splitter = new ContinousBranchesDecomposition( model );
		if ( !splitter.checkInput() || !splitter.process() )
		{
			fail( splitter.getErrorMessage() );
		}

		final FromContinuousBranches builder = new FromContinuousBranches( splitter.getBranches(), splitter.getLinks() );
		if ( !builder.checkInput() || !builder.process() )
		{
			fail( builder.getErrorMessage() );
		}

		final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph = builder.getResult();

		// Are all the source spots in the reconstructed graph?
		for ( final Spot spot : model.getTrackModel().vertexSet()) {
			if ( !graph.containsVertex( spot ) )
			{
				fail( "The reconstructed graph misses one spot that was in the original model: " + spot );
			}
		}

		// Are all the source edges in the reconstructed graph?
		for ( final DefaultWeightedEdge edge : model.getTrackModel().edgeSet() )
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

	public static void main( final String[] args ) throws Exception
	{
		final ContinousBranchesDecompositionTest test = new ContinousBranchesDecompositionTest();
		test.setUp();

		final SelectionModel sm = new SelectionModel( test.model );

		ImageJ.main( args );
		final HyperStackDisplayer displayer = new HyperStackDisplayer( test.model, sm );
		displayer.render();

		final ContinousBranchesDecomposition splitter = new ContinousBranchesDecomposition( test.model );
		if ( !splitter.checkInput() || !splitter.process() )
		{
			System.err.println( splitter.getErrorMessage() );
			return;
		}

		final Collection< List< Spot >> branches = splitter.getBranches();
		final Collection< Spot[] > links = splitter.getLinks();

		final Model model2 = new Model();
		model2.beginUpdate();
		try
		{
			for ( final List< Spot > branch : branches )
			{
				final Iterator< Spot > it = branch.iterator();
				Spot previous = it.next();
				model2.addSpotTo( previous, previous.getFeature( Spot.FRAME ).intValue() );
				while ( it.hasNext() )
				{
					final Spot spot = it.next();
					model2.addSpotTo( spot, spot.getFeature( Spot.FRAME ).intValue() );
					model2.addEdge( previous, spot, -1d );
					previous = spot;
				}
			}

			// Links
			for ( final Spot[] link : links )
			{
				final Spot source = link[ 0 ];
				final Spot target = link[ 1 ];
				try
				{
					model2.addEdge( source, target, -2d );
				}
				catch ( final NullPointerException npe )
				{
					System.err.println( "Could not add the edge " + source + "-" + target + ": already exists." );// DEBUG
				}
			}
		}
		finally
		{
			model2.endUpdate();
		}

		final TrackIndexAnalyzer analyzer = new TrackIndexAnalyzer();
		analyzer.process( model2.getTrackModel().trackIDs( true ), model2 );
		final EdgeTargetAnalyzer edgeAnalyzer = new EdgeTargetAnalyzer();
		edgeAnalyzer.process( model2.getTrackModel().edgeSet(), model2 );

		final PerEdgeFeatureColorGenerator ecg = new PerEdgeFeatureColorGenerator( model2, EdgeTargetAnalyzer.EDGE_COST );

		final SelectionModel sm2 = new SelectionModel( model2 );
		final HyperStackDisplayer displayer2 = new HyperStackDisplayer( model2, sm2 );
		displayer2.setDisplaySettings( TrackMateModelView.KEY_TRACK_COLORING, ecg );
		displayer2.render();
		final TrackScheme trackScheme = new TrackScheme( model2, sm2 );
		trackScheme.setDisplaySettings( TrackMateModelView.KEY_TRACK_COLORING, ecg );
		trackScheme.render();

	}

}
