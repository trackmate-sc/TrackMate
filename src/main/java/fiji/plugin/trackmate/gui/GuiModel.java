package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.EDITOR_KEYMAP_HOME;

import org.scijava.object.ObjectService;
import org.scijava.ui.behaviour.KeyPressedManager;
import org.scijava.ui.behaviour.util.Actions;

import bdv.ui.appearance.AppearanceManager;
import bdv.ui.keymap.Keymap;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.editor.labkit.component.EditorKeymapManager;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.hyperstack.behaviours.semiautotracking.SemiAutoTrackingParams;
import fiji.plugin.trackmate.visualization.ui.KeyConfigContexts;
import fiji.plugin.trackmate.visualization.ui.TrackMateActions;
import fiji.plugin.trackmate.visualization.ui.TrackMateKeymapManager;
import ij.ImagePlus;
import ij.Prefs;

/**
 * GUI model for TrackMate views.
 */
public class GuiModel
{
	
	private final Model model;

	private final SelectionModel selectionModel;

	private final DisplaySettings displaySettings;

	private final SemiAutoTrackingParams semiAutoTrackingparams;

	private final Actions globalActions;

	private final KeyPressedManager keyPressedManager;

	private final TrackMateKeymapManager keymapManager;

	private final EditorKeymapManager editorKeymapManager;

	private final AppearanceManager appearanceManager;

	private final Settings settings;

	private final TrackMate trackmate;

	private final WindowManager windowManager;

	/**
	 * Creates a new GuiModel.
	 * 
	 * @param model
	 *            the data model.
	 * @param settings
	 *            the settings objects. Viewer that can display an image will
	 *            use the {@link Settings#imp} field to do so.
	 * @param displaySettings
	 *            the display settings to use for the views.
	 */
	public GuiModel( final Model model, final Settings settings, final DisplaySettings displaySettings )
	{
		this.model = model;
		this.settings = settings;
		this.selectionModel = new SelectionModel( model );
		this.displaySettings = displaySettings;
		this.trackmate = createTrackMate( model, settings );
		this.semiAutoTrackingparams = new SemiAutoTrackingParams();
		
		// Keymap and actions
		this.editorKeymapManager = new EditorKeymapManager();
		this.appearanceManager = new AppearanceManager( EDITOR_KEYMAP_HOME );
		this.keyPressedManager = new KeyPressedManager();
		this.keymapManager = new TrackMateKeymapManager();
		final Keymap keymap = keymapManager.getForwardSelectedKeymap();
		keymap.updateListeners().add( () -> getGlobalActions().updateKeyConfig( keymap.getConfig() ) );
		this.globalActions = new Actions( keymapManager.getForwardSelectedKeymap().getConfig(), KeyConfigContexts.TRACKMATE );
		TrackMateActions.install( globalActions, model, selectionModel );

		// Window manager
		this.windowManager = new WindowManager( this );
	}

	/**
	 * Creates a new GuiModel with default display settings.
	 * 
	 * @param model
	 *            the data model.
	 * @param settings
	 *            the settings objects. Viewer that can display an image will
	 *            use the {@link Settings#imp} field to do so.
	 */
	public GuiModel( final Model model, final Settings settings )
	{
		this( model, settings, DisplaySettings.defaultStyle().copy() );
	}

	/**
	 * Creates a new GuiModel with default settings and display settings, and
	 * without an image.
	 * 
	 * @param model
	 *            the data model.
	 */
	public GuiModel( final Model model )
	{
		this( model, new Settings() );
	}

	/**
	 * Creates a new GuiModel with default settings, and without an image.
	 * 
	 * @param model
	 *            the data model.
	 * @param displaySettings
	 *            the display settings to use for the views.
	 */
	public GuiModel( final Model model, final DisplaySettings displaySettings )
	{
		this( model, new Settings(), displaySettings );
	}

	/**
	 * Creates a new GuiModel with default display settings, and default
	 * settings set to use the given image.
	 * 
	 * @param model
	 *            the data model.
	 * @param imp
	 *            the image to use in the settings.
	 */
	public GuiModel( final Model model, final ImagePlus imp )
	{
		this( model, new Settings( imp ) );
	}

	/**
	 * Hook for subclassers: <br>
	 * Creates the TrackMate instance that will be controlled in the GUI.
	 * 
	 * @param model
	 *            the model to create the TrackMate instance with.
	 * @param settings
	 *            the settings to create the TrackMate instance with.
	 * @return a new {@link TrackMate} instance.
	 */
	protected TrackMate createTrackMate( final Model model, final Settings settings )
	{
		/*
		 * Since we are now sure that we will be working on this model with this
		 * settings, we need to pass to the model the units from the settings.
		 */
		final String spaceUnits = settings.imp.getCalibration().getXUnit();
		final String timeUnits = settings.imp.getCalibration().getTimeUnit();
		model.setPhysicalUnits( spaceUnits, timeUnits );

		final TrackMate trackmate = new TrackMate( model, settings );
		final ObjectService objectService = TMUtils.getContext().service( ObjectService.class );
		if ( objectService != null )
			objectService.addObject( trackmate );

		// Set the num of threads from IJ prefs.
		trackmate.setNumThreads( Prefs.getThreads() );

		return trackmate;
	}

	/**
	 * Actions that operates on the whole TrackMate session.
	 * <p>
	 * For instance, saving, importing, creating a new view, showing the
	 * preference window, etc.
	 *
	 * @return the global actions.
	 */
	public Actions getGlobalActions()
	{
		return globalActions;
	}

	public KeyPressedManager getKeyPressedManager()
	{
		return keyPressedManager;
	}

	public TrackMateKeymapManager getKeymapManager()
	{
		return keymapManager;
	}

	public EditorKeymapManager getEditorKeymapManager()
	{
		return editorKeymapManager;
	}

	public Model getModel()
	{
		return model;
	}

	public Settings getSettings()
	{
		return settings;
	}

	public SelectionModel getSelectionModel()
	{
		return selectionModel;
	}

	public DisplaySettings getDisplaySettings()
	{
		return displaySettings;
	}

	public TrackMate getTrackMate()
	{
		return trackmate;
	}

	public WindowManager getWindowManager()
	{
		return windowManager;
	}

	public AppearanceManager getAppearanceManager()
	{
		return appearanceManager;
	}

	public SemiAutoTrackingParams getSemiAutoTrackingParams()
	{
		return semiAutoTrackingparams;
	}
}
