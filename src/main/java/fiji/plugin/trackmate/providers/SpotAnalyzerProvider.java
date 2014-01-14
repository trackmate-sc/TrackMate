package fiji.plugin.trackmate.providers;

import java.util.HashMap;

import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.log.LogService;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;

import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;

/**
 * A provider for the spot analyzer factories provided in the GUI.
 */
public class SpotAnalyzerProvider extends AbstractFeatureAnalyzerProvider< SpotAnalyzerFactory< ? >>
{

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * This provider provides the GUI with the spotFeatureAnalyzers currently
	 * available in TrackMate. Each spotFeatureAnalyzer is identified by a key
	 * String, which can be used to retrieve new instance of the
	 * spotFeatureAnalyzer.
	 */
	public SpotAnalyzerProvider()
	{
		registerSpotFeatureAnalyzers();
	}

	/*
	 * METHODS
	 */

	/**
	 * Discovers the spot feature analyzer factories in the current application
	 * context and registers them in this provider.
	 */
	@SuppressWarnings( "rawtypes" )
	protected void registerSpotFeatureAnalyzers()
	{
		final HashMap< String, SpotAnalyzerFactory< ? >> implementations = new HashMap< String, SpotAnalyzerFactory< ? > >();

		final Context context = new Context( LogService.class, PluginService.class );
		final LogService log = context.getService( LogService.class );
		final PluginService pluginService = context.getService( PluginService.class );
		for ( final PluginInfo< SpotAnalyzerFactory > info : pluginService.getPluginsOfType( SpotAnalyzerFactory.class ) )
		{
			try
			{
				final SpotAnalyzerFactory< ? > analyzer = info.createInstance();
				implementations.put( analyzer.getKey(), analyzer );
			}
			catch ( final InstantiableException e )
			{
				log.error( "Could not instantiate " + info.getClassName(), e );
			}
		}

		for ( final SpotAnalyzerFactory< ? > analyzer : implementations.values() )
		{
			registerAnalyzer( analyzer.getKey(), analyzer );
		}

		if ( implementations.size() < 1 )
		{
			log.error( "Could not find any spot feature analyzer factory.\n" );
		}
	}
}
