package fiji.plugin.trackmate.tracking.sparselap;

import static fiji.plugin.trackmate.tracking.LAPUtils.checkFeatureMap;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_CUTOFF_PERCENTILE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.OutputAlgorithm;
import net.imglib2.util.Util;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.tracking.jonkervolgenant.JVSUtils;
import fiji.plugin.trackmate.tracking.jonkervolgenant.SparseCostMatrix;
import fiji.plugin.trackmate.tracking.sparselap.linker.CostFunction;

/**
 * A cost matrix creator that deals with the case where only gap-closing events
 * are allowed.
 * 
 * @author Jean-Yves Tinevez
 */
public class GapClosingOnlyCostMatrixCreator implements Benchmark, OutputAlgorithm< SparseCostMatrix >
{

	private static final String BASE_ERROR_MESSAGE = "[GapClosingOnlyCostMatrixCreator] ";

	private String errorMessage;

	private final List< Spot > segmentEnds;

	private final List< Spot > segmentStarts;

	private final Map< String, Object > settings;

	private SparseCostMatrix scm;

	private long processingTime;

	private int[] uniqueSources;

	private int[] uniqueTargets;

	public GapClosingOnlyCostMatrixCreator( final List< Spot > segmentEnds, final List< Spot > segmentStarts, final Map< String, Object > settings )
	{
		this.segmentEnds = segmentEnds;
		this.segmentStarts = segmentStarts;
		this.settings = settings;
	}

	@Override
	public boolean checkInput()
	{
		if ( segmentEnds.isEmpty() )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Segment ends list is empty.";
			return false;
		}
		if ( segmentStarts.isEmpty() )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Segment starts list is empty.";
			return false;
		}
		final StringBuilder str = new StringBuilder();
		if ( !checkSettingsValidity( settings, str ) )
		{
			errorMessage = BASE_ERROR_MESSAGE + str.toString();
			return false;
		}
		return true;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		@SuppressWarnings( "unchecked" )
		final Map< String, Double > gcFeaturePenalties = ( Map< String, Double > ) settings.get( KEY_GAP_CLOSING_FEATURE_PENALTIES );
		final CostFunction< Spot > gcCostFunction = getCostFunctionFor( gcFeaturePenalties );
		final int maxFrameInterval = ( Integer ) settings.get( KEY_GAP_CLOSING_MAX_FRAME_GAP );
		final double maxDistance = ( Double ) settings.get( KEY_GAP_CLOSING_MAX_DISTANCE );
		final double costThreshold = maxDistance * maxDistance;

		/*
		 * Top-left quadrant
		 */

		// Sources and targets. They are sorted by increasing column
		// then
		// increasing line (scanning row by row).
		final ResizableIntArray sourceIndexes = new ResizableIntArray();
		final ResizableIntArray targetIndexes = new ResizableIntArray();
		// Corresponding costs.
		final ResizableDoubleArray linkCosts = new ResizableDoubleArray();

		// Loop over all sources and all targets, and find max cost.
		for ( int i = 0; i < segmentEnds.size(); i++ )
		{
			final Spot source = segmentEnds.get( i );

			for ( int j = 0; j < segmentStarts.size(); j++ )
			{
				if ( i == j )
				{
					// Cannot link to yourself.
					continue;
				}

				final Spot target = segmentStarts.get( j );

				// Check frame interval.
				final int tdiff = ( int ) target.diffTo( source, Spot.FRAME );
				if ( tdiff > maxFrameInterval || tdiff < 1 )
				{
					continue;
				}

				// Check max distance
				final double cost = gcCostFunction.linkingCost( source, target );
				if ( cost > costThreshold )
				{
					continue;
				}

				sourceIndexes.add( i );
				targetIndexes.add( j );
				linkCosts.add( cost );
			}
		}

		sourceIndexes.trimToSize();
		targetIndexes.trimToSize();
		linkCosts.trimToSize();

		/*
		 * Get gap-closing alternative costs.
		 */
		final double alternativeCost = getGapClosingAlternativeCost( linkCosts.data );

		uniqueSources = JVSUtils.unique( sourceIndexes.data );
		uniqueTargets = JVSUtils.unique( targetIndexes.data );

		// Build the cost matrix
		final int nCols = uniqueTargets.length;
		final int nRows = uniqueSources.length;
		final double[] cc = new double[ sourceIndexes.size ];
		final int[] kk = new int[ sourceIndexes.size ];
		final int[] number = new int[ nRows ];

		int psi = sourceIndexes.data[ 0 ];
		int rowIndex = 0;
		int index = -1;
		int rowCount = -1;
		for ( int i = 0; i < sourceIndexes.size; i++ )
		{
			index++;
			rowCount++;
			final int si = sourceIndexes.data[ i ];
			if ( si != psi )
			{
				// new source
				psi = si;
				// Store the number of element in the line
				number[ rowIndex ] = rowCount;
				// Increment row index, global index and reset row count;
				rowIndex++;
				rowCount = 0;
				// You are now on a new line.
			}

			cc[ index ] = linkCosts.data[ i ];
			final int ti = targetIndexes.data[ i ];
			final int sp = Arrays.binarySearch( uniqueTargets, ti );
			kk[ index ] = sp;
		}
		// Terminate the last row
		// Store the number of element in the line
		rowCount++;
		number[ rowIndex ] = rowCount;

		final SparseCostMatrix scmtl = new SparseCostMatrix( cc, kk, number, nCols );

		/*
		 * Top-right quadrant
		 */

		final double[] cctr = new double[ nRows ];
		Arrays.fill( cctr, alternativeCost );
		final int[] numbertr = new int[ nRows ];
		Arrays.fill( numbertr, 1 );
		final int[] kktr = new int[ nRows ];
		for ( int i = 0; i < kktr.length; i++ )
		{
			kktr[ i ] = i;
		}
		final SparseCostMatrix scmtr = new SparseCostMatrix( cctr, kktr, numbertr, nRows );

		/*
		 * Bottom-left quadrant.
		 */

		final double[] ccbl = new double[ nCols ];
		Arrays.fill( ccbl, alternativeCost );
		final int[] numberbl = new int[ nCols ];
		Arrays.fill( numberbl, 1 );
		final int[] kkbl = new int[ nCols ];
		for ( int i = 0; i < kkbl.length; i++ )
		{
			kkbl[ i ] = i;
		}
		final SparseCostMatrix scmbl = new SparseCostMatrix( ccbl, kkbl, numberbl, nCols );

		/*
		 * Bottom-right quadrant.
		 */

		final SparseCostMatrix scmbr = scmtl.transpose();
		scmbr.fillWith( alternativeCost );

		/*
		 * Put all of them together
		 */

		scm = ( scmtl.hcat( scmtr ) ).vcat( scmbl.hcat( scmbr ) );

		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	/**
	 * Returns the index of the specified source in the generated cost matrix.
	 * 
	 * @return the index array, such that <code>sourceIndex[ i ]</code> is the
	 *         index in the specified segment ends list of the row
	 *         <code>i</code> in the generated cost matrix.
	 * @see #getTargetIndex()
	 * @see #getResult()
	 */
	public int[] getSourceIndex()
	{
		return uniqueSources;
	}

	/**
	 * Returns the index of the specified target in the generated cost matrix.
	 * 
	 * @return the index array, such that <code>targetIndex[ j ]</code> is the
	 *         index in the specified segment starts list of the column
	 *         <code>j</code> in the generated cost matrix.
	 * @see #getSourceIndex()
	 * @see #getResult()
	 */
	public int[] getTargetIndex()
	{
		return uniqueTargets;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	/**
	 * Computes the alternative cost for the rejection of gap-closing event.
	 * 
	 * @param gapClosingCosts
	 *            the gap-closing costs
	 * @return the alternative cost for the rejection of gap-closing events.
	 */
	protected double getGapClosingAlternativeCost( final double[] gapClosingCosts )
	{
		/*
		 * Here we follow loosely the Jaqaman et al. paper, regarding that the
		 * u-track code changed a lot from the description in the paper. We do
		 * like Nick did for the non-sparse version.
		 */
		final double percentile = ( Double ) settings.get( KEY_CUTOFF_PERCENTILE );
		final double alternativeLinkingCostFactor = ( Double ) settings.get( KEY_ALTERNATIVE_LINKING_COST_FACTOR );
		return alternativeLinkingCostFactor * Util.computePercentile( gapClosingCosts, percentile );
	}

	protected CostFunction< Spot > getCostFunctionFor( final Map< String, Double > featurePenalties )
	{
		final CostFunction< Spot > costFunction;
		if ( null == featurePenalties || featurePenalties.isEmpty() )
		{
			costFunction = new DefaultCostFunction();
		}
		else
		{
			costFunction = new FeaturePenaltyCostFunction( featurePenalties );
		}
		return costFunction;
	}

	/**
	 * The generated cost matrix according to the LAP framework.
	 * 
	 * @return a new {@link SparseCostMatrix}.
	 */
	@Override
	public SparseCostMatrix getResult()
	{
		return scm;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	private static final boolean checkSettingsValidity( final Map< String, Object > settings, final StringBuilder str )
	{
		if ( null == settings )
		{
			str.append( "Settings map is null.\n" );
			return false;
		}

		boolean ok = true;
		// Gap-closing
		ok = ok & checkParameter( settings, KEY_GAP_CLOSING_MAX_DISTANCE, Double.class, str );
		ok = ok & checkParameter( settings, KEY_GAP_CLOSING_MAX_FRAME_GAP, Integer.class, str );
		ok = ok & checkFeatureMap( settings, KEY_GAP_CLOSING_FEATURE_PENALTIES, str );
		// Others
		ok = ok & checkParameter( settings, KEY_ALTERNATIVE_LINKING_COST_FACTOR, Double.class, str );
		ok = ok & checkParameter( settings, KEY_CUTOFF_PERCENTILE, Double.class, str );

		// Check keys
		final List< String > mandatoryKeys = new ArrayList< String >();
		mandatoryKeys.add( KEY_GAP_CLOSING_MAX_DISTANCE );
		mandatoryKeys.add( KEY_GAP_CLOSING_MAX_FRAME_GAP );
		mandatoryKeys.add( KEY_ALTERNATIVE_LINKING_COST_FACTOR );
		mandatoryKeys.add( KEY_CUTOFF_PERCENTILE );
		final List< String > optionalKeys = new ArrayList< String >();
		optionalKeys.add( KEY_GAP_CLOSING_FEATURE_PENALTIES );
		ok = ok & checkMapKeys( settings, mandatoryKeys, optionalKeys, str );

		return ok;
	}


}
