package fiji.plugin.trackmate.gui.editor.labkit.component;

import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEYMAP_HOME;
import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEY_CONFIG_SCOPE;

import org.scijava.Context;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionsBuilder;

import bdv.KeyConfigScopes;
import bdv.ui.keymap.KeymapManager;
import fiji.plugin.trackmate.util.TMUtils;

public class TMKeymapManager extends KeymapManager
{

	public TMKeymapManager()
	{
		super( KEYMAP_HOME );
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
	}
}
