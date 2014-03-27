package fiji.plugin.trackmate;

import javax.swing.ImageIcon;

import org.scijava.plugin.SciJavaPlugin;

import fiji.plugin.trackmate.action.TrackMateActionFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.tracking.spot.SpotTracker;
import fiji.plugin.trackmate.visualization.ViewFactory;

/**
 * Interface for TrackMate modules.
 * <p>
 * Modules are the way users can extend TrackMate. We rely on SciJava automatic
 * plugin discovery to facilitate extension. To have a module discovered
 * annotate its class with for instance
 *
 * <pre>
 * @Plugin( type = SpotAnalyzerFactory.class, priority = 1d, visible = false )
 * </pre>
 * 
 * This will have a {@link SpotAnalyzerFactory} module registered in TrackMate.
 * It will be given a priority of 1 (0 is the default), which means that it will
 * be run <b>after</b> the analyzers of a <b>lower</b> priority. This is used
 * when some modules depends on the results generated by other modules. The
 * <code>visible</code> tag determines whether the module is visible or not in
 * the GUI.
 * <p>
 * Currently there are 7 types of modules:
 * <ul>
 * <li> {@link SpotDetectorFactory}: generates detectors for the detection step
 * of TrackMate.
 * <li> {@link SpotAnalyzerFactory}: generates analyzers that grant spots with
 * scalar numerical features.
 * <li> {@link ViewFactory}: generates views that can display the detection and
 * tracking results.
 * <li> {@link SpotTracker} TODO make a factory for them.
 * <li> {@link TrackAnalyzer}: compute scalar numerical features for tracks.
 * <li> {@link EdgeAnalyzer}: compute scalar numerical features for edges
 * (individual links between spots).
 * <li> {@link TrackMateActionFactory}: generates actions that provide general
 * use actions for TrackMate from the GUI.
 * </ul>
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2014
 *
 */
public interface TrackMateModule extends SciJavaPlugin
{

	/**
	 * Returns a html string containing a descriptive information about this
	 * module.
	 *
	 * @return a html string.
	 */
	public String getInfoText();

	/**
	 * Returns the icon for this action. Can be <code>null</code>.
	 *
	 * @return the icon. Returns <code>null</code> to be safely ignored.
	 */
	public ImageIcon getIcon();

	/**
	 * Returns a unique identifier of this module.
	 *
	 * @return the action key, as a string.
	 */
	public String getKey();

	/**
	 * Returns the human-compliant name of this module.
	 *
	 * @return the name, as a String.
	 */
	public String getName();

}
