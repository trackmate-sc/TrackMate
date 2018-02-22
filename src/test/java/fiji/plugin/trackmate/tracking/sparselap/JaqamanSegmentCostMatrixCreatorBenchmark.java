package fiji.plugin.trackmate.tracking.sparselap;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_CUTOFF_PERCENTILE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_MAX_DISTANCE;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.tracking.LAPUtils;
import fiji.plugin.trackmate.tracking.oldlap.FastLAPTrackerFactory;
import fiji.plugin.trackmate.tracking.sparselap.costmatrix.JaqamanSegmentCostMatrixCreator;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.ImageJ;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

public class JaqamanSegmentCostMatrixCreatorBenchmark
{

	public static void main( final String[] args )
	{
//		benchmark();
//		baseTest();
		testSimil();
	}

	public static final void benchmarkMemory()
	{
		final int nFrames = 50;
		final int nSpotsPerFrame = 400;
		final long seed = 1l;

		/*
		 * Create random model.
		 */

		final Random ran = new Random( seed );
		final SpotCollection spots = new SpotCollection();
		for ( int frame = 0; frame < nFrames; frame++ )
		{
			final Collection< Spot > ls = new ArrayList< >( nSpotsPerFrame );
			for ( int j = 0; j < nSpotsPerFrame; j++ )
			{
				final Spot spot = new Spot( ran.nextDouble() * 100, ran.nextDouble() * 100, ran.nextDouble() * 100, ran.nextDouble() * 2, ran.nextDouble() * 10 );
				ls.add( spot );
			}
			spots.put( frame, ls );
		}
		spots.setVisible( true );
		final Model model = new Model();
		model.setSpots( spots, false );

		final Settings settings = new Settings();
//		settings.trackerFactory = new FastLAPTrackerFactory();
		settings.trackerFactory = new SparseLAPTrackerFactory();
		final Map< String, Object > ts = LAPUtils.getDefaultLAPSettingsMap();
		ts.put( KEY_ALLOW_TRACK_SPLITTING, true );
		ts.put( KEY_ALLOW_TRACK_MERGING, true );
		settings.trackerSettings = ts;

		final TrackMate trackmate = new TrackMate( model, settings );
		trackmate.setNumThreads( 1 );
		final long start = System.currentTimeMillis();
		final boolean ok = trackmate.execTracking();
		if ( !ok )
		{
			System.err.println( trackmate.getErrorMessage() );
		}
		final long end = System.currentTimeMillis();
		System.out.println( "Done in " + ( end - start ) + " ms." );
	}

	public static final void testSimil()
	{
		ImageJ.main( null );
		final File file = new File( "samples/VESICLE.xml" );
		final TmXmlReader reader = new TmXmlReader( file );
		final Model model = reader.getModel();
		final SpotCollection spots = model.getSpots();


		final Settings settings = new Settings();
		final Map< String, Object > ts = LAPUtils.getDefaultLAPSettingsMap();
		ts.put( KEY_ALLOW_TRACK_SPLITTING, true );
		ts.put( KEY_ALLOW_TRACK_MERGING, false );
		settings.trackerSettings = ts;


		/*
		 * SPARSE
		 */

		final Model m1 = new Model();
		m1.setSpots( spots, false );
		settings.trackerFactory = new SparseLAPTrackerFactory();
		final long start = System.currentTimeMillis();
		final TrackMate trackmate1 = new TrackMate( m1, settings );
		final boolean ok = trackmate1.execTracking();
		if ( !ok )
		{
			System.err.println( trackmate1.getErrorMessage() );
		}
		final long end = System.currentTimeMillis();
		System.out.println( "Done in " + ( end - start ) + " ms." );

		/*
		 * NON-SPARSE
		 */
		
		final Model m2 = new Model();
		m2.setSpots( spots, false );

		settings.trackerFactory = new FastLAPTrackerFactory();
		final long start2 = System.currentTimeMillis();
		final TrackMate trackmate2 = new TrackMate( m2, settings );
		final boolean ok2 = trackmate2.execTracking();
		if ( !ok2 )
		{
			System.err.println( trackmate2.getErrorMessage() );
		}
		final long end2 = System.currentTimeMillis();
		System.out.println( "Done in " + ( end2 - start2 ) + " ms." );


		/*
		 * TEST
		 */

		int good = 0;
		int bad = 0;
		for ( final Spot spot : spots.iterable( true ) )
		{
			final Set< DefaultWeightedEdge > edges = m1.getTrackModel().edgesOf( spot );
			for ( final DefaultWeightedEdge edge : edges )
			{
				Spot s1 = m1.getTrackModel().getEdgeSource( edge );
				if ( s1 == spot )
				{
					s1 = m1.getTrackModel().getEdgeTarget( edge );
				}

				if ( !m2.getTrackModel().containsEdge( spot, s1 ) )
				{
					System.out.println( "Could not find edge " + edge + " in 2nd model." );
					bad++;
				}
				else
				{
					good++;
				}
			}
		}
		System.out.println( String.format( "Found %d edges in the second model over the %d edges of the 1st model.", good, ( good + bad ) ) );// DEBUG

	}

	public static final void baseTest()
	{
		final File file = new File( "samples/FakeTracks.xml" );
		final TmXmlReader reader = new TmXmlReader( file );
		final Model model = reader.getModel();
		final SpotCollection spots = model.getSpots();
		final Settings s = new Settings();
		reader.readSettings( s, null, new TrackerProvider(), null, null, null );

		final Map< String, Object > settings = s.trackerSettings;
		settings.put( KEY_ALLOW_GAP_CLOSING, false );
		settings.put( KEY_ALLOW_TRACK_SPLITTING, false );
		settings.put( KEY_ALLOW_TRACK_MERGING, false );

		final SparseLAPTracker tracker = new SparseLAPTracker( spots, settings );
		if ( !tracker.checkInput() || !tracker.process() )
		{
			System.err.println( tracker.getErrorMessage() );
			return;
		}

		final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph = tracker.getResult();

		ImageJ.main( null );
		model.setTracks( graph, false );
		final HyperStackDisplayer view = new HyperStackDisplayer( model, new SelectionModel( model ) );
		view.render();

		final Map< String, Object > s2 = new HashMap< >();
		s2.put( KEY_ALLOW_GAP_CLOSING, true );
		s2.put( KEY_GAP_CLOSING_MAX_DISTANCE, settings.get( KEY_GAP_CLOSING_MAX_DISTANCE ) );
		s2.put( KEY_GAP_CLOSING_MAX_FRAME_GAP, settings.get( KEY_GAP_CLOSING_MAX_FRAME_GAP ) );
		s2.put( KEY_GAP_CLOSING_FEATURE_PENALTIES, settings.get( KEY_GAP_CLOSING_FEATURE_PENALTIES ) );
		// Splitting
		s2.put( KEY_ALLOW_TRACK_SPLITTING, true );
		s2.put( KEY_SPLITTING_MAX_DISTANCE, settings.get( KEY_SPLITTING_MAX_DISTANCE ) );
		s2.put( KEY_SPLITTING_FEATURE_PENALTIES, settings.get( KEY_SPLITTING_FEATURE_PENALTIES ) );
		// Merging
		s2.put( KEY_ALLOW_TRACK_MERGING, true );
		s2.put( KEY_MERGING_MAX_DISTANCE, settings.get( KEY_MERGING_MAX_DISTANCE ) );
		s2.put( KEY_MERGING_FEATURE_PENALTIES, settings.get( KEY_MERGING_FEATURE_PENALTIES ) );
		// Others
		s2.put( KEY_ALTERNATIVE_LINKING_COST_FACTOR, settings.get( KEY_ALTERNATIVE_LINKING_COST_FACTOR ) );
		s2.put( KEY_CUTOFF_PERCENTILE, settings.get( KEY_CUTOFF_PERCENTILE ) );

		final JaqamanSegmentCostMatrixCreator creator = new JaqamanSegmentCostMatrixCreator( graph, s2 );
		creator.setNumThreads();
		if ( !creator.checkInput() | !creator.process() )
		{
			System.out.println( creator.getErrorMessage() );
			return;
		}
		System.out.println( "Cost matrix creation done in " + creator.getProcessingTime() + " ms." );
		System.out.println( creator.getResult().toString( creator.getSourceList(), creator.getTargetList() ) );
	}

	public static void benchmark()
	{
		final Map< String, Object > settings = LAPUtils.getDefaultLAPSettingsMap();
		final int nFrames = 100;
		final int nSpotsPerFrame = 1000;
		final long seed = 1l;

		/*
		 * Create random model.
		 */

		final Random ran = new Random( seed );
		final SpotCollection spots = new SpotCollection();
		for ( int frame = 0; frame < nFrames; frame++ )
		{
			final Collection< Spot > ls = new ArrayList< >( nSpotsPerFrame );
			for ( int j = 0; j < nSpotsPerFrame; j++ )
			{
				final Spot spot = new Spot( ran.nextDouble() * 100, ran.nextDouble() * 100, ran.nextDouble() * 100, ran.nextDouble() * 2, ran.nextDouble() * 10 );
				ls.add( spot );
			}
			spots.put( frame, ls );
		}
		spots.setVisible( true );

//		final Model model = new Model();
//		model.setSpots( spots, false );

		/*
		 * Frame to frame tracker.
		 */

		final Map< String, Object > s1 = new HashMap< >();
		s1.put( KEY_LINKING_MAX_DISTANCE, 10d );
		s1.put( KEY_LINKING_FEATURE_PENALTIES, settings.get( KEY_LINKING_FEATURE_PENALTIES ) );
		s1.put( KEY_ALTERNATIVE_LINKING_COST_FACTOR, settings.get( KEY_ALTERNATIVE_LINKING_COST_FACTOR ) );
		final SparseLAPFrameToFrameTracker t1 = new SparseLAPFrameToFrameTracker( spots, s1 );
		t1.setNumThreads();
		t1.setLogger( Logger.DEFAULT_LOGGER );
		if ( !t1.checkInput() || !t1.process() )
		{
			System.out.println( t1.getErrorMessage() );
			return;
		}
		System.out.println( "Frame to frame tracking done in " + t1.getProcessingTime() + " ms." );

//		model.setTracks( t1.getResult(), false );

		/*
		 * Segment tracker
		 */

		final Map< String, Object > s2 = new HashMap< >();
		s2.put( KEY_ALLOW_GAP_CLOSING, true );
		s2.put( KEY_GAP_CLOSING_MAX_DISTANCE, settings.get( KEY_GAP_CLOSING_MAX_DISTANCE ) );
		s2.put( KEY_GAP_CLOSING_MAX_FRAME_GAP, settings.get( KEY_GAP_CLOSING_MAX_FRAME_GAP ) );
		s2.put( KEY_GAP_CLOSING_FEATURE_PENALTIES, settings.get( KEY_GAP_CLOSING_FEATURE_PENALTIES ) );
		// Splitting
		s2.put( KEY_ALLOW_TRACK_SPLITTING, true );
		s2.put( KEY_SPLITTING_MAX_DISTANCE, settings.get( KEY_SPLITTING_MAX_DISTANCE ) );
		s2.put( KEY_SPLITTING_FEATURE_PENALTIES, settings.get( KEY_SPLITTING_FEATURE_PENALTIES ) );
		// Merging
		s2.put( KEY_ALLOW_TRACK_MERGING, true );
		s2.put( KEY_MERGING_MAX_DISTANCE, settings.get( KEY_MERGING_MAX_DISTANCE ) );
		s2.put( KEY_MERGING_FEATURE_PENALTIES, settings.get( KEY_MERGING_FEATURE_PENALTIES ) );
		// Others
		s2.put( KEY_ALTERNATIVE_LINKING_COST_FACTOR, settings.get( KEY_ALTERNATIVE_LINKING_COST_FACTOR ) );
		s2.put( KEY_CUTOFF_PERCENTILE, settings.get( KEY_CUTOFF_PERCENTILE ) );

		final JaqamanSegmentCostMatrixCreator creator = new JaqamanSegmentCostMatrixCreator( t1.getResult(), s2 );
		creator.setNumThreads();
		if ( !creator.checkInput() | !creator.process() )
		{
			System.out.println( creator.getErrorMessage() );
			return;
		}
		System.out.println( "Cost matrix creation done in " + creator.getProcessingTime() + " ms." );
	}
}
