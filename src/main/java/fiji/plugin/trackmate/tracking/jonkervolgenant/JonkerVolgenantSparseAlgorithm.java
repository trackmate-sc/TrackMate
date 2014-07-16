package fiji.plugin.trackmate.tracking.jonkervolgenant;

import java.util.Arrays;

import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.OutputAlgorithm;

public class JonkerVolgenantSparseAlgorithm implements OutputAlgorithm< int[] >, Benchmark
{

	private final int[] x;

	private final int[] y;

	private final double[] v;

	private String errorMessage;

	private long processingTime;

	private final CRSMatrix cm;

	public JonkerVolgenantSparseAlgorithm( final CRSMatrix cm )
	{
		this.cm = cm;
		this.x = new int[ cm.nRows ];
		this.y = new int[ cm.nCols ];
		this.v = new double[ cm.nCols ];
	}

	/*
	 * CORE METHODS
	 */

	private void exec()
	{
		final int[] col = new int[ cm.nCols ];
		for ( int j = 0; j < col.length; j++ )
		{
			col[ j ] = j;
		}

		Arrays.fill( v, Double.MAX_VALUE );
		for ( int i = 0; i < cm.nRows; i++ )
		{
			for ( int k = cm.start[ i ]; k < cm.start[ i ] + cm.number[ i ]; k++ )
			{
				final int j = cm.kk[ k ];
				if ( cm.cc[ k ] < v[ j ] )
				{
					v[ j ] = cm.cc[ k ];
					y[ j ] = i + 1;
				}
			}
		}

		for ( int j = cm.nCols - 1; j >= 0; j-- )
		{
			final int i = y[ j ] - 1;
			if ( x[ i ] == 0 )
			{
				x[ i ] = j + 1;
			}
			else
			{
				if ( x[ i ] > 0 )
				{
					x[ i ] = -x[ i ];
				}
				y[ j ] = 0;
			}
		}

		int f = 0;
		final int[] free = new int[ cm.nRows ];
		for ( int i = 0; i < cm.nRows; i++ )
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
				for ( int k = cm.start[ i ]; k < cm.start[ i ] + cm.number[ i ]; k++ )
				{
					final int j = cm.kk[ k ];
					if ( j != j1 )
					{
						if ( cm.cc[ k ] - v[ j ] < min )
						{
							min = cm.cc[ k ] - v[ j ];
						}
					}
				}
				v[ j1 ] -= min;
			}
		}

		if ( f == 0 )
		{
 return;
		}

		System.out.println( "After reduction transfer" );// DEBUG
		for ( int i = 0; i < x.length; i++ )
		{
			System.out.println( "\t" + i + "\t->\t" + ( x[ i ] - 1 ) ); // DEBUG
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
				double v0 = cm.cc[ cm.start[ i ] ] - v[ 0 ];
				int j0 = 0, j1 = -1;
				double vj = Double.MAX_VALUE;
				for ( int kj = cm.start[ i ] + 1; kj < cm.start[ i ] + cm.number[ i ]; kj++ )
				{
					final int j = cm.kk[ kj ];
					final double h = cm.cc[ kj ] - v[ j ];
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

		System.out.println( "After augmenting row reduction" );// DEBUG
		for ( int i = 0; i < x.length; i++ )
		{
			System.out.println( "\t" + i + "\t->\t" + ( x[ i ] - 1 ) ); // DEBUG
		}

		// augmentation
		final int f0 = f;
		final double[] d = new double[ cm.nCols ];
		final int[] pred = new int[ cm.nCols ];
		for ( f = 0; f < f0; f++ )
		{
			final int i1 = free[ f ];
			int low = 0, up = 0;
			// initialize d- and pred-array
			Arrays.fill( d, Double.MAX_VALUE );
			for ( int k = cm.start[ i1 ]; k < cm.start[ i1 ] + cm.number[ i1 ]; k++ )
			{
				final int j = cm.kk[ k ];
				d[ j ] = cm.cc[ k ] - v[ j ];
//				pred[ j ] = i1;
			}
			int last;
			int i, j = -1;
			double min = Double.MAX_VALUE;
			LOOP: do
			{
				// find new columns with new value for minimum d
				// unnecessary, even if it is in the paper:
				// if (up == low)
				{
					last = low;
					min = d[ col[ up++ ] ];
					for ( int k = up; k < cm.nCols; k++ )
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

					final int kj1 = Arrays.binarySearch( cm.kk, cm.start[ i ], cm.start[ i ] + cm.number[ i ], j1 );
					if ( kj1 < 0 )
					{
						continue;
					}

					final double u1 = cm.cc[ kj1 ] - v[ j1 ] - min;
					for ( int k = up; k < cm.nCols; k++ )
					{
						j = col[ k ];
						final int kj = Arrays.binarySearch( cm.kk, cm.start[ i ], cm.start[ i ] + cm.number[ i ], j );
						if ( kj < 0 )
						{
							continue;
						}

						final double h = cm.cc[ kj ] - v[ j ] - u1;
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

			System.out.println( "Augmenting " + f );// DEBUG
			for ( int iii = 0; iii < x.length; iii++ )
			{
				System.out.println( "\t" + iii + "\t->\t" + ( x[ iii ] - 1 ) ); // DEBUG
			}
		}
	}

	/*
	 * ALGORITHM METHODS
	 */

	@Override
	public boolean checkInput()
	{
		return true;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		exec();

		// Dec
		for ( int i = 0; i < x.length; i++ )
		{
			x[ i ]--;
		}
		for ( int j = 0; j < y.length; j++ )
		{
			y[ j ]--;
		}

		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public int[] getResult()
	{
		return x;
	}

	/*
	 * MAIN METHOD
	 */

	public static void main( final String[] args )
	{
		final int[] kk = new int[] { 0, 1, 2, 3, 4, 5, 0, 2, 3, 4, 0, 1, 2, 3, 0, 1, 2, 0, 1, 0, 2, 3, 5 };
		final double[] cc = new double[] { 20.1, 19.2, 18.3, 17.4, 16.5, 15.6, 14.1, 12.8, 11.9, 10.7, 9.2, 8.3, 7.4, 6.5, 5.8, 4.7, 3.6, 2.9, 1.1, 10.2, 1.3, 2.4, 10.2 };
		final int[] number = new int[] { 6, 4, 4, 3, 2, 4 };
		final CRSMatrix cm = new CRSMatrix( cc, kk, number );
		System.out.println( cm.toString() );

		final JonkerVolgenantSparseAlgorithm algo = new JonkerVolgenantSparseAlgorithm( cm );
		if ( !algo.checkInput() || !algo.process() )
		{
			System.out.println( algo.getErrorMessage() );
		}
		else
		{
			final int[] x = algo.getResult();
			System.out.println( "Solution:" );
			for ( int i = 0; i < x.length; i++ )
			{
				System.out.println( "\t" + i + "\t->\t" + x[ i ] );
			}
		}


	}

}
