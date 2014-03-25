package fiji.plugin.trackmate;

import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeVelocityAnalyzer;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.LogPanel;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.descriptors.ConfigureViewsDescriptor;
import fiji.plugin.trackmate.gui.descriptors.SomeDialogDescriptor;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.io.TmXmlReader_v12;
import fiji.plugin.trackmate.io.TmXmlReader_v20;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.providers.ViewProvider;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.util.Version;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.SpotImageUpdater;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class LoadTrackMatePlugIn_ extends SomeDialogDescriptor implements PlugIn
{

	private JFrame frame;

	private Model model;

	private Settings settings;

	private static final String KEY = "LoadPlugin";

	public LoadTrackMatePlugIn_()
	{
		super( new LogPanel() );
	}

	/**
	 * Loads a TrackMate file in the GUI.
	 *
	 * @param filePath
	 *            the path to a TrackMate XML file, to load. If
	 *            <code>null</code> or 0-length, the user will be asked to
	 *            browse to a TrackMate file.
	 */
	@Override
	public void run( final String filePath )
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

		final Logger logger = Logger.IJ_LOGGER; // logPanel.getLogger();
		if ( null == filePath || filePath.length() == 0 )
		{

			if ( null == file || file.length() == 0 )
			{
				final File folder = new File( System.getProperty( "user.dir" ) );
				final File parent = folder.getParentFile();
				final File parent2 = parent == null ? null : parent.getParentFile();
				file = new File( parent2 != null ? parent2 : parent != null ? parent : folder, "TrackMateData.xml" );
			}
			final File tmpFile = IOUtils.askForFileForLoading( file, "Load a TrackMate XML file", frame, logger );
			if ( null == tmpFile ) { return; }
			file = tmpFile;
		}
		else
		{
			file = new File( filePath );
			if ( !file.exists() )
			{
				IJ.error( TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION, "Could not find file with path " + filePath + "." );
				return;
			}
			if ( !file.canRead() )
			{
				IJ.error( TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION, "Could not read file with path " + filePath + "." );
				return;
			}
		}

		// Read the file content
		TmXmlReader reader = createReader( file );
		if ( !reader.isReadingOk() )
		{
			IJ.error( TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION, reader.getErrorMessage() );
			return;
		}

		final Version version = new Version( reader.getVersion() );
		if ( version.compareTo( new Version( "2.0.0" ) ) < 0 )
		{
			logger.log( "Detecting a file version " + version + ". Using the right reader.\n", Logger.GREEN_COLOR );
			reader = new TmXmlReader_v12( file );
		}
		else if ( version.compareTo( new Version( "2.1.0" ) ) < 0 )
		{
			logger.log( "Detecting a file version " + version + ". Using the right reader.\n", Logger.GREEN_COLOR );
			reader = new TmXmlReader_v20( file );
		}
		if ( !reader.isReadingOk() )
		{
			logger.error( reader.getErrorMessage() );
			logger.error( "Aborting.\n" ); // If I cannot even open the xml
			// file, it is not worth going on.
			return;
		}

		// Log
		final String logText = reader.getLog() + '\n';
		// Model
		model = reader.getModel();
		if ( !reader.isReadingOk() )
		{
			logger.error( "Problem reading the model:\n" + reader.getErrorMessage() );
		}

		// Settings -> empty for now.
		settings = createSettings();

		// With this we can create a new controller from the provided one:
		final TrackMate trackmate = createTrackMate();

		// Tune model and settings to be usable in the GUI even with old
		// versions
		if ( version.compareTo( new Version( "2.0.0" ) ) < 0 )
		{
			settings.addEdgeAnalyzer( new EdgeTargetAnalyzer() );
			settings.addEdgeAnalyzer( new EdgeVelocityAnalyzer() );
			trackmate.computeEdgeFeatures( true );
			model.setLogger( Logger.IJ_LOGGER );
			trackmate.computeEdgeFeatures( true );
		}
		else if ( version.compareTo( new Version( "2.1.0" ) ) < 0 )
		{
			model.setLogger( Logger.IJ_LOGGER );
			// trackmate.computeTrackFeatures(true);
		}

		final TrackMateGUIController controller = new TrackMateGUIController( trackmate );

		// We feed then the reader with the providers taken from the NEW
		// controller.
		final DetectorProvider detectorProvider = controller.getDetectorProvider();
		final TrackerProvider trackerProvider = controller.getTrackerProvider();
		final SpotAnalyzerProvider spotAnalyzerProvider = controller.getSpotAnalyzerProvider();
		final EdgeAnalyzerProvider edgeAnalyzerProvider = controller.getEdgeAnalyzerProvider();
		final TrackAnalyzerProvider trackAnalyzerProvider = controller.getTrackAnalyzerProvider();
		reader.readSettings( settings, detectorProvider, trackerProvider, spotAnalyzerProvider, edgeAnalyzerProvider, trackAnalyzerProvider );
		if ( !reader.isReadingOk() )
		{
			logger.error( "Problem reading the settings:\n" + reader.getErrorMessage() );
		}

		// Hook actions
		postRead( trackmate );

		// GUI position
		if ( null != settings.imp )
		{
			GuiUtils.positionWindow( controller.getGUI(), settings.imp.getWindow() );
		}

		// GUI state
		String guiState = reader.getGUIState();

		// Views
		final ViewProvider viewProvider = controller.getViewProvider();
		final Collection< TrackMateModelView > views = reader.getViews( viewProvider, model, settings, controller.getSelectionModel() );
		for ( final TrackMateModelView view : views )
		{
			if ( view instanceof TrackScheme )
			{
				final TrackScheme trackscheme = ( TrackScheme ) view;
				trackscheme.setSpotImageUpdater( new SpotImageUpdater( settings ) );
			}
		}

		if ( !reader.isReadingOk() )
		{
			final Logger newlogger = controller.getGUI().getLogger();
			newlogger.error( "Some errors occured while reading file:\n" );
			newlogger.error( reader.getErrorMessage() );
		}

		if ( null == guiState )
		{
			guiState = ConfigureViewsDescriptor.KEY;
		}
		controller.setGUIStateString( guiState );

		// Setup and render views
		if ( views.isEmpty() )
		{ // at least one view.
			views.add( new HyperStackDisplayer( model, controller.getSelectionModel(), settings.imp ) );
		}
		final Map< String, Object > displaySettings = controller.getGuimodel().getDisplaySettings();
		for ( final TrackMateModelView view : views )
		{
			for ( final String key : displaySettings.keySet() )
			{
				controller.getGuimodel().addView( view );
				view.setDisplaySettings( key, displaySettings.get( key ) );
			}
			view.render();
		}

		// Text
		controller.getGUI().getLogPanel().setTextContent( logText );
		model.getLogger().log( "File loaded on " + TMUtils.getCurrentTimeString() + '\n', Logger.BLUE_COLOR );
	}

	@Override
	public void displayingPanel()
	{
		frame = new JFrame();
		frame.getContentPane().add( logPanel );
		frame.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		frame.pack();
		frame.setVisible( true );
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	/*
	 * HOOKS
	 */

	/**
	 * Hook for subclassers:<br>
	 * The {@link TrackMate} object is loaded and properly configured. This
	 * method is called just before the controller and GUI are launched.
	 *
	 * @param trackmate
	 *            the {@link TrackMate} instance that was fledged after loading.
	 */
	protected void postRead( final TrackMate trackmate )
	{}

	/**
	 * Hook for subclassers: <br>
	 * Creates the TrackMate instance that will be controlled in the GUI.
	 *
	 * @return a new {@link TrackMate} instance.
	 */
	protected TrackMate createTrackMate()
	{
		return new TrackMate( model, settings );
	}

	/**
	 * Hook for subclassers: <br>
	 * Creates the {@link TmXmlReader} instance that will be used to load the
	 * file.
	 *
	 * @return a new {@link TmXmlReader} instance.
	 */
	protected TmXmlReader createReader( final File file )
	{
		return new TmXmlReader( file );
	}

	/**
	 * Hook for subclassers: <br>
	 * Creates the {@link Settings} instance that will be used to tune the
	 *
	 * @return a new {@link Settings} instance.
	 */
	protected Settings createSettings()
	{
		return new Settings();
	}



	/*
	 * MAIN METHOD
	 */

	/**
	 * @param args
	 */
	public static void main( final String[] args )
	{
		ImageJ.main( args );
		final LoadTrackMatePlugIn_ plugIn = new LoadTrackMatePlugIn_();
		// plugIn.run( "samples/FakeTracks.xml" );
		plugIn.run( "/Users/tinevez/Desktop/Data/Mamut/parhyale/BDV130418A325_NoTempReg-mamut_JY2.xml" );
	}

}
