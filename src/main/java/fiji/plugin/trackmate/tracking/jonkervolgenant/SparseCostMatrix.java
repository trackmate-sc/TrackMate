package fiji.plugin.trackmate.tracking.jonkervolgenant;

import java.util.Arrays;

public class SparseCostMatrix
{

	final int[] sources;

	final int[] targets;

	final double[] costs;

	final int[][] rows;

	final int[][] cols;

	final int nRows;

	final int nCols;

	/**
	 * i & j: integer indices, ranging from 0 to maxI, maxJ.
	 * 
	 * @param sources
	 * @param targets
	 * @param costs
	 */
	public SparseCostMatrix( final int[] sources, final int[] targets, final double[] costs )
	{
		this.sources = sources;
		this.targets = targets;
		this.costs = costs;

		if ( sources.length != targets.length ) { throw new IllegalArgumentException( "Source and target arrays have different sizes." ); }
		if ( sources.length != costs.length ) { throw new IllegalArgumentException( "Source and cost arrays have different sizes." ); }

		/*
		 * Get max I & J
		 */

		int maxI = sources[ 0 ];
		int maxJ = targets[ 0 ];
		for ( int k = 1; k < sources.length; k++ )
		{
			if ( maxI < sources[ k ] )
			{
				maxI = sources[ k ];
			}
			if ( maxJ < targets[ k ] )
			{
				maxJ = targets[ k ];
			}
		}
		this.nRows = maxI + 1;
		this.nCols = maxJ + 1;

		/*
		 * Build bin count
		 */

		final int[] histoI = new int[ nRows ];
		for ( final int ival : sources )
		{
			histoI[ ival ]++;
		}

		final int[] histoJ = new int[ nCols ];
		for ( final int jval : targets )
		{
			histoJ[ jval ]++;
		}

		/*
		 * Build row content
		 */

		this.rows = new int[ nRows ][];

		for ( int kr = 0; kr < nRows; kr++ )
		{
			rows[ kr ] = new int[ histoI[ kr ] ];
		}

		this.cols = new int[ nCols ][];
		for ( int kc = 0; kc < nCols; kc++ )
		{
			cols[ kc ] = new int[ histoJ[ kc ] ];
		}

		final int[] rowsIndices = new int[ nRows ];
		final int[] colsIndices = new int[ nCols ];
		for ( int k = 0; k < sources.length; k++ )
		{
			final int row = sources[ k ];
			rows[ row ][ rowsIndices[ row ]++ ] = k;
			final int col = targets[ k ];
			cols[ col ][ colsIndices[ col ]++ ] = k;
		}
	}

	public double[][] toFullMatrix()
	{
		final double[][] cm = new double[ nRows ][ nCols ];
		for ( final double[] ds : cm )
		{
			Arrays.fill( ds, Double.MAX_VALUE );
		}

		for ( int i = 0; i < rows.length; i++ )
		{
			final int[] row = rows[i];
			for ( final int kj : row )
			{
				final int j = targets[kj];
				final double c = costs[kj];
				cm[ i ][ j ] = c;
			}
		}

		return cm;
	}

	@Override
	public String toString()
	{
		final StringBuilder str = new StringBuilder();
		str.append( super.toString() + '\n' );
		str.append( "  " + rows.length + " × " + cols.length + " matrix with " + sources.length + " non-null elements.\n" );
		str.append( "      |" );
		for ( int c = 0; c < cols.length; c++ )
		{
			str.append( String.format( "%7d", c ) );
		}
		str.append( '\n' );

		str.append( "______|" );
		final char[] line = new char[ 7 * nCols ];
		Arrays.fill( line, '_' );
		str.append( line );
		str.append( '\n' );

		for ( int r = 0; r < rows.length; r++ )
		{
			str.append( String.format( "%5d |", r ) );

			final StringBuilder rowStr = new StringBuilder();
			final char[] spaces = new char[ 7 * nCols ];
			Arrays.fill( spaces, ' ' );
			rowStr.append( spaces );

			final int[] row = rows[ r ];
			for ( final int k : row )
			{
				final int col = targets[ k ];
				final double cost = costs[ k ];
				rowStr.replace( col * 7, ( col + 1 ) * 7, String.format( "% 7.1f", cost ) );
			}
			rowStr.append( '\n' );
			str.append( rowStr.toString() );
		}
		return str.toString();
	}

	/*
	 * MAIN METHOD
	 */

	public static void main( final String[] args )
	{
		final int[] i = new int[] { 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 4, 4, 5, 5, 5, 5, 4, 4 };
		final int[] j = new int[] { 0, 1, 2, 3, 4, 5, 0, 2, 3, 4, 0, 1, 2, 3, 0, 1, 2, 0, 1, 0, 2, 3, 5, 4, 5 };
		final double[] c = new double[] { 20.1, 19.2, 18.3, 17.4, 16.5, 15.6, 14.1, 12.8, 11.9, 10.7, 9.2, 8.3, 7.4, 6.5, 5.8, 4.7, 3.6, 2.9, 1.1, 10.2, 1.3, 2.4, 10.2, 6.5, 7.2 };
		final SparseCostMatrix sm = new SparseCostMatrix( i, j, c );

		System.out.println( sm.toString() );
		System.out.println();

		final double[][] ds = sm.toFullMatrix();
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
