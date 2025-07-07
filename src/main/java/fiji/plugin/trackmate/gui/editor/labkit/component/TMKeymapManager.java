package fiji.plugin.trackmate.gui.editor.labkit.component;

import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEYMAP_HOME;
import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEY_CONFIG_SCOPE;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.scijava.Context;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionsBuilder;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

import bdv.ui.keymap.Keymap;
import bdv.ui.keymap.KeymapManager;
import fiji.plugin.trackmate.util.TMUtils;

public class TMKeymapManager extends KeymapManager
{

	private static final String DEFAULT_KEYMAP_PATH = "/keymaps/Default-BDV.yaml";

	public TMKeymapManager()
	{
		super( KEYMAP_HOME );
	}

	static Keymap loadBDVKeymap()
	{
		final InputStream inputStream = TMKeymapManager.class.getResourceAsStream( DEFAULT_KEYMAP_PATH );

		if ( inputStream == null )
		{
			System.out.println(
					"Critical error: Required keymap file not found: " + DEFAULT_KEYMAP_PATH +
							"\nThis indicates a corrupted installation or missing resource file." +
							"\nExpected location in JAR: " + DEFAULT_KEYMAP_PATH );
			return null;
		}

		try (InputStreamReader reader = new InputStreamReader( inputStream ))
		{
			return new Keymap( "Default", new InputTriggerConfig( YamlConfigIO.read( reader ) ) );
		}
		catch ( final IOException e )
		{
			System.err.println( "Failed to load the default keymap in " + DEFAULT_KEYMAP_PATH );
			System.err.println( "Using builtin keymap. Error was:" );
			e.printStackTrace();
		}
		return null;
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
		setCommandDescriptions( builder.build() );
	}
}
