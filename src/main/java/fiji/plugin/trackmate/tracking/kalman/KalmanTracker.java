/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.tracking.kalman;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.scijava.Cancelable;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.jaqaman.JaqamanLinker;
import fiji.plugin.trackmate.tracking.jaqaman.costfunction.CostFunction;
import fiji.plugin.trackmate.tracking.jaqaman.costfunction.FeaturePenaltyCostFunction;
import fiji.plugin.trackmate.tracking.jaqaman.costfunction.SquareDistCostFunction;
import fiji.plugin.trackmate.tracking.jaqaman.costmatrix.JaqamanLinkingCostMatrixCreator;
import net.imglib2.algorithm.Benchmark;

public class KalmanTracker implements SpotTracker, Benchmark, Cancelable
{

	private static final double ALTERNATIVE_COST_FACTOR = 1.05d;

	private static final double PERCENTILE = 1d;

	private static final String BASE_ERROR_MSG = "[KalmanTracker] ";

	private SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph;

	private String errorMessage;

	private Logger logger = Logger.VOID_LOGGER;

	private final SpotCollection spots;

	private final double maxSearchRadius;

	private final int maxFrameGap;

	private final double initialSearchRadius;

	private final Map< String, Double > featurePenalties;

	private boolean savePredictions = false;

	private SpotCollection predictionsCollection;

	private long processingTime;

	private boolean isCanceled;

	private String cancelReason;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * Creates a new Kalman tracker.
	 * 
	 * @param spots
	 *            the spots to track.
	 * @param maxSearchRadius
	 *            the maximal search radius to continue a track, in physical
	 *            units.
	 * @param maxFrameGap
	 *            the max frame gap when detections are missing, after which a
	 *            track will be stopped.
	 * @param initialSearchRadius
	 *            the initial search radius to nucleate new tracks.
	 * @param featurePenalties
	 *            the feature penalties.
	 */
	public KalmanTracker( final SpotCollection spots, final double maxSearchRadius, final int maxFrameGap, final double initialSearchRadius, final Map< String, Double > featurePenalties )
	{
		this.spots = spots;
		this.maxSearchRadius = maxSearchRadius;
		this.maxFrameGap = maxFrameGap;
		this.initialSearchRadius = initialSearchRadius;
		this.featurePenalties = featurePenalties;
	}

	/*
	 * PUBLIC METHODS
	 */

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
		final long start = System.currentTimeMillis();

		isCanceled = false;
		cancelReason = null;

		/*
		 * Outputs
		 */

		graph = new SimpleWeightedGraph<>( DefaultWeightedEdge.class );
		predictionsCollection = new SpotCollection();

		/*
		 * Constants.
		 */

		// Max KF search cost.
		final double maxCost = maxSearchRadius * maxSearchRadius;
		// Cost function to nucleate KFs.
		// final CostFunction< Spot, Spot > nucleatingCostFunction = new
		// SquareDistCostFunction();
		final CostFunction< Spot, Spot > nucleatingCostFunction = getCostFunction( featurePenalties );
		// Max cost to nucleate KFs.
		final double maxInitialCost = initialSearchRadius * initialSearchRadius;

		final CostFunction< Spot, Spot > costFunction = getCostFunction( featurePenalties );

		// Find first and second non-empty frames.
		final NavigableSet< Integer > keySet = spots.keySet();
		final Iterator< Integer > frameIterator = keySet.iterator();

		/*
		 * Initialize. Find first links just based on square distance. We do
		 * this via the orphan spots lists.
		 */

		// Spots in the PREVIOUS frame that were not part of a link.
		Collection< Spot > previousOrphanSpots = new ArrayList<>();
		if ( !frameIterator.hasNext() )
			return true;

		int firstFrame = frameIterator.next();
		while ( true )
		{
			previousOrphanSpots = generateSpotList( spots, firstFrame );
			if ( !frameIterator.hasNext() )
				return true;
			if ( !previousOrphanSpots.isEmpty() )
				break;

			firstFrame = frameIterator.next();
		}

		/*
		 * Spots in the current frame that are not part of a new link (no
		 * parent).
		 */
		Collection< Spot > orphanSpots = new ArrayList<>();
		int secondFrame = frameIterator.next();
		while ( true )
		{
			orphanSpots = generateSpotList( spots, secondFrame );
			if ( !frameIterator.hasNext() )
				return true;
			if ( !orphanSpots.isEmpty() )
				break;

			secondFrame = frameIterator.next();
		}

		/*
		 * Estimate Kalman filter variances.
		 *
		 * The search radius is used to derive an estimate of the noise that
		 * affects position and velocity. The two are linked: if we need a large
		 * search radius, then the fluctuations over predicted states are large.
		 */
		final double positionProcessStd = maxSearchRadius / 3d;
		final double velocityProcessStd = maxSearchRadius / 3d;
		/*
		 * We assume the detector did a good job and that positions measured are
		 * accurate up to a fraction of the spot radius
		 */

		double meanSpotRadius = 0d;
		for ( final Spot spot : orphanSpots )
			meanSpotRadius += spot.getFeature( Spot.RADIUS ).doubleValue();

		meanSpotRadius /= orphanSpots.size();
		final double positionMeasurementStd = meanSpotRadius / 10d;

		// The master map that contains the currently active KFs.
		final Map< CVMKalmanFilter, Spot > kalmanFiltersMap = new HashMap<>( orphanSpots.size() );

		/*
		 * Then loop over time, starting from second frame.
		 */
		int p = 1;
		for ( int frame = secondFrame; frame <= keySet.last(); frame++ )
		{
			if ( isCanceled() )
				return true; // It's ok to be canceled.

			p++;

			// Use the spot in the next frame has measurements.
			final List< Spot > measurements = generateSpotList( spots, frame );

			/*
			 * Predict for all Kalman filters, and use it to generate linking
			 * candidates.
			 */
			final Map< Spot, CVMKalmanFilter > predictionMap = new HashMap<>( kalmanFiltersMap.size() );
			for ( final CVMKalmanFilter kf : kalmanFiltersMap.keySet() )
			{
				final double[] X = kf.predict();
				final Spot s = kalmanFiltersMap.get( kf );
				final Spot predSpot = new SpotBase( X[ 0 ], X[ 1 ], X[ 2 ], s.getFeature( Spot.RADIUS ), s.getFeature( Spot.QUALITY ) );
				// copy the necessary features of original spot to the predicted
				// spot
				if ( null != featurePenalties )
					predSpot.copyFeaturesFrom( s, featurePenalties.keySet() );

				predictionMap.put( predSpot, kf );

				if ( savePredictions )
				{
					final Spot pred = new SpotBase( X[ 0 ], X[ 1 ], X[ 2 ], s.getFeature( Spot.RADIUS ), s.getFeature( Spot.QUALITY ) );
					pred.setName( "Pred_" + s.getName() );
					pred.putFeature( Spot.RADIUS, s.getFeature( Spot.RADIUS ) );
					predictionsCollection.add( predSpot, frame );
				}

			}
			final List< Spot > predictions = new ArrayList<>( predictionMap.keySet() );

			/*
			 * The KF for which we could not find a measurement in the target
			 * frame. Is updated later.
			 */
			final Collection< CVMKalmanFilter > childlessKFs = new HashSet<>( kalmanFiltersMap.keySet() );

			/*
			 * Find the global (in space) optimum for associating a prediction
			 * to a measurement.
			 */

			orphanSpots = new HashSet<>( measurements );
			if ( !predictions.isEmpty() && !measurements.isEmpty() )
			{
				// Only link measurements to predictions if we have predictions.
				final JaqamanLinkingCostMatrixCreator< Spot, Spot > crm = new JaqamanLinkingCostMatrixCreator<>(
						predictions,
						measurements,
						costFunction,
						maxCost,
						ALTERNATIVE_COST_FACTOR,
						PERCENTILE );
				final JaqamanLinker< Spot, Spot > linker = new JaqamanLinker<>( crm );
				if ( !linker.checkInput() || !linker.process() )
				{
					errorMessage = BASE_ERROR_MSG + "Error linking candidates in frame " + frame + ": " + linker.getErrorMessage();
					return false;
				}
				final Map< Spot, Spot > agnts = linker.getResult();
				final Map< Spot, Double > costs = linker.getAssignmentCosts();
				// Deal with found links.
				for ( final Spot spotty : agnts.keySet() )
				{
					final CVMKalmanFilter kf = predictionMap.get( spotty );

					// Create links for found match.
					final Spot source = kalmanFiltersMap.get( kf );
					final Spot target = agnts.get( spotty );

					graph.addVertex( source );
					graph.addVertex( target );
					final DefaultWeightedEdge edge = graph.addEdge( source, target );
					final double cost = costs.get( spotty );
					graph.setEdgeWeight( edge, cost );

					// Update Kalman filter
					kf.update( toMeasurement( target ) );

					// Update Kalman track spot
					kalmanFiltersMap.put( kf, target );

					// Remove from orphan set
					orphanSpots.remove( target );

					// Remove from childless KF set
					childlessKFs.remove( kf );
				}
			}

			/*
			 * Deal with orphans from the previous frame. (We deal with orphans
			 * from previous frame only now because we want to link in priority
			 * target spots to predictions. Nucleating new KF from nearest
			 * neighbor only comes second.
			 */
			if ( !previousOrphanSpots.isEmpty() && !orphanSpots.isEmpty() )
			{

				/*
				 * We now deal with orphans of the previous frame. We try to
				 * find them a target from the list of spots that are not
				 * already part of a link created via KF. That is: the orphan
				 * spots of this frame.
				 */

				final JaqamanLinkingCostMatrixCreator< Spot, Spot > ic = new JaqamanLinkingCostMatrixCreator<>(
						previousOrphanSpots,
						orphanSpots,
						nucleatingCostFunction,
						maxInitialCost,
						ALTERNATIVE_COST_FACTOR,
						PERCENTILE );
				final JaqamanLinker< Spot, Spot > newLinker = new JaqamanLinker<>( ic );
				if ( !newLinker.checkInput() || !newLinker.process() )
				{
					errorMessage = BASE_ERROR_MSG + "Error linking spots from frame " + ( frame - 1 ) + " to frame " + frame + ": " + newLinker.getErrorMessage();
					return false;
				}
				final Map< Spot, Spot > newAssignments = newLinker.getResult();
				final Map< Spot, Double > assignmentCosts = newLinker.getAssignmentCosts();

				// Build links and new KFs from these links.
				for ( final Spot source : newAssignments.keySet() )
				{
					final Spot target = newAssignments.get( source );

					// Remove from orphan collection.
					orphanSpots.remove( target );

					// Derive initial state and create Kalman filter.
					final double[] XP = estimateInitialState( source, target );
					final CVMKalmanFilter kt = new CVMKalmanFilter( XP, Double.MIN_NORMAL, positionProcessStd, velocityProcessStd, positionMeasurementStd );
					// We trust the initial state a lot.

					// Store filter and source
					kalmanFiltersMap.put( kt, target );

					// Add edge to the graph.
					graph.addVertex( source );
					graph.addVertex( target );
					final DefaultWeightedEdge edge = graph.addEdge( source, target );
					final double cost = assignmentCosts.get( source );
					graph.setEdgeWeight( edge, cost );
				}
			}
			previousOrphanSpots = orphanSpots;

			// Deal with childless KFs.
			for ( final CVMKalmanFilter kf : childlessKFs )
			{
				// Echo we missed a measurement
				kf.update( null );

				/*
				 * We can bridge a limited number of gaps. If too much, we die.
				 * If not, we will use predicted state next time.
				 */
				if ( kf.getNOcclusion() > maxFrameGap )
					kalmanFiltersMap.remove( kf );
			}

			final double progress = ( double ) p / keySet.size();
			logger.setProgress( progress );
		}

		if ( savePredictions )
			predictionsCollection.setVisible( true );

		final long end = System.currentTimeMillis();
		processingTime = end - start;

		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	/**
	 * Returns the saved predicted state as a {@link SpotCollection}.
	 *
	 * @return the predicted states.
	 * @see #setSavePredictions(boolean)
	 */
	public SpotCollection getPredictions()
	{
		return predictionsCollection;
	}

	/**
	 * Sets whether the tracker saves the predicted states.
	 *
	 * @param doSave
	 *            if <code>true</code>, the predicted states will be saved.
	 * @see #getPredictions()
	 */
	public void setSavePredictions( final boolean doSave )
	{
		this.savePredictions = doSave;
	}

	@Override
	public void setNumThreads()
	{}

	@Override
	public void setNumThreads( final int numThreads )
	{}

	@Override
	public int getNumThreads()
	{
		return 1;
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

	private static final double[] toMeasurement( final Spot spot )
	{
		final double[] d = new double[] {
				spot.getDoublePosition( 0 ),
				spot.getDoublePosition( 1 ),
				spot.getDoublePosition( 2 )
		};
		return d;
	}

	private static final double[] estimateInitialState( final Spot first, final Spot second )
	{
		final double[] xp = new double[] {
				second.getDoublePosition( 0 ),
				second.getDoublePosition( 1 ),
				second.getDoublePosition( 2 ),
				second.diffTo( first, Spot.POSITION_X ),
				second.diffTo( first, Spot.POSITION_Y ),
				second.diffTo( first, Spot.POSITION_Z )
		};
		return xp;
	}

	private static final List< Spot > generateSpotList( final SpotCollection spots, final int frame )
	{
		final List< Spot > list = new ArrayList<>( spots.getNSpots( frame, true ) );
		for ( final Iterator< Spot > iterator = spots.iterator( frame, true ); iterator.hasNext(); )
			list.add( iterator.next() );

		return list;
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
			return new SquareDistCostFunction();

		return new FeaturePenaltyCostFunction( featurePenalties );
	}

	// --- org.scijava.Cancelable methods ---

	@Override
	public boolean isCanceled()
	{
		return isCanceled;
	}

	@Override
	public void cancel( final String reason )
	{
		isCanceled = true;
		cancelReason = reason;
	}

	@Override
	public String getCancelReason()
	{
		return cancelReason;
	}
}
