package fiji.plugin.trackmate.util.cli;

import java.util.Map;

import fiji.plugin.trackmate.util.cli.CLIConfigurator.Command;

public class TrackMateSettingsBuilder
{

	private TrackMateSettingsBuilder()
	{}

	private static void toMap( final Command< ? > arg, final Map< String, Object > settings )
	{
		if ( arg.getKey() != null )
			settings.put( arg.getKey(), arg.getValueObject() );
	}

	private static void fromMap( final Map< String, Object > settings, final Command< ? > arg )
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
	 * @param elements
	 *            the extra parameters as StyleElements.
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
