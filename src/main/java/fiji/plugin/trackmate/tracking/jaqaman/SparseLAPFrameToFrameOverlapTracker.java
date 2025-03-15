package fiji.plugin.trackmate.tracking.jaqaman;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MIN_IOU;
import static fiji.plugin.trackmate.tracking.jaqaman.LAPUtils.checkFeatureMap;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.jaqaman.costfunction.CostFunction;
import fiji.plugin.trackmate.tracking.jaqaman.costfunction.OverlapFeaturePenaltyCostFunction;
import fiji.plugin.trackmate.tracking.jaqaman.costfunction.OverlapCostFunction;
import fiji.plugin.trackmate.util.Threads;
import fiji.plugin.trackmate.tracking.jaqaman.costmatrix.JaqamanLinkingCostMatrixCreator;

public class SparseLAPFrameToFrameOverlapTracker extends SparseLAPFrameToFrameTracker 
{

	/*
	 * CONSTRUCTOR
	 */
	
	
	public SparseLAPFrameToFrameOverlapTracker( final SpotCollection spots, final Map< String, Object > settings )
	{
		super(spots, settings);
	}

	/*
	 * METHODS
	 */

	 @Override
	public boolean process()
	{
		isCanceled = false;
		cancelReason = null;

		/*
		 * Check input now.
		 */

		// Check that the objects list itself isn't null
		if ( null == spots )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The spot collection is null.";
			return false;
		}

		// Check that the objects list contains inner collections.
		if ( spots.keySet().isEmpty() )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The spot collection is empty.";
			return false;
		}

		// Check that at least one inner collection contains an object.
		boolean empty = true;
		for ( final int frame : spots.keySet() )
		{
			if ( spots.getNSpots( frame, true ) > 0 )
			{
				empty = false;
				break;
			}
		}
		if ( empty )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The spot collection is empty.";
			return false;
		}
		// Check parameters
		final StringBuilder errorHolder = new StringBuilder();
		if ( !checkSettingsValidity( settings, errorHolder ) )
		{
			errorMessage = BASE_ERROR_MESSAGE + errorHolder.toString();
			return false;
		}

		/*
		 * Process.
		 */

		final long start = System.currentTimeMillis();

		// Prepare frame pairs in order, not necessarily separated by 1.
		final ArrayList< int[] > framePairs = new ArrayList<>( spots.keySet().size() - 1 );
		final Iterator< Integer > frameIterator = spots.keySet().iterator();
		int frame0 = frameIterator.next();
		int frame1;
		while ( frameIterator.hasNext() )
		{ // ascending order
			frame1 = frameIterator.next();
			framePairs.add( new int[] { frame0, frame1 } );
			frame0 = frame1;
		}

		// Prepare cost function
		@SuppressWarnings( "unchecked" )
		final Map< String, Double > featurePenalties = ( Map< String, Double > ) settings.get( KEY_LINKING_FEATURE_PENALTIES );
		final CostFunction< Spot, Spot > costFunction = getCostFunction( featurePenalties );
		final Double minIOU = ( Double ) settings.get( KEY_LINKING_MIN_IOU );
		final double costThreshold = 1 - minIOU;
		final double alternativeCostFactor = ( Double ) settings.get( KEY_ALTERNATIVE_LINKING_COST_FACTOR );

		// Instantiate graph
		graph = new SimpleWeightedGraph<>( DefaultWeightedEdge.class );

		// Prepare workers.
		final AtomicInteger progress = new AtomicInteger( 0 );
		final AtomicBoolean ok = new AtomicBoolean( true );
		final ExecutorService executors = Threads.newFixedThreadPool( numThreads );
		final List< Future< Void > > futures = new ArrayList<>( framePairs.size() );
		for ( final int[] framePair : framePairs )
		{
			final Future< Void > future = executors.submit( new Callable< Void >()
			{

				@Override
				public Void call() throws Exception
				{
					if ( !ok.get() || isCanceled() )
						return null;

					// Get frame pairs
					final int lFrame0 = framePair[ 0 ];
					final int lFrame1 = framePair[ 1 ];

					// Get spots - we have to create a list from each
					// content.
					final List< Spot > sources = new ArrayList<>( spots.getNSpots( lFrame0, true ) );
					for ( final Iterator< Spot > iterator = spots.iterator( lFrame0, true ); iterator.hasNext(); )
						sources.add( iterator.next() );

					final List< Spot > targets = new ArrayList<>( spots.getNSpots( lFrame1, true ) );
					for ( final Iterator< Spot > iterator = spots.iterator( lFrame1, true ); iterator.hasNext(); )
						targets.add( iterator.next() );

					if ( sources.isEmpty() || targets.isEmpty() )
						return null;

					/*
					 * Run the linker.
					 */

					final JaqamanLinkingCostMatrixCreator< Spot, Spot > creator = new JaqamanLinkingCostMatrixCreator<>( sources, targets, costFunction, costThreshold, alternativeCostFactor, 1d );
					final JaqamanLinker< Spot, Spot > linker = new JaqamanLinker<>( creator );
					if ( !linker.checkInput() || !linker.process() )
					{
						errorMessage = "At frame " + lFrame0 + " to " + lFrame1 + ": " + linker.getErrorMessage();
						ok.set( false );
						return null;
					}

					/*
					 * Update graph.
					 */

					synchronized ( graph )
					{
						final Map< Spot, Double > costs = linker.getAssignmentCosts();
						final Map< Spot, Spot > assignment = linker.getResult();
						for ( final Spot source : assignment.keySet() )
						{
							final double cost = costs.get( source );
							final Spot target = assignment.get( source );
							graph.addVertex( source );
							graph.addVertex( target );
							final DefaultWeightedEdge edge = graph.addEdge( source, target );
							graph.setEdgeWeight( edge, cost );
						}
					}

					logger.setProgress( progress.incrementAndGet() / framePairs.size() );
					return null;
				}
			} );
			futures.add( future );
		}

		logger.setStatus( "Frame to frame linking..." );
		try
		{
			for ( final Future< ? > future : futures )
				future.get();

			executors.shutdown();
		}
		catch ( InterruptedException | ExecutionException e )
		{
			ok.set( false );
			errorMessage = BASE_ERROR_MESSAGE + e.getMessage();
			e.printStackTrace();
		}
		logger.setProgress( 1. );
		logger.setStatus( "" );

		final long end = System.currentTimeMillis();
		processingTime = end - start;

		return ok.get();
	}

	/**
	 * Creates a suitable cost function.
	 *
	 * @param featurePenalties
	 *            feature penalties to base costs on. Can be <code>null</code>.
	 * @return a new {@link CostFunction}
	 */
	protected CostFunction< Spot, Spot > getCostFunction( final Map< String, Double > featurePenalties )
	{
		if ( null == featurePenalties || featurePenalties.isEmpty() )
			return new OverlapCostFunction();

		return new OverlapFeaturePenaltyCostFunction( featurePenalties );
	}

	protected boolean checkSettingsValidity( final Map< String, Object > settings, final StringBuilder str )
	{
		if ( null == settings )
		{
			str.append( "Settings map is null.\n" );
			return false;
		}

		boolean ok = true;
		// Linking
		ok = ok & checkParameter( settings, KEY_LINKING_MIN_IOU, Double.class, str );
		ok = ok & checkFeatureMap( settings, KEY_LINKING_FEATURE_PENALTIES, str );
		// Others
		ok = ok & checkParameter( settings, KEY_ALTERNATIVE_LINKING_COST_FACTOR, Double.class, str );

		// Check keys
		final List< String > mandatoryKeys = new ArrayList<>();
		mandatoryKeys.add( KEY_LINKING_MIN_IOU );
		mandatoryKeys.add( KEY_ALTERNATIVE_LINKING_COST_FACTOR );
		final List< String > optionalKeys = new ArrayList<>();
		optionalKeys.add( KEY_LINKING_FEATURE_PENALTIES );
		ok = ok & checkMapKeys( settings, mandatoryKeys, optionalKeys, str );

		return ok;
	}
}
