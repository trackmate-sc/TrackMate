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
package fiji.plugin.trackmate.tracking;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.OutputAlgorithm;

/**
 * This interface should be used when creating algorithms for linking objects
 * across multiple frames in time-lapse images.
 * <p>
 * A SpotTracker algorithm is simply expected to <b>create</b> a new
 * {@link SimpleWeightedGraph} from the spot collection help in the
 * {@link fiji.plugin.trackmate.Model} that is given to it. We use a simple
 * weighted graph:
 * <ul>
 * <li>Though the weights themselves are not used for subsequent steps, it is
 * suggested to use edge weight to report the cost of a link.
 * <li>The graph is undirected, however, some link direction can be retrieved
 * later on using the {@link Spot#FRAME} feature. The {@link SpotTracker}
 * implementation does not have to deal with this; only undirected edges are
 * created.
 * <li>Several links between two spots are not permitted.
 * <li>A link with the same spot for source and target is not allowed.
 * <li>A link with the source spot and the target spot in the same frame is not
 * allowed. This must be enforced by implementations.
 * </ul>
 * <p>
 * A {@link SpotTracker} implements {@link MultiThreaded}. If concrete
 * implementations are not multithreaded, they can safely ignore the associated
 * methods.
 */
public interface SpotTracker extends OutputAlgorithm< SimpleWeightedGraph< Spot, DefaultWeightedEdge > >, MultiThreaded
{
	/**
	 * Sets the {@link Logger} instance that will receive messages from this
	 * {@link SpotTracker}.
	 *
	 * @param logger
	 *            the logger to echo messages to.
	 */
	public void setLogger( final Logger logger );
}
