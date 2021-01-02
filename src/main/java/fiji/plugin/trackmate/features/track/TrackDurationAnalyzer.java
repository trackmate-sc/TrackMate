package fiji.plugin.trackmate.features.track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;

@Plugin( type = TrackAnalyzer.class )
public class TrackDurationAnalyzer extends AbstractTrackAnalyzer
{

	public static final String KEY = "Track duration";

	public static final String TRACK_DURATION = "TRACK_DURATION";
	public static final String TRACK_START = "TRACK_START";
	public static final String TRACK_STOP = "TRACK_STOP";
	public static final String TRACK_DISPLACEMENT = "TRACK_DISPLACEMENT";
	public static final List< String > FEATURES = new ArrayList<>( 4 );
	public static final Map< String, String > FEATURE_NAMES = new HashMap<>( 4 );
	public static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap<>( 4 );
	public static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap<>( 4 );
	public static final Map< String, Boolean > IS_INT = new HashMap<>( 4 );

	static
	{
		FEATURES.add( TRACK_DURATION );
		FEATURES.add( TRACK_START );
		FEATURES.add( TRACK_STOP );
		FEATURES.add( TRACK_DISPLACEMENT );

		FEATURE_NAMES.put( TRACK_DURATION, "Track duration" );
		FEATURE_NAMES.put( TRACK_START, "Track start" );
		FEATURE_NAMES.put( TRACK_STOP, "Track stop" );
		FEATURE_NAMES.put( TRACK_DISPLACEMENT, "Track displacement" );

		FEATURE_SHORT_NAMES.put( TRACK_DURATION, "Duration" );
		FEATURE_SHORT_NAMES.put( TRACK_START, "Track start" );
		FEATURE_SHORT_NAMES.put( TRACK_STOP, "Track stop" );
		FEATURE_SHORT_NAMES.put( TRACK_DISPLACEMENT, "Track disp." );

		FEATURE_DIMENSIONS.put( TRACK_DURATION, Dimension.TIME );
		FEATURE_DIMENSIONS.put( TRACK_START, Dimension.TIME );
		FEATURE_DIMENSIONS.put( TRACK_STOP, Dimension.TIME );
		FEATURE_DIMENSIONS.put( TRACK_DISPLACEMENT, Dimension.LENGTH );

		IS_INT.put( TRACK_DURATION, Boolean.FALSE );
		IS_INT.put( TRACK_START, Boolean.FALSE );
		IS_INT.put( TRACK_STOP, Boolean.FALSE );
		IS_INT.put( TRACK_DISPLACEMENT, Boolean.FALSE );
	}

	public TrackDurationAnalyzer()
	{
		super( KEY, KEY, FEATURES, FEATURE_NAMES, FEATURE_SHORT_NAMES, FEATURE_DIMENSIONS, IS_INT );
	}

	@Override
	protected void analyze( final Integer trackID, final Model model )
	{
		final FeatureModel fm = model.getFeatureModel();

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
		if ( null == startSpot || null == endSpot )
			return;

		fm.putTrackFeature( trackID, TRACK_DURATION, ( maxT - minT ) );
		fm.putTrackFeature( trackID, TRACK_START, minT );
		fm.putTrackFeature( trackID, TRACK_STOP, maxT );
		fm.putTrackFeature( trackID, TRACK_DISPLACEMENT, Math.sqrt( startSpot.squareDistanceTo( endSpot ) ) );
	}
}
