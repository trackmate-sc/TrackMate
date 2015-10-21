package fiji.plugin.trackmate.tracking.oldlap.hungarian;

/**
 * Implements the LAPJV algorithm.
 * <p>
 * Based on: Jonker, R., &amp; Volgenant, A. (1987). <i>A shortest augmenting
 * path algorithm for dense and sparse linear assignment problems</i>.
 * Computing, 38(4), 325-340.
 * </p>
 * 
 * @author Johannes Schindelin
 */

public class JonkerVolgenantAlgorithm implements AssignmentAlgorithm
{
	@Override
	public int[][] computeAssignments( final double[][] costMatrix )
	{
		final int n = costMatrix.length;
		final double[] v = new double[ n ];

		// x and y contain the row/column indexes *plus 1* so that
		// x[column] == 0 means it is unassigned
		final int[] x = new int[ n ];
		final int[] y = new int[ n ];

		final int[] col = new int[ n ];

		// initialization
		// step 1: column reduction
		for ( int j = n - 1; j >= 0; j-- )
		{
			col[ j ] = j;
			double h = costMatrix[ 0 ][ j ];
			int i1 = 0;
			for ( int i = 1; i < n; i++ )
			{
				if ( costMatrix[ i ][ j ] < h )
				{
					h = costMatrix[ i ][ j ];
					i1 = i;
				}
			}
			v[ j ] = h;
			if ( x[ i1 ] == 0 )
			{
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
		final int[] free = new int[ n ];
		for ( int i = 0; i < n; i++ )
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
				for ( int j = 0; j < n; j++ )
				{
					if ( j != j1 )
					{
						if ( costMatrix[ i ][ j ] - v[ j ] < min )
						{
							min = costMatrix[ i ][ j ] - v[ j ];
						}
					}
				}
				v[ j1 ] -= min;
			}
		}

		if ( f == 0 )
		{
			final int[][] solution = new int[ n ][ 2 ];
			for ( int i = 0; i < n; i++ )
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
				double v0 = costMatrix[ i ][ 0 ] - v[ 0 ];
				int j0 = 0, j1 = -1;
				double vj = Double.MAX_VALUE;
				for ( int j = 1; j < n; j++ )
				{
					final double h = costMatrix[ i ][ j ] - v[ j ];
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
		final double[] d = new double[ n ];
		final int[] pred = new int[ n ];
		for ( f = 0; f < f0; f++ )
		{
			final int i1 = free[ f ];
			int low = 0, up = 0;
			// initialize d- and pred-array
			for ( int j = 0; j < n; j++ )
			{
				d[ j ] = costMatrix[ i1 ][ j ] - v[ j ];
				pred[ j ] = i1;
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
					for ( int k = up; k < n; k++ )
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
					final double u1 = costMatrix[ i ][ j1 ] - v[ j1 ] - min;
					for ( int k = up; k < n; k++ )
					{
						j = col[ k ];
						final double h = costMatrix[ i ][ j ] - v[ j ] - u1;
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

		final int[][] solution = new int[ n ][ 2 ];
		for ( int i = 0; i < n; i++ )
		{
			solution[ i ][ 0 ] = i;
			solution[ i ][ 1 ] = x[ i ] - 1;
		}
		return solution;
	}
}
