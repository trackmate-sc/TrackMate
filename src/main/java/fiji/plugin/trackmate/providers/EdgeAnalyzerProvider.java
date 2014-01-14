package fiji.plugin.trackmate.providers;

import java.util.HashMap;

import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.log.LogService;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;

/**
 * A provider for the edge analyzers provided in the GUI.
 */
public class EdgeAnalyzerProvider extends AbstractFeatureAnalyzerProvider< EdgeAnalyzer >
{

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * This provider provides the GUI with the model spotFeatureAnalyzers
	 * currently available in the TrackMate trackmate. Each spotFeatureAnalyzer
	 * is identified by a key String, which can be used to retrieve new instance
	 * of the spotFeatureAnalyzer.
	 * <p>
	 * If you want to add custom spotFeatureAnalyzers to TrackMate, a simple way
	 * is to extend this factory so that it is registered with the custom
	 * spotFeatureAnalyzers and provide this extended factory to the
	 * {@link TrackMate} trackmate.
	 */
	public EdgeAnalyzerProvider()
	{
		registerEdgeFeatureAnalyzers();
	}

	/*
	 * METHODS
	 */

	/**
	 * Discovers the edge feature analyzer factories in the current application
	 * context and registers them in this provider.
	 */
	protected void registerEdgeFeatureAnalyzers()
	{
		final HashMap< String, EdgeAnalyzer > implementations = new HashMap< String, EdgeAnalyzer >();

		final Context context = new Context( LogService.class, PluginService.class );
		final LogService log = context.getService( LogService.class );
		final PluginService pluginService = context.getService( PluginService.class );
		for ( final PluginInfo< EdgeAnalyzer > info : pluginService.getPluginsOfType( EdgeAnalyzer.class ) )
		{
			try
			{
				final EdgeAnalyzer analyzer = info.createInstance();
				implementations.put( analyzer.getKey(), analyzer );
			}
			catch ( final InstantiableException e )
			{
				log.error( "Could not instantiate " + info.getClassName(), e );
			}
		}

		for ( final EdgeAnalyzer analyzer : implementations.values() )
		{
			registerAnalyzer( analyzer.getKey(), analyzer );
		}

		if ( implementations.size() < 1 )
		{
			log.error( "Could not find any spot feature analyzer factory.\n" );
		}
	}

}
