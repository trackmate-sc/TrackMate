package fiji.plugin.trackmate.tracking;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.SortedMap;

/**
 * Interface for classes that manage a large collection of
 * {@link TrackableObject}s.
 * <p>
 * The interface relies heavily on the ability of {@link TrackableObject}s to
 * return and specify the frame they belong to, so as to return a sorted and
 * hashed-per-frame collection.
 *
 * @param <T>
 *            the class of the objects stored in this collection. Must implement
 *            {@link TrackableObject}.
 */
public interface TrackableObjectCollection< T extends TrackableObject > extends SortedMap< Integer, Collection< T > >
{

	/*
	 * METHODS
	 */

	/**
	 * Returns the object in this collection with the specified ID. Returns
	 * <code>null</code> if an object with this ID cannot be found in this
	 * collection.
	 *
	 * @param ID
	 *            the ID to look for.
	 * @return the object found, or <code>null</code>.
	 */
	public T search( int ID );

	/**
	 * Adds the specified object to this collection, at the temporal frame
	 * specified.
	 * <p>
	 * If the frame does not exist yet in the collection, it is created and
	 * added to the keys of this collection. By calling this method, the
	 * specified object has its frame field updated with the specified frame
	 * value.
	 *
	 * @param obj
	 *            the object to store.
	 * @param frame
	 *            the frame to store it at. Must not be <code>null</code>.
	 */
	public void add( T obj, Integer frame );

	/**
	 * Removes the specified object from this collection, if it is at the
	 * specified frame.
	 *
	 * @param obj
	 *            the object to remove.
	 * @param frame
	 *            the frame it is to be found at.
	 * @return <code>true</code> if and only if the object could be found at the
	 *         specified frame and was successfully removed from this
	 *         collection.
	 */
	public boolean remove( T obj, Integer frame );

	/**
	 * Marks all the objects of this collection with the specified visibility
	 * flag.
	 * <p>
	 * This is done by modifying the visibility stored in all the
	 * {@link TrackableObject} of this collection.
	 *
	 * @param visible
	 *            the visibility flag.
	 */
	public void setVisible( boolean visible );

	/**
	 * Returns the object of this collection belonging to the specified frame
	 * closest to the specified object.
	 * <p>
	 * Concrete implementations decide the proper meaning of "closest to".
	 *
	 * @param obj
	 *            the object around which to search for.
	 * @param frame
	 *            the frame of this collection to search in.
	 * @param visibleObjectsOnly
	 *            if <code>true</code>, the search will take place only amongst
	 *            object marked as visible.
	 * @return the closest object in the specified frame, or <code>null</code>
	 *         if such an object could not be found (<i>e.g.</i> if the
	 *         specified frame does not contain any object).
	 */
	public T getClosestObject( T obj, int frame, boolean visibleObjectsOnly );

	// FIXME shall we get rid of this?
	public T getObjectAt( T obj, int frame, boolean visibleObjectsOnly );

	/**
	 * Returns the <code>n</code> objects of this collection belonging to the
	 * specified frame closest to the specified object.
	 * <p>
	 * Concrete implementations decide the proper meaning of "closest to". If
	 * the number of objects in the frame is exhausted, a shorter list is
	 * returned.
	 *
	 * @param obj
	 *            the object around which to search for.
	 * @param frame
	 *            the frame of this collection to search in.
	 * @param n
	 *            the number of spots to search for.
	 * @param visibleObjectsOnly
	 *            if <code>true</code>, the search will take place only amongst
	 *            object marked as visible.
	 * @return a new list, with of at most <code>n</code> objects.
	 */
	public List< T > getNClosestObjects( T obj, int frame, int n, boolean visibleObjectsOnly );

	/**
	 * Returns the number of objects in this whole collection.
	 *
	 * @param visibleObjectsOnly
	 *            if <code>true</code>, only the objects marked as visible will
	 *            be counted.
	 * @return the number of objects.
	 */
	public int getNObjects( boolean visibleObjectsOnly );

	/**
	 * Returns the number of objects in the specified frame of this collection.
	 *
	 * @param frame
	 *            the frame to count objects in.
	 * @param visibleObjectsOnly
	 *            if <code>true</code>, only the objects marked as visible will
	 *            be counted.
	 * @return the number of objects in the specified frame.
	 */
	public int getNObjects( int frame, boolean visibleObjectsOnly );

	/**
	 * Returns a new {@link Iterator} that will iterate over this whole
	 * collection.
	 *
	 * @param visibleObjectsOnly
	 *            if <code>true</code>, only objects marked as visible will be
	 *            iterated.
	 * @return a new iterator.
	 */
	public Iterator< T > iterator( boolean visibleObjectsOnly );

	/**
	 * Returns a new {@link Iterator} that will iterate over the objects of this
	 * collection contained in the specified frame.
	 *
	 * @param frame
	 *            the frame to iterate over.
	 * @param visibleObjectsOnly
	 *            if <code>true</code>, only objects marked as visible will be
	 *            iterated.
	 * @return a new iterator.
	 */
	public Iterator< T > iterator( Integer frame, boolean visibleObjectsOnly );

	/**
	 * Returns an {@link Iterable} over the objects of this collection contained
	 * in the specified frame.
	 *
	 * @param visibleObjectsOnly
	 *            if <code>true</code>, only objects marked as visible will be
	 *            iterated.
	 * @return a new iterator.
	 */
	public Iterable< T > iterable( boolean visibleObjectsOnly );

	/**
	 * Returns an {@link Iterable} over the objects of this collection contained
	 * in the specified frame.
	 *
	 * @param frame
	 *            the frame to iterate over.
	 * @param visibleObjectsOnly
	 *            if <code>true</code>, only objects marked as visible will be
	 *            iterated.
	 * @return a new iterator.
	 */
	public Iterable< T > iterable( int frame, boolean visibleObjectsOnly );

	/*
	 * SORTEDMAP
	 */

	/**
	 * Replaces the content of this collection at the specified frame by the
	 * specified object collection.
	 * 
	 * @param frame
	 *            the frame to store the objects at.
	 * @param objs
	 *            the collection of objects to put at the specified frame.
	 * @return the previous collection associated with this frame, or
	 *         <code>null</code> if there was no objects for frame. (A
	 *         <code>null</code> return can also indicate that the map
	 *         previously associated <code>null</code> with frame, if the
	 *         implementation supports <code>null</code> values.)
	 */
	@Override
	public Collection< T > put( Integer frame, Collection< T > objs );

	/**
	 * Returns the first frame for which this collection stores at least one
	 * object.
	 *
	 * @return the first frame with non-<code>null</code> content.
	 */
	@Override
	public Integer firstKey();

	/**
	 * Returns the last frame for which this collection stores at least one
	 * object.
	 *
	 * @return the last frame with non-<code>null</code> content.
	 */
	@Override
	public Integer lastKey();

	/**
	 * Returns the a {@link NavigableSet} of the frames for which this
	 * collection has a non-<code>null</code> content.
	 *
	 * @return a {@link NavigableSet}.
	 */
	@Override
	public NavigableSet< Integer > keySet();

	/**
	 * Removes any content from this collection.
	 */
	@Override
	public void clear();

	/**
	 * Returns a new collection built by including only the spots marked as
	 * visible.
	 *
	 * @return a new {@link TrackableObjectCollection}
	 */
	public TrackableObjectCollection< T > crop();

}
