package fiji.plugin.trackmate.providers;

import java.util.Collections;
import java.util.List;

import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.log.LogService;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;

import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;

/**
 * A provider for the spot analyzer factories provided in the GUI.
 */
@SuppressWarnings( "rawtypes" )
public class SpotAnalyzerProvider extends AbstractFeatureAnalyzerProvider< SpotAnalyzerFactory >
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
	protected void registerSpotFeatureAnalyzers()
	{
		final Context context = new Context( LogService.class, PluginService.class );
		final LogService log = context.getService( LogService.class );
		final PluginService pluginService = context.getService( PluginService.class );
		final List< PluginInfo< SpotAnalyzerFactory >> infos = pluginService.getPluginsOfType( SpotAnalyzerFactory.class );
		Collections.sort( infos, priorityComparator );

		for ( final PluginInfo< SpotAnalyzerFactory > info : infos )
		{
			try
			{
				final SpotAnalyzerFactory< ? > analyzer = info.createInstance();
				registerAnalyzer( analyzer.getKey(), analyzer );
			}
			catch ( final InstantiableException e )
			{
				log.error( "Could not instantiate " + info.getClassName(), e );
			}
		}
	}
}
