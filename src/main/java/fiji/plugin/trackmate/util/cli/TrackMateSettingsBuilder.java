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
package fiji.plugin.trackmate.util.cli;

import java.util.LinkedHashMap;
import java.util.Map;

import fiji.plugin.trackmate.util.cli.Configurator.Argument;
import fiji.plugin.trackmate.util.cli.Configurator.SelectableArguments;

public class TrackMateSettingsBuilder
{

	private TrackMateSettingsBuilder()
	{}

	private static void toMap( final Argument< ?, ? > arg, final Map< String, Object > settings )
	{
		if ( arg.getKey() != null )
			settings.put( arg.getKey(), arg.getValueObject() );
	}

	private static void toMap( final SelectableArguments selectable, final Map< String, Object > settings )
	{
		if ( selectable.getKey() != null )
			settings.put( selectable.getKey(), selectable.getSelection().getKey() );
	}

	private static void fromMap( final Map< String, Object > settings, final Argument< ?, ? > arg )
	{
		final Object val = settings.get( arg.getKey() );
		if ( val != null )
			arg.setValueObject( val );
	}

	private static void fromMap( final Map< String, Object > settings, final SelectableArguments selectable )
	{
		final Object val = settings.get( selectable.getKey() );
		if ( val != null )
			selectable.select( ( String ) val );
	}

	/**
	 * Serializes the specified CLI config and extra parameters to a TrackMate
	 * settings map.
	 *
	 * @param settings
	 *            the map to serialize settings to.
	 * @param cli
	 *            the CLI config.
	 */
	public static void toTrackMateSettings( final Map< String, Object > settings, final CLIConfigurator cli )
	{
		toMap( cli.getCommandArg(), settings );
		toTrackMateSettings( settings, ( Configurator ) cli );
	}

	/**
	 * Serializes the specified config and extra parameters to a TrackMate
	 * settings map.
	 *
	 * @param settings
	 *            the map to serialize settings to.
	 * @param config
	 *            the config.
	 */
	public static void toTrackMateSettings( final Map< String, Object > settings, final Configurator config )
	{
		config.getArguments().forEach( arg -> toMap( arg, settings ) );
		config.getSelectables().forEach( selectable -> toMap( selectable, settings ) );
	}


	/**
	 * Serializes the parameters stored in a TrackMate map to a CLI config
	 * object. The parameters in the map unknown to the CLI config are simply
	 * ignored.
	 *
	 * @param settings
	 *            the map to read parameters from.
	 * @param cli
	 *            the CLI config to write parameters into.
	 */
	public static final void fromTrackMateSettings( final Map< String, Object > settings, final CLIConfigurator cli )
	{
		fromMap( settings, cli.getCommandArg() );
		fromTrackMateSettings( settings, ( Configurator ) cli );
	}

	/**
	 * Serializes the parameters stored in a TrackMate map to a config object.
	 * The parameters in the map unknown to the config are simply ignored.
	 *
	 * @param settings
	 *            the map to read parameters from.
	 * @param config
	 *            the config to write parameters into.
	 */
	public static final void fromTrackMateSettings( final Map< String, Object > settings, final Configurator config )
	{
		config.getArguments().forEach( arg -> fromMap( settings, arg ) );
		config.getSelectables().forEach( selectable -> fromMap( settings, selectable ) );
	}

	/**
	 * Returns a map containing the default settings of the specified CLI
	 * configurator. The map will contain the keys of all arguments and
	 * selectable arguments, and their default values.
	 *
	 * @param config
	 *            the configurator.
	 * @return a new map containing the default settings.
	 */
	public static final Map< String, Object > getDefaultSettings( final Configurator config )
    {
		final Map< String, Object > settings = new LinkedHashMap< String, Object >();
		config.arguments.forEach( arg -> {
			final String key = arg.getKey();
			if ( key == null )
				return;

			if ( !arg.hasDefaultValue() )
				throw new IllegalArgumentException( "The argument '" + key + "' in the configurator " + config + " has no default value, which is required." );
			settings.put( key, arg.getDefaultValue() );
		} );
		config.selectables.forEach( sel -> {
			final String selKey = sel.getKey();
			if ( selKey == null )
				return;

			final String argKey = sel.getSelection().getKey();
			if ( argKey == null )
				throw new IllegalArgumentException( "The selectable argument '" + selKey + "' in the configurator " + config + " has no key, which is required." );
			settings.put( selKey, argKey );
		} );
		return settings;
	}
}
