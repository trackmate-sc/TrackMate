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
package fiji.plugin.trackmate.tracking.kalman;

import java.io.File;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.ImagePlus;

public class KalmanTrackerInteractiveTest2
{

	public static void main( final String[] args )
	{
		final File file = new File( "samples/FakeTracks.xml" );
		final TmXmlReader reader = new TmXmlReader( file );
		if ( !reader.isReadingOk() )
		{
			System.out.println( reader.getErrorMessage() );
			return;
		}

		final Model model = reader.getModel();
		final SpotCollection spots = model.getSpots();

		final ImagePlus imp = reader.readImage();
		final KalmanTracker tracker = new KalmanTracker( spots, 15d, 2, 15d, null );
		tracker.setLogger( Logger.DEFAULT_LOGGER );
		if ( !tracker.checkInput() || !tracker.process() )
		{
			System.out.println( tracker.getErrorMessage() );
			return;
		}

		final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph = tracker.getResult();
		model.setTracks( graph, false );

		model.beginUpdate();
		try
		{
			final SpotCollection predictions = tracker.getPredictions();
			for ( final Integer frame : predictions.keySet() )
				for ( final Spot spot : predictions.iterable( frame, true ) )
					model.addSpotTo( spot, frame );
		}
		finally
		{
			model.endUpdate();
		}

		ij.ImageJ.main( args );
		final SelectionModel selectionModel = new SelectionModel( model );

		final DisplaySettings ds = DisplaySettings.defaultStyle().copy();
		ds.setSpotColorBy( TrackMateObject.SPOTS, Spot.QUALITY );

		final HyperStackDisplayer view = new HyperStackDisplayer( model, selectionModel, imp, ds );
		view.render();
	}
}
