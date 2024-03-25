/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
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

import fiji.plugin.trackmate.TrackMateModule;
import fiji.plugin.trackmate.util.TMUtils;

public abstract class AbstractProvider< K extends TrackMateModule >
{
	private final Class< K > cl;

	public AbstractProvider( final Class< K > cl )
	{
		this.cl = cl;
		registerModules();
	}

	protected List< String > keys;

	protected List< String > visibleKeys;

	protected List< String > disabled;

	protected Map< String, K > implementations;

	private void registerModules()
	{
		final Context context = TMUtils.getContext();
		final LogService log = context.getService( LogService.class );
		final PluginService pluginService = context.getService( PluginService.class );
		final List< PluginInfo< K > > infos = pluginService.getPluginsOfType( cl );

		keys = new ArrayList<>( infos.size() );
		visibleKeys = new ArrayList<>( infos.size() );
		disabled = new ArrayList<>( infos.size() );
		implementations = new HashMap<>();

		for ( final PluginInfo< K > info : infos )
		{
			if ( !info.isEnabled() )
			{
				disabled.add( info.getClassName() );
				continue;
			}
			try
			{
				final K implementation = info.createInstance();
				final String key = implementation.getKey();

				implementations.put( key, implementation );
				keys.add( key );
				if ( info.isVisible() )
					visibleKeys.add( key );

			}
			catch ( final InstantiableException e )
			{
				log.error( "Could not instantiate " + info.getClassName(), e );
			}
		}
	}

	public List< String > getKeys()
	{
		return new ArrayList<>( keys );
	}

	public List< String > getVisibleKeys()
	{
		return new ArrayList<>( visibleKeys );
	}

	public List< String > getDisabled()
	{
		return new ArrayList<>( disabled );
	}

	public K getFactory( final String key )
	{
		return implementations.get( key );
	}

	public String echo()
	{
		final StringBuilder str = new StringBuilder();
		str.append( "Discovered modules for " + cl.getSimpleName() + ":\n" );
		str.append( "  Enabled & visible:" );
		if ( getVisibleKeys().isEmpty() )
		{
			str.append( " none.\n" );
		}
		else
		{
			str.append( '\n' );
			for ( final String key : getVisibleKeys() )
				str.append( "  - " + key + "\t-->\t" + getFactory( key ).getName() + '\n' );
		}
		str.append( "  Enabled & not visible:" );
		final List< String > invisibleKeys = getKeys();
		invisibleKeys.removeAll( getVisibleKeys() );
		if ( invisibleKeys.isEmpty() )
		{
			str.append( " none.\n" );
		}
		else
		{
			str.append( '\n' );
			for ( final String key : invisibleKeys )
				str.append( "  - " + key + "\t-->\t" + getFactory( key ).getName() + '\n' );
		}
		str.append( "  Disabled:" );
		if ( getDisabled().isEmpty() )
		{
			str.append( " none.\n" );
		}
		else
		{
			str.append( '\n' );
			for ( final String cn : getDisabled() )
				str.append( "  - " + cn + '\n' );
		}
		return str.toString();
	}
}
