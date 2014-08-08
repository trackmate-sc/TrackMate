package fiji.plugin.trackmate.tracking.sparselap.costmatrix;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import fiji.plugin.trackmate.tracking.sparselap.GraphSegmentSplitter;
import fiji.plugin.trackmate.tracking.sparselap.ResizableDoubleArray;
import fiji.plugin.trackmate.tracking.sparselap.SparseLAPFrameToFrameTracker;
import fiji.plugin.trackmate.tracking.sparselap.costfunction.CostFunction;
import fiji.plugin.trackmate.tracking.sparselap.costfunction.FeaturePenaltyCostFunctionPercentile;
import fiji.plugin.trackmate.tracking.sparselap.costfunction.SquareDistCostFunctionPercentile;
import fiji.plugin.trackmate.tracking.sparselap.jonkervolgenant.SparseCostMatrix;

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
public class JaqamanSegmentCostMatrixCreator implements CostMatrixCreator< Spot, Spot >
{

	private static final String BASE_ERROR_MESSAGE = "[JaqamanSegmentCostMatrixCreator] ";

	private final Map< String, Object > settings;

	private String errorMessage;

	private SparseCostMatrix scm;

	private long processingTime;

	private List< Spot > uniqueSources;

	private List< Spot > uniqueTargets;

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
		final CostFunction< Spot, Spot > gcCostFunction = getCostFunctionFor( gcFeaturePenalties );
		final int maxFrameInterval = ( Integer ) settings.get( KEY_GAP_CLOSING_MAX_FRAME_GAP );
		final double gcMaxDistance = ( Double ) settings.get( KEY_GAP_CLOSING_MAX_DISTANCE );
		final double gcCostThreshold = gcMaxDistance * gcMaxDistance;
		final boolean allowGapClosing = ( Boolean ) settings.get( KEY_ALLOW_GAP_CLOSING );

		// Merging
		@SuppressWarnings( "unchecked" )
		final Map< String, Double > mFeaturePenalties = ( Map< String, Double > ) settings.get( KEY_MERGING_FEATURE_PENALTIES );
		final CostFunction< Spot, Spot > mCostFunction = getCostFunctionFor( mFeaturePenalties );
		final double mMaxDistance = ( Double ) settings.get( KEY_MERGING_MAX_DISTANCE );
		final double mCostThreshold = mMaxDistance * mMaxDistance;
		final boolean allowMerging = ( Boolean ) settings.get( KEY_ALLOW_TRACK_MERGING );

		// Splitting
		@SuppressWarnings( "unchecked" )
		final Map< String, Double > sFeaturePenalties = ( Map< String, Double > ) settings.get( KEY_SPLITTING_FEATURE_PENALTIES );
		final CostFunction< Spot, Spot > sCostFunction = getCostFunctionFor( sFeaturePenalties );
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
		}
		else
		{
			allMiddles = Collections.emptyList();
		}

		/*
		 * Sources and targets.
		 */
		final ArrayList< Spot > sources = new ArrayList< Spot >();
		final ArrayList< Spot > targets = new ArrayList< Spot >();
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
					linkCosts.add( cost );

				}
			}
		}

		/*
		 * Compute the alternative cost from the cost array
		 */

		linkCosts.trimToSize();
		alternativeCost = gcCostFunction.aternativeCost( linkCosts.data );

		/*
		 * Build a sparse cost matrix from this.
		 */

		final DefaultCostMatrixCreator< Spot, Spot > creator = new DefaultCostMatrixCreator< Spot, Spot >( sources, targets, linkCosts.data, alternativeCost );
		if ( !creator.checkInput() || !creator.process() )
		{
			errorMessage = creator.getErrorMessage();
			return false;
		}

		scm = creator.getResult();
		uniqueSources = creator.getSourceList();
		uniqueTargets = creator.getTargetList();

		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	protected CostFunction< Spot, Spot > getCostFunctionFor( final Map< String, Double > featurePenalties )
	{
		// Link Nick Perry original non sparse LAP framework.
		final double altCostFact = ( Double ) settings.get( KEY_ALTERNATIVE_LINKING_COST_FACTOR );
		final double percentileCutOff = ( Double ) settings.get( KEY_CUTOFF_PERCENTILE );
		final CostFunction< Spot, Spot > costFunction;
		if ( null == featurePenalties || featurePenalties.isEmpty() )
		{
			costFunction = new SquareDistCostFunctionPercentile( altCostFact, percentileCutOff );
		}
		else
		{
			costFunction = new FeaturePenaltyCostFunctionPercentile( featurePenalties, altCostFact, percentileCutOff );
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
	 * @return the list of Spot, such that <code>sourceList.get( i )</code> is
	 *         the spot corresponding to the row <code>i</code> in the generated
	 *         cost matrix.
	 * @see #getTargetList()
	 * @see #getResult()
	 */
	@Override
	public List< Spot > getSourceList()
	{
		return uniqueSources;
	}

	/**
	 * Returns the list of targets in the generated cost matrix.
	 * 
	 * @return the list of Spot, such that <code>targetList.get( j )</code> is
	 *         the spot corresponding to the column <code>j</code> in the
	 *         generated cost matrix.
	 * @see #getSourceList()
	 * @see #getResult()
	 */
	@Override
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
	@Override
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

}
