package fiji.plugin.trackmate.visualization;

import java.awt.Color;

/**
 * Interface for color generator that can color objects based on a
 * feature identified by a String.
 * @author Jean-Yves Tinevez - 2013
 *
 * @param <K> the type of object to color.
 */
public interface FeatureColorGenerator< K >
{

	/**
	 * Returns a color for the given object.
	 *
	 * @param obj
	 *            the object to color.
	 * @return a color for this object.
	 */
	public Color color( K obj );
}
