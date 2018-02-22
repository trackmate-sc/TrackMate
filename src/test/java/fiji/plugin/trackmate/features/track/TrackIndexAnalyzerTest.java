/**
 *
 */
package fiji.plugin.trackmate.features.track;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.Spot;

/**
 * @author Jean-Yves Tinevez
 */
public class TrackIndexAnalyzerTest
{

	public static final int N_TRACKS = 10;

	private static final int DEPTH = 5;

	private Model model;

	/** Create a simple linear graph with {@value #N_TRACKS} tracks. */
	@Before
	public void setUp()
	{
		model = new Model();
		model.beginUpdate();
		try
		{
			for ( int i = 0; i < N_TRACKS; i++ )
			{
				Spot previous = null;
				for ( int j = 0; j < DEPTH; j++ )
				{
					final Spot spot = new Spot( 0d, 0d, 0d, 1d, -1d );
					model.addSpotTo( spot, j );
					if ( null != previous )
					{
						model.addEdge( previous, spot, 1 );
					}
					previous = spot;
				}
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
		// Compute track index
		final Set< Integer > trackIDs = model.getTrackModel().trackIDs( true );
		final TrackIndexAnalyzer analyzer = new TrackIndexAnalyzer();
		analyzer.process( trackIDs, model );

		// Collect track indices
		final ArrayList< Integer > trackIndices = new ArrayList< >( trackIDs.size() );
		for ( final Integer trackID : trackIDs )
		{
			trackIndices.add( model.getFeatureModel().getTrackFeature( trackID, TrackIndexAnalyzer.TRACK_INDEX ).intValue() );
		}

		// Check values: they must be 0, 1, 2, ... in the order of the filtered
		// track IDs (which reflect track names order)
		for ( int i = 0; i < N_TRACKS; i++ )
		{
			assertEquals( "Bad track index:", i, trackIndices.get( i ).longValue() );
		}
	}

	@Test
	public final void testModelChanged()
	{

		// Compute track index
		Set< Integer > trackIDs = model.getTrackModel().trackIDs( true );
		final TestTrackIndexAnalyzer analyzer = new TestTrackIndexAnalyzer();
		analyzer.process( trackIDs, model );
		assertTrue( analyzer.hasBeenCalled );

		// Collect track indices
		final ArrayList< Integer > trackIndices = new ArrayList< >( trackIDs.size() );
		for ( final Integer trackID : trackIDs )
		{
			trackIndices.add( model.getFeatureModel().getTrackFeature( trackID, TrackIndexAnalyzer.TRACK_INDEX ).intValue() );
		}

		// Reset analyzer
		analyzer.hasBeenCalled = false;

		// Prepare listener -> forward to analyzer
		final ModelChangeListener listener = new ModelChangeListener()
		{
			@Override
			public void modelChanged( final ModelChangeEvent event )
			{
				if ( analyzer.isLocal() )
				{
					analyzer.process( event.getTrackUpdated(), model );
				}
				else
				{
					analyzer.process( model.getTrackModel().trackIDs( true ), model );
				}
			}
		};

		/*
		 * Modify the model a first time: We attach a new spot to an existing
		 * track. It must not modify the track indices, nor generate a call to
		 * recalculate them.
		 */
		model.addModelChangeListener( listener );
		model.beginUpdate();
		try
		{
			final Spot targetSpot = model.getSpots().iterator( 0, true ).next();
			final Spot newSpot = model.addSpotTo( new Spot( 0d, 0d, 0d, 1d, -1d ), 1 );
			model.addEdge( targetSpot, newSpot, 1 );
		}
		finally
		{
			model.endUpdate();
		}

		// Reset analyzer
		analyzer.hasBeenCalled = false;

		/*
		 * Second modification: we create a new track by cutting one track in
		 * the middle
		 */
		model.addModelChangeListener( listener );
		model.beginUpdate();
		try
		{
			final Spot targetSpot = model.getSpots().iterator( DEPTH / 2, true ).next();
			model.removeSpot( targetSpot );
		}
		finally
		{
			model.endUpdate();
		}

		// Process method must have been called
		assertTrue( analyzer.hasBeenCalled );

		// There must N_TRACKS+1 indices now
		trackIDs = model.getTrackModel().trackIDs( true );
		assertEquals( ( long ) N_TRACKS + 1, trackIDs.size() );

		// With correct indices
		final Iterator< Integer > it = trackIDs.iterator();
		for ( int i = 0; i < trackIDs.size(); i++ )
		{
			assertEquals( i, model.getFeatureModel().getTrackFeature( it.next(), TrackIndexAnalyzer.TRACK_INDEX ).longValue() );
		}
		// FAILS BECAUSE TRANCK INDEX IS A GLOBAL TRACK ANALYZER AND NEEDS TO
		// RECOMPUTE FOR THE WHOLE MODEL
		// C:EST LA VIE

	}

	/**
	 * Subclass of {@link TrackIndexAnalyzer} to monitor method calls.
	 */
	private static final class TestTrackIndexAnalyzer extends TrackIndexAnalyzer
	{

		private boolean hasBeenCalled = false;

		@Override
		public void process( final Collection< Integer > trackIDs, final Model model )
		{
			hasBeenCalled = true;
			super.process( trackIDs, model );
		}
	}

}
