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

public class TrackLocationAnalyzerTest
{

	private static final int N_TRACKS = 10;

	private static final int DEPTH = 9;

	private Model model;

	private HashMap< Integer, Double > expectedX;

	private HashMap< Integer, Double > expectedY;

	private HashMap< Integer, Double > expectedZ;

	@Before
	public void setUp()
	{
		model = new Model();
		model.beginUpdate();
		try
		{
			expectedX = new HashMap< >( N_TRACKS );
			expectedY = new HashMap< >( N_TRACKS );
			expectedZ = new HashMap< >( N_TRACKS );

			for ( int i = 0; i < N_TRACKS; i++ )
			{

				Spot previous = null;

				final HashSet< Spot > track = new HashSet< >();
				for ( int j = 0; j <= DEPTH; j++ )
				{
					// We use deterministic locations
					final Spot spot = new Spot( j + i, j + i, j + i, 1d, -1d );
					model.addSpotTo( spot, j );
					track.add( spot );
					if ( null != previous )
					{
						model.addEdge( previous, spot, 1 );
					}
					previous = spot;
				}

				final int key = model.getTrackModel().trackIDOf( previous );
				final double mean = ( double ) DEPTH / 2 + i;
				expectedX.put( key, Double.valueOf( mean ) );
				expectedY.put( key, Double.valueOf( mean ) );
				expectedZ.put( key, Double.valueOf( mean ) );
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
		final TrackLocationAnalyzer analyzer = new TrackLocationAnalyzer();
		analyzer.process( model.getTrackModel().trackIDs( true ), model );

		// Collect features
		for ( final Integer trackID : model.getTrackModel().trackIDs( true ) )
		{

			assertEquals( expectedX.get( trackID ), model.getFeatureModel().getTrackFeature( trackID, TrackLocationAnalyzer.X_LOCATION ) );
			assertEquals( expectedY.get( trackID ), model.getFeatureModel().getTrackFeature( trackID, TrackLocationAnalyzer.Y_LOCATION ) );
			assertEquals( expectedZ.get( trackID ), model.getFeatureModel().getTrackFeature( trackID, TrackLocationAnalyzer.Z_LOCATION ) );

		}
	}

	@Test
	public final void testModelChanged()
	{
		// Copy old keys
		final HashSet< Integer > oldKeys = new HashSet< >( model.getTrackModel().trackIDs( true ) );

		// First analysis
		final TestTrackLocationAnalyzer analyzer = new TestTrackLocationAnalyzer();
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

		// New change: remove the first spot on the first track - it should be
		// re-analyzed
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

		// The location k features for this track must have changed by 0.5 with
		// respect to previous calculation
		assertEquals( expectedX.get( firstKey ) + 0.5d, model.getFeatureModel().getTrackFeature( newKey, TrackLocationAnalyzer.X_LOCATION ).doubleValue(), Double.MIN_VALUE );
		assertEquals( expectedY.get( firstKey ) + 0.5d, model.getFeatureModel().getTrackFeature( newKey, TrackLocationAnalyzer.Y_LOCATION ).doubleValue(), Double.MIN_VALUE );
		assertEquals( expectedZ.get( firstKey ) + 0.5d, model.getFeatureModel().getTrackFeature( newKey, TrackLocationAnalyzer.Z_LOCATION ).doubleValue(), Double.MIN_VALUE );
	}

	/**
	 * Subclass of {@link TrackIndexAnalyzer} to monitor method calls.
	 */
	private static final class TestTrackLocationAnalyzer extends TrackLocationAnalyzer
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
