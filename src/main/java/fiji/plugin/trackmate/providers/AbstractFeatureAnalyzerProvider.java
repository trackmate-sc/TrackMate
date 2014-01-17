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

import fiji.plugin.trackmate.features.FeatureAnalyzer;

public abstract class AbstractFeatureAnalyzerProvider< K extends FeatureAnalyzer & SciJavaPlugin >
{
	/**
	 * The detector names, in the order they will appear in the GUI. These names
	 * will be used as keys to access relevant analyzer classes.
	 */
	protected List< String > names = new ArrayList< String >();

	protected Map< String, K > analyzers = new HashMap< String, K >();

	/**
	 * Registers the specified {@link FeatureAnalyzer} with the specified key.
	 * The analyzer is appended after all previously registered analyzers.
	 *
	 * @param key
	 *            a String used as a key for the analyzer.
	 * @param featureAnalyzer
	 *            the {@link FeatureAnalyzer} to register.
	 */
	private void registerAnalyzer( final String key, final K featureAnalyzer )
	{
		names.add( key );
		analyzers.put( key, featureAnalyzer );
	}

	/**
	 * Returns the instance of the target analyzer identified by the key
	 * parameter. If the key is unknown to this provider, <code>null</code> is
	 * returned.
	 */
	public K getFeatureAnalyzer( final String key )
	{
		return analyzers.get( key );
	}

	/**
	 * Returns a list of the analyzers keys available through this provider, in
	 * the order they were registered.
	 */
	public List< String > getAvailableFeatureAnalyzers()
	{
		return names;
	}

	/**
	 * Discovers the spot feature analyzer factories in the current application
	 * context and registers them in this provider.
	 */
	protected void registerFeatureAnalyzers( final Class< K > cl )
	{
		final Context context = new Context( LogService.class, PluginService.class );
		final LogService log = context.getService( LogService.class );
		final PluginService pluginService = context.getService( PluginService.class );
		final List< PluginInfo< K >> infos = pluginService.getPluginsOfType( cl );

		final Comparator< PluginInfo< K >> priorityComparator = new Comparator< PluginInfo< K > >()
		{
			@Override
			public int compare( final PluginInfo< K > o1, final PluginInfo< K > o2 )
			{
				return o1.getPriority() > o2.getPriority() ? 1 : o1.getPriority() < o2.getPriority() ? -1 : 0;
			}
		};

		Collections.sort( infos, priorityComparator );

		for ( final PluginInfo< K > info : infos )
		{
			if ( !info.isEnabled() )
			{
				continue;
			}
			try
			{
				final K analyzer = info.createInstance();
				registerAnalyzer( analyzer.getKey(), analyzer );
			}
			catch ( final InstantiableException e )
			{
				log.error( "Could not instantiate " + info.getClassName(), e );
			}
		}
	}
}
