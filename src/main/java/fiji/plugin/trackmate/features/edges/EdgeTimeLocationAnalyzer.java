package fiji.plugin.trackmate.features.edges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;

@Plugin( type = EdgeAnalyzer.class )
public class EdgeTimeLocationAnalyzer extends AbstractEdgeAnalyzer
{

	public static final String KEY = "Edge location";

	public static final String TIME = "EDGE_TIME";
	public static final String X_LOCATION = "EDGE_X_LOCATION";
	public static final String Y_LOCATION = "EDGE_Y_LOCATION";
	public static final String Z_LOCATION = "EDGE_Z_LOCATION";
	public static final List< String > FEATURES = new ArrayList<>( 4 );
	public static final Map< String, String > FEATURE_NAMES = new HashMap<>( 4 );
	public static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap<>( 4 );
	public static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap<>( 4 );
	public static final Map< String, Boolean > IS_INT = new HashMap<>( 4 );

	static
	{
		FEATURES.add( TIME );
		FEATURES.add( X_LOCATION );
		FEATURES.add( Y_LOCATION );
		FEATURES.add( Z_LOCATION );

		FEATURE_NAMES.put( TIME, "Edge time" );
		FEATURE_NAMES.put( X_LOCATION, "Edge X" );
		FEATURE_NAMES.put( Y_LOCATION, "Edge Y" );
		FEATURE_NAMES.put( Z_LOCATION, "Edge Z" );

		FEATURE_SHORT_NAMES.put( TIME, "Edge T" );
		FEATURE_SHORT_NAMES.put( X_LOCATION, "Edge X" );
		FEATURE_SHORT_NAMES.put( Y_LOCATION, "Edge Y" );
		FEATURE_SHORT_NAMES.put( Z_LOCATION, "Edge Z" );

		FEATURE_DIMENSIONS.put( TIME, Dimension.TIME );
		FEATURE_DIMENSIONS.put( X_LOCATION, Dimension.POSITION );
		FEATURE_DIMENSIONS.put( Y_LOCATION, Dimension.POSITION );
		FEATURE_DIMENSIONS.put( Z_LOCATION, Dimension.POSITION );

		IS_INT.put( TIME, Boolean.FALSE );
		IS_INT.put( X_LOCATION, Boolean.FALSE );
		IS_INT.put( Y_LOCATION, Boolean.FALSE );
		IS_INT.put( Z_LOCATION, Boolean.FALSE );
	}

	public EdgeTimeLocationAnalyzer()
	{
		super( KEY, KEY, FEATURES, FEATURE_NAMES, FEATURE_SHORT_NAMES, FEATURE_DIMENSIONS, IS_INT );
	}

	@Override
	protected void analyze( final DefaultWeightedEdge edge, final Model model )
	{
		final FeatureModel featureModel = model.getFeatureModel();
		final Spot source = model.getTrackModel().getEdgeSource( edge );
		final Spot target = model.getTrackModel().getEdgeTarget( edge );

		final double x = 0.5 * ( source.getFeature( Spot.POSITION_X ) + target.getFeature( Spot.POSITION_X ) );
		final double y = 0.5 * ( source.getFeature( Spot.POSITION_Y ) + target.getFeature( Spot.POSITION_Y ) );
		final double z = 0.5 * ( source.getFeature( Spot.POSITION_Z ) + target.getFeature( Spot.POSITION_Z ) );
		final double t = 0.5 * ( source.getFeature( Spot.POSITION_T ) + target.getFeature( Spot.POSITION_T ) );

		featureModel.putEdgeFeature( edge, TIME, t );
		featureModel.putEdgeFeature( edge, X_LOCATION, x );
		featureModel.putEdgeFeature( edge, Y_LOCATION, y );
		featureModel.putEdgeFeature( edge, Z_LOCATION, z );
	}
}
