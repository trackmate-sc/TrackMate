package fiji.plugin.trackmate.gui.wizard.descriptors;

import java.awt.Frame;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.SwingUtilities;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.components.LogPanel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.wizard.WizardSequence;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.io.TmXmlWriter;
import fiji.plugin.trackmate.util.TMUtils;

public class SaveDescriptor extends SomeDialogDescriptor
{

	private static final String KEY = "Saving";

	private final TrackMate trackmate;

	private final DisplaySettings displaySettings;

	private final WizardSequence sequence;

	public SaveDescriptor( final TrackMate trackmate, final DisplaySettings displaySettings, final WizardSequence sequence )
	{
		super( KEY, ( LogPanel ) sequence.logDescriptor().getPanelComponent() );
		this.trackmate = trackmate;
		this.displaySettings = displaySettings;
		this.sequence = sequence;
	}

	@Override
	public void displayingPanel()
	{
		final LogPanel logPanel = ( LogPanel ) targetPanel;
		final Logger logger = logPanel.getLogger();
		logger.log( "Saving data...\n", Logger.BLUE_COLOR );
		if ( null == file )
			file = TMUtils.proposeTrackMateSaveFile( trackmate.getSettings(), logger );

		/*
		 * If we are to save tracks, we better ensures that track and edge
		 * features are there, even if we have to enforce it.
		 */
		if ( trackmate.getModel().getTrackModel().nTracks( false ) > 0 )
		{
			trackmate.computeEdgeFeatures( true );
			trackmate.computeTrackFeatures( true );
		}

		final File tmpFile = IOUtils.askForFileForSaving( file, ( Frame ) SwingUtilities.getWindowAncestor( logPanel ), logger );
		if ( null == tmpFile )
			return;
		file = tmpFile;

		/*
		 * Write model, settings and GUI state
		 */

		final TmXmlWriter writer = new TmXmlWriter( file, logger );

		writer.appendLog( logPanel.getTextContent() );
		writer.appendModel( trackmate.getModel() );
		writer.appendSettings( trackmate.getSettings() );
		writer.appendGUIState( sequence.current().getPanelDescriptorIdentifier() );
		writer.appendDisplaySettings( displaySettings );

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
}
