package fiji.plugin.trackmate.features.track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.TMUtils;
import net.imglib2.util.Util;

@Plugin( type = TrackAnalyzer.class )
public class TrackSpeedStatisticsAnalyzer extends AbstractTrackAnalyzer
{

	public static final String KEY = "Track speed";

	public static final String TRACK_MEAN_SPEED = "TRACK_MEAN_SPEED";
	public static final String TRACK_MAX_SPEED = "TRACK_MAX_SPEED";
	public static final String TRACK_MIN_SPEED = "TRACK_MIN_SPEED";
	public static final String TRACK_MEDIAN_SPEED = "TRACK_MEDIAN_SPEED";
	public static final String TRACK_STD_SPEED = "TRACK_STD_SPEED";
	public static final List< String > FEATURES = new ArrayList<>( 5 );
	public static final Map< String, String > FEATURE_NAMES = new HashMap<>( 5 );
	public static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap<>( 5 );
	public static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap<>( 5 );
	public static final Map< String, Boolean > IS_INT = new HashMap<>( 5 );

	static
	{
		FEATURES.add( TRACK_MEAN_SPEED );
		FEATURES.add( TRACK_MAX_SPEED );
		FEATURES.add( TRACK_MIN_SPEED );
		FEATURES.add( TRACK_MEDIAN_SPEED );
		FEATURES.add( TRACK_STD_SPEED );

		FEATURE_NAMES.put( TRACK_MEAN_SPEED, "Track mean speed" );
		FEATURE_NAMES.put( TRACK_STD_SPEED, "Track std speed" );
		FEATURE_NAMES.put( TRACK_MAX_SPEED, "Track max speed" );
		FEATURE_NAMES.put( TRACK_MIN_SPEED, "Track min speed" );
		FEATURE_NAMES.put( TRACK_MEDIAN_SPEED, "Track median speed" );

		FEATURE_SHORT_NAMES.put( TRACK_MEAN_SPEED, "Mean sp." );
		FEATURE_SHORT_NAMES.put( TRACK_MAX_SPEED, "Max speed" );
		FEATURE_SHORT_NAMES.put( TRACK_MIN_SPEED, "Min speed" );
		FEATURE_SHORT_NAMES.put( TRACK_MEDIAN_SPEED, "Med. speed" );
		FEATURE_SHORT_NAMES.put( TRACK_STD_SPEED, "Std speed" );

		FEATURE_DIMENSIONS.put( TRACK_MEAN_SPEED, Dimension.VELOCITY );
		FEATURE_DIMENSIONS.put( TRACK_MAX_SPEED, Dimension.VELOCITY );
		FEATURE_DIMENSIONS.put( TRACK_MIN_SPEED, Dimension.VELOCITY );
		FEATURE_DIMENSIONS.put( TRACK_MEDIAN_SPEED, Dimension.VELOCITY );
		FEATURE_DIMENSIONS.put( TRACK_STD_SPEED, Dimension.VELOCITY );

		IS_INT.put( TRACK_MEAN_SPEED, Boolean.FALSE );
		IS_INT.put( TRACK_MAX_SPEED, Boolean.FALSE );
		IS_INT.put( TRACK_MIN_SPEED, Boolean.FALSE );
		IS_INT.put( TRACK_MEDIAN_SPEED, Boolean.FALSE );
		IS_INT.put( TRACK_STD_SPEED, Boolean.FALSE );
	}

	public TrackSpeedStatisticsAnalyzer()
	{
		super( KEY, KEY, FEATURES, FEATURE_NAMES, FEATURE_SHORT_NAMES, FEATURE_DIMENSIONS, IS_INT );
	}

	@Override
	protected void analyze( final Integer trackID, final Model model )
	{
		final FeatureModel fm = model.getFeatureModel();
		final Set< DefaultWeightedEdge > track = model.getTrackModel().trackEdges( trackID );
		final double[] speeds = new double[ track.size() ];
		int n = 0;
		for ( final DefaultWeightedEdge edge : track )
		{
			final Spot source = model.getTrackModel().getEdgeSource( edge );
			final Spot target = model.getTrackModel().getEdgeTarget( edge );
			final double d2 = source.squareDistanceTo( target );
			final double dt = source.diffTo( target, Spot.POSITION_T );
			final double val = Math.sqrt( d2 ) / Math.abs( dt );
			speeds[ n++ ] = val;
		}

		Util.quicksort( speeds, 0, track.size() - 1 );
		final double median = speeds[ track.size() / 2 ];
		final double min = speeds[ 0 ];
		final double max = speeds[ track.size() - 1 ];
		final double mean = Util.average( speeds );
		final double std = TMUtils.standardDeviation( speeds );

		fm.putTrackFeature( trackID, TRACK_MEDIAN_SPEED, median );
		fm.putTrackFeature( trackID, TRACK_MIN_SPEED, min );
		fm.putTrackFeature( trackID, TRACK_MAX_SPEED, max );
		fm.putTrackFeature( trackID, TRACK_MEAN_SPEED, mean );
		fm.putTrackFeature( trackID, TRACK_STD_SPEED, std );
	}
}
