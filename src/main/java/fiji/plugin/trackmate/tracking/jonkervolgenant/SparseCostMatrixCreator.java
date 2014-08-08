package fiji.plugin.trackmate.tracking.jonkervolgenant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * A utility class that creates a {@link SparseCostMatrix} from a list of row,
 * column, and cost values.
 * 
 * @author Jean-Yves Tinevez - 2014
 * 
 * @param <K>
 */
public class SparseCostMatrixCreator< K extends Comparable< K >, J extends Comparable< J > >
{

	private final SparseCostMatrix scm;

	private final ArrayList< K > uniqueRows;

	private final ArrayList< J > uniqueCols;

	/**
	 * Processes a list of possible assignments and generates a new
	 * {@link SparseCostMatrix} representing the LAP.
	 * 
	 * @param rows
	 *            the rows of the possible assignment in the cost matrix.
	 * @param cols
	 *            the columns of the possible assignment in the cost matrix.
	 * @param costs
	 *            the costs.
	 * @throws IllegalArgumentException
	 *             if:
	 *             <ul>
	 *             <li>the row list is null or empty.
	 *             <li>the row list, column list and cost array do not have all
	 *             the same size.
	 *             <li>there are duplicates assignment (same row and column).
	 *             </ul>
	 */
	public SparseCostMatrixCreator( final List< K > rows, final List< J > cols, final double[] costs )
	{
		if ( rows == null || rows.isEmpty() ) { throw new IllegalArgumentException( "The row list is null or empty." ); }
		if ( rows.size() != cols.size() ) { throw new IllegalArgumentException( "Row and column lists do not have the same number of elements. Found " + rows.size() + " and " + cols.size() + "." ); }
		if ( rows.size() != costs.length ) { throw new IllegalArgumentException( "Row list and cost array do not have the same number of elements. Found " + rows.size() + " and " + costs.length + "." ); }

		uniqueRows = new ArrayList< K >( new HashSet< K >( rows ) );
		Collections.sort( uniqueRows );
		uniqueCols = new ArrayList< J >( new HashSet< J >( cols ) );
		Collections.sort( uniqueCols );
		
		final List< Assignment > assignments = new ArrayList< Assignment >( costs.length );
		for ( int i = 0; i < costs.length; i++ )
		{
			final K rowObj = rows.get( i );
			final J colObj = cols.get( i );
			final int r = Collections.binarySearch( uniqueRows, rowObj );
			final int c = Collections.binarySearch( uniqueCols, colObj );
			assignments.add( new Assignment( r, c, costs[ i ] ) );
		}
		Collections.sort( assignments );

		// Test we do not have duplicates.
		Assignment previousAssgn = assignments.get( 0 );
		for ( int i = 1; i < assignments.size(); i++ )
		{
			final Assignment assgn = assignments.get( i );
			if ( assgn.equals( previousAssgn ) ) { throw new IllegalArgumentException( "Found duplicate assignment at index: " + assgn + "." ); }
			previousAssgn = assgn;
		}

		final int nRows = uniqueRows.size();
		final int nCols = uniqueCols.size();
		final int[] kk = new int[ costs.length ];
		final int[] number = new int[ nRows ];
		final double[] cc = new double[ costs.length ];

		Assignment a = assignments.get( 0 );
		kk[ 0 ] = a.c;
		cc[ 0 ] = a.cost;
		int currentRow = a.r;
		int nOfEl = 0;
		for ( int i = 1; i < assignments.size(); i++ )
		{
			a = assignments.get( i );

			kk[ i ] = a.c;
			cc[ i ] = a.cost;
			nOfEl++;

			if ( a.r != currentRow )
			{
				number[ currentRow ] = nOfEl;
				nOfEl = 0;
				currentRow = a.r;
			}
		}
		number[ currentRow ] = nOfEl + 1;

		scm = new SparseCostMatrix( cc, kk, number, nCols );
	}
	
	/**
	 * The objects as they appear in the rows of the sparse cost matrix.
	 * 
	 * @return a new List.
	 * @see #getCostMatrix()
	 * @see #getMatrixCols()
	 */
	public List< K > getMatrixRows()
	{
		return uniqueRows;
	}

	/**
	 * The objects as they appear in the columns of the sparse cost matrix.
	 * 
	 * @return a new List.
	 * @see #getCostMatrix()
	 * @see #getMatrixRows()
	 */
	public List< J > getMatrixCols()
	{
		return uniqueCols;
	}
	
	/**
	 * Returns the generate {@link SparseCostMatrix}.
	 * 
	 * @return a new sparse cost matrix.
	 * @see #getMatrixRows()
	 * @see #getMatrixCols()
	 */
	public SparseCostMatrix getCostMatrix()
	{
		return scm;
	}

	private final static class Assignment implements Comparable< Assignment >
	{
		private final int r;

		private final int c;

		private final double cost;

		private Assignment( final int r, final int c, final double cost )
		{
			this.r = r;
			this.c = c;
			this.cost = cost;
		}

		@Override
		public int compareTo( final Assignment o )
		{
			if ( r == o.r ) { return c - o.c; }
			return r - o.r;
		}

		@Override
		public boolean equals( final Object obj )
		{
			if ( obj instanceof Assignment )
			{
				final Assignment o = ( Assignment ) obj;
				return r == o.r && c == o.c;
			}
			return false;
		}

		@Override
		public int hashCode()
		{
			int hash = 23;
			hash = hash * 31 + r;
			hash = hash * 31 + c;
			return hash;
		}

		@Override
		public String toString()
		{
			return "Assignment r = " + r + ", c = " + c + ", cost = " + cost;
		}
	}

	public static void main( final String[] args )
	{
		final List< String > names = Arrays.asList( new String[] { "Florian", "Jean-Yves", "Spencer", "Pascal", "Audrey", "Nathalie", "Anne" } );
		final List< String > activities = Arrays.asList( new String[] { "MEMI-OP", "Leading", "Confocals", "Spinning-disks", "Image analysis", "HCA", "Biphoton", "Administration", "Grant writing" } );

		final int nAssgn = 30;

		final long seed = new Random().nextLong();
		final Random ran = new Random(seed );
		System.out.println( "Random seed used: " + seed );
		// Ensure we do not have duplicate assignments.
		final HashSet< Assignment > assgns = new HashSet< Assignment >();
		for ( int i = 0; i < nAssgn; i++ )
		{
			final int row = ran.nextInt( names.size() );
			final int col = ran.nextInt( activities.size() );
			final double cost = ran.nextInt( 100 );
			assgns.add( new Assignment( row, col, cost ) );
		}

		final List< String > rows = new ArrayList< String >( assgns.size() );
		final List< String > cols = new ArrayList< String >( assgns.size() );
		final double[] costs = new double[ assgns.size() ];
		final Iterator< Assignment > it = assgns.iterator();
		for ( int i = 0; i < assgns.size(); i++ )
		{
			final Assignment next = it.next();
			rows.add( names.get( next.r ) );
			cols.add( activities.get( next.c ) );
			costs[ i ] = next.cost;
		}

		final SparseCostMatrixCreator< String, String > creator = new SparseCostMatrixCreator< String, String >( rows, cols, costs );
		final SparseCostMatrix costMatrix = creator.getCostMatrix();
		final List< String > matrixCols = creator.getMatrixCols();
		final List< String > matrixRows = creator.getMatrixRows();
		System.out.println( costMatrix.toString( matrixRows, matrixCols ) );

		final JonkerVolgenantSparseAlgorithm solver = new JonkerVolgenantSparseAlgorithm( costMatrix );
		if ( !solver.checkInput() || !solver.process() )
		{
			System.err.println( solver.getErrorMessage() );
			return;
		}

		solver.getResult();
		System.out.println( solver.resultToString( matrixRows, matrixCols ) );
	}

}
