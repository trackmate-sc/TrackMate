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

import java.util.Set;

import org.jgrapht.alg.util.NeighborCache;
import org.jgrapht.graph.SimpleDirectedGraph;

public class RecursiveCumSum< V, E >
{

	private final NeighborCache< V, E > cache;

	private final Function2< V, V > function;

	public RecursiveCumSum( final SimpleDirectedGraph< V, E > graph, final Function2< V, V > function )
	{
		this.cache = new NeighborCache<>( graph );
		this.function = function;
	}

	public V apply( V current )
	{

		Set< V > children = cache.successorsOf( current );

		if ( children.size() == 0 )
		{
			// It is a leaf
			return current;
		}

		V val = current;
		for ( V child : children )
			function.compute( val, apply( child ), val );

		return val;

	}

}
