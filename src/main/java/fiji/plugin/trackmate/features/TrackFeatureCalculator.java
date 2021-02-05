package fiji.plugin.trackmate.features;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.scijava.Cancelable;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;

/**
 * A class dedicated to centralizing the calculation of the numerical features
 * of tracks, through {@link TrackAnalyzer}s.
 *
 * @author Jean-Yves Tinevez - 2013
 *
 */
public class TrackFeatureCalculator extends MultiThreadedBenchmarkAlgorithm implements Cancelable
{

	private static final String BASE_ERROR_MSG = "[TrackFeatureCalculator] ";

	private final Settings settings;

	private final Model model;

	private boolean isCanceled;

	private String cancelReason;

	public TrackFeatureCalculator( final Model model, final Settings settings )
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
	 * Calculates the track features configured in the {@link Settings} for all
	 * the tracks of this model.
	 */
	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		// Declare what you do.
		for ( final TrackAnalyzer analyzer : settings.getTrackAnalyzers() )
		{
			final Collection< String > features = analyzer.getFeatures();
			final Map< String, String > featureNames = analyzer.getFeatureNames();
			final Map< String, String > featureShortNames = analyzer.getFeatureShortNames();
			final Map< String, Dimension > featureDimensions = analyzer.getFeatureDimensions();
			final Map< String, Boolean > isIntFeature = analyzer.getIsIntFeature();
			model.getFeatureModel().declareTrackFeatures( features, featureNames, featureShortNames, featureDimensions, isIntFeature );
		}

		// Do it.
		computeTrackFeaturesAgent( model.getTrackModel().trackIDs( false ), settings.getTrackAnalyzers(), true );

		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	/**
	 * Calculates all the track features configured in the {@link Settings}
	 * object for the specified tracks.
	 */
	public void computeTrackFeatures( final Collection< Integer > trackIDs, final boolean doLogIt )
	{
		final List< TrackAnalyzer > trackFeatureAnalyzers = settings.getTrackAnalyzers();
		computeTrackFeaturesAgent( trackIDs, trackFeatureAnalyzers, doLogIt );
	}

	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Calculate all features for the tracks with the given IDs.
	 */
	private void computeTrackFeaturesAgent( final Collection< Integer > trackIDs, final List< TrackAnalyzer > analyzers, final boolean doLogIt )
	{
		isCanceled = false;
		cancelReason = null;

		final Logger logger = model.getLogger();
		if ( doLogIt )
		{
			logger.log( "Computing track features:\n", Logger.BLUE_COLOR );
		}

		for ( final TrackAnalyzer analyzer : analyzers )
		{
			if ( isCanceled() )
				return;

			if ( analyzer.isManualFeature() )
			{
				// Skip manual analyzers
				continue;
			}

			analyzer.setNumThreads( numThreads );
			if ( analyzer.isLocal() )
			{
				analyzer.process( trackIDs, model );
			}
			else
			{
				analyzer.process( model.getTrackModel().trackIDs( false ), model );
			}

			if ( doLogIt )
				logger.log( "  - " + analyzer.getName() + " in " + analyzer.getProcessingTime() + " ms.\n" );

		}
	}

	// --- org.scijava.Cancelable methods ---

	@Override
	public boolean isCanceled()
	{
		return isCanceled;
	}

	@Override
	public void cancel( final String reason )
	{
		isCanceled = true;
		cancelReason = reason;
	}

	@Override
	public String getCancelReason()
	{
		return cancelReason;
	}
}
