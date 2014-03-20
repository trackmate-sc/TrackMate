package fiji.plugin.trackmate.gui.descriptors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.io.TmXmlWriter;

public class SaveDescriptor extends SomeDialogDescriptor
{

	private static final String KEY = "Saving";

	private final TrackMate trackmate;

	private final TrackMateGUIController controller;

	public SaveDescriptor( final TrackMateGUIController controller )
	{
		super( controller.getGUI().getLogPanel() );
		this.trackmate = controller.getPlugin();
		this.controller = controller;
	}

	@Override
	public void displayingPanel()
	{

		final Logger logger = logPanel.getLogger();
		logger.log( "Saving data...\n", Logger.BLUE_COLOR );
		if ( null == file )
		{
			// File folder = new
			// File(System.getProperty("user.dir")).getParentFile().getParentFile();
			final File folder = new File( trackmate.getSettings().imp.getOriginalFileInfo().directory );
			try
			{
				file = new File( folder.getPath() + File.separator + trackmate.getSettings().imp.getShortTitle() + ".xml" );
			}
			catch ( final NullPointerException npe )
			{
				file = new File( folder.getPath() + File.separator + "TrackMateData.xml" );
			}
		}

		// If we are to save tracks, we better ensures that track and edge
		// features are there, even if we have to enforce it
		if ( trackmate.getModel().getTrackModel().nTracks( false ) > 0 )
		{
			trackmate.computeEdgeFeatures( true );
			trackmate.computeTrackFeatures( true );
		}

		final File tmpFile = IOUtils.askForFileForSaving( file, controller.getGUI(), logger );
		if ( null == tmpFile ) { return; }
		file = tmpFile;

		/*
		 * Write model, settings and GUI state
		 */

		final TmXmlWriter writer = new TmXmlWriter( file );

		writer.appendLog( logPanel.getTextContent() );
		writer.appendModel( trackmate.getModel() );
		writer.appendSettings( trackmate.getSettings() );
		writer.appendGUIState( controller.getGuimodel() );

		try
		{
			writer.writeToFile();
			logger.log( "Data saved to: " + file.toString() + '\n' );
		}
		catch ( final FileNotFoundException e )
		{
			logger.error( "File not found:\n" + e.getMessage() + '\n' );
			return;
		}
		catch ( final IOException e )
		{
			logger.error( "Input/Output error:\n" + e.getMessage() + '\n' );
			return;
		}

	}

	@Override
	public String getKey()
	{
		return KEY;
	}

}
