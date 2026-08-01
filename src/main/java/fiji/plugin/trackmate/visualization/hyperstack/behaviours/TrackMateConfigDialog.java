package fiji.plugin.trackmate.visualization.hyperstack.behaviours;

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

public class TrackMateConfigDialog
{

	public static void prefDialog( final Frame frame, final Keymap keymap, final KeymapManager keymapManager, final Actions actions )
	{
		final PreferencesDialog preferencesDialog = new PreferencesDialog( frame, keymap, new String[] { TrackMateImpBehaviour.KEY_CONFIG_CONTEXT } );
		fiji.plugin.trackmate.gui.GuiUtils.positionWindow( preferencesDialog, frame );
		BigDataViewerActions.toggleDialogAction( actions, preferencesDialog, BigDataViewerActions.PREFERENCES_DIALOG, BigDataViewerActions.PREFERENCES_DIALOG_KEYS );
		preferencesDialog.addPage( new KeymapSettingsPage( "Keymap", keymapManager, keymapManager.getCommandDescriptions() ) );
	}

	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( TrackMateImpBehaviour.KEY_CONFIG_SCOPE, TrackMateImpBehaviour.KEY_CONFIG_CONTEXT );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( CloseWindowActions.CLOSE_DIALOG, CloseWindowActions.CLOSE_DIALOG_KEYS, "Close the active dialog." );
			descriptions.add( BigDataViewerActions.PREFERENCES_DIALOG, BigDataViewerActions.PREFERENCES_DIALOG_KEYS, "Open the preferences dialog." );
		}
	}

}
