package fiji.plugin.trackmate.tracking.jonkervolgenant;

import java.util.Arrays;

import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.OutputAlgorithm;
import fiji.plugin.trackmate.tracking.hungarian.JonkerVolgenantAlgorithm;

/**
 * Implements the Jonker-Volgenant algorithm for linear assignment problems,
 * tailored for sparse cost matrices.
 * <p>
 * We rely on the {@link SparseCostMatrix} class to represent these costs. The
 * implementation itself is an unlikely mix between:
 * <ul>
 * <li>my (JYT) limited understanding of the original Volgemant paper (
 * <code>Volgenant. Linear and semi-assignment problems:
 * A core oriented approach. Computers & Operations Research (1996) vol. 23 (10)
 * pp. 917-932</code>);</li>
 * <li>my limited understanding of Lee Kamensky python implementation of the
 * same algorithm in python using numpy for the CellProfilter project (
 * {@link https://
 * github.com/CellProfiler/CellProfiler/blob/master/cellprofiler/cpmath/lapjv.
 * py});</li>
 * <li>Johannes java implementation of the algorithm for non-sparse matrices (
 * {@link JonkerVolgenantAlgorithm}).</li>
 * </ul>
 * <p>
 * Computation time performance degrades significantly compared to the
 * non-sparse version. Benchmarks show that when increasing the density from
 * 0.1% to 70%, the computation time increased by a factor ranging from 1.5 to 7
 * compared to the non-sparse version. For a given density, the comparison
 * depends very weakly on the matrix size.
 * 
 * 
 * @author Jean-Yves Tinevez - 2014
 * @author Johannes Schindelin
 * @see http://www.sciencedirect.com/science/article/pii/030505489600010X#
 * @see JonkerVolgenantAlgorithm
 * 
 */
public class JonkerVolgenantSparseAlgorithm implements OutputAlgorithm< int[] >, Benchmark
{

	private int[] output;

	private String errorMessage;

	private long processingTime;

	private final SparseCostMatrix cm;

	/**
	 * Instantiates a new Jonker-Volgenant algorithm for the specified sparse
	 * cost matrix.
	 * 
	 * @param cm
	 *            the cost matrix of the linear assignment problem to solve.
	 */
	public JonkerVolgenantSparseAlgorithm( final SparseCostMatrix cm )
	{
		this.cm = cm;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		final int[] x = new int[ cm.nRows ];
		final int[] y = new int[ cm.nCols ];
		final double[] v = new double[ cm.nCols ];

		final int[] col = new int[ cm.nCols ];
		for ( int j = 0; j < col.length; j++ )
		{
			col[ j ] = j;
		}

		/*
		 * Column reduction
		 */

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

		/*
		 * Reduction transfer.
		 */

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

		if ( f == 0 ) { return true; }

		/*
		 * Augmenting row reduction.
		 */

		for ( int count = 0; count < 2; count++ )
		{
			int k = 0;
			final int f0 = f;
			f = 0;
			while ( k < f0 )
			{
				final int i = free[ k++ ];
				double v0 = Double.MAX_VALUE;
				int j0 = 0, j1 = -1;
				double vj = Double.MAX_VALUE;
				for ( int kj = cm.start[ i ]; kj < cm.start[ i ] + cm.number[ i ]; kj++ )
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

		/*
		 * Augmentation.
		 */

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
				pred[ j ] = i1;
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
		}

		/*
		 * Terminate and prepare outputs.
		 */

		this.output = new int[ x.length ];
		for ( int i = 0; i < x.length; i++ )
		{
			output[ i ] = x[ i ] - 1;
		}

		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
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
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	/**
	 * Returns JVS results as row assignments. The row <code>i</code> is
	 * associated to the column <code>x[i]</code> in the cost matrix.
	 * 
	 * @return the row assignments as an <code>int[]</code> array. This array is
	 *         re-instantiated upon calling {@link #process()}.
	 */
	@Override
	public int[] getResult()
	{
		return output;
	}
}
