/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2025 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.gui.editor.labkit.component;

import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEY_CONFIG_CONTEXT;
import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEY_CONFIG_SCOPE;

import javax.swing.SwingUtilities;

import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.InputActionBindings;

import bdv.BigDataViewerActions;
import bdv.tools.CloseWindowActions;
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


	private static final String RESET_VIEW = "reset view";
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
			descriptions.add( BigDataViewerActions.PREFERENCES_DIALOG, BigDataViewerActions.PREFERENCES_DIALOG_KEYS, "Show the Preferences dialog." );
			descriptions.add( CloseWindowActions.CLOSE_DIALOG, CloseWindowActions.CLOSE_DIALOG_KEYS, "Close the active dialog." );
		}
	}
}
