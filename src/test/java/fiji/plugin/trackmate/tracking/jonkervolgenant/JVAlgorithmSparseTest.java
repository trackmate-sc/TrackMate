package fiji.plugin.trackmate.tracking.jonkervolgenant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class JVAlgorithmSparseTest
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

	private SparseCostMatrix generateSparseMatrix( final double[][] weights )
	{
		final int n = weights.length;
		final int[] sources = new int[ n * n ];
		final int[] targets = new int[ n * n ];
		final double[] costs = new double[ n * n ];

		int index = 0;
		for ( int i = 0; i < n; i++ )
		{
			for ( int j = 0; j < n; j++ )
			{
				sources[ index ] = i;
				targets[ index ] = j;
				costs[ index ] = weights[ i ][ j ];
				index++;
			}
		}
		return new SparseCostMatrix( sources, targets, costs );
	}

	public void speedTest()
	{
		final JVAlgorithmSparse jvs = new JVAlgorithmSparse();
		final JVAlgorithm jonkerVolgenant = new JVAlgorithm();
		seed = 17;
		final int nRepeats = 100;
		final int matrixSize = 100;
		final double[][] weights = generateMatrix( matrixSize );
		final SparseCostMatrix sparse = generateSparseMatrix( weights );
		final long start1 = System.currentTimeMillis();
		for ( int i = 0; i < nRepeats; i++ )
		{
			jvs.computeAssignments( sparse );
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
		final int n = 10;
		seed = 17;
		final double[][] weights = generateMatrix( n );
		final SparseCostMatrix CM = generateSparseMatrix( weights );

		System.out.println( CM.toString() ); // DEBUG

		// Non sparse
		System.out.println( "\n\n------------\nNON SPARSE\n--------------\n\n" );
		final JVAlgorithm jonkerVolgenant = new JVAlgorithm();
		final int[][] jvResult = jonkerVolgenant.computeAssignments( weights );

		// Sparse with non-sparse entries
		System.out.println( "\n\n-------\nSPARSE\n-------\n\n" );
		final JVAlgorithmSparse jvs = new JVAlgorithmSparse();
		final int[][] jvSparseResult = jvs.computeAssignments( CM );

		// Compare
		assertEquals( jvSparseResult.length, jvResult.length );

		double jvsSparse = 0, jonkerVolgenantCost = 0;
		for ( int i = 0; i < jvSparseResult.length; i++ )
		{
			jvsSparse += weights[ jvSparseResult[ i ][ 0 ] ][ jvSparseResult[ i ][ 1 ] ];
		}
		for ( int i = 0; i < jvResult.length; i++ )
		{
			jonkerVolgenantCost += weights[ jvResult[ i ][ 0 ] ][ jvResult[ i ][ 1 ] ];
		}
		assertEquals( jonkerVolgenantCost, jvsSparse, 1e-5 );
	}
}
