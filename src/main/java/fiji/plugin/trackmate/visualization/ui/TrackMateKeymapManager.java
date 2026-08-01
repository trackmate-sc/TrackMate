package fiji.plugin.trackmate.visualization.ui;

import org.scijava.Context;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider.Scope;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionsBuilder;

import bdv.ui.keymap.KeymapManager;
import fiji.plugin.trackmate.util.TMUtils;

public class TrackMateKeymapManager extends KeymapManager
{

	public static final Scope KEY_CONFIG_SCOPE = new Scope( "TrackMate" );

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
