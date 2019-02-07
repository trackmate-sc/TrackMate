package fiji.plugin.trackmate.graph;

import java.util.Set;

import org.jgrapht.alg.util.NeighborCache;
import org.jgrapht.graph.SimpleDirectedGraph;

public class RecursiveCumSum<V, E> {

	private final NeighborCache<V, E> cache;
	private final Function2<V, V> function;

	public RecursiveCumSum(final SimpleDirectedGraph<V, E> graph, final Function2<V, V> function) {
		this.cache = new NeighborCache<>(graph);
		this.function = function;
	}
	
	public V apply(V current) {
		
		Set<V> children = cache.successorsOf(current);
		
		if (children.size() == 0) {
			// It is a leaf
			return current;
		}
		
		V val = current;
		for (V child : children) 
			function.compute(val, apply(child), val);

		return val;
		
	}

}
