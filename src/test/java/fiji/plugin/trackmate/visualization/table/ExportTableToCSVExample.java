/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
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
