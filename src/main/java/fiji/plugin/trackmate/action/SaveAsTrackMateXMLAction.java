package fiji.plugin.trackmate.action;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.GuiModel;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.wizard.descriptors.ConfigureViewsDescriptor;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.io.xml.TmXmlWriter;
import fiji.plugin.trackmate.util.TMUtils;

public class SaveAsTrackMateXMLAction extends AbstractTMAction
{

	private static final String INFO_TEXT = "<html>"
			+ "Exports the current model to a TrackMate XML file. "
			+ "<p>"
			+ "This file format was TrackMate's native file format before v9, "
			+ "and can be used in tools written for these versions."
			+ "</html>";

	private static final String NAME = "Save as TrackMate XML";

	private static final String KEY = "SAVE_AS_TRACKMATE_XML";

	@Override
	public void execute( final GuiModel guiModel, final Frame parent )
	{
		File file = TMUtils.proposeTrackMateSaveFile( guiModel.getSettings(), logger );
		if ( null == file )
		{
			final File folder = new File( System.getProperty( "user.dir" ) ).getParentFile().getParentFile();
			file = new File( folder.getPath() + File.separator + "TrackMateData.xml" );
		}
		final String xmlPath = file.getAbsolutePath().replace( ".geff", ".xml" );
		file = new File( xmlPath );

		file = IOUtils.askForFileForSaving( file, parent, "xml" );
		if ( file == null )
		{
			logger.log( "Saving aborted.\n" );
			return;
		}
		final TmXmlWriter writer = new TmXmlWriter( file, logger );

		final TrackMate trackmate = guiModel.getTrackMate();
		final DisplaySettings displaySettings = guiModel.getDisplaySettings();

		writer.appendModel( trackmate.getModel() );
		writer.appendSettings( trackmate.getSettings() );
		writer.appendGUIState( ConfigureViewsDescriptor.KEY );
		writer.appendDisplaySettings( displaySettings );

		try
		{
			writer.writeToFile();
		}
		catch ( final IOException e )
		{
			logger.error( "Could not write to file " + file.toString() + ".\n" );
			e.printStackTrace();
			return;
		}
		logger.log( "Data saved to: " + file.toString() + '\n' );
	}

	@Plugin( type = TrackMateActionFactory.class )
	public static class Factory implements TrackMateActionFactory
	{

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getName()
		{
			return NAME;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public ImageIcon getIcon()
		{
			return Icons.TRACKMATE_ICON_16x16;
		}

		@Override
		public TrackMateAction create()
		{
			return new SaveAsTrackMateXMLAction();
		}
	}
}
