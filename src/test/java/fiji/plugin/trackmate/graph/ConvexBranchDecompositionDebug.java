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
package fiji.plugin.trackmate.graph;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.graph.ConvexBranchesDecomposition.TrackBranchDecomposition;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class ConvexBranchDecompositionDebug
{
	public static void main( final String[] args )
	{

		final Spot sa0 = new SpotBase( 0, 0, 0, 1, -1, "SA_0" );
		final Spot sa1 = new SpotBase( 0, 0, 0, 1, -1, "SA_1" );
		final Spot sa3 = new SpotBase( 0, 0, 0, 1, -1, "SA_3" );
		final Spot sa4 = new SpotBase( 0, 0, 0, 1, -1, "SA_4" );

		final Spot sb0 = new SpotBase( 0, 0, 0, 1, -1, "SB_0" );
		final Spot sb1 = new SpotBase( 0, 0, 0, 1, -1, "SB_1" );
		final Spot sb3 = new SpotBase( 0, 0, 0, 1, -1, "SB_3" );
		final Spot sb4 = new SpotBase( 0, 0, 0, 1, -1, "SB_4" );

		final Spot nexus = new SpotBase( 0, 0, 0, 1, -1, "NEXUS" );

		final SpotCollection spots = new SpotCollection();
		spots.add( sa0, 0 );
		spots.add( sb0, 0 );
		spots.add( sa1, 1 );
		spots.add( sb1, 1 );
		spots.add( nexus, 2 );
		spots.add( sa3, 3 );
		spots.add( sb3, 3 );
		spots.add( sa4, 4 );
		spots.add( sb4, 4 );

		final Model model = new Model();
		model.setSpots( spots, false );

		model.addEdge( sa0, sa1, -2 );
		model.addEdge( sb0, sb1, -2 );
		model.addEdge( sa1, nexus, -2 );
		model.addEdge( sb1, nexus, -2 );
		model.addEdge( nexus, sa3, -2 );
		model.addEdge( nexus, sb3, -2 );
		model.addEdge( sa3, sa4, -2 );
		model.addEdge( sb3, sb4, -2 );

		final SelectionModel sm = new SelectionModel( model );
		final TrackScheme trackScheme = new TrackScheme( model, sm, DisplaySettings.defaultStyle().copy() );
		trackScheme.render();

		final TrackModel tm = model.getTrackModel();
		final Integer trackID = tm.trackIDOf( sa0 );
		final TimeDirectedNeighborIndex neighborIndex = tm.getDirectedNeighborIndex();

		final TrackBranchDecomposition branchDecomposition = ConvexBranchesDecomposition.processTrack( trackID, tm, neighborIndex, false, false );
		System.out.println( branchDecomposition );

		ConvexBranchesDecomposition.buildBranchGraph( branchDecomposition );
	}
}
