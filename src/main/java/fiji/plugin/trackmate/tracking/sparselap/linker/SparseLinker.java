package fiji.plugin.trackmate.tracking.sparselap.linker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.imglib2.algorithm.BenchmarkAlgorithm;
import net.imglib2.util.Util;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.tracking.jonkervolgenant.JonkerVolgenantSparseAlgorithm;
import fiji.plugin.trackmate.tracking.jonkervolgenant.SparseCostMatrix;
import fiji.plugin.trackmate.tracking.sparselap.DefaultCostFunction;

class SparseLinker< K > extends BenchmarkAlgorithm
{

	private final List< K > sources;

	private final List< K > targets;

	private final CostFunction< K > costFunction;

	private final double costThreshold;

	private final double alternativeCostFactor;

	public SparseLinker( final List< K > sources, final List< K > targets, final CostFunction< K > costFunction, final double costThreshold, final double alternativeCostFactor )
	{
		this.sources = sources;
		this.targets = targets;
		this.costFunction = costFunction;
		this.costThreshold = costThreshold;
		this.alternativeCostFactor = alternativeCostFactor;
	}

	@Override
	public boolean checkInput()
	{
		return true;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		/*
		 * 1. Top-left quadrant.
		 * 
		 * Let's suppose we have n objects in the source and m objects in the
		 * target. The top-left quadrant of the matrix cost will be a cost
		 * matrix with n lines and m columns.
		 */
		final int n = sources.size();
		final int m = targets.size();

		final List< double[] > costsPerLine = new ArrayList< double[] >( n + m );
		final List< int[] > columnsPerLine = new ArrayList< int[] >( n + m );

		double maxCost = Double.NEGATIVE_INFINITY;

		for ( int i = 0; i < n; i++ )
		{
			final double[] lineCost = new double[ m + 1 ];
			final int[] targetCols = new int[ m + 1 ];
			int index = 0;

			// Line i: cost to link source nbr i to any target j.
			final K source = sources.get( i );
			for ( int j = 0; j < m; j++ )
			{
				final K target = targets.get( j );
				final double cost = costFunction.linkingCost( source, target );
				if ( cost < costThreshold )
				{
					// Accept cost, create an entry in the sparse matrix and
					// compute max
					if ( maxCost < cost )
					{
						maxCost = cost;
					}

					lineCost[ index ] = cost;
					targetCols[ index ] = j; // therefore the array will be
												// sorted
					index++;
				}
			}

			/*
			 * We finished scanning this line for the top-left quadrant. But the
			 * top-right quadrant is made of only one diagonal element. So we
			 * need to make room in the arrays only for one more element.
			 */
			index++;
			costsPerLine.add( Arrays.copyOf( lineCost, index ) );
			columnsPerLine.add( Arrays.copyOf( targetCols, index ) );
		}
		
		/*
		 * 2. Top-right quadrant.
		 * 
		 * At this stage we know what is the max cost. So we can generate the
		 * alternate costs.
		 */

		final double alternativeCost = maxCost * alternativeCostFactor;
		for ( int i = 0; i < n; i++ )
		{
			final double[] costs = costsPerLine.get( i );
			final int[] columns = columnsPerLine.get( i );
			// Diagonal of top-right quadrant
			final int index = costs.length - 1; // store as last element
			costs[ index ] = alternativeCost;
			columns[ index ] = m + i; // diagonal
		}

		/*
		 * 3. Bottom-right quadrant & Bottom-left quadrant.
		 * 
		 * Bottom-left qudrant is not hard: This is the same than for the
		 * top-right quadrant. It is again a diagonal matrix.
		 * 
		 * Bottom-right quadrant is harder. The SUPPINFO of Jaqaman et al. 2008
		 * is a bit unclear on this. Upon inspection of the MATLAB code, we see
		 * that the right way to do it is to take the top-left quadrant,
		 * transpose it and change all non-infinite cost to the alternativeCost.
		 * This is what Nick did in his original LAPTracker.
		 * 
		 * Transposing will be hard, and we have to do the bottom-left quadrant
		 * at the same time.
		 */

		for ( int j = 0; j < m; j++ )
		{
			final double[] transposedLineCost = new double[ n + 1 ];
			final int[] transposedTargetCols = new int[ n + 1 ];

			// For the bottom-left quadrant, add the diagonal.
			transposedLineCost[ 0 ] = alternativeCost;
			transposedTargetCols[ 0 ] = j;

			int index = 1;
			for ( int i = 0; i < n; i++ )
			{

				// Find if line i had a column j
				final int[] origColumns = columnsPerLine.get( i );
				final int k = Arrays.binarySearch( origColumns, j );
				if ( k < 0 )
				{
					continue; // Nope.
				}

				transposedLineCost[ index ] = alternativeCost; // they get this.
				transposedTargetCols[ index ] = m + i;
				index++;
			}

			costsPerLine.add( Arrays.copyOf( transposedLineCost, index ) );
			columnsPerLine.add( Arrays.copyOf( transposedTargetCols, index ) );
		}

		/*
		 * 4. Building the sparse cost matrix.
		 * 
		 * Let's merge this together!
		 */
		int l = 0;
		final int[] number = new int[ n + m ];
		for ( int i = 0; i < n + m; i++ )
		{
			final double[] cl = costsPerLine.get( i );
			l += cl.length;
			number[ i ] = cl.length;
		}

		final double[] cc = new double[ l ];
		final int[] kk = new int[ l ];
		int index = 0;
		for ( int i = 0; i < n + m; i++ )
		{
			final double[] cl = costsPerLine.get( i );
			final int[] kl = columnsPerLine.get( i );
			final int size = cl.length;

			System.arraycopy( cl, 0, cc, index, size );
			System.arraycopy( kl, 0, kk, index, size );
			index += size;
		}

		System.out.println( Util.printCoordinates( kk ) );// DEBUG
		System.out.println( Util.printCoordinates( number ) );// DEBUG

		final SparseCostMatrix scm = new SparseCostMatrix( cc, kk, number, n + m );
		System.out.println( scm );// DEBUG

		final JonkerVolgenantSparseAlgorithm solver = new JonkerVolgenantSparseAlgorithm( scm );
		if ( !solver.checkInput() || !solver.process() )
		{
			errorMessage = solver.getErrorMessage();
			return false;
		}

		final int[] assignments = solver.getResult();
		for ( int i = 0; i < n; i++ )
		{
			final int targetID = assignments[ i ];
			if ( targetID < m )
			{
				System.out.println( "" + sources.get( i ) + "\t->\t" + targets.get( targetID ) );
			}
			else
			{
				System.out.println( "" + sources.get( i ) + "\t->\tNOPE" ); // DEBUG
			}
		}

		final long end = System.currentTimeMillis();
		processingTime = end - start;

		return true;
	}
	
	public static void main( final String[] args )
	{
		final Spot s1 = new Spot( 0, 1, 0, 1, -1, "s1" );
		final Spot s2 = new Spot( 0, 2, 0, 1, -1, "s2" );
		final Spot s3 = new Spot( 0, 3, 0, 1, -1, "s3" );

		final Spot t1 = new Spot( 1, 0.2, 0, 0.5, -1, "t1" );
		final Spot t2 = new Spot( 1, 1.2, 0, 0.5, -1, "t2" );
		final Spot t3 = new Spot( 1, 2.2, 0, 0.5, -1, "t3" );
		final Spot t4 = new Spot( 1, 3.2, 0, 0.5, -1, "t4" );
		final Spot t5 = new Spot( 1, 4.2, 0, 0.5, -1, "t5" );

		final List< Spot > targets = Arrays.asList( new Spot[] { s1, s2, s3 } );
		final List< Spot > sources = Arrays.asList( new Spot[] { t1, t2, t3, t4, t5 } );

		final CostFunction< Spot > costFunction = new DefaultCostFunction();
		final double costThreshold = 4d;
		final double alternativeCostFactor = 1.05;
		final SparseLinker< Spot > linker = new SparseLinker< Spot >( sources, targets, costFunction, costThreshold, alternativeCostFactor );

		if ( !linker.checkInput() || !linker.process() )
		{
			System.out.println( linker.getErrorMessage() );
		}
		else
		{
			System.out.println( "Done in " + linker.getProcessingTime() + " ms." );
		}

	}
	
}
