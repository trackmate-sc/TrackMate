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
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

public class TrackSpeedStatisticsAnalyzerTest
{

	private static final int N_TRACKS = 10;

	private static final int DEPTH = 9;

	private Model model;

	private HashMap< Integer, Double > expectedVmean;

	private HashMap< Integer, Double > expectedVmax;

	@Before
	public void setUp()
	{
		model = new Model();
		model.beginUpdate();
		try
		{
			expectedVmean = new HashMap< >( N_TRACKS );
			expectedVmax = new HashMap< >( N_TRACKS );

			// Linear movement
			for ( int i = 1; i < N_TRACKS + 1; i++ )
			{
				Spot previous = null;

				final HashSet< Spot > track = new HashSet< >();
				for ( int j = 0; j <= DEPTH; j++ )
				{
					// We use deterministic locations
					final Spot spot = new Spot( j * i, i, i, 1d, -1d );
					spot.putFeature( Spot.POSITION_T, Double.valueOf( j ) );
					model.addSpotTo( spot, j );
					track.add( spot );
					if ( null != previous )
					{
						model.addEdge( previous, spot, 1 );
					}
					previous = spot;
				}

				final int key = model.getTrackModel().trackIDOf( previous );
				final double speed = i;
				expectedVmean.put( key, Double.valueOf( speed ) );
				expectedVmax.put( key, Double.valueOf( speed ) );
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
		final TrackSpeedStatisticsAnalyzer analyzer = new TrackSpeedStatisticsAnalyzer();
		analyzer.process( model.getTrackModel().trackIDs( true ), model );

		// Collect features
		for ( final Integer trackID : model.getTrackModel().trackIDs( true ) )
		{

			assertEquals( expectedVmax.get( trackID ), model.getFeatureModel().getTrackFeature( trackID, TrackSpeedStatisticsAnalyzer.TRACK_MAX_SPEED ) );
			assertEquals( expectedVmean.get( trackID ), model.getFeatureModel().getTrackFeature( trackID, TrackSpeedStatisticsAnalyzer.TRACK_MEAN_SPEED ) );

		}
	}

	@Test
	public final void testProcess2()
	{
		// Build parabolic model
		final Model model2 = new Model();
		model2.beginUpdate();
		try
		{

			// Parabolic movement
			Spot previous = null;
			final HashSet< Spot > track = new HashSet< >();
			for ( int j = 0; j <= DEPTH; j++ )
			{
				// We use deterministic locations
				final Spot spot = new Spot( j * j, 0d, 0d, 1d, -1d );
				spot.putFeature( Spot.POSITION_T, Double.valueOf( j ) );
				model2.addSpotTo( spot, j );
				track.add( spot );
				if ( null != previous )
				{
					model2.addEdge( previous, spot, 1 );
				}
				previous = spot;
			}

		}
		finally
		{
			model2.endUpdate();
		}

		// Expected values
		final double meanV = 9;
		final double stdV = 5.477225575051661;
		final double minV = 1;
		final double maxV = 17;
		final double medianV = 9;

		// Process model
		final TrackSpeedStatisticsAnalyzer analyzer = new TrackSpeedStatisticsAnalyzer();
		analyzer.process( model2.getTrackModel().trackIDs( true ), model2 );

		// Collect features
		for ( final Integer trackID : model2.getTrackModel().trackIDs( true ) )
		{

			assertEquals( meanV, model2.getFeatureModel().getTrackFeature( trackID, TrackSpeedStatisticsAnalyzer.TRACK_MEAN_SPEED ), Double.MIN_VALUE );
			assertEquals( stdV, model2.getFeatureModel().getTrackFeature( trackID, TrackSpeedStatisticsAnalyzer.TRACK_STD_SPEED ), 1e-6 );
			assertEquals( minV, model2.getFeatureModel().getTrackFeature( trackID, TrackSpeedStatisticsAnalyzer.TRACK_MIN_SPEED ), Double.MIN_VALUE );
			assertEquals( maxV, model2.getFeatureModel().getTrackFeature( trackID, TrackSpeedStatisticsAnalyzer.TRACK_MAX_SPEED ), Double.MIN_VALUE );
			assertEquals( medianV, model2.getFeatureModel().getTrackFeature( trackID, TrackSpeedStatisticsAnalyzer.TRACK_MEDIAN_SPEED ), Double.MIN_VALUE );

		}
	}

	@Test
	public final void testModelChanged()
	{
		// Copy old keys
		final HashSet< Integer > oldKeys = new HashSet< >( model.getTrackModel().trackIDs( true ) );

		// First analysis
		final TestTrackSpeedStatisticsAnalyzer analyzer = new TestTrackSpeedStatisticsAnalyzer();
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
	}

	@Test
	public final void testModelChanged2()
	{
		// Copy old keys
		final HashSet< Integer > oldKeys = new HashSet< >( model.getTrackModel().trackIDs( true ) );

		// First analysis
		final TestTrackSpeedStatisticsAnalyzer analyzer = new TestTrackSpeedStatisticsAnalyzer();
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

		// New change: remove the first spot on the first track - the new track
		// emerging should be re-analyzed
		final Integer firstKey = oldKeys.iterator().next();
		final TreeSet< Spot > sortedTrack = new TreeSet< >( Spot.frameComparator );
		sortedTrack.addAll( model.getTrackModel().trackSpots( firstKey ) );
		final Iterator< Spot > it = sortedTrack.iterator();
		final Spot firstSpot = it.next();
		final Spot secondSpot = it.next();

		model.beginUpdate();
		try
		{
			model.removeSpot( firstSpot );
		}
		finally
		{
			model.endUpdate();
		}

		// The analyzer must have done something:
		assertTrue( analyzer.hasBeenCalled );

		// Check the track IDs: must be of size 1 since we removed the first
		// spot of a track
		assertEquals( 1, analyzer.keys.size() );
		final Integer newKey = analyzer.keys.iterator().next();
		assertEquals( model.getTrackModel().trackIDOf( secondSpot ).longValue(), newKey.longValue() );

		// That did not affect speed values )was a constant speed track)
		assertEquals( expectedVmean.get( firstKey ).doubleValue(), model.getFeatureModel().getTrackFeature( newKey, TrackSpeedStatisticsAnalyzer.TRACK_MEAN_SPEED ).doubleValue(), Double.MIN_VALUE );
		assertEquals( expectedVmax.get( firstKey ).doubleValue(), model.getFeatureModel().getTrackFeature( newKey, TrackSpeedStatisticsAnalyzer.TRACK_MAX_SPEED ).doubleValue(), Double.MIN_VALUE );
	}

	@Test
	public final void testModelChanged3()
	{
		// Copy old keys
		final HashSet< Integer > oldKeys = new HashSet< >( model.getTrackModel().trackIDs( true ) );

		// First analysis
		final TestTrackSpeedStatisticsAnalyzer analyzer = new TestTrackSpeedStatisticsAnalyzer();
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

		// New change: we displace the last spot of first track, making the edge
		// faster
		final Integer firstKey = oldKeys.iterator().next();
		final TreeSet< Spot > sortedTrack = new TreeSet< >( Spot.frameComparator );
		sortedTrack.addAll( model.getTrackModel().trackSpots( firstKey ) );
		final Iterator< Spot > it = sortedTrack.descendingIterator();
		final Spot lastSpot = it.next();
		final Spot penultimateSpot = it.next();

		model.beginUpdate();
		try
		{
			lastSpot.putFeature( Spot.POSITION_X, 2 * lastSpot.getFeature( Spot.POSITION_X ) );
			model.updateFeatures( lastSpot );
		}
		finally
		{
			model.endUpdate();
		}

		// The analyzer must have done something:
		assertTrue( analyzer.hasBeenCalled );

		// Check the track IDs: must be of size 1 since we removed the first
		// spot of a track
		assertEquals( 1, analyzer.keys.size() );
		final Integer newKey = analyzer.keys.iterator().next();
		assertEquals( model.getTrackModel().trackIDOf( lastSpot ).longValue(), newKey.longValue() );

		// Track must be faster now
		assertTrue( expectedVmean.get( firstKey ).doubleValue() < model.getFeatureModel().getTrackFeature( newKey, TrackSpeedStatisticsAnalyzer.TRACK_MEAN_SPEED ).doubleValue() );
		// max speed is the one on this edge
		final double maxSpeed = lastSpot.getFeature( Spot.POSITION_X ).doubleValue() - penultimateSpot.getFeature( Spot.POSITION_X ).doubleValue();
		assertEquals( maxSpeed, model.getFeatureModel().getTrackFeature( newKey, TrackSpeedStatisticsAnalyzer.TRACK_MAX_SPEED ).doubleValue(), Double.MIN_VALUE );
	}

	/**
	 * Subclass of {@link TrackIndexAnalyzer} to monitor method calls.
	 */
	private static final class TestTrackSpeedStatisticsAnalyzer extends TrackSpeedStatisticsAnalyzer
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
