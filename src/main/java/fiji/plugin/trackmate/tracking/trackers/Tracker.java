package fiji.plugin.trackmate.tracking.trackers;

import net.imglib2.algorithm.OutputAlgorithm;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.tracking.TrackableObject;

public interface Tracker<T extends TrackableObject> extends OutputAlgorithm<SimpleWeightedGraph<T, DefaultWeightedEdge>> {
	
	public void setLogger(final Logger logger);

}
