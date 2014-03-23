package fiji.plugin.trackmate.graph;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.tracking.TrackableObject;

public class TimeDirectedDepthFirstIterator<T extends TrackableObject> extends SortedDepthFirstIterator<T, DefaultWeightedEdge> {

	public TimeDirectedDepthFirstIterator(final Graph<T, DefaultWeightedEdge> g, final T startVertex) {
		super(g, startVertex, null);
	}
	
	
	
    @Override
	protected void addUnseenChildrenOf(final T vertex) {
    	
    	final int ts = vertex.frame();
        for (final DefaultWeightedEdge edge : specifics.edgesOf(vertex)) {
            if (nListeners != 0) {
                fireEdgeTraversed(createEdgeTraversalEvent(edge));
            }

            final T oppositeV = Graphs.getOppositeVertex(graph, edge, vertex);
            final int tt = oppositeV.frame();
            if (tt <= ts) {
            	continue;
            }

            if ( seen.containsKey(oppositeV)) {
                encounterVertexAgain(oppositeV, edge);
            } else {
                encounterVertex(oppositeV, edge);
            }
        }
    }

	
	
}