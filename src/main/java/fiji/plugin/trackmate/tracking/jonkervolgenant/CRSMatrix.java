package fiji.plugin.trackmate.tracking.jonkervolgenant;

import java.util.Arrays;

public class CRSMatrix
{

	final double[] cc;

	final int[] kk;

	final int[] number;

	final int nRows;

	final int nCols;

	final int cardinality;

	final int[] start;

	public CRSMatrix( final double[] cc, final int[] kk, final int[] number )
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
		final CRSMatrix cm = new CRSMatrix( cc, kk, number );
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
