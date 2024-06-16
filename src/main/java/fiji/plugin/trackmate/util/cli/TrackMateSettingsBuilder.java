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

	public static final void toTrackMateSettings( final CLIConfigurator cli, final Map< String, Object > settings )
	{
		toMap( cli.getExecutableArg(), settings );
		cli.getArguments().forEach( arg -> toMap( arg, settings ) );
	}

	public static final void fromTrackMateSettings( final Map< String, Object > settings, final CLIConfigurator cli )
	{
		fromMap( settings, cli.getExecutableArg() );
		cli.getArguments().forEach( arg -> fromMap( settings, arg ) );
	}
}
