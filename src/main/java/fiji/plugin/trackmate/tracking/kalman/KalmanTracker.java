package fiji.plugin.trackmate.tracking.kalman;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;

import net.imglib2.RealPoint;
import net.imglib2.algorithm.Benchmark;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.sparselap.costfunction.CostFunction;
import fiji.plugin.trackmate.tracking.sparselap.costfunction.SquareDistCostFunction;
import fiji.plugin.trackmate.tracking.sparselap.costmatrix.JaqamanLinkingCostMatrixCreator;
import fiji.plugin.trackmate.tracking.sparselap.linker.JaqamanLinker;

public class KalmanTracker implements SpotTracker, Benchmark
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

	private boolean savePredictions = false;

	private SpotCollection predictionsCollection;

	private long processingTime;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * @param spots
	 *            the spots to track.
	 * @param maxSearchRadius
	 * @param maxFrameGap
	 * @param initialSearchRadius
	 */
	public KalmanTracker( final SpotCollection spots, final double maxSearchRadius, final int maxFrameGap, final double initialSearchRadius )
	{
		this.spots = spots;
		this.maxSearchRadius = maxSearchRadius;
		this.maxFrameGap = maxFrameGap;
		this.initialSearchRadius = initialSearchRadius;
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

		/*
		 * Outputs
		 */

		graph = new SimpleWeightedGraph< Spot, DefaultWeightedEdge >( DefaultWeightedEdge.class );
		predictionsCollection = new SpotCollection();

		/*
		 * Constants.
		 */

		// Max KF search cost.
		final double maxCost = maxSearchRadius * maxSearchRadius;
		// Cost function to nucleate KFs.
		final CostFunction< Spot, Spot > nucleatingCostFunction = new SquareDistCostFunction();
		// Max cost to nucleate KFs.
		final double maxInitialCost = initialSearchRadius * initialSearchRadius;

		// Find first and second non-empty frames.
		final NavigableSet< Integer > keySet = spots.keySet();
		final Iterator< Integer > frameIterator = keySet.iterator();
		final int firstFrame = frameIterator.next();
		if ( !frameIterator.hasNext() ) { return true; }
		final int secondFrame = frameIterator.next();

		/*
		 * Initialize. Find first links just based on square distance. We do
		 * this via the orphan spots lists.
		 */

		// Spots in the current frame that are not part of a new link (no
		// parent).
		Collection< Spot > orphanSpots = generateSpotList( spots, secondFrame );
		// Spots in the PREVIOUS frame that were not part of a link.
		Collection< Spot > previousOrphanSpots = generateSpotList( spots, firstFrame );

		/*
		 * Estimate Kalman filter variances.
		 * 
		 * The search radius is used to derive an estimate of the noise that
		 * affects position and velocity. The two are linked: if we need a large
		 * search radius, then the fluoctuations over predicted states are
		 * large.
		 */
		final double positionProcessStd = maxSearchRadius / 3d;
		final double velocityProcessStd = maxSearchRadius / 3d;
		/*
		 * We assume the detector did a good job and that positions measured are
		 * accurate up to a fraction of the spot radius
		 */

		double meanSpotRadius = 0d;
		for ( final Spot spot : orphanSpots )
		{
			meanSpotRadius += spot.getFeature( Spot.RADIUS ).doubleValue();
		}
		meanSpotRadius /= orphanSpots.size();
		final double positionMeasurementStd = meanSpotRadius / 10d;

		// The master map that contains the currently active KFs.
		final Map< CVMKalmanFilter, Spot > kalmanFiltersMap = new HashMap< CVMKalmanFilter, Spot >( orphanSpots.size() );

		/*
		 * Then loop over time, starting from second frame.
		 */
		int p = 1;
		for ( int frame = secondFrame; frame <= keySet.last(); frame++ )
		{
			p++;

			// Use the spot in the next frame has measurements.
			final List< Spot > measurements = generateSpotList( spots, frame );

			// Predict for all Kalman filters, and use it to generate linking
			// candidates.
			final Map< ComparableRealPoint, CVMKalmanFilter > predictionMap = new HashMap< ComparableRealPoint, CVMKalmanFilter >( kalmanFiltersMap.size() );
			for ( final CVMKalmanFilter kf : kalmanFiltersMap.keySet() )
			{
				final double[] X = kf.predict();
				final ComparableRealPoint point = new ComparableRealPoint( X );
				predictionMap.put( new ComparableRealPoint( X ), kf );

				if ( savePredictions )
				{
					final Spot pred = toSpot( point );
					final Spot s = kalmanFiltersMap.get( kf );
					pred.setName( "Pred_" + s.getName() );
					pred.putFeature( Spot.RADIUS, s.getFeature( Spot.RADIUS ) );
					predictionsCollection.add( pred, frame );
				}
			}
			final List< ComparableRealPoint > predictions = new ArrayList< ComparableRealPoint >( predictionMap.keySet() );

			// The KF for which we could not find a measurement in the target
			// frame. Is updated later.
			final Collection< CVMKalmanFilter > childlessKFs = new HashSet< CVMKalmanFilter >( kalmanFiltersMap.keySet() );

			// Find the global (in space) optimum for associating a prediction
			// to a measurement.

			if ( !predictions.isEmpty() && !measurements.isEmpty() )
			{
				// Only link measurements to predictions if we have predictions.

				final JaqamanLinkingCostMatrixCreator< ComparableRealPoint, Spot > crm = new JaqamanLinkingCostMatrixCreator< ComparableRealPoint, Spot >( predictions, measurements, CF, maxCost, ALTERNATIVE_COST_FACTOR, PERCENTILE );
				final JaqamanLinker< ComparableRealPoint, Spot > linker = new JaqamanLinker< ComparableRealPoint, Spot >( crm );
				if ( !linker.checkInput() || !linker.process() )
				{
					errorMessage = BASE_ERROR_MSG + "Error linking candidates in frame " + frame + ": " + linker.getErrorMessage();
					return false;
				}
				final Map< ComparableRealPoint, Spot > agnts = linker.getResult();
				final Map< ComparableRealPoint, Double > costs = linker.getAssignmentCosts();

				// Deal with found links.
				orphanSpots = new HashSet< Spot >( measurements );
				for ( final ComparableRealPoint cm : agnts.keySet() )
				{
					final CVMKalmanFilter kf = predictionMap.get( cm );

					// Create links for found match.
					final Spot source = kalmanFiltersMap.get( kf );
					final Spot target = agnts.get( cm );

					graph.addVertex( source );
					graph.addVertex( target );
					final DefaultWeightedEdge edge = graph.addEdge( source, target );
					final double cost = costs.get( cm );
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

				final JaqamanLinkingCostMatrixCreator< Spot, Spot > ic = new JaqamanLinkingCostMatrixCreator< Spot, Spot >( previousOrphanSpots, orphanSpots, nucleatingCostFunction, maxInitialCost, ALTERNATIVE_COST_FACTOR, PERCENTILE );
				final JaqamanLinker< Spot, Spot > newLinker = new JaqamanLinker< Spot, Spot >( ic );
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

				// We can bridge a limited number of gaps. If too much, we die.
				// If not, we will use predicted state next time.
				if ( kf.getNOcclusion() > maxFrameGap )
				{
					kalmanFiltersMap.remove( kf );
				}
			}

			final double progress = ( double ) p / keySet.size();
			logger.setProgress( progress );
		}

		if ( savePredictions )
		{
			predictionsCollection.setVisible( true );
		}

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
				spot.getDoublePosition( 0 ), spot.getDoublePosition( 1 ), spot.getDoublePosition( 2 )
		};
		return d;
	}

	private static final Spot toSpot( final ComparableRealPoint X )
	{
		final Spot spot = new Spot( X, 2d, -1d );
		return spot;
	}

	private static final double[] estimateInitialState( final Spot first, final Spot second )
	{
		final double[] xp = new double[] { second.getDoublePosition( 0 ), second.getDoublePosition( 1 ), second.getDoublePosition( 2 ),
				second.diffTo( first, Spot.POSITION_X ), second.diffTo( first, Spot.POSITION_Y ), second.diffTo( first, Spot.POSITION_Z ) };
		return xp;
	}

	private static final List< Spot > generateSpotList( final SpotCollection spots, final int frame )
	{
		final List< Spot > list = new ArrayList< Spot >( spots.getNSpots( frame, true ) );
		for ( final Iterator< Spot > iterator = spots.iterator( frame, true ); iterator.hasNext(); )
		{
			list.add( iterator.next() );
		}
		return list;
	}

	private static final class ComparableRealPoint extends RealPoint implements Comparable< ComparableRealPoint >
	{
		public ComparableRealPoint( final double[] A )
		{
			// Wrap array.
			super( A, false );
		}

		/**
		 * Sort based on X, Y, Z
		 */
		@Override
		public int compareTo( final ComparableRealPoint o )
		{
			int i = 0;
			while ( i < n )
			{
				if ( getDoublePosition( i ) != o.getDoublePosition( i ) ) { return ( int ) Math.signum( getDoublePosition( i ) - o.getDoublePosition( i ) ); }
				i++;
			}
			return 0;
		}
	}

	/**
	 * Cost function that returns the square distance between a KF state and a
	 * spots.
	 */
	private static final CostFunction< ComparableRealPoint, Spot > CF = new CostFunction< ComparableRealPoint, Spot >()
			{

		@Override
		public double linkingCost( final ComparableRealPoint state, final Spot spot )
		{
			final double dx = state.getDoublePosition( 0 ) - spot.getDoublePosition( 0 );
			final double dy = state.getDoublePosition( 1 ) - spot.getDoublePosition( 1 );
			final double dz = state.getDoublePosition( 2 ) - spot.getDoublePosition( 2 );
			return dx * dx + dy * dy + dz * dz + Double.MIN_NORMAL;
			// So that it's never 0
		}
			};


}
