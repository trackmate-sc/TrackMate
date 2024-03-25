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
/**
 * Package for the classes that compute scalar features on links between spots
 * (edges), such as instantaneous velocities, etc....
 * <p>
 * All analyzers should implement
 * {@link fiji.plugin.trackmate.features.edges.EdgeAnalyzer}, which is limited
 * to the independent analysis of a single edge.
 * <p>
 * Registration of analyzers is done through SciJava plugins discovery
 * mechanism. Annotate your class with
 * <code>@Plugin( type = EdgeAnalyzer.class )</code> to have it used in
 * TrackMate.
 *
 * @author Jean-Yves Tinevez
 */
package fiji.plugin.trackmate.features.edges;
