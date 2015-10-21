package fiji.plugin.trackmate.tracking.sparselap.linker;

import static org.junit.Assert.assertEquals;
import fiji.plugin.trackmate.tracking.oldlap.hungarian.JonkerVolgenantAlgorithm;

import java.util.Arrays;
import java.util.Random;

public class LAPJVBenchmark
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
	
	private int[] generateIntLinSpaced( final int start, final int end, final int n )
	{
		final int[] result = new int[ n ];
		final double inc = ( ( double ) end - start ) / ( n );

		for ( int i = 0; i < result.length; i++ )
		{
			result[ i ] = start + ( int ) Math.floor( i * inc );
		}
		return result;
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

	public final void timeVaryingDensity()
	{
		final int nRepeats = 10;
		final int size = 500;
		final Random ran = new Random();
		seed = ran.nextInt();

		final int[] cardinalities = generateIntLogSpaced( 4 * size, size * size, 100 );

		System.out.println( "" + size + " x " + size + " cost matrix, " + nRepeats + " repetitions." );
		System.out.println( "Density\tTimeNotSparse(ms)\tTimeSparse(ms)\tFactor" );

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
			System.out.print( String.format( "%.4f", ( double ) cm.cardinality / ( size * size ) ) );

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
			System.out.print( String.format( "\t%.1f", ( double ) dt1 / nRepeats ) );

			// SPARSE
			start = System.currentTimeMillis();
			int[] sRes = new int[ 0 ];
			for ( int i = 0; i < nRepeats; i++ )
			{
				final LAPJV sparseAlgo = new LAPJV( cm );
				sparseAlgo.process();
				sRes = sparseAlgo.getResult();
			}
			end = System.currentTimeMillis();
			final long dt2 = end - start;

			System.out.print( String.format( "\t\t\t%.1f", ( double ) dt2 / nRepeats ) );
			System.out.print( String.format( "\t\t%.2f\n", ( double ) dt2 / dt1 ) );

			// TEST
			for ( int i = 0; i < sRes.length; i++ )
			{
				assertEquals( nsRes[ i ][ 1 ], sRes[ i ] );
			}
		}
	}

	public final void timeVaryingSize()
	{
		final int nRepeats = 10;
		final double density = 0.10d;
		final Random ran = new Random();
		seed = ran.nextInt();

		final int[] sizes = generateIntLinSpaced( 100, 1000, 50 );

		System.out.println( "Size\tTimeNotSparse(ms)\tTimeSparse(ms)\tFactor" );

		for ( int ci = 0; ci < sizes.length; ci++ )
		{
			final int size = sizes[ ci ];
			final int card = ( int ) Math.floor( size * size * density );
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
			System.out.print( String.format( "% 4d", size ) );

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
			System.out.print( String.format( "\t%.1f", ( double ) dt1 / nRepeats ) );

			// SPARSE
			start = System.currentTimeMillis();
			int[] sRes = new int[ 0 ];
			for ( int i = 0; i < nRepeats; i++ )
			{
				final LAPJV sparseAlgo = new LAPJV( cm );
				sparseAlgo.process();
				sRes = sparseAlgo.getResult();
			}
			end = System.currentTimeMillis();
			final long dt2 = end - start;

			System.out.print( String.format( "\t\t\t%.1f", ( double ) dt2 / nRepeats ) );
			System.out.print( String.format( "\t\t%.2f\n", ( double ) dt2 / dt1 ) );

			// TEST
			for ( int i = 0; i < sRes.length; i++ )
			{
				assertEquals( nsRes[ i ][ 1 ], sRes[ i ] );
			}
		}
	}


	public static void main( final String[] args )
	{
		final LAPJVBenchmark benchmark = new LAPJVBenchmark();

		System.out.println( "---------------" );
		System.out.println( "Varying density" );
		System.out.println( "---------------" );
		System.out.println();
		benchmark.timeVaryingDensity();

		System.out.println( "------------" );
		System.out.println( "Varying size" );
		System.out.println( "------------" );
		System.out.println();
		benchmark.timeVaryingSize();
	}
}
