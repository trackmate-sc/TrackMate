package fiji.plugin.trackmate.features;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;

/**
 * A class dedicated to centralizing the calculation of the numerical features
 * of spots, through {@link EdgeAnalyzer}s.
 *
 * @author Jean-Yves Tinevez - 2013
 *
 */
public class EdgeFeatureCalculator extends MultiThreadedBenchmarkAlgorithm
{

	private static final String BASE_ERROR_MSG = "[EdgeFeatureCalculator] ";

	private final Settings settings;

	private final Model model;

	public EdgeFeatureCalculator( final Model model, final Settings settings )
	{
		this.settings = settings;
		this.model = model;
	}

	/*
	 * METHODS
	 */

	@Override
	public boolean checkInput()
	{
		if ( null == model )
		{
			errorMessage = BASE_ERROR_MSG + "Model object is null.";
			return false;
		}
		if ( null == settings )
		{
			errorMessage = BASE_ERROR_MSG + "Settings object is null.";
			return false;
		}
		return true;
	}

	/**
	 * Calculates the edge features configured in the {@link Settings} for all
	 * the edges of this model.
	 */
	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		// Declare what you do.
		for ( final EdgeAnalyzer analyzer : settings.getEdgeAnalyzers() )
		{
			final Collection< String > features = analyzer.getFeatures();
			final Map< String, String > featureNames = analyzer.getFeatureNames();
			final Map< String, String > featureShortNames = analyzer.getFeatureShortNames();
			final Map< String, Dimension > featureDimensions = analyzer.getFeatureDimensions();
			final Map< String, Boolean > isIntFeature = analyzer.getIsIntFeature();
			model.getFeatureModel().declareEdgeFeatures( features, featureNames, featureShortNames, featureDimensions, isIntFeature );
		}

		// Do it.
		computeEdgeFeaturesAgent( model.getTrackModel().edgeSet(), settings.getEdgeAnalyzers(), true );

		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	/**
	 * Calculates all the edge features configured in the {@link Settings}
	 * object for the specified edges.
	 *
	 * @param edges
	 *            the edges to compute.
	 * @param doLogIt
	 *            if <code>true</code>, the logger of the model will be notified
	 *            of the calculation.
	 */
	public void computeEdgesFeatures( final Collection< DefaultWeightedEdge > edges, final boolean doLogIt )
	{
		final List< EdgeAnalyzer > spotFeatureAnalyzers = settings.getEdgeAnalyzers();
		computeEdgeFeaturesAgent( edges, spotFeatureAnalyzers, doLogIt );
	}

	/*
	 * PRIVATE METHODS
	 */

	private void computeEdgeFeaturesAgent( final Collection< DefaultWeightedEdge > edges, final List< EdgeAnalyzer > analyzers, final boolean doLogIt )
	{
		final Logger logger = model.getLogger();
		if ( doLogIt )
		{
			logger.log( "Computing edge features:\n", Logger.BLUE_COLOR );
		}
		for ( final EdgeAnalyzer analyzer : analyzers )
		{
			if ( analyzer.isManualFeature() )
			{
				// Skip manual features.
				continue;
			}
			analyzer.setNumThreads( numThreads );
			analyzer.process( edges, model );
			if ( doLogIt )
				logger.log( "  - " + analyzer.getName() + " in " + analyzer.getProcessingTime() + " ms.\n" );

		}
	}

}
