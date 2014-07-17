package fiji.plugin.trackmate.tracking.jonkervolgenant;

import java.util.Arrays;

/**
 * A class to represent a sparse cost matrix.
 * <p>
 * This class aims at representing in a memory-efficient way a possibly
 * rectangular double matrix used in linear assignment problems (LAP). It is
 * useful when the number of sources (rows of the matrix) and the number of
 * targets (columns of the matrix) are large, but only a fraction of the costs
 * are not infinite (assignment forbidden). In this case, infinite cost can be
 * omitted and it is implicitly understood that missing values of the matrix
 * represent infinite costs. This situation is very common in single-particle
 * tracking for Life-Sciences, and this class is especially designed for these
 * problems.
 * <p>
 * This class does not do much. It just stores the arrays (described below) that
 * represent the matrix. It is the caller responsibility to ensure they are
 * properly arranged and that they represent the desired cost matrix. The arrays
 * are accessible via <code>default</code> visibility.
 * <p>
 * This matrix follow the row compressed storage convention, taken from the
 * Volgenant paper: <code>Volgenant. Linear and semi-assignment problems: A core
 * oriented approach. Computers & Operations Research (1996) vol. 23 (10) pp.
 * 917-932</code>
 * 
 * 
 * 
 * @author Jean-Yves Tinevez - 2014
 */
public class SparseCostMatrix
{

	/**
	 * The linear array of non-infinite costs.
	 */
	final double[] cc;

	/**
	 * The linear array storing the column index of each non infinite-cost.
	 * Column indices are stored adjacently row by row, and for each row, are
	 * stored in ascending order.
	 */
	final int[] kk;

	/**
	 * The array of the number of non-infinite costs for each row.
	 */
	final int[] number;

	/**
	 * The number of rows in the cost matrix.
	 */
	final int nRows;

	/**
	 * The number of columns in the cost matrix.
	 */
	final int nCols;

	/**
	 * The number of non-infinite costs in the matrix.
	 */
	final int cardinality;

	/**
	 * The array of indices in {@link #kk} the column index where a row stats.
	 */
	final int[] start;

	/**
	 * Instantiate a new sparse cost matrix. The caller must provide 3 arrays:
	 * <ol>
	 * <li> <code>cc</code>, the <code>double[]</code> array containing all the
	 * non-infinite costs.
	 * <li> <code>kk</code>, an <code>int[]</code> array of the same length that
	 * <code>cc</code>, and that contains the columns of the cost.
	 * </ol>
	 * These two arrays must be arranged row by row, starting with the first
	 * one. And in each row, the columns must be sorted in increasing order (to
	 * facilitate index search). Also, each row must have at least one
	 * non-infinte cost. If not, an {@link IllegalArgumentException} is thrown.
	 * <ol start="3">
	 * <li> <code>number</code> an <code>int[]</code> array, with one element per
	 * row, that contains the number of non infinite cost for a row.
	 * </ol>
	 * 
	 * @param cc
	 *            the cost array.
	 * @param kk
	 *            the column index of each cost.
	 * @param number
	 *            the number of element for each row.
	 * @throws IllegalArgumentException
	 *             if the cost and column arrays are not of the same size, if
	 *             the column array is not sorted row by row, of if one row has
	 *             0 non-infinite costs.
	 */
	public SparseCostMatrix( final double[] cc, final int[] kk, final int[] number )
	{
		this.cc = cc;
		this.kk = kk;
		this.number = number;

		// Check sizes
		if (cc.length != kk.length) {
 throw new IllegalArgumentException( "Cost and column indices arrays must have the same length. Found " + cc.length + " and " + kk.length + "." );
		}

		this.cardinality = cc.length;
		this.nRows = number.length;
		// loop on each row
		int maxCol = -1;
		this.start = new int[ nRows ];
		start[ 0 ] = 0;
		for ( int i = 1; i < nRows; i++ )
		{
			if ( number[ i ] == 0 ) { throw new IllegalArgumentException( "All the rows must have at least one one cost. Row " + i + " have none." ); }
			start[ i ] = start[ i - 1 ] + number[ i - 1 ];
		}
		for ( int i = 0; i < nRows; i++ )
		{
			// Iterate through each column
			int previousK = -1;
			for ( int j = start[ i ]; j < number[ i ]; j++ )
			{
				final int k = kk[ j ];
				if ( k < previousK ) { throw new IllegalArgumentException( "The column indices array must be sorted within each row. The column elements at line " + i + " are not properly sorted." ); }
				previousK = k;
				if ( k > maxCol )
				{
					maxCol = k;
				}
			}
		}
		this.nCols = maxCol + 1;
	}

	@Override
	public String toString()
	{
		final StringBuilder str = new StringBuilder();
		str.append( super.toString() + '\n' );
		str.append( "  " + nRows + " × " + nCols + " matrix with " + cardinality + " non-null elements.\n" );

		str.append( "      |" );
		for ( int c = 0; c < nCols; c++ )
		{
			str.append( String.format( "%7d", c ) );
		}
		str.append( '\n' );

		str.append( "______|" );
		final char[] line = new char[ 7 * nCols ];
		Arrays.fill( line, '_' );
		str.append( line );
		str.append( '\n' );

		for ( int r = 0; r < nRows; r++ )
		{
			str.append( String.format( "%5d |", r ) );

			final StringBuilder rowStr = new StringBuilder();
			final char[] spaces = new char[ 7 * nCols ];
			Arrays.fill( spaces, ' ' );
			rowStr.append( spaces );

			for ( int k = start[ r ]; k < start[ r ] + number[ r ]; k++ )
			{
				final int col = kk[ k ];
				final double cost = cc[ k ];
				rowStr.replace( col * 7, ( col + 1 ) * 7, String.format( "% 7.1f", cost ) );
			}
			rowStr.append( '\n' );
			str.append( rowStr.toString() );
		}

		return str.toString();
	}

	/**
	 * Computes the total cost for an assignment specified by row. It is
	 * supposed that row <code>i</code> is assigned to column
	 * <code>rowAssignment[i]</code>.
	 * 
	 * @param rowAssignment
	 *            the assignment, specified by row.
	 * @return the total cost for this assignment.
	 */
	public double totalAssignmentCost( final int[] rowAssignment )
	{
		double sum = 0;
		for ( int i = 0; i < rowAssignment.length; i++ )
		{
			final int j = rowAssignment[ i ];
			final int kj = Arrays.binarySearch( kk, start[ i ], start[ i ] + number[ i ], j );
			sum += cc[ kj ];
		}
		return sum;
	}

	/**
	 * Creates and returns a new <code>double[][]</code> matrix representing a
	 * non-sparse version of this cost matrix. Missing costs are replace by
	 * {@link Double#MAX_VALUE}.
	 * 
	 * @return a new <code>double[][]</code>
	 */
	public double[][] toFullMatrix()
	{
		final double[][] cm = new double[ nRows ][ nCols ];
		for ( final double[] ds : cm )
		{
			Arrays.fill( ds, Double.MAX_VALUE );
		}

		for ( int r = 0; r < nRows; r++ )
		{
			for ( int k = start[ r ]; k < start[ r ] + number[ r ]; k++ )
			{
				final int c = kk[ k ];
				final double cost = cc[ k ];
				cm[ r ][ c ] = cost;
			}
		}

		return cm;
	}

	public static void main( final String[] args )
	{
		final int[] kk = new int[] { 0, 1, 2, 3, 4, 5, 0, 2, 3, 4, 0, 1, 2, 3, 0, 1, 2, 0, 1, 0, 2, 3, 5 };
		final double[] cc = new double[] { 20.1, 19.2, 18.3, 17.4, 16.5, 15.6, 14.1, 12.8, 11.9, 10.7, 9.2, 8.3, 7.4, 6.5, 5.8, 4.7, 3.6, 2.9, 1.1, 10.2, 1.3, 2.4, 10.2 };
		final int[] number = new int[] { 6, 4, 4, 3, 2, 4 };
		final SparseCostMatrix cm = new SparseCostMatrix( cc, kk, number );
		System.out.println( cm.toString() );

		final double[][] ds = cm.toFullMatrix();
		for ( final double[] ds2 : ds )
		{
			for ( final double d : ds2 )
			{
				System.out.print( String.format( "% 12.3g", d ) );
			}
			System.out.println();
		}

	}

}
