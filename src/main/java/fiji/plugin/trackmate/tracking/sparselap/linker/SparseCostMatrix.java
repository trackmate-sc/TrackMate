package fiji.plugin.trackmate.tracking.sparselap.linker;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
 * oriented approach. Computers &amp; Operations Research (1996) vol. 23 (10) pp.
 * 917-932</code>
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
	public SparseCostMatrix( final double[] cc, final int[] kk, final int[] number, final int nCols )
	{
		this.cc = cc;
		this.kk = kk;
		this.number = number;
		this.nCols = nCols;

		// Check sizes
		if (cc.length != kk.length) {
 throw new IllegalArgumentException( "Cost and column indices arrays must have the same length. Found " + cc.length + " and " + kk.length + "." );
		}

		this.cardinality = cc.length;
		this.nRows = number.length;
		// loop on each row
		this.start = new int[ nRows ];
		if ( nRows > 0 )
		{
			start[ 0 ] = 0;
		}
		for ( int i = 1; i < nRows; i++ )
		{
			if ( number[ i ] == 0 ) { throw new IllegalArgumentException( "All the rows must have at least one cost. Row " + i + " have none." ); }
			start[ i ] = start[ i - 1 ] + number[ i - 1 ];
		}

		final int[] colHistogram = new int[ nCols ];
		for ( int i = 0; i < nRows; i++ )
		{
			// Iterate through each column
			int previousK = -1;
			for ( int j = start[ i ]; j < start[ i ] + number[ i ]; j++ )
			{
				final int k = kk[ j ];
				if ( k >= nCols ) { throw new IllegalArgumentException( "At line " + i + ", the column indices array contains a column index (" + k + ") that is larger than or equal to the declared number of column (" + nCols + ")." ); }
				colHistogram[ k ]++;
				if ( k <= previousK ) { throw new IllegalArgumentException( "The column indices array must be sorted within each row. The column elements at line " + i + " are not properly sorted." ); }
				previousK = k;
			}
		}

		// Check that each column have at least one assignment
		for ( int j = 0; j < colHistogram.length; j++ )
		{
			if ( colHistogram[ j ] == 0 ) { throw new IllegalArgumentException( "All the columns must have at least one cost. The column " + j + " has none." ); }
		}
	}

	@Override
	public String toString()
	{
		return toString( Collections.EMPTY_LIST, Collections.EMPTY_LIST );
	}

	public String toString( final List< ? > rows, final List< ? > columns )
	{
		final String[] colNames = new String[ nCols ];
		// default names
		for ( int j = 0; j < colNames.length; j++ )
		{
			colNames[ j ] = "" + j;
		}
		final String[] rowNames = new String[ nRows ];
		for ( int i = 0; i < rowNames.length; i++ )
		{
			rowNames[ i ] = "" + i;
		}

		for ( int j = 0; j < columns.size(); j++ )
		{
			final Object col = columns.get( j );
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
		str.append( super.toString() + '\n' );
		str.append( "  " + nRows + " × " + nCols + " matrix with " + cardinality + " non-null elements. " );
		str.append( String.format( "Density = %.2f%%.\n", ( double ) cardinality / ( nRows * nCols ) * 100d ) );

		for ( int i = 0; i < rowWidth; i++ )
		{
			str.append( ' ' );
		}
		str.append( '|' );
		for ( int c = 0; c < nCols; c++ )
		{
			for ( int i = 0; i < ( colWidth - colNames[ c ].length() ); i++ )
			{
				str.append( ' ' );
			}
			str.append( colNames[ c ] );
		}
		str.append( '\n' );

		for ( int i = 0; i < rowWidth; i++ )
		{
			str.append( '_' );
		}
		str.append( '|' );
		final char[] line = new char[ colWidth * nCols ];
		Arrays.fill( line, '_' );
		str.append( line );
		str.append( '\n' );

		for ( int r = 0; r < nRows; r++ )
		{
			str.append( rowNames[ r ] );
			for ( int i = 0; i < rowWidth - rowNames[ r ].length(); i++ )
			{
				str.append( ' ' );
			}
			str.append( '|' );

			final StringBuilder rowStr = new StringBuilder();
			final char[] spaces = new char[ colWidth * nCols ];
			Arrays.fill( spaces, ' ' );
			rowStr.append( spaces );

			for ( int k = start[ r ]; k < start[ r ] + number[ r ]; k++ )
			{
				final int col = kk[ k ];
				final double cost = cc[ k ];
				rowStr.replace( col * colWidth, ( col + 1 ) * colWidth, String.format( "% " + colWidth + ".1f", cost ) );
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
	
	/**
	 * Returns the value stored by this matrix at the specified row and column.
	 * If a value is not present in the sparse matrix, the specified missing
	 * value is returned.
	 * 
	 * @param i
	 *            the row.
	 * @param j
	 *            the column.
	 * @param missingValue
	 *            what to return if the sparse matrix does not store a value at
	 *            the specified row and column.
	 * @return the value.
	 */
	public final double get( final int i, final int j, final double missingValue )
	{
		final int k = Arrays.binarySearch( kk, start[ i ], start[ i ] + number[ i ], j );
		if ( k < 0 )
			return missingValue;

		return cc[ k ];
	}

	/**
	 * Exposes the array of all the non-infinite costs.
	 * 
	 * @return the costs.
	 */
	public double[] getCosts()
	{
		return cc;
	}

	public int getNCols()
	{
		return nCols;
	}

	public int getNRows()
	{
		return nRows;
	}

	/**
	 * Returns the vertical concatenation of this matrix with the specified one.
	 * So that if this matrix is A and the specified matrix is B, you get
	 * 
	 * <pre>
	 * -----
	 * | A |
	 * | B |
	 * -----
	 * </pre>
	 * 
	 * @param B
	 *            the matrix to concatenate this matrix with
	 * @return a new sparse matrix.
	 * @throws IllegalArgumentException
	 *             if B does not have the same number of columns as this matrix.
	 */
	public final SparseCostMatrix vcat( final SparseCostMatrix B )
	{
		if ( nCols != B.nCols ) { throw new IllegalArgumentException( "Matrices A & B do not have the same number of columns. Found " + nCols + " and " + B.nCols + " respectively." ); }

		final double[] cc2 = new double[ cardinality + B.cardinality ];
		final int[] kk2 = new int[ cardinality + B.cardinality ];
		final int[] number2 = new int[ nRows + B.nRows ];

		// Append A
		System.arraycopy( kk, 0, kk2, 0, cardinality );
		System.arraycopy( cc, 0, cc2, 0, cardinality );
		System.arraycopy( number, 0, number2, 0, nRows );

		// Append B
		System.arraycopy( B.kk, 0, kk2, cardinality, B.cardinality );
		System.arraycopy( B.cc, 0, cc2, cardinality, B.cardinality );
		System.arraycopy( B.number, 0, number2, nRows, B.nRows );

		return new SparseCostMatrix( cc2, kk2, number2, nCols );
	}

	/**
	 * Returns the horizontal concatenation of this matrix with the specified
	 * one. So that if this matrix is A and the specified matrix is B, you get
	 * 
	 * <pre>
	 * -------
	 * | A B |
	 * -------
	 * </pre>
	 * 
	 * @param B
	 *            the matrix to concatenate this matrix with
	 * @return a new sparse matrix.
	 * @throws IllegalArgumentException
	 *             if B does not have the same number of rows as this matrix.
	 */
	public final SparseCostMatrix hcat( final SparseCostMatrix B )
	{
		if ( nRows != B.nRows ) { throw new IllegalArgumentException( "Matrices A & B do not have the same number of rows. Found " + nRows + " and " + B.nRows + " respectively." ); }

		final double[] cc2 = new double[ cardinality + B.cardinality ];
		final int[] kk2 = new int[ cardinality + B.cardinality ];
		final int[] number2 = new int[ nRows ];

		// Append line by line
		int Aindex = 0;
		int Bindex = 0;
		int Cindex = 0;
		for ( int i = 0; i < nRows; i++ )
		{
			// A
			System.arraycopy( cc, Aindex, cc2, Cindex, number[ i ] );
			System.arraycopy( kk, Aindex, kk2, Cindex, number[ i ] );
			Aindex += number[ i ];
			Cindex += number[ i ];

			// B
			System.arraycopy( B.cc, Bindex, cc2, Cindex, B.number[ i ] );
			// For the columns, we need to increment them by A.nCols
			for ( int j = 0; j < B.number[ i ]; j++ )
			{
				kk2[ Cindex + j ] = B.kk[ Bindex + j ] + nCols;
			}
			Bindex += B.number[ i ];
			Cindex += B.number[ i ];

			// number
			number2[ i ] = number[ i ] + B.number[ i ];
		}

		return new SparseCostMatrix( cc2, kk2, number2, nCols + B.nCols );
	}

	/**
	 * Returns the transpose of this matrix.
	 * 
	 * @return a new sparse matrix.
	 */
	public final SparseCostMatrix transpose()
	{
		// Build column histogram, which will give the transposed number
		final int[] number2 = new int[ nCols ];
		for ( final int j : kk )
		{
			number2[ j ]++;
		}

		// Prepare column & cost storage.
		final int[][] cols = new int[ nCols ][];
		final double[][] costs = new double[ nCols ][];
		for ( int j = 0; j < cols.length; j++ )
		{
			cols[ j ] = new int[ number2[ j ] ];
			costs[ j ] = new double[ number2[ j ] ];
		}

		// Parse source column array and store at what line they happen. Add to
		// cost arrays.
		int currentLine = 0;
		int previousJ = -1;
		int walked = 0;
		final int[] colIndex = new int[ nCols ];
		for ( int k = 0; k < cardinality; k++ )
		{
			final int j = kk[ k ];
			final double c = cc[ k ];
			
			// Determine whether we changed line.
			if ( j <= previousJ || walked >= number[ currentLine ] )
			{
				currentLine++;
				walked = 0;
			}
			walked++;
			previousJ = j;

			cols[ j ][ colIndex[ j ] ] = currentLine;
			costs[ j ][ colIndex[ j ] ] = c;

			colIndex[ j ]++;
		}

		// Concatenate
		final double[] cc2 = new double[ cardinality ];
		final int[] kk2 = new int[ cardinality ];
		int index = 0;
		for ( int i = 0; i < cols.length; i++ )
		{
			System.arraycopy( cols[ i ], 0, kk2, index, number2[ i ] );
			System.arraycopy( costs[ i ], 0, cc2, index, number2[ i ] );
			index += number2[ i ];
		}
		return new SparseCostMatrix( cc2, kk2, number2, nRows );
	}

	/**
	 * Replace all the non-infinite values of this matrix by the specified
	 * value.
	 * 
	 * @param value
	 *            the value to write in this matrix.
	 */
	public void fillWith( final double value )
	{
		Arrays.fill( cc, value );
	}
}
