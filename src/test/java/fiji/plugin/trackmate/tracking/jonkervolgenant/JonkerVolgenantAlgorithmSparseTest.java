package fiji.plugin.trackmate.tracking.jonkervolgenant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import fiji.plugin.trackmate.tracking.hungarian.JonkerVolgenantAlgorithm;

public class JonkerVolgenantAlgorithmSparseTest
{

	private int seed;

	private static final String randomString()
	{
		return Long.toHexString( Double.doubleToLongBits( Math.random() ) );
	}

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

	private LAPSparseStructure< String, String > generateSparseMatrix( final double[][] weights )
	{
		final int n = weights.length;
		final List< String > rows = new ArrayList< String >( n );
		final List< String > cols = new ArrayList< String >( n );
		for ( int i = 0; i < n; i++ )
		{
			rows.add( randomString() );
			cols.add( randomString() );
		}

		final List< String > sources = new ArrayList< String >( n * n );
		final List< String > targets = new ArrayList< String >( n * n );
		final List< Double > costs = new ArrayList< Double >( n * n );
		for ( int i = 0; i < n; i++ )
		{
			final String row = rows.get( i );
			for ( int j = 0; j < n; j++ )
			{
				final String col = cols.get( j );
				sources.add( row );
				targets.add( col );
				costs.add( weights[ i ][ j ] );
			}
		}
		return new LAPSparseStructure< String, String >( sources, targets, costs );
	}

	@Test
	public void speedTest()
	{
		final JonkerVolgenantAlgorithmSparse< String, String > jvs = new JonkerVolgenantAlgorithmSparse< String, String >();
		final JonkerVolgenantAlgorithm jonkerVolgenant = new JonkerVolgenantAlgorithm();
		seed = 17;
		final int nRepeats = 100;
		final int matrixSize = 100;
		final double[][] weights = generateMatrix( matrixSize );
		final LAPSparseStructure< String, String > sparse = generateSparseMatrix( weights );
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
		final int n = 9;
		final double[][] weights = generateMatrix( n );

		// Non sparse
		final JonkerVolgenantAlgorithm jonkerVolgenant = new JonkerVolgenantAlgorithm();
		final int[][] jonkerVolgenantResult = jonkerVolgenant.computeAssignments( weights );

		// Sparse with non-sparse entries
		final JonkerVolgenantAlgorithmSparse< String, String > jvs = new JonkerVolgenantAlgorithmSparse< String, String >();
		final LAPSparseStructure< String, String > CM = generateSparseMatrix( weights );
		final int[][] jvSparseResult = jvs.computeAssignments( CM );

		// Compare
		assertEquals( jvSparseResult.length, jonkerVolgenantResult.length );

		double munkresKuhnCost = 0, jonkerVolgenantCost = 0;
		for ( int i = 0; i < jvSparseResult.length; i++ )
		{
			munkresKuhnCost += weights[ jvSparseResult[ i ][ 0 ] ][ jvSparseResult[ i ][ 1 ] ];
		}
		for ( int i = 0; i < jonkerVolgenantResult.length; i++ )
		{
			jonkerVolgenantCost += weights[ jonkerVolgenantResult[ i ][ 0 ] ][ jonkerVolgenantResult[ i ][ 1 ] ];
		}
		assertEquals( munkresKuhnCost, jonkerVolgenantCost, 1e-5 );
	}
}
