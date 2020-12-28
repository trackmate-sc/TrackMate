package fiji.plugin.trackmate.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.scijava.object.ObjectService;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.action.ExportAllSpotsStatsAction;
import fiji.plugin.trackmate.action.ExportStatsToIJAction;
import fiji.plugin.trackmate.detection.ManualDetectorFactory;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.ModelFeatureUpdater;
import fiji.plugin.trackmate.gui.descriptors.ActionChooserDescriptor;
import fiji.plugin.trackmate.gui.descriptors.ConfigureViewsDescriptor;
import fiji.plugin.trackmate.gui.descriptors.DetectionDescriptor;
import fiji.plugin.trackmate.gui.descriptors.DetectorChoiceDescriptor;
import fiji.plugin.trackmate.gui.descriptors.DetectorConfigurationDescriptor;
import fiji.plugin.trackmate.gui.descriptors.GrapherDescriptor;
import fiji.plugin.trackmate.gui.descriptors.InitFilterDescriptor;
import fiji.plugin.trackmate.gui.descriptors.LogPanelDescriptor;
import fiji.plugin.trackmate.gui.descriptors.SaveDescriptor;
import fiji.plugin.trackmate.gui.descriptors.SomeDialogDescriptor;
import fiji.plugin.trackmate.gui.descriptors.SpotFilterDescriptor;
import fiji.plugin.trackmate.gui.descriptors.StartDialogDescriptor;
import fiji.plugin.trackmate.gui.descriptors.TrackFilterDescriptor;
import fiji.plugin.trackmate.gui.descriptors.TrackerChoiceDescriptor;
import fiji.plugin.trackmate.gui.descriptors.TrackerConfigurationDescriptor;
import fiji.plugin.trackmate.gui.descriptors.TrackingDescriptor;
import fiji.plugin.trackmate.gui.descriptors.ViewChoiceDescriptor;
import fiji.plugin.trackmate.gui.descriptors.WizardPanelDescriptor;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.providers.ActionProvider;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.providers.ViewProvider;
import fiji.plugin.trackmate.tracking.ManualTrackerFactory;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.trackscheme.SpotImageUpdater;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;
import ij.IJ;
import ij.Prefs;

public class TrackMateGUIController implements ActionListener
{

	/*
	 * FIELDS
	 */

	private static final boolean DEBUG = false;

	protected final Logger logger;

	/** The trackmate piloted here. */
	protected final TrackMate trackmate;

	/** The GUI controlled by this controller. */
	protected final TrackMateWizard gui;

	protected final TrackMateGUIModel guimodel;

	protected DetectorConfigurationDescriptor detectorConfigurationDescriptor;

	protected DetectorChoiceDescriptor detectorChoiceDescriptor;

	protected StartDialogDescriptor startDialoDescriptor;

	protected DetectionDescriptor detectionDescriptor;

	protected InitFilterDescriptor initFilterDescriptor;

	protected ViewChoiceDescriptor viewChoiceDescriptor;

	protected SpotFilterDescriptor spotFilterDescriptor;

	protected TrackerChoiceDescriptor trackerChoiceDescriptor;

	protected TrackerConfigurationDescriptor trackerConfigurationDescriptor;

	protected TrackingDescriptor trackingDescriptor;

	protected GrapherDescriptor grapherDescriptor;

	protected TrackFilterDescriptor trackFilterDescriptor;

	protected ConfigureViewsDescriptor configureViewsDescriptor;

	protected ActionChooserDescriptor actionChooserDescriptor;

	protected LogPanelDescriptor logPanelDescriptor;

	protected SaveDescriptor saveDescriptor;

	protected final Collection< WizardPanelDescriptor > registeredDescriptors;

	protected final SelectionModel selectionModel;

	protected final DisplaySettings displaySettings;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * @param trackmate
	 *            the TRackMate instance that will configured and piloted from
	 *            this GUI.
	 * 
	 * @param displaySettings
	 *            The display settings model that will be used everywhere in
	 *            this GUI instance.
	 * @param selectionModel
	 */
	public TrackMateGUIController( final TrackMate trackmate, final DisplaySettings displaySettings, final SelectionModel selectionModel )
	{

		/*
		 * I can't stand the metal look. If this is a problem, contact me
		 * (jeanyves.tinevez@gmail.com)
		 */
		if ( IJ.isMacOSX() || IJ.isWindows() )
		{
			try
			{
				UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
			}
			catch ( ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e )
			{
				e.printStackTrace();
			}
		}

		this.trackmate = trackmate;
		this.displaySettings = displaySettings;
		this.selectionModel = selectionModel;
		trackmate.setNumThreads( Prefs.getThreads() );

		/*
		 * Instantiate GUI
		 */

		final FeatureDisplaySelector featureSelector = new FeatureDisplaySelector( trackmate.getModel(), trackmate.getSettings(), displaySettings );
		this.gui = new TrackMateWizard( this );
		this.logger = gui.getLogger();

		/*
		 * Add this TrackMate instance to the ObjectService
		 */
		final ObjectService objectService = TMUtils.getContext().service( ObjectService.class );
		if ( objectService != null )
			objectService.addObject( trackmate );

		// Feature updater
		final ModelFeatureUpdater modelFeatureUpdater = new ModelFeatureUpdater( trackmate.getModel(), trackmate.getSettings() );
		modelFeatureUpdater.setNumThreads( trackmate.getNumThreads() );

		// 0.
		this.guimodel = new TrackMateGUIModel();

		// 1.
		this.registeredDescriptors = createDescriptors( trackmate.getSettings(), featureSelector );

		trackmate.getModel().setLogger( logger );
		gui.setVisible( true );
		gui.addActionListener( this );

		init();
	}

	/*
	 * PUBLIC METHODS
	 */

	public DisplaySettings getDisplaySettings()
	{
		return displaySettings;
	}

	/**
	 * Exposes the {@link TrackMateWizard} instance controlled here.
	 */
	public TrackMateWizard getGUI()
	{
		return gui;
	}

	/**
	 * Exposes the {@link TrackMate} trackmate piloted by the wizard.
	 */
	public TrackMate getPlugin()
	{
		return trackmate;
	}

	/**
	 * Exposes the {@link SelectionModel} shared amongst all
	 * {@link fiji.plugin.trackmate.SelectionChangeListener}s controlled by this
	 * instance.
	 *
	 * @return the {@link SelectionModel}.
	 */
	public SelectionModel getSelectionModel()
	{
		return selectionModel;
	}

	public TrackMateGUIModel getGuimodel()
	{
		return guimodel;
	}

	/**
	 * Sets the GUI current state via a key string. Registered descriptors are
	 * parsed until one is found that has a matching key (
	 * {@link WizardPanelDescriptor#getKey()}). Then it is displayed. If a
	 * matching key is not found, nothing is done, and an error is logged in the
	 * {@link LogPanel}.
	 * <p>
	 * This method is typically called to restore a saved GUI state.
	 *
	 * @param stateKey
	 *            the target state string.
	 */
	public void setGUIStateString( final String stateKey )
	{
		for ( final WizardPanelDescriptor descriptor : registeredDescriptors )
		{
			if ( stateKey.trim().replaceAll( " [^\\x00-\\x7F]", "" ).equals(
					descriptor.getKey().trim().replaceAll( " [^\\x00-\\x7F]", "" ) ) )
			{

				if ( descriptor.equals( spotFilterDescriptor ) )
				{
					/*
					 * Special case: we need this otherwise the component of
					 * this descriptor is not instantiated.
					 */
					spotFilterDescriptor.aboutToDisplayPanel();
				}

				guimodel.currentDescriptor = descriptor;
				gui.show( descriptor );
				if ( null == nextDescriptor( descriptor ) )
					gui.setNextButtonEnabled( false );
				else
					gui.setNextButtonEnabled( true );

				if ( null == previousDescriptor( descriptor ) )
					gui.setPreviousButtonEnabled( false );
				else
					gui.setPreviousButtonEnabled( true );

				descriptor.displayingPanel();
				return;
			}
		}

		logger.error( "Cannot move to state " + stateKey + ". Unknown state.\n" );
	}

	/*
	 * PROTECTED METHODS
	 */

	/**
	 * Creates the map of next descriptor for each descriptor.
	 *
	 * @param featureSelector
	 */
	protected Collection< WizardPanelDescriptor > createDescriptors( final Settings settings, final FeatureDisplaySelector featureSelector )
	{

		/*
		 * Logging panel: receive message, share with the TrackMateModel
		 */
		final LogPanel logPanel = gui.getLogPanel();
		logPanelDescriptor = new LogPanelDescriptor( logPanel );

		/*
		 * Start panel
		 */
		startDialoDescriptor = new StartDialogDescriptor( this )
		{
			@Override
			public void aboutToHidePanel()
			{
				super.aboutToHidePanel();
				// Reset the default save location.
				SomeDialogDescriptor.file = null;
			}

			@Override
			public void displayingPanel()
			{
				super.displayingPanel();
				if ( startDialoDescriptor.isImpValid() )
				{
					// Ensure we reset default save location
					gui.setNextButtonEnabled( true );
				}
				else
				{
					gui.setNextButtonEnabled( false );
				}
			}
		};
		/*
		 * Listen if the selected imp is valid and toggle next button
		 * accordingly.
		 */
		startDialoDescriptor.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				// Ensure we reset default save location
				if ( startDialoDescriptor.isImpValid() )
					gui.setNextButtonEnabled( true );
				else
					gui.setNextButtonEnabled( false );
			}
		} );

		/*
		 * Choose detector
		 */
		detectorChoiceDescriptor = new DetectorChoiceDescriptor( new DetectorProvider(), trackmate, this );

		/*
		 * Configure chosen detector
		 */
		detectorConfigurationDescriptor = new DetectorConfigurationDescriptor( trackmate, this );

		/*
		 * Execute and report detection progress
		 */
		detectionDescriptor = new DetectionDescriptor( this );

		/*
		 * Initial spot filter: discard obvious spurious spot based on quality.
		 */
		final FeatureFilter initialFilter = new FeatureFilter( Spot.QUALITY, settings.initialSpotFilterValue.doubleValue(), true );
		initFilterDescriptor = new InitFilterDescriptor( trackmate, this, initialFilter );

		/*
		 * Select and render a view
		 */
		// We need the GUI model to register the created view there.
		viewChoiceDescriptor = new ViewChoiceDescriptor( new ViewProvider(), guimodel, this );

		/*
		 * Spot filtering
		 */
		spotFilterDescriptor = new SpotFilterDescriptor( this, settings.getSpotFilters(), featureSelector );

		// Filtered
		spotFilterDescriptor.addChangeListener( e -> {
			// We set the thresholds field of the model but do not touch its
			trackmate.getSettings().setSpotFilters( spotFilterDescriptor.getComponent().getFeatureFilters() );
			trackmate.execSpotFiltering( false );
		} );

		/*
		 * Choose a tracker
		 */
		trackerChoiceDescriptor = new TrackerChoiceDescriptor( new TrackerProvider(), trackmate, this );

		/*
		 * Configure chosen tracker
		 */
		trackerConfigurationDescriptor = new TrackerConfigurationDescriptor( new TrackerProvider(), trackmate, this );

		/*
		 * Execute tracking
		 */
		trackingDescriptor = new TrackingDescriptor( this );

		/*
		 * Track filtering
		 */
		trackFilterDescriptor = new TrackFilterDescriptor( this, settings.getTrackFilters(), featureSelector );
		trackFilterDescriptor.addChangeListener( e -> {
			trackmate.getSettings().setTrackFilters( trackFilterDescriptor.getComponent().getFeatureFilters() );
			trackmate.execTrackFiltering( false );
		} );

		/*
		 * Finished, let's change the display settings.
		 */
		configureViewsDescriptor = new ConfigureViewsDescriptor( 
				displaySettings, 
				featureSelector, 
				this,
				new LaunchTrackSchemeAction(),
				new ShowTrackTablesAction(),
				new ShowSpotTableAction() );

		/*
		 * Export and graph features.
		 */
		grapherDescriptor = new GrapherDescriptor( trackmate, this );

		/*
		 * Offer to take some actions on the data.
		 */
		actionChooserDescriptor = new ActionChooserDescriptor( new ActionProvider(), trackmate, this );

		/*
		 * Save descriptor
		 */
		saveDescriptor = new SaveDescriptor( this );

		/*
		 * Store created descriptors
		 */
		final ArrayList< WizardPanelDescriptor > descriptors = new ArrayList<>( 16 );
		descriptors.add( actionChooserDescriptor );
		descriptors.add( configureViewsDescriptor );
		descriptors.add( detectorChoiceDescriptor );
		descriptors.add( detectorConfigurationDescriptor );
		descriptors.add( detectionDescriptor );
		descriptors.add( grapherDescriptor );
		descriptors.add( initFilterDescriptor );
		descriptors.add( logPanelDescriptor );
		descriptors.add( saveDescriptor );
		descriptors.add( spotFilterDescriptor );
		descriptors.add( startDialoDescriptor );
		descriptors.add( trackFilterDescriptor );
		descriptors.add( trackerChoiceDescriptor );
		descriptors.add( trackerConfigurationDescriptor );
		descriptors.add( trackingDescriptor );
		descriptors.add( viewChoiceDescriptor );
		return descriptors;
	}

	protected WizardPanelDescriptor getFirstDescriptor()
	{
		return startDialoDescriptor;
	}

	protected WizardPanelDescriptor nextDescriptor( final WizardPanelDescriptor currentDescriptor )
	{

		if ( currentDescriptor == startDialoDescriptor )
		{
			return detectorChoiceDescriptor;

		}
		else if ( currentDescriptor == detectorChoiceDescriptor )
		{
			return detectorConfigurationDescriptor;

		}
		else if ( currentDescriptor == detectorConfigurationDescriptor )
		{
			if ( trackmate.getSettings().detectorFactory.getKey().equals( ManualDetectorFactory.DETECTOR_KEY ) )
			{
				if ( new ViewProvider().getVisibleKeys().size() == 1 )
					return spotFilterDescriptor;
				else
					return viewChoiceDescriptor;
			}
			else
			{
				return detectionDescriptor;
			}

		}
		else if ( currentDescriptor == detectionDescriptor )
		{
			return initFilterDescriptor;

		}
		else if ( currentDescriptor == initFilterDescriptor )
		{
			// Skip choice of view if we just have one.
			if ( new ViewProvider().getVisibleKeys().size() == 1 )
				return spotFilterDescriptor;
			else
				return viewChoiceDescriptor;

		}
		else if ( currentDescriptor == viewChoiceDescriptor )
		{
			return spotFilterDescriptor;

		}
		else if ( currentDescriptor == spotFilterDescriptor )
		{
			return trackerChoiceDescriptor;

		}
		else if ( currentDescriptor == trackerChoiceDescriptor )
		{
			if ( null == trackmate.getSettings().trackerFactory || trackmate.getSettings().trackerFactory.getKey().equals( ManualTrackerFactory.TRACKER_KEY ) )
				return trackFilterDescriptor;

			return trackerConfigurationDescriptor;

		}
		else if ( currentDescriptor == trackerConfigurationDescriptor )
		{
			return trackingDescriptor;

		}
		else if ( currentDescriptor == trackingDescriptor )
		{
			return trackFilterDescriptor;

		}
		else if ( currentDescriptor == trackFilterDescriptor )
		{
			return configureViewsDescriptor;

		}
		else if ( currentDescriptor == configureViewsDescriptor )
		{
			return grapherDescriptor;

		}
		else if ( currentDescriptor == grapherDescriptor )
		{
			return actionChooserDescriptor;

		}
		else if ( currentDescriptor == actionChooserDescriptor )
		{
			return null;

		}
		else
		{
			throw new IllegalArgumentException( "Next descriptor for " + currentDescriptor + " is unknown." );
		}
	}

	protected WizardPanelDescriptor previousDescriptor( final WizardPanelDescriptor currentDescriptor )
	{

		if ( currentDescriptor == startDialoDescriptor )
		{
			return null;

		}
		else if ( currentDescriptor == detectorChoiceDescriptor )
		{
			return startDialoDescriptor;

		}
		else if ( currentDescriptor == detectorConfigurationDescriptor )
		{
			return detectorChoiceDescriptor;

		}
		else if ( currentDescriptor == detectionDescriptor )
		{
			return detectorConfigurationDescriptor;

		}
		else if ( currentDescriptor == initFilterDescriptor )
		{
			return detectorConfigurationDescriptor;

		}
		else if ( currentDescriptor == viewChoiceDescriptor )
		{
			return detectorConfigurationDescriptor;

		}
		else if ( currentDescriptor == spotFilterDescriptor )
		{
			if ( new ViewProvider().getVisibleKeys().size() == 1 )
				return detectorConfigurationDescriptor;
			else
				return viewChoiceDescriptor;

		}
		else if ( currentDescriptor == trackerChoiceDescriptor )
		{
			return spotFilterDescriptor;

		}
		else if ( currentDescriptor == trackerConfigurationDescriptor )
		{
			return trackerChoiceDescriptor;

		}
		else if ( currentDescriptor == trackingDescriptor )
		{
			return trackerConfigurationDescriptor;

		}
		else if ( currentDescriptor == trackFilterDescriptor )
		{
			if ( null == trackmate.getSettings().trackerFactory || trackmate.getSettings().trackerFactory.getKey().equals( ManualTrackerFactory.TRACKER_KEY ) )
				return trackerChoiceDescriptor;

			return trackerConfigurationDescriptor;

		}
		else if ( currentDescriptor == configureViewsDescriptor )
		{
			return trackFilterDescriptor;

		}
		else if ( currentDescriptor == grapherDescriptor )
		{
			return configureViewsDescriptor;

		}
		else if ( currentDescriptor == actionChooserDescriptor )
		{
			return grapherDescriptor;

		}
		else
		{
			throw new IllegalArgumentException( "Previous descriptor for " + currentDescriptor + " is unknown." );
		}
	}

	/**
	 * Display the first panel
	 */
	protected void init()
	{
		// Get start panel id
		gui.setPreviousButtonEnabled( false );
		final WizardPanelDescriptor panelDescriptor = getFirstDescriptor();
		guimodel.currentDescriptor = panelDescriptor;

		final String welcomeMessage = TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION + " started on:\n" + TMUtils.getCurrentTimeString() + '\n';
		// Log GUI processing start
		gui.getLogger().log( welcomeMessage, Logger.BLUE_COLOR );
		gui.getLogger().log( "Please note that TrackMate is available through Fiji, and is based on a publication. "
				+ "If you use it successfully for your research please be so kind to cite our work:\n" );
		gui.getLogger().log( "Tinevez, JY.; Perry, N. & Schindelin, J. et al. (2017), 'TrackMate: An open and extensible platform for single-particle tracking.', "
				+ "Methods 115: 80-90, PMID 27713081.\n", Logger.GREEN_COLOR );
		gui.getLogger().log( "https://www.ncbi.nlm.nih.gov/pubmed/27713081\n", Logger.BLUE_COLOR );
		gui.getLogger().log( "https://www.sciencedirect.com/science/article/pii/S1046202316303346\n", Logger.BLUE_COLOR );
		// Execute about to be displayed action of the new one
		panelDescriptor.aboutToDisplayPanel();

		// Display matching panel
		gui.show( panelDescriptor );

		// Show the panel in the dialog, and execute action after display
		panelDescriptor.displayingPanel();
	}

	/*
	 * ACTION LISTENER
	 */

	@Override
	public void actionPerformed( final ActionEvent event )
	{
		if ( DEBUG )
			System.out.println( "[TrackMateGUIController] Caught event " + event );

		if ( event == gui.NEXT_BUTTON_PRESSED && guimodel.actionFlag )
		{

			next();

		}
		else if ( event == gui.PREVIOUS_BUTTON_PRESSED && guimodel.actionFlag )
		{

			previous();

		}
		else if ( event == gui.SAVE_BUTTON_PRESSED && guimodel.actionFlag )
		{

			guimodel.actionFlag = false;
			gui.jButtonNext.setText( "Resume" );
			disableButtonsAndStoreState();
			new Thread( "TrackMate saving thread" )
			{
				@Override
				public void run()
				{
					save();
					gui.jButtonNext.setEnabled( true );
				}
			}.start();

		}
		else if ( ( event == gui.NEXT_BUTTON_PRESSED || event == gui.PREVIOUS_BUTTON_PRESSED || event == gui.LOAD_BUTTON_PRESSED || event == gui.SAVE_BUTTON_PRESSED ) && !guimodel.actionFlag )
		{

			// Display previous panel, but do not execute its actions
			guimodel.actionFlag = true;
			gui.show( guimodel.previousDescriptor );

			// Put back buttons
			gui.jButtonNext.setText( "Next" );
			restoreButtonsState();

		}
		else if ( event == gui.LOG_BUTTON_PRESSED )
		{

			if ( guimodel.displayingLog )
			{

				restoreButtonsState();
				gui.show( guimodel.previousDescriptor );
				guimodel.displayingLog = false;

			}
			else
			{
				disableButtonsAndStoreState();
				guimodel.previousDescriptor = guimodel.currentDescriptor;
				gui.show( logPanelDescriptor );
				gui.setLogButtonEnabled( true );
				guimodel.displayingLog = true;
			}
		}
		else if ( event == gui.DISPLAY_CONFIG_BUTTON_PRESSED )
		{
			if ( guimodel.displayingDisplayConfig )
			{

				restoreButtonsState();
				gui.show( guimodel.previousDescriptor );
				guimodel.displayingDisplayConfig = false;

			}
			else
			{
				disableButtonsAndStoreState();
				guimodel.previousDescriptor = guimodel.currentDescriptor;
				trackmate.computeSpotFeatures( true );
				trackmate.computeEdgeFeatures( true );
				trackmate.computeTrackFeatures( true );
				gui.show( configureViewsDescriptor );
				gui.setDisplayConfigButtonEnabled( true );
				guimodel.displayingDisplayConfig = true;
			}
		}
	}

	private void next()
	{

		gui.setNextButtonEnabled( false );

		// Execute leave action of the old panel
		final WizardPanelDescriptor oldDescriptor = guimodel.currentDescriptor;
		if ( oldDescriptor != null )
			oldDescriptor.aboutToHidePanel();

		// Find and store new one to display
		final WizardPanelDescriptor panelDescriptor = nextDescriptor( oldDescriptor );
		guimodel.currentDescriptor = panelDescriptor;

		// Re-enable the previous button, in case it was disabled
		gui.setPreviousButtonEnabled( true );

		// Execute about to be displayed action of the new one
		panelDescriptor.aboutToDisplayPanel();

		// Display matching panel
		gui.show( panelDescriptor );

		// Show the panel in the dialog, and execute action after display
		panelDescriptor.displayingPanel();
	}

	private void previous()
	{
		/*
		 * Move to previous panel, but do not execute its forward-navigation
		 * actions.
		 */
		final WizardPanelDescriptor olDescriptor = guimodel.currentDescriptor;
		final WizardPanelDescriptor panelDescriptor = previousDescriptor( olDescriptor );
		// Execute its backward-navigation actions.
		panelDescriptor.comingBackToPanel();
		// Do whatever we do when the panel is shown.
		panelDescriptor.displayingPanel();
		gui.show( panelDescriptor );
		guimodel.currentDescriptor = panelDescriptor;

		// Check if the new panel has a next panel. If not, disable the next
		// button
		if ( null == previousDescriptor( panelDescriptor ) )
			gui.setPreviousButtonEnabled( false );

		// Re-enable the previous button, in case it was disabled
		gui.setNextButtonEnabled( true );
	}

	private void save()
	{
		// Store current state
		guimodel.previousDescriptor = guimodel.currentDescriptor;

		/*
		 * Special case: if we are currently configuring a detector or a
		 * tracker, stores the settings currently displayed in TrackMate.
		 */

		if ( guimodel.currentDescriptor.equals( trackerConfigurationDescriptor )
				|| guimodel.currentDescriptor.equals( detectorConfigurationDescriptor ) )
		{
			// This will flush currently displayed settings to TrackMate.
			guimodel.currentDescriptor.aboutToHidePanel();
		}

		// Move to save state and execute
		saveDescriptor.aboutToDisplayPanel();

		gui.show( saveDescriptor );
		gui.getLogger().log( TMUtils.getCurrentTimeString() + '\n', Logger.BLUE_COLOR );
		saveDescriptor.displayingPanel();
	}

	/**
	 * Disable the 4 bottom buttons and memorize their state to that they can be
	 * restored when calling {@link #restoreButtonsState()}.
	 */
	public void disableButtonsAndStoreState()
	{
		guimodel.loadButtonState = gui.jButtonLoad.isEnabled();
		guimodel.saveButtonState = gui.jButtonSave.isEnabled();
		guimodel.previousButtonState = gui.jButtonPrevious.isEnabled();
		guimodel.nextButtonState = gui.jButtonNext.isEnabled();
		guimodel.displayConfigButtonState = gui.jButtonDisplayConfig.isEnabled();
		guimodel.logButtonState = gui.jButtonLog.isEnabled();
		SwingUtilities.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
				gui.jButtonLoad.setEnabled( false );
				gui.jButtonNext.setEnabled( false );
				gui.jButtonPrevious.setEnabled( false );
				gui.jButtonSave.setEnabled( false );
				gui.jButtonLog.setEnabled( false );
				gui.jButtonDisplayConfig.setEnabled( false );
			}
		} );
	}

	/**
	 * Restore the button state saved when calling
	 * {@link #disableButtonsAndStoreState()}. Do nothing if
	 * {@link #disableButtonsAndStoreState()} was not called before.
	 */
	public void restoreButtonsState()
	{
		gui.setLoadButtonEnabled( guimodel.loadButtonState );
		gui.setSaveButtonEnabled( guimodel.saveButtonState );
		gui.setPreviousButtonEnabled( guimodel.previousButtonState );
		gui.setNextButtonEnabled( guimodel.nextButtonState );
		gui.setDisplayConfigButtonEnabled( guimodel.displayConfigButtonState );
		gui.setLogButtonEnabled( guimodel.logButtonState );
	}

	private void launchTrackScheme()
	{
		new Thread( "Launching TrackScheme thread" )
		{
			@Override
			public void run()
			{
				final TrackScheme trackscheme = new TrackScheme( trackmate.getModel(), selectionModel, displaySettings );
				final SpotImageUpdater thumbnailUpdater = new SpotImageUpdater( trackmate.getSettings() );
				trackscheme.setSpotImageUpdater( thumbnailUpdater );
				trackscheme.render();
				guimodel.addView( trackscheme );
				// De-register
				trackscheme.getGUI().addWindowListener( new WindowAdapter()
				{
					@Override
					public void windowClosing( final WindowEvent e )
					{
						guimodel.removeView( trackscheme );
					}
				} );

			}
		}.start();
	}

	private void showTables( final boolean showSpotTable )
	{
		if ( guimodel.displayingLog == false && guimodel.displayingDisplayConfig == false )
			disableButtonsAndStoreState();
		gui.show( logPanelDescriptor );
		new Thread( "TrackMate export analysis to IJ thread." )
		{
			@Override
			public void run()
			{
				try
				{
					AbstractTMAction action;
					if ( showSpotTable )
						action = new ExportAllSpotsStatsAction( selectionModel );
					else
						action = new ExportStatsToIJAction( selectionModel );

					action.execute( trackmate );

				}
				finally
				{
					gui.show( configureViewsDescriptor );
					if ( guimodel.displayingLog == false && guimodel.displayingDisplayConfig == false )
						restoreButtonsState();
				}
			}
		}.start();
	}

	private static final Icon TRACK_TABLES_ICON = new ImageIcon( TrackMateWizard.class.getResource( "images/table_multiple.png" ) );

	private static final Icon SPOT_TABLE_ICON = new ImageIcon( TrackMateWizard.class.getResource( "images/table.png" ) );

	private static final String TRACK_TABLES_BUTTON_TOOLTIP = "<html>"
			+ "Export the features of all tracks, edges and all <br>"
			+ "spots belonging to a track to ImageJ tables."
			+ "</html>";

	private static final String SPOT_TABLE_BUTTON_TOOLTIP = "Export the features of all spots to ImageJ tables.";

	private static final String TRACKSCHEME_BUTTON_TOOLTIP = "<html>Launch a new instance of TrackScheme.</html>";

	private class LaunchTrackSchemeAction extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private LaunchTrackSchemeAction()
		{
			super( "TrackScheme", TrackScheme.TRACK_SCHEME_ICON_16x16 );
			putValue( SHORT_DESCRIPTION, TRACKSCHEME_BUTTON_TOOLTIP );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			launchTrackScheme();
		}
	}

	private class ShowTrackTablesAction extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private ShowTrackTablesAction()
		{
			super( "Tracks", TRACK_TABLES_ICON );
			putValue( SHORT_DESCRIPTION, TRACK_TABLES_BUTTON_TOOLTIP );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			showTables( false );
		}
	}

	private class ShowSpotTableAction extends AbstractAction
	{
		private static final long serialVersionUID = 1L;

		private ShowSpotTableAction()
		{
			super( "Spots", SPOT_TABLE_ICON );
			putValue( SHORT_DESCRIPTION, SPOT_TABLE_BUTTON_TOOLTIP );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			showTables( true );
		}
	}
}
