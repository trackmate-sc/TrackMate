package fiji.plugin.trackmate;

import java.util.Map;

/**
 * Interface for object that can store numerical features, indexed by String
 * keys.
 * <p>
 * This interface is adapted for implementations that want to be doted with
 * possibly many scalar features (<i>e.g.</i> X, Y, Z, T position, intensity,
 * etc.), but not for feature vectors (<i>e.g.</i> the many X,Y coordinates of a
 * 2D contour).
 * 
 * @author Christian Dietz
 * @author Jean-Yves Tinevez
 */
public interface FeatureHolder
{

	/**
	 * Returns the global feature mapping of this feature holder.
	 *
	 * @return the feature mapping, as a {@link Map}.
	 */
	public Map< String, Double > getFeatures();

	/**
	 * Returns the numerical value for the specified feature, as a
	 * {@link Double} object.
	 * <p>
	 * Because we store and return objects, the value returned can be
	 * <code>null</code>. This is meant to signify that the value for the
	 * requested feature is missing. {@link Double#NaN} is used to return
	 * feature values that are not defined.
	 *
	 * @param feature
	 *            the feature key.
	 * @return the stored value, as a {@link Double}.
	 */
	public Double getFeature( final String feature );

	/**
	 * Stores a numerical feature in this object using the specified String as a
	 * key to a {@link Double} value.
	 * <p>
	 * {@link Double}s can store <code>int</code> values without loss of
	 * precision, and these are acceptable values here. {@link Double#NaN} is
	 * acceptable as well and is used to tag features that do not have a defined
	 * value for this object. The <code>null</code> pointer is used to tag
	 * missing values.
	 * <p>
	 * It is recommended to use the {@link Double#valueOf(double)} to generate a
	 * {@link Double} object from a <code>double</code> primitive, so as to
	 * benefit from caching frequently requested objects.
	 *
	 * @param feature
	 *            the feature key.
	 * @param value
	 *            the feature value, as a {@link Double} object.
	 */
	public void putFeature( final String feature, final Double value );
}
