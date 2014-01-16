package fiji.plugin.trackmate.providers;

import fiji.plugin.trackmate.features.track.TrackAnalyzer;

/**
 * A provider for the track analyzers provided in the GUI.
 * <p>
 * Concrete implementation must declare what features they can compute
 * numerically, using the method {@link #getFeaturesForKey(String)}.
 * <p>
 * Feature key names are for historical reason all capitalized in an enum
 * manner. For instance: POSITION_X, MAX_INTENSITY, etc... They must be suitable
 * to be used as a attribute key in an xml file.
 */
public class TrackAnalyzerProvider extends AbstractFeatureAnalyzerProvider< TrackAnalyzer >
{
	/**
	 * This provider provides the GUI with the model trackFeatureAnalyzers
	 * currently available in TrackMate. Each trackFeatureAnalyzer is identified
	 * by a key String, which can be used to retrieve new instance of the
	 * trackFeatureAnalyzer.
	 */
	public TrackAnalyzerProvider()
	{
		registerFeatureAnalyzers( TrackAnalyzer.class );
	}
}
