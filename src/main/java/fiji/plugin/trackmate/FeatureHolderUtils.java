package fiji.plugin.trackmate;

import java.util.Comparator;

/**
 * Utility class for feature holders
 *
 * @author Christian Dietz (University of Konstanz)
 */
public class FeatureHolderUtils
{

	/**
	 * Returns the difference of the feature value for two {@link FeatureHolder}
	 * s. By construction, this operation is anti-symmetric (
	 * <code>diffTo(A, B) = - diffTo(B, A)</code>).
	 * <p>
	 * Will throw a {@link NullPointerException} if any of the
	 * {@link FeatureHolder}s does not store the named feature.
	 *
	 * @param f1
	 *            the first {@link FeatureHolder}.
	 * @param f2
	 *            the second {@link FeatureHolder}.
	 * @param feature
	 *            the feature key.
	 * @return the value of <code>f1 - f2</code> for the specified feature.
	 */
	public static double diffTo( final FeatureHolder f1, final FeatureHolder f2, final String feature )
	{
		final double d1 = f1.getFeature( feature ).doubleValue();
		final double d2 = f2.getFeature( feature ).doubleValue();
		return d1 - d2;
	}

	/**
	 * Returns the absolute normalized difference of the feature value of two
	 * {@link FeatureHolder}s.
	 * <p>
	 * If <code>a</code> and <code>b</code> are the feature values, then the
	 * absolute normalized difference is defined as
	 * <code>Math.abs( a - b) / ( (a+b)/2 )</code>.
	 * <p>
	 * By construction, this operation is symmetric (
	 * <code>normalizeDiffTo(A, B) =
	 * normalizeDiffTo(B, A)</code>).
	 * <p>
	 * Will generate a {@link NullPointerException} if any of the
	 * {@link FeatureHolder}s does not store the named feature.
	 *
	 * @param f1
	 *            the first {@link FeatureHolder}.
	 * @param f2
	 *            the second {@link FeatureHolder}.
	 * @param feature
	 *            the feature key.
	 * @return the value of <code>abs( f1 - f2) / ( (f1+f2)/2 )</code> for the
	 *         specified feature.
	 */
	public static double normalizeDiffTo( final FeatureHolder f1, final FeatureHolder f2, final String feature )
	{
		final double a = f1.getFeature( feature ).doubleValue();
		final double b = f2.getFeature( feature ).doubleValue();
		if ( a == -b )
			return 0d; // FIXME It should be Inf or NaN or I don't know
		else
			return Math.abs( a - b ) / ( ( a + b ) / 2d );
	}

	/**
	 * Returns a new {@link Comparator} that compares {@link FeatureHolder}s
	 * based on the feature specified by the given key.
	 *
	 * @param feature
	 *            the feature to use for comparison.
	 * @return a new {@link Comparator}.
	 */
	public static < F extends FeatureHolder > Comparator< F > featureComparator( final String feature )
	{
		final Comparator< F > comparator = new Comparator< F >()
		{
			@Override
			public int compare( final F o1, final F o2 )
			{
				final double diff = FeatureHolderUtils.diffTo( o2, o1, feature );
				if ( diff == 0 )
					return 0;
				else if ( diff < 0 )
					return 1;
				else
					return -1;
			}
		};
		return comparator;
	}
}
