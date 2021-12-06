package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.gui.Icons.ISBI_ICON;

import java.awt.Frame;
import java.io.IOException;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import ij.ImagePlus;
import ij.gui.GenericDialog;

public class CTCExporterAction extends AbstractTMAction
{

	public static final String NAME = "Export to CTC format";

	public static final String KEY = "CTC_EXPORTER";

	public static final String INFO_TEXT = "<html>" +
			"Export the current TrackMate session to the Cell-Tracking-Challenge file format."
			+ "<p>"
			+ "See the <a url=\"http://celltrackingchallenge.net/\">challenge webpage</a> for details: http://celltrackingchallenge.net" +
			"</html>";

	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
	{
		final GenericDialog dialog = new GenericDialog( "CTC exporter", parent );

		final ImagePlus imp = trackmate.getSettings().imp;
		String defaultPath;
		if ( imp == null || imp.getOriginalFileInfo() == null )
			defaultPath = System.getProperty( "user.home" );
		else
			defaultPath = imp.getOriginalFileInfo().directory;

		dialog.addDirectoryField( "Export to", defaultPath );
		dialog.addChoice( "Data is", new String[] { "Gold truth", "Silver truth", "Results" }, "Results" );
		dialog.showDialog();

		if ( dialog.wasCanceled() )
			return;

		final String exportRootFolder = dialog.getNextString();
		final int choiceIndex = dialog.getNextChoiceIndex();
		final String suffix;
		switch ( choiceIndex )
		{
		case 0:
		default:
			suffix = "_GT";
			break;
		case 1:
			suffix = "_ST";
			break;
		case 2:
			suffix = "_RES";
			break;
		}

		try
		{
			CTCExporter.exportAll( exportRootFolder, trackmate, suffix );
		}
		catch ( final IOException e )
		{
			logger.error( e.getMessage() );
			e.printStackTrace();
		}
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
		public String getKey()
		{
			return KEY;
		}

		@Override
		public TrackMateAction create()
		{
			return new CTCExporterAction();
		}

		@Override
		public ImageIcon getIcon()
		{
			return ISBI_ICON;
		}

		@Override
		public String getName()
		{
			return NAME;
		}
	}
}
