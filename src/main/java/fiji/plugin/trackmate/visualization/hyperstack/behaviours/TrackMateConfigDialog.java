package fiji.plugin.trackmate.visualization.hyperstack.behaviours;

import static fiji.plugin.trackmate.visualization.ui.KeyConfigContexts.ALL_SPOTS_TABLE;
import static fiji.plugin.trackmate.visualization.ui.KeyConfigContexts.HYPERSTACK_DISPLAYER;
import static fiji.plugin.trackmate.visualization.ui.KeyConfigContexts.KEY_CONFIG_SCOPE;
import static fiji.plugin.trackmate.visualization.ui.KeyConfigContexts.TRACKMATE;
import static fiji.plugin.trackmate.visualization.ui.KeyConfigContexts.TRACKSCHEME;
import static fiji.plugin.trackmate.visualization.ui.KeyConfigContexts.TRACK_TABLE;

import java.awt.Frame;

import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.Actions;

import bdv.BigDataViewerActions;
import bdv.tools.CloseWindowActions;
import bdv.tools.PreferencesDialog;
import bdv.ui.keymap.Keymap;
import bdv.ui.keymap.KeymapManager;
import bdv.ui.keymap.KeymapSettingsPage;
import fiji.plugin.trackmate.gui.GuiUtils;

public class TrackMateConfigDialog
{

	public static void prefDialog( final Frame frame, final Keymap keymap, final KeymapManager keymapManager, final Actions actions )
	{
		final PreferencesDialog preferencesDialog = new PreferencesDialog( frame, keymap,
				new String[] { TRACKMATE, HYPERSTACK_DISPLAYER, TRACKSCHEME, ALL_SPOTS_TABLE, TRACK_TABLE } );
		GuiUtils.positionWindow( preferencesDialog, frame );
		BigDataViewerActions.toggleDialogAction( actions, preferencesDialog, BigDataViewerActions.PREFERENCES_DIALOG, BigDataViewerActions.PREFERENCES_DIALOG_KEYS );
		preferencesDialog.addPage( new KeymapSettingsPage( "Keymap", keymapManager, keymapManager.getCommandDescriptions() ) );
	}

	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KEY_CONFIG_SCOPE, TRACKMATE );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( CloseWindowActions.CLOSE_DIALOG, CloseWindowActions.CLOSE_DIALOG_KEYS, "Close the active dialog." );
			descriptions.add( BigDataViewerActions.PREFERENCES_DIALOG, BigDataViewerActions.PREFERENCES_DIALOG_KEYS, "Open the preferences dialog." );
		}
	}
}
