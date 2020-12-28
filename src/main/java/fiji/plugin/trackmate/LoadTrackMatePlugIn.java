package fiji.plugin.trackmate;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.scijava.util.VersionUtils;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.LogPanel;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.descriptors.ConfigureViewsDescriptor;
import fiji.plugin.trackmate.gui.descriptors.SomeDialogDescriptor;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.ViewProvider;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.ViewUtils;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

public class LoadTrackMatePlugIn extends SomeDialogDescriptor implements PlugIn
{

	private static final String KEY = "LoadPlugin";

	public LoadTrackMatePlugIn()
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

		/*
		 * I can't stand the metal look. If this is a problem, contact me
		 * (jeanyves.tinevez at gmail dot com)
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

		// Views
		final Collection< TrackMateModelView > views = reader.getViews(
				new ViewProvider(),
				model,
				settings,
				selectionModel,
				displaySettings );

		if ( !reader.isReadingOk() )
		{
			logger.error( "Some errors occurred while reading file:\n" );
			logger.error( reader.getErrorMessage() );
		}

		/*
		 * Setup and render views
		 */

		// At least one view.
		if ( views.isEmpty() )
			views.add( new HyperStackDisplayer( model, selectionModel, settings.imp, displaySettings ) );

		for ( final TrackMateModelView view : views )
		{
			if ( view instanceof TrackScheme )
				continue; // Don't relaunch TrackScheme.

			view.render();
		}

		// Create GUI.
		final TrackMateGUIController controller = new TrackMateGUIController( trackmate, displaySettings, selectionModel );

		// GUI position
		GuiUtils.positionWindow( controller.getGUI(), settings.imp.getWindow() );

		// GUI state
		String guiState = reader.getGUIState();

		if ( null == guiState )
			guiState = ConfigureViewsDescriptor.KEY;

		/*
		 * Set GUI state.
		 */

		controller.setGUIStateString( guiState );

		// Text
		controller.getGUI().getLogger().log( "Session log saved in the file:\n"
				+ "--------------------\n"
				+ logText
				+ "--------------------\n",
				Color.GRAY );
		model.getLogger().log( "File loaded on " + TMUtils.getCurrentTimeString() + '\n', Logger.BLUE_COLOR );

		if ( !reader.isReadingOk() )
		{
			final Logger newlogger = controller.getGUI().getLogger();
			newlogger.error( "Some errors occurred while reading file:\n" );
			newlogger.error( reader.getErrorMessage() );
		}
	}

	/**
	 * Returns <code>true</code> is the specified file is an ICY track XML file.
	 *
	 * @param lFile
	 *            the file to inspect.
	 * @return <code>true</code> if it is an ICY track XML file.
	 */
	protected boolean checkIsICY( final File lFile )
	{
		final SAXBuilder sb = new SAXBuilder();
		Element r = null;
		try
		{
			final Document document = sb.build( lFile );
			r = document.getRootElement();
		}
		catch ( final JDOMException e )
		{
			return false;
		}
		catch ( final IOException e )
		{
			return false;
		}
		if ( !r.getName().equals( "root" ) || r.getChild( "trackfile" ) == null )
		{ return false; }
		return true;
	}

	@Override
	public void displayingPanel()
	{}

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
		plugIn.run( "samples/MAX_Merged.xml" );
	}
}
