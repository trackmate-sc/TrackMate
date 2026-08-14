package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.visualization.ui.KeyConfigContexts.ALL_SPOTS_TABLE;
import static fiji.plugin.trackmate.visualization.ui.KeyConfigContexts.HYPERSTACK_DISPLAYER;
import static fiji.plugin.trackmate.visualization.ui.KeyConfigContexts.KEY_CONFIG_SCOPE;
import static fiji.plugin.trackmate.visualization.ui.KeyConfigContexts.TRACKMATE;
import static fiji.plugin.trackmate.visualization.ui.KeyConfigContexts.TRACKSCHEME;
import static fiji.plugin.trackmate.visualization.ui.KeyConfigContexts.TRACK_TABLE;

import java.awt.Window;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.Actions;

import bdv.BigDataViewerActions;
import bdv.tools.CloseWindowActions;
import bdv.tools.PreferencesDialog;
import bdv.ui.appearance.AppearanceSettingsPage;
import bdv.ui.keymap.Keymap;
import bdv.ui.keymap.KeymapSettingsPage;
import bdv.util.InvokeOnEDT;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsConfigPage;
import fiji.plugin.trackmate.gui.editor.LabkitLauncher;
import fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.bvv.TrackMateBVV;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.hyperstack.behaviours.semiautotracking.SpotEditToolSettingsPage;
import fiji.plugin.trackmate.visualization.table.AllSpotsTableView;
import fiji.plugin.trackmate.visualization.table.TrackTableView;
import fiji.plugin.trackmate.visualization.trackscheme.SpotImageUpdater;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;
import fiji.plugin.trackmate.visualization.ui.TrackMateKeymapManager;
import ij.ImagePlus;

public class WindowManager
{

	private final GuiModel guiModel;

	private final List< TrackMateModelView > views = new ArrayList<>();

	private final List< Window > windows = new ArrayList<>();

	private HyperStackDisplayer mainView;

	public WindowManager( final GuiModel guiModel )
	{
		this.guiModel = guiModel;

		// Unwrap
		final TrackMateKeymapManager keymapManager = guiModel.getKeymapManager();
		final Keymap keymap = keymapManager.getForwardSelectedKeymap();
		final Actions globalActions = guiModel.getGlobalActions();

		// Preferences dialog
		final PreferencesDialog preferencesDialog = new PreferencesDialog( null, keymap,
				new String[] { TRACKMATE, HYPERSTACK_DISPLAYER, TRACKSCHEME, ALL_SPOTS_TABLE, TRACK_TABLE } );

		preferencesDialog.setTitle( "TrackMate Preferences" );
		preferencesDialog.setLocationRelativeTo( null );
		BigDataViewerActions.toggleDialogAction( globalActions, preferencesDialog, BigDataViewerActions.PREFERENCES_DIALOG, BigDataViewerActions.PREFERENCES_DIALOG_KEYS );
		preferencesDialog.addPage( new DisplaySettingsConfigPage( "Display settings", guiModel.getDisplaySettingsManager() ) );
		preferencesDialog.addPage( new KeymapSettingsPage( "Global keymaps", keymapManager, keymapManager.getCommandDescriptions() ) );
		preferencesDialog.addPage( new KeymapSettingsPage( "Editor keymap", guiModel.getEditorKeymapManager(), guiModel.getEditorKeymapManager().getCommandDescriptions() ) );
		preferencesDialog.addPage( new KeymapSettingsPage( "3D View keymap", guiModel.getBvvKeymapManager(), guiModel.getBvvKeymapManager().getCommandDescriptions() ) );
		preferencesDialog.addPage( new SpotEditToolSettingsPage( "Semi-auto tracking", guiModel.getSemiAutoTrackingParams() ) );
		preferencesDialog.addPage( new AppearanceSettingsPage( "BDVs appearance", guiModel.getAppearanceManager() ) );
	}

	public HyperStackDisplayer createHyperStackDisplayer()
	{
		if ( mainView != null )
			throw new IllegalStateException( "There can be only one main view." );
		this.mainView = new HyperStackDisplayer( guiModel );
		mainView.render();
		return mainView;
	}

	public TMLabKitFrame createSpotEditor( final boolean singleTimepoint )
	{
		final ImagePlus imp = guiModel.getSettings().imp;
		int timepoint;
		if ( imp == null )
			timepoint = -1;
		else
			timepoint = singleTimepoint ? imp.getFrame() - 1 : -1;
		final TMLabKitFrame frame = LabkitLauncher.launch( guiModel, timepoint );
		registerWindow( frame );
		return frame;
	}

	public TrackScheme createTrackScheme()
	{
		final TrackScheme trackscheme = new TrackScheme( guiModel );
		final SpotImageUpdater thumbnailUpdater = new SpotImageUpdater( guiModel.getSettings() );
		trackscheme.setSpotImageUpdater( thumbnailUpdater );
		registerView( trackscheme );
		trackscheme.render();
		return trackscheme;
	}

	public TrackMateBVV< ? > createBVV()
	{
		final ImagePlus imp = guiModel.getSettings().imp;
		if ( imp != null )
		{
			final TrackMateBVV< ? > tbvv = new TrackMateBVV<>( guiModel, imp );
			registerView( tbvv );
			tbvv.render();
			return tbvv;
		}
		return null;
	}

	public AllSpotsTableView createAllSpotsTable()
	{
		final String imageFileName = TMUtils.getImagePathWithoutExtension( guiModel.getSettings() );
		final AllSpotsTableView view = new AllSpotsTableView( guiModel, imageFileName );
		registerView( view );
		view.render();
		return view;
	}

	public TrackTableView createTrackTable()
	{
		final String imageFileName = TMUtils.getImagePathWithoutExtension( guiModel.getSettings() );
		final TrackTableView view = new TrackTableView( guiModel, imageFileName );
		registerView( view );
		view.render();
		return view;
	}

	/**
	 * Registers a TrackMate view created by this window manager.
	 * 
	 * @param view
	 *            the view to register.
	 */
	private void registerView( final TrackMateModelView view )
	{
		views.add( view );
	}

	/**
	 * Registers a frame in this window manager. This is useful for windows and
	 * dialogs that are not TrackMateModelView.
	 * 
	 * @param frame
	 *            the frame to register.
	 */
	private void registerWindow( final Window window )
	{
		windows.add( window );
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

	/**
	 * Close all opened views and dialogs.
	 */
	public void closeAll()
	{
		final ArrayList< Window > windowsToClose = new ArrayList<>();
		views.forEach( v -> windowsToClose.add( v.getWindow() ) );
		windowsToClose.addAll( windows );
		try
		{
			InvokeOnEDT.invokeAndWait(
					() -> windowsToClose.stream()
							.filter( Objects::nonNull )
							.forEach( window -> window
									.dispatchEvent( new WindowEvent( window, WindowEvent.WINDOW_CLOSING ) ) ) );
		}
		catch ( final InvocationTargetException e )
		{
			e.printStackTrace();
		}
		catch ( final InterruptedException e )
		{
			Thread.currentThread().interrupt();
			e.printStackTrace();
		}
	}
}
