package fiji.plugin.trackmate;

import static fiji.plugin.trackmate.gui.Icons.TRACKMATE_ICON;

import java.awt.Color;
import java.io.File;

import javax.swing.JFrame;

import org.scijava.util.VersionUtils;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.LogPanel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.wizard.TrackMateWizardSequence;
import fiji.plugin.trackmate.gui.wizard.descriptors.ConfigureViewsDescriptor;
import fiji.plugin.trackmate.gui.wizard.descriptors.LogPanelDescriptor2;
import fiji.plugin.trackmate.gui.wizard.descriptors.SomeDialogDescriptor;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.ViewUtils;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

public class LoadTrackMatePlugIn extends SomeDialogDescriptor implements PlugIn
{

	private static final String KEY = "LoadPlugin";

	public LoadTrackMatePlugIn()
	{
		super( KEY, new LogPanel() );
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
		/*
		 * I can't stand the metal look. If this is a problem, contact me
		 * (jeanyves.tinevez at gmail dot com)
		 */
		GuiUtils.setSystemLookAndFeel();

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
			final File tmpFile = IOUtils.askForFileForLoading( file, "Load a TrackMate XML file", null, logger );
			if ( null == tmpFile ) { return; }
			file = tmpFile;
		}
		else
		{
			file = new File( filePath );
			if ( !file.exists() )
			{
				IJ.error( TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION,
						"Could not find file with path " + filePath + "." );
				return;
			}
			if ( !file.canRead() )
			{
				IJ.error( TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION,
						"Could not read file with path " + filePath + "." );
				return;
			}
		}

		// Read the file content
		final TmXmlReader reader = createReader( file );
		if ( !reader.isReadingOk() )
		{
			IJ.error( TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION, reader.getErrorMessage() );
			return;
		}

		final String version = reader.getVersion();
		if ( VersionUtils.compare( version, "2.1.0" ) < 0 )
		{
			// Since v7.0.0 we do not support TrackMate file version < 2.1.0.
			logger.error( "Cannot read TrackMate file version lower than 2.1.0.\nAborting.\n" );
			return;
		}
		if ( !reader.isReadingOk() )
		{
			logger.error( reader.getErrorMessage() );
			logger.error( "Aborting.\n" );
			/*
			 * If I cannot even open the xml file, it is not worth going on.
			 */
			return;
		}

		// Log
		final String logText = reader.getLog() + '\n';

		// Model
		final Model model = reader.getModel();
		if ( !reader.isReadingOk() )
			logger.error( "Problem reading the model:\n" + reader.getErrorMessage() );

		/*
		 * Read image.
		 */

		final ImagePlus imp = reader.readImage();

		/*
		 * Read settings.
		 */

		final Settings settings = reader.readSettings( imp );
		if ( !reader.isReadingOk() )
			logger.error( "Problem reading the settings:\n" + reader.getErrorMessage() );

		if ( null == settings.imp )
			settings.imp = ViewUtils.makeEmpytImagePlus( model );

		/*
		 * Create TrackMate.
		 */

		final TrackMate trackmate = new TrackMate( model, settings );

		// Hook actions
		postRead( trackmate );

		// Display settings.
		final DisplaySettings displaySettings = reader.getDisplaySettings();

		// Selection model.
		final SelectionModel selectionModel = new SelectionModel( model );

		if ( !reader.isReadingOk() )
		{
			logger.error( "Some errors occurred while reading file:\n" );
			logger.error( reader.getErrorMessage() );
		}

		// Main view.
		final TrackMateModelView displayer = new HyperStackDisplayer( model, selectionModel, settings.imp, displaySettings );
		displayer.render();
		
		// GUI state
		String panelIdentifier = reader.getGUIState();
		
		if ( null == panelIdentifier )
			panelIdentifier = ConfigureViewsDescriptor.KEY;

		// Wizard.
		final TrackMateWizardSequence sequence = new TrackMateWizardSequence( trackmate, selectionModel, displaySettings );
		sequence.setCurrent( panelIdentifier );
		final JFrame frame = sequence.run( "TrackMate on " + settings.imp.getShortTitle() );
		frame.setIconImage( TRACKMATE_ICON.getImage() );
		GuiUtils.positionWindow( frame, settings.imp.getWindow() );
		frame.setVisible( true );

		// Text		
		final LogPanelDescriptor2 logDescriptor = ( LogPanelDescriptor2 ) sequence.logDescriptor();
		final LogPanel logPanel = ( LogPanel ) logDescriptor.getPanelComponent();
		final Logger logger2 = logPanel.getLogger();
		
		logger2.log( "Session log saved in the file:\n"
				+ "--------------------\n"
				+ logText
				+ "--------------------\n",
				Color.GRAY );
		logger2.log( "File loaded on " + TMUtils.getCurrentTimeString() + '\n', Logger.BLUE_COLOR );

		if ( !reader.isReadingOk() )
		{
			logger2.error( "Some errors occurred while reading file:\n" );
			logger2.error( reader.getErrorMessage() );
		}
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
	 * Creates the {@link TmXmlReader} instance that will be used to load the
	 * file.
	 *
	 * @param lFile
	 *            the file to read from.
	 * @return a new {@link TmXmlReader} instance.
	 */
	protected TmXmlReader createReader( final File lFile )
	{
		return new TmXmlReader( lFile );
	}

	/*
	 * MAIN METHOD
	 */

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		final LoadTrackMatePlugIn plugIn = new LoadTrackMatePlugIn();
//		plugIn.run( "samples/FakeTracks.xml" );
//		plugIn.run( "samples/MAX_Merged.xml" );
		plugIn.run( "" );
	}
}
