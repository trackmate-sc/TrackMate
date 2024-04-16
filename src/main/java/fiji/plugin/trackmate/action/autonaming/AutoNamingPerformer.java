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
package fiji.plugin.trackmate.action.autonaming;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.DepthFirstIterator;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.graph.ConvexBranchesDecomposition;
import fiji.plugin.trackmate.graph.ConvexBranchesDecomposition.TrackBranchDecomposition;
import fiji.plugin.trackmate.graph.TimeDirectedNeighborIndex;

public class AutoNamingPerformer
{

	public static void autoNameSpots( final Model model, final AutoNamingRule rule )
	{
		final TimeDirectedNeighborIndex neighborIndex = model.getTrackModel().getDirectedNeighborIndex();
		for ( final Integer trackID : model.getTrackModel().unsortedTrackIDs( true ) )
		{
			final TrackBranchDecomposition branchDecomposition = ConvexBranchesDecomposition.processTrack( trackID, model.getTrackModel(), neighborIndex, true, false );
			final SimpleDirectedGraph< List< Spot >, DefaultEdge > branchGraph = ConvexBranchesDecomposition.buildBranchGraph( branchDecomposition );
			processTrack( rule, model.getTrackModel(), branchGraph );
		}
	}

	private static void processTrack(
			final AutoNamingRule rule, 
			final TrackModel model,
			final SimpleDirectedGraph< List< Spot >, DefaultEdge > graph )
	{
		// Find the roots. Might be several.
		final List< List< Spot > > roots = graph.vertexSet().stream()
				.filter( key -> graph.incomingEdgesOf( key ).size() == 0 )
				.collect( Collectors.toList() );

		for ( final List< Spot > root : roots )
		{
			// Name the spots in the root branch.
			final Spot first = root.get( 0 );
			rule.nameRoot( first, model );

			// Other spots in the root branch.
			Spot predecessor = first;
			for ( int i = 1; i < root.size(); i++ )
			{
				final Spot current = root.get( i );
				rule.nameSpot( current, predecessor );
				predecessor = current;
			}

			// Iterate through branch, settings the name of children.
			final DepthFirstIterator< List< Spot >, DefaultEdge > it = new DepthFirstIterator<>( graph, root );
			while ( it.hasNext() )
			{
				final List< Spot > currentBranch = it.next();

				// Collect children branches.
				final List< List< Spot > > childrenBranches = new ArrayList<>();
				final Set< DefaultEdge > edges = graph.outgoingEdgesOf( currentBranch );
				for ( final DefaultEdge edge : edges )
				{
					final List< Spot > cb = Graphs.getOppositeVertex( graph, edge, currentBranch );
					childrenBranches.add( cb );
				}

				// Build siblings spots (first one of the children branch.
				final Collection< Spot > siblings = new ArrayList<>( childrenBranches.size() );
				for ( final List< Spot > cb : childrenBranches )
					siblings.add( cb.get( 0 ) );

				// Collect mother spot (last one of current branch).
				final Spot mother = currentBranch.get( currentBranch.size() - 1 );

				// Name the branch first spots.
				rule.nameBranches( mother, siblings );
				
				// Name the spots inside each branch.
				for ( final List< Spot > cb : childrenBranches )
				{
					Spot parent = cb.get( 0 );
					for ( int i = 1; i < cb.size(); i++ )
					{
						final Spot current = cb.get( i );
						rule.nameSpot( current, parent );
						parent = current;
					}
				}
			}
		}
	}
}
