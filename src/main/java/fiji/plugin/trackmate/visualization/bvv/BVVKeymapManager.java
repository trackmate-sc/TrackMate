package fiji.plugin.trackmate.visualization.bvv;

import java.io.File;

import org.scijava.Context;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionsBuilder;

import bdv.ui.keymap.KeymapManager;
import fiji.plugin.trackmate.util.TMUtils;

/**
 * Keymap manager for BigVolumeViewer (BVV) actions in TrackMate.
 * <p>
 * This manager is separate from the global TrackMate keymap and handles
 * BVV-specific key bindings (navigation, rotation, zoom, etc.).
 * It uses its own subdirectory to avoid conflicts with other keymap managers.
 */
public class BVVKeymapManager extends KeymapManager
{

	private static final String KEYMAP_HOME = new File(
			new File( System.getProperty( "user.home" ), ".trackmate" ), "bvv"
	).getAbsolutePath();

	public BVVKeymapManager()
	{
		super( KEYMAP_HOME );
	}

	@Override
	public synchronized void discoverCommandDescriptions()
	{
		final CommandDescriptionsBuilder builder = new CommandDescriptionsBuilder();
		final Context context = TMUtils.getContext();
		context.inject( builder );
		// Discover BVV-specific command descriptions
		builder.discoverProviders(
				bvv.core.KeyConfigScopes.BIGVOLUMEVIEWER,
				bdv.KeyConfigScopes.BIGDATAVIEWER );
		setCommandDescriptions( builder.build() );
	}
}
