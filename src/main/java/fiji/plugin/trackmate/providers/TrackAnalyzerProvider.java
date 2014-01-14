package fiji.plugin.trackmate.providers;

import java.util.HashMap;

import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.log.LogService;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;

import fiji.plugin.trackmate.TrackMate;
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

	/*
	 * BLANK CONSTRUCTOR
	 */

	/**
	 * This provider provides the GUI with the model trackFeatureAnalyzers
	 * currently available in the TrackMate trackmate. Each trackFeatureAnalyzer
	 * is identified by a key String, which can be used to retrieve new instance
	 * of the trackFeatureAnalyzer.
	 * <p>
	 * If you want to add custom trackFeatureAnalyzers to TrackMate, a simple
	 * way is to extend this factory so that it is registered with the custom
	 * trackFeatureAnalyzers and provide this extended factory to the
	 * {@link TrackMate} trackmate.
	 */
	public TrackAnalyzerProvider()
	{
		registerTrackFeatureAnalyzers();
	}

	/*
	 * METHODS
	 */

	/**
	 * Discovers the track feature analyzers in the current application context
	 * and registers them in this provider.
	 */
	protected void registerTrackFeatureAnalyzers()
	{
		final HashMap< String, TrackAnalyzer > implementations = new HashMap< String, TrackAnalyzer >();

		final Context context = new Context( LogService.class, PluginService.class );
		final LogService log = context.getService( LogService.class );
		final PluginService pluginService = context.getService( PluginService.class );
		for ( final PluginInfo< TrackAnalyzer > info : pluginService.getPluginsOfType( TrackAnalyzer.class ) )
		{
			try
			{
				final TrackAnalyzer analyzer = info.createInstance();
				implementations.put( analyzer.getKey(), analyzer );
			}
			catch ( final InstantiableException e )
			{
				log.error( "Could not instantiate " + info.getClassName(), e );
			}
		}

		for ( final TrackAnalyzer analyzer : implementations.values() )
		{
			registerAnalyzer( analyzer.getKey(), analyzer );
		}

		if ( implementations.size() < 1 )
		{
			log.error( "Could not find any spot feature analyzer factory.\n" );
		}
	}

}
