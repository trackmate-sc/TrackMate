package fiji.plugin.trackmate.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.tracking.TrackableObject;

public class TimeDirectedSortedDepthFirstIterator<T extends TrackableObject>
		extends SortedDepthFirstIterator<T, DefaultWeightedEdge> {

	public TimeDirectedSortedDepthFirstIterator(
			final Graph<T, DefaultWeightedEdge> g, final T startVertex,
			final Comparator<T> comparator) {
		super(g, startVertex, comparator);
	}

	@Override
	protected void addUnseenChildrenOf(final T vertex) {

		// Retrieve target vertices, and sort them in a list
		final List<T> sortedChildren = new ArrayList<T>();
		// Keep a map of matching edges so that we can retrieve them in the same
		// order
		final Map<T, DefaultWeightedEdge> localEdges = new HashMap<T, DefaultWeightedEdge>();

		final int ts = vertex.frame();
		for (final DefaultWeightedEdge edge : specifics.edgesOf(vertex)) {

			final T oppositeV = Graphs.getOppositeVertex(graph, edge, vertex);
			final int tt = oppositeV.frame();
			if (tt <= ts) {
				continue;
			}

			if (!seen.containsKey(oppositeV)) {
				sortedChildren.add(oppositeV);
			}
			localEdges.put(oppositeV, edge);
		}

		Collections.sort(sortedChildren, Collections.reverseOrder(comparator));
		final Iterator<T> it = sortedChildren.iterator();
		while (it.hasNext()) {
			final T child = it.next();

			if (nListeners != 0) {
				fireEdgeTraversed(createEdgeTraversalEvent(localEdges
						.get(child)));
			}

			if (seen.containsKey(child)) {
				encounterVertexAgain(child, localEdges.get(child));
			} else {
				encounterVertex(child, localEdges.get(child));
			}
		}
	}

}