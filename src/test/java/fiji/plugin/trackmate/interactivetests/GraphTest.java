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

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.traverse.GraphIterator;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.graph.GraphUtils;
import fiji.plugin.trackmate.graph.TimeDirectedNeighborIndex;

public class GraphTest
{

	public static void main( final String[] args )
	{

		final Model model = getExampleModel();
//		final Model model = getComplicatedExample(); // not a tree, will fail.

		countOverallLeaves( model );
		pickLeavesOfOneTrack( model );

		System.out.println( GraphUtils.toString( model.getTrackModel() ) );

	}

	private static void pickLeavesOfOneTrack( final Model model )
	{
		final TimeDirectedNeighborIndex cache = model.getTrackModel().getDirectedNeighborIndex();
		final TreeSet< Spot > spots = new TreeSet<>( Spot.frameComparator );
		spots.addAll( model.getTrackModel().vertexSet() );
		final Spot first = spots.first();
		final GraphIterator< Spot, DefaultWeightedEdge > iterator = model.getTrackModel().getDepthFirstIterator( first, true );

		while ( iterator.hasNext() )
		{
			final Spot spot = iterator.next();
			final boolean isBranching = cache.successorsOf( spot ).size() > 1;
			if ( isBranching )
			{
				System.out.println( " - " + spot + " is branching to " + cache.successorsOf( spot ).size() + " children." );
			}
			else
			{
				final boolean isLeaf = cache.successorsOf( spot ).size() == 0;
				if ( isLeaf )
				{
					System.out.println( " - " + spot + " is a leaf." );
				}
				else
				{
					System.out.println( " - " + spot );
				}
			}
		}
	}

	private static void countOverallLeaves( final Model model )
	{
		final TimeDirectedNeighborIndex cache = model.getTrackModel().getDirectedNeighborIndex();
		int nleaves = 0;
		final Set< Spot > spots = model.getTrackModel().vertexSet();
		for ( final Spot spot : spots )
		{
			if ( cache.successorsOf( spot ).size() == 0 )
			{
				nleaves++;
			}
		}
		System.out.println( "Iterated over " + spots.size() + " spots." );
		System.out.println( "Found " + nleaves + " leaves." );
	}

	public static final Model getExampleModel()
	{

		final Model model = new Model();

		// Create spots

		final Spot root = new SpotBase( 3d, 0d, 0d, 1d, -1d, "Zygote" );

		final Spot AB = new SpotBase( 0d, 1d, 0d, 1d, -1d, "AB" );
		final Spot P1 = new SpotBase( 3d, 1d, 0d, 1d, -1d, "P1" );

		final Spot P2 = new SpotBase( 4d, 2d, 0d, 1d, -1d, "P2" );
		final Spot EMS = new SpotBase( 2d, 2d, 0d, 1d, -1d, "EMS" );

		final Spot P3 = new SpotBase( 5d, 3d, 0d, 1d, -1d, "P3" );
		final Spot C = new SpotBase( 3d, 3d, 0d, 1d, -1d, "C" );
		final Spot E = new SpotBase( 1d, 3d, 0d, 1d, -1d, "E" );
		final Spot MS = new SpotBase( 2d, 3d, 0d, 1d, -1d, "MS" );
		final Spot AB3 = new SpotBase( 0d, 3d, 0d, 1d, -1d, "AB" );

		final Spot D = new SpotBase( 4d, 4d, 0d, 1d, -1d, "D" );
		final Spot P4 = new SpotBase( 5d, 4d, 0d, 1d, -1d, "P4" );

		// Add them to the graph

		model.beginUpdate();
		try
		{

			model.addSpotTo( root, 0 );

			model.addSpotTo( AB, 1 );
			model.addSpotTo( P1, 1 );

			model.addSpotTo( P2, 2 );
			model.addSpotTo( EMS, 2 );

			model.addSpotTo( P3, 3 );
			model.addSpotTo( C, 3 );
			model.addSpotTo( E, 3 );
			model.addSpotTo( MS, 3 );
			model.addSpotTo( AB3, 3 );

			model.addSpotTo( D, 4 );
			model.addSpotTo( P4, 4 );

			// Create links

			model.addEdge( root, AB, 1 );
			model.addEdge( root, P1, 1 );

			model.addEdge( P1, P2, 1 );
			model.addEdge( P1, EMS, 1 );

			model.addEdge( AB, AB3, 1 );

			model.addEdge( EMS, E, 1 );
			model.addEdge( EMS, MS, 1 );

			model.addEdge( P2, P3, 1 );
			model.addEdge( P2, C, 1 );

			model.addEdge( P3, P4, 1 );
			model.addEdge( P3, D, 1 );

		}
		finally
		{
			model.endUpdate();
		}

		// Done!

		return model;
	}

	public static final Model getComplicatedExample()
	{
		final Model model = getExampleModel();

		// Retrieve target spot by name
		Spot P3 = null;
		for ( final Iterator< Spot > it = model.getSpots().iterator( false ); it.hasNext(); )
		{
			final Spot spot = it.next();
			if ( spot.getName().equals( "P3" ) )
			{
				P3 = spot;
				break;
			}
		}

		// Update model
		model.beginUpdate();
		try
		{
			// new spots
			final Spot Q1 = model.addSpotTo( new SpotBase( 0d, 0d, 0d, 1d, -1d, "Q1" ), 0 );
			final Spot Q2 = model.addSpotTo( new SpotBase( 0d, 0d, 0d, 1d, -1d, "Q2" ), 1 );
			final Spot Q3 = model.addSpotTo( new SpotBase( 0d, 0d, 0d, 1d, -1d, "Q3" ), 2 );
			// new links
			model.addEdge( Q1, Q2, -1 );
			model.addEdge( Q2, Q3, -1 );
			model.addEdge( Q3, P3, -1 );
		}
		finally
		{
			model.endUpdate();
		}

		return model;
	}
}
