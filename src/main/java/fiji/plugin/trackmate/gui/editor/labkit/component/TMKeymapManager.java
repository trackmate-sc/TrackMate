package fiji.plugin.trackmate.gui.editor.labkit.component;

import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEYMAP_HOME;
import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEY_CONFIG_SCOPE;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.scijava.Context;
import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.gui.Command;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionsBuilder;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

import bdv.KeyConfigScopes;
import bdv.ui.keymap.Keymap;
import bdv.ui.keymap.KeymapManager;
import fiji.plugin.trackmate.util.TMUtils;

public class TMKeymapManager extends KeymapManager
{

	public TMKeymapManager()
	{
		super( KEYMAP_HOME );
	}

	@Override
	protected List< Keymap > loadBuiltinStyles()
	{
		synchronized ( KeymapManager.class )
		{
			final List< Keymap > loadedBuiltinStyles = new ArrayList<>( 1 );
			final String filename = "../../../../../../../keymaps/Default.yaml";
			try (InputStreamReader reader = new InputStreamReader( TMKeymapManager.class.getResourceAsStream( filename ) ))
			{
				final Keymap km = new Keymap( "Default", new InputTriggerConfig( YamlConfigIO.read( reader ) ) );
				loadedBuiltinStyles.add( km );
			}
			catch ( final IOException e )
			{
				System.err.println( "Failed to load the default keymap in " + filename );
				System.err.println( "Using builtin keymap. Error was:" );
				e.printStackTrace();
				return super.getBuiltinStyles();
			}
			return loadedBuiltinStyles;
		}
	}

	/**
	 * Overloaded, so that we can restrict the scope to TrackMate-LabKit & BDV.
	 */
	@Override
	public synchronized void discoverCommandDescriptions()
	{
		final CommandDescriptionsBuilder builder = new CommandDescriptionsBuilder();
		final Context context = TMUtils.getContext();
		context.inject( builder );
		builder.discoverProviders( KEY_CONFIG_SCOPE );
		builder.discoverProviders( KeyConfigScopes.BIGDATAVIEWER );
		setCommandDescriptions( builder.build() );

		final Keymap keymap = getBuiltinStyles().get( 0 );
		final InputTriggerConfig config = keymap.getConfig();

		final CommandDescriptions cd = builder.build();
		final Map< Command, String > map = cd.createCommandDescriptionsMap();
		for ( final Command command : map.keySet() )
		{
			final Set< InputTrigger > inputs = config.getInputs( command.getName(), command.getContext() );
			System.out.println( command.getName() + " -> " + inputs + " -> " + map.get( command ) ); // DEBUG
		}

	}
}
