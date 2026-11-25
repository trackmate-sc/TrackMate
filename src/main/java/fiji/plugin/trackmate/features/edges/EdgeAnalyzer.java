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
package fiji.plugin.trackmate.features.edges;

import java.util.Collection;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.features.FeatureAnalyzer;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;

/**
 * Interface for analyzers that can compute scalar numerical features for an
 * edge of a TrackMate model. An edge, or a link, is the single link that exists
 * between two spots after tracking.
 *
 * @author Jean-Yves Tinevez &lt;jeanyves.tinevez@gmail.com&gt;
 */
public interface EdgeAnalyzer extends Benchmark, FeatureAnalyzer, MultiThreaded
{

	/**
	 * Scores a collection of link between two spots. The results must be stored
	 * in the {@link fiji.plugin.trackmate.FeatureModel}.
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
	 * 
	 * @return whether this analyzer is a local analyzer.
	 */
	public boolean isLocal();

}
