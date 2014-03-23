package fiji.plugin.trackmate.tracking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.imglib2.algorithm.MultiThreaded;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.features.FeatureFilter;

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
public class DefaultTOCollection<T extends TrackableObject>
		implements MultiThreaded, TrackableObjectCollection<T> {

	public static final Double ZERO = Double.valueOf(0d);

	public static final Double ONE = Double.valueOf(1d);

	/**
	 * Time units for filtering and cropping operation timeouts. Filtering
	 * should not take more than 1 minute.
	 */
	protected static final TimeUnit TIME_OUT_UNITS = TimeUnit.MINUTES;

	/**
	 * Time for filtering and cropping operation timeouts. Filtering should not
	 * take more than 1 minute.
	 */
	protected static final long TIME_OUT_DELAY = 1;

	/** The frame by frame list of spot this object wrap. */
	protected ConcurrentSkipListMap<Integer, Set<T>> content = new ConcurrentSkipListMap<Integer, Set<T>>();

	protected int numThreads;

	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Construct a new empty spot collection.
	 */
	public DefaultTOCollection() {
		setNumThreads();
	}

	/*
	 * METHODS
	 */

	/**
	 * Retrieves and returns the {@link Spot} object in this collection with the
	 * specified ID. Returns <code>null</code> if the spot cannot be found. All
	 * spots, visible or not, are searched for.
	 * 
	 * @param ID
	 *            the ID to look for.
	 * @return the spot with the specified ID or <code>null</code> if this spot
	 *         does not exist or does not belong to this collection.
	 */
	@Override
	public T search(final int ID) {
		T obj = null;
		for (final T s : iterable(false)) {
			if (s.ID() == ID) {
				obj = s;
				break;
			}
		}
		return obj;
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
	 * Adds the given spot to this collection, at the specified frame, and mark
	 * it as visible.
	 * <p>
	 * If the frame does not exist yet in the collection, it is created and
	 * added. Upon adding, the added spot has its feature {@link Spot#FRAME}
	 * updated with the passed frame value.
	 */
	@Override
	public void add(final T object, final Integer frame) {
		Set<T> objects = content.get(frame);
		if (null == objects) {
			objects = new HashSet<T>();
			content.put(frame, objects);
		}
		objects.add(object);
		object.setFrame(frame);
		object.setVisible(true);
	}

	/**
	 * Removes the given spot from this collection, at the specified frame.
	 * <p>
	 * If the spot frame collection does not exist yet, nothing is done and
	 * <code>false</code> is returned. If the spot cannot be found in the frame
	 * content, nothing is done and <code>false</code> is returned.
	 */
	@Override
	public boolean remove(final T object, final Integer frame) {
		final Set<T> objects = content.get(frame);
		if (null == objects) {
			return false;
		}
		return objects.remove(object);
	}

	/**
	 * Marks all the content of this collection as visible or invisible,
	 * 
	 * @param visible
	 *            if true, all spots will be marked as visible.
	 */
	@Override
	public void setVisible(final boolean visible) {
		final Collection<Integer> frames = content.keySet();

		final ExecutorService executors = Executors
				.newFixedThreadPool(numThreads);
		for (final Integer frame : frames) {

			final Runnable command = new Runnable() {
				@Override
				public void run() {

					final Set<T> objects = content.get(frame);
					for (final T object : objects) {
						object.setVisible(visible);
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
				System.err.println("[SpotCollection.setVisible()] Timeout of "
						+ TIME_OUT_DELAY + " " + TIME_OUT_UNITS + " reached.");
			}
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the closest {@link Spot} to the given location (encoded as a
	 * Spot), contained in the frame <code>frame</code>. If the frame has no
	 * spot, return <code>null</code>.
	 * 
	 * @param location
	 *            the location to search for.
	 * @param frame
	 *            the frame to inspect.
	 * @param visibleSpotsOnly
	 *            if true, will only search though visible spots. If false, will
	 *            search through all spots.
	 * @return the closest spot to the specified location, member of this
	 *         collection.
	 */
	@Override
	public final T getClosestObject(final T location, final int frame,
			final boolean visibleObjectsOnly) {
		final Set<T> objects = content.get(frame);
		if (null == objects)
			return null;
		double d2;
		double minDist = Double.POSITIVE_INFINITY;
		T target = null;
		for (final T s : objects) {

			if (visibleObjectsOnly && (!s.isVisible())) {
				continue;
			}

			d2 = TrackableObjectUtils.squareDistanceTo(s, location);
			if (d2 < minDist) {
				minDist = d2;
				target = s;
			}

		}
		return target;
	}

	/**
	 * Returns the {@link Spot} at the given location (encoded as a Spot),
	 * contained in the frame <code>frame</code>. A spot is returned <b>only</b>
	 * if there exists a spot such that the given location is within the spot
	 * radius. Otherwise <code>null</code> is returned.
	 * 
	 * @param location
	 *            the location to search for.
	 * @param frame
	 *            the frame to inspect.
	 * @param visibleSpotsOnly
	 *            if true, will only search though visible spots. If false, will
	 *            search through all spots.
	 * @return the closest spot such that the specified location is within its
	 *         radius, member of this collection, or <code>null</code> is such a
	 *         spots cannot be found.
	 */
	@Override
	public final T getObjectAt(final T location, final int frame,
			final boolean visibleObjectsOnly) {
		final Set<T> objects = content.get(frame);
		if (null == objects || objects.isEmpty()) {
			return null;
		}

		final TreeMap<Double, T> distanceToObject = new TreeMap<Double, T>();
		double d2;
		for (final T s : objects) {

			if (visibleObjectsOnly && (!s.isVisible())) {
				continue;
			}

			d2 = TrackableObjectUtils.squareDistanceTo(s, location);
			if (d2 < s.radius() * s.radius()) {
				distanceToObject.put(d2, s);
			}
		}
		if (distanceToObject.isEmpty()) {
			return null;
		} else {
			return distanceToObject.firstEntry().getValue();
		}
	}

	/**
	 * Returns the <code>n</code> closest {@link Spot} to the given location
	 * (encoded as a Spot), contained in the frame <code>frame</code>. If the
	 * number of spots in the frame is exhausted, a shorter list is returned.
	 * <p>
	 * The list is ordered by increasing distance to the given location.
	 * 
	 * @param location
	 *            the location to search for.
	 * @param frame
	 *            the frame to inspect.
	 * @param n
	 *            the number of spots to search for.
	 * @param visibleSpotsOnly
	 *            if true, will only search though visible spots. If false, will
	 *            search through all spots.
	 * @return a new list, with of at most <code>n</code> spots, ordered by
	 *         increasing distance from the specified location.
	 */
	@Override
	public final List<T> getNClosestObjects(final T location, final int frame,
			int n, final boolean visibleObjectsOnly) {
		final Set<T> objects = content.get(frame);
		final TreeMap<Double, T> distanceToObject = new TreeMap<Double, T>();

		double d2;
		for (final T s : objects) {

			if (visibleObjectsOnly && (!s.isVisible())) {
				continue;
			}

			d2 = TrackableObjectUtils.squareDistanceTo(s, location);
			distanceToObject.put(d2, s);
		}

		final List<T> selectedSpots = new ArrayList<T>(n);
		final Iterator<Double> it = distanceToObject.keySet().iterator();
		while (n > 0 && it.hasNext()) {
			selectedSpots.add(distanceToObject.get(it.next()));
			n--;
		}
		return selectedSpots;
	}

	/**
	 * Returns the total number of spots in this collection, over all frames.
	 * 
	 * @param visibleSpotsOnly
	 *            if true, will only count visible spots. If false count all
	 *            spots.
	 * @return the total number of spots in this collection.
	 */
	@Override
	public final int getNObjects(final boolean visibleObjectsOnly) {
		int nobjects = 0;
		if (visibleObjectsOnly) {

			final Iterator<T> it = iterator(true);
			while (it.hasNext()) {
				it.next();
				nobjects++;
			}

		} else {

			for (final Set<T> objects : content.values())
				nobjects += objects.size();
		}
		return nobjects;
	}

	/**
	 * Returns the number of spots at the given frame.
	 * 
	 * @param visibleSpotsOnly
	 *            if true, will only count visible spots. If false count all
	 *            spots.
	 * @return the number of spots at the given frame.
	 */
	@Override
	public int getNObjects(final int frame, final boolean visibleObjectsOnly) {
		if (visibleObjectsOnly) {
			final Iterator<T> it = iterator(frame, true);
			int nObjects = 0;
			while (it.hasNext()) {
				it.next();
				nObjects++;
			}
			return nObjects;

		} else {

			final Set<T> objects = content.get(frame);
			if (null == objects)
				return 0;
			else
				return objects.size();
		}
	}

	/*
	 * FEATURES
	 */

	/*
	 * ITERABLE & co
	 */

	/**
	 * Return an iterator that iterates over all the spots contained in this
	 * collection.
	 * 
	 * @param visibleSpotsOnly
	 *            if true, the returned iterator will only iterate through
	 *            visible spots. If false, it will iterate over all spots.
	 * @return an iterator that iterates over this collection.
	 */
	@Override
	public Iterator<T> iterator(final boolean visibleObjectsOnly) {
		if (visibleObjectsOnly) {
			return new VisibleObjectsIterator();
		} else {
			return new AllObjectsIterator();
		}
	}

	/**
	 * Return an iterator that iterates over the spots in the specified frame.
	 * 
	 * @param visibleSpotsOnly
	 *            if true, the returned iterator will only iterate through
	 *            visible spots. If false, it will iterate over all spots.
	 * @param frame
	 *            the frame to iterate over.
	 * @return an iterator that iterates over the content of a frame of this
	 *         collection.
	 */
	@Override
	public Iterator<T> iterator(final Integer frame,
			final boolean visibleObjectsOnly) {
		final Set<T> frameContent = content.get(frame);
		if (null == frameContent) {
			return EMPTY_ITERATOR;
		}
		if (visibleObjectsOnly) {
			return new VisibleObjectsFrameIterator(frameContent);
		} else {
			return frameContent.iterator();
		}
	}

	/**
	 * A convenience methods that returns an {@link Iterable} wrapper for this
	 * collection as a whole.
	 * 
	 * @param visibleSpotsOnly
	 *            if true, the iterable will contains only visible spots.
	 *            Otherwise, it will contain all the spots.
	 * @return an iterable view of this spot collection.
	 */
	@Override
	public Iterable<T> iterable(final boolean visibleSpotsOnly) {
		return new WholeCollectionIterable(visibleSpotsOnly);
	}

	/**
	 * A convenience methods that returns an {@link Iterable} wrapper for a
	 * specific frame of this spot collection. The iterable is backed-up by the
	 * actual collection content, so modifying it can have unexpected results.
	 * 
	 * @param visibleSpotsOnly
	 *            if true, the iterable will contains only visible spots of the
	 *            specified frame. Otherwise, it will contain all the spots of
	 *            the specified frame.
	 * @param frame
	 *            the frame of the content the returned iterable will wrap.
	 * @return an iterable view of the content of a single frame of this spot
	 *         collection.
	 */
	@Override
	public Iterable<T> iterable(final int frame,
			final boolean visibleObjectsOnly) {
		if (visibleObjectsOnly) {
			return new FrameVisibleIterable(frame);
		} else {
			return content.get(frame);
		}
	}

	/*
	 * SORTEDMAP
	 */

	/**
	 * Stores the specified spots as the content of the specified frame. The
	 * added spots are all marked as not visible. Their {@link Spot#FRAME} is
	 * updated to be the specified frame.
	 * 
	 * @param frame
	 *            the frame to store these spots at. The specified spots replace
	 *            the previous content of this frame, if any.
	 * @param spots
	 *            the spots to store.
	 */
	@Override
	public void put(final int frame, final Collection<T> objects) {
		final Set<T> value = new HashSet<T>(objects);
		for (final T object : value) {
			object.setFrame(frame);
			object.setVisible(false);
		}
		content.put(frame, value);
	}

	/**
	 * Returns the first (lowest) frame currently in this collection.
	 * 
	 * @return the first (lowest) frame currently in this collection.
	 */
	@Override
	public Integer firstKey() {
		if (content.isEmpty()) {
			return 0;
		}
		return content.firstKey();
	}

	/**
	 * Returns the last (highest) frame currently in this collection.
	 * 
	 * @return the last (highest) frame currently in this collection.
	 */
	@Override
	public Integer lastKey() {
		if (content.isEmpty()) {
			return 0;
		}
		return content.lastKey();
	}

	/**
	 * Returns a NavigableSet view of the frames contained in this collection.
	 * The set's iterator returns the keys in ascending order. The set is backed
	 * by the map, so changes to the map are reflected in the set, and
	 * vice-versa. The set supports element removal, which removes the
	 * corresponding mapping from the map, via the Iterator.remove, Set.remove,
	 * removeAll, retainAll, and clear operations. It does not support the add
	 * or addAll operations.
	 * <p>
	 * The view's iterator is a "weakly consistent" iterator that will never
	 * throw ConcurrentModificationException, and guarantees to traverse
	 * elements as they existed upon construction of the iterator, and may (but
	 * is not guaranteed to) reflect any modifications subsequent to
	 * construction.
	 * 
	 * @return a navigable set view of the frames in this collection.
	 */
	@Override
	public NavigableSet<Integer> keySet() {
		return content.keySet();
	}

	/**
	 * Removes all the content from this collection.
	 */
	@Override
	public void clear() {
		content.clear();
	}

	/*
	 * MULTITHREADING
	 */

	@Override
	public void setNumThreads() {
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void setNumThreads(final int numThreads) {
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads() {
		return numThreads;
	}

	/*
	 * PRIVATE CLASSES
	 */

	private class AllObjectsIterator implements Iterator<T> {

		private boolean hasNext = true;

		private final Iterator<Integer> frameIterator;

		private Iterator<T> contentIterator;

		private T next = null;

		public AllObjectsIterator() {
			this.frameIterator = content.keySet().iterator();
			if (!frameIterator.hasNext()) {
				hasNext = false;
				return;
			}
			final Set<T> currentFrameContent = content
					.get(frameIterator.next());
			contentIterator = currentFrameContent.iterator();
			iterate();
		}

		private void iterate() {
			while (true) {

				// Is there still spots in current content?
				if (!contentIterator.hasNext()) {
					// No. Then move to next frame.
					// Is there still frames to iterate over?
					if (!frameIterator.hasNext()) {
						// No. Then we are done
						hasNext = false;
						next = null;
						return;
					} else {
						contentIterator = content.get(frameIterator.next())
								.iterator();
						continue;
					}
				}
				next = contentIterator.next();
				return;
			}
		}

		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public T next() {
			final T toReturn = next;
			iterate();
			return toReturn;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException(
					"Remove operation is not supported for SpotCollection iterators.");
		}

	}

	private class VisibleObjectsIterator implements Iterator<T> {

		private boolean hasNext = true;

		private final Iterator<Integer> frameIterator;

		private Iterator<T> contentIterator;

		private T next = null;

		private Set<T> currentFrameContent;

		public VisibleObjectsIterator() {
			this.frameIterator = content.keySet().iterator();
			if (!frameIterator.hasNext()) {
				hasNext = false;
				return;
			}
			currentFrameContent = content.get(frameIterator.next());
			contentIterator = currentFrameContent.iterator();
			iterate();
		}

		private void iterate() {

			while (true) {
				// Is there still spots in current content?
				if (!contentIterator.hasNext()) {
					// No. Then move to next frame.
					// Is there still frames to iterate over?
					if (!frameIterator.hasNext()) {
						// No. Then we are done
						hasNext = false;
						next = null;
						return;
					} else {
						// Yes. Then start iterating over the next frame.
						currentFrameContent = content.get(frameIterator.next());
						contentIterator = currentFrameContent.iterator();
						continue;
					}
				}
				next = contentIterator.next();
				// Is it visible?
				if (next.isVisible()) {
					// Yes! Be happy and return
					return;
				}
			}
		}

		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public T next() {
			final T toReturn = next;
			iterate();
			return toReturn;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException(
					"Remove operation is not supported for Collection iterators.");
		}

	}

	private class VisibleObjectsFrameIterator implements Iterator<T> {

		private boolean hasNext = true;

		private T next = null;

		private final Iterator<T> contentIterator;

		public VisibleObjectsFrameIterator(final Set<T> frameContent) {
			if (null == frameContent) {
				this.contentIterator = EMPTY_ITERATOR;
			} else {
				this.contentIterator = frameContent.iterator();
			}
			iterate();
		}

		private void iterate() {
			while (true) {
				if (!contentIterator.hasNext()) {
					// No. Then we are done
					hasNext = false;
					next = null;
					return;
				}
				next = contentIterator.next();
				// Is it visible?
				if (next.isVisible()) {
					// Yes. Be happy, and return.
					return;
				}
			}
		}

		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public T next() {
			final T toReturn = next;
			iterate();
			return toReturn;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException(
					"Remove operation is not supported for SpotCollection iterators.");
		}

	}

	/**
	 * Returns a new {@link SpotCollection}, made of only the spots marked as
	 * visible. All the spots will then be marked as not-visible.
	 * 
	 * @return a new spot collection, made of only the spots marked as visible.
	 */
	@Override
	public DefaultTOCollection<T> crop() {
		final DefaultTOCollection<T> ns = new DefaultTOCollection<T>();
		ns.setNumThreads(numThreads);

		final Collection<Integer> frames = content.keySet();
		final ExecutorService executors = Executors
				.newFixedThreadPool(numThreads);
		for (final Integer frame : frames) {

			final Runnable command = new Runnable() {
				@Override
				public void run() {
					final Set<T> fc = content.get(frame);
					final Set<T> nfc = new HashSet<T>(getNObjects(frame, true));

					for (final T object : fc) {
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

	/**
	 * A convenience wrapper that implements {@link Iterable} for this spot
	 * collection.
	 */
	private final class WholeCollectionIterable implements Iterable<T> {

		private final boolean visibleObjectsOnly;

		public WholeCollectionIterable(final boolean visibleObjectsOnly) {
			this.visibleObjectsOnly = visibleObjectsOnly;
		}

		@Override
		public Iterator<T> iterator() {
			if (visibleObjectsOnly) {
				return new VisibleObjectsIterator();
			} else {
				return new AllObjectsIterator();
			}
		}
	}

	/**
	 * A convenience wrapper that implements {@link Iterable} for this spot
	 * collection.
	 */
	private final class FrameVisibleIterable implements Iterable<T> {

		private final int frame;

		public FrameVisibleIterable(final int frame) {
			this.frame = frame;
		}

		@Override
		public Iterator<T> iterator() {
			return new VisibleObjectsFrameIterator(content.get(frame));
		}
	}

	private final Iterator<T> EMPTY_ITERATOR = new Iterator<T>() {

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public T next() {
			return null;
		}

		@Override
		public void remove() {
		}
	};

	/*
	 * STATIC METHODS
	 */

	/**
	 * Creates a new {@link SpotCollection} containing only the specified spots.
	 * Their frame origin is retrieved from their {@link Spot#FRAME} feature, so
	 * it must be set properly for all spots. All the spots of the new
	 * collection have the same visibility that the one they carry.
	 * 
	 * @param spots
	 *            the spot collection to build from.
	 * @return a new {@link SpotCollection} instance.
	 */
	public DefaultTOCollection(final Iterable<T> objects) {
		final DefaultTOCollection<T> sc = new DefaultTOCollection<T>();
		for (final T obj : objects) {
			final int frame = obj.frame();
			Set<T> fc = sc.content.get(frame);
			if (null == fc) {
				fc = new HashSet<T>();
				sc.content.put(frame, fc);
			}
			fc.add(obj);
		}
	}

	/**
	 * Creates a new {@link SpotCollection} from a copy of the specified map of
	 * sets. The spots added this way are completely untouched. In particular,
	 * their {@link #VISIBLITY} feature is left untouched, which makes this
	 * method suitable to de-serialize a {@link SpotCollection}.
	 * 
	 * @param source
	 *            the map to buidl the spot collection from.
	 * @return a new SpotCollection.
	 */
	public DefaultTOCollection(final Map<Integer, Set<T>> source) {
		final DefaultTOCollection<T> sc = new DefaultTOCollection<T>();
		sc.content = new ConcurrentSkipListMap<Integer, Set<T>>(source);
	}
}
