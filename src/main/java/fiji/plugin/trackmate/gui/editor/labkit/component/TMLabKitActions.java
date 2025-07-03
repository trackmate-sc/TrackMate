package fiji.plugin.trackmate.gui.editor.labkit.component;

import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEY_CONFIG_CONTEXT;
import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEY_CONFIG_SCOPE;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.InputActionBindings;

import bdv.BigDataViewerActions;
import bdv.tools.PreferencesDialog;
import bdv.ui.appearance.AppearanceManager;
import bdv.ui.appearance.AppearanceSettingsPage;
import bdv.ui.keymap.Keymap;
import bdv.ui.keymap.KeymapManager;
import bdv.ui.keymap.KeymapSettingsPage;
import fiji.plugin.trackmate.gui.editor.labkit.model.TMLabKitModel;
import fiji.plugin.trackmate.gui.editor.labkit.model.TMTransformationModel;

public class TMLabKitActions
{

	public static void install(
			final Actions actions,
			final TMLabKitModel model,
			final TMLabKitFrame frame,
			final InputActionBindings keybindings,
			final KeymapManager keymapManager,
			final AppearanceManager appearanceManager )
	{
		final Keymap keymap = keymapManager.getForwardSelectedKeymap();

		/*
		 * Configure preferences dialog.
		 */

		final PreferencesDialog preferencesDialog = new PreferencesDialog( frame, keymap, new String[] { KEY_CONFIG_CONTEXT } );
		fiji.plugin.trackmate.gui.GuiUtils.positionWindow( preferencesDialog, frame );
		SwingUtilities.replaceUIActionMap( preferencesDialog.getRootPane(), keybindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap( preferencesDialog.getRootPane(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );
		BigDataViewerActions.toggleDialogAction( actions, preferencesDialog, TOGGLE_PREFERENCES_DIALOG, TOGGLE_PREFERENCES_DIALOG_KEYS );

		preferencesDialog.addPage( new KeymapSettingsPage( "Keymap", keymapManager, keymapManager.getCommandDescriptions() ) );
		preferencesDialog.addPage( new AppearanceSettingsPage( "Appearance", appearanceManager ) );
		appearanceManager.appearance().updateListeners().add( frame::repaint );
		SwingUtilities.invokeLater( () -> appearanceManager.updateLookAndFeel() );

		/*
		 * View actions
		 */

		final TMTransformationModel transformationModel = ( TMTransformationModel ) model.imageLabelingModel().transformationModel();
		actions.runnableAction( () -> transformationModel.resetView(), RESET_VIEW, RESET_VIEW_KEYS );
	}

	private static final String TOGGLE_PREFERENCES_DIALOG = "preferences";
	private static final String CLOSE_PREFERENCES_DIALOG = "close dialog window";
	private static final String RESET_VIEW = "reset view";

	private static final String[] TOGGLE_PREFERENCES_DIALOG_KEYS = new String[] { "ctrl COMMA" };
	private static final String[] CLOSE_PREFERENCES_DIALOG_KEYS = new String[] { "ESCAPE" };
	private static final String[] RESET_VIEW_KEYS =  new String[] { "shift R "};

	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KEY_CONFIG_SCOPE, KEY_CONFIG_CONTEXT );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( TOGGLE_PREFERENCES_DIALOG, TOGGLE_PREFERENCES_DIALOG_KEYS, "Toggle the Preferences dialog." );
			descriptions.add( CLOSE_PREFERENCES_DIALOG, CLOSE_PREFERENCES_DIALOG_KEYS, "Close the Preferences dialog." );
			descriptions.add( RESET_VIEW, RESET_VIEW_KEYS, "Reset the view." );
		}
	}
}
