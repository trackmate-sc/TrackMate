/**
 *
 */
package fiji.plugin.trackmate.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.traverse.AbstractGraphIterator;
import org.jgrapht.util.TypeUtil;

/**
 * A depth-first iterator, that - when branching - chooses the next vertex
 * according to a specified comparator.
 * <p>
 * I had to copy-paste whole sections of JGraphT code to make this one: I could
 * not extend the desired class, for the interesting method and field were
 * private.
 *
 * @author Jean-Yves Tinevez 2012-2013
 */
public class SortedDepthFirstIterator< V, E > extends AbstractGraphIterator< V, E >
{

	// ~ Static fields/initializers
	// ---------------------------------------------

	private static final int CCS_BEFORE_COMPONENT = 1;

	private static final int CCS_WITHIN_COMPONENT = 2;

	private static final int CCS_AFTER_COMPONENT = 3;

	/**
	 * Sentinel object. Unfortunately, we can't use null, because ArrayDeque
	 * won't accept those. And we don't want to rely on the caller to provide a
	 * sentinel object for us. So we have to play typecasting games.
	 */
	private static final Object SENTINEL = new Object();

	/**
	 * Standard vertex visit state enumeration.
	 */
	private static enum VisitColor
	{
		/**
		 * Vertex has not been returned via iterator yet.
		 */
		WHITE,

		/**
		 * Vertex has been returned via iterator, but we're not done with all of
		 * its out-edges yet.
		 */
		GRAY,

		/**
		 * Vertex has been returned via iterator, and we're done with all of its
		 * out-edges.
		 */
		BLACK
	}

	// ~ Instance fields
	// --------------------------------------------------------

	private final Deque< Object > stack = new ArrayDeque< >();

	private final ConnectedComponentTraversalEvent ccFinishedEvent =
			new ConnectedComponentTraversalEvent(
					this,
					ConnectedComponentTraversalEvent.CONNECTED_COMPONENT_FINISHED );

	private final ConnectedComponentTraversalEvent ccStartedEvent =
			new ConnectedComponentTraversalEvent(
					this,
					ConnectedComponentTraversalEvent.CONNECTED_COMPONENT_STARTED );

	private Iterator< V > vertexIterator = null;

	/**
	 * Stores the vertices that have been seen during iteration and (optionally)
	 * some additional traversal info regarding each vertex.
	 */
	protected Map< V, VisitColor > seen = new HashMap< >();

	private V startVertex;

	protected Specifics< V, E > specifics;

	protected final Comparator< V > comparator;

	/** The connected component state */
	private int state = CCS_BEFORE_COMPONENT;

	// ~ Constructors
	// -----------------------------------------------------------

	/**
	 * Creates a new iterator for the specified graph. Iteration will start at
	 * the specified start vertex. If the specified start vertex is <code>
	 * null</code>, Iteration will start at an arbitrary graph vertex.
	 *
	 * @param g
	 *            the graph to be iterated.
	 * @param startVertex
	 *            the vertex iteration to be started.
	 *
	 * @throws IllegalArgumentException
	 *             if <code>g==null</code> or does not contain
	 *             <code>startVertex</code>
	 */
	public SortedDepthFirstIterator( final Graph< V, E > g, final V startVertex, final Comparator< V > comparator )
	{
		super( g );
		this.comparator = comparator;

		specifics = createGraphSpecifics( g );
		vertexIterator = g.vertexSet().iterator();
		setCrossComponentTraversal( startVertex == null );

		if ( startVertex == null )
		{
			// pick a start vertex if graph not empty
			if ( vertexIterator.hasNext() )
			{
				this.startVertex = vertexIterator.next();
			}
			else
			{
				this.startVertex = null;
			}
		}
		else if ( g.containsVertex( startVertex ) )
		{
			this.startVertex = startVertex;
		}
		else
		{
			throw new IllegalArgumentException(
					"graph must contain the start vertex" );
		}
	}

	// ~ Methods
	// ----------------------------------------------------------------

	/**
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext()
	{
		if ( startVertex != null )
		{
			encounterStartVertex();
		}

		if ( isConnectedComponentExhausted() )
		{
			if ( state == CCS_WITHIN_COMPONENT )
			{
				state = CCS_AFTER_COMPONENT;
				if ( nListeners != 0 )
				{
					fireConnectedComponentFinished( ccFinishedEvent );
				}
			}

			if ( isCrossComponentTraversal() )
			{
				while ( vertexIterator.hasNext() )
				{
					final V v = vertexIterator.next();

					if ( !seen.containsKey( v ) )
					{
						encounterVertex( v, null );
						state = CCS_BEFORE_COMPONENT;

						return true;
					}
				}

				return false;
			}
			return false;
		}
		return true;
	}

	/**
	 * @see java.util.Iterator#next()
	 */
	@Override
	public V next()
	{

		if ( startVertex != null )
		{
			encounterStartVertex();
		}

		if ( hasNext() )
		{
			if ( state == CCS_BEFORE_COMPONENT )
			{
				state = CCS_WITHIN_COMPONENT;
				if ( nListeners != 0 )
				{
					fireConnectedComponentStarted( ccStartedEvent );
				}
			}

			final V nextVertex = provideNextVertex();
			if ( nListeners != 0 )
			{
				fireVertexTraversed( createVertexTraversalEvent( nextVertex ) );
			}

			addUnseenChildrenOf( nextVertex );

			return nextVertex;
		}
		throw new NoSuchElementException();
	}

	/**
	 * Called when a vertex has been finished (meaning is dependent on traversal
	 * represented by subclass).
	 *
	 * @param vertex
	 *            vertex which has been finished
	 */
	private void finishVertex( final V vertex )
	{
		if ( nListeners != 0 )
		{
			fireVertexFinished( createVertexTraversalEvent( vertex ) );
		}
	}

	// -------------------------------------------------------------------------

	private static < V, E > Specifics< V, E > createGraphSpecifics( final Graph< V, E > g )
	{
		if (g.getType().isDirected())
			return new DirectedSpecifics< >( g );

		return new UndirectedSpecifics< >( g );
	}

	/**
	 * This is where we add the multiple children in proper sorted order.
	 */
	protected void addUnseenChildrenOf( final V vertex )
	{

		// Retrieve target vertices, and sort them in a list
		final List< V > sortedChildren = new ArrayList< >();
		// Keep a map of matching edges so that we can retrieve them in the same
		// order
		final Map< V, E > localEdges = new HashMap< >();

		for ( final E edge : specifics.edgesOf( vertex ) )
		{
			final V oppositeV = Graphs.getOppositeVertex( graph, edge, vertex );
			if ( !seen.containsKey( oppositeV ) )
			{
				sortedChildren.add( oppositeV );
			}
			localEdges.put( oppositeV, edge );
		}
		Collections.sort( sortedChildren, Collections.reverseOrder( comparator ) );

		final Iterator< V > it = sortedChildren.iterator();
		while ( it.hasNext() )
		{
			final V child = it.next();

			if ( nListeners != 0 )
			{
				fireEdgeTraversed( createEdgeTraversalEvent( localEdges.get( child ) ) );
			}

			if ( seen.containsKey( child ) )
			{
				encounterVertexAgain( child, localEdges.get( child ) );
			}
			else
			{
				encounterVertex( child, localEdges.get( child ) );
			}
		}
	}

	private void encounterStartVertex()
	{
		encounterVertex( startVertex, null );
		startVertex = null;
	}

	// ~ Depth-first iterator methods
	// ----------------------------------------------------------------

	private boolean isConnectedComponentExhausted()
	{
		for ( ;; )
		{
			if ( stack.isEmpty() ) { return true; }
			if ( stack.getLast() != SENTINEL )
			{
				// Found a non-sentinel.
				return false;
			}

			// Found a sentinel: pop it, record the finish time,
			// and then loop to check the rest of the stack.

			// Pop null we peeked at above.
			stack.removeLast();

			// This will pop corresponding vertex to be recorded as finished.
			recordFinish();
		}
	}

	/**
	 * @param edge  
	 */
	protected void encounterVertex( final V vertex, final E edge )
	{
		seen.put( vertex, VisitColor.WHITE );
		stack.addLast( vertex );
	}

	/**
	 * @param edge  
	 */
	protected void encounterVertexAgain( final V vertex, final E edge )
	{
		final VisitColor color = seen.get( vertex );
		if ( color != VisitColor.WHITE )
		{
			// We've already visited this vertex; no need to mess with the
			// stack (either it's BLACK and not there at all, or it's GRAY
			// and therefore just a sentinel).
			return;
		}

		// Since we've encountered it before, and it's still WHITE, it
		// *must* be on the stack. Use removeLastOccurrence on the
		// assumption that for typical topologies and traversals,
		// it's likely to be nearer the top of the stack than
		// the bottom of the stack.
		final boolean found = stack.removeLastOccurrence( vertex );
		assert ( found );
		stack.addLast( vertex );
	}

	private V provideNextVertex()
	{
		V v;
		for ( ;; )
		{
			final Object o = stack.removeLast();
			if ( o == SENTINEL )
			{
				// This is a finish-time sentinel we previously pushed.
				recordFinish();
				// Now carry on with another pop until we find a non-sentinel
			}
			else
			{
				// Got a real vertex to start working on
				v = TypeUtil.uncheckedCast( o );
				break;
			}
		}

		// Push a sentinel for v onto the stack so that we'll know
		// when we're done with it.
		stack.addLast( v );
		stack.addLast( SENTINEL );
		seen.put( v, VisitColor.GRAY );
		return v;
	}

	private void recordFinish()
	{
		final V v = TypeUtil.uncheckedCast( stack.removeLast() );
		seen.put( v, VisitColor.BLACK );
		finishVertex( v );
	}

	// ~ Inner Classes
	// ----------------------------------------------------------

	/**
	 * Provides unified interface for operations that are different in directed
	 * graphs and in undirected graphs.
	 */
	abstract static class Specifics< VV, EE >
	{
		/**
		 * Returns the edges outgoing from the specified vertex in case of
		 * directed graph, and the edge touching the specified vertex in case of
		 * undirected graph.
		 *
		 * @param vertex
		 *            the vertex whose outgoing edges are to be returned.
		 *
		 * @return the edges outgoing from the specified vertex in case of
		 *         directed graph, and the edge touching the specified vertex in
		 *         case of undirected graph.
		 */
		public abstract Set< ? extends EE > edgesOf( VV vertex );
	}

	private static class DirectedSpecifics< VV, EE > extends Specifics< VV, EE >
	{
		private final Graph< VV, EE > graph;

		/**
		 * Creates a new DirectedSpecifics object.
		 *
		 * @param g
		 *            the graph for which this specifics object to be created.
		 */
		public DirectedSpecifics( final Graph< VV, EE > g )
		{
			graph = g;
		}

		@Override
		public Set< ? extends EE > edgesOf( final VV vertex )
		{
			return graph.outgoingEdgesOf( vertex );
		}
	}

	private static class UndirectedSpecifics< VV, EE > extends Specifics< VV, EE >
	{
		private final Graph< VV, EE > graph;

		/**
		 * Creates a new UndirectedSpecifics object.
		 *
		 * @param g
		 *            the graph for which this specifics object to be created.
		 */
		public UndirectedSpecifics( final Graph< VV, EE > g )
		{
			graph = g;
		}

		@Override
		public Set< EE > edgesOf( final VV vertex )
		{
			return graph.edgesOf( vertex );
		}
	}

}
