package fiji.plugin.trackmate.util.mesh;

import java.util.BitSet;
import java.util.Comparator;
import java.util.Random;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TLongArrayList;

/**
 * Utilities to sort a Trove list and return the sorting index to sort other
 * lists with.
 */
public class SortArrays
{

	public static void reorder( final TDoubleArrayList data, final int[] ind )
	{
		final BitSet done = new BitSet( data.size() );
		for ( int i = 0; i < data.size() && done.cardinality() < data.size(); i++ )
		{
			int ia = i;
			int ib = ind[ ia ];
			if ( done.get( ia ) )
			{ // index is already done
				continue;
			}
			if ( ia == ib )
			{ // element is at the right place
				done.set( ia );
				continue;
			}
			final int x = ia; // start a loop at x = ia
			// some next index will be x again eventually
			final double a = data.getQuick( ia );
			// keep element a as the last value after the loop
			while ( ib != x && !done.get( ia ) )
			{
				final double b = data.getQuick( ib );
				// element from index b must go to index a
				data.setQuick( ia, b );
				done.set( ia );
				ia = ib;
				ib = ind[ ia ]; // get next index
			}
			data.setQuick( ia, a ); // set value a to last index
			done.set( ia );
		}
	}

	public static int[] quicksort( final TDoubleArrayList main )
	{
		final int[] index = new int[ main.size() ];
		for ( int i = 0; i < index.length; i++ )
			index[ i ] = i;
		quicksort( main, index );
		return index;
	}

	public static void quicksort( final TDoubleArrayList main, final int[] index )
	{
		quicksort( main, index, 0, index.length - 1 );
	}

	// quicksort a[left] to a[right]
	public static void quicksort( final TDoubleArrayList a, final int[] index, final int left, final int right )
	{
		if ( right <= left )
			return;
		final int i = partition( a, index, left, right );
		quicksort( a, index, left, i - 1 );
		quicksort( a, index, i + 1, right );
	}

	// partition a[left] to a[right], assumes left < right
	private static int partition( final TDoubleArrayList a, final int[] index,
			final int left, final int right )
	{
		int i = left - 1;
		int j = right;
		while ( true )
		{
			while ( less( a.getQuick( ++i ), a.getQuick( right ) ) )
				;
			while ( less( a.getQuick( right ), a.getQuick( --j ) ) )
				if ( j == left )
					break; // don't go out-of-bounds
			if ( i >= j )
				break; // check if pointers cross
			exch( a, index, i, j ); // swap two elements into place
		}
		exch( a, index, i, right ); // swap with partition element
		return i;
	}

	// is x < y ?
	private static boolean less( final double x, final double y )
	{
		return ( x < y );
	}

	// exchange a[i] and a[j]
	private static void exch( final TDoubleArrayList a, final int[] index, final int i, final int j )
	{
		final double swap = a.getQuick( i );
		a.setQuick( i, a.getQuick( j ) );
		a.setQuick( j, swap );
		final int b = index[ i ];
		index[ i ] = index[ j ];
		index[ j ] = b;
	}

	/*
	 * Sorting index arrays with a comparator.
	 */

	public static void quicksort( final TLongArrayList main, final Comparator< Long > c )
	{
		final int[] index = new int[ main.size() ];
		for ( int i = 0; i < index.length; i++ )
			index[ i ] = i;
		quicksort( main, 0, main.size(), c );
	}

	private static void quicksort( final TLongArrayList a, final int left, final int right, final Comparator< Long > c )
	{
		if ( right <= left )
			return;
		final int i = partition( a, left, right, c );
		quicksort( a, left, i - 1, c );
		quicksort( a, i + 1, right, c );
	}

	// partition a[left] to a[right], assumes left < right
	private static int partition( final TLongArrayList a,
			final int left, final int right, final Comparator< Long > c )
	{
		int i = left - 1;
		int j = right;
		while ( true )
		{
			while ( less( a.getQuick( ++i ), a.getQuick( right ) ) );
			while ( less( a.getQuick( right ), a.getQuick( --j ) ) )
				if ( j == left )
					break; // don't go out-of-bounds
			if ( i >= j )
				break; // check if pointers cross
			exch( a, i, j ); // swap two elements into place
		}
		exch( a, i, right ); // swap with partition element
		return i;
	}

	// exchange a[i] and a[j]
	private static void exch( final TLongArrayList a, final int i, final int j )
	{
		final long swap = a.getQuick( i );
		a.setQuick( i, a.getQuick( j ) );
		a.setQuick( j, swap );
	}

	/*
	 * Main.
	 */

	public static void main( final String[] args )
	{
		final Random ran = new Random( 1l );
		final int n = 10;
		final TDoubleArrayList arr = new TDoubleArrayList();
		for ( int i = 0; i < n; i++ )
			arr.add( ran.nextDouble() );

		final TDoubleArrayList copy = new TDoubleArrayList( arr );

		System.out.print( String.format( "Before sorting: %4.2f", arr.get( 0 ) ) );
		for ( int i = 1; i < arr.size(); i++ )
			System.out.print( String.format( ", %4.2f", arr.get( i ) ) );
		System.out.println();

		final int[] index = quicksort( arr );
		System.out.print( String.format( "After sorting:  %4.2f", arr.get( 0 ) ) );
		for ( int i = 1; i < arr.size(); i++ )
			System.out.print( String.format( ", %4.2f", arr.get( i ) ) );
		System.out.println();

		System.out.print( String.format( "Index:          %4d", index[ 0 ] ) );
		for ( int i = 1; i < arr.size(); i++ )
			System.out.print( String.format( ", %4d", index[ i ] ) );
		System.out.println();

		reorder( arr, index );
		System.out.print( String.format( "Reorder copy:   %4.2f", copy.get( 0 ) ) );
		for ( int i = 1; i < copy.size(); i++ )
			System.out.print( String.format( ", %4.2f", copy.get( i ) ) );
		System.out.println();
	}


}
