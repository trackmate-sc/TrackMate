package fiji.plugin.trackmate.providers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.log.LogService;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;

public class DetectorProvider
{

	/*
	 * BLANK CONSTRUCTOR
	 */

	protected Map< String, SpotDetectorFactory< ? > > implementations;

	protected List< String > keys;

	protected List< String > infoTexts;

	protected List< String > names;

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

		final Context context = new Context( LogService.class, PluginService.class );
		final LogService log = context.getService( LogService.class );
		final PluginService pluginService = context.getService( PluginService.class );
		for ( final PluginInfo< SpotDetectorFactory > info : pluginService.getPluginsOfType( SpotDetectorFactory.class ) )
		{
			try
			{
				final SpotDetectorFactory< ? > detectorFactory = info.createInstance();
				implementations.put( detectorFactory.getKey(), detectorFactory );
			}
			catch ( final InstantiableException e )
			{
				log.error( "Could not instantiate " + info.getClassName(), e );
			}
		}

		keys = new ArrayList< String >( implementations.size() );
		infoTexts = new ArrayList< String >( implementations.size() );
		names = new ArrayList< String >( implementations.size() );
		for ( final SpotDetectorFactory< ? > detectorFactory : implementations.values() )
		{
			keys.add( detectorFactory.getKey() );
			infoTexts.add( detectorFactory.getInfoText() );
			names.add( detectorFactory.getName() );
		}

		if ( implementations.size() < 1 )
		{
			log.error( "Could not find any detector factory.\n" );
		}
	}

	/**
	 * Returns a new instance of the target detector factory identified by the
	 * key parameter. If the key is unknown to this provider, returns
	 * <code>null</code>.
	 * <p>
	 * Querying twice the same detector factory with this method returns the
	 * same instance.
	 *
	 * @return the desired {@link SpotDetectorFactory} instance.
	 */
	public SpotDetectorFactory< ? > getDetectorFactory( final String key )
	{
		final SpotDetectorFactory< ? > detectorFactory = implementations.get( key );
		return detectorFactory;
	}

	/**
	 * Returns the keys of all the spot detectors known to this provider. The
	 * list share the same index than for {@link #getNames()} and
	 * {@link #getInfoTexts()}.
	 *
	 * @return the keys, as a list of strings.
	 */
	public List< String > getKeys()
	{
		return keys;
	}

	/**
	 * Returns the names of all the spot detectors known to this provider. The
	 * list share the same index than for {@link #getKeys()} and
	 * {@link #getInfoTexts()}.
	 *
	 * @return the detector names, as a list of strings.
	 */
	public List< String > getNames()
	{
		return names;
	}

	/**
	 * Returns the info texts of all the spot detectors known to this provider.
	 * The list share the same index than for {@link #getKeys()} and
	 * {@link #getNames()}.
	 *
	 * @return the detector info texts, as a list of strings.
	 */
	public List< String > getInfoTexts()
	{
		return infoTexts;
	}

}
