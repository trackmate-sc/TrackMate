package fiji.plugin.trackmate.io.json;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;

/**
 * Reads and writes the feature declarations of a {@link FeatureModel} to and
 * from a JSON tree. Only the declarations (feature keys, names, short names,
 * dimensions and integer flags) are serialized; the feature *values* are not,
 * as they are stored in the TrackMate objects themselves.
 */
public class FeatureModelIO
{
	private static Gson getGson()
	{
		return new GsonBuilder()
				.setPrettyPrinting()
				.create();
	}

	public static JsonElement toJsonTree( final FeatureModel featureModel )
	{
		return getGson().toJsonTree( featureModel );
	}

	/**
	 * Populates the specified {@link FeatureModel} with the declarations found
	 * in the specified map, as written by {@link #toJsonTree(FeatureModel)}.
	 * Missing or malformed sections are skipped silently.
	 *
	 * @param featureDeclarations
	 *            the map to read from. Expected to be the parsed JSON object
	 *            stored under the {@code featureDeclarations} key of the
	 *            TrackMate extra metadata.
	 * @param featureModel
	 *            the feature model to populate.
	 */
	public static void fromJsonTree( final Map< String, Object > featureDeclarations, final FeatureModel featureModel )
	{
		if ( featureDeclarations == null )
			return;

		declare( featureDeclarations, "spot", featureModel::declareSpotFeatures );
		declare( featureDeclarations, "edge", featureModel::declareEdgeFeatures );
		declare( featureDeclarations, "track", featureModel::declareTrackFeatures );
	}

	/**
	 * The signature of the three <code>declareXxxFeatures</code> methods of
	 * {@link FeatureModel}, abstracted over the object type.
	 */
	private interface FeatureDeclarator
	{
		void declare(
				final Collection< String > features,
				final Map< String, String > featureNames,
				final Map< String, String > featureShortNames,
				final Map< String, Dimension > featureDimensions,
				final Map< String, Boolean > isIntFeature );
	}

	/**
	 * Reads one section of the feature declarations (identified by its
	 * prefix, e.g. {@code spot}) and passes it to the specified declarator.
	 * If the section has no feature list, it is skipped.
	 */
	private static void declare( final Map< String, Object > declarations, final String prefix, final FeatureDeclarator declarator )
	{
		final Object features = declarations.get( prefix + "Features" );
		if ( !( features instanceof List ) )
			return;

		declarator.declare(
				castToStringList( features ),
				readStringMap( declarations.get( prefix + "FeatureNames" ) ),
				readStringMap( declarations.get( prefix + "FeatureShortNames" ) ),
				readDimensionMap( declarations.get( prefix + "FeatureDimensions" ) ),
				readBooleanMap( declarations.get( prefix + "FeatureIsInt" ) ) );
	}

	@SuppressWarnings( "unchecked" )
	private static Collection< String > castToStringList( final Object o )
	{
		return ( List< String > ) o;
	}

	@SuppressWarnings( "unchecked" )
	private static Map< String, String > readStringMap( final Object o )
	{
		final Map< String, String > map = new HashMap<>();
		if ( o instanceof Map )
			for ( final Map.Entry< String, Object > entry : ( ( Map< String, Object > ) o ).entrySet() )
				map.put( entry.getKey(), String.valueOf( entry.getValue() ) );
		return map;
	}

	@SuppressWarnings( "unchecked" )
	private static Map< String, Boolean > readBooleanMap( final Object o )
	{
		final Map< String, Boolean > map = new HashMap<>();
		if ( o instanceof Map )
			for ( final Map.Entry< String, Object > entry : ( ( Map< String, Object > ) o ).entrySet() )
				map.put( entry.getKey(), ( Boolean ) entry.getValue() );
		return map;
	}

	@SuppressWarnings( "unchecked" )
	private static Map< String, Dimension > readDimensionMap( final Object o )
	{
		final Map< String, Dimension > map = new HashMap<>();
		if ( o instanceof Map )
			for ( final Map.Entry< String, Object > entry : ( ( Map< String, Object > ) o ).entrySet() )
				map.put( entry.getKey(), Dimension.valueOf( String.valueOf( entry.getValue() ) ) );
		return map;
	}
}
