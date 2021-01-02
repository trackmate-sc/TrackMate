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
public class TrackLocationAnalyzer extends AbstractTrackAnalyzer
{

	public static final String KEY = "Track location";

	public static final String X_LOCATION = "TRACK_X_LOCATION";
	public static final String Y_LOCATION = "TRACK_Y_LOCATION";
	public static final String Z_LOCATION = "TRACK_Z_LOCATION";
	public static final List< String > FEATURES = new ArrayList<>( 3 );
	public static final Map< String, String > FEATURE_NAMES = new HashMap<>( 3 );
	public static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap<>( 3 );
	public static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap<>( 3 );
	public static final Map< String, Boolean > IS_INT = new HashMap<>( 3 );

	static
	{
		FEATURES.add( X_LOCATION );
		FEATURES.add( Y_LOCATION );
		FEATURES.add( Z_LOCATION );

		FEATURE_NAMES.put( X_LOCATION, "Track mean X" );
		FEATURE_NAMES.put( Y_LOCATION, "Track mean Y" );
		FEATURE_NAMES.put( Z_LOCATION, "Track mean Z" );

		FEATURE_SHORT_NAMES.put( X_LOCATION, "Track X" );
		FEATURE_SHORT_NAMES.put( Y_LOCATION, "Track Y" );
		FEATURE_SHORT_NAMES.put( Z_LOCATION, "Track Z" );

		FEATURE_DIMENSIONS.put( X_LOCATION, Dimension.POSITION );
		FEATURE_DIMENSIONS.put( Y_LOCATION, Dimension.POSITION );
		FEATURE_DIMENSIONS.put( Z_LOCATION, Dimension.POSITION );

		IS_INT.put( X_LOCATION, Boolean.FALSE );
		IS_INT.put( Y_LOCATION, Boolean.FALSE );
		IS_INT.put( Z_LOCATION, Boolean.FALSE );
	}

	public TrackLocationAnalyzer()
	{
		super( KEY, KEY, FEATURES, FEATURE_NAMES, FEATURE_SHORT_NAMES, FEATURE_DIMENSIONS, IS_INT );
	}

	@Override
	protected void analyze( final Integer trackID, final Model model )
	{
		final FeatureModel fm = model.getFeatureModel();
		final Set< Spot > track = model.getTrackModel().trackSpots( trackID );

		double x = 0.;
		double y = 0.;
		double z = 0.;

		for ( final Spot spot : track )
		{
			x += spot.getFeature( Spot.POSITION_X );
			y += spot.getFeature( Spot.POSITION_Y );
			z += spot.getFeature( Spot.POSITION_Z );
		}
		final int nspots = track.size();
		x /= nspots;
		y /= nspots;
		z /= nspots;

		fm.putTrackFeature( trackID, X_LOCATION, x );
		fm.putTrackFeature( trackID, Y_LOCATION, y );
		fm.putTrackFeature( trackID, Z_LOCATION, z );
	}
}
