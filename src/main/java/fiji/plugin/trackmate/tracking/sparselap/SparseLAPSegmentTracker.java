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
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_MAX_DISTANCE;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.imglib2.algorithm.Benchmark;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.jonkervolgenant.JVSUtils;
import fiji.plugin.trackmate.tracking.sparselap.linker.CostFunction;

public class SparseLAPSegmentTracker implements SpotTracker, Benchmark
{

	private static final String BASE_ERROR_MESSAGE = "[SparseLAPSegmentTracker] ";

	private final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph;

	private final Map< String, Object > settings;

	private String errorMessage;

	private Logger logger;

	private long processingTime;

	public SparseLAPSegmentTracker( final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph, final Map< String, Object > settings )
	{
		this.graph = graph;
		this.settings = settings;
	}

	@Override
	public SimpleWeightedGraph< Spot, DefaultWeightedEdge > getResult()
	{
		return graph;
	}

	@Override
	public boolean checkInput()
	{
		return true;
	}

	@Override
	public boolean process()
	{
		/*
		 * Check input now.
		 */

		// Check that the objects list itself isn't null
		if ( null == graph )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The input graph is null.";
			return false;
		}

		// Check parameters
		final StringBuilder errorHolder = new StringBuilder();
		if ( !checkSettingsValidity( settings, errorHolder ) )
		{
			errorMessage = errorHolder.toString();
			return false;
		}

		/*
		 * Process.
		 */

		final long start = System.currentTimeMillis();

		final boolean doGapClosing = ( Boolean ) settings.get( KEY_ALLOW_GAP_CLOSING );
		final boolean doTrackSplitting = ( Boolean ) settings.get( KEY_ALLOW_TRACK_SPLITTING );
		final boolean doTrackMerging = ( Boolean ) settings.get( KEY_ALLOW_TRACK_MERGING );

		if ( !doGapClosing && doTrackSplitting && doTrackMerging )
		{
			// Do nothing.
			final long end = System.currentTimeMillis();
			processingTime = end - start;
			return true;
		}

		final GraphSegmentSplitter segmentSplitter = new GraphSegmentSplitter( graph );
		final List< Spot > segmentEnds = segmentSplitter.getSegmentEnds();
		final List< Spot > segmentStarts = segmentSplitter.getSegmentStarts();
		final List< List< Spot >> segmentMiddles = segmentSplitter.getSegmentMiddles();

		final Map< String, Double > gcFeaturePenalties = ( Map< String, Double > ) settings.get( KEY_GAP_CLOSING_FEATURE_PENALTIES );
		final CostFunction< Spot > gcCostFunction = getCostFunctionFor( gcFeaturePenalties );

		final Map< String, Double > tmFeaturePenalties = ( Map< String, Double > ) settings.get( KEY_MERGING_FEATURE_PENALTIES );
		final CostFunction< Spot > tmCostFunction = getCostFunctionFor( tmFeaturePenalties );

		final Map< String, Double > tsFeaturePenalties = ( Map< String, Double > ) settings.get( KEY_SPLITTING_FEATURE_PENALTIES );
		final CostFunction< Spot > tsCostFunction = getCostFunctionFor( tsFeaturePenalties );

		if ( !doTrackMerging && !doTrackSplitting )
		{
			// Gap-closing only
			final int maxFrameInterval = ( Integer ) settings.get( KEY_GAP_CLOSING_MAX_FRAME_GAP );
			final double maxDistance = ( Double ) settings.get( KEY_GAP_CLOSING_MAX_DISTANCE );
			final double costThreshold = maxDistance * maxDistance;

			// Array that stores whether a source, resp. a target, takes place
			// in at least one possible assignment.
			final boolean[] sourceHasCostArray = new boolean[ segmentEnds.size() ];
			final boolean[] targetHasCostArray = new boolean[ segmentStarts.size() ];

			// Storage for the possible assignment row and column. We store them
			// using Sudzik elegant pairing.
			final List< Long > keys = new ArrayList< Long >();
			// Corresponding costs.
			final List< Double > keyCosts = new ArrayList< Double >();

			// Loop over all sources and all targets, and find max cost.
			double maxCost = Double.NEGATIVE_INFINITY;
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

					if ( cost > maxCost )
					{
						maxCost = cost;
					}

					targetHasCostArray[ j ] = true;
					sourceHasCostArray[ i ] = true;

					final Long key = Long.valueOf( JVSUtils.szudzikPair( i, j ) );
					// They will be ordered column by column then line by line
					// in the list.
					keys.add( key );
					keyCosts.add( Double.valueOf( cost ) );
				}
			}

			System.out.println( "Found " + keys.size() + " possible assignments, over " + segmentEnds.size() + " x " + segmentStarts.size() + " combinations." );// DEBUG
		}

		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
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
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public void setLogger( final Logger logger )
	{
		this.logger = logger;
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
		ok = ok & checkParameter( settings, KEY_CUTOFF_PERCENTILE, Double.class, str );
		ok = ok & checkParameter( settings, KEY_ALTERNATIVE_LINKING_COST_FACTOR, Double.class, str );

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
		mandatoryKeys.add( KEY_BLOCKING_VALUE );
		final List< String > optionalKeys = new ArrayList< String >();
		optionalKeys.add( KEY_GAP_CLOSING_FEATURE_PENALTIES );
		optionalKeys.add( KEY_SPLITTING_FEATURE_PENALTIES );
		optionalKeys.add( KEY_MERGING_FEATURE_PENALTIES );
		ok = ok & checkMapKeys( settings, mandatoryKeys, optionalKeys, str );

		return ok;
	}


}
