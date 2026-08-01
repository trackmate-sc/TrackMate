package fiji.plugin.trackmate.visualization.ui;

import org.scijava.Context;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionsBuilder;

import bdv.ui.keymap.KeymapManager;
import fiji.plugin.trackmate.util.TMUtils;

public class TrackMateKeymapManager extends KeymapManager
{

	@Override
	public synchronized void discoverCommandDescriptions()
	{
		final CommandDescriptionsBuilder builder = new CommandDescriptionsBuilder();
		final Context context = TMUtils.getContext();
		context.inject( builder );
		builder.discoverProviders( KeyConfigContexts.KEY_CONFIG_SCOPE );
		setCommandDescriptions( builder.build() );
	}
}
