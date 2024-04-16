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

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.scijava.util.AppUtils;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTracker;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.ImagePlus;

public class NNTrackerTest
{

	/*
	 * MAIN METHOD
	 */

	public static void main( final String args[] )
	{

		final File file = new File( AppUtils.getBaseDirectory( TrackMate.class ), "samples/FakeTracks.xml" );

		// 1 - Load test spots
		System.out.println( "Opening file: " + file.getAbsolutePath() );
		final TmXmlReader reader = new TmXmlReader( file );
		final Model model = reader.getModel();
		final ImagePlus imp = reader.readImage();

		System.out.println( "Spots: " + model.getSpots() );
		System.out.println( "Found " + model.getTrackModel().nTracks( false ) + " tracks in the file:" );
		System.out.println( "Track features: " );
		System.out.println();

		// 2 - Track the test spots
		final long start = System.currentTimeMillis();
		final Map< String, Object > settings = new HashMap<>();
		settings.put( KEY_LINKING_MAX_DISTANCE, 15d );
		final NearestNeighborTracker tracker = new NearestNeighborTracker( model.getSpots(), settings );
		tracker.setLogger( Logger.DEFAULT_LOGGER );

		if ( !tracker.checkInput() )
			System.err.println( "Error checking input: " + tracker.getErrorMessage() );
		if ( !tracker.process() )
			System.err.println( "Error in process: " + tracker.getErrorMessage() );
		final long end = System.currentTimeMillis();
		model.setTracks( tracker.getResult(), true );

		// 3 - Print out results for testing
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println( "Found " + model.getTrackModel().nTracks( false ) + " final tracks." );
		System.out.println( "Whole tracking done in " + ( end - start ) + " ms." );
		System.out.println();

		// 5 - Display tracks
		// Load Image
		ij.ImageJ.main( args );

		final TrackMateModelView sd2d = new HyperStackDisplayer( model, new SelectionModel( model ), imp, DisplaySettings.defaultStyle().copy() );
		sd2d.render();
	}
}
