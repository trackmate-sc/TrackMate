package fiji.plugin.trackmate.tracking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
import fiji.plugin.trackmate.tracking.spot.SpotCollection;


/**
 * A default implementation of {@link TrackableObjectCollection}.
 * <p>
 * This implementations wraps a {@link ConcurrentSkipListMap}, and is therefore
 * suited for multi-threading applications. It also extends
 * {@link MultiThreaded}, so that one can specify the number of threads to use
 * for some methods (namely {@link #crop()}, {@link #setVisible(boolean)}).
 *
 * @param <T>
 *            the class of the objects stored in this collection. Must implement
 *            {@link TrackableObject}.
 */
public class DefaultTOCollection< T extends TrackableObject > implements MultiThreaded, TrackableObjectCollection< T >
{

	/**
	 * Time units for filtering and visibility operation timeouts. They should
	 * not take more than 10 minute.
	 */
	protected static final TimeUnit TIME_OUT_UNITS = TimeUnit.MINUTES;

	/**
	 * Time for filtering and cropping operation timeouts. Filtering should not
	 * take more than 1 minute.
	 */
	protected static final long TIME_OUT_DELAY = 10;

	/** The frame by frame list of {@link TrackableObject}s this object wraps. */
	protected ConcurrentSkipListMap< Integer, Collection< T >> content = new ConcurrentSkipListMap< Integer, Collection< T >>();

	protected int numThreads;

	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Construct a new empty spot collection.
	 */
	public DefaultTOCollection()
	{
		setNumThreads();
	}

	/*
	 * METHODS
	 */

	@Override
	public T search( final int ID )
	{
		T obj = null;
		for ( final T s : iterable( false ) )
		{
			if ( s.ID() == ID )
			{
				obj = s;
				break;
			}
		}
		return obj;
	}

	@Override
	public String toString()
	{
		String str = super.toString();
		str += ": contains " + getNObjects( false ) + " spots total in " + keySet().size() + " different frames, over which " + getNObjects( true ) + " are visible:\n";
		for ( final int key : content.keySet() )
		{
			str += "\tframe " + key + ": " + getNObjects( key, false ) + " spots total, " + getNObjects( key, true ) + " visible.\n";
		}
		return str;
	}

	@Override
	public void add( final T object, final Integer frame )
	{
		Collection< T > objects = content.get( frame );
		if ( null == objects )
		{
			objects = new HashSet< T >();
			content.put( frame, objects );
		}
		objects.add( object );
		object.setFrame( frame );
		object.setVisible( true );
	}


	@Override
	public boolean remove( final T object, final Integer frame )
	{
		final Collection< T > objects = content.get( frame );
		if ( null == objects ) { return false; }
		return objects.remove( object );
	}

	@Override
	public void setVisible( final boolean visible )
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

					final Collection< T > objects = content.get( frame );
					for ( final T object : objects )
					{
						object.setVisible( visible );
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
			{
				System.err.println( "[SpotCollection.setVisible()] Timeout of " + TIME_OUT_DELAY + " " + TIME_OUT_UNITS + " reached." );
			}
		}
		catch ( final InterruptedException e )
		{
			e.printStackTrace();
		}
	}

	@Override
	public final T getClosestObject( final T location, final int frame, final boolean visibleObjectsOnly )
	{
		final Collection< T > objects = content.get( frame );
		if ( null == objects )
			return null;
		double d2;
		double minDist = Double.POSITIVE_INFINITY;
		T target = null;
		for ( final T s : objects )
		{

			if ( visibleObjectsOnly && ( !s.isVisible() ) )
			{
				continue;
			}

			d2 = TrackableObjectUtils.squareDistanceTo( s, location );
			if ( d2 < minDist )
			{
				minDist = d2;
				target = s;
			}

		}
		return target;
	}


	@Override
	public final T getObjectAt( final T location, final int frame, final boolean visibleObjectsOnly )
	{
		final Collection< T > objects = content.get( frame );
		if ( null == objects || objects.isEmpty() ) { return null; }

		final TreeMap< Double, T > distanceToObject = new TreeMap< Double, T >();
		double d2;
		for ( final T s : objects )
		{

			if ( visibleObjectsOnly && ( !s.isVisible() ) )
			{
				continue;
			}

			d2 = TrackableObjectUtils.squareDistanceTo( s, location );
			if ( d2 < s.radius() * s.radius() )
			{
				distanceToObject.put( d2, s );
			}
		}
		if ( distanceToObject.isEmpty() )
		{
			return null;
		}
		else
		{
			return distanceToObject.firstEntry().getValue();
		}
	}


	@Override
	public final List< T > getNClosestObjects( final T location, final int frame, int n, final boolean visibleObjectsOnly )
	{
		final Collection< T > objects = content.get( frame );
		final TreeMap< Double, T > distanceToObject = new TreeMap< Double, T >();

		double d2;
		for ( final T s : objects )
		{

			if ( visibleObjectsOnly && ( !s.isVisible() ) )
			{
				continue;
			}

			d2 = TrackableObjectUtils.squareDistanceTo( s, location );
			distanceToObject.put( d2, s );
		}

		final List< T > selectedSpots = new ArrayList< T >( n );
		final Iterator< Double > it = distanceToObject.keySet().iterator();
		while ( n > 0 && it.hasNext() )
		{
			selectedSpots.add( distanceToObject.get( it.next() ) );
			n--;
		}
		return selectedSpots;
	}

	@Override
	public final int getNObjects( final boolean visibleObjectsOnly )
	{
		int nobjects = 0;
		if ( visibleObjectsOnly )
		{

			final Iterator< T > it = iterator( true );
			while ( it.hasNext() )
			{
				it.next();
				nobjects++;
			}

		}
		else
		{

			for ( final Collection< T > objects : content.values() )
				nobjects += objects.size();
		}
		return nobjects;
	}


	@Override
	public int getNObjects( final int frame, final boolean visibleObjectsOnly )
	{
		if ( visibleObjectsOnly )
		{
			final Iterator< T > it = iterator( frame, true );
			int nObjects = 0;
			while ( it.hasNext() )
			{
				it.next();
				nObjects++;
			}
			return nObjects;

		}
		else
		{

			final Collection< T > objects = content.get( frame );
			if ( null == objects )
				return 0;
			else
				return objects.size();
		}
	}

	@Override
	public Iterator< T > iterator( final boolean visibleObjectsOnly )
	{
		if ( visibleObjectsOnly )
		{
			return new VisibleObjectsIterator();
		}
		else
		{
			return new AllObjectsIterator();
		}
	}


	@Override
	public Iterator< T > iterator( final Integer frame, final boolean visibleObjectsOnly )
	{
		final Collection< T > frameContent = content.get( frame );
		if ( null == frameContent ) { return EMPTY_ITERATOR; }
		if ( visibleObjectsOnly )
		{
			return new VisibleObjectsFrameIterator( frameContent );
		}
		else
		{
			return frameContent.iterator();
		}
	}


	@Override
	public Iterable< T > iterable( final boolean visibleSpotsOnly )
	{
		return new WholeCollectionIterable( visibleSpotsOnly );
	}


	@Override
	public Iterable< T > iterable( final int frame, final boolean visibleObjectsOnly )
	{
		if ( visibleObjectsOnly )
		{
			return new FrameVisibleIterable( frame );
		}
		else
		{
			return content.get( frame );
		}
	}

	/*
	 * SORTEDMAP
	 */


	@Override
	public Collection< T > put( final Integer frame, final Collection< T > objects )
	{
		final Set< T > value = new HashSet< T >( objects );
		for ( final T object : value )
		{
			object.setFrame( frame );
			object.setVisible( false );
		}
		return content.put( frame, value );
	}

	@Override
	public Integer firstKey()
	{
		if ( content.isEmpty() ) { return 0; }
		return content.firstKey();
	}

	@Override
	public Integer lastKey()
	{
		if ( content.isEmpty() ) { return 0; }
		return content.lastKey();
	}

	@Override
	public NavigableSet< Integer > keySet()
	{
		return content.keySet();
	}

	@Override
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

	private class AllObjectsIterator implements Iterator< T >
	{

		private boolean hasNext = true;

		private final Iterator< Integer > frameIterator;

		private Iterator< T > contentIterator;

		private T next = null;

		public AllObjectsIterator()
		{
			this.frameIterator = content.keySet().iterator();
			if ( !frameIterator.hasNext() )
			{
				hasNext = false;
				return;
			}
			final Collection< T > currentFrameContent = content.get( frameIterator.next() );
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
					else
					{
						contentIterator = content.get( frameIterator.next() ).iterator();
						continue;
					}
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
		public T next()
		{
			final T toReturn = next;
			iterate();
			return toReturn;
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException( "Remove operation is not supported for SpotCollection iterators." );
		}

	}

	private class VisibleObjectsIterator implements Iterator< T >
	{

		private boolean hasNext = true;

		private final Iterator< Integer > frameIterator;

		private Iterator< T > contentIterator;

		private T next = null;

		private Collection< T > currentFrameContent;

		public VisibleObjectsIterator()
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
					else
					{
						// Yes. Then start iterating over the next frame.
						currentFrameContent = content.get( frameIterator.next() );
						contentIterator = currentFrameContent.iterator();
						continue;
					}
				}
				next = contentIterator.next();
				// Is it visible?
				if ( next.isVisible() )
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
		public T next()
		{
			final T toReturn = next;
			iterate();
			return toReturn;
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException( "Remove operation is not supported for Collection iterators." );
		}

	}

	private class VisibleObjectsFrameIterator implements Iterator< T >
	{

		private boolean hasNext = true;

		private T next = null;

		private final Iterator< T > contentIterator;

		public VisibleObjectsFrameIterator( final Collection< T > frameContent )
		{
			if ( null == frameContent )
			{
				this.contentIterator = EMPTY_ITERATOR;
			}
			else
			{
				this.contentIterator = frameContent.iterator();
			}
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
				if ( next.isVisible() )
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
		public T next()
		{
			final T toReturn = next;
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
	 * Returns a new {@link SpotCollection}, made of only the spots marked as
	 * visible. All the spots will then be marked as not-visible.
	 *
	 * @return a new spot collection, made of only the spots marked as visible.
	 */
	@Override
	public DefaultTOCollection< T > crop()
	{
		final DefaultTOCollection< T > ns = new DefaultTOCollection< T >();
		ns.setNumThreads( numThreads );

		final Collection< Integer > frames = content.keySet();
		final ExecutorService executors = Executors.newFixedThreadPool( numThreads );
		for ( final Integer frame : frames )
		{

			final Runnable command = new Runnable()
			{
				@Override
				public void run()
				{
					final Collection< T > fc = content.get( frame );
					final Set< T > nfc = new HashSet< T >( getNObjects( frame, true ) );

					for ( final T object : fc )
					{
						if ( object.isVisible() )
						{
							nfc.add( object );
							object.setVisible( false );
						}
					}
					ns.content.put( frame, nfc );
				}
			};
			executors.execute( command );
		}

		executors.shutdown();
		try
		{
			final boolean ok = executors.awaitTermination( TIME_OUT_DELAY, TIME_OUT_UNITS );
			if ( !ok )
			{
				System.err.println( "[SpotCollection.crop()] Timeout of " + TIME_OUT_DELAY + " " + TIME_OUT_UNITS + " reached while cropping." );
			}
		}
		catch ( final InterruptedException e )
		{
			e.printStackTrace();
		}
		return ns;
	}

	@Override
	public Comparator< ? super Integer > comparator()
	{
		return content.comparator();
	}

	@Override
	public Set< Entry< Integer, Collection< T >>> entrySet()
	{
		return content.entrySet();
	}

	@Override
	public SortedMap< Integer, Collection< T >> headMap( final Integer toFrame )
	{
		return content.headMap( toFrame );
	}

	@Override
	public SortedMap< Integer, Collection< T >> subMap( final Integer fromFrame, final Integer toFrame )
	{
		return content.subMap( fromFrame, toFrame );
	}

	@Override
	public SortedMap< Integer, Collection< T >> tailMap( final Integer fromFrame )
	{
		return content.tailMap( fromFrame );
	}

	@Override
	public Collection< Collection< T >> values()
	{
		return content.values();
	}

	@Override
	public boolean containsKey( final Object frame )
	{
		return content.containsKey( frame );
	}

	@Override
	public boolean containsValue( final Object value )
	{
		return content.containsValue( value );
	}

	@Override
	public Collection< T > get( final Object frame )
	{
		return content.get( frame );
	}

	@Override
	public boolean isEmpty()
	{
		return content.isEmpty();
	}

	@Override
	public void putAll( final Map< ? extends Integer, ? extends Collection< T >> map )
	{
		content.putAll( map );
	}

	@Override
	public Collection< T > remove( final Object frame )
	{
		return content.remove( frame );
	}

	@Override
	public int size()
	{
		return content.size();
	}

	/*
	 * INNER CLASSES
	 */

	/**
	 * A convenience wrapper that implements {@link Iterable} for this spot
	 * collection.
	 */
	private final class WholeCollectionIterable implements Iterable< T >
	{

		private final boolean visibleObjectsOnly;

		public WholeCollectionIterable( final boolean visibleObjectsOnly )
		{
			this.visibleObjectsOnly = visibleObjectsOnly;
		}

		@Override
		public Iterator< T > iterator()
		{
			if ( visibleObjectsOnly )
			{
				return new VisibleObjectsIterator();
			}
			else
			{
				return new AllObjectsIterator();
			}
		}
	}

	private final class FrameVisibleIterable implements Iterable< T >
	{

		private final int frame;

		public FrameVisibleIterable( final int frame )
		{
			this.frame = frame;
		}

		@Override
		public Iterator< T > iterator()
		{
			return new VisibleObjectsFrameIterator( content.get( frame ) );
		}
	}

	private final Iterator< T > EMPTY_ITERATOR = new Iterator< T >()
	{

		@Override
		public boolean hasNext()
		{
			return false;
		}

		@Override
		public T next()
		{
			return null;
		}

		@Override
		public void remove()
		{}
	};
}
