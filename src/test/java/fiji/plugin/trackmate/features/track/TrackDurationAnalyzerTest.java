package fiji.plugin.trackmate.features.track;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.Spot;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

public class TrackDurationAnalyzerTest
{

	private static final int N_TRACKS = 10;

	private static final int DEPTH = 9; // must be at least 6 to avoid tracks
										// too shorts - may make this test fail
										// sometimes

	private Model model;

	private HashMap< Integer, Double > expectedDuration;

	private HashMap< Integer, Double > expectedStart;

	private HashMap< Integer, Double > expectedStop;

	private HashMap< Integer, Double > expectedDisplacement;

	private int key;

	@Before
	public void setUp()
	{
		final Random ran = new Random();
		model = new Model();
		model.beginUpdate();
		try
		{

			expectedDuration = new HashMap< >( N_TRACKS );
			expectedStart = new HashMap< >( N_TRACKS );
			expectedStop = new HashMap< >( N_TRACKS );
			expectedDisplacement = new HashMap< >( N_TRACKS );

			for ( int i = 0; i < N_TRACKS; i++ )
			{
				Spot previous = null;

				final int start = ran.nextInt( DEPTH );
				final int stop = start + DEPTH + ran.nextInt( DEPTH );
				final int duration = stop - start;
				final double displacement = ran.nextDouble();

				final HashSet< Spot > track = new HashSet< >();
				for ( int j = start; j <= stop; j++ )
				{
					final Spot spot = new Spot( 0d, 0d, 0d, 1d, -1d );
					spot.putFeature( Spot.POSITION_T, Double.valueOf( j ) );
					model.addSpotTo( spot, j );
					track.add( spot );
					if ( null != previous )
						model.addEdge( previous, spot, 1 );

					previous = spot;
				}
				if ( null == previous )
					continue;

				previous.putFeature( Spot.POSITION_X, displacement );
				key = model.getTrackModel().trackIDOf( previous );
				expectedDuration.put( key, Double.valueOf( duration ) );
				expectedStart.put( key, Double.valueOf( start ) );
				expectedStop.put( key, Double.valueOf( stop ) );
				expectedDisplacement.put( key, displacement );
			}

		}
		finally
		{
			model.endUpdate();
		}
	}

	@Test
	public final void testProcess()
	{
		// Process model
		final TrackDurationAnalyzer analyzer = new TrackDurationAnalyzer();
		analyzer.process( model.getTrackModel().trackIDs( true ), model );

		// Collect features
		for ( final Integer trackID : model.getTrackModel().trackIDs( true ) )
		{

			assertEquals( expectedDisplacement.get( trackID ), model.getFeatureModel().getTrackFeature( trackID, TrackDurationAnalyzer.TRACK_DISPLACEMENT ) );
			assertEquals( expectedStart.get( trackID ), model.getFeatureModel().getTrackFeature( trackID, TrackDurationAnalyzer.TRACK_START ) );
			assertEquals( expectedStop.get( trackID ), model.getFeatureModel().getTrackFeature( trackID, TrackDurationAnalyzer.TRACK_STOP ) );
			assertEquals( expectedDuration.get( trackID ), model.getFeatureModel().getTrackFeature( trackID, TrackDurationAnalyzer.TRACK_DURATION ) );

		}
	}

	@Test
	public final void testModelChanged()
	{
		// Copy old keys
		final HashSet< Integer > oldKeys = new HashSet< >( model.getTrackModel().trackIDs( true ) );

		// First analysis
		final TestTrackDurationAnalyzer analyzer = new TestTrackDurationAnalyzer();
		analyzer.process( oldKeys, model );

		// Reset analyzer
		analyzer.hasBeenCalled = false;
		analyzer.keys = null;

		// Prepare listener for model change
		final ModelChangeListener listener = new ModelChangeListener()
		{
			@Override
			public void modelChanged( final ModelChangeEvent event )
			{
				analyzer.process( event.getTrackUpdated(), model );
			}
		};
		model.addModelChangeListener( listener );

		// Add a new track to the model - the old tracks should not be affected
		model.beginUpdate();
		try
		{
			final Spot spot1 = model.addSpotTo( new Spot( 0d, 0d, 0d, 1d, -1d ), 0 );
			spot1.putFeature( Spot.POSITION_T, 0d );
			final Spot spot2 = model.addSpotTo( new Spot( 0d, 0d, 0d, 1d, -1d ), 1 );
			spot2.putFeature( Spot.POSITION_T, 1d );
			model.addEdge( spot1, spot2, 1 );

		}
		finally
		{
			model.endUpdate();
		}

		// The analyzer must have done something:
		assertTrue( analyzer.hasBeenCalled );

		// Check the track IDs the analyzer received - none of the old keys must
		// be in it
		for ( final Integer calledKey : analyzer.keys )
		{
			if ( oldKeys.contains( calledKey ) )
			{
				fail( "Track with ID " + calledKey + " should not have been re-analyzed." );
			}
		}

		// Reset analyzer
		analyzer.hasBeenCalled = false;
		analyzer.keys = null;

		// New change: graft a new spot on the first track - it should be
		// re-analyzed
		final Integer firstKey = oldKeys.iterator().next();
		final TreeSet< Spot > sortedTrack = new TreeSet< >( Spot.frameComparator );
		sortedTrack.addAll( model.getTrackModel().trackSpots( firstKey ) );
		final Spot firstSpot = sortedTrack.first();
		Spot newSpot = null;
		final int firstFrame = firstSpot.getFeature( Spot.FRAME ).intValue();
		model.beginUpdate();
		try
		{
			newSpot = model.addSpotTo( new Spot( 0d, 0d, 0d, 1d, -1d ), firstFrame + 1 );
			newSpot.putFeature( Spot.POSITION_T, Double.valueOf( firstFrame + 1 ) );
			model.addEdge( firstSpot, newSpot, 1 );
		}
		finally
		{
			model.endUpdate();
		}

		// The analyzer must have done something:
		assertTrue( analyzer.hasBeenCalled );

		// Check the track IDs: must be of size 1, and they to the track with
		// firstSpot and newSpot in it
		assertEquals( 1, analyzer.keys.size() );
		// The ID of the modified track has changed
		final Integer newKey = analyzer.keys.iterator().next();
		assertTrue( model.getTrackModel().trackSpots( newKey ).contains( firstSpot ) );
		assertTrue( model.getTrackModel().trackSpots( newKey ).contains( newSpot ) );

		// But the track features for this track should not have changed: the
		// grafting did not affect
		// start nor stop nor displacement
		assertEquals( expectedDisplacement.get( firstKey ), model.getFeatureModel().getTrackFeature( newKey, TrackDurationAnalyzer.TRACK_DISPLACEMENT ) );
		assertEquals( expectedStart.get( firstKey ), model.getFeatureModel().getTrackFeature( newKey, TrackDurationAnalyzer.TRACK_START ) );
		assertEquals( expectedStop.get( firstKey ), model.getFeatureModel().getTrackFeature( newKey, TrackDurationAnalyzer.TRACK_STOP ) );
		assertEquals( expectedDuration.get( firstKey ), model.getFeatureModel().getTrackFeature( newKey, TrackDurationAnalyzer.TRACK_DURATION ) );

	}

	@Test
	public final void testModelChanged2()
	{
		// Copy old keys
		final HashSet< Integer > oldKeys = new HashSet< >( model.getTrackModel().trackIDs( true ) );

		// First analysis
		final TestTrackDurationAnalyzer analyzer = new TestTrackDurationAnalyzer();
		analyzer.process( oldKeys, model );

		// Reset analyzer
		analyzer.hasBeenCalled = false;
		analyzer.keys = null;

		// Prepare listener for model change
		final ModelChangeListener listener = new ModelChangeListener()
		{
			@Override
			public void modelChanged( final ModelChangeEvent event )
			{
				analyzer.process( event.getTrackUpdated(), model );
			}
		};
		model.addModelChangeListener( listener );

		// Get a track
		final Integer aKey = model.getTrackModel().trackIDs( true ).iterator().next();

		// Store feature for later
		final double oldVal = model.getFeatureModel().getTrackFeature( aKey, TrackDurationAnalyzer.TRACK_DURATION );
		final double increment = 10d;

		// Move the last spot of a track further in time to change duration and
		// stop feature
		final TreeSet< Spot > sortedTrack = new TreeSet< >( Spot.frameComparator );
		sortedTrack.addAll( model.getTrackModel().trackSpots( aKey ) );
		final Spot aspot = sortedTrack.last();

		// Move a spot in time
		model.beginUpdate();
		try
		{
			aspot.putFeature( Spot.POSITION_T, aspot.getFeature( Spot.POSITION_T ) + increment );
			model.updateFeatures( aspot );
		}
		finally
		{
			model.endUpdate();
		}

		// The analyzer must have done something:
		assertTrue( analyzer.hasBeenCalled );

		// Check the track IDs: must be of size 1, be the one of the track we
		// modified
		assertEquals( 1, analyzer.keys.size() );
		assertEquals( aKey.longValue(), analyzer.keys.iterator().next().longValue() );

		// Check that the feature has been updated properly
		assertEquals( oldVal + increment, model.getFeatureModel().getTrackFeature( aKey, TrackDurationAnalyzer.TRACK_DURATION ).doubleValue(), Double.MIN_VALUE );
	}

	@Test
	public final void testModelChanged3()
	{
		// Copy old keys
		final HashSet< Integer > oldKeys = new HashSet< >( model.getTrackModel().trackIDs( true ) );

		// First analysis
		final TestTrackDurationAnalyzer analyzer = new TestTrackDurationAnalyzer();
		analyzer.process( oldKeys, model );

		// Reset analyzer
		analyzer.hasBeenCalled = false;
		analyzer.keys = null;

		// Prepare listener for model change
		final ModelChangeListener listener = new ModelChangeListener()
		{
			@Override
			public void modelChanged( final ModelChangeEvent event )
			{
				analyzer.process( event.getTrackUpdated(), model );
			}
		};
		model.addModelChangeListener( listener );

		// Get its middle spot
		final TreeSet< Spot > sortedTrack = new TreeSet< >( Spot.frameComparator );
		sortedTrack.addAll( model.getTrackModel().trackSpots( key ) );
		Spot aspot = null;
		final Iterator< Spot > it = sortedTrack.iterator();
		for ( int i = 0; i < sortedTrack.size() / 2; i++ )
		{
			aspot = it.next();
		}
		// Store first and last spot for later
		final Spot firstSpot = sortedTrack.first();
		final Spot lastSpot = sortedTrack.last();

		// Remove it
		model.beginUpdate();
		try
		{
			model.removeSpot( aspot );
		}
		finally
		{
			model.endUpdate();
		}

		// The analyzer must have done something:
		assertTrue( analyzer.hasBeenCalled );

		// Check the track IDs: must be of size 2: the two track that were
		// created from the removal
		assertEquals( 2, analyzer.keys.size() );
		for ( final Integer targetKey : analyzer.keys )
		{
			assertTrue( targetKey.equals( model.getTrackModel().trackIDOf( firstSpot ) ) || targetKey.equals( model.getTrackModel().trackIDOf( lastSpot ) ) );
		}

	}

	/**
	 * Subclass of {@link TrackIndexAnalyzer} to monitor method calls.
	 */
	private static final class TestTrackDurationAnalyzer extends TrackDurationAnalyzer
	{

		private boolean hasBeenCalled = false;

		private Collection< Integer > keys;

		@Override
		public void process( final Collection< Integer > trackIDs, final Model model )
		{
			hasBeenCalled = true;
			keys = trackIDs;
			super.process( trackIDs, model );
		}
	}

}
