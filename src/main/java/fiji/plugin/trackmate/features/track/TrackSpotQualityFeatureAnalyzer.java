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
import net.imglib2.util.Util;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;

@SuppressWarnings( "deprecation" )
@Plugin( type = TrackAnalyzer.class )
public class TrackSpotQualityFeatureAnalyzer implements TrackAnalyzer
{

	/*
	 * CONSTANTS
	 */
	public static final String KEY = "TRACK_SPOT_QUALITY";

	public static final String TRACK_MEAN_QUALITY = "TRACK_MEAN_QUALITY";

	public static final String TRACK_MAX_QUALITY = "TRACK_MAX_QUALITY";

	public static final String TRACK_MIN_QUALITY = "TRACK_MIN_QUALITY";

	public static final String TRACK_MEDIAN_QUALITY = "TRACK_MEDIAN_QUALITY";

	public static final String TRACK_STD_QUALITY = "TRACK_STD_QUALITY";

	public static final List< String > FEATURES = new ArrayList< >( 5 );

	public static final Map< String, String > FEATURE_NAMES = new HashMap< >( 5 );

	public static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap< >( 5 );

	public static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap< >( 5 );

	public static final Map< String, Boolean > IS_INT = new HashMap< >( 5 );

	static
	{
		FEATURES.add( TRACK_MEAN_QUALITY );
		FEATURES.add( TRACK_MAX_QUALITY );
		FEATURES.add( TRACK_MIN_QUALITY );
		FEATURES.add( TRACK_MEDIAN_QUALITY );
		FEATURES.add( TRACK_STD_QUALITY );

		FEATURE_NAMES.put( TRACK_MEAN_QUALITY, "Mean quality" );
		FEATURE_NAMES.put( TRACK_MAX_QUALITY, "Maximal quality" );
		FEATURE_NAMES.put( TRACK_MIN_QUALITY, "Minimal quality" );
		FEATURE_NAMES.put( TRACK_MEDIAN_QUALITY, "Median quality" );
		FEATURE_NAMES.put( TRACK_STD_QUALITY, "Quality standard deviation" );

		FEATURE_SHORT_NAMES.put( TRACK_MEAN_QUALITY, "Mean Q" );
		FEATURE_SHORT_NAMES.put( TRACK_MAX_QUALITY, "Max Q" );
		FEATURE_SHORT_NAMES.put( TRACK_MIN_QUALITY, "Min Q" );
		FEATURE_SHORT_NAMES.put( TRACK_MEDIAN_QUALITY, "Median Q" );
		FEATURE_SHORT_NAMES.put( TRACK_STD_QUALITY, "Q std" );

		FEATURE_DIMENSIONS.put( TRACK_MEAN_QUALITY, Dimension.QUALITY );
		FEATURE_DIMENSIONS.put( TRACK_MAX_QUALITY, Dimension.QUALITY );
		FEATURE_DIMENSIONS.put( TRACK_MIN_QUALITY, Dimension.QUALITY );
		FEATURE_DIMENSIONS.put( TRACK_MEDIAN_QUALITY, Dimension.QUALITY );
		FEATURE_DIMENSIONS.put( TRACK_STD_QUALITY, Dimension.QUALITY );

		IS_INT.put( TRACK_MEAN_QUALITY, Boolean.FALSE );
		IS_INT.put( TRACK_MAX_QUALITY, Boolean.FALSE );
		IS_INT.put( TRACK_MIN_QUALITY, Boolean.FALSE );
		IS_INT.put( TRACK_MEDIAN_QUALITY, Boolean.FALSE );
		IS_INT.put( TRACK_STD_QUALITY, Boolean.FALSE );
	}

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

		if ( trackIDs.isEmpty() ) { return; }

		final ArrayBlockingQueue< Integer > queue = new ArrayBlockingQueue< >( trackIDs.size(), false, trackIDs );
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

						double sum = 0, sum2 = 0;

						// Others
						final double[] qualities = new double[ track.size() ];
						int n = 0;

						for ( final Spot spot : track )
						{
							final double val = spot.getFeature( Spot.QUALITY );

							// For median, min and max
							qualities[ n++ ] = val;
							// For variance and mean
							sum += val;
							sum2 += val * val;
						}

						Util.quicksort( qualities, 0, track.size() - 1 );
						final double median = qualities[ track.size() / 2 ];
						final double min = qualities[ 0 ];
						final double max = qualities[ track.size() - 1 ];
						final double mean = sum / track.size();
						final double mean2 = sum2 / track.size();
						final double variance = mean2 - mean * mean;

						fm.putTrackFeature( trackID, TRACK_MEDIAN_QUALITY, median );
						fm.putTrackFeature( trackID, TRACK_MIN_QUALITY, min );
						fm.putTrackFeature( trackID, TRACK_MAX_QUALITY, max );
						fm.putTrackFeature( trackID, TRACK_MEAN_QUALITY, mean );
						fm.putTrackFeature( trackID, TRACK_STD_QUALITY, Math.sqrt( variance ) );

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
