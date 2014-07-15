package fiji.plugin.trackmate.tracking.jonkervolgenant;

import java.util.Arrays;

import net.imglib2.util.Util;

/**
 * Implements the LAPJV algorithm.
 * <p>
 * Based on: Jonker, R., & Volgenant, A. (1987). <i>A shortest augmenting path
 * algorithm for dense and sparse linear assignment problems</i>. Computing,
 * 38(4), 325-340.
 * </p>
 * 
 * @author Johannes Schindelin
 */

public class JVAlgorithmSparse
{
	public int[][] computeAssignments( final SparseCostMatrix cm )
	{
		final int nCols = cm.nCols;
		final int nRows = cm.nRows;
		final double[] v = new double[ nCols ];

		// x and y contain the row/column indexes *plus 1* so that
		// x[column] == 0 means it is unassigned
		final int[] x = new int[ nRows ];
		final int[] y = new int[ nCols ];

		final int[] col = new int[ nCols ];

		// initialization
		// step 1: column reduction
		for ( int j = nCols - 1; j >= 0; j-- )
		{
			final int[] currentCol = cm.cols[ j ];

			col[ j ] = j;
			double h = cm.costs[ currentCol[ 0 ] ];
			int i1 = cm.sources[ currentCol[ 0 ] ];
			for ( int kc = 1; kc < currentCol.length; kc++ )
			{
				final double currentCost = cm.costs[ currentCol[ kc ] ];
				if ( currentCost < h )
				{
					h = currentCost;
					i1 = cm.sources[ currentCol[ kc ] ]; // i1 is row
				}
			}
			v[ j ] = h;
			if ( x[ i1 ] == 0 )
			{
				// That row is free! Yeah!
				x[ i1 ] = j + 1;
				y[ j ] = i1 + 1;
			}
			else
			{
				if ( x[ i1 ] > 0 )
				{
					x[ i1 ] = -x[ i1 ];
				}
				y[ j ] = 0;
			}
		}

		// step 2: reduction transfer
		int f = 0;
		final int[] free = new int[ nRows ];
		for ( int i = 0; i < nRows; i++ )
		{
			if ( x[ i ] == 0 )
			{
				// unassigned row in free-array
				free[ f++ ] = i;
				continue;
			}
			else if ( x[ i ] < 0 )
			{
				// no reduction transfer possible
				x[ i ] = -x[ i ];
			}
			else
			{
				// reduction transfer from assigned row
				final int j1 = x[ i ] - 1;
				double min = Double.MAX_VALUE;

				final int[] row = cm.rows[ i ];

				for ( final int kr : row )
				{
					final int j = cm.targets[ kr ]; // j is col
					if ( j != j1 )
					{
						final double c = cm.costs[ kr ];
						if ( c - v[ j ] < min )
						{
							min = c - v[ j ];
						}
					}
				}
				v[ j1 ] -= min;
			}
		}

		if ( f == 0 )
		{
			final int[][] solution = new int[ nRows ][ 2 ];
			for ( int i = 0; i < nRows; i++ )
			{
				solution[ i ][ 0 ] = i;
				solution[ i ][ 1 ] = x[ i ] - 1;
			}
			return solution;
		}

		// improve initial solution
		// augmenting row reduction
		for ( int count = 0; count < 2; count++ )
		{
			int k = 0;
			final int f0 = f;
			f = 0;
			while ( k < f0 )
			{
				final int i = free[ k++ ];
				final int[] row = cm.rows[ i ];
				final int kc0 = row[ 0 ];
				final int firstCol = cm.targets[ kc0 ];
				double v0 = cm.costs[ kc0 ] - v[ firstCol ];
				int j0 = 0, j1 = -1;
				double vj = Double.MAX_VALUE;
				for ( int kj = 1; kj < row.length; kj++ )
				{
					final int j = cm.targets[ row[ kj ] ]; // j is col
					final double h = cm.costs[ row[ kj ] ] - v[ j ];
					if ( h < vj )
					{
						if ( h > v0 )
						{
							vj = h;
							j1 = j;
						}
						else
						{
							vj = v0;
							v0 = h;
							j1 = j0;
							j0 = j;
						}
					}
				}
				int i0 = y[ j0 ] - 1;
				if ( v0 < vj )
				{
					v[ j0 ] -= vj - v0;
				}
				else
				{
					if ( i0 >= 0 )
					{
						j0 = j1;
						i0 = y[ j1 ] - 1;
					}
				}
				if ( i0 >= 0 )
				{
					if ( v0 < vj )
					{
						free[ --k ] = i0;
					}
					else
					{
						free[ f++ ] = i0;
					}
				}
				x[ i ] = j0 + 1;
				y[ j0 ] = i + 1;
			}
		}

		// augmentation
		final int f0 = f;
		final double[] d = new double[ nCols ];
		final int[] pred = new int[ nCols ];
		for ( f = 0; f < f0; f++ )
		{
			final int i1 = free[ f ];
			int low = 0, up = 0;
			// initialize d- and pred-array
			Arrays.fill( pred, i1 );
			Arrays.fill( d, Double.MAX_VALUE );
			final int[] rowi1 = cm.rows[ i1 ];
			for ( final int ki : rowi1 )
			{
				final int j = cm.targets[ ki ];
				d[ j ] = cm.costs[ ki ] - v[ j ];
//				pred[ j ] = i1;
			}

			int last, i, j = -1;
			double min;
			LOOP: do
			{
				// find new columns with new value for minimum d
				// unnecessary, even if it is in the paper:
				// if (up == low)
				{
					last = low;
					min = d[ col[ up++ ] ];
					for ( int k = up; k < nCols; k++ )
					{
						j = col[ k ];
						final double h = d[ j ];
						if ( h <= min )
						{
							if ( h < min )
							{
								up = low;
								min = h;
							}
							col[ k ] = col[ up ];
							col[ up++ ] = j;
						}
					}
					for ( int h = low; h < up; h++ )
					{
						j = col[ h ];
						if ( y[ j ] == 0 )
						{
							break LOOP;
						}
					}
				}
				// scan a row
				do
				{
					final int j1 = col[ low++ ];
					i = y[ j1 ] - 1;

					// Manually search :( for the linear index or row i, col j1
					final int[] colj1 = cm.cols[ j1 ];
					int l = -1;
					for ( int k2 = 0; k2 < colj1.length; k2++ )
					{
						if ( cm.sources[ colj1[ k2 ] ] == i )
						{
							l = colj1[ k2 ];
							break;
						}
					}
					if ( l < 0 )
					{
						continue;
					}

					final double u1 = cm.costs[ l ] - v[ j1 ] - min;
					for ( int k = up; k < nCols; k++ )
					{
						j = col[ k ];
						// Manually search :( for the linear index or row i, col
						// j
						final int[] currentCol = cm.cols[ j ];
						int l2 = -1;
						for ( int k2 = 0; k2 < colj1.length; k2++ )
						{
							if ( cm.sources[ currentCol[ k2 ] ] == i )
							{
								l2 = currentCol[ k2 ];
								break;
							}
						}
						if ( l2 < 0 )
						{
							continue;
						}

						final double h = cm.costs[ l2 ] - v[ j ] - u1;

						if ( h < d[ j ] )
						{
							d[ j ] = h;
							pred[ j ] = i;
							if ( h == min )
							{
								if ( y[ j ] == 0 )
								{
									break LOOP;
								}
								col[ k ] = col[ up ];
								col[ up++ ] = j;
							}
						}
					}
				}
				while ( low != up );
			}
			while ( low == up );

			// updating of column pieces
			for ( int k = 0; k < last; k++ )
			{
				final int j0 = col[ k ];
				v[ j0 ] += d[ j0 ] - min;
			}

			// augmentation
			do
			{
				i = pred[ j ];
				y[ j ] = i + 1;
				final int k = j;
				j = x[ i ] - 1;
				x[ i ] = k + 1;
			}
			while ( i1 != i );
		}

		final int[][] solution = new int[ nRows ][ 2 ];
		for ( int i = 0; i < nRows; i++ )
		{
			solution[ i ][ 0 ] = i;
			solution[ i ][ 1 ] = x[ i ] - 1;
		}
		return solution;
	}


	public static void main( final String[] args )
	{
		final int[] i = new int[] { 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 4, 4, 5, 5, 5, 5, 4, 4 };
		final int[] j = new int[] { 0, 1, 2, 3, 4, 5, 0, 2, 3, 4, 0, 1, 2, 3, 0, 1, 2, 0, 1, 0, 2, 3, 5, 4, 5 };
		final double[] c = new double[] { 20.1, 19.2, 18.3, 17.4, 16.5, 15.6, 14.1, 12.8, 11.9, 10.7, 9.2, 8.3, 7.4, 6.5, 5.8, 4.7, 3.6, 2.9, 1.1, 10.2, 1.3, 2.4, 10.2, 6.5, 7.2 };
		final SparseCostMatrix sm = new SparseCostMatrix( i, j, c );

		System.out.println( sm.toString() );

		final JVAlgorithmSparse algo = new JVAlgorithmSparse();
		final int[][] result = algo.computeAssignments( sm );
		for ( final int[] is : result )
		{
			System.out.println( Util.printCoordinates( is ) );
		}

	}
}
