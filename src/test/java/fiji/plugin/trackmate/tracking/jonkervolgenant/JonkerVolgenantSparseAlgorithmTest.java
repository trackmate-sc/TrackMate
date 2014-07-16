package fiji.plugin.trackmate.tracking.jonkervolgenant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

public class JonkerVolgenantSparseAlgorithmTest
{

	private int seed;

	private int pseudoRandom()
	{
		return seed = 3170425 * seed + 132102;
	}

	private double pseudoRandom( final double min, final double max )
	{
		final int random = pseudoRandom() & 0x7fffffff;
		return min + random * ( ( max - min ) / Integer.MAX_VALUE );
	}

	private double[][] generateMatrix( final int n )
	{
		final double[][] m = new double[ n ][ n ];
		for ( int j = 0; j < n; j++ )
		{
			for ( int i = 0; i < n; i++ )
			{
				m[ j ][ i ] = Math.floor( pseudoRandom( 1, 100 ) );
			}
		}
		return m;
	}

	private CRSMatrix generateSparseMatrix( final double[][] weights )
	{
		final int n = weights.length;
		final int[] number = new int[ n ];
		final int[] kk = new int[ n * n ];
		final double[] cc = new double[ n * n ];

		int index = 0;
		for ( int i = 0; i < n; i++ )
		{
			number[ i ] = n;
			for ( int j = 0; j < n; j++ )
			{
				kk[ index ] = j;
				cc[ index ] = weights[ i ][ j ];
				index++;
			}
		}
		return new CRSMatrix( cc, kk, number );
	}

	@Test
	public void speedTest()
	{
		JonkerVolgenantSparseAlgorithm jvs;
		final JonkerVolgenantAlgorithm jonkerVolgenant = new JonkerVolgenantAlgorithm();
		seed = new Random().nextInt();
		final int nRepeats = 10;
		final int matrixSize = 400;
		final double[][] weights = generateMatrix( matrixSize );
		final CRSMatrix sparse = generateSparseMatrix( weights );
		final long start1 = System.currentTimeMillis();
		for ( int i = 0; i < nRepeats; i++ )
		{
			jvs = new JonkerVolgenantSparseAlgorithm( sparse );
			jvs.process();
		}
		final long end1 = System.currentTimeMillis();
		final long start2 = System.currentTimeMillis();
		for ( int i = 0; i < nRepeats; i++ )
		{
			jonkerVolgenant.computeAssignments( weights );
		}
		final long end2 = System.currentTimeMillis();
		System.err.println( "Jonker-Volgenant sparse: " + ( end1 - start1 ) + "ms, Jonker-Volgenant non-sparse: " + ( end2 - start2 ) + "ms" );
		assertTrue( end1 - start1 > end2 - start2 );
	}

	@Test
	public final void testSparseIsNonSparse()
	{
		final int n = 50;
		seed = new Random().nextInt();
		final double[][] weights = generateMatrix( n );

		final CRSMatrix CM = generateSparseMatrix( weights );

		// Non sparse
		final JonkerVolgenantAlgorithm jonkerVolgenant = new JonkerVolgenantAlgorithm();
		final int[][] jvResult = jonkerVolgenant.computeAssignments( weights );

		// Sparse with non-sparse entries
		final JonkerVolgenantSparseAlgorithm jvs = new JonkerVolgenantSparseAlgorithm( CM );
		jvs.process();
		final int[] jvSparseResult = jvs.getResult();

		// Compare
		assertEquals( jvSparseResult.length, jvResult.length );

		double jvsSparse = 0, jonkerVolgenantCost = 0;
		for ( int i = 0; i < jvSparseResult.length; i++ )
		{
			jvsSparse += weights[ i ][ jvSparseResult[ i ] ];
		}
		for ( int i = 0; i < jvResult.length; i++ )
		{
			jonkerVolgenantCost += weights[ jvResult[ i ][ 0 ] ][ jvResult[ i ][ 1 ] ];
		}
		assertEquals( jonkerVolgenantCost, jvsSparse, 1e-5 );
	}
}