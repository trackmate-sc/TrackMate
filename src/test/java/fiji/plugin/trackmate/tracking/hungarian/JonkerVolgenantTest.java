package fiji.plugin.trackmate.tracking.hungarian;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JonkerVolgenantTest
{

	private int seed;

	private int pseudoRandom()
	{
		return seed = 3170425 * seed + 132102;
	}

	private double pseudoRandom( final double min, final double max )
	{
		int random = pseudoRandom() & 0x7fffffff;
		return min + random * ( ( max - min ) / ( double ) Integer.MAX_VALUE );
	}

	private double[][] generateMatrix( final int n )
	{
		double[][] m = new double[ n ][ n ];
		for ( int j = 0; j < n; j++ )
		{
			for ( int i = 0; i < n; i++ )
			{
				m[ j ][ i ] = Math.floor( pseudoRandom( 1, 100 ) );
			}
		}
		return m;
	}

	@Test
	public void testJonkerVolgenant()
	{
		double[][] weights = generateMatrix( 9 );

		MunkresKuhnAlgorithm munkresKuhn = new MunkresKuhnAlgorithm();
		int[][] munkresKuhnResult = munkresKuhn.computeAssignments( weights );
		JonkerVolgenantAlgorithm jonkerVolgenant = new JonkerVolgenantAlgorithm();
		int[][] jonkerVolgenantResult = jonkerVolgenant.computeAssignments( weights );

		assertEquals( munkresKuhnResult.length, jonkerVolgenantResult.length );

		double munkresKuhnCost = 0, jonkerVolgenantCost = 0;
		for ( int i = 0; i < munkresKuhnResult.length; i++ )
		{
			munkresKuhnCost += weights[ munkresKuhnResult[ i ][ 0 ] ][ munkresKuhnResult[ i ][ 1 ] ];
		}
		for ( int i = 0; i < jonkerVolgenantResult.length; i++ )
		{
			jonkerVolgenantCost += weights[ jonkerVolgenantResult[ i ][ 0 ] ][ jonkerVolgenantResult[ i ][ 1 ] ];
		}
		assertEquals( munkresKuhnCost, jonkerVolgenantCost, 1e-5 );
	}
}
