package fiji.plugin.trackmate.tracking.sparselap.linker;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.OutputAlgorithm;
import net.imglib2.util.Util;

/**
 * Implements the Jonker-Volgenant algorithm for linear assignment problems,
 * tailored for sparse cost matrices.
 * <p>
 * We rely on the {@link SparseCostMatrix} class to represent these costs. The
 * implementation itself is an unlikely mix between:
 * <ul>
 * <li>my (JYT) limited understanding of the original Volgemant paper (
 * <code>Volgenant. Linear and semi-assignment problems:
 * A core oriented approach. Computers &amp; Operations Research (1996) vol. 23 (10)
 * pp. 917-932</code>);</li>
 * <li>my limited understanding of Lee Kamensky python implementation of the
 * same algorithm in python using numpy for the CellProfilter project;</li>
 * <li>Johannes java implementation of the algorithm for non-sparse matrices.
 * </li>
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
 * @see <a href=
 *      "http://www.sciencedirect.com/science/article/pii/030505489600010X"> A.
 *      Volgenant
 *      "Linear and semi-assignment problems: A core oriented approach"</a>
 */
public class LAPJV implements OutputAlgorithm< int[] >, Benchmark
{

	private static final String BASE_ERROR_MESSAGE = "[JonkerVolgenantSparseAlgorithm] ";

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
	public LAPJV( final SparseCostMatrix cm )
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
		if ( cm.nRows > cm.nCols )
		{
			errorMessage = BASE_ERROR_MESSAGE + "This solver converges only if the cost matrix has more rows than column. Found " + cm.nRows + " rows and " + cm.nCols + " columns.";
			return false;
		}
		final double minCost = Util.min( cm.cc );
		if ( minCost <= 0 )
		{
			errorMessage = BASE_ERROR_MESSAGE + "This solver only accept strictly positive costs. Found " + minCost + ".";
			return false;
		}
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

	public String resultToString()
	{
		return resultToString( Collections.emptyList(), Collections.emptyList() );
	}

	public String resultToString( final List< ? > rows, final List< ? > cols )
	{
		if ( null == output ) { return "Not solved yet. Process the algorithm prior to calling this method."; }

		final String[] colNames = new String[ cm.nCols ];
		// default names
		for ( int j = 0; j < colNames.length; j++ )
		{
			colNames[ j ] = "" + j;
		}
		final String[] rowNames = new String[ cm.nRows ];
		for ( int i = 0; i < rowNames.length; i++ )
		{
			rowNames[ i ] = "" + i;
		}

		for ( int j = 0; j < cols.size(); j++ )
		{
			final Object col = cols.get( j );
			if ( null != col )
			{
				final String str = col.toString();
				colNames[ j ] = str;
			}
		}

		int colWidth = -1;
		for ( final String str : colNames )
		{
			if ( str.length() > colWidth )
			{
				colWidth = str.length();
			}

		}
		colWidth = colWidth + 1;
		colWidth = Math.max( colWidth, 7 );

		final Set< String > unassignedColNames = new HashSet<>( Arrays.asList( colNames ) );

		for ( int i = 0; i < rows.size(); i++ )
		{
			final Object row = rows.get( i );
			if ( null != row )
			{
				final String str = row.toString();
				rowNames[ i ] = str;
			}
		}

		int rowWidth = -1;
		for ( final String str : rowNames )
		{
			if ( str.length() > rowWidth )
			{
				rowWidth = str.length();
			}

		}
		rowWidth = rowWidth + 1;
		rowWidth = Math.max( 7, rowWidth );

		final StringBuilder str = new StringBuilder();
		final double totalCost = cm.totalAssignmentCost( output );
		final int digits = ( int ) ( Math.log10( totalCost ) + 2 );

		str.append( String.format( "Optimal assignment with total cost = %" + digits + ".1f:\n", totalCost ) );
		for ( int i = 0; i < output.length; i++ )
		{
			final int j = output[ i ];
			final double cost = cm.get( i, j, Double.POSITIVE_INFINITY );

			{
				for ( int k = 0; k < ( rowWidth - rowNames[ i ].length() ); k++ )
				{
					str.append( ' ' );
				}
				str.append( rowNames[ i ] );
			}
			str.append( " → " );
			{
				str.append( colNames[ j ] );
				unassignedColNames.remove( colNames[ j ] );
				for ( int k = 0; k < ( colWidth - colNames[ j ].length() ); k++ )
				{
					str.append( ' ' );
				}
			}
			str.append( String.format( " cost = %" + digits + ".1f\n", cost ) );
		}
		if ( cm.nCols > cm.nRows )
		{
			str.append( "Unassigned columns:\n" );
			for ( final String ucn : unassignedColNames )
			{
				{
					for ( int k = 0; k < rowWidth / 2; k++ )
					{
						str.append( ' ' );
					}
					str.append( 'ø' );
					for ( int k = 0; k < rowWidth - rowWidth / 2 - 1; k++ )
					{
						str.append( ' ' );
					}
				}
				str.append( " → " );
				{
					str.append( ucn );
					for ( int k = 0; k < ( colWidth - ucn.length() ); k++ )
					{
						str.append( ' ' );
					}
				}
				str.append( '\n' );
			}
		}

		return str.toString();
	}
}
