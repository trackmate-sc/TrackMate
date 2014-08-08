package fiji.plugin.trackmate.tracking.sparselap.costmatrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import fiji.plugin.trackmate.tracking.sparselap.jonkervolgenant.SparseCostMatrix;
import fiji.plugin.trackmate.tracking.sparselap.linker.JaqamanLinker;

/**
 * A {@link CostMatrixCreator} that build a cost matrix from 3 lists containing
 * the sources, the targets and the associated costs.
 * 
 * @author Jean-Yves Tinevez - 2014
 * 
 * @param <K>
 */
public class DefaultCostMatrixCreator< K extends Comparable< K >, J extends Comparable< J > > implements CostMatrixCreator< K, J >
{

	private static final String BASE_ERROR_MESSAGE = "[DefaultCostMatrixCreator] ";

	private SparseCostMatrix scm;

	private ArrayList< K > uniqueRows;

	private ArrayList< J > uniqueCols;

	private long processingTime;

	private String errorMessage;

	private final double alternativeCost;

	private final List< K > rows;

	private final List< J > cols;

	private final double[] costs;

	public DefaultCostMatrixCreator( final List< K > rows, final List< J > cols, final double[] costs, final double alternativeCost )
	{
		this.rows = rows;
		this.cols = cols;
		this.costs = costs;
		this.alternativeCost = alternativeCost;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public SparseCostMatrix getResult()
	{
		return scm;
	}

	@Override
	public boolean checkInput()
	{
		if ( rows == null || rows.isEmpty() )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The row list is null or empty.";
			return false;
		}
		if ( rows.size() != cols.size() ) { 
			errorMessage = BASE_ERROR_MESSAGE +"Row and column lists do not have the same number of elements. Found " + rows.size() + " and " + cols.size() + "." ;
			return false;
		}
		if ( rows.size() != costs.length )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Row list and cost array do not have the same number of elements. Found " + rows.size() + " and " + costs.length + ".";
			return false;
		}
		return true;
	}

	@Override
	public boolean process()
	{
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
			if ( assgn.equals( previousAssgn ) )
			{
				errorMessage = BASE_ERROR_MESSAGE + "Found duplicate assignment at index: " + assgn + ".";
				return false;
			}
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
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public List< K > getSourceList()
	{
		return uniqueRows;
	}

	@Override
	public List< J > getTargetList()
	{
		return uniqueCols;
	}

	@Override
	public double getAlternativeCost()
	{
		return alternativeCost;
	}


	public final static class Assignment implements Comparable< Assignment >
	{
		private final int r;

		private final int c;

		private final double cost;

		public Assignment( final int r, final int c, final double cost )
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

		final DefaultCostMatrixCreator< String, String > creator = new DefaultCostMatrixCreator< String, String >( rows, cols, costs, 80 );

		final JaqamanLinker< String, String > solver = new JaqamanLinker< String, String >( creator );
		if ( !solver.checkInput() || !solver.process() )
		{
			System.err.println( solver.getErrorMessage() );
			return;
		}

		solver.getResult();
		System.out.println( solver.resultToString() );
	}

}
