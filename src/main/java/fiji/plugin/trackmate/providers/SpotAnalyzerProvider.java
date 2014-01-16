package fiji.plugin.trackmate.providers;

import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;

/**
 * A provider for the spot analyzer factories provided in the GUI.
 */
@SuppressWarnings( "rawtypes" )
public class SpotAnalyzerProvider extends AbstractFeatureAnalyzerProvider< SpotAnalyzerFactory >
{
	/**
	 * This provider provides the GUI with the spotFeatureAnalyzers currently
	 * available in TrackMate. Each spotFeatureAnalyzer is identified by a key
	 * String, which can be used to retrieve new instance of the
	 * spotFeatureAnalyzer.
	 */
	public SpotAnalyzerProvider()
	{
		registerFeatureAnalyzers( SpotAnalyzerFactory.class );
	}
}
