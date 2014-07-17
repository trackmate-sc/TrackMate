package fiji.plugin.trackmate.tracking.jonkervolgenant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;

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

//	@Test
	public void speedTest()
	{
		JonkerVolgenantSparseAlgorithm jvs;
		final JonkerVolgenantAlgorithm jonkerVolgenant = new JonkerVolgenantAlgorithm();
		seed = new Random().nextInt();
		final int nRepeats = 10;
		final int matrixSize = 400;
		final double[][] weights = generateMatrix( matrixSize );
		final SparseCostMatrix sparse = generateSparseMatrix( weights );
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

//	@Test
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

//	@Test
	public final void timeSparse()
	{
		final int nRepeats = 10;
		final int size = 500;
		final Random ran = new Random( 0l );
		seed = ran.nextInt();

		final int[] cardinalities = generateIntLogSpaced( 4 * size, size * size, 100 );

		System.out.println( "" + size + " x " + size + " cost matrix, " + nRepeats + " repetitions." );
		System.out.println( "Cardinality\tTimeNotSparse(ms)\tTimeSparse(ms)\tFactor" );

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
			System.out.print( "" + cm.cardinality );

			// NON-SPARSE
			final double[][] fullMatrix = cm.toFullMatrix();
			long start = System.currentTimeMillis();
			int[][] nsRes = new int[ 0 ][];
			for ( int i = 0; i < nRepeats; i++ )
			{
				final JonkerVolgenantAlgorithm nonSparseAlgo = new JonkerVolgenantAlgorithm();
				nsRes = nonSparseAlgo.computeAssignments( fullMatrix );
			}
			long end = System.currentTimeMillis();
			final long dt1 = end - start;
			System.out.print( "\t\t" + dt1 );

			// SPARSE
			start = System.currentTimeMillis();
			int[] sRes = new int[ 0 ];
			for ( int i = 0; i < nRepeats; i++ )
			{
				final JonkerVolgenantSparseAlgorithm sparseAlgo = new JonkerVolgenantSparseAlgorithm( cm );
				sparseAlgo.process();
				sRes = sparseAlgo.getResult();
			}
			end = System.currentTimeMillis();
			final long dt2 = end - start;

			System.out.print( "\t\t\t" + dt2 );
			System.out.print( String.format( "\t\t%.2f\n", ( double ) dt2 / dt1 ) );

			// TEST
			for ( int i = 0; i < sRes.length; i++ )
			{
				assertEquals( nsRes[ i ][ 1 ], sRes[ i ] );
			}

			/*
			 * System.out.println( "\n" + cm ); System.out.println( "CM layout:"
			 * ); System.out.print( "cc:\t" ); for ( final double c : cm.cc ) {
			 * System.out.print( String.format( "% 5.0f", c ) ); }
			 * System.out.println(); System.out.print( "kk:\t" ); for ( final
			 * int k : cm.kk ) { System.out.print( String.format( "% 5d", k ) );
			 * } System.out.println(); System.out.println( "nbr:\t" +
			 * Util.printCoordinates( cm.number ) ); System.out.println(
			 * "strt:\t" + Util.printCoordinates( cm.start ) );
			 * System.out.println( "\nAssignments:" ); System.out.println(
			 * JVSUtils.resultToString( nsRes, sRes ) ); System.out.println(
			 * "Total cost non-sparse = " + JVSUtils.totalAssignmentCost(
			 * fullMatrix, nsRes ) ); System.out.println( "Total cost sparse = "
			 * + cm.totalAssignmentCost( sRes ) ); break;
			 */
		}

	}
}