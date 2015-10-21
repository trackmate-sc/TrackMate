package fiji.plugin.trackmate.tracking.sparselap.linker;

import static org.junit.Assert.assertEquals;
import fiji.plugin.trackmate.tracking.oldlap.hungarian.JonkerVolgenantAlgorithm;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

public class LAPJVTest
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

	private int[] generateIntLogSpaced( final int start, final int end, final int n )
	{
		final int[] result = new int[ n ];
		result[ 0 ] = start;
		int index = 1;
		double ratio = Math.pow( ( double ) end / start, ( 1.0d / ( n - index ) ) );
		while ( index < n )
		{
			final double next_value = result[ index - 1 ] * ratio;
			if ( next_value - result[ index - 1 ] >= 1 )
			{
				result[ index ] = ( int ) Math.floor( next_value );
			}
			else
			{
				result[ index ] = result[ index - 1 ] + 1;
				ratio = Math.pow( ( double ) end / start, ( 1.0d / ( n - index ) ) );
			}
			index++;
		}
		return result;
	}

	private SparseCostMatrix generateSparseMatrix( final double[][] weights )
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
		return new SparseCostMatrix( cc, kk, number, n );
	}
	
	private int[] uniqueAndSort(final int[] arr)
	{
		final int[] result = arr.clone();
		Arrays.sort(result);
		int j = 0;
		for (int i = 1; i < arr.length; i++)
		{
			if ( result[ j ] != result[ i ] )
			{
				result[++j] = result[i];
			}
		}
		return Arrays.copyOf( result, ++j );
	}

	@Test
	public final void testSparseIsNonSparse()
	{
		final int n = 50;
		seed = new Random().nextInt();
		final double[][] weights = generateMatrix( n );

		final SparseCostMatrix CM = generateSparseMatrix( weights );

		// Non sparse
		final JonkerVolgenantAlgorithm jonkerVolgenant = new JonkerVolgenantAlgorithm();
		final int[][] jvResult = jonkerVolgenant.computeAssignments( weights );

		// Sparse with non-sparse entries
		final LAPJV jvs = new LAPJV( CM );
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

	@Test
	public final void testVayringDensity()
	{
		final int size = 100;
		final Random ran = new Random();
		seed = ran.nextInt();

		final int[] cardinalities = generateIntLogSpaced( 4 * size, size * size, 10 );

		for ( int ci = 0; ci < cardinalities.length; ci++ )
		{
			final int card = cardinalities[ ci ];
			final double density = ( double ) card / ( size * size );
			final double[] cc = new double[ card ];
			final int[] kk = new int[ card ];
			final int[] number = new int[ size ];
			int index = 0;
			for ( int i = 0; i < size; i++ )
			{ // Row by row.
				/*
				 * Element creation follows density. This gives only approximate
				 * desired cardinality. It's like that. At least the diagonal
				 */
				final int nToCreate = ( int ) Math.max( 1, ( size * density ) * ( 1 + 0.2 * ran.nextGaussian() ) );
				final int[] cols = new int[ nToCreate ];
				cols[ 0 ] = i; // diagonal
				for ( int k = 1; k < cols.length; k++ )
				{
					cols[ k ] = ran.nextInt( size );
				}
				// Extract unique
				final int[] uCols = uniqueAndSort( cols );
				for ( final int c : uCols )
				{
					kk[ index ] = c;
					cc[ index ] = Math.floor( pseudoRandom( 1, 100 ) );
					index++;
				}
				number[ i ] = uCols.length;
			}
			final int[] kk2 = Arrays.copyOf( kk, index );
			final double[] cc2 = Arrays.copyOf( cc, index );

			final SparseCostMatrix cm = new SparseCostMatrix( cc2, kk2, number, size );

			// NON-SPARSE
			final double[][] fullMatrix = cm.toFullMatrix();
			int[][] nsRes = new int[ 0 ][];
			final JonkerVolgenantAlgorithm nonSparseAlgo = new JonkerVolgenantAlgorithm();
			nsRes = nonSparseAlgo.computeAssignments( fullMatrix );

			// SPARSE
			int[] sRes = new int[ 0 ];
			final LAPJV sparseAlgo = new LAPJV( cm );
			sparseAlgo.process();
			sRes = sparseAlgo.getResult();

			// TEST
			for ( int i = 0; i < sRes.length; i++ )
			{
				assertEquals( nsRes[ i ][ 1 ], sRes[ i ] );
			}
		}

	}
}
