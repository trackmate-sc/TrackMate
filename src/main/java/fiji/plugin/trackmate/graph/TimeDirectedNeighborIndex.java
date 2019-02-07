package fiji.plugin.trackmate.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.util.NeighborCache;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.util.ModifiableInteger;

import fiji.plugin.trackmate.Spot;

public class TimeDirectedNeighborIndex extends NeighborCache< Spot, DefaultWeightedEdge >
{

	// ~ Instance fields
	// --------------------------------------------------------

	Map< Spot, Neighbors< Spot, DefaultWeightedEdge > > predecessorMap = new HashMap< >();

	Map< Spot, Neighbors< Spot, DefaultWeightedEdge > > successorMap = new HashMap< >();

	private final Graph< Spot, DefaultWeightedEdge > graph;

	// ~ Constructors
	// -----------------------------------------------------------

	public TimeDirectedNeighborIndex( final Graph< Spot, DefaultWeightedEdge > g )
	{
		super( g );
		this.graph = g;
	}

	// ~ Methods
	// ----------------------------------------------------------------

	/**
	 * Returns the set of vertices which are the predecessors of a specified
	 * vertex. The returned set is backed by the index, and will be updated when
	 * the graph changes as long as the index has been added as a listener to
	 * the graph.
	 *
	 * @param v
	 *            the vertex whose predecessors are desired
	 *
	 * @return all unique predecessors of the specified vertex
	 */
	@Override
	public Set< Spot > predecessorsOf( final Spot v )
	{
		return getPredecessors( v ).getNeighbors();
	}

	/**
	 * Returns the set of vertices which are the predecessors of a specified
	 * vertex. If the graph is a multigraph, vertices may appear more than once
	 * in the returned list. Because a list of predecessors can not be
	 * efficiently maintained, it is reconstructed on every invocation by
	 * duplicating entries in the neighbor set. It is thus more efficient to use
	 * {@link #predecessorsOf(Spot)} unless duplicate neighbors are required.
	 *
	 * @param v
	 *            the vertex whose predecessors are desired
	 *
	 * @return all predecessors of the specified vertex
	 */
	public List< Spot > predecessorListOf( final Spot v )
	{
		return getPredecessors( v ).getNeighborList();
	}

	/**
	 * Returns the set of vertices which are the successors of a specified
	 * vertex. The returned set is backed by the index, and will be updated when
	 * the graph changes as long as the index has been added as a listener to
	 * the graph.
	 *
	 * @param v
	 *            the vertex whose successors are desired
	 *
	 * @return all unique successors of the specified vertex
	 */
	@Override
	public Set< Spot > successorsOf( final Spot v )
	{
		return getSuccessors( v ).getNeighbors();
	}

	/**
	 * Returns the set of vertices which are the successors of a specified
	 * vertex. If the graph is a multigraph, vertices may appear more than once
	 * in the returned list. Because a list of successors can not be efficiently
	 * maintained, it is reconstructed on every invocation by duplicating
	 * entries in the neighbor set. It is thus more efficient to use
	 * {@link #successorsOf(Spot)} unless duplicate neighbors are required.
	 *
	 * @param v
	 *            the vertex whose successors are desired
	 *
	 * @return all successors of the specified vertex
	 */
	public List< Spot > successorListOf( final Spot v )
	{
		return getSuccessors( v ).getNeighborList();
	}

	/**
	 * @see org.jgrapht.event.GraphListener#edgeAdded(GraphEdgeChangeEvent)
	 */
	@Override
	public void edgeAdded( final GraphEdgeChangeEvent< Spot, DefaultWeightedEdge > e )
	{
		final DefaultWeightedEdge edge = e.getEdge();
		final Spot source = graph.getEdgeSource( edge );
		final Spot target = graph.getEdgeTarget( edge );

		// if a map does not already contain an entry,
		// then skip addNeighbor, since instantiating the map
		// will take care of processing the edge (which has already
		// been added)

		if ( successorMap.containsKey( source ) )
		{
			getSuccessors( source ).addNeighbor( target );
		}
		else
		{
			getSuccessors( source );
		}
		if ( predecessorMap.containsKey( target ) )
		{
			getPredecessors( target ).addNeighbor( source );
		}
		else
		{
			getPredecessors( target );
		}
	}

	/**
	 * @see org.jgrapht.event.GraphListener#edgeRemoved(GraphEdgeChangeEvent)
	 */
	@Override
	public void edgeRemoved( final GraphEdgeChangeEvent< Spot, DefaultWeightedEdge > e )
	{
		final DefaultWeightedEdge edge = e.getEdge();
		final Spot source = graph.getEdgeSource( edge );
		final Spot target = graph.getEdgeTarget( edge );
		if ( successorMap.containsKey( source ) )
		{
			successorMap.get( source ).removeNeighbor( target );
		}
		if ( predecessorMap.containsKey( target ) )
		{
			predecessorMap.get( target ).removeNeighbor( source );
		}
	}

	/**
	 * @see org.jgrapht.event.VertexSetListener#vertexAdded(GraphVertexChangeEvent)
	 */
	@Override
	public void vertexAdded( final GraphVertexChangeEvent< Spot > e )
	{
		// nothing to cache until there are edges
	}

	/**
	 * @see org.jgrapht.event.VertexSetListener#vertexRemoved(GraphVertexChangeEvent)
	 */
	@Override
	public void vertexRemoved( final GraphVertexChangeEvent< Spot > e )
	{
		predecessorMap.remove( e.getVertex() );
		successorMap.remove( e.getVertex() );
	}

	private Neighbors< Spot, DefaultWeightedEdge > getPredecessors( final Spot v )
	{
		Neighbors< Spot, DefaultWeightedEdge > neighbors = predecessorMap.get( v );
		if ( neighbors == null )
		{
			final List< Spot > nl = Graphs.neighborListOf( graph, v );
			final List< Spot > bnl = new ArrayList< >();
			final int ts = v.getFeature( Spot.FRAME ).intValue();
			for ( final Spot spot : nl )
			{
				final int tt = spot.getFeature( Spot.FRAME ).intValue();
				if ( tt < ts )
				{
					bnl.add( spot );
				}
			}
			neighbors = new Neighbors< >( v, bnl );
			predecessorMap.put( v, neighbors );
		}
		return neighbors;
	}

	private Neighbors< Spot, DefaultWeightedEdge > getSuccessors( final Spot v )
	{
		Neighbors< Spot, DefaultWeightedEdge > neighbors = successorMap.get( v );
		if ( neighbors == null )
		{
			final List< Spot > nl = Graphs.neighborListOf( graph, v );
			final List< Spot > bnl = new ArrayList< >();
			final int ts = v.getFeature( Spot.FRAME ).intValue();
			for ( final Spot spot : nl )
			{
				final int tt = spot.getFeature( Spot.FRAME ).intValue();
				if ( tt > ts )
				{
					bnl.add( spot );
				}
			}
			neighbors = new Neighbors< >( v, bnl );
			successorMap.put( v, neighbors );
		}
		return neighbors;
	}

	// ~ Inner Classes
	// ----------------------------------------------------------

	/**
	 * Stores cached neighbors for a single vertex. Includes support for live
	 * neighbor sets and duplicate neighbors.
	 */
	static class Neighbors< V, E >
	{
		private final Map< V, ModifiableInteger > neighborCounts =
				new LinkedHashMap< >();

		private final Set< V > neighborSet =
				Collections.unmodifiableSet(
						neighborCounts.keySet() );

		/**
		 * @param v  
		 */
		public Neighbors( final V v, final Collection< V > neighbors )
		{
			// add all current neighbors
			for ( final V neighbor : neighbors )
			{
				addNeighbor( neighbor );
			}
		}

		public void addNeighbor( final V v )
		{
			ModifiableInteger count = neighborCounts.get( v );
			if ( count == null )
			{
				count = new ModifiableInteger( 1 );
				neighborCounts.put( v, count );
			}
			else
			{
				count.increment();
			}
		}

		public void removeNeighbor( final V v )
		{
			final ModifiableInteger count = neighborCounts.get( v );
			if ( count == null ) { throw new IllegalArgumentException(
					"Attempting to remove a neighbor that wasn't present" ); }

			count.decrement();
			if ( count.getValue() == 0 )
			{
				neighborCounts.remove( v );
			}
		}

		public Set< V > getNeighbors()
		{
			return neighborSet;
		}

		public List< V > getNeighborList()
		{
			final List< V > neighbors = new ArrayList< >();
			for ( final Map.Entry< V, ModifiableInteger > entry : neighborCounts.entrySet() )
			{
				final V v = entry.getKey();
				final int count = entry.getValue().intValue();
				for ( int i = 0; i < count; i++ )
				{
					neighbors.add( v );
				}
			}
			return neighbors;
		}
	}

}
