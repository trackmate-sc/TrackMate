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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.scijava.util.AppUtils;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.SpotFeatureGrapher;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class SpotFeatureGrapherExample
{

	public static void main( final String[] args )
	{

		// Load objects
		final File file = new File( AppUtils.getBaseDirectory( TrackMate.class ), "samples/FakeTracks.xml" );
		final TmXmlReader reader = new TmXmlReader( file );
		final Model model = reader.getModel();
		final SelectionModel selectionModel = new SelectionModel( model );

		final List< String > Y = new ArrayList<>( 1 );
		Y.add( Spot.POSITION_T );
		final List< Spot > spots = new ArrayList<>( model.getSpots().getNSpots( true ) );
		for ( final Iterator< Spot > it = model.getSpots().iterator( true ); it.hasNext(); )
			spots.add( it.next() );

		final SpotFeatureGrapher grapher = new SpotFeatureGrapher(
				spots,
				Spot.POSITION_X,
				Y,
				model,
				selectionModel,
				DisplaySettings.defaultStyle().copy(),
				true );
		final JFrame frame = grapher.render();
		frame.setLocationRelativeTo( null );
		frame.setVisible( true );

		final TrackIndexAnalyzer analyzer = new TrackIndexAnalyzer();
		analyzer.process( model.getTrackModel().trackIDs( true ), model );
		// needed for trackScheme
		final TrackScheme trackScheme = new TrackScheme( model, new SelectionModel( model ), DisplaySettings.defaultStyle().copy() );
		trackScheme.render();

	}

	/**
	 * Another example: spots that go in spiral
	 */
	@SuppressWarnings( "unused" )
	private static Model getSpiralModel()
	{

		final int N_SPOTS = 50;
		final List< Spot > spots = new ArrayList<>( N_SPOTS );
		final SpotCollection sc = new SpotCollection();
		for ( int i = 0; i < N_SPOTS; i++ )
		{
			final double x = 100d + 100 * i / 100. * Math.cos( i / 100. * 5 * 2 * Math.PI );
			final double y = 100d + 100 * i / 100. * Math.sin( i / 100. * 5 * 2 * Math.PI );
			final double z = 0d;
			final Spot spot = new SpotBase( x, y, z, 2d, -1d );
			spot.putFeature( Spot.POSITION_T, Double.valueOf( i ) );

			spots.add( spot );

			final List< Spot > ts = new ArrayList<>( 1 );
			ts.add( spot );
			sc.put( i, ts );
			spot.putFeature( SpotCollection.VISIBILITY, SpotCollection.ONE );
		}

		final Model model = new Model();
		model.setSpots( sc, false );

		final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph = new SimpleWeightedGraph<>( DefaultWeightedEdge.class );
		for ( final Spot spot : spots )
			graph.addVertex( spot );

		Spot source = spots.get( 0 );
		for ( int i = 1; i < N_SPOTS; i++ )
		{
			final Spot target = spots.get( i );
			final DefaultWeightedEdge edge = graph.addEdge( source, target );
			graph.setEdgeWeight( edge, 1 );
			source = target;
		}
		model.setTracks( graph, true );

		return model;
	}
}
