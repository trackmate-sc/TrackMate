package fiji.plugin.trackmate.providers;

import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;

/**
 * A provider for the edge analyzers provided in the GUI.
 */
public class EdgeAnalyzerProvider extends AbstractFeatureAnalyzerProvider< EdgeAnalyzer >
{
	/**
	 * This provider provides the GUI with the model spotFeatureAnalyzers
	 * currently available in TrackMate. Each spotFeatureAnalyzer is identified
	 * by a key String, which can be used to retrieve new instance of the
	 * spotFeatureAnalyzer.
	 */
	public EdgeAnalyzerProvider()
	{
		registerFeatureAnalyzers( EdgeAnalyzer.class );
	}

}
