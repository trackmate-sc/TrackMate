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
package fiji.plugin.trackmate;

import java.lang.reflect.Constructor;
import java.util.Map;

import fiji.plugin.trackmate.util.TMUtils;

/**
 * Base interface for TrackMate factories, like spot detector and tracker
 * factories.
 * <p>
 * Ships methods with default implementations for loading/saving the settings
 * parameters from/to XML.
 */
public interface TrackMateFactoryBase< F extends TrackMateFactoryBase< F > > extends TrackMateModule
{

	/**
	 * Returns a new default settings map suitable for the target detector.
	 * Settings are instantiated with default values.
	 *
	 * @return a new map.
	 */
	public Map< String, Object > getDefaultSettings();

	/**
	 * Check that the given settings map is suitable for target detector.
	 *
	 * @param settings
	 *            the map to test.
	 * @return an error message if the settings are unsuitable. Otherwise
	 *         returns <code>null</code>.
	 */
	public default String checkSettings( final Map< String, Object > settings )
	{
		return TMUtils.checkSettings( settings, getDefaultSettings() );
	}

	/**
	 * A utility method that builds a string representation of a settings map
	 * owing to the currently selected tracker in this provider.
	 *
	 * @param sm
	 *            the map to echo.
	 * @return a string representation of the map.
	 */
	public default String toString( final Map< String, Object > sm )
	{
		final String error = checkSettings( sm );
		if ( null != error )
			return error;
		return TMUtils.echoMap( sm, 0 );
	}

	/**
	 * Returns a copy the current instance.
	 *
	 * @return a new instance of this factory.
	 */
	public default F copy()
	{
		// We use reflection for this.
		try
		{
			final Class< ? > clazz = this.getClass();
			final Constructor< ? > constructor = clazz.getDeclaredConstructor();
			@SuppressWarnings( "unchecked" )
			final F copy = ( F ) constructor.newInstance();
			return copy;
		}
		catch ( final ReflectiveOperationException e )
		{
			e.printStackTrace();
			throw new RuntimeException( "Could not copy the factory: " + e.getMessage(), e );
		}
	}
}
