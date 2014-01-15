/**
 * Package for the classes that compute scalar features on links between
 * spots (edges), such as instantaneous velocities, etc....
 * <p>
 * All analyzers should implement {@link fiji.plugin.trackmate.features.edges.EdgeAnalyzer},
 * which is limited to the independent analysis of a single edge.
 * <p>
 * Registration of analyzers is done through SciJava plugins discovery mechanism. Annotate your
 * class with <code>@Plugin( type = EdgeAnalyzer.class )</code> to have it used in TrackMate.
 *
 * @author Jean-Yves Tinevez
 */
package fiji.plugin.trackmate.features.edges;

