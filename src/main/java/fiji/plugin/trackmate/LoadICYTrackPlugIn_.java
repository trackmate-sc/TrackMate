package fiji.plugin.trackmate;

import fiji.plugin.trackmate.detection.ManualDetectorFactory;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.descriptors.ConfigureViewsDescriptor;
import fiji.plugin.trackmate.gui.panels.components.ImagePlusChooser;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.io.IcyXmlReader;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.tracking.ManualTrackerFactory;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.ViewUtils;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.IJ;
import ij.ImagePlus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Map;

import javax.swing.JFrame;

public class LoadICYTrackPlugIn_ extends LoadTrackMatePlugIn_
{

	private static final String KEY = "ICY_LOADER";
	private JFrame frame;

	@Override
	public void run( final String filePath )
	{
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

		/*
		 * Get the target image.
		 */

		final ImagePlusChooser impChooser = new ImagePlusChooser( "Pick target", "Select the target image:", "blank image" );
		impChooser.setLocationRelativeTo( null );
		impChooser.setVisible( true );

		final ActionListener copyOverlayListener = new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				if ( e == impChooser.OK_BUTTON_PUSHED )
				{
					new Thread( "Load ICY track XML thread" )
					{
						@Override
						public void run()
						{
							final ImagePlus imp = impChooser.getSelectedImagePlus();
							loadWith( imp );
						}
					}.start();
				}
				else
				{
					return;
				}
				impChooser.setVisible( false );
			}
		};
		impChooser.addActionListener( copyOverlayListener );
	}

	@SuppressWarnings( "rawtypes" )
	protected void loadWith( final ImagePlus dest )
	{
		ImagePlus imp = dest;
		final double[] calibration;
		double dt;

		if ( null == imp )
		{
			calibration = new double[] { 1d, 1d, 1d };
			dt = 1d;
		}
		else
		{
			calibration = TMUtils.getSpatialCalibration( imp );
			dt = imp.getCalibration().frameInterval;
			if ( dt <= 0 )
			{
				dt = 1d;
			}
		}

		/*
		 * Read the file content
		 */
		final IcyXmlReader reader = createReader( file, calibration, dt );
		model = reader.getModel();
		if ( !reader.isReadingOk() )
		{
			IJ.error( TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION, reader.getErrorMessage() );
			return;
		}

		/*
		 * Build a settings object
		 */
		settings = createSettings();
		if ( null == imp )
		{
			imp = ViewUtils.makeEmpytImagePlus( model );
		}
		settings.setFrom( imp );

		// Default detector and tracker for ICY
		settings.detectorFactory = new ManualDetectorFactory();
		settings.detectorSettings = settings.detectorFactory.getDefaultSettings();
		settings.trackerFactory = new ManualTrackerFactory();
		settings.trackerSettings = settings.trackerFactory.getDefaultSettings();

		// With this we can create a new controller from the provided one:
		final TrackMate trackmate = createTrackMate();

		// We need track ID NOW (otherwise we cannot instantiate the
		// controller... :( ).
		settings.addTrackAnalyzer( new TrackIndexAnalyzer() );
		trackmate.computeTrackFeatures( true );

		// Create a new controller for the new GUI
		final TrackMateGUIController controller = new TrackMateGUIController( trackmate );

		// Spot, edge and track analyzers: Add them all
		final SpotAnalyzerProvider spotAnalyzerProvider = controller.getSpotAnalyzerProvider();
		for ( final String key : spotAnalyzerProvider.getKeys() )
		{
			settings.addSpotAnalyzerFactory( spotAnalyzerProvider.getFactory( key ) );
		}
		final EdgeAnalyzerProvider edgeAnalyzerProvider = controller.getEdgeAnalyzerProvider();
		for ( final String key : edgeAnalyzerProvider.getKeys() )
		{
			settings.addEdgeAnalyzer( edgeAnalyzerProvider.getFactory( key ) );
		}
		final TrackAnalyzerProvider trackAnalyzerProvider = controller.getTrackAnalyzerProvider();
		for ( final String key : trackAnalyzerProvider.getKeys() )
		{
			if ( key.equals( TrackIndexAnalyzer.KEY ) )
			{
				continue;
			}
			settings.addTrackAnalyzer( trackAnalyzerProvider.getFactory( key ) );
		}

		// Compute features
		trackmate.computeSpotFeatures( true );
		trackmate.computeTrackFeatures( true );
		trackmate.computeEdgeFeatures( true );

		// Hook actions
		postRead( trackmate );

		// GUI position
		if ( null != settings.imp )
		{
			GuiUtils.positionWindow( controller.getGUI(), settings.imp.getWindow() );
		}

		// GUI state
		final String guiState = ConfigureViewsDescriptor.KEY;
		controller.setGUIStateString( guiState );

		// View
		final TrackMateModelView view = new HyperStackDisplayer( model, controller.getSelectionModel(), settings.imp );
		controller.getGuimodel().addView( view );
		final Map< String, Object > displaySettings = controller.getGuimodel().getDisplaySettings();
		for ( final String key : displaySettings.keySet() )
		{
			view.setDisplaySettings( key, displaySettings.get( key ) );
		}
		view.render();

		// Text
		controller.getGUI().getLogPanel().setTextContent( "Imported from the ICY track XML file version v" + reader.getVersion() + "\nFile: " + file + "\n" );
		model.getLogger().log( "File loaded on " + TMUtils.getCurrentTimeString() + '\n', Logger.BLUE_COLOR );
	}

	@Override
	protected TmXmlReader createReader( final File lFile )
	{
		throw new UnsupportedOperationException( "Cannot instantiate a TrackMate reader for a ICY loader." );
	}

	protected IcyXmlReader createReader( final File lFile, final double[] calibration, final double dt )
	{
		return new IcyXmlReader( lFile, calibration, dt );
	}

	@Override
	public String getKey()
	{
		return KEY;
	}
}
