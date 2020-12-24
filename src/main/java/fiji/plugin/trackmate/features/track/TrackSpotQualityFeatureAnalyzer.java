package fiji.plugin.trackmate.features.track;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import net.imglib2.multithreading.SimpleMultiThreading;

@SuppressWarnings( "deprecation" )
@Plugin( type = TrackAnalyzer.class )
public class TrackSpotQualityFeatureAnalyzer implements TrackAnalyzer
{

	public static final String KEY = "TRACK_SPOT_QUALITY";

	public static final String TRACK_MEAN_QUALITY = "TRACK_MEAN_QUALITY";
	public static final List< String > FEATURES = Collections.singletonList( TRACK_MEAN_QUALITY );
	public static final Map< String, String > FEATURE_NAMES = Collections.singletonMap( TRACK_MEAN_QUALITY, "Track mean quality" );
	public static final Map< String, String > FEATURE_SHORT_NAMES = Collections.singletonMap( TRACK_MEAN_QUALITY, "Mean Q" );
	public static final Map< String, Dimension > FEATURE_DIMENSIONS = Collections.singletonMap( TRACK_MEAN_QUALITY, Dimension.QUALITY );
	public static final Map< String, Boolean > IS_INT = Collections.singletonMap( TRACK_MEAN_QUALITY, Boolean.FALSE );

	private int numThreads;

	private long processingTime;

	public TrackSpotQualityFeatureAnalyzer()
	{
		setNumThreads();
	}

	/*
	 * METHODS
	 */

	@Override
	public boolean isLocal()
	{
		return true;
	}

	@Override
	public void process( final Collection< Integer > trackIDs, final Model model )
	{

		if ( trackIDs.isEmpty() )
			return;

		final ArrayBlockingQueue< Integer > queue = new ArrayBlockingQueue<>( trackIDs.size(), false, trackIDs );
		final FeatureModel fm = model.getFeatureModel();

		final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );
		for ( int i = 0; i < threads.length; i++ )
		{
			threads[ i ] = new Thread( "TrackSpotFeatureAnalyzer thread " + i )
			{
				@Override
				public void run()
				{
					Integer trackID;
					while ( ( trackID = queue.poll() ) != null )
					{
						final Set< Spot > track = model.getTrackModel().trackSpots( trackID );
						final double mean = track
								.stream()
								.filter( Objects::nonNull )
								.mapToDouble( s -> s.getFeature( Spot.QUALITY ).doubleValue() )
								.average()
								.getAsDouble();
						fm.putTrackFeature( trackID, TRACK_MEAN_QUALITY, Double.valueOf( mean ) );
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
	public String getKey()
	{
		return KEY;
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
