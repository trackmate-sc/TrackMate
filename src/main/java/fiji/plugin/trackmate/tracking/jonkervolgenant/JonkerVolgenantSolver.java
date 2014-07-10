package fiji.plugin.trackmate.tracking.jonkervolgenant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import net.imglib2.algorithm.OutputAlgorithm;
import net.imglib2.util.Util;

/**
 * Ported from Lee code.
 */
public class JonkerVolgenantSolver implements OutputAlgorithm< int[][] >
{

	private static final String BASE_ERROR_MESSAGE = "[JonkerVolgenant] ";

	/**
	 * The index of the source object, so that linking object <code>i[n]</code>
	 * with object <code>j[n]</code> has a cost <code>c[n]</code>.
	 */
	private final int[] i;

	/**
	 * The index of the target object, so that linking object <code>i[n]</code>
	 * with object <code>j[n]</code> has a cost <code>c[n]</code>.
	 */
	private final int[] j;

	/**
	 * The cost of for linking object <code>i[n]</code> with object
	 * <code>j[n]</code>.
	 */
	private final double[] c;

	private int[][] result;

	private String errorMessage;

	public JonkerVolgenantSolver( final int[] sourceIndex, final int[] targetIndex, final double[] costs )
	{
		this.i = sourceIndex;
		this.j = targetIndex;
		this.c = costs;
	}

	@Override
	public boolean checkInput()
	{
		if ( i.length != j.length )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Source and target index arrays must have the same length.";
			return false;
		}
		if ( c.length != i.length )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Cost array and index arrays must have the same length.";
			return false;
		}
		for ( final double cost : c )
		{
			if ( cost < 0d )
			{
				errorMessage = BASE_ERROR_MESSAGE + "All costs must be positive or 0.";
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean process()
	{
		/*
		 * Order and test source indices.
		 */

		final int[][] jbc = JVSUtils.bincount( j );
		final int[] jUnique = jbc[ 0 ];
		final int[] jCount = jbc[ 1 ];
		final int[] jIndex = jbc[ 2 ];
		System.out.println( JVSUtils.bincountToString( jbc ) );

		for ( final int jc : jCount )
		{
			if ( jc == 0 )
			{
				errorMessage = BASE_ERROR_MESSAGE + "All target objects must have at least one cost linking to a source object.";
				return false;
			}
		}

		final int[][] ibc = JVSUtils.bincount( i );
		final int[] iUnique = ibc[ 0 ];
		final int[] iCount = ibc[ 1 ];
		final int[] iIndex = ibc[ 2 ];
		System.out.println( JVSUtils.bincountToString( ibc ) );

		for ( final int ic : iCount )
		{
			if ( ic == 0 )
			{
				errorMessage = BASE_ERROR_MESSAGE + "All source objects must have at least one cost linking to a target object.";
				return false;
			}
		}

		/*
		 * Prepare outputs
		 */

		// For each i, the assigned j
		final int[] x = new int[ iUnique.length ];
		Arrays.fill( x, jUnique.length ); // Means: not assigned.

		// For each j, the assigned i
		final int[] y = new int[ jUnique.length ];
		Arrays.fill( y, iUnique.length ); // Means: not assigned.

		/*
		 * COLUMN REDUCTION.
		 */

		/*
		 * For a given j, find the i with the minimum cost. We iterate in
		 * reverse order, as suggested in the paper.
		 */

		final double[] v = new double[ jUnique.length ];
		// How manu matches have we found for i?
		final int[] iMatchCount = new int[ iUnique.length ];

		for ( int kju = jUnique.length - 1; kju > -1; kju-- )
		{
			/*
			 * We focus on the value ju, and look where it is in the index. It
			 * can be found by iterating the index, and searching for index
			 * values equal to kju. Then at this index, but in the cost vector
			 * we have the cost that maps this ju to a i.
			 */
			double minCostJtoI = Double.POSITIVE_INFINITY;
			int minCostJtoIindex = -1;
			for ( int kindex = 0; kindex < jIndex.length; kindex++ )
			{
				if ( jIndex[ kindex ] == kju )
				{
					if (c[kindex] < minCostJtoI) {
						minCostJtoI = c[ kindex ];
						minCostJtoIindex = kindex;
					}
				}
			}


			if ( minCostJtoIindex >= 0 )
			{
				// Found.
				final int targetI = i[ minCostJtoIindex ];
				System.out.println( "For column " + kju + ", found a min cost = " + minCostJtoI + " at row " + targetI ); // DEBUG
				System.out.println( "V = " + Util.printCoordinates( v ) ); // DEBUG
				final int kiu = Arrays.binarySearch( iUnique, targetI );

				if ( iMatchCount[ kiu ] == 0 )
				{
					v[ kju ] = minCostJtoI;
					y[ kju ] = kiu;
					x[ kiu ] = kju;
				}
				else if ( minCostJtoI < v[ x[ kiu ] ] )
				{
					v[ kju ] = minCostJtoI;
					final int temp = x[ kiu ];
					y[ kju ] = kiu;
					x[ kiu ] = kju;
					y[ temp ] = jUnique.length; // erase
				}
				else
				{
					y[ kju ] = jUnique.length;
				}
				iMatchCount[ kiu ]++;
			}
			else
			{
				/*
				 * Should not happen at this stage, since we ensured that there
				 * is a I for any J. But hey, things can go wrong.
				 */
				final int ju = jUnique[ kju ];
				errorMessage = BASE_ERROR_MESSAGE + "Could not find a single matching source for the target " + ju + ". All target objects must have at least one cost linking to a source object.";
				return false;
			}

			result = new int[][] { iUnique, x, jUnique, y };
			System.out.println( Util.printCoordinates( iMatchCount ) ); // DEBUG
			System.out.println( JVSUtils.resultToString( result ) ); // DEBUG
			System.out.println(); // DEBUG

		}

		/*
		 * REDUCTION TRANSFER
		 */

		final Collection< Integer > freeRows = new ArrayList< Integer >();

		for ( int kiu = 0; kiu < iMatchCount.length; kiu++ )
		{
			if ( iMatchCount[ kiu ] == 0 )
			{
				freeRows.add( Integer.valueOf( kiu ) );
			}
			else if ( iMatchCount[ kiu ] == 1 )
			{
				final int j1 = x[ kiu ];
				double minU = Double.POSITIVE_INFINITY;
				for ( int kju = 0; kju < jUnique.length; kju++ )
				{
					if ( kju == j1 )
					{
						continue;
					}
					// Search for the cost

					if ( minU > -v[ kju ] )
					{
						minU = -v[ kju ];
					}
				}
			}


		}

		/*
		 * TERMINATE
		 */
		result = new int[][] { iUnique, x, jUnique, y };

		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public int[][] getResult()
	{
		return result;
	}


	/*
	 * MAIN METHOD
	 */

	public static void main( final String[] args )
	{
//		final int[] i = new int[] { 0, 0, 1, 1, 2, 2 };
//		final int[] j = new int[] { 0, 1, 1, 2, 2, 3 };
//		final double[] c = new double[] { 21, 12, 43, 64, 25, 36 };

		final int[] i = new int[] {
			     0,
			     0,
			     0,
			     0,
			     0,
			     1,
			     1,
			     1,
			     1,
			     1,
			     2,
			     2,
			     2,
			     2,
			     2,
			     3,
			     3,
			     3,
			     3,
			     3,
			     4,
			     4,
			     4,
			     4,
			     4
		};
		final int[] j = new int[] {
				0,
				1,
				2,
				3,
				4,
				0,
				1,
				2,
				3,
				4,
				0,
				1,
				2,
				3,
				4,
				0,
				1,
				2,
				3,
				4,
				0,
				1,
				2,
				3,
				4
		};

		final double[] c = new double[] {
				17,
				23,
				4,
				10,
				11,
				24,
				5,
				6,
				12,
				18,
				1,
				7,
				13,
				19,
				25,
				8,
				14,
				20,
				21,
				2,
				15,
				16,
				22,
				3,
				9,
		};

		final JonkerVolgenantSolver solver = new JonkerVolgenantSolver( i, j, c );
		final long start = System.currentTimeMillis();
		if ( !solver.checkInput() || !solver.process() )
		{
			System.out.println( "Problem with Jonker-Volgenant solver:\n" + solver.getErrorMessage() );
			System.exit( 1 );
		}
		final long end = System.currentTimeMillis();
		final int[][] result = solver.getResult();
		System.out.println( "Algorithm converged in " + ( end - start ) + " ms." );
		System.out.println( JVSUtils.resultToString( solver.getResult() ) );

	}

}
