package fiji.plugin.trackmate.providers;

import java.util.HashMap;
import java.util.Map;

import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.log.LogService;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;

public class DetectorProvider extends AbstractProvider {

	/*
	 * BLANK CONSTRUCTOR
	 */

	protected Map< String, SpotDetectorFactory< ? > > implementations;

	/**
	 * This provider provides the GUI with the spot detectors currently available in the
	 * current TrackMate version. Each detector is identified by a key String, which can be used
	 * to retrieve new instance of the detector, settings for the target detector and a
	 * GUI panel able to configure these settings.
	 * <p>
	 * If you want to add custom detectors to TrackMate GUI, a simple way is to extend this
	 * factory so that it is registered with the custom detectors and pass this
	 * extended provider to the {@link TrackMate} trackmate.
	 * @param settings
	 * @param model
	 */
	public DetectorProvider() {
		registerDetectors();
		currentKey = LogDetectorFactory.DETECTOR_KEY;
	}


	/*
	 * METHODS
	 */

	/**
	 * Discovers the detector factories in the current application context and
	 * registers them in this provider.
	 */
	@SuppressWarnings( "rawtypes" )
	protected void registerDetectors() {
		implementations = new HashMap< String, SpotDetectorFactory< ? > >();
		// Find the rest from the context.
		final Context context = new Context( LogService.class, PluginService.class );
		final LogService log = context.getService( LogService.class );
		final PluginService pluginService = context.getService( PluginService.class );
		for ( final PluginInfo< SpotDetectorFactory > info : pluginService.getPluginsOfType( SpotDetectorFactory.class ) )
		{
			try
			{
				final SpotDetectorFactory< ? > detectorFactory = info.createInstance();
				implementations.put( detectorFactory.getName(), detectorFactory );
				keys.add( detectorFactory.getKey() );
				infoTexts.add( detectorFactory.getInfoText() );
				names.add( detectorFactory.getName() );
			}
			catch ( final InstantiableException e )
			{
				log.error( "Could not instantiate " + info.getClassName(), e );
			}
		}

		if ( implementations.size() < 1 )
		{
			log.error( "Could not find any detector factory.\n" );
		}
	}

	/**
	 * Returns a new instance of the target detector identified by the key
	 * parameter. If the key is unknown to this provider, return
	 * <code>null</code>.
	 */
	public SpotDetectorFactory< ? > getDetectorFactory()
	{
		final SpotDetectorFactory< ? > detectorFactory = implementations.get( currentKey );
		return detectorFactory;
	}

}
