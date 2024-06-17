package fiji.plugin.trackmate.util.cli;

import java.util.Arrays;
import java.util.Map;

import fiji.plugin.trackmate.gui.displaysettings.StyleElements.BooleanElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.BoundedDoubleElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.DoubleElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.EnumElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.FeatureElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.IntElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.StringElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.StyleElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.StyleElementVisitor;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.Command;

public class TrackMateSettingsBuilder
{


	private static class MyStyleElementVisitor implements StyleElementVisitor
	{

		private final Map< String, Object > settings;

		public MyStyleElementVisitor( final Map< String, Object > settings )
		{
			this.settings = settings;
		}

		@Override
		public void visit( final BooleanElement el )
		{
			settings.put( el.getLabel(), el.get() );
		}

		@Override
		public void visit( final BoundedDoubleElement el )
		{
			settings.put( el.getLabel(), el.get() );
		}

		@Override
		public void visit( final DoubleElement el )
		{
			settings.put( el.getLabel(), el.get() );
		}

		@Override
		public < E > void visit( final EnumElement< E > el )
		{
			settings.put( el.getLabel(), el.getValue() );
		}

		@Override
		public void visit( final FeatureElement el )
		{
			settings.put( el.getLabel(), el.getFeature() );
		}

		@Override
		public void visit( final IntElement el )
		{
			settings.put( el.getLabel(), el.get() );
		}

		@Override
		public void visit( final StringElement el )
		{
			settings.put( el.getLabel(), el.get() );
		}

	}

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
	public static void toTrackMateSettings( final Map< String, Object > settings, final CLIConfigurator cli, final StyleElement elements )
	{
		toMap( cli.getExecutableArg(), settings );
		cli.getArguments().forEach( arg -> toMap( arg, settings ) );
		Arrays.asList( elements ).forEach( el -> el.accept( new MyStyleElementVisitor( settings ) ) );
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
		fromMap( settings, cli.getExecutableArg() );
		cli.getArguments().forEach( arg -> fromMap( settings, arg ) );
	}
}
