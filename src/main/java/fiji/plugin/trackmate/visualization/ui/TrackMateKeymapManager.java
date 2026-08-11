package fiji.plugin.trackmate.visualization.ui;

import java.io.File;

import org.scijava.Context;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionsBuilder;

import bdv.ui.keymap.KeymapManager;
import fiji.plugin.trackmate.util.TMUtils;

public class TrackMateKeymapManager extends KeymapManager
{

	private static final String KEYMAP_HOME = new File( System.getProperty( "user.home" ), ".trackmate" ).getAbsolutePath();

	public TrackMateKeymapManager()
	{
		super( KEYMAP_HOME );
	}

	@Override
	public synchronized void discoverCommandDescriptions()
	{
		final CommandDescriptionsBuilder builder = new CommandDescriptionsBuilder();
		final Context context = TMUtils.getContext();
		context.inject( builder );
		builder.discoverProviders(
				KeyConfigContexts.KEY_CONFIG_SCOPE, // TrackMate scope
				bvv.core.KeyConfigScopes.BIGVOLUMEVIEWER, // BVV
				bdv.KeyConfigScopes.BIGDATAVIEWER ); // BDV, required by BVV
		setCommandDescriptions( builder.build() );
	}
}
