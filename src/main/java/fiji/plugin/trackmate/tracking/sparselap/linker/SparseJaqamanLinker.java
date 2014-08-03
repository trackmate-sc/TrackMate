package fiji.plugin.trackmate.tracking.sparselap.linker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.imglib2.algorithm.BenchmarkAlgorithm;
import net.imglib2.algorithm.OutputAlgorithm;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.tracking.jonkervolgenant.JonkerVolgenantSparseAlgorithm;
import fiji.plugin.trackmate.tracking.jonkervolgenant.SparseCostMatrix;
import fiji.plugin.trackmate.tracking.sparselap.DefaultCostFunction;

/**
 * Links two lists of objects based on the frame-to-frame linking of the LAP
 * framework described in Jaqaman <i>et al.</i>, Nature Methods, <b>2008</b>.
 * <p>
 * 
 * 
 * @author Jean-Yves Tinevez - 2014
 * 
 * @param <K>
 *            the type of the objects to link.
 */
public class SparseJaqamanLinker< K > extends BenchmarkAlgorithm implements OutputAlgorithm< int[][] >
{

	private final List< K > sources;

	private final List< K > targets;

	private final CostFunction< K > costFunction;

	private final double costThreshold;

	private final double alternativeCostFactor;

	private int[][] assignments;

	private double[] costs;

	/**
	 * Creates a new linker for the two specified object lists.
	 * 
	 * @param sources
	 *            the source objects.
	 * @param targets
	 *            the target objects.
	 * @param costFunction
	 *            a {@link CostFunction} that can compute a cost to link any
	 *            source to any target.
	 * @param costThreshold
	 *            the cost threshold above which linking will be forbidden.
	 * @param alternativeCostFactor
	 *            the Jaqaman et al. 2008 alternative cost factor, required to
	 *            build the cost for a link <b>not to happen</b>.
	 * @see {Jaqaman <i>et al.</i>, Nature Methods, <b>2008</b>, Figure 1b.}
	 */
	public SparseJaqamanLinker( final List< K > sources, final List< K > targets, final CostFunction< K > costFunction, final double costThreshold, final double alternativeCostFactor )
	{
		this.sources = sources;
		this.targets = targets;
		this.costFunction = costFunction;
		this.costThreshold = costThreshold;
		this.alternativeCostFactor = alternativeCostFactor;
	}

	/**
	 * Returns the resulting assignments from this algorithm. This
	 * <code>int[][]</code> array is such that the source object at index
	 * <code>int[i][0]</code> is linked to target at index
	 * <code>j = assignment[i][1]</code>.
	 * 
	 * @return the assignment array.
	 * @see #getAssignmentCosts()
	 */
	@Override
	public int[][] getResult()
	{
		return assignments;
	}

	/**
	 * Returns the cost array associated to the assignment result.
	 * 
	 * @return the assignment costs.
	 * @see #getResult()
	 */
	public double[] getAssignmentCosts()
	{
		return costs;
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
		int n = sources.size();
		final int m = targets.size();

		final List< double[] > costsPerLine = new ArrayList< double[] >( n + m );
		final List< int[] > columnsPerLine = new ArrayList< int[] >( n + m );

		double maxCost = Double.NEGATIVE_INFINITY;

		int[] acceptedSources = new int[ sources.size() ];
		int acceptedSourcesIndex = 0;

		for ( int i = 0; i < n; i++ )
		{
			final double[] lineCost = new double[ m + 1 ];
			final int[] targetCols = new int[ m + 1 ];
			int index = 0;

			// Line i: cost to link source nbr i to any target j.
			final K source = sources.get( i );
			boolean hasCost = false;
			for ( int j = 0; j < m; j++ )
			{
				final K target = targets.get( j );
				final double cost = costFunction.linkingCost( source, target );
				if ( cost < costThreshold )
				{
					// Accept cost, create an entry in the sparse matrix and
					// compute max
					hasCost = true;
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
			 * If the source does not have any non-infinite cost, then do not
			 * include it in the sparse matrix.
			 */
			if ( !hasCost )
			{
				continue;
			}

			acceptedSources[ acceptedSourcesIndex++ ] = i;

			/*
			 * We finished scanning this line for the top-left quadrant. But the
			 * top-right quadrant is made of only one diagonal element. So we
			 * need to make room in the arrays only for one more element.
			 */
			index++;
			costsPerLine.add( Arrays.copyOf( lineCost, index ) );
			columnsPerLine.add( Arrays.copyOf( targetCols, index ) );
		}
		// Trim the accepted sources index.
		acceptedSources = Arrays.copyOf( acceptedSources, acceptedSourcesIndex );
		n = acceptedSources.length;

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
		 * Bottom-left quadrant is not hard: This is the same than for the
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

		final SparseCostMatrix scm = new SparseCostMatrix( cc, kk, number, n + m );
		final JonkerVolgenantSparseAlgorithm solver = new JonkerVolgenantSparseAlgorithm( scm );
		if ( !solver.checkInput() || !solver.process() )
		{
			errorMessage = solver.getErrorMessage();
			return false;
		}

		final int[] trimmedAssignments = solver.getResult();

		// Store related costs;
		final double[] trimmedCosts = new double[ trimmedAssignments.length ];
		for ( int i = 0; i < trimmedAssignments.length; i++ )
		{
			final int j = trimmedAssignments[ i ];
			trimmedCosts[ i ] = scm.get( i, j, Double.POSITIVE_INFINITY );
		}

		// Put indexes back from trimmed list of sources.
		assignments = new int[ sources.size() ][];
		costs = new double[ sources.size() ];
		int assgntIndex = 0;
		for ( int i = 0; i < acceptedSources.length; i++ )
		{
			final int originalSource = acceptedSources[ i ];
			final int target = trimmedAssignments[ i ];
			if ( originalSource < n && target < targets.size() )
			{
				assignments[ assgntIndex ] = new int[ 2 ];
				assignments[ assgntIndex ][ 0 ] = originalSource;
				assignments[ assgntIndex ][ 1 ] = target;
				costs[ assgntIndex ] = trimmedCosts[ i ];
				assgntIndex++;
			}
		}

		assignments = Arrays.copyOf( assignments, assgntIndex );
		costs = Arrays.copyOf( costs, assgntIndex );

		final long end = System.currentTimeMillis();
		processingTime = end - start;

		return true;
	}

	/*
	 * MAIN METHOD
	 */

	public static void main( final String[] args )
	{
		final Spot s1 = new Spot( 0, 1, 0, 1, -1, "s1" );
		final Spot s2 = new Spot( 0, 2, 0, 1, -1, "s2" );
		final Spot s3 = new Spot( 0, 3, 0, 1, -1, "s3" );

		final Spot t1 = new Spot( 1, 0.1, 0, 0.5, -1, "t1" );
		final Spot t2 = new Spot( 1, 1.1, 0, 0.5, -1, "t2" );
		final Spot t3 = new Spot( 1, 2.1, 0, 0.5, -1, "t3" );
		final Spot t4 = new Spot( 1, 3.1, 0, 0.5, -1, "t4" );
		final Spot t5 = new Spot( 1, 4.1, 0, 0.5, -1, "t5" );

		final List< Spot > targets = Arrays.asList( new Spot[] { s1, s2, s3 } );
		final List< Spot > sources = Arrays.asList( new Spot[] { t1, t2, t3, t4, t5 } );

		final CostFunction< Spot > costFunction = new DefaultCostFunction();
		final double costThreshold = 2d;
		final double alternativeCostFactor = 1.05;
		final SparseJaqamanLinker< Spot > linker = new SparseJaqamanLinker< Spot >( sources, targets, costFunction, costThreshold, alternativeCostFactor );

		if ( !linker.checkInput() || !linker.process() )
		{
			System.out.println( linker.getErrorMessage() );
		}
		else
		{
			final int[][] assignments = linker.getResult();
			final double[] costs = linker.getAssignmentCosts();
			for ( int i = 0; i < assignments.length; i++ )
			{
				final int sourceID = assignments[ i ][ 0 ];
				final int targetID = assignments[ i ][ 1 ];
				if ( targetID < targets.size() )
				{
					System.out.println( "" + sources.get( sourceID ) + "\t->\t" + targets.get( targetID ) + " \tcost = " + costs[ i ] );
				}
				else
				{
					System.out.println( "" + sources.get( sourceID ) + "\t->\tNOPE\tcost = " + costs[ i ] );
				}
			}

			System.out.println( "Done in " + linker.getProcessingTime() + " ms." );
		}
	}
}
