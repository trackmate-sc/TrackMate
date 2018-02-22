package fiji.plugin.trackmate.features.track;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import javax.swing.ImageIcon;

import net.imglib2.multithreading.SimpleMultiThreading;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;

@SuppressWarnings( "deprecation" )
@Plugin( type = TrackAnalyzer.class )
public class TrackDurationAnalyzer implements TrackAnalyzer
{

	public static final String KEY = "Track duration";

	public static final String TRACK_DURATION = "TRACK_DURATION";

	public static final String TRACK_START = "TRACK_START";

	public static final String TRACK_STOP = "TRACK_STOP";

	public static final String TRACK_DISPLACEMENT = "TRACK_DISPLACEMENT";

	public static final List< String > FEATURES = new ArrayList< >( 4 );

	public static final Map< String, String > FEATURE_NAMES = new HashMap< >( 4 );

	public static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap< >( 4 );

	public static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap< >( 4 );

	public static final Map< String, Boolean > IS_INT = new HashMap< >( 4 );

	static
	{
		FEATURES.add( TRACK_DURATION );
		FEATURES.add( TRACK_START );
		FEATURES.add( TRACK_STOP );
		FEATURES.add( TRACK_DISPLACEMENT );

		FEATURE_NAMES.put( TRACK_DURATION, "Duration of track" );
		FEATURE_NAMES.put( TRACK_START, "Track start" );
		FEATURE_NAMES.put( TRACK_STOP, "Track stop" );
		FEATURE_NAMES.put( TRACK_DISPLACEMENT, "Track displacement" );

		FEATURE_SHORT_NAMES.put( TRACK_DURATION, "Duration" );
		FEATURE_SHORT_NAMES.put( TRACK_START, "T start" );
		FEATURE_SHORT_NAMES.put( TRACK_STOP, "T stop" );
		FEATURE_SHORT_NAMES.put( TRACK_DISPLACEMENT, "Displacement" );

		FEATURE_DIMENSIONS.put( TRACK_DURATION, Dimension.TIME );
		FEATURE_DIMENSIONS.put( TRACK_START, Dimension.TIME );
		FEATURE_DIMENSIONS.put( TRACK_STOP, Dimension.TIME );
		FEATURE_DIMENSIONS.put( TRACK_DISPLACEMENT, Dimension.LENGTH );

		IS_INT.put( TRACK_DURATION, Boolean.FALSE );
		IS_INT.put( TRACK_START, Boolean.FALSE );
		IS_INT.put( TRACK_STOP, Boolean.FALSE );
		IS_INT.put( TRACK_DISPLACEMENT, Boolean.FALSE );
	}

	private int numThreads;

	private long processingTime;

	public TrackDurationAnalyzer()
	{
		setNumThreads();
	}

	@Override
	public boolean isLocal()
	{
		return true;
	}

	@Override
	public void process( final Collection< Integer > trackIDs, final Model model )
	{

		if ( trackIDs.isEmpty() ) { return; }

		final ArrayBlockingQueue< Integer > queue = new ArrayBlockingQueue< >( trackIDs.size(), false, trackIDs );
		final FeatureModel fm = model.getFeatureModel();

		final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );
		for ( int i = 0; i < threads.length; i++ )
		{
			threads[ i ] = new Thread( "TrackDurationAnalyzer thread " + i )
			{
				@Override
				public void run()
				{
					Integer trackID;
					while ( ( trackID = queue.poll() ) != null )
					{

						// I love brute force.
						final Set< Spot > track = model.getTrackModel().trackSpots( trackID );
						double minT = Double.POSITIVE_INFINITY;
						double maxT = Double.NEGATIVE_INFINITY;
						Double t;
						Spot startSpot = null;
						Spot endSpot = null;
						for ( final Spot spot : track )
						{
							t = spot.getFeature( Spot.POSITION_T );
							if ( t < minT )
							{
								minT = t;
								startSpot = spot;
							}
							if ( t > maxT )
							{
								maxT = t;
								endSpot = spot;
							}
						}
						if (null == startSpot || null == endSpot)
							continue;
						
						fm.putTrackFeature( trackID, TRACK_DURATION, ( maxT - minT ) );
						fm.putTrackFeature( trackID, TRACK_START, minT );
						fm.putTrackFeature( trackID, TRACK_STOP, maxT );
						fm.putTrackFeature( trackID, TRACK_DISPLACEMENT, Math.sqrt( startSpot.squareDistanceTo( endSpot ) ) );

					}
				}
			};
		}

		final long start = System.currentTimeMillis();
		SimpleMultiThreading.startAndJoin( threads );
		final long end = System.currentTimeMillis();
		processingTime = end - start;
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}

	@Override
	public void setNumThreads()
	{
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void setNumThreads( final int numThreads )
	{
		this.numThreads = numThreads;

	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public List< String > getFeatures()
	{
		return FEATURES;
	}

	@Override
	public Map< String, String > getFeatureShortNames()
	{
		return FEATURE_SHORT_NAMES;
	}

	@Override
	public Map< String, String > getFeatureNames()
	{
		return FEATURE_NAMES;
	}

	@Override
	public Map< String, Dimension > getFeatureDimensions()
	{
		return FEATURE_DIMENSIONS;
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public String getInfoText()
	{
		return null;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return KEY;
	}

	@Override
	public Map< String, Boolean > getIsIntFeature()
	{
		return IS_INT;
	}

	@Override
	public boolean isManualFeature()
	{
		return false;
	}
}
