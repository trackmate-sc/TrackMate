package fiji.plugin.trackmate.features.edges;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.features.FeatureAnalyzer;

import java.util.Collection;

import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;

import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * Interface for analyzers that can compute scalar numerical features for an
 * edge of a TrackMate model. An edge, or a link, is the single link that exists
 * between two spots after tracking.
 *
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com>
 */
public interface EdgeAnalyzer extends Benchmark, FeatureAnalyzer, MultiThreaded
{

	/**
	 * Scores a collection of link between two spots. The results must be stored
	 * in the {@link FeatureModel}.
	 * <p>
	 * Note: ideally concrete implementation should work in a multi-threaded
	 * fashion for performance reason, when possible.
	 *
	 * @param edges
	 *            the collection of edges whose features are to be calculated.
	 * @param model
	 *            the {@link Model} they belong to.
	 */
	public void process( final Collection< DefaultWeightedEdge > edges, Model model );

	/**
	 * Returns <code>true</code> if this analyzer is a local analyzer. That is:
	 * a modification that affects only one edge requires the edge features to
	 * be re-calculated only for this edge. If <code>false</code>, any model
	 * modification involving an edge will trigger a recalculation over the
	 * whole track this edge belong to.
	 * <p>
	 * Example of local edge feature: the edge length (distance between the two
	 * spots). This one does not depend on other edge values.
	 * <p>
	 * Example of non-local edge feature: the local curvature of the trajectory,
	 * which depends on the neighbor edges.
	 */
	public boolean isLocal();

}
