package fiji.plugin.trackmate.gui.editor.labkit.component;

import static bdv.BigDataViewerActions.BRIGHTNESS_SETTINGS;
import static bdv.BigDataViewerActions.COLLAPSE_CARDS;
import static bdv.BigDataViewerActions.COLLAPSE_CARDS_KEYS;
import static bdv.BigDataViewerActions.CROP;
import static bdv.BigDataViewerActions.CROP_KEYS;
import static bdv.BigDataViewerActions.EXPAND_CARDS;
import static bdv.BigDataViewerActions.EXPAND_CARDS_KEYS;
import static bdv.BigDataViewerActions.GO_TO_BOOKMARK;
import static bdv.BigDataViewerActions.GO_TO_BOOKMARK_ROTATION;
import static bdv.BigDataViewerActions.LOAD_SETTINGS;
import static bdv.BigDataViewerActions.LOAD_SETTINGS_KEYS;
import static bdv.BigDataViewerActions.MANUAL_TRANSFORM;
import static bdv.BigDataViewerActions.PREFERENCES_DIALOG;
import static bdv.BigDataViewerActions.PREFERENCES_DIALOG_KEYS;
import static bdv.BigDataViewerActions.RECORD_MAX_PROJECTION_MOVIE;
import static bdv.BigDataViewerActions.RECORD_MAX_PROJECTION_MOVIE_KEYS;
import static bdv.BigDataViewerActions.RECORD_MOVIE;
import static bdv.BigDataViewerActions.RECORD_MOVIE_KEYS;
import static bdv.BigDataViewerActions.SAVE_SETTINGS;
import static bdv.BigDataViewerActions.SAVE_SETTINGS_KEYS;
import static bdv.BigDataViewerActions.SET_BOOKMARK;
import static bdv.BigDataViewerActions.SHOW_HELP;
import static bdv.BigDataViewerActions.SHOW_HELP_KEYS;
import static bdv.BigDataViewerActions.VISIBILITY_AND_GROUPING;
import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEY_CONFIG_CONTEXT;
import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEY_CONFIG_SCOPE;

import javax.swing.SwingUtilities;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.InputActionBindings;

import bdv.BigDataViewerActions;
import bdv.KeyConfigContexts;
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

		final PreferencesDialog preferencesDialog = new PreferencesDialog( frame, keymap, new String[] { KEY_CONFIG_CONTEXT, KeyConfigContexts.BIGDATAVIEWER } );
		fiji.plugin.trackmate.gui.GuiUtils.positionWindow( preferencesDialog, frame );
//		SwingUtilities.replaceUIActionMap( preferencesDialog.getRootPane(), keybindings.getConcatenatedActionMap() );
//		SwingUtilities.replaceUIInputMap( preferencesDialog.getRootPane(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );
		BigDataViewerActions.toggleDialogAction( actions, preferencesDialog, BigDataViewerActions.PREFERENCES_DIALOG, BigDataViewerActions.PREFERENCES_DIALOG_KEYS );

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

	private static final String[] RESET_VIEW_KEYS = new String[] { "shift R " };

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
			descriptions.add( RESET_VIEW, RESET_VIEW_KEYS, "Reset the view." );
		}
	}

	/*
	 * BDV actions with custom default key bindings.
	 */
	@Plugin( type = CommandDescriptionProvider.class, priority = Priority.EXTREMELY_LOW )
	public static class DescriptionsBDV extends BigDataViewerActions.Descriptions
	{
		private static final String[] BRIGHTNESS_SETTINGS_KEYS 		= new String[] {"not mapped"};
		private static final String[] VISIBILITY_AND_GROUPING_KEYS 	= new String[] {"not mapped"};
		private static final String[] MANUAL_TRANSFORM_KEYS 		= new String[] {"ctrl T"};
		private static final String[] SET_BOOKMARK_KEYS 			= new String[] {"ctrl shift B"};
		private static final String[] GO_TO_BOOKMARK_KEYS 			= new String[] {"ctrl B"};
		private static final String[] GO_TO_BOOKMARK_ROTATION_KEYS 	= new String[] {"ctrl O"};

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( BRIGHTNESS_SETTINGS, BRIGHTNESS_SETTINGS_KEYS, "Show the Brightness&Colors dialog." );
			descriptions.add( VISIBILITY_AND_GROUPING, VISIBILITY_AND_GROUPING_KEYS, "Show the Visibility&Grouping dialog." );
			descriptions.add( SHOW_HELP, SHOW_HELP_KEYS, "Show the Help dialog." );
			descriptions.add( CROP, CROP_KEYS, "Show the Crop dialog." );
			descriptions.add( MANUAL_TRANSFORM, MANUAL_TRANSFORM_KEYS, "Toggle manual transformation mode." );
			descriptions.add( SAVE_SETTINGS, SAVE_SETTINGS_KEYS, "Save the BigDataViewer settings to a settings.xml file." );
			descriptions.add( LOAD_SETTINGS, LOAD_SETTINGS_KEYS, "Load the BigDataViewer settings from a settings.xml file." );
			descriptions.add( EXPAND_CARDS, EXPAND_CARDS_KEYS, "Expand and focus the BigDataViewer card panel" );
			descriptions.add( COLLAPSE_CARDS, COLLAPSE_CARDS_KEYS, "Collapse the BigDataViewer card panel" );
			descriptions.add( RECORD_MOVIE, RECORD_MOVIE_KEYS, "Show the Record Movie dialog." );
			descriptions.add( RECORD_MAX_PROJECTION_MOVIE, RECORD_MAX_PROJECTION_MOVIE_KEYS, "Show the Record Max Projection Movie dialog." );
			descriptions.add( SET_BOOKMARK, SET_BOOKMARK_KEYS, "Set a labeled bookmark at the current location." );
			descriptions.add( GO_TO_BOOKMARK, GO_TO_BOOKMARK_KEYS, "Retrieve a labeled bookmark location." );
			descriptions.add( GO_TO_BOOKMARK_ROTATION, GO_TO_BOOKMARK_ROTATION_KEYS, "Retrieve a labeled bookmark, set only the orientation." );
			descriptions.add( PREFERENCES_DIALOG, PREFERENCES_DIALOG_KEYS, "Show the Preferences dialog." );
		}
	}
}
