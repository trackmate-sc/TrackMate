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

import fiji.plugin.trackmate.visualization.ViewFactory;

public class ViewProvider
{
	/**
	 * The view keys, in the order they will appear in the GUI.
	 */
	protected List< String > keys = new ArrayList< >();

	protected List< String > visibleKeys = new ArrayList< >();

	protected Map< String, ViewFactory > factories = new HashMap< >();

	/*
	 * BLANK CONSTRUCTOR
	 */

	/**
	 * This provider provides the GUI with the model views currently available
	 * in the TrackMate trackmate. Each view is identified by a key String,
	 * which can be used to retrieve new instance of the view.
	 * <p>
	 * If you want to add custom views to TrackMate, a simple way is to extend
	 * this factory so that it is registered with the custom views and provide
	 * this extended factory to the {@link fiji.plugin.trackmate.TrackMate}
	 * trackmate.
	 */
	public ViewProvider()
	{
		registerViews();
	}

	private void registerView( final String key, final ViewFactory view, final boolean visible )
	{
		keys.add( key );
		factories.put( key, view );
		if ( visible )
		{
			visibleKeys.add( key );
		}
	}

	public ViewFactory getFactory( final String key )
	{
		return factories.get( key );
	}

	public List< String > getAvailableViews()
	{
		return keys;
	}

	public List< String > getVisibleViews()
	{
		return visibleKeys;
	}

	protected void registerViews()
	{
		final Context context = new Context( LogService.class, PluginService.class );
		final LogService log = context.getService( LogService.class );
		final PluginService pluginService = context.getService( PluginService.class );
		final List< PluginInfo< ViewFactory > > infos = pluginService.getPluginsOfType( ViewFactory.class );

		final Comparator< PluginInfo< ViewFactory > > priorityComparator = new Comparator< PluginInfo< ViewFactory > >()
		{
			@Override
			public int compare( final PluginInfo< ViewFactory > o1, final PluginInfo< ViewFactory > o2 )
			{
				return o1.getPriority() > o2.getPriority() ? 1 : o1.getPriority() < o2.getPriority() ? -1 : 0;
			}
		};

		Collections.sort( infos, priorityComparator );

		for ( final PluginInfo< ViewFactory > info : infos )
		{
			try
			{
				final ViewFactory view = info.createInstance();
				registerView( view.getKey(), view, info.isVisible() );
			}
			catch ( final InstantiableException e )
			{
				log.error( "Could not instantiate " + info.getClassName(), e );
			}
		}
	}
}
