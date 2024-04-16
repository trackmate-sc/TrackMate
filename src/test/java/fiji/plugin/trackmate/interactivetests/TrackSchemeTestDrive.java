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
package fiji.plugin.trackmate.interactivetests;

import java.io.File;

import org.scijava.util.AppUtils;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.edges.EdgeSpeedAnalyzer;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class TrackSchemeTestDrive
{

	public static void main( final String[] args )
	{

		final File file = new File( AppUtils.getBaseDirectory( TrackMate.class ), "samples/FakeTracks.xml" );

		final TmXmlReader reader = new TmXmlReader( file );
		final Model model = reader.getModel();
		System.out.println( model.getFeatureModel().echo() );

		System.out.println( "From the XML file:" );
		System.out.println( "Found " + model.getTrackModel().nTracks( false ) + " tracks in total." );
		System.out.println();

		final DisplaySettings ds = DisplaySettings.defaultStyle().copy();
		ds.setTrackColorBy( TrackMateObject.EDGES, EdgeSpeedAnalyzer.DISPLACEMENT );

		// Instantiate displayer
		final SelectionModel sm = new SelectionModel( model );
		final TrackScheme trackscheme = new TrackScheme( model, sm, ds );
		trackscheme.render();
		trackscheme.refresh();
	}
}
