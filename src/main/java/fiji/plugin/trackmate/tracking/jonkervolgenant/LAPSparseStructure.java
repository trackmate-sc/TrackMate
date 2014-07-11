package fiji.plugin.trackmate.tracking.jonkervolgenant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A data structure that stores a sparse linear assignment problem.
 * <p>
 * This class stores all the possible assignments between a list of sources and
 * a list of targets with a specified cost. It is best to use this class when
 * the assignment is sparse, that is: only a few assignments are considered (all
 * the others are supposed to have infinite costs ergo are not considered).
 * <p>
 * This class is immutable and can be used concurrently.
 * 
 * @author Jean-Yves Tinevez - 2014
 * 
 * @param <K>
 *            the type of the sources
 * @param <L>
 *            the type of the targets
 */
public class LAPSparseStructure< K, L >
{
	private final List< K > rows;

	private final List< L > cols;

	private final Map< K, Integer > sourceToRow;

	private final Map< L, Integer > targetToCol;

	private final Map< Long, Double > costMap;

	private final Double missingVal;

	private int nRows;

	private int nCols;

	/**
	 * Create a new LAP sparse structure, for assignments specified with the 3
	 * given lists. The 3 lists specify respectively for a common index:
	 * <ul>
	 * <li>The source object.</li>
	 * <li>The target object.</li>
	 * <li>The cost of linking these two objects.</li>
	 * </ul>
	 * If present, an assignment must be unique. That is: there must at most one
	 * cost for a specific pair.
	 * 
	 * @param sources
	 *            the list of sources.
	 * @param targets
	 *            the list of targets.
	 * @param costs
	 *            the list of costs.
	 * @param missingVal
	 *            the cost value to return when queried for an unspecified
	 *            assignment.
	 * @throws IllegalArgumentException
	 *             when:
	 *             <ul>
	 *             <li>The 3 lists are not of the same size.</li>
	 *             <li>There is more than one cost for a given pair.</li>
	 *             </ul>
	 */
	public LAPSparseStructure( final List< K > sources, final List< L > targets, final List< Double > costs, final Double missingVal )
	{
		this.missingVal = missingVal;

		if ( sources.size() != targets.size() ) { throw new IllegalArgumentException( "Source and target lists have different sizes." ); }
		if ( sources.size() != costs.size() ) { throw new IllegalArgumentException( "Source and cost lists have different sizes." ); }

		// Extract unique objects in the rows & cols
		this.rows = new ArrayList< K >();
		this.cols = new ArrayList< L >();

		this.sourceToRow = new HashMap< K, Integer >();
		this.targetToCol = new HashMap< L, Integer >();
		this.costMap = new HashMap< Long, Double >( costs.size() );

		 int row = -1;
		 int col = -1;
		for ( int l = 0; l < sources.size(); l++ )
		{

			// Rows
			final K source = sources.get( l );
			int r;
			if ( !rows.contains( source ) )
			{
				rows.add( source );
				row++;
				sourceToRow.put( source, Integer.valueOf( row ) );
				r = row;
			}
			else
			{
				r = sourceToRow.get( source );
			}

			// Columns
			final L target = targets.get( l );
			int c;
			if ( !cols.contains( target ) )
			{
				cols.add( target );
				col++;
				targetToCol.put( target, Integer.valueOf( col ) );
				c = col;
			}
			else
			{
				c = targetToCol.get( target );
			}

			// Mapping to costs
			final Double cost = costs.get( l );
			final long key = JVSUtils.szudzikPair( r, c );
			final Double previous = costMap.put( Long.valueOf( key ), cost );
			if ( null != previous ) { throw new IllegalArgumentException( "There can be only one cost assigned from a source to a target. Source " + source + " and target " + target + " are linked by at least 2 costs." ); }

		}

		this.nRows = rows.size();
		this.nCols = cols.size();
	}

	/**
	 * Create a new LAP sparse structure, for assignments specified with the 3
	 * given lists. The 3 lists specify respectively for a common index:
	 * <ul>
	 * <li>The source object.</li>
	 * <li>The target object.</li>
	 * <li>The cost of linking these two objects.</li>
	 * </ul>
	 * If present, an assignment must be unique. That is: there must at most one
	 * cost for a specific pair. When queried for an unspecified assignment,
	 * this structure will return an infinite cost.
	 * 
	 * @param sources
	 *            the list of sources.
	 * @param targets
	 *            the list of targets.
	 * @param costs
	 *            the list of costs.
	 * @throws IllegalArgumentException
	 *             when:
	 *             <ul>
	 *             <li>The 3 lists are not of the same size.</li>
	 *             <li>There is more than one cost for a given pair.</li>
	 *             </ul>
	 */
	public LAPSparseStructure( final List< K > sources, final List< L > targets, final List< Double > costs )
	{
		this( sources, targets, costs, Double.POSITIVE_INFINITY );
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Returns the row index of the assignment matrix for the specified source
	 * object, or -1 if the LAP does not contain such an object.
	 * 
	 * @param source
	 *            the source object to query.
	 * @return the corresponding row in the assignment matrix, or -1 if the
	 *         object is not found.
	 */
	public int rowForSource( final K source )
	{
		final Integer row = sourceToRow.get( source );
		if ( null == row ) { return -1; }
		return row.intValue();
	}

	/**
	 * Returns the column index of the assignment matrix for the specified
	 * target object, or -1 if the LAP does not contain such an object.
	 * 
	 * @param target
	 *            the target object to query.
	 * @return the corresponding column in the assignment matrix, or -1 if the
	 *         object is not found.
	 */
	public int colForTarget( final L target )
	{
		final Integer col = targetToCol.get( target );
		if ( null == col ) { return -1; }
		return col.intValue();
	}

	/**
	 * Returns an iterator that iterates over the cost values in the specified
	 * row of the assignment matrix. If a given assignment (row, column) is not
	 * specified in this sparse structure, the missing value specified at
	 * construction is returned.
	 * 
	 * @param row
	 *            the row to iterate.
	 * @return a new iterator.
	 */
	public Iterator< Double > rowIterator( final int row )
	{
		final Iterator< Double > it = new Iterator< Double >()
		{
			final int r = row;

			int c = 0;

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException( "Cannot remove from a LAPSparseStructure." );
			}

			@Override
			public Double next()
			{
				final Long key = Long.valueOf( JVSUtils.szudzikPair( r, c ) );
				final Double val = costMap.get( key );
				c++;
				if ( val == null ) { return missingVal; }
				return val;
			}

			@Override
			public boolean hasNext()
			{
				return c < nCols;
			}
		};
		return it;
	}

	/**
	 * Returns an iterator that iterates over the cost values in the specified
	 * column of the assignment matrix. If a given assignment (row, column) is
	 * not specified in this sparse structure, the missing value specified at
	 * construction is returned.
	 * 
	 * @param col
	 *            the column to iterate.
	 * @return a new iterator.
	 */
	public Iterator< Double > colIterator( final int col )
	{
		final Iterator< Double > it = new Iterator< Double >()
		{
			final int c = col;

			int r = 0;

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException( "Cannot remove from a LAPSparseStructure." );
			}

			@Override
			public Double next()
			{
				final Long key = Long.valueOf( JVSUtils.szudzikPair( r, c ) );
				final Double val = costMap.get( key );
				r++;
				if ( val == null ) { return missingVal; }
				return val;
			}

			@Override
			public boolean hasNext()
			{
				return r < nRows;
			}
		};
		return it;
	}

	/**
	 * Returns the cost value at the specified row and column. If such an
	 * assignment is missing from the matrix, the missing value specified at
	 * construction is returned.
	 * 
	 * @param row
	 *            the row.
	 * @param col
	 *            the column.
	 * @return the cost for this assignment.
	 */
	public Double get( final int row, final int col )
	{
		final Long key = Long.valueOf( JVSUtils.szudzikPair( row, col ) );
		final Double val = costMap.get( key );
		if ( val == null ) { return missingVal; }
		return val;
	}

	@Override
	public String toString()
	{
		// Determine row & col names max size
		int rowSizeMax = 0;
		for ( final K row : rows )
		{
			final int size = row.toString().length();
			if ( size > rowSizeMax )
			{
				rowSizeMax = size;
			}
		}

		int colSizeMax = 0;
		for ( final L col : cols )
		{
			final int size = col.toString().length();
			if ( size > colSizeMax )
			{
				colSizeMax = size;
			}
		}

		final StringBuilder str = new StringBuilder();

		// First line
		str.append( super.toString() + "\n" );
		appendNSpaces( str, rowSizeMax + 2 );
		for ( final L col : cols )
		{
			appendNSpaces( str, colSizeMax - col.toString().length() + 2 );
			str.append( col.toString() );
		}
		str.append( '\n' );

		// Other lines
		for ( int r = 0; r < rows.size(); r++ )
		{
			final K row = rows.get( r );
			str.append( row.toString() );
			appendNSpaces( str, rowSizeMax - row.toString().length() + 2 );
			for ( int c = 0; c < cols.size(); c++ )
			{
				final Long key = Long.valueOf( JVSUtils.szudzikPair( r, c ) );
				final Double cost = costMap.get( key );
				String costStr;
				if ( cost != null )
				{
					costStr = String.format( "% " + ( colSizeMax + 2 ) + ".1f", cost );
				}
				else
				{
					costStr = "";
				}
				str.append( costStr );
				appendNSpaces( str, colSizeMax - costStr.length() + 2 );
			}
			str.append( '\n' );
		}
		str.append( "Value when mapping is missing: " + missingVal );

		return str.toString();
	}

	/*
	 * STATIC METHODS
	 */

	private static final void appendNSpaces( final StringBuilder str, final int n )
	{
		for ( int i = 0; i < n; i++ )
		{
			str.append( ' ' );
		}
	}

	/*
	 * MAIN METHOD
	 */

	public static void main( final String[] args )
	{
		final List< String > sources = Arrays.asList( new String[] { "John", "Paul", "Ringo", "George", "Paul", "Paul", "John", "Ringo", "George" } );
		final List< String > targets = Arrays.asList( new String[] { "Guitar", "Bass", "Drums", "Guitar", "Keyboard", "Voice", "Voice", "Voice", "Voice" } );
		final List< Double > costs = Arrays.asList( new Double[] { 1.0, 1.0, 1.0, 1.0, 2.0, 2.0, 1.5, 3.0, 4.0 } );
		final LAPSparseStructure< String, String > sparse = new LAPSparseStructure< String, String >( sources, targets, costs );
		System.out.println( sparse );
	}
}
