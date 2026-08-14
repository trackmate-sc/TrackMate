/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
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

import java.awt.Font;
import java.awt.Frame;

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
import bdv.viewer.ViewerPanel;
import bdv.viewer.animate.MessageOverlayAnimator;
import fiji.plugin.trackmate.gui.editor.labkit.model.TMLabKitModel;
import fiji.plugin.trackmate.gui.editor.labkit.model.TMTransformationModel;
import fiji.plugin.trackmate.gui.editor.labkit.model.UndoRedoStack;
import net.imglib2.Interval;

public class TMLabKitActions
{

	public static void install(
			final Actions actions,
			final TMLabKitModel model,
			final Frame frame,
			final ViewerPanel viewerPanel,
			final InputActionBindings keybindings,
			final KeymapManager keymapManager,
			final AppearanceManager appearanceManager )
	{
		final Keymap keymap = keymapManager.getForwardSelectedKeymap();

		/*
		 * Configure preferences dialog.
		 */

		final PreferencesDialog preferencesDialog = new PreferencesDialog( frame, keymap, new String[] { KEY_CONFIG_CONTEXT } );
		preferencesDialog.setTitle( "Segmentation editor Preferences" );
		fiji.plugin.trackmate.gui.GuiUtils.positionWindow( preferencesDialog, frame );
		BigDataViewerActions.toggleDialogAction( actions, preferencesDialog, BigDataViewerActions.PREFERENCES_DIALOG, BigDataViewerActions.PREFERENCES_DIALOG_KEYS );

		preferencesDialog.addPage( new KeymapSettingsPage( "Editor keymap", keymapManager, keymapManager.getCommandDescriptions() ) );
		preferencesDialog.addPage( new AppearanceSettingsPage( "BDVs appearance", appearanceManager ) );
		appearanceManager.appearance().updateListeners().add( frame::repaint );
		SwingUtilities.invokeLater( () -> appearanceManager.updateLookAndFeel() );

		/*
		 * View actions
		 */

		final TMTransformationModel transformationModel = model.imageLabelingModel().transformationModel();
		actions.runnableAction( () -> transformationModel.resetView(), RESET_VIEW, RESET_VIEW_KEYS );

		/*
		 * Undo / redo actions
		 */

		final boolean hasTime = model.imageLabelingModel().isTimeSeries();
		final MessageOverlayAnimator messages = new MessageOverlayAnimator( 2000, 0.01, 0.2, new Font( "Arial", Font.PLAIN, 12 ) );
		viewerPanel.addOverlayAnimator( messages );
		final UndoRedoStack undoRedo = model.imageLabelingModel().undoRedo();
		actions.runnableAction( () -> undo( undoRedo, messages, hasTime ), UNDO, UNDO_KEYS );
		actions.runnableAction( () -> redo( undoRedo, messages, hasTime ), REDO, REDO_KEYS );
	}

	private static final void undo( final UndoRedoStack undoRedo, final MessageOverlayAnimator messages, final boolean hasTime )
	{
		final Interval interval = undoRedo.undo();
		if ( interval == null )
			return;
		messages.add( "Undo" + undoRedoMsg( interval, hasTime ) );
	}

	private static final void redo( final UndoRedoStack undoRedo, final MessageOverlayAnimator messages, final boolean hasTime )
	{
		final Interval interval = undoRedo.redo();
		if ( interval == null )
			return;
		messages.add( "Redo" + undoRedoMsg( interval, hasTime ) );
	}

	private static final String undoRedoMsg( final Interval interval, final boolean hasTime )
	{
		String out = ( hasTime )
				? " at frame " + interval.min( interval.numDimensions() - 1 ) + " @ "
				: " @ ";
		out += "[" + interval.min( 0 );
		for ( int i = 1; i < interval.numDimensions() - 1; i++ )
			out += ", " + interval.min( i );
		out += "] → [" + interval.max( 0 );
		for ( int i = 1; i < interval.numDimensions() - 1; i++ )
			out += ", " + interval.max( i );
		out += "]";
		return out;
	}

	private static final String RESET_VIEW = "reset view";

	private static final String[] RESET_VIEW_KEYS = new String[] { "shift R " };

	private static final String UNDO = "undo";

	private static final String[] UNDO_KEYS = new String[] { "ctrl Z", "meta Z" };

	private static final String REDO = "redo";

	private static final String[] REDO_KEYS = new String[] { "ctrl shift Z", "meta shift Z" };

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
			descriptions.add( UNDO, UNDO_KEYS, "Undo the last edit." );
			descriptions.add( REDO, REDO_KEYS, "Redo the last undone edit." );
		}
	}
}
