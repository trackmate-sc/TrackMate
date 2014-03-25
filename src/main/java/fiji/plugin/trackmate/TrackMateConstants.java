package fiji.plugin.trackmate;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * Utility class for constants of {@link FeatureHolder}s
 *
 * @author Christian Dietz (University of Konstanz)
 */
public final class TrackMateConstants {

	/** numeric constants */
	public static Double ZERO = Double.valueOf( 0d );

	public static Double ONE = Double.valueOf( 1d );


	/** The name of the frame feature. */
	public static final String FRAME = "FRAME";

	/** The name of the frame feature. */
	public static final String RADIUS = "RADIUS";

	/** The name of the frame feature. */
	public static final String QUALITY = "QUALITY";

	/** The name of the frame feature. */
	public static final String VISIBILITY = "VISIBILITY";

	/** The name of the time feature. */
	public static final String POSITION_T = "POSITION_T";

	public static final String POSITION_X = "POSITION_X";

	public static final String POSITION_Y = "POSITION_Y";

	public static final String POSITION_Z = "POSITION_Z";

	/** The position features. */
	public final static String[] POSITION_FEATURES = new String[] { POSITION_X,
			POSITION_Y, POSITION_Z };


	/** The 7 privileged spot feature names. */
	public static Map< String, String > FEATURE_NAMES = new HashMap<String, String>();

	/** The 7 privileged spot feature short names. */
	public static Map< String, String > FEATURE_SHORT_NAMES = new HashMap<String, String>();

	public static Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap<String, Dimension>();

	public final static Collection< String > FEATURES = new ArrayList< String >( 7 );

	/** The 7 privileged spot feature isInt flags. */
	public final static Map< String, Boolean > IS_INT = new HashMap< String, Boolean >( 7 );

	static
	{
		FEATURES.add( QUALITY );
		FEATURES.add( POSITION_X );
		FEATURES.add( POSITION_Y );
		FEATURES.add( POSITION_Z );
		FEATURES.add( POSITION_T );
		FEATURES.add( FRAME );
		FEATURES.add( RADIUS );
		FEATURES.add( VISIBILITY );

		FEATURE_NAMES.put( POSITION_X, "X" );
		FEATURE_NAMES.put( POSITION_Y, "Y" );
		FEATURE_NAMES.put( POSITION_Z, "Z" );
		FEATURE_NAMES.put( POSITION_T, "T" );
		FEATURE_NAMES.put( FRAME, "Frame" );
		FEATURE_NAMES.put( RADIUS, "Radius" );
		FEATURE_NAMES.put( QUALITY, "Quality" );
		FEATURE_NAMES.put( VISIBILITY, "Visibility" );

		FEATURE_SHORT_NAMES.put( POSITION_X, "X" );
		FEATURE_SHORT_NAMES.put( POSITION_Y, "Y" );
		FEATURE_SHORT_NAMES.put( POSITION_Z, "Z" );
		FEATURE_SHORT_NAMES.put( POSITION_T, "T" );
		FEATURE_SHORT_NAMES.put( FRAME, "Frame" );
		FEATURE_SHORT_NAMES.put( RADIUS, "R" );
		FEATURE_SHORT_NAMES.put( QUALITY, "Quality" );
		FEATURE_SHORT_NAMES.put( VISIBILITY, "Visibility" );

		FEATURE_DIMENSIONS.put( POSITION_X, Dimension.POSITION );
		FEATURE_DIMENSIONS.put( POSITION_Y, Dimension.POSITION );
		FEATURE_DIMENSIONS.put( POSITION_Z, Dimension.POSITION );
		FEATURE_DIMENSIONS.put( POSITION_T, Dimension.TIME );
		FEATURE_DIMENSIONS.put( FRAME, Dimension.NONE );
		FEATURE_DIMENSIONS.put( RADIUS, Dimension.LENGTH );
		FEATURE_DIMENSIONS.put( QUALITY, Dimension.QUALITY );
		FEATURE_DIMENSIONS.put( VISIBILITY, Dimension.NONE );

		IS_INT.put( POSITION_X, Boolean.FALSE );
		IS_INT.put( POSITION_Y, Boolean.FALSE );
		IS_INT.put( POSITION_Z, Boolean.FALSE );
		IS_INT.put( POSITION_T, Boolean.FALSE );
		IS_INT.put( FRAME, Boolean.TRUE );
		IS_INT.put( RADIUS, Boolean.FALSE );
		IS_INT.put( QUALITY, Boolean.FALSE );
		IS_INT.put( VISIBILITY, Boolean.TRUE );
	}

}
