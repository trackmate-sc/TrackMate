package fiji.plugin.trackmate.gui.editor.labkit.component;

import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEY_CONFIG_CONTEXT;
import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEY_CONFIG_SCOPE;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.InputActionBindings;

import bdv.BigDataViewerActions;
import bdv.tools.InitializeViewerState;
import bdv.tools.PreferencesDialog;
import bdv.ui.appearance.AppearanceManager;
import bdv.ui.appearance.AppearanceSettingsPage;
import bdv.ui.keymap.Keymap;
import bdv.ui.keymap.KeymapManager;
import bdv.ui.keymap.KeymapSettingsPage;
import bdv.viewer.ViewerPanel;
import bdv.viewer.animate.SimilarityTransformAnimator;
import net.imglib2.realtransform.AffineTransform3D;

public class TMLabKitActions
{

	public static void install(
			final Actions actions,
			final ViewerPanel viewerPanel,
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

		actions.namedAction( new MyResetViewAction( viewerPanel ), RESET_VIEW_KEYS );
	}

	private static class MyResetViewAction extends AbstractNamedAction
	{

		private final ViewerPanel viewerPanel;

		public MyResetViewAction( final ViewerPanel viewerPanel )
		{
			super( RESET_VIEW );
			this.viewerPanel = viewerPanel;
			putValue( Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke( "shift R" ) );
		}

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed( final ActionEvent arg0 )
		{
			final int width = viewerPanel.getWidth();
			final int height = viewerPanel.getHeight();
			final double cX = width / 2.;
			final double cY = height / 2.;

			// Source
			final AffineTransform3D c = new AffineTransform3D();
			viewerPanel.state().getViewerTransform( c );
			c.set( c.get( 0, 3 ) - cX, 0, 3 );
			c.set( c.get( 1, 3 ) - cY, 1, 3 );

			// Target
			final AffineTransform3D t = InitializeViewerState.initTransform( width, height, false, viewerPanel.state() );
			t.set( t.get( 0, 3 ) - cX, 0, 3 );
			t.set( t.get( 1, 3 ) - cY, 1, 3 );

			// Run
			viewerPanel.setTransformAnimator( new SimilarityTransformAnimator( c, t, cX, cY, 300 ) ); // FIXME
		}
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
