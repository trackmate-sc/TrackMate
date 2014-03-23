package fiji.plugin.trackmate;

import java.util.Comparator;

/**
 * Utility class for feature holders
 * 
 * @author Christian Dietz (University of Konstanz)
 */
public class FeatureHolderUtils {

	/**
	 * Returns the difference of the feature value for this spot with the one of
	 * the specified spot. By construction, this operation is anti-symmetric (
	 * <code>A.diffTo(B) = - B.diffTo(A)</code>).
	 * <p>
	 * Will generate a {@link NullPointerException} if one of the spots does not
	 * store the named feature.
	 * 
	 * @param s
	 *            the spot to compare to.
	 * @param feature
	 *            the name of the feature to use for calculation.
	 */
	public static double diffTo(final FeatureHolder f1, final FeatureHolder f2,
			final String feature) {
		final double d1 = f1.getFeature(feature).doubleValue();
		final double d2 = f2.getFeature(feature).doubleValue();
		return d1 - d2;
	}

	/**
	 * Returns the absolute normalized difference of the feature value of this
	 * spot with the one of the given spot.
	 * <p>
	 * If <code>a</code> and <code>b</code> are the feature values, then the
	 * absolute normalized difference is defined as
	 * <code>Math.abs( a - b) / ( (a+b)/2 )</code>.
	 * <p>
	 * By construction, this operation is symmetric (
	 * <code>A.normalizeDiffTo(B) =
	 * B.normalizeDiffTo(A)</code>).
	 * <p>
	 * Will generate a {@link NullPointerException} if one of the spots does not
	 * store the named feature.
	 * 
	 * @param s
	 *            the spot to compare to.
	 * @param feature
	 *            the name of the feature to use for calculation.
	 */
	public static double normalizeDiffTo(final FeatureHolder f1,
			final FeatureHolder f2, final String feature) {
		final double a = f1.getFeature(feature).doubleValue();
		final double b = f2.getFeature(feature).doubleValue();
		if (a == -b)
			return 0d;
		else
			return Math.abs(a - b) / ((a + b) / 2);
	}

	public static <F extends FeatureHolder> Comparator<F> featureComparator(
			final String feature) {
		final Comparator<F> comparator = new Comparator<F>() {
			@Override
			public int compare(final F o1, final F o2) {
				final double diff = FeatureHolderUtils.diffTo(o2, o1, feature);
				if (diff == 0)
					return 0;
				else if (diff < 0)
					return 1;
				else
					return -1;
			}
		};
		return comparator;
	}

}
