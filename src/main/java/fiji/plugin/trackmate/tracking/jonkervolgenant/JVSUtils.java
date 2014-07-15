package fiji.plugin.trackmate.tracking.jonkervolgenant;

import java.util.Arrays;

/**
 * Static utilities for the sparse Konker-Volgenant solver.
 * 
 * @author Jean-Yves Tinevez - 2014
 */
public class JVSUtils
{
	/*
	 * UTIL METHODS
	 */
	

	/**
	 * Returns a 3-element array of <code>int</code> arrays:
	 * <ol start="0">
	 * <li>the array of unique values in the specified array, sorted by
	 * ascending order.</li>
	 * <li>the count of each of these unique values in the specified array.</li>
	 * <li>the index in the unique value array of each value in the specified
	 * array.</li>
	 * </ol>
	 * 
	 * @return a new <code>int[][]</code>.
	 */
	public static final int[][] bincount( final int[] vals )
	{
		final int[] unique = unique( vals );
		final int[] index = indexIn( vals, unique );
		final int[] bc = new int[ unique.length ];
		for ( int i = 0; i < vals.length; i++ )
		{
			bc[ index[ i ] ]++;
		}
		return new int[][] { unique, bc, index };
	}

	public static final String bincountToString( final int[][] bc )
	{
		final int[] unique = bc[ 0 ];
		final int[] count = bc[ 1 ];
		final int[] index = bc[ 2 ];
		final StringBuilder str = new StringBuilder();
		str.append( "Array with " + index.length + " elements.\n" );
		str.append( "\tunique: " );
		for ( final int i : unique )
		{
			str.append( "\t" + i );
		}
		str.append( '\n' );
		str.append( "\tcount\t:" );
		for ( final int i : count )
		{
			str.append( "\t" + i );
		}
		str.append( '\n' );
		return str.toString();
	}

	/**
	 * Returns the sorted array of unique values of the specified array.
	 * 
	 * @param vals
	 *            the array to inspect.
	 * @return a new <code>int[]</code> array containing the unique values,
	 *         sorted by ascending order.
	 */
	public static final int[] unique( final int[] vals )
	{
		final int[] result = vals.clone();
		Arrays.sort( result );
		int j = 0;
		for ( int i = 1; i < vals.length; i++ )
		{
			if ( result[ j ] != result[ i ] )
			{
				result[ ++j ] = result[ i ];
			}
		}
		return Arrays.copyOf( result, ++j );
	}

	/**
	 * Return the index of each value in the first array in the second one. The
	 * second array <b>must be sorted</b>.
	 * 
	 * @param vals
	 *            the array to index.
	 * @param catalog
	 *            the catalog array, must be sorted.
	 * @return the index array, that is, <code>index[i]</code> contains the
	 *         index in the catalog of the value <code>val[i]</code>;
	 */
	public static final int[] indexIn( final int[] vals, final int[] catalog )
	{
		final int[] index = new int[ vals.length ];
		for ( int i = 0; i < vals.length; i++ )
		{
			final int val = vals[ i ];
			final int p = Arrays.binarySearch( catalog, val );
			index[ i ] = p;
		}
		return index;
	}

	public final static String resultToString( final int[][] result )
	{
		final StringBuilder str = new StringBuilder();

		str.append( "SOURCE\t→\tTARGET\n" );
		for ( int k = 0; k < result.length; k++ )
		{
			str.append( "" + result[ k ][ 0 ] + "\t→\t" + result[ k ][ 1 ] + "\n" );
		}
		return str.toString();
	}

	public static final String printResults( final String header, final int[] x, final int[] y, final double[] u, final double[] v )
	{
		final StringBuilder str = new StringBuilder();
		str.append( "\n\n_______________________________________________________\n" );
		str.append( '\t' + header + '\n' );
		str.append( "\tx[]:\n" );
		for ( int i = 0; i < x.length; i++ )
		{
			str.append( "\t\t" + i + ":\t" + x[ i ] + '\n' );
		}
		str.append( "\ty[]:\n" );
		for ( int i = 0; i < y.length; i++ )
		{
			str.append( "\t\t" + i + ":\t" + y[ i ] + '\n' );
		}
		str.append( "\tu[]:\n" );
		for ( int i = 0; i < u.length; i++ )
		{
			str.append( "\t\t" + i + ":\t" + u[ i ] + '\n' );
		}
		str.append( "\tv[]:\n" );
		for ( int i = 0; i < v.length; i++ )
		{
			str.append( "\t\t" + i + ":\t" + v[ i ] + '\n' );
		}
		return str.toString();
	}

	/**
	 * Returns the elegant pairing of Matthew Szudzik. Uniquely associates a
	 * <code>long</code> integer to two <code>int</code> non-negative integers.
	 * 
	 * @param x
	 *            the first integer. Must be non-negative.
	 * @param y
	 *            the second integer. Must be non-negative.
	 * @return a <code>long</code> uniquely associated to the two integers.
	 * @see http://szudzik.com/ElegantPairing.pdf
	 */
	public static final long szudzikPair( final int x, final int y )
	{
		assert x >= 0 && y >= 0;
		final long lx = x;
		final long ly = y;
		if ( lx >= ly )
		{
			return lx * lx + lx + ly;
		}
		else
		{
			return ly * ly + lx;
		}
	}

	/**
	 * Retrieves the two <code>int</code> integers associated to the specified
	 * <code>long</code>, following the elegant pairing of Matthew Szudzik.
	 * 
	 * @param z
	 *            the <code>long</code> paired integer.
	 * @return a <code>int</code> array of 2 elements containing the paired
	 *         integers.
	 * @see http://szudzik.com/ElegantPairing.pdf
	 */
	public static final int[] szudzikUnpair( final long z )
	{
		final int[] pair = new int[ 2 ];
		final double preciseZ = Math.sqrt( z );
		long floor = ( long ) Math.floor( preciseZ );
		if ( floor * floor > z )
		{
			floor--;
		}
		final long t = z - floor * floor;
		if ( t < floor )
		{
			pair[ 0 ] = ( int ) t;
			pair[ 1 ] = ( int ) floor;
		}
		else
		{
			pair[ 0 ] = ( int ) floor;
			pair[ 1 ] = ( int ) t - ( int ) floor;
		}
		return pair;
	}


}
