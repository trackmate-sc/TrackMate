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

import java.util.Map;

import fiji.plugin.trackmate.util.cli.CLIConfigurator.Argument;

public class TrackMateSettingsBuilder
{

	private TrackMateSettingsBuilder()
	{}

	private static void toMap( final Argument< ?, ? > arg, final Map< String, Object > settings )
	{
		if ( arg.getKey() != null )
			settings.put( arg.getKey(), arg.getValueObject() );
	}

	private static void fromMap( final Map< String, Object > settings, final Argument< ?, ? > arg )
	{
		final Object val = settings.get( arg.getKey() );
		if ( val != null )
			arg.setValueObject( val );
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
		cli.getArguments().forEach( arg -> toMap( arg, settings ) );
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
		cli.getArguments().forEach( arg -> fromMap( settings, arg ) );
	}
}
