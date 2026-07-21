package fiji.plugin.trackmate.io;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.io.geff.TmGeffReader;
import fiji.plugin.trackmate.io.geff.TmGeffWriter;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.table.AllSpotsTableView;
import fiji.plugin.trackmate.visualization.table.TrackTableView;
import ij.ImagePlus;

public class TmGeffWriterTestDrive
{

	public static void main( final String[] args ) throws IOException
	{
		final String path = "samples/FakeTracks.xml";
		System.out.println( "Reading from " + path );
		final TmXmlReader reader = new TmXmlReader( new File( path ) );
		if ( !reader.isReadingOk() )
		{
			System.err.println( reader.getErrorMessage() );
			return;
		}
		System.out.println( "Done." );
		final String savePath = path.replace( ".xml", ".geff" );

		final Model model = reader.getModel();
		final ImagePlus imp = reader.readImage();
		final Settings settings = reader.readSettings( imp );

		System.out.println( "Deleting previous " + savePath );
		FileUtils.deleteDirectory( new File( savePath ) );
		System.out.println( "Done." );

		System.out.println( "Writing to " + savePath );

		final TmGeffWriter geffWriter = new TmGeffWriter( savePath );
		geffWriter.appendModel( model );
		geffWriter.appendSettings( settings );
		geffWriter.appendDisplaySettings( reader.getDisplaySettings() );
		geffWriter.appendLog( reader.getLog() );
		geffWriter.appendGUIState( reader.getGUIState() );
		geffWriter.write();

		System.out.println( "Done." );

		System.out.println( "Reading back from " + savePath );
		final TmGeffReader geffReader = new TmGeffReader( savePath );
		final Model model2 = geffReader.getModel();
		System.out.println( "Done." );

		final SelectionModel sm = new SelectionModel( model2 );
		final DisplaySettings ds = DisplaySettingsIO.readUserDefault();
		final HyperStackDisplayer view = new HyperStackDisplayer( model2, sm, ds );
		view.render();

		final AllSpotsTableView table = new AllSpotsTableView( model2, sm, ds, savePath );
		table.render();

		final TrackTableView trackTable = new TrackTableView( model2, sm, ds, savePath );
		trackTable.render();
	}
}
