package fiji.plugin.trackmate.features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.util.DoubleArray;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.manual.ManualEdgeColorAnalyzer;
import fiji.plugin.trackmate.features.manual.ManualSpotColorAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.ObjectType;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import fiji.plugin.trackmate.visualization.ManualEdgeColorGenerator;
import fiji.plugin.trackmate.visualization.ManualEdgePerSpotColorGenerator;
import fiji.plugin.trackmate.visualization.ManualSpotColorGenerator;
import fiji.plugin.trackmate.visualization.ManualSpotPerEdgeColorGenerator;
import fiji.plugin.trackmate.visualization.PerEdgeFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.PerSpotFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.SpotColorGenerator;
import fiji.plugin.trackmate.visualization.SpotColorGeneratorPerEdgeFeature;
import fiji.plugin.trackmate.visualization.SpotColorGeneratorPerTrackFeature;
import fiji.plugin.trackmate.visualization.UniformSpotColorGenerator;
import fiji.plugin.trackmate.visualization.UniformTrackColorGenerator;

public class FeatureUtils
{

	private static final String USE_UNIFORM_COLOR_NAME = "Uniform color";

	public static final String USE_UNIFORM_COLOR_KEY = "UNIFORM_COLOR";

	public static final Map< String, String > collectFeatureKeys( final ObjectType target, final Model model, final Settings settings )
	{
		final Map< String, String > inverseMap = new HashMap<>();
		// will be used to sort.

		switch ( target )
		{
		case SPOTS:
		{

			// Collect all.
			if ( model != null )
			{
				for ( final String featureKey : model.getFeatureModel().getSpotFeatureNames().keySet() )
					inverseMap.put( model.getFeatureModel().getSpotFeatureNames().get( featureKey ), featureKey );
			}
			else
			{
				// If we have no model, we still want to add spot features.
				for ( final String featureKey : Spot.FEATURE_NAMES.keySet() )
					inverseMap.put( Spot.FEATURE_NAMES.get( featureKey ), featureKey );
			}
			if ( settings != null )
			{
				for ( final SpotAnalyzerFactory< ? > sf : settings.getSpotAnalyzerFactories() )
					for ( final String featureKey : sf.getFeatureNames().keySet() )
						inverseMap.put( sf.getFeatureNames().get( featureKey ), featureKey );
			}
			break;
		}

		case EDGES:
		{
			if ( model != null )
			{
				for ( final String featureKey : model.getFeatureModel().getEdgeFeatureNames().keySet() )
					inverseMap.put( model.getFeatureModel().getEdgeFeatureNames().get( featureKey ), featureKey );
			}
			if ( settings != null )
			{
				for ( final EdgeAnalyzer ea : settings.getEdgeAnalyzers() )
					for ( final String featureKey : ea.getFeatureNames().keySet() )
						inverseMap.put( ea.getFeatureNames().get( featureKey ), featureKey );
			}
			break;
		}

		case TRACKS:
		{
			if ( model != null )
			{
				for ( final String featureKey : model.getFeatureModel().getTrackFeatureNames().keySet() )
					inverseMap.put( model.getFeatureModel().getTrackFeatureNames().get( featureKey ), featureKey );
			}
			if ( settings != null )
			{
				for ( final TrackAnalyzer ta : settings.getTrackAnalyzers() )
					for ( final String featureKey : ta.getFeatureNames().keySet() )
						inverseMap.put( ta.getFeatureNames().get( featureKey ), featureKey );
			}
			break;
		}

		case DEFAULT:
		{
			inverseMap.put( USE_UNIFORM_COLOR_NAME, USE_UNIFORM_COLOR_KEY );
			break;
		}

		default:
			throw new IllegalArgumentException( "Unknown object type: " + target );
		}

		// Sort by feature name.
		final List< String > featureNameList = new ArrayList<>( inverseMap.keySet() );
		featureNameList.sort( null );

		final Map< String, String > featureNames = new LinkedHashMap<>( featureNameList.size() );
		for ( final String featureName : featureNameList )
			featureNames.put( inverseMap.get( featureName ), featureName );

		return featureNames;
	}

	/**
	 * Can contains {@link Double#NaN} for missing or undefined values.
	 *
	 * @param featureKey
	 * @param target
	 * @param model
	 * @param settings
	 * @param visibleOnly
	 * @return
	 */
	public static double[] collectFeatureValues(
			final String featureKey,
			final ObjectType target,
			final Model model,
			final Settings settings,
			final boolean visibleOnly )
	{
		final FeatureModel fm = model.getFeatureModel();
		switch ( target )
		{
		case DEFAULT:
			return new double[] {};

		case EDGES:
		{
			final DoubleArray val = new DoubleArray();
			for ( final Integer trackID : model.getTrackModel().trackIDs( visibleOnly ) )
			{
				for ( final DefaultWeightedEdge edge : model.getTrackModel().trackEdges( trackID ) )
				{
					final Double ef = fm.getEdgeFeature( edge, featureKey );
					val.addValue( ef == null ? Double.NaN : ef.doubleValue() );
				}
			}
			return val.copyArray();
		}
		case SPOTS:
		{

			final DoubleArray val = new DoubleArray();
			for ( final Spot spot : model.getSpots().iterable( visibleOnly ) )
			{
				final Double sf = spot.getFeature( featureKey );
				val.addValue( sf == null ? Double.NaN : sf.doubleValue() );
			}
			return val.copyArray();
		}
		case TRACKS:
		{
			final DoubleArray val = new DoubleArray();
			for ( final Integer trackID : model.getTrackModel().trackIDs( visibleOnly ) )
			{
				final Double tf = fm.getTrackFeature( trackID, featureKey );
				val.addValue( tf == null ? Double.NaN : tf.doubleValue() );
			}
			return val.copyArray();
		}
		default:
			throw new IllegalArgumentException( "Unknown object type: " + target );
		}
	}

	public static final FeatureColorGenerator< Spot > createSpotColorGenerator( final Model model, final DisplaySettings displaySettings )
	{
		switch ( displaySettings.getSpotColorByType() )
		{
		case DEFAULT:
			return new UniformSpotColorGenerator( displaySettings.getSpotUniformColor() );

		case EDGES:

			if ( displaySettings.getSpotColorByFeature().equals( ManualEdgeColorAnalyzer.FEATURE ) )
				return new ManualSpotPerEdgeColorGenerator( model, displaySettings.getMissingValueColor() );

			return new SpotColorGeneratorPerEdgeFeature(
					model,
					displaySettings.getSpotColorByFeature(),
					displaySettings.getMissingValueColor(),
					displaySettings.getUndefinedValueColor(),
					displaySettings.getColormap(),
					displaySettings.getSpotMin(),
					displaySettings.getSpotMax() );

		case SPOTS:

			if ( displaySettings.getSpotColorByFeature().equals( ManualSpotColorAnalyzerFactory.FEATURE ) )
				return new ManualSpotColorGenerator( displaySettings.getMissingValueColor() );

			return new SpotColorGenerator(
					displaySettings.getSpotColorByFeature(),
					displaySettings.getMissingValueColor(),
					displaySettings.getUndefinedValueColor(),
					displaySettings.getColormap(),
					displaySettings.getSpotMin(),
					displaySettings.getSpotMax() );

		case TRACKS:
			return new SpotColorGeneratorPerTrackFeature(
					model,
					displaySettings.getSpotColorByFeature(),
					displaySettings.getMissingValueColor(),
					displaySettings.getUndefinedValueColor(),
					displaySettings.getColormap(),
					displaySettings.getSpotMin(),
					displaySettings.getSpotMax() );

		default:
			throw new IllegalArgumentException( "Unknown type: " + displaySettings.getSpotColorByType() );
		}
	}

	public static final FeatureColorGenerator< DefaultWeightedEdge > createTrackColorGenerator( final Model model, final DisplaySettings displaySettings )
	{
		switch ( displaySettings.getTrackColorByType() )
		{
		case DEFAULT:
			return new UniformTrackColorGenerator( displaySettings.getTrackUniformColor() );

		case EDGES:

			if ( displaySettings.getTrackColorByFeature().equals( ManualEdgeColorAnalyzer.FEATURE ) )
				return new ManualEdgeColorGenerator( model, displaySettings.getMissingValueColor() );

			return new PerEdgeFeatureColorGenerator(
					model,
					displaySettings.getTrackColorByFeature(),
					displaySettings.getMissingValueColor(),
					displaySettings.getUndefinedValueColor(),
					displaySettings.getColormap(),
					displaySettings.getTrackMin(),
					displaySettings.getTrackMax() );

		case SPOTS:

			if ( displaySettings.getTrackColorByFeature().equals( ManualSpotColorAnalyzerFactory.FEATURE ) )
				return new ManualEdgePerSpotColorGenerator( model, displaySettings.getMissingValueColor() );

			return new PerSpotFeatureColorGenerator(
					model,
					displaySettings.getTrackColorByFeature(),
					displaySettings.getMissingValueColor(),
					displaySettings.getUndefinedValueColor(),
					displaySettings.getColormap(),
					displaySettings.getTrackMin(),
					displaySettings.getTrackMax() );

		case TRACKS:
			return new PerTrackFeatureColorGenerator(
					model,
					displaySettings.getTrackColorByFeature(),
					displaySettings.getMissingValueColor(),
					displaySettings.getUndefinedValueColor(),
					displaySettings.getColormap(),
					displaySettings.getTrackMin(),
					displaySettings.getTrackMax() );

		default:
			throw new IllegalArgumentException( "Unknown type: " + displaySettings.getTrackColorByType() );
		}
	}

	public static final Model DUMMY_MODEL = new Model();
	static
	{
		final Random ran = new Random();
		DUMMY_MODEL.beginUpdate();
		try
		{

			for ( int i = 0; i < 100; i++ )
			{
				Spot previous = null;
				for ( int t = 0; t < 20; t++ )
				{

					final double x = ran.nextDouble();
					final double y = ran.nextDouble();
					final double z = ran.nextDouble();
					final double r = ran.nextDouble();
					final double q = ran.nextDouble();
					final Spot spot = new Spot( x, y, z, r, q );
					DUMMY_MODEL.addSpotTo( spot, t );
					if ( previous != null )
						DUMMY_MODEL.addEdge( previous, spot, ran.nextDouble() );

					previous = spot;
				}
			}
		}
		finally
		{
			DUMMY_MODEL.endUpdate();
		}
	}
}
