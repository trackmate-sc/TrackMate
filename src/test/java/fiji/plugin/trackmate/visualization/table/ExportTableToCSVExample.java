package fiji.plugin.trackmate.visualization.table;

import java.io.File;
import java.io.IOException;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.io.TmXmlReader;

public class ExportTableToCSVExample
{

	public static void main( String[] args ) throws IOException
	{
		final TmXmlReader reader = new TmXmlReader( new File( "samples/FakeTracks.xml" ) );
		final Model model = reader.getModel();
//		final SelectionModel selectionModel = new SelectionModel( model );
		final DisplaySettings ds = reader.getDisplaySettings();

		// Export all spots.
		File allSpotsCSVFile = new File( "samples/AllSpotsCSVExport.csv" );
		AllSpotsTableView.createSpotTable( model, ds ).exportToCsv( allSpotsCSVFile );

		// Export spots in tracks.
		File spotsInTracksTableCSVFile = new File( "samples/SpotsInTracksCSVExport.csv" );
		TrackTableView.createSpotTable( model, ds ).exportToCsv( spotsInTracksTableCSVFile );
		
		// Export tracks.
		File trackTableCSVFile = new File("samples/TracksCSVExport.csv");
		TrackTableView.createTrackTable( model, ds ).exportToCsv( trackTableCSVFile );

		// Export edges.
		File edgeTableCSVFile = new File( "samples/EdgesCSVExport.csv" );
		TrackTableView.createEdgeTable( model, ds ).exportToCsv( edgeTableCSVFile );

	}

}
