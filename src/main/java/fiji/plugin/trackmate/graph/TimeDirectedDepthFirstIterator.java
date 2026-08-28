/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
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
/**
 * 
 */
package fiji.plugin.trackmate.graph;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;

public class TimeDirectedDepthFirstIterator extends SortedDepthFirstIterator< Spot, DefaultWeightedEdge >
{

	private final boolean reversed;

	public TimeDirectedDepthFirstIterator( final Graph< Spot, DefaultWeightedEdge > g, final Spot startVertex )
	{
		this( g, startVertex, false );
	}

	public TimeDirectedDepthFirstIterator( final Graph< Spot, DefaultWeightedEdge > g, final Spot startVertex, final boolean reversed )
	{
		super( g, startVertex, null );
		this.reversed = reversed;
	}

	@Override
	protected void addUnseenChildrenOf( final Spot vertex )
	{

		final int ts = vertex.getFeature( Spot.FRAME ).intValue();
		for ( final DefaultWeightedEdge edge : specifics.edgesOf( vertex ) )
		{
			if ( nListeners != 0 )
				fireEdgeTraversed( createEdgeTraversalEvent( edge ) );

			final Spot oppositeV = Graphs.getOppositeVertex( graph, edge, vertex );
			final int tt = oppositeV.getFeature( Spot.FRAME ).intValue();
			if ( reversed ? tt >= ts : tt <= ts )
				continue;

			if ( seen.containsKey( oppositeV ) )
				encounterVertexAgain( oppositeV, edge );
			else
				encounterVertex( oppositeV, edge );
		}
	}

}
