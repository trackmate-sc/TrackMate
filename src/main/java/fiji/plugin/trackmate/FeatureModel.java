package fiji.plugin.trackmate;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;

/**
 * This class represents the part of the {@link Model} that is in charge of
 * dealing with spot features and track features.
 *
 * @author Jean-Yves Tinevez, 2011, 2012. Revised December 2020.
 *
 */
public class FeatureModel
{

	/*
	 * FIELDS
	 */

	private final Collection< String > trackFeatures = new LinkedHashSet<>();

	private final Map< String, String > trackFeatureNames = new HashMap<>();

	private final Map< String, String > trackFeatureShortNames = new HashMap<>();

	private final Map< String, Dimension > trackFeatureDimensions = new HashMap<>();

	private final Map< String, Boolean > trackFeatureIsInt = new HashMap<>();

	/**
	 * Feature storage. We use a Map of Map as a 2D Map. The list maps each
	 * track to its feature map. The feature map maps each feature to the double
	 * value for the specified feature.
	 */
	Map< Integer, Map< String, Double > > trackFeatureValues = new ConcurrentHashMap<>();

	/**
	 * Feature storage for edges.
	 */
	private final ConcurrentHashMap< DefaultWeightedEdge, ConcurrentHashMap< String, Double > > edgeFeatureValues = new ConcurrentHashMap<>();

	private final Collection< String > edgeFeatures = new LinkedHashSet<>();

	private final Map< String, String > edgeFeatureNames = new HashMap<>();

	private final Map< String, String > edgeFeatureShortNames = new HashMap<>();

	private final Map< String, Dimension > edgeFeatureDimensions = new HashMap<>();

	private final Map< String, Boolean > edgeFeatureIsInt = new HashMap<>();

	private final Collection< String > spotFeatures = new LinkedHashSet<>();

	private final Map< String, String > spotFeatureNames = new HashMap<>();

	private final Map< String, String > spotFeatureShortNames = new HashMap<>();

	private final Map< String, Dimension > spotFeatureDimensions = new HashMap<>();

	private final Map< String, Boolean > spotFeatureIsInt = new HashMap<>();

	private final Model model;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * Instantiates a new feature model. The basic spot features (POSITON_*,
	 * RADIUS, FRAME, QUALITY) are declared. Edge and track feature declarations
	 * are left blank.
	 *
	 * @param model
	 *            the parent {@link Model}.
	 */
	protected FeatureModel( final Model model )
	{
		this.model = model;
		// Adds the base spot, edge & track features
		declareSpotFeatures(
				Spot.FEATURES,
				Spot.FEATURE_NAMES,
				Spot.FEATURE_SHORT_NAMES,
				Spot.FEATURE_DIMENSIONS,
				Spot.IS_INT );
		declareEdgeFeatures(
				EdgeTargetAnalyzer.FEATURES,
				EdgeTargetAnalyzer.FEATURE_NAMES,
				EdgeTargetAnalyzer.FEATURE_SHORT_NAMES,
				EdgeTargetAnalyzer.FEATURE_DIMENSIONS,
				EdgeTargetAnalyzer.IS_INT );
		declareTrackFeatures(
				TrackIndexAnalyzer.FEATURES,
				TrackIndexAnalyzer.FEATURE_NAMES,
				TrackIndexAnalyzer.FEATURE_SHORT_NAMES,
				TrackIndexAnalyzer.FEATURE_DIMENSIONS,
				TrackIndexAnalyzer.IS_INT );
	}

	/*
	 * EDGE FEATURES
	 */

	/**
	 * Stores a numerical feature for an edge of this model.
	 * <p>
	 * Note that no checks are made to ensures that the edge exists in the
	 * {@link TrackModel}, and that the feature is declared in this
	 * {@link FeatureModel}.
	 *
	 * @param edge
	 *            the edge whose features to update.
	 * @param feature
	 *            the feature.
	 * @param value
	 *            the feature value
	 */
	public synchronized void putEdgeFeature( final DefaultWeightedEdge edge, final String feature, final Double value )
	{
		ConcurrentHashMap< String, Double > map = edgeFeatureValues.get( edge );
		if ( null == map )
		{
			map = new ConcurrentHashMap<>();
			edgeFeatureValues.put( edge, map );
		}
		map.put( feature, value );
	}

	public Double getEdgeFeature( final DefaultWeightedEdge edge, final String featureName )
	{
		final ConcurrentHashMap< String, Double > map = edgeFeatureValues.get( edge );
		if ( null == map )
			return null;
		return map.get( featureName );
	}

	/**
	 * Remove the value of the designated feature for the specified edge.
	 * 
	 * @param edge
	 *            the edge
	 * @param feature
	 *            the feature
	 */
	public void removeEdgeFeature( final DefaultWeightedEdge edge, final String feature )
	{
		final ConcurrentHashMap< String, Double > map = edgeFeatureValues.get( edge );
		if ( null == map )
			return;
		map.remove( feature );
	}

	/**
	 * Returns edge features as declared in this model.
	 *
	 * @return the edge features.
	 */
	public Collection< String > getEdgeFeatures()
	{
		return edgeFeatures;
	}

	/**
	 * Declares edge features, by specifying their name, short name and
	 * dimension. An {@link IllegalArgumentException} will be thrown if any of
	 * the map misses a feature.
	 *
	 * @param features
	 *            the list of edge features to register.
	 * @param featureNames
	 *            the name of these features.
	 * @param featureShortNames
	 *            the short name of these features.
	 * @param featureDimensions
	 *            the dimension of these features.
	 * @param isIntFeature
	 *            whether some of these features are made of <code>int</code>s (
	 *            <code>true</code>) or <code>double</code>s (<code>false</code>
	 *            ).
	 */
	public void declareEdgeFeatures( final Collection< String > features, final Map< String, String > featureNames, final Map< String, String > featureShortNames, final Map< String, Dimension > featureDimensions, final Map< String, Boolean > isIntFeature )
	{
		edgeFeatures.addAll( features );
		for ( final String feature : features )
		{
			final String name = featureNames.get( feature );
			if ( null == name )
				throw new IllegalArgumentException( "Feature " + feature + " misses a name." );
			edgeFeatureNames.put( feature, name );

			final String shortName = featureShortNames.get( feature );
			if ( null == shortName )
				throw new IllegalArgumentException( "Feature " + feature + " misses a short name." );
			edgeFeatureShortNames.put( feature, shortName );

			final Dimension dimension = featureDimensions.get( feature );
			if ( null == dimension )
				throw new IllegalArgumentException( "Feature " + feature + " misses a dimension." );
			edgeFeatureDimensions.put( feature, dimension );

			final Boolean isInt = isIntFeature.get( feature );
			if ( null == isInt )
				throw new IllegalArgumentException( "Feature " + feature + " misses the isInt flag." );
			edgeFeatureIsInt.put( feature, isInt );
		}
	}

	/**
	 * Returns the name mapping of the edge features that are dealt with in this
	 * model.
	 *
	 * @return the map of edge feature names.
	 */
	public Map< String, String > getEdgeFeatureNames()
	{
		return edgeFeatureNames;
	}

	/**
	 * Returns the short name mapping of the edge features that are dealt with
	 * in this model.
	 *
	 * @return the map of edge short names.
	 */
	public Map< String, String > getEdgeFeatureShortNames()
	{
		return edgeFeatureShortNames;
	}

	/**
	 * Returns the dimension mapping of the edge features that are dealt with in
	 * this model.
	 *
	 * @return the map of edge feature dimensions.
	 */
	public Map< String, Dimension > getEdgeFeatureDimensions()
	{
		return edgeFeatureDimensions;
	}

	/**
	 * Returns the map that states whether the target feature is integer values
	 * (<code>true</code>) or double valued (<code>false</code>).
	 *
	 * @return the map of isInt flag.
	 */
	public Map< String, Boolean > getEdgeFeatureIsInt()
	{
		return edgeFeatureIsInt;
	}

	/*
	 * TRACK FEATURES
	 */

	/**
	 * Returns the track features that are dealt with in this model.
	 * 
	 * @return the collection of track features managed in this model.
	 */
	public Collection< String > getTrackFeatures()
	{
		return trackFeatures;
	}

	/**
	 * Declares track features, by specifying their names, short name and
	 * dimension. An {@link IllegalArgumentException} will be thrown if any of
	 * the map misses a feature.
	 *
	 * @param features
	 *            the list of track feature to register.
	 * @param featureNames
	 *            the name of these features.
	 * @param featureShortNames
	 *            the short name of these features.
	 * @param featureDimensions
	 *            the dimension of these features.
	 * @param isIntFeature
	 *            whether some of these features are made of <code>int</code>s (
	 *            <code>true</code>) or <code>double</code>s (<code>false</code>
	 *            ).
	 */
	public void declareTrackFeatures( final Collection< String > features, final Map< String, String > featureNames, final Map< String, String > featureShortNames, final Map< String, Dimension > featureDimensions, final Map< String, Boolean > isIntFeature )
	{
		trackFeatures.addAll( features );
		for ( final String feature : features )
		{

			final String name = featureNames.get( feature );
			if ( null == name )
				throw new IllegalArgumentException( "Feature " + feature + " misses a name." );
			trackFeatureNames.put( feature, name );

			final String shortName = featureShortNames.get( feature );
			if ( null == shortName )
				throw new IllegalArgumentException( "Feature " + feature + " misses a short name." );
			trackFeatureShortNames.put( feature, shortName );

			final Dimension dimension = featureDimensions.get( feature );
			if ( null == dimension )
				throw new IllegalArgumentException( "Feature " + feature + " misses a dimension." );
			trackFeatureDimensions.put( feature, dimension );

			final Boolean isInt = isIntFeature.get( feature );
			if ( null == isInt )
				throw new IllegalArgumentException( "Feature " + feature + " misses the isInt flag." );
			trackFeatureIsInt.put( feature, isInt );
		}
	}

	/**
	 * Returns the name mapping of the track features that are dealt with in
	 * this model.
	 * 
	 * @return the feature name.
	 */
	public Map< String, String > getTrackFeatureNames()
	{
		return trackFeatureNames;
	}

	/**
	 * Returns the short name mapping of the track features that are dealt with
	 * in this model.
	 *
	 * @return the feature short name.
	 */
	public Map< String, String > getTrackFeatureShortNames()
	{
		return trackFeatureShortNames;
	}

	/**
	 * Returns the dimension mapping of the track features that are dealt with
	 * in this model.
	 * 
	 * @return the feature dimension.
	 */
	public Map< String, Dimension > getTrackFeatureDimensions()
	{
		return trackFeatureDimensions;
	}

	/**
	 * Returns the map that states whether the target feature is integer values
	 * (<code>true</code>) or double valued (<code>false</code>).
	 *
	 * @return the map of isInt flag.
	 */
	public Map< String, Boolean > getTrackFeatureIsInt()
	{
		return trackFeatureIsInt;
	}

	/**
	 * Stores a track numerical feature.
	 * <p>
	 * Note that no checks are made to ensures that the track ID exists in the
	 * {@link TrackModel}, and that the feature is declared in this
	 * {@link FeatureModel}.
	 *
	 * @param trackID
	 *            the ID of the track. It must be an existing track ID.
	 * @param feature
	 *            the feature.
	 * @param value
	 *            the feature value.
	 */
	public synchronized void putTrackFeature( final Integer trackID, final String feature, final Double value )
	{
		Map< String, Double > trackFeatureMap = trackFeatureValues.get( trackID );
		if ( null == trackFeatureMap )
		{
			trackFeatureMap = new HashMap<>( trackFeatures.size() );
			trackFeatureValues.put( trackID, trackFeatureMap );
		}
		trackFeatureMap.put( feature, value );
	}

	/**
	 * Remove the value of the designated feature for the track with the
	 * specified ID.
	 * 
	 * @param trackID
	 *            the track ID
	 * @param feature
	 *            the feature
	 */
	public void removeTrackFeature( final Integer trackID, final String feature )
	{
		final Map< String, Double > map = trackFeatureValues.get( trackID );
		if ( null == map )
			return;
		map.remove( feature );
	}

	/**
	 * Returns the numerical value of the specified track feature for the
	 * specified track.
	 *
	 * @param trackID
	 *            the track ID to quest.
	 * @param feature
	 *            the desired feature.
	 * @return the value of the specified feature.
	 */
	public Double getTrackFeature( final Integer trackID, final String feature )
	{
		final Map< String, Double > valueMap = trackFeatureValues.get( trackID );
		return valueMap == null ? null : valueMap.get( feature );
	}

	/**
	 * Returns the map of all track features declared for all tracks of the
	 * model.
	 *
	 * @return a new mapping of feature vs its numerical values.
	 */
	public Map< String, double[] > getTrackFeatureValues()
	{
		final Map< String, double[] > featureValues = new HashMap<>();
		Double val;
		final int nTracks = model.getTrackModel().nTracks( false );
		for ( final String feature : trackFeatures )
		{
			// Make a double array to comply to JFreeChart histograms
			boolean noDataFlag = true;
			final double[] values = new double[ nTracks ];
			int index = 0;
			for ( final Integer trackID : model.getTrackModel().trackIDs( false ) )
			{
				val = getTrackFeature( trackID, feature );
				if ( null == val )
					continue;

				values[ index++ ] = val;
				noDataFlag = false;
			}

			if ( noDataFlag )
				featureValues.put( feature, noDataFlag ? new double[ 0 ] : values );
		}
		return featureValues;
	}

	/*
	 * SPOT FEATURES the spot features are stored in the Spot object themselves,
	 * but we declare them here.
	 */

	/**
	 * Declares spot features, by specifying their names, short name and
	 * dimension. An {@link IllegalArgumentException} will be thrown if any of
	 * the map misses a feature.
	 *
	 * @param features
	 *            the list of spot feature to register.
	 * @param featureNames
	 *            the name of these features.
	 * @param featureShortNames
	 *            the short name of these features.
	 * @param featureDimensions
	 *            the dimension of these features.
	 * @param isIntFeature
	 *            whether some of these features are made of <code>int</code>s (
	 *            <code>true</code>) or <code>double</code>s (<code>false</code>
	 *            ).
	 */
	public void declareSpotFeatures( final Collection< String > features, final Map< String, String > featureNames, final Map< String, String > featureShortNames, final Map< String, Dimension > featureDimensions, final Map< String, Boolean > isIntFeature )
	{
		spotFeatures.addAll( features );
		for ( final String feature : features )
		{

			final String name = featureNames.get( feature );
			if ( null == name )
				throw new IllegalArgumentException( "Feature " + feature + " misses a name." );
			spotFeatureNames.put( feature, name );

			final String shortName = featureShortNames.get( feature );
			if ( null == shortName )
				throw new IllegalArgumentException( "Feature " + feature + " misses a short name." );
			spotFeatureShortNames.put( feature, shortName );

			final Dimension dimension = featureDimensions.get( feature );
			if ( null == dimension )
				throw new IllegalArgumentException( "Feature " + feature + " misses a dimension." );
			spotFeatureDimensions.put( feature, dimension );

			final Boolean isInt = isIntFeature.get( feature );
			if ( null == isInt )
				throw new IllegalArgumentException( "Feature " + feature + " misses the isInt flag." );
			spotFeatureIsInt.put( feature, isInt );

		}
	}

	/**
	 * Returns spot features as declared in this model.
	 *
	 * @return the spot features.
	 */
	public Collection< String > getSpotFeatures()
	{
		return spotFeatures;
	}

	/**
	 * Returns the name mapping of the spot features that are dealt with in this
	 * model.
	 *
	 * @return the map of spot feature names.
	 */
	public Map< String, String > getSpotFeatureNames()
	{
		return spotFeatureNames;
	}

	/**
	 * Returns the short name mapping of the spot features that are dealt with
	 * in this model.
	 *
	 * @return the map of spot short names.
	 */
	public Map< String, String > getSpotFeatureShortNames()
	{
		return spotFeatureShortNames;
	}

	/**
	 * Returns the dimension mapping of the spot features that are dealt with in
	 * this model.
	 *
	 * @return the map of spot feature dimensions.
	 */
	public Map< String, Dimension > getSpotFeatureDimensions()
	{
		return spotFeatureDimensions;
	}

	/**
	 * Returns the map that states whether the target feature is integer values
	 * (<code>true</code>) or double valued (<code>false</code>).
	 *
	 * @return the map of isInt flag.
	 */
	public Map< String, Boolean > getSpotFeatureIsInt()
	{
		return spotFeatureIsInt;
	}

	@Override
	public String toString()
	{
		final StringBuilder str = new StringBuilder();

		// Spots
		str.append( "Spot features declared:\n" );
		appendFeatureDeclarations( str, spotFeatures, spotFeatureNames, spotFeatureShortNames, spotFeatureDimensions, spotFeatureIsInt );
		str.append( '\n' );

		// Edges
		str.append( "Edge features declared:\n" );
		appendFeatureDeclarations( str, edgeFeatures, edgeFeatureNames, edgeFeatureShortNames, edgeFeatureDimensions, edgeFeatureIsInt );
		str.append( '\n' );

		// Track
		str.append( "Track features declared:\n" );
		appendFeatureDeclarations( str, trackFeatures, trackFeatureNames, trackFeatureShortNames, trackFeatureDimensions, trackFeatureIsInt );
		str.append( '\n' );

		return str.toString();
	}

	/**
	 * Echoes the full content of this {@link FeatureModel}.
	 * 
	 * @return a String representation of the full content of this model.
	 */
	public String echo()
	{
		final StringBuilder str = new StringBuilder();

		// Spots
		str.append( "Spot features:\n" );
		str.append( " - Declared:\n" );
		appendFeatureDeclarations( str, spotFeatures, spotFeatureNames, spotFeatureShortNames, spotFeatureDimensions, spotFeatureIsInt );
		str.append( '\n' );

		// Edges
		str.append( "Edge features:\n" );
		str.append( " - Declared:\n" );
		appendFeatureDeclarations( str, edgeFeatures, edgeFeatureNames, edgeFeatureShortNames, edgeFeatureDimensions, edgeFeatureIsInt );
		str.append( '\n' );
		str.append( " - Values:\n" );
		appendFeatureValues( str, edgeFeatureValues );

		// Track
		str.append( "Track features:\n" );
		str.append( " - Declared:\n" );
		appendFeatureDeclarations( str, trackFeatures, trackFeatureNames, trackFeatureShortNames, trackFeatureDimensions, trackFeatureIsInt );
		str.append( '\n' );
		str.append( " - Values:\n" );
		appendFeatureValues( str, trackFeatureValues );

		return str.toString();
	}

	/*
	 * STATIC UTILS
	 */

	private static final < K > void appendFeatureValues( final StringBuilder str, final Map< K, ? extends Map< String, Double > > values )
	{
		for ( final K key : values.keySet() )
		{
			final String header = "   - " + key.toString() + ":\n";
			str.append( header );
			final Map< String, Double > map = values.get( key );
			for ( final String feature : map.keySet() )
				str.append( "     - " + feature + " = " + map.get( feature ) + '\n' );
		}
	}

	private static final void appendFeatureDeclarations( final StringBuilder str, final Collection< String > features, final Map< String, String > featureNames, final Map< String, String > featureShortNames, final Map< String, Dimension > featureDimensions, final Map< String, Boolean > isIntFeature )
	{
		for ( final String feature : features )
		{
			str.append( "   - " + feature + ": " + featureNames.get( feature ) + ", '" + featureShortNames.get( feature ) + "' (" + featureDimensions.get( feature ) + ")" );
			if ( isIntFeature.get( feature ).booleanValue() )
				str.append( " - integer valued.\n" );
			else
				str.append( " - double valued.\n" );
		}
	}
}
