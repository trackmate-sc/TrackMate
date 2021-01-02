package fiji.plugin.trackmate.features.track;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;

@Plugin( type = TrackAnalyzer.class )
public class TrackSpotQualityFeatureAnalyzer extends AbstractTrackAnalyzer
{

	public static final String KEY = "Track quality";

	public static final String TRACK_MEAN_QUALITY = "TRACK_MEAN_QUALITY";
	public static final List< String > FEATURES = Collections.singletonList( TRACK_MEAN_QUALITY );
	public static final Map< String, String > FEATURE_NAMES = Collections.singletonMap( TRACK_MEAN_QUALITY, "Track mean quality" );
	public static final Map< String, String > FEATURE_SHORT_NAMES = Collections.singletonMap( TRACK_MEAN_QUALITY, "Mean Q" );
	public static final Map< String, Dimension > FEATURE_DIMENSIONS = Collections.singletonMap( TRACK_MEAN_QUALITY, Dimension.QUALITY );
	public static final Map< String, Boolean > IS_INT = Collections.singletonMap( TRACK_MEAN_QUALITY, Boolean.FALSE );

	public TrackSpotQualityFeatureAnalyzer()
	{
		super( KEY, KEY, FEATURES, FEATURE_NAMES, FEATURE_SHORT_NAMES, FEATURE_DIMENSIONS, IS_INT );
	}

	@Override
	protected void analyze( final Integer trackID, final Model model )
	{
		final FeatureModel fm = model.getFeatureModel();
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
