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
package fiji.plugin.trackmate.visualization;

import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * Interface for functions that can generate track colors, <b>with a different
 * color for each vertex and edge</b>. This allow more detailed views, but is
 * most likely more costly in memory and computation time.
 * <p>
 * The spot coloring can seem to be redundant with individual spot coloring
 * defined elsewhere. However, it must be noted that this interface is intended
 * for <b>track coloring</b>, and is applied to spots in tracks only. Concrete
 * implementations of {@link TrackMateModelView} decide whether they abide to
 * individual spot coloring or spots within tracks coloring (this interface).
 *
 * @author Jean-Yves Tinevez
 */
public interface TrackColorGenerator extends FeatureColorGenerator< DefaultWeightedEdge >
{}
