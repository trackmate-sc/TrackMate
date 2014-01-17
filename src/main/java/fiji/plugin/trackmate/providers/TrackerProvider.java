package fiji.plugin.trackmate.providers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.log.LogService;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.plugin.SciJavaPlugin;

import fiji.plugin.trackmate.tracking.SpotTrackerFactory;

public class TrackerProvider
{
	/*
	 * BLANK CONSTRUCTOR
	 */

	protected Map< String, SpotTrackerFactory > implementations;

	protected List< String > keys;

	protected List< String > visibleKeys;

	protected List< String > disabled;

	/**
	 * This provider provides the GUI with the spot tracker factories currently
	 * available in the current TrackMate version. Each tracker factory is
	 * identified by a key String, which can be used to retrieve new instance of
	 * the factory.
	 * <p>
	 * If you want to add custom detectors to TrackMate GUI, a simple way is to
	 * make a {@link SpotTrackerFactory} class, and annotate it with using the
	 * {@link SciJavaPlugin} framework. It will then be automatically discovered
	 * by this class.
	 */
	public TrackerProvider() {
		registerModules();
	}


	/*
	 * METHODS
	 */

	/**
	 * Discovers the detector factories in the current application context and
	 * registers them in this provider.
	 */
	protected void registerModules()
	{
		implementations = new HashMap< String, SpotTrackerFactory >();

		final Context context = new Context( LogService.class, PluginService.class );
		final LogService log = context.getService( LogService.class );
		final PluginService pluginService = context.getService( PluginService.class );
		final List< PluginInfo< SpotTrackerFactory >> infos = pluginService.getPluginsOfType( SpotTrackerFactory.class );

		// Sort by increasing priority.
		final Comparator< PluginInfo< SpotTrackerFactory >> priorityComparator = new Comparator< PluginInfo< SpotTrackerFactory > >()
		{
			@Override
			public int compare( final PluginInfo< SpotTrackerFactory > o1, final PluginInfo< SpotTrackerFactory > o2 )
			{
				return o1.getPriority() > o2.getPriority() ? 1 : o1.getPriority() < o2.getPriority() ? -1 : 0;
			}
		};
		Collections.sort( infos, priorityComparator );

		keys = new ArrayList< String >( implementations.size() );
		visibleKeys = new ArrayList< String >( implementations.size() );
		disabled = new ArrayList< String >( implementations.size() );
		for ( final PluginInfo< SpotTrackerFactory > info : infos )
		{
			// Discard non enabled ones.
			if ( !info.isEnabled() )
			{
				disabled.add( info.getClassName() );
				continue;
			}
			try
			{
				final SpotTrackerFactory factory = info.createInstance();
				implementations.put( factory.getKey(), factory );
				keys.add( factory.getKey() );
				// Separately store the key of those marked as visible
				if ( info.isVisible() )
				{
					visibleKeys.add( factory.getKey() );
				}
			}
			catch ( final InstantiableException e )
			{
				log.error( "Could not instantiate " + info.getClassName(), e );
			}
		}

		if ( implementations.size() < 1 )
		{
			log.error( "Could not find any factory.\n" );
		}
	}

	/**
	 * Returns a new instance of the target factory identified by the key
	 * parameter.
	 * <p>
	 * Querying twice the same factory with this method returns the same
	 * instance.
	 *
	 * @return the desired {@link SpotTrackerFactory} instance.
	 */
	public SpotTrackerFactory getFactory( final String key )
	{
		final SpotTrackerFactory factory = implementations.get( key );
		return factory;
	}

	/**
	 * Returns the keys of all the spot tracker factories known to this
	 * provider.
	 *
	 * @return the keys, as a new list of strings, ordered by priority.
	 */
	public List< String > getKeys()
	{
		return new ArrayList< String >( keys );
	}

	/**
	 * Returns the keys of all the spot tracker factories known to this provider
	 * and marked as visible.
	 *
	 * @return the keys, as a new list of strings, ordered by priority.
	 */
	public List< String > getVisibleKeys()
	{
		return new ArrayList< String >( visibleKeys );
	}

	public List< String > getDisabledClasses()
	{
		return disabled;
	}

	/*
	 * MAIN
	 */

	public static void main( final String[] args )
	{
		final TrackerProvider provider = new TrackerProvider();
		System.out.println( "Discovered modules:" );
		System.out.println( "  Enabled:" );
		System.out.println( "    Visible:" );
		final List< String > allKeys = provider.getKeys();
		for ( final String key : provider.getVisibleKeys() )
		{
			System.out.println( "      - " + key + "\t-->\t" + provider.getFactory( key ).getName() );
		}
		allKeys.removeAll( provider.getVisibleKeys() );
		System.out.println( "    Not visible:" );
		for ( final String key : allKeys )
		{
			System.out.println( "      - " + key + "\t-->\t" + provider.getFactory( key ).getName() );
		}
		System.out.println( "  Disabled:" );
		for ( final String cn : provider.getDisabledClasses() )
		{
			System.out.println( "      - " + cn );
		}

	}
}
