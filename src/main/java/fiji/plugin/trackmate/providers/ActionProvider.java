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

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.TrackMateActionFactory;
import fiji.plugin.trackmate.gui.TrackMateGUIController;

public class ActionProvider {

	protected List< String > keys = new ArrayList< String >();

	protected List< String > visibleKeys = new ArrayList< String >();

	protected Map< String, TrackMateActionFactory > factories = new HashMap< String, TrackMateActionFactory >();

	/*
	 * BLANK CONSTRUCTOR
	 */

	/**
	 * This provider provides the GUI with the TrackMate actions currently
	 * available in TrackMate. Each action is identified by a key String, which
	 * can be used to retrieve new instance of the action.
	 * <p>
	 * If you want to add custom actions to TrackMate, a simple way is to extend
	 * this factory so that it is registered with the custom actions and provide
	 * this extended factory to the {@link TrackMate} trackmate.
	 *
	 * @param trackmate
	 *            the {@link TrackMate} instance these actions will operate on.
	 * @param guiController
	 *            the {@link TrackMateGUIController} controller that controls
	 *            the GUI these actions are launched from.
	 */
	public ActionProvider() {
		registerActions();
	}

	/*
	 * METHODS
	 */

	/**
	 * Registers the standard actions shipped with TrackMate, and instantiates
	 * some of them.
	 */
	protected void registerActions() {
		final Context context = new Context( LogService.class, PluginService.class );
		final LogService log = context.getService( LogService.class );
		final PluginService pluginService = context.getService( PluginService.class );
		final List< PluginInfo< TrackMateActionFactory >> infos = pluginService.getPluginsOfType( TrackMateActionFactory.class );

		final Comparator< PluginInfo< TrackMateActionFactory >> priorityComparator = new Comparator< PluginInfo< TrackMateActionFactory > >()
		{
			@Override
			public int compare( final PluginInfo< TrackMateActionFactory > o1, final PluginInfo< TrackMateActionFactory > o2 )
			{
				return o1.getPriority() > o2.getPriority() ? 1 : o1.getPriority() < o2.getPriority() ? -1 : 0;
			}
		};

		Collections.sort( infos, priorityComparator );

		for ( final PluginInfo< TrackMateActionFactory > info : infos )
		{
			try
			{
				final TrackMateActionFactory factory = info.createInstance();
				register( factory.getKey(), factory, info.isVisible() );
			}
			catch ( final InstantiableException e )
			{
				log.error( "Could not instantiate " + info.getClassName(), e );
			}
		}
	}

	private void register( final String key, final TrackMateActionFactory action, final boolean visible )
	{
		keys.add( key );
		factories.put( key, action );
		if ( visible )
		{
			visibleKeys.add( key );
		}
	}

	public TrackMateActionFactory getFactory( final String key )
	{
		return factories.get( key );
	}

	public List< String > getAvailableActions()
	{
		return keys;
	}

	public List< String > getVisibleActions()
	{
		return visibleKeys;
	}
}
