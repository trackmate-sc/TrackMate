package fiji.plugin.trackmate.providers;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotContrastAndSNRAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotRadiusEstimatorFactory;

/**
 * A provider for the spot analyzer factories provided in the GUI.
 */
public class SpotAnalyzerProvider extends AbstractFeatureAnalyzerProvider<SpotAnalyzerFactory<?>> {

	@SuppressWarnings("rawtypes")
	protected SpotAnalyzerFactory	spotIntensityAnalyzerFactory;
	@SuppressWarnings("rawtypes")
	protected SpotAnalyzerFactory	spotContrastAndSNRAnalyzerFactory;
	@SuppressWarnings("rawtypes")
	protected SpotAnalyzerFactory	spotRadiusEstimatorFactory;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * This provider provides the GUI with the model spotFeatureAnalyzers currently available in the
	 * TrackMate trackmate. Each spotFeatureAnalyzer is identified by a key String, which can be used
	 * to retrieve new instance of the spotFeatureAnalyzer.
	 * <p>
	 * If you want to add custom spotFeatureAnalyzers to TrackMate, a simple way is to extend this
	 * factory so that it is registered with the custom spotFeatureAnalyzers and provide this
	 * extended factory to the {@link TrackMate} trackmate.
	 */
	public SpotAnalyzerProvider() {
		registerSpotFeatureAnalyzers();
	}


	/*
	 * METHODS
	 */

	/**
	 * Registers the standard spotFeatureAnalyzers shipped with TrackMate.
	 */
	@SuppressWarnings("rawtypes")
	protected void registerSpotFeatureAnalyzers() {
		this.spotIntensityAnalyzerFactory = new SpotIntensityAnalyzerFactory();
		this.spotContrastAndSNRAnalyzerFactory = new SpotContrastAndSNRAnalyzerFactory();
		this.spotRadiusEstimatorFactory = new SpotRadiusEstimatorFactory();
		// Here order matters.
		registerAnalyzer(SpotIntensityAnalyzerFactory.KEY, spotIntensityAnalyzerFactory);
		registerAnalyzer(SpotContrastAndSNRAnalyzerFactory.KEY, spotContrastAndSNRAnalyzerFactory);
		registerAnalyzer(SpotRadiusEstimatorFactory.KEY, spotRadiusEstimatorFactory);
	}
}
