package fiji.plugin.trackmate.tracking;


import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;

public interface TrackableObjectCollection<T extends TrackableObject> {
	
	/*
	 * METHODS
	 */
	public T search( int ID );

	@Override
	public String toString();

	public void add( T trackableObject, Integer frame );

	public boolean remove( T trackableObject, Integer frame );

	public void setVisible( boolean visible );

	public T getClosestObject( T trackableObject, int frame, boolean visibleObjectsOnly );

	public T getObjectAt( T trackableObject, int frame, boolean visibleObjectsOnly );

	public List< T > getNClosestObjects( T trackableObject, int frame, int n, boolean visibleObjectsOnly );

	public int getNObjects( boolean visibleObjectsOnly );

	public int getNObjects( int frame, boolean visibleObjectsOnly );

	public Iterator< T > iterator( boolean visibleObjectsOnly );

	public Iterator< T > iterator( Integer frame, boolean visibleObjectsOnly );

	public Iterable< T > iterable( boolean visibleObjectsOnly );

	public Iterable< T > iterable( int frame, boolean visibleObjectsOnly );

	/*
	 * SORTEDMAP
	 */
	public void put( int frame, Collection< T > spots );
	
	public Integer firstKey();

	public Integer lastKey();

	public NavigableSet< Integer > keySet();

	public void clear();

	public TrackableObjectCollection<T> crop();


}
