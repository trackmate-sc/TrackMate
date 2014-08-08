package fiji.plugin.trackmate.tracking.sparselap;

import static fiji.plugin.trackmate.tracking.LAPUtils.checkFeatureMap;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_BLOCKING_VALUE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_CUTOFF_PERCENTILE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_MAX_DISTANCE;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.OutputAlgorithm;
import net.imglib2.util.Util;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.tracking.LAPUtils;
import fiji.plugin.trackmate.tracking.jonkervolgenant.SparseCostMatrix;
import fiji.plugin.trackmate.tracking.sparselap.linker.CostFunction;

/**
 * This class generates the top-left quadrant of the LAP segment linking cost
 * matrix, following <code>Jaqaman et al., 2008 Nature Methods</code>. It can
 * also computes the alternative cost value, to use to complete this quadrant
 * with the 3 others in the final LAP cost matrix.
 * <p>
 * Warning: we changed and simplified some things compared to the original paper
 * and the MATLAB implementation by Khulud Jaqaman:
 * <ul>
 * <li>There is only one alternative cost for all segment linking, and it
 * calculated as <code>alternativeCostFactor x 90% percentile</code> of all the
 * non-infinite costs.
 * <li>Costs are based on square distance +/- feature penalties.
 * </ul>
 * 
 * @author Jean-Yves Tinevez - 2014
 * 
 */
public class JaqamanSegmentCostMatrixCreator implements Benchmark, OutputAlgorithm< SparseCostMatrix >
{

	private static final String BASE_ERROR_MESSAGE = "[JaqamanSegmentCostMatrixCreator] ";

	private final Map< String, Object > settings;

	private String errorMessage;

	private SparseCostMatrix scm;

	private long processingTime;

	private ArrayList< Spot > uniqueSources;

	private ArrayList< Spot > uniqueTargets;

	private final UndirectedGraph< Spot, DefaultWeightedEdge > graph;

	private double alternativeCost = -1;

	/**
	 * Instantiates a cost matrix creator for the top-left quadrant of the
	 * segment linking cost matrix.
	 * 
	 */
	public JaqamanSegmentCostMatrixCreator( final UndirectedGraph< Spot, DefaultWeightedEdge > graph, final Map< String, Object > settings )
	{
		this.graph = graph;
		this.settings = settings;
	}

	@Override
	public boolean checkInput()
	{
		final StringBuilder str = new StringBuilder();
		if ( !checkSettingsValidity( settings, str ) )
		{
			errorMessage = BASE_ERROR_MESSAGE + str.toString();
			return false;
		}
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		/*
		 * Extract parameters
		 */

		// Gap closing.
		@SuppressWarnings( "unchecked" )
		final Map< String, Double > gcFeaturePenalties = ( Map< String, Double > ) settings.get( KEY_GAP_CLOSING_FEATURE_PENALTIES );
		final CostFunction< Spot > gcCostFunction = getCostFunctionFor( gcFeaturePenalties );
		final int maxFrameInterval = ( Integer ) settings.get( KEY_GAP_CLOSING_MAX_FRAME_GAP );
		final double gcMaxDistance = ( Double ) settings.get( KEY_GAP_CLOSING_MAX_DISTANCE );
		final double gcCostThreshold = gcMaxDistance * gcMaxDistance;
		final boolean allowGapClosing = ( Boolean ) settings.get( KEY_ALLOW_GAP_CLOSING );

		// Merging
		@SuppressWarnings( "unchecked" )
		final Map< String, Double > mFeaturePenalties = ( Map< String, Double > ) settings.get( KEY_MERGING_FEATURE_PENALTIES );
		final CostFunction< Spot > mCostFunction = getCostFunctionFor( mFeaturePenalties );
		final double mMaxDistance = ( Double ) settings.get( KEY_MERGING_MAX_DISTANCE );
		final double mCostThreshold = mMaxDistance * mMaxDistance;
		final boolean allowMerging = ( Boolean ) settings.get( KEY_ALLOW_TRACK_MERGING );

		// Splitting
		@SuppressWarnings( "unchecked" )
		final Map< String, Double > sFeaturePenalties = ( Map< String, Double > ) settings.get( KEY_SPLITTING_FEATURE_PENALTIES );
		final CostFunction< Spot > sCostFunction = getCostFunctionFor( sFeaturePenalties );
		final boolean allowSplitting = ( Boolean ) settings.get( KEY_ALLOW_TRACK_SPLITTING );
		final double sMaxDistance = ( Double ) settings.get( KEY_SPLITTING_MAX_DISTANCE );
		final double sCostThreshold = sMaxDistance * sMaxDistance;

		/*
		 * Find segment ends, starts and middle points.
		 */

		final boolean mergingOrSplitting = allowMerging || allowSplitting;

		final GraphSegmentSplitter segmentSplitter = new GraphSegmentSplitter( graph, mergingOrSplitting );
		final List< Spot > segmentEnds = segmentSplitter.getSegmentEnds();
		final List< Spot > segmentStarts = segmentSplitter.getSegmentStarts();

		/*
		 * Generate all middle points list. We have to sort it by the same order
		 * we will sort the unique list of targets, otherwise the SCM will
		 * complains it does not receive columns in the right order.
		 */
		final List< Spot > allMiddles;
		if ( mergingOrSplitting )
		{
			final List< List< Spot >> segmentMiddles = segmentSplitter.getSegmentMiddles();
			allMiddles = new ArrayList< Spot >();
			for ( final List< Spot > segment : segmentMiddles )
			{
				allMiddles.addAll( segment );
			}
			Collections.sort( allMiddles, idComparator );
		}
		else
		{
			allMiddles = Collections.emptyList();
		}

		/*
		 * Sources and targets. We need to ensure they will be sorted by
		 * increasing column then increasing row, so we need to maintain two
		 * lists of targets. We will merge them after having sorted them
		 * separately.
		 */
		final ArrayList< Spot > sources = new ArrayList< Spot >();
		final HashSet< Spot > targetsGC = new HashSet< Spot >();
		final HashSet< Spot > targetsM = new HashSet< Spot >();
		// But we still need to have a linear list with all targets
		final ArrayList< Spot > targets = new ArrayList< Spot >();
		// The same for segment ends and starts
		Collections.sort( segmentEnds, idComparator );
		Collections.sort( segmentStarts, idComparator );

		// Corresponding costs.
		final ResizableDoubleArray linkCosts = new ResizableDoubleArray();

		/*
		 * A. We iterate over all segment ends, targeting 1st the segment starts
		 * (gap-closing) then the segment middles (merging).
		 */
		for ( int i = 0; i < segmentEnds.size(); i++ )
		{
			final Spot source = segmentEnds.get( i );

			/*
			 * Iterate over segment starts - GAP-CLOSING.
			 */

			if ( allowGapClosing )
			{
				for ( int j = 0; j < segmentStarts.size(); j++ )
				{
					final Spot target = segmentStarts.get( j );

					// Check frame interval, must be within user specification.
					final int tdiff = ( int ) target.diffTo( source, Spot.FRAME );
					if ( tdiff < 1 || tdiff > maxFrameInterval )
					{
						continue;
					}

					// Check max distance
					final double cost = gcCostFunction.linkingCost( source, target );
					if ( cost > gcCostThreshold )
					{
						continue;
					}

					sources.add( source );
					targetsGC.add( target );
					targets.add( target );
					linkCosts.add( cost );
				}
			}

			/*
			 * Iterate over middle points - MERGING.
			 */

			if ( !allowMerging )
			{
				continue;
			}
			for ( int j = 0; j < allMiddles.size(); j++ )
			{
				final Spot target = allMiddles.get( j );

				// Check frame interval, must be 1.
				final int tdiff = ( int ) target.diffTo( source, Spot.FRAME );
				if ( tdiff != 1 )
				{
					continue;
				}

				// Check max distance
				final double cost = mCostFunction.linkingCost( source, target );
				if ( cost > mCostThreshold )
				{
					continue;
				}

				sources.add( source );
				targetsM.add( target );
				targets.add( target );
				linkCosts.add( cost );
			}
		}

		/*
		 * Iterate over middle points targeting segment starts - SPLITTING
		 */
		if ( allowSplitting )
		{

			for ( int i = 0; i < allMiddles.size(); i++ )
			{
				final Spot source = allMiddles.get( i );

				for ( int j = 0; j < segmentStarts.size(); j++ )
				{
					final Spot target = segmentStarts.get( j );

					// Check frame interval, must be 1.
					final int tdiff = ( int ) target.diffTo( source, Spot.FRAME );
					if ( tdiff != 1 )
					{
						continue;
					}

					// Check max distance
					final double cost = sCostFunction.linkingCost( source, target );
					if ( cost > sCostThreshold )
					{
						continue;
					}

					sources.add( source );
					targets.add( target );
					targetsGC.add( target );
					linkCosts.add( cost );

				}
			}
		}

		/*
		 * Compute the alternative cost from the cost array
		 */

		linkCosts.trimToSize();
		this.alternativeCost = computeAlternativeCost( linkCosts.data );

		/*
		 * Build a sparse cost matrix from this.
		 */

		/*
		 * uniqueSources must be sorted according to the order in the original
		 * list. Fortunately, duplicate elements are neighbors in the list.
		 */
		uniqueSources = new ArrayList< Spot >();
		Spot previousSpot = sources.get( 0 );
		uniqueSources.add( previousSpot );
		for ( int ii = 1; ii < sources.size(); ii++ )
		{
			final Spot spot = sources.get( ii );
			if ( spot != previousSpot )
			{
				previousSpot = spot;
				uniqueSources.add( spot );
			}
		}

		/*
		 * uniqueTargets must be sorted by the same comparator that was used to
		 * sort the mother list. Otherwise we will generate column index not
		 * strictly in increasing number and the SCM will complain.
		 */
		final ArrayList< Spot > uniqueTargetsGC = new ArrayList< Spot >( targetsGC );
		Collections.sort( uniqueTargetsGC, idComparator );
		final ArrayList< Spot > uniqueTargetsM = new ArrayList< Spot >( targetsM );
		Collections.sort( uniqueTargetsM, idComparator );
		uniqueTargets = new ArrayList< Spot >();
		uniqueTargets.addAll( uniqueTargetsGC );
		uniqueTargets.addAll( uniqueTargetsM );

		// Build the cost matrix
		final int nCols = uniqueTargets.size();
		final int nRows = uniqueSources.size();
		final double[] cc = new double[ sources.size() ];
		final int[] kk = new int[ sources.size() ];
		final int[] number = new int[ nRows ];

		Spot previousSource = sources.get( 0 );
		int rowIndex = 0;
		int index = -1;
		int rowCount = -1;
		for ( int i = 0; i < sources.size(); i++ )
		{
			index++;
			rowCount++;
			final Spot source = sources.get( i );
			if ( source != previousSource )
			{
				// new source
				previousSource = source;
				// Store the number of element in the line
				number[ rowIndex ] = rowCount;
				// Increment row index, global index and reset row count;
				rowIndex++;
				rowCount = 0;
				// You are now on a new line.
			}

			cc[ index ] = linkCosts.data[ i ];
			final Spot target = targets.get( i );
			int sp = Collections.binarySearch( uniqueTargetsGC, target, idComparator );
			if ( sp < 0 )
			{
				sp = Collections.binarySearch( uniqueTargetsM, target, idComparator ) + uniqueTargetsGC.size();
			}
			kk[ index ] = sp;
		}
		// Terminate the last row
		// Store the number of element in the line
		rowCount++;
		number[ rowIndex ] = rowCount;

		scm = new SparseCostMatrix( cc, kk, number, nCols );

		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	protected double computeAlternativeCost( final double[] data )
	{
		// Link Nick Perry original non sparse LAP framework.
		final double altCostFact = ( Double ) settings.get( KEY_ALTERNATIVE_LINKING_COST_FACTOR );
		final double percentileCutOff = ( Double ) settings.get( KEY_CUTOFF_PERCENTILE );
		return altCostFact * Util.computePercentile( data, percentileCutOff );
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

	@Override
	public SparseCostMatrix getResult()
	{
		return scm;
	}

	/**
	 * Returns the list of sources in the generated cost matrix.
	 * 
	 * @return the list of Spot, such that <code>sourceIndex.get( i )</code> is
	 *         the spot corresponding to the row <code>i</code> in the generated
	 *         cost matrix.
	 * @see #getTargetList()
	 * @see #getResult()
	 */
	public List< Spot > getSourceList()
	{
		return uniqueSources;
	}

	/**
	 * Returns the list of targets in the generated cost matrix.
	 * 
	 * @return the list of Spot, such that <code>targetIndex.get( j )</code> is
	 *         the spot corresponding to the column <code>j</code> in the
	 *         generated cost matrix.
	 * @see #getSourceList()
	 * @see #getResult()
	 */
	public List< Spot > getTargetList()
	{
		return uniqueTargets;
	}

	/**
	 * Returns the alternative cost derived from all the costs calculated when
	 * creating the cost matrix.
	 * 
	 * @return the alternative cost.
	 */
	public double getAlternativeCost()
	{
		return alternativeCost;
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
		ok = ok & checkParameter( settings, KEY_ALLOW_GAP_CLOSING, Boolean.class, str );
		ok = ok & checkParameter( settings, KEY_GAP_CLOSING_MAX_DISTANCE, Double.class, str );
		ok = ok & checkParameter( settings, KEY_GAP_CLOSING_MAX_FRAME_GAP, Integer.class, str );
		ok = ok & checkFeatureMap( settings, KEY_GAP_CLOSING_FEATURE_PENALTIES, str );
		// Splitting
		ok = ok & checkParameter( settings, KEY_ALLOW_TRACK_SPLITTING, Boolean.class, str );
		ok = ok & checkParameter( settings, KEY_SPLITTING_MAX_DISTANCE, Double.class, str );
		ok = ok & checkFeatureMap( settings, KEY_SPLITTING_FEATURE_PENALTIES, str );
		// Merging
		ok = ok & checkParameter( settings, KEY_ALLOW_TRACK_MERGING, Boolean.class, str );
		ok = ok & checkParameter( settings, KEY_MERGING_MAX_DISTANCE, Double.class, str );
		ok = ok & checkFeatureMap( settings, KEY_MERGING_FEATURE_PENALTIES, str );
		// Others
		ok = ok & checkParameter( settings, KEY_ALTERNATIVE_LINKING_COST_FACTOR, Double.class, str );
		ok = ok & checkParameter( settings, KEY_CUTOFF_PERCENTILE, Double.class, str );

		// Check keys
		final List< String > mandatoryKeys = new ArrayList< String >();
		mandatoryKeys.add( KEY_ALLOW_GAP_CLOSING );
		mandatoryKeys.add( KEY_GAP_CLOSING_MAX_DISTANCE );
		mandatoryKeys.add( KEY_GAP_CLOSING_MAX_FRAME_GAP );
		mandatoryKeys.add( KEY_ALLOW_TRACK_SPLITTING );
		mandatoryKeys.add( KEY_SPLITTING_MAX_DISTANCE );
		mandatoryKeys.add( KEY_ALLOW_TRACK_MERGING );
		mandatoryKeys.add( KEY_MERGING_MAX_DISTANCE );
		mandatoryKeys.add( KEY_ALTERNATIVE_LINKING_COST_FACTOR );
		mandatoryKeys.add( KEY_CUTOFF_PERCENTILE );
		final List< String > optionalKeys = new ArrayList< String >();
		optionalKeys.add( KEY_GAP_CLOSING_FEATURE_PENALTIES );
		optionalKeys.add( KEY_SPLITTING_FEATURE_PENALTIES );
		optionalKeys.add( KEY_MERGING_FEATURE_PENALTIES );
		ok = ok & checkMapKeys( settings, mandatoryKeys, optionalKeys, str );

		return ok;
	}

	public static void main( final String[] args )
	{
		final File file = new File( "samples/FakeTracks.xml" );
		final TmXmlReader reader = new TmXmlReader( file );
		final Model model = reader.getModel();
		final SpotCollection spots = model.getSpots();

		final Settings settings0 = new Settings();
		reader.readSettings( settings0, null, new TrackerProvider(), null, null, null );

		final Map< String, Object > settings1 = new HashMap< String, Object >();
		settings1.put( KEY_LINKING_MAX_DISTANCE, 15d );
		settings1.put( KEY_ALTERNATIVE_LINKING_COST_FACTOR, 1.05 );
		final SparseLAPFrameToFrameTracker ftfTracker = new SparseLAPFrameToFrameTracker( spots, settings1 );
		if ( !ftfTracker.checkInput() || !ftfTracker.process() )
		{
			System.err.println( ftfTracker.getErrorMessage() );
			return;
		}

		final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph = ftfTracker.getResult();
		model.setTracks( graph, true );

		// Track merging
		final Map< String, Object > settings2 = LAPUtils.getDefaultLAPSettingsMap();
		settings2.remove( KEY_LINKING_FEATURE_PENALTIES );
		settings2.remove( KEY_BLOCKING_VALUE );
		settings2.remove( KEY_LINKING_MAX_DISTANCE );

		settings2.put( KEY_ALLOW_GAP_CLOSING, false );
		settings2.put( KEY_ALLOW_TRACK_MERGING, true );
		settings2.put( KEY_ALLOW_TRACK_SPLITTING, true );


		final JaqamanSegmentCostMatrixCreator costMatrixCreator = new JaqamanSegmentCostMatrixCreator( graph, settings2 );
		if ( !costMatrixCreator.checkInput() || !costMatrixCreator.process() )
		{
			System.err.println( costMatrixCreator.getErrorMessage() );
			return;
		}

		System.out.println( costMatrixCreator.getResult().toString( costMatrixCreator.getSourceList(), costMatrixCreator.getTargetList() ) );
		System.out.println( "Generated in " + costMatrixCreator.getProcessingTime() + " ms." );

		//		final SelectionModel sm = new SelectionModel( model );
		//		final TrackScheme trackScheme = new TrackScheme( model, sm );
		//		trackScheme.render();
		//		ImageJ.main( args );
		//		final HyperStackDisplayer view = new HyperStackDisplayer( model, sm );
		//		view.render();
	}

	private static final Comparator< Spot > idComparator = new Comparator< Spot >()
			{
		@Override
		public int compare( final Spot o1, final Spot o2 )
		{
			return o1.ID() - o2.ID();
		}
			};
}
