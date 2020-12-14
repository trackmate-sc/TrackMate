package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.visualization.TrackMateModelView.DEFAULT_HIGHLIGHT_COLOR;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.DEFAULT_SPOT_COLOR;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.DEFAULT_TRACK_DISPLAY_DEPTH;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.DEFAULT_TRACK_DISPLAY_MODE;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_COLOR;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_COLORMAP;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_DISPLAY_SPOT_AS_ROIS;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_DISPLAY_SPOT_NAMES;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_HIGHLIGHT_COLOR;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOTS_VISIBLE;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOT_COLORING;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOT_RADIUS_RATIO;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACKS_VISIBLE;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACK_COLORING;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACK_DISPLAY_DEPTH;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACK_DISPLAY_MODE;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.scijava.object.ObjectService;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackMateOptionUtils;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.action.ExportAllSpotsStatsAction;
import fiji.plugin.trackmate.action.ExportStatsToIJAction;
import fiji.plugin.trackmate.detection.ManualDetectorFactory;
import fiji.plugin.trackmate.features.ModelFeatureUpdater;
import fiji.plugin.trackmate.features.edges.EdgeVelocityAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.gui.descriptors.ActionChooserDescriptor;
import fiji.plugin.trackmate.gui.descriptors.ConfigureViewsDescriptor;
import fiji.plugin.trackmate.gui.descriptors.DetectionDescriptor;
import fiji.plugin.trackmate.gui.descriptors.DetectorChoiceDescriptor;
import fiji.plugin.trackmate.gui.descriptors.DetectorConfigurationDescriptor;
import fiji.plugin.trackmate.gui.descriptors.GrapherDescriptor;
import fiji.plugin.trackmate.gui.descriptors.InitFilterDescriptor;
import fiji.plugin.trackmate.gui.descriptors.LoadDescriptor;
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
import fiji.plugin.trackmate.gui.panels.components.ColorByFeatureGUIPanel;
import fiji.plugin.trackmate.providers.ActionProvider;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.providers.ViewProvider;
import fiji.plugin.trackmate.tracking.ManualTrackerFactory;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import fiji.plugin.trackmate.visualization.ManualEdgeColorGenerator;
import fiji.plugin.trackmate.visualization.ManualSpotColorGenerator;
import fiji.plugin.trackmate.visualization.PerEdgeFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.SpotColorGenerator;
import fiji.plugin.trackmate.visualization.SpotColorGeneratorPerTrackFeature;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
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

	protected SpotAnalyzerProvider spotAnalyzerProvider;

	protected EdgeAnalyzerProvider edgeAnalyzerProvider;

	protected TrackAnalyzerProvider trackAnalyzerProvider;

	protected DetectorProvider detectorProvider;

	protected ViewProvider viewProvider;

	protected TrackerProvider trackerProvider;

	protected ActionProvider actionProvider;

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

	protected LoadDescriptor loadDescriptor;

	protected Collection< WizardPanelDescriptor > registeredDescriptors;

	protected SelectionModel selectionModel;

	protected PerTrackFeatureColorGenerator trackColorGenerator;

	protected PerEdgeFeatureColorGenerator edgeColorGenerator;

	protected FeatureColorGenerator< Spot > spotColorGenerator;

	protected ManualEdgeColorGenerator manualEdgeColorGenerator;

	protected ManualSpotColorGenerator manualSpotColorGenerator;

	protected FeatureColorGenerator< Spot > spotColorGeneratorPerTrackFeature;

	/**
	 * The listener in charge of listening to display settings changes and
	 * forwarding them to the views registered in the {@link #guimodel}.
	 */
	protected DisplaySettingsListener displaySettingsListener;

	/*
	 * CONSTRUCTOR
	 */

	public TrackMateGUIController( final TrackMate trackmate )
	{

		// I can't stand the metal look. If this is a problem, contact me
		// (jeanyves.tinevez@gmail.com)
		if ( IJ.isMacOSX() || IJ.isWindows() )
		{
			try
			{
				UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
			}
			catch ( final ClassNotFoundException e )
			{
				e.printStackTrace();
			}
			catch ( final InstantiationException e )
			{
				e.printStackTrace();
			}
			catch ( final IllegalAccessException e )
			{
				e.printStackTrace();
			}
			catch ( final UnsupportedLookAndFeelException e )
			{
				e.printStackTrace();
			}
		}

		this.trackmate = trackmate;
		trackmate.setNumThreads( Prefs.getThreads() );

		/*
		 * Instantiate GUI
		 */

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

		// Feature colorers
		this.spotColorGenerator = createSpotColorGenerator();
		this.edgeColorGenerator = createEdgeColorGenerator();
		this.trackColorGenerator = createTrackColorGenerator();
		this.manualEdgeColorGenerator = createManualEdgeColorGenerator();
		this.manualSpotColorGenerator = createManualSpotColorGenerator();
		this.spotColorGeneratorPerTrackFeature = createSpotColorGeneratorPerTrackFeature();

		// 0.
		this.guimodel = new TrackMateGUIModel();
		this.guimodel.setDisplaySettings( createDisplaySettings( trackmate.getModel() ) );
		this.displaySettingsListener = new DisplaySettingsListener()
		{
			@Override
			public void displaySettingsChanged( final DisplaySettingsEvent event )
			{
				guimodel.getDisplaySettings().put( event.getKey(), event.getNewValue() );
				for ( final TrackMateModelView view : guimodel.views )
				{
					view.setDisplaySettings( event.getKey(), event.getNewValue() );
					view.refresh();
				}
			}
		};

		// 1.
		createSelectionModel();
		// 2.
		createProviders();
		// 3.
		registeredDescriptors = createDescriptors();

		trackmate.getModel().setLogger( logger );
		gui.setVisible( true );
		gui.addActionListener( this );

		init();
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Creates a new {@link TrackMateGUIController} instance, set to operate on
	 * the specified {@link TrackMate} instance.
	 * <p>
	 * Subclassers want to override this method to return the correct type.
	 *
	 * @param lTrackmate
	 *            the instance that will be piloted by the new controller.
	 * @return a new instance of the controller.
	 */
	public TrackMateGUIController createOn( final TrackMate lTrackmate )
	{
		return new TrackMateGUIController( lTrackmate );
	}

	/**
	 * Closes the GUI controlled by this instance.
	 */
	public void quit()
	{
		gui.dispose();
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

			if ( stateKey.equals( descriptor.getKey() ) )
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
			}
		}

		logger.error( "Cannot move to state " + stateKey + ". Unknown state.\n" );
	}

	/**
	 * Returns the {@link ViewProvider} instance, serving
	 * {@link TrackMateModelView}s to this GUI
	 *
	 * @return the view provider.
	 */
	public ViewProvider getViewProvider()
	{
		return viewProvider;
	}

	/**
	 * Returns the {@link DetectorProvider} instance, serving
	 * {@link fiji.plugin.trackmate.detection.SpotDetectorFactory}s to this GUI
	 *
	 * @return the detector provider.
	 */
	public DetectorProvider getDetectorProvider()
	{
		return detectorProvider;
	}

	/**
	 * Returns the {@link SpotAnalyzerProvider} instance, serving
	 * {@link fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory}s to this
	 * GUI.
	 *
	 * @return the spot analyzer provider.
	 */
	public SpotAnalyzerProvider getSpotAnalyzerProvider()
	{
		return spotAnalyzerProvider;
	}

	/**
	 * Returns the {@link EdgeAnalyzerProvider} instance, serving
	 * {@link fiji.plugin.trackmate.features.edges.EdgeAnalyzer}s to this GUI.
	 *
	 * @return the edge analyzer provider.
	 */
	public EdgeAnalyzerProvider getEdgeAnalyzerProvider()
	{
		return edgeAnalyzerProvider;
	}

	/**
	 * Returns the {@link TrackAnalyzerProvider} instance, serving
	 * {@link fiji.plugin.trackmate.features.track.TrackAnalyzer}s to this GUI.
	 *
	 * @return the track analyzer provider.
	 */
	public TrackAnalyzerProvider getTrackAnalyzerProvider()
	{
		return trackAnalyzerProvider;
	}

	/**
	 * Returns the {@link TrackerProvider} instance, serving
	 * {@link fiji.plugin.trackmate.tracking.SpotTracker}s to this GUI.
	 *
	 * @return the tracker provider.
	 */
	public TrackerProvider getTrackerProvider()
	{
		return trackerProvider;
	}

	/*
	 * PROTECTED METHODS
	 */

	protected void createSelectionModel()
	{
		selectionModel = new SelectionModel( trackmate.getModel() );
	}

	protected FeatureColorGenerator< Spot > createSpotColorGenerator()
	{
		return new SpotColorGenerator( trackmate.getModel() );
	}

	protected PerEdgeFeatureColorGenerator createEdgeColorGenerator()
	{
		return new PerEdgeFeatureColorGenerator( trackmate.getModel(), EdgeVelocityAnalyzer.VELOCITY );
	}

	protected PerTrackFeatureColorGenerator createTrackColorGenerator()
	{
		final PerTrackFeatureColorGenerator generator = new PerTrackFeatureColorGenerator( trackmate.getModel(), TrackIndexAnalyzer.TRACK_INDEX );
		return generator;
	}

	protected ManualSpotColorGenerator createManualSpotColorGenerator()
	{
		return new ManualSpotColorGenerator();
	}

	protected ManualEdgeColorGenerator createManualEdgeColorGenerator()
	{
		return new ManualEdgeColorGenerator( trackmate.getModel() );
	}

	protected FeatureColorGenerator< Spot > createSpotColorGeneratorPerTrackFeature()
	{
		final FeatureColorGenerator< Spot > generator = new SpotColorGeneratorPerTrackFeature( trackmate.getModel(), TrackIndexAnalyzer.TRACK_INDEX );
		return generator;
	}

	protected void createProviders()
	{
		spotAnalyzerProvider = new SpotAnalyzerProvider( trackmate.getSettings().imp );
		edgeAnalyzerProvider = new EdgeAnalyzerProvider();
		trackAnalyzerProvider = new TrackAnalyzerProvider();
		detectorProvider = new DetectorProvider();
		viewProvider = new ViewProvider();
		trackerProvider = new TrackerProvider();
		actionProvider = new ActionProvider();
	}

	/**
	 * Creates the map of next descriptor for each descriptor.
	 */
	protected Collection< WizardPanelDescriptor > createDescriptors()
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
		detectorChoiceDescriptor = new DetectorChoiceDescriptor( detectorProvider, trackmate, this );

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
		initFilterDescriptor = new InitFilterDescriptor( trackmate, this );

		/*
		 * Select and render a view
		 */
		// We need the GUI model to register the created view there.
		viewChoiceDescriptor = new ViewChoiceDescriptor( viewProvider, guimodel, this );

		/*
		 * Spot filtering
		 */
		spotFilterDescriptor = new SpotFilterDescriptor( trackmate, spotColorGenerator, this );
		// display color changed
		spotFilterDescriptor.addActionListener( new ActionListener()
		{

			@Override
			public void actionPerformed( final ActionEvent event )
			{
				if ( event == spotFilterDescriptor.getComponent().COLOR_FEATURE_CHANGED )
				{

					final FeatureColorGenerator< Spot > newValue;
					@SuppressWarnings( "unchecked" )
					final FeatureColorGenerator< Spot > oldValue = ( FeatureColorGenerator< Spot > ) guimodel.getDisplaySettings().get( KEY_SPOT_COLORING );
					if ( null == spotFilterDescriptor.getComponent() )
						return;

					switch ( spotFilterDescriptor.getComponent().getColorCategory() )
					{
					case DEFAULT:
						newValue = spotColorGenerator;
						spotColorGenerator.setFeature( null );
						break;
					case TRACKS:
						newValue = spotColorGeneratorPerTrackFeature;
						spotColorGeneratorPerTrackFeature.setFeature( spotFilterDescriptor.getComponent().getColorFeature() );
						break;
					default:
						newValue = spotColorGenerator;
						spotColorGenerator.setFeature( spotFilterDescriptor.getComponent().getColorFeature() );
						break;
					}
					final DisplaySettingsEvent dsEvent = new DisplaySettingsEvent( spotFilterDescriptor.getComponent(), KEY_SPOT_COLORING, newValue, oldValue );
					displaySettingsListener.displaySettingsChanged( dsEvent );
				}
			}
		} );
		// Filtered
		spotFilterDescriptor.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent event )
			{
				// We set the thresholds field of the model but do not touch its
				// selected spot field yet.
				trackmate.getSettings().setSpotFilters( spotFilterDescriptor.getComponent().getFeatureFilters() );
				trackmate.execSpotFiltering( false );
			}
		} );

		/*
		 * Choose a tracker
		 */
		trackerChoiceDescriptor = new TrackerChoiceDescriptor( trackerProvider, trackmate, this );

		/*
		 * Configure chosen tracker
		 */
		trackerConfigurationDescriptor = new TrackerConfigurationDescriptor( trackerProvider, trackmate, this );

		/*
		 * Execute tracking
		 */
		trackingDescriptor = new TrackingDescriptor( this );

		/*
		 * Track filtering
		 */
		trackFilterDescriptor = new TrackFilterDescriptor( trackmate, trackColorGenerator, this );
		trackFilterDescriptor.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent event )
			{
				if ( trackFilterDescriptor.getComponent().getColorCategory().equals( ColorByFeatureGUIPanel.Category.DEFAULT ) )
					trackColorGenerator.setFeature( null );
				else
					trackColorGenerator.setFeature( trackFilterDescriptor.getComponent().getColorFeature() );

				for ( final TrackMateModelView view : guimodel.views )
				{
					view.setDisplaySettings( TrackMateModelView.KEY_TRACK_COLORING, trackColorGenerator );
					view.refresh();
				}
			}
		} );
		trackFilterDescriptor.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent event )
			{
				// We set the thresholds field of the model but do not touch its
				// selected spot field yet.
				trackmate.getSettings().setTrackFilters( trackFilterDescriptor.getComponent().getFeatureFilters() );
				trackmate.execTrackFiltering( false );
			}
		} );

		/*
		 * Finished, let's change the display settings.
		 */
		configureViewsDescriptor = new ConfigureViewsDescriptor( trackmate, spotColorGenerator, edgeColorGenerator, trackColorGenerator, spotColorGeneratorPerTrackFeature, manualSpotColorGenerator, manualEdgeColorGenerator, this );
		configureViewsDescriptor.getComponent().addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent event )
			{
				if ( event == configureViewsDescriptor.getComponent().TRACK_SCHEME_BUTTON_PRESSED )
					launchTrackScheme();
				else if ( event == configureViewsDescriptor.getComponent().DO_ANALYSIS_BUTTON_PRESSED )
					launchDoAnalysis( false );
				else if ( event == configureViewsDescriptor.getComponent().DO_ANALYSIS_BUTTON_WITH_SHIFT_PRESSED )
					launchDoAnalysis( true );
				else
					System.out.println( "[TrackMateGUIController] Caught unknown event: " + event );
			}
		} );
		configureViewsDescriptor.getComponent().addDisplaySettingsChangeListener( displaySettingsListener );

		/*
		 * Export and graph features.
		 */
		grapherDescriptor = new GrapherDescriptor( trackmate, this );

		/*
		 * Offer to take some actions on the data.
		 */
		actionChooserDescriptor = new ActionChooserDescriptor( actionProvider, trackmate, this );

		/*
		 * Save descriptor
		 */
		saveDescriptor = new SaveDescriptor( this );

		/*
		 * Load descriptor
		 */
		loadDescriptor = new LoadDescriptor( this );

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
		descriptors.add( loadDescriptor );
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
				if ( viewProvider.getVisibleKeys().size() == 1 )
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
			if ( viewProvider.getVisibleKeys().size() == 1 )
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
			if ( viewProvider.getVisibleKeys().size() == 1 )
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
		// We need to listen to events happening on the View configuration
		configureViewsDescriptor.getComponent().addActionListener( this );

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
		gui.getLogger().log( "https://scholar.google.com/scholar?cluster=9846627681021220605\n", Logger.BLUE_COLOR );
		// Execute about to be displayed action of the new one
		panelDescriptor.aboutToDisplayPanel();

		// Display matching panel
		gui.show( panelDescriptor );

		// Show the panel in the dialog, and execute action after display
		panelDescriptor.displayingPanel();
	}

	/**
	 * Returns the starting display settings that will be passed to any new view
	 * registered within this GUI.
	 *
	 * @param model
	 *            the model this GUI will configure; might be required by some
	 *            display settings.
	 * @return a map of display settings mappings.
	 */
	protected Map< String, Object > createDisplaySettings( final Model model )
	{
		final Map< String, Object > displaySettings = new HashMap<>();
		displaySettings.put( KEY_COLOR, DEFAULT_SPOT_COLOR );
		displaySettings.put( KEY_HIGHLIGHT_COLOR, DEFAULT_HIGHLIGHT_COLOR );
		displaySettings.put( KEY_SPOTS_VISIBLE, true );
		displaySettings.put( KEY_DISPLAY_SPOT_NAMES, false );
		displaySettings.put( KEY_DISPLAY_SPOT_AS_ROIS, true );
		displaySettings.put( KEY_SPOT_COLORING, spotColorGenerator );
		displaySettings.put( KEY_SPOT_RADIUS_RATIO, 1.0d );
		displaySettings.put( KEY_TRACKS_VISIBLE, true );
		displaySettings.put( KEY_TRACK_DISPLAY_MODE, DEFAULT_TRACK_DISPLAY_MODE );
		displaySettings.put( KEY_TRACK_DISPLAY_DEPTH, DEFAULT_TRACK_DISPLAY_DEPTH );
		displaySettings.put( KEY_TRACK_COLORING, trackColorGenerator );
		displaySettings.put( KEY_COLORMAP, TrackMateOptionUtils.getOptions().getPaintScale() );
		return displaySettings;
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
		else if ( event == gui.LOAD_BUTTON_PRESSED && guimodel.actionFlag )
		{

			/*
			 * TODO: There is actually NO load button anymore. The user load the
			 * data directly through another plugin call. We left this code here
			 * intact in case I change my mind. Removing it will actually
			 * trigger an appreciable simplification of the code, but I let it
			 * linger here a bit more. - Sep 2013
			 */

			guimodel.actionFlag = false;
			gui.jButtonNext.setText( "Resume" );
			disableButtonsAndStoreState();
			load();
			restoreButtonsState();

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
				configureViewsDescriptor.getComponent().refreshGUI();
				configureViewsDescriptor.getComponent().refreshColorFeatures();
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
		{
			oldDescriptor.aboutToHidePanel();
		}

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
		// Move to previous panel, but do not execute its forward-navigation
		// actions.
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

	private void load()
	{
		// Store current state
		guimodel.previousDescriptor = guimodel.currentDescriptor;

		// Move to load state and show log panel
		loadDescriptor.aboutToDisplayPanel();
		gui.show( loadDescriptor );

		// Instantiate GuiReader, ask for file, and load it in memory
		loadDescriptor.displayingPanel();
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
		final JButton button = configureViewsDescriptor.getComponent().getTrackSchemeButton();
		button.setEnabled( false );
		new Thread( "Launching TrackScheme thread" )
		{
			@Override
			public void run()
			{
				final TrackScheme trackscheme = new TrackScheme( trackmate.getModel(), selectionModel );
				final SpotImageUpdater thumbnailUpdater = new SpotImageUpdater( trackmate.getSettings() );
				trackscheme.setSpotImageUpdater( thumbnailUpdater );
				for ( final String settingKey : guimodel.getDisplaySettings().keySet() )
					trackscheme.setDisplaySettings( settingKey, guimodel.getDisplaySettings().get( settingKey ) );

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

				button.setEnabled( true );
			}
		}.start();
	}

	private void launchDoAnalysis( final boolean showAllSpotStats )
	{
		final JButton button = configureViewsDescriptor.getComponent().getDoAnalysisButton();
		button.setEnabled( false );
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
					if ( showAllSpotStats )
						action = new ExportAllSpotsStatsAction( selectionModel );
					else
						action = new ExportStatsToIJAction( selectionModel );

					action.execute( trackmate );

				}
				finally
				{
					gui.show( configureViewsDescriptor );
					button.setEnabled( true );
					if ( guimodel.displayingLog == false && guimodel.displayingDisplayConfig == false )
						restoreButtonsState();
				}
			}
		}.start();
	}

}
