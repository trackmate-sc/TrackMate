package fiji.plugin.trackmate.tracking;

import java.util.Comparator;

import fiji.plugin.trackmate.util.AlphanumComparator;

public abstract class TrackableObjectUtils {

	public static int frameDiff(final TrackableObject t1,
			final TrackableObject t2) {
		return t1.frame() - t2.frame();
	}

	public static double squareDistanceTo(final TrackableObject t1,
			final TrackableObject t2) {
		double sumSquared = 0d;

		for (int d = 0; d < t1.numDimensions(); d++) {
			final double t1pos = t1.getDoublePosition(d);
			final double t2pos = t2.getDoublePosition(d);
			sumSquared += (t1pos - t2pos) * (t1pos - t2pos);
		}
		return sumSquared;
	}

	/**
	 * A comparator used to sort spots by name. The comparison uses numerical
	 * natural sorting, So that "Spot_4" comes before "Spot_122".
	 */
	public static <T extends TrackableObject> Comparator<T> nameComparator() {
		return new Comparator<T>() {
			private final AlphanumComparator comparator = AlphanumComparator.instance;

			@Override
			public int compare(final T o1, T o2) {
				return comparator.compare(o1.getName(), o2.getName());
			}
		};
	}

	public static <T extends TrackableObject> Comparator<T> frameComparator() {
		return new Comparator<T>() {

			@Override
			public int compare(final T o1, T o2) {
				return o1.frame() - o2.frame();
			}
		};
	}
}
