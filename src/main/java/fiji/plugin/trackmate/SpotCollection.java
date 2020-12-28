package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import fiji.plugin.trackmate.features.FeatureFilter;
import net.imglib2.algorithm.MultiThreaded;

/**
 * A utility class that wrap the {@link java.util.SortedMap} we use to store the
 * spots contained in each frame with a few utility methods.
 * <p>
 * Internally we rely on ConcurrentSkipListMap to allow concurrent access
 * without clashes.
 * <p>
 * This class is {@link MultiThreaded}. There are a few processes that can
 * benefit from multithreaded computation ({@link #filter(Collection)},
 * {@link #filter(FeatureFilter)}
 *
 * @author Jean-Yves Tinevez - Feb 2011 -2013. Revised December 2020.
 */
public class SpotCollection implements MultiThreaded
{

	public static final Double ZERO = Double.valueOf( 0d );

	public static final Double ONE = Double.valueOf( 1d );

	public static final String VISIBILITY = "VISIBILITY";

	/**
	 * Time units for filtering and cropping operation timeouts. Filtering
	 * should not take more than 1 minute.
	 */
	private static final TimeUnit TIME_OUT_UNITS = TimeUnit.MINUTES;

	/**
	 * Time for filtering and cropping operation timeouts. Filtering should not
	 * take more than 1 minute.
	 */
	private static final long TIME_OUT_DELAY = 1;

	/** The frame by frame list of spot this object wrap. */
	private ConcurrentSkipListMap< Integer, Set< Spot > > content = new ConcurrentSkipListMap<>();

	private int numThreads;

	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Construct a new empty spot collection.
	 */
	public SpotCollection()
	{
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
	public Spot search( final int ID )
	{
		/*
		 * Having a map id -> spot would be better, but we don't have a big need
		 * for this.
		 */
		for ( final Spot spot : iterable( false ) )
			if ( spot.ID() == ID )
				return spot;

		return null;
	}

	@Override
	public String toString()
	{
		String str = super.toString();
		str += ": contains " + getNSpots( false ) + " spots total in "
				+ keySet().size() + " different frames, over which "
				+ getNSpots( true ) + " are visible:\n";
		for ( final int key : content.keySet() )
			str += "\tframe " + key + ": "
					+ getNSpots( key, false ) + " spots total, "
					+ getNSpots( key, true ) + " visible.\n";

		return str;
	}

	/**
	 * Adds the given spot to this collection, at the specified frame, and mark
	 * it as visible.
	 * <p>
	 * If the frame does not exist yet in the collection, it is created and
	 * added. Upon adding, the added spot has its feature {@link Spot#FRAME}
	 * updated with the passed frame value.
	 * 
	 * @param spot
	 *            the spot to add.
	 * @param frame
	 *            the frame to add it to.
	 */
	public void add( final Spot spot, final Integer frame )
	{
		Set< Spot > spots = content.get( frame );
		if ( null == spots )
		{
			spots = new HashSet<>();
			content.put( frame, spots );
		}
		spots.add( spot );
		spot.putFeature( Spot.FRAME, Double.valueOf( frame ) );
		spot.putFeature( VISIBILITY, ONE );
	}

	/**
	 * Removes the given spot from this collection, at the specified frame.
	 * <p>
	 * If the spot frame collection does not exist yet, nothing is done and
	 * <code>false</code> is returned. If the spot cannot be found in the frame
	 * content, nothing is done and <code>false</code> is returned.
	 * 
	 * @param spot
	 *            the spot to remove.
	 * @param frame
	 *            the frame to remove it from.
	 * @return <code>true</code> if the spot was succesfully removed.
	 */
	public boolean remove( final Spot spot, final Integer frame )
	{
		final Set< Spot > spots = content.get( frame );
		if ( null == spots )
			return false;
		return spots.remove( spot );
	}

	/**
	 * Marks all the content of this collection as visible or invisible.
	 *
	 * @param visible
	 *            if true, all spots will be marked as visible.
	 */
	public void setVisible( final boolean visible )
	{
		final Double val = visible ? ONE : ZERO;
		final Collection< Integer > frames = content.keySet();

		final ExecutorService executors = Executors.newFixedThreadPool( numThreads );
		for ( final Integer frame : frames )
		{

			final Runnable command = new Runnable()
			{
				@Override
				public void run()
				{

					final Set< Spot > spots = content.get( frame );
					for ( final Spot spot : spots )
						spot.putFeature( VISIBILITY, val );
				}
			};
			executors.execute( command );
		}

		executors.shutdown();
		try
		{
			final boolean ok = executors.awaitTermination( TIME_OUT_DELAY, TIME_OUT_UNITS );
			if ( !ok )
				System.err.println( "[SpotCollection.setVisible()] Timeout of " + TIME_OUT_DELAY + " " + TIME_OUT_UNITS + " reached." );
		}
		catch ( final InterruptedException e )
		{
			e.printStackTrace();
		}
	}

	/**
	 * Filters out the content of this collection using the specified
	 * {@link FeatureFilter}. Spots that are filtered out are marked as
	 * invisible, and visible otherwise.
	 *
	 * @param featurefilter
	 *            the filter to use.
	 */
	public final void filter( final FeatureFilter featurefilter )
	{

		final Collection< Integer > frames = content.keySet();
		final ExecutorService executors = Executors.newFixedThreadPool( numThreads );

		for ( final Integer frame : frames )
		{

			final Runnable command = new Runnable()
			{
				@Override
				public void run()
				{
					final Set< Spot > spots = content.get( frame );
					final double tval = featurefilter.value;

					if ( featurefilter.isAbove )
					{
						for ( final Spot spot : spots )
						{
							final Double val = spot.getFeature( featurefilter.feature );
							spot.putFeature( VISIBILITY, val.compareTo( tval ) < 0 ? ZERO : ONE );
						}

					}
					else
					{
						for ( final Spot spot : spots )
						{
							final Double val = spot.getFeature( featurefilter.feature );
							spot.putFeature( VISIBILITY, val.compareTo( tval ) > 0 ? ZERO : ONE );
						}
					}
				}
			};
			executors.execute( command );
		}

		executors.shutdown();
		try
		{
			final boolean ok = executors.awaitTermination( TIME_OUT_DELAY, TIME_OUT_UNITS );
			if ( !ok )
				System.err.println( "[SpotCollection.filter()] Timeout of " + TIME_OUT_DELAY + " " + TIME_OUT_UNITS + " reached while filtering." );
		}
		catch ( final InterruptedException e )
		{
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
	public final void filter( final Collection< FeatureFilter > filters )
	{

		final Collection< Integer > frames = content.keySet();
		final ExecutorService executors = Executors.newFixedThreadPool( numThreads );

		for ( final Integer frame : frames )
		{
			final Runnable command = new Runnable()
			{
				@Override
				public void run()
				{
					final Set< Spot > spots = content.get( frame );
					for ( final Spot spot : spots )
					{

						boolean shouldNotBeVisible = false;
						for ( final FeatureFilter featureFilter : filters )
						{

							final Double val = spot.getFeature( featureFilter.feature );
							final double tval = featureFilter.value;
							final boolean isAbove = featureFilter.isAbove;

							if ( null == val || isAbove && val.compareTo( tval ) < 0 || !isAbove && val.compareTo( tval ) > 0 )
							{
								shouldNotBeVisible = true;
								break;
							}
						} // loop over filters
						spot.putFeature( VISIBILITY, shouldNotBeVisible ? ZERO : ONE );

					} // loop over spots
				}
			};
			executors.execute( command );
		}

		executors.shutdown();
		try
		{
			final boolean ok = executors.awaitTermination( TIME_OUT_DELAY, TIME_OUT_UNITS );
			if ( !ok )
				System.err.println( "[SpotCollection.filter()] Timeout of " + TIME_OUT_DELAY + " " + TIME_OUT_UNITS + " reached while filtering." );
		}
		catch ( final InterruptedException e )
		{
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
	public final Spot getClosestSpot( final Spot location, final int frame, final boolean visibleSpotsOnly )
	{
		final Set< Spot > spots = content.get( frame );
		if ( null == spots )
			return null;
		double minDist = Double.POSITIVE_INFINITY;
		Spot target = null;
		for ( final Spot spot : spots )
		{

			if ( visibleSpotsOnly && !isVisible( spot ) )
				continue;

			final double d2 = spot.squareDistanceTo( location );
			if ( d2 < minDist )
			{
				minDist = d2;
				target = spot;
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
	public final Spot getSpotAt( final Spot location, final int frame, final boolean visibleSpotsOnly )
	{
		final Set< Spot > spots = content.get( frame );
		if ( null == spots || spots.isEmpty() )
			return null;

		double minDist2 = Double.POSITIVE_INFINITY;
		Spot bestSpot = null;
		for ( final Spot spot : spots )
		{
			if ( visibleSpotsOnly && !isVisible( spot ) )
				continue;

			final double d2 = spot.squareDistanceTo( location );
			final double radius = spot.getFeature( Spot.RADIUS );
			if ( d2 < Math.min( minDist2, radius * radius ) )
			{
				minDist2 = d2;
				bestSpot = spot;
			}
		}
		return bestSpot;
	}

	/**
	 * Returns the total number of spots in this collection, over all frames.
	 *
	 * @param visibleSpotsOnly
	 *            if true, will only count visible spots. If false count all
	 *            spots.
	 * @return the total number of spots in this collection.
	 */
	public final int getNSpots( final boolean visibleSpotsOnly )
	{
		int nspots = 0;
		if ( visibleSpotsOnly )
		{
			final Iterator< Spot > it = iterator( true );
			while ( it.hasNext() )
			{
				it.next();
				nspots++;
			}

		}
		else
		{
			for ( final Set< Spot > spots : content.values() )
				nspots += spots.size();
		}
		return nspots;
	}

	/**
	 * Returns the number of spots at the given frame.
	 *
	 * @param frame
	 *            the frame.
	 * @param visibleSpotsOnly
	 *            if true, will only count visible spots. If false count all
	 *            spots.
	 * @return the number of spots at the given frame.
	 */
	public int getNSpots( final int frame, final boolean visibleSpotsOnly )
	{
		if ( visibleSpotsOnly )
		{
			final Iterator< Spot > it = iterator( frame, true );
			int nspots = 0;
			while ( it.hasNext() )
			{
				it.next();
				nspots++;
			}
			return nspots;
		}

		final Set< Spot > spots = content.get( frame );
		if ( null == spots )
			return 0;

		return spots.size();
	}

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
	public Iterator< Spot > iterator( final boolean visibleSpotsOnly )
	{
		if ( visibleSpotsOnly )
			return new VisibleSpotsIterator();

		return new AllSpotsIterator();
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
	public Iterator< Spot > iterator( final Integer frame, final boolean visibleSpotsOnly )
	{
		final Set< Spot > frameContent = content.get( frame );
		if ( null == frameContent )
			return EMPTY_ITERATOR;

		if ( visibleSpotsOnly )
			return new VisibleSpotsFrameIterator( frameContent );

		return frameContent.iterator();
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
	public Iterable< Spot > iterable( final boolean visibleSpotsOnly )
	{
		return new WholeCollectionIterable( visibleSpotsOnly );
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
	public Iterable< Spot > iterable( final int frame, final boolean visibleSpotsOnly )
	{
		if ( visibleSpotsOnly )
			return new FrameVisibleIterable( frame );

		return content.get( frame );
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
	public void put( final int frame, final Collection< Spot > spots )
	{
		final Set< Spot > value = new HashSet<>( spots );
		for ( final Spot spot : value )
		{
			spot.putFeature( Spot.FRAME, Double.valueOf( frame ) );
			spot.putFeature( VISIBILITY, ZERO );
		}
		content.put( frame, value );
	}

	/**
	 * Returns the first (lowest) frame currently in this collection.
	 *
	 * @return the first (lowest) frame currently in this collection.
	 */
	public Integer firstKey()
	{
		if ( content.isEmpty() )
			return 0;
		return content.firstKey();
	}

	/**
	 * Returns the last (highest) frame currently in this collection.
	 *
	 * @return the last (highest) frame currently in this collection.
	 */
	public Integer lastKey()
	{
		if ( content.isEmpty() )
			return 0;
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
	public NavigableSet< Integer > keySet()
	{
		return content.keySet();
	}

	/**
	 * Removes all the content from this collection.
	 */
	public void clear()
	{
		content.clear();
	}

	/*
	 * MULTITHREADING
	 */

	@Override
	public void setNumThreads()
	{
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void setNumThreads( final int numThreads )
	{
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}

	/*
	 * PRIVATE CLASSES
	 */

	private class AllSpotsIterator implements Iterator< Spot >
	{

		private boolean hasNext = true;

		private final Iterator< Integer > frameIterator;

		private Iterator< Spot > contentIterator;

		private Spot next = null;

		public AllSpotsIterator()
		{
			this.frameIterator = content.keySet().iterator();
			if ( !frameIterator.hasNext() )
			{
				hasNext = false;
				return;
			}
			final Set< Spot > currentFrameContent = content.get( frameIterator.next() );
			contentIterator = currentFrameContent.iterator();
			iterate();
		}

		private void iterate()
		{
			while ( true )
			{

				// Is there still spots in current content?
				if ( !contentIterator.hasNext() )
				{
					// No. Then move to next frame.
					// Is there still frames to iterate over?
					if ( !frameIterator.hasNext() )
					{
						// No. Then we are done
						hasNext = false;
						next = null;
						return;
					}

					contentIterator = content.get( frameIterator.next() ).iterator();
					continue;
				}
				next = contentIterator.next();
				return;
			}
		}

		@Override
		public boolean hasNext()
		{
			return hasNext;
		}

		@Override
		public Spot next()
		{
			final Spot toReturn = next;
			iterate();
			return toReturn;
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException( "Remove operation is not supported for SpotCollection iterators." );
		}
	}

	private class VisibleSpotsIterator implements Iterator< Spot >
	{

		private boolean hasNext = true;

		private final Iterator< Integer > frameIterator;

		private Iterator< Spot > contentIterator;

		private Spot next = null;

		private Set< Spot > currentFrameContent;

		public VisibleSpotsIterator()
		{
			this.frameIterator = content.keySet().iterator();
			if ( !frameIterator.hasNext() )
			{
				hasNext = false;
				return;
			}
			currentFrameContent = content.get( frameIterator.next() );
			contentIterator = currentFrameContent.iterator();
			iterate();
		}

		private void iterate()
		{

			while ( true )
			{
				// Is there still spots in current content?
				if ( !contentIterator.hasNext() )
				{
					// No. Then move to next frame.
					// Is there still frames to iterate over?
					if ( !frameIterator.hasNext() )
					{
						// No. Then we are done
						hasNext = false;
						next = null;
						return;
					}

					// Yes. Then start iterating over the next frame.
					currentFrameContent = content.get( frameIterator.next() );
					contentIterator = currentFrameContent.iterator();
					continue;
				}
				next = contentIterator.next();
				// Is it visible?
				if ( next.getFeature( VISIBILITY ).compareTo( ZERO ) > 0 )
				{
					// Yes! Be happy and return
					return;
				}
			}
		}

		@Override
		public boolean hasNext()
		{
			return hasNext;
		}

		@Override
		public Spot next()
		{
			final Spot toReturn = next;
			iterate();
			return toReturn;
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException( "Remove operation is not supported for SpotCollection iterators." );
		}
	}

	private class VisibleSpotsFrameIterator implements Iterator< Spot >
	{

		private boolean hasNext = true;

		private Spot next = null;

		private final Iterator< Spot > contentIterator;

		public VisibleSpotsFrameIterator( final Set< Spot > frameContent )
		{
			this.contentIterator = ( null == frameContent ) ? EMPTY_ITERATOR : frameContent.iterator();
			iterate();
		}

		private void iterate()
		{
			while ( true )
			{
				if ( !contentIterator.hasNext() )
				{
					// No. Then we are done
					hasNext = false;
					next = null;
					return;
				}
				next = contentIterator.next();
				// Is it visible?
				if ( next.getFeature( VISIBILITY ).compareTo( ZERO ) > 0 )
				{
					// Yes. Be happy, and return.
					return;
				}
			}
		}

		@Override
		public boolean hasNext()
		{
			return hasNext;
		}

		@Override
		public Spot next()
		{
			final Spot toReturn = next;
			iterate();
			return toReturn;
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException( "Remove operation is not supported for SpotCollection iterators." );
		}
	}

	/**
	 * Remove all the non-visible spots of this collection.
	 */
	public void crop()
	{
		final Collection< Integer > frames = content.keySet();
		final ExecutorService executors = Executors.newFixedThreadPool( numThreads );
		for ( final Integer frame : frames )
		{
			final Runnable command = new Runnable()
			{
				@Override
				public void run()
				{
					final Set< Spot > fc = content.get( frame );
					final List< Spot > toRemove = new ArrayList<>();
					for ( final Spot spot : fc )
						if ( !isVisible( spot ) )
							toRemove.add( spot );

					fc.removeAll( toRemove );
				}
			};
			executors.execute( command );
		}

		executors.shutdown();
		try
		{
			final boolean ok = executors.awaitTermination( TIME_OUT_DELAY, TIME_OUT_UNITS );
			if ( !ok )
				System.err.println( "[SpotCollection.crop()] Timeout of " + TIME_OUT_DELAY + " " + TIME_OUT_UNITS + " reached while cropping." );
		}
		catch ( final InterruptedException e )
		{
			e.printStackTrace();
		}
	}

	/**
	 * A convenience wrapper that implements {@link Iterable} for this spot
	 * collection.
	 */
	private final class WholeCollectionIterable implements Iterable< Spot >
	{

		private final boolean visibleSpotsOnly;

		public WholeCollectionIterable( final boolean visibleSpotsOnly )
		{
			this.visibleSpotsOnly = visibleSpotsOnly;
		}

		@Override
		public Iterator< Spot > iterator()
		{
			if ( visibleSpotsOnly )
				return new VisibleSpotsIterator();

			return new AllSpotsIterator();
		}
	}

	/**
	 * A convenience wrapper that implements {@link Iterable} for this spot
	 * collection.
	 */
	private final class FrameVisibleIterable implements Iterable< Spot >
	{

		private final int frame;

		public FrameVisibleIterable( final int frame )
		{
			this.frame = frame;
		}

		@Override
		public Iterator< Spot > iterator()
		{
			return new VisibleSpotsFrameIterator( content.get( frame ) );
		}
	}

	private static final Iterator< Spot > EMPTY_ITERATOR = new Iterator< Spot >()
	{

		@Override
		public boolean hasNext()
		{
			return false;
		}

		@Override
		public Spot next()
		{
			return null;
		}

		@Override
		public void remove()
		{}
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
	public static SpotCollection fromCollection( final Iterable< Spot > spots )
	{
		final SpotCollection sc = new SpotCollection();
		for ( final Spot spot : spots )
		{
			final int frame = spot.getFeature( Spot.FRAME ).intValue();
			Set< Spot > fc = sc.content.get( frame );
			if ( null == fc )
			{
				fc = new HashSet<>();
				sc.content.put( frame, fc );
			}
			fc.add( spot );
		}
		return sc;
	}

	/**
	 * Creates a new {@link SpotCollection} from a copy of the specified map of
	 * sets. The spots added this way are completely untouched. In particular,
	 * their {@link #VISIBILITY} feature is left untouched, which makes this
	 * method suitable to de-serialize a {@link SpotCollection}.
	 *
	 * @param source
	 *            the map to buidl the spot collection from.
	 * @return a new SpotCollection.
	 */
	public static SpotCollection fromMap( final Map< Integer, Set< Spot > > source )
	{
		final SpotCollection sc = new SpotCollection();
		sc.content = new ConcurrentSkipListMap<>( source );
		return sc;
	}

	private static final boolean isVisible( final Spot spot )
	{
		return spot.getFeature( VISIBILITY ).compareTo( ZERO ) > 0;
	}
}
