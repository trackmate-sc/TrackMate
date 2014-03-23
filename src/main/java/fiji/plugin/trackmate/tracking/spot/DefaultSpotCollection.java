package fiji.plugin.trackmate.tracking.spot;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.imglib2.algorithm.MultiThreaded;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackmateConstants;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.tracking.DefaultTOCollection;

/**
 * A utility class that wrap the {@link SortedMap} we use to store the spots
 * contained in each frame with a few utility methods.
 * <p>
 * Internally we rely on ConcurrentSkipListMap to allow concurrent access
 * without clashes.
 * <p>
 * This class is {@link MultiThreaded}. There are a few processes that can
 * benefit from multithreaded computation ({@link #filter(Collection)},
 * {@link #filter(FeatureFilter)}
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> - Feb 2011 - 2013
 * 
 */
public class DefaultSpotCollection extends DefaultTOCollection<Spot> implements
		SpotCollection {

	public DefaultSpotCollection() {
		super();
	}

	/**
	 * Returns a new {@link SpotCollection}, made of only the spots marked as
	 * visible. All the spots will then be marked as not-visible.
	 * 
	 * @return a new spot collection, made of only the spots marked as visible.
	 */
	@Override
	public DefaultSpotCollection crop() {
		final DefaultSpotCollection ns = new DefaultSpotCollection();
		ns.setNumThreads(numThreads);

		final Collection<Integer> frames = content.keySet();
		final ExecutorService executors = Executors
				.newFixedThreadPool(numThreads);
		for (final Integer frame : frames) {

			final Runnable command = new Runnable() {
				@Override
				public void run() {
					final Set<Spot> fc = content.get(frame);
					final Set<Spot> nfc = new HashSet<Spot>(getNObjects(frame,
							true));

					for (final Spot object : fc) {
						if (object.isVisible()) {
							nfc.add(object);
							object.setVisible(false);
						}
					}
					ns.content.put(frame, nfc);
				}
			};
			executors.execute(command);
		}

		executors.shutdown();
		try {
			final boolean ok = executors.awaitTermination(TIME_OUT_DELAY,
					TIME_OUT_UNITS);
			if (!ok) {
				System.err.println("[SpotCollection.crop()] Timeout of "
						+ TIME_OUT_DELAY + " " + TIME_OUT_UNITS
						+ " reached while cropping.");
			}
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
		return ns;
	}

	@Override
	public String toString() {
		String str = super.toString();
		str += ": contains " + getNObjects(false) + " spots total in "
				+ keySet().size() + " different frames, over which "
				+ getNObjects(true) + " are visible:\n";
		for (final int key : content.keySet()) {
			str += "\tframe " + key + ": " + getNObjects(key, false)
					+ " spots total, " + getNObjects(key, true) + " visible.\n";
		}
		return str;
	}

	/**
	 * Filters out the content of this collection using the specified
	 * {@link FeatureFilter}. Spots that are filtered out are marked as
	 * invisible, and visible otherwise.
	 * 
	 * @param featurefilter
	 *            the filter to use.
	 */
	@Override
	public final void filter(final FeatureFilter featurefilter) {

		final Collection<Integer> frames = content.keySet();
		final ExecutorService executors = Executors
				.newFixedThreadPool(numThreads);

		for (final Integer frame : frames) {

			final Runnable command = new Runnable() {
				@Override
				public void run() {

					Double val, tval;

					final Set<Spot> objects = content.get(frame);
					tval = featurefilter.value;

					if (featurefilter.isAbove) {

						for (final Spot object : objects) {
							val = object.getFeature(featurefilter.feature);
							if (val.compareTo(tval) < 0) {
								object.setVisible(false);
							} else {
								object.setVisible(true);
							}
						}

					} else {

						for (final Spot object : objects) {
							val = object.getFeature(featurefilter.feature);
							if (val.compareTo(tval) > 0) {
								object.setVisible(false);
							} else {
								object.setVisible(true);
							}
						}
					}
				}
			};
			executors.execute(command);
		}

		executors.shutdown();
		try {
			final boolean ok = executors.awaitTermination(TIME_OUT_DELAY,
					TIME_OUT_UNITS);
			if (!ok) {
				System.err.println("[SpotCollection.filter()] Timeout of "
						+ TIME_OUT_DELAY + " " + TIME_OUT_UNITS
						+ " reached while filtering.");
			}
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Filters out the content of this collection using the specified
	 * {@link FeatureFilter} collection. Spots that are filtered out are marked
	 * as invisible, and visible otherwise. To be marked as visible, a spot must
	 * pass <b>all</b> of the specified filters (AND chaining).
	 * 
	 * @param filters
	 *            the filter collection to use.
	 */
	@Override
	public final void filter(final Collection<FeatureFilter> filters) {

		final Collection<Integer> frames = content.keySet();
		final ExecutorService executors = Executors
				.newFixedThreadPool(numThreads);

		for (final Integer frame : frames) {
			final Runnable command = new Runnable() {
				@Override
				public void run() {
					final Set<Spot> objects = content.get(frame);

					Double val, tval;
					boolean isAbove, shouldNotBeVisible;
					for (final Spot object : objects) {

						shouldNotBeVisible = false;
						for (final FeatureFilter featureFilter : filters) {

							val = object.getFeature(featureFilter.feature);
							tval = featureFilter.value;
							isAbove = featureFilter.isAbove;

							if (isAbove && val.compareTo(tval) < 0 || !isAbove
									&& val.compareTo(tval) > 0) {
								shouldNotBeVisible = true;
								break;
							}
						} // loop over filters

						if (shouldNotBeVisible) {
							object.setVisible(false);
						} else {
							object.setVisible(true);
						}
					} // loop over spots

				}

			};
			executors.execute(command);
		}

		executors.shutdown();
		try {
			final boolean ok = executors.awaitTermination(TIME_OUT_DELAY,
					TIME_OUT_UNITS);
			if (!ok) {
				System.err.println("[SpotCollection.filter()] Timeout of "
						+ TIME_OUT_DELAY + " " + TIME_OUT_UNITS
						+ " reached while filtering.");
			}
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Builds and returns a new map of feature values for this spot collection.
	 * Each feature maps a double array, with 1 element per {@link Spot}, all
	 * pooled together.
	 * 
	 * @param features
	 *            the features to collect
	 * @param visibleOnly
	 *            if <code>true</code>, only the visible spot values will be
	 *            collected.
	 * @return a new map instance.
	 */
	@Override
	public Map<String, double[]> collectValues(
			final Collection<String> features, final boolean visibleOnly) {
		final Map<String, double[]> featureValues = new ConcurrentHashMap<String, double[]>(
				features.size());
		final ExecutorService executors = Executors
				.newFixedThreadPool(numThreads);

		for (final String feature : features) {
			final Runnable command = new Runnable() {
				@Override
				public void run() {
					final double[] values = collectValues(feature, visibleOnly);
					featureValues.put(feature, values);
				}

			};
			executors.execute(command);
		}

		executors.shutdown();
		try {
			final boolean ok = executors.awaitTermination(TIME_OUT_DELAY,
					TIME_OUT_UNITS);
			if (!ok) {
				System.err
						.println("[SpotCollection.collectValues()] Timeout of "
								+ TIME_OUT_DELAY + " " + TIME_OUT_UNITS
								+ " reached while filtering.");
			}
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

		return featureValues;
	}

	/**
	 * Returns the feature values of this Spot collection as a new double array.
	 * <p>
	 * If some spots do not have the interrogated feature set (stored value is
	 * <code>null</code>) or if the value is {@link Double#NaN}, they are
	 * skipped. The returned array might be therefore of smaller size than the
	 * number of spots interrogated.
	 * 
	 * @param feature
	 *            the feature to collect.
	 * @param visibleOnly
	 *            if <code>true</code>, only the visible spot values will be
	 *            collected.
	 * @return a new <code>double</code> array.
	 */
	@Override
	public final double[] collectValues(final String feature,
			final boolean visibleOnly) {
		final double[] values = new double[getNObjects(visibleOnly)];
		int index = 0;
		for (final Spot object : iterable(visibleOnly)) {
			final Double feat = object.getFeature(feature);
			if (null == feat) {
				continue;
			}
			final double val = feat.doubleValue();
			if (Double.isNaN(val)) {
				continue;
			}
			values[index] = val;
			index++;
		}
		return values;
	}

	public static SpotCollection fromCollection(final Iterable<Spot> spots) {
		final DefaultSpotCollection sc = new DefaultSpotCollection();
		for (final Spot spot : spots) {
			final int frame = spot.getFeature(TrackmateConstants.FRAME)
					.intValue();
			Set<Spot> fc = sc.content.get(frame);
			if (null == fc) {
				fc = new HashSet<Spot>();
				sc.content.put(frame, fc);
			}
			fc.add(spot);
		}
		return sc;
	}

	public static SpotCollection fromMap(final Map<Integer, Set<Spot>> source) {
		final DefaultSpotCollection sc = new DefaultSpotCollection();
		sc.content = new ConcurrentSkipListMap<Integer, Set<Spot>>(source);
		return sc;
	}
}
