package fiji.plugin.trackmate.providers;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.meta.ImgPlus;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.spot.SpotAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotContrastAndSNRAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotRadiusEstimatorFactory;

/**
 * A provider for the spot analyzer factories provided in the GUI.
 */
public class SpotAnalyzerProvider {


	/** The detector names, in the order they will appear in the GUI.
	 * These names will be used as keys to access relevant spot analyzer classes.  */
	protected List<String> analyzerNames;
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
		this.spotContrastAndSNRAnalyzerFactory = new SpotContrastAndSNRAnalyzerFactory();
		this.spotIntensityAnalyzerFactory = new SpotIntensityAnalyzerFactory();
		this.spotRadiusEstimatorFactory = new SpotRadiusEstimatorFactory();

		analyzerNames = new ArrayList<String>(3);
		analyzerNames.add(SpotIntensityAnalyzerFactory.KEY);
		analyzerNames.add(SpotContrastAndSNRAnalyzerFactory.KEY); // must be after the statistics one
		analyzerNames.add(SpotRadiusEstimatorFactory.KEY);
	}

	/**
	 * Returns the instance of the target spotFeatureAnalyzer identified by the
	 * key parameter, and configured to operate on the specified {@link ImgPlus}
	 * . If the key is unknown to this provider, <code>null</code> is returned.
	 */
	@SuppressWarnings("rawtypes")
	public SpotAnalyzerFactory getSpotFeatureAnalyzer(final String key) {

		if (key.equals(SpotIntensityAnalyzerFactory.KEY)) {
			return spotIntensityAnalyzerFactory;

		} else if (key.equals(SpotContrastAndSNRAnalyzerFactory.KEY)) {
			return spotContrastAndSNRAnalyzerFactory;

		} else if (key.equals(SpotRadiusEstimatorFactory.KEY)) {
			return spotRadiusEstimatorFactory;

		} else {
			return null;
		}
	}

	/**
	 * Returns a list of the {@link SpotAnalyzer} names available through this provider.
	 */
	public List<String> getAvailableSpotFeatureAnalyzers() {
		return analyzerNames;
	}

}
