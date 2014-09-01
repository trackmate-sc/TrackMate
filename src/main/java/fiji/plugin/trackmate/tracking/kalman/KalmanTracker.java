package fiji.plugin.trackmate.tracking.kalman;

import ij.ImagePlus;

import java.io.File;
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

import Jama.Matrix;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.sparselap.costfunction.CostFunction;
import fiji.plugin.trackmate.tracking.sparselap.costfunction.SquareDistCostFunction;
import fiji.plugin.trackmate.tracking.sparselap.costmatrix.JaqamanLinkingCostMatrixCreator;
import fiji.plugin.trackmate.tracking.sparselap.linker.JaqamanLinker;
import fiji.plugin.trackmate.visualization.SpotColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class KalmanTracker implements SpotTracker
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

	private final boolean savePredictions = true; // DEBUG

	private SpotCollection predictionsCollection;

	/*
	 * CONSTRUCTOR
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

		// The master map that contains the currently active KFs.
		final Map< LinearMotionKalmanTracker, Spot > kalmanFiltersMap = new HashMap< LinearMotionKalmanTracker, Spot >( orphanSpots.size() );

		/*
		 * Then loop over time, starting from second frame.
		 */
		for ( int frame = secondFrame; frame < keySet.last(); frame++ )
		{
			// Use the spot in the next frame has measurements.
			final List< Spot > measurements = generateSpotList( spots, frame );

			// Predict for all Kalman filters, and use it to generate linking
			// candidates.
			final Map< ComparableMatrix, LinearMotionKalmanTracker > predictionMap = new HashMap< KalmanTracker.ComparableMatrix, LinearMotionKalmanTracker >( kalmanFiltersMap.size() );
			for ( final LinearMotionKalmanTracker kf : kalmanFiltersMap.keySet() )
			{
				final Matrix X = kf.predict();
				predictionMap.put( new ComparableMatrix( X.getArray() ), kf );

				if ( savePredictions )
				{
					final Spot pred = toSpot( X );
					final Spot s = kalmanFiltersMap.get( kf );
					pred.setName( "Pred_" + s.getName() );
					pred.putFeature( Spot.RADIUS, s.getFeature( Spot.RADIUS ) );
					predictionsCollection.add( pred, frame );
				}
			}
			final List< ComparableMatrix > predictions = new ArrayList< ComparableMatrix >( predictionMap.keySet() );

			// The KF for which we could not find a measurement in the target
			// frame. Is updated later.
			final Collection< LinearMotionKalmanTracker > childlessKFs = new HashSet< LinearMotionKalmanTracker >( kalmanFiltersMap.keySet() );

			// Find the global (in space) optimum for associating a prediction
			// to a measurement.

			if ( !predictions.isEmpty() && !measurements.isEmpty() )
			{
				// Only link measurements to predictions if we have predictions.

				final JaqamanLinkingCostMatrixCreator< ComparableMatrix, Spot > crm = new JaqamanLinkingCostMatrixCreator< ComparableMatrix, Spot >( predictions, measurements, CF, maxCost, ALTERNATIVE_COST_FACTOR, PERCENTILE );
				final JaqamanLinker< ComparableMatrix, Spot > linker = new JaqamanLinker< ComparableMatrix, Spot >( crm );
				if ( !linker.checkInput() || !linker.process() )
				{
					errorMessage = BASE_ERROR_MSG + "Error linking candidates in frame " + frame + ": " + linker.getErrorMessage();
					return false;
				}
				final Map< ComparableMatrix, Spot > agnts = linker.getResult();
				final Map< ComparableMatrix, Double > costs = linker.getAssignmentCosts();

				// Deal with found links.
				orphanSpots = new HashSet< Spot >( measurements );
				for ( final ComparableMatrix cm : agnts.keySet() )
				{
					final LinearMotionKalmanTracker kf = predictionMap.get( cm );

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
			if ( !previousOrphanSpots.isEmpty() )
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
					final Matrix XP = estimateInitialState( source, target );
					final LinearMotionKalmanTracker kt = new LinearMotionKalmanTracker( XP );

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
			for ( final LinearMotionKalmanTracker kf : childlessKFs )
			{
				// Echo we missed a measurement
				kf.update( null );

				// We can bridge a limited number of gaps. If too much, we die.
				// If not, we will use predicted state next time.
				if ( kf.getnOcclusion() > maxFrameGap )
				{
					kalmanFiltersMap.remove( kf );
				}
			}
		}

		if ( savePredictions )
		{
			predictionsCollection.setVisible( true );
		}
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	public SpotCollection getPredictions()
	{
		return predictionsCollection;
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
	public void setLogger( final Logger logger )
	{
		this.logger = logger;
	}

	private static final Matrix toMeasurement( final Spot spot )
	{
		final double[] d = new double[] {
				spot.getDoublePosition( 0 ), spot.getDoublePosition( 1 ), spot.getDoublePosition( 2 )
		};
		return new Matrix( d, 3 );
	}

	private static final Spot toSpot( final Matrix X )
	{
		final Spot spot = new Spot( X.get( 0, 0 ), X.get( 1, 0 ), X.get( 2, 0 ), 2d, -1d );
		return spot;
	}

	private static final Matrix estimateInitialState( final Spot first, final Spot second )
	{
		final double[] xp = new double[] { second.getDoublePosition( 0 ), second.getDoublePosition( 1 ), second.getDoublePosition( 2 ),
				second.diffTo( first, Spot.POSITION_X ), second.diffTo( first, Spot.POSITION_Y ), second.diffTo( first, Spot.POSITION_Z ) };
		final Matrix XP = new Matrix( xp, 6 );
		return XP;
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

	/*
	 * MAIN METHODS
	 */

	public static void main( final String[] args )
	{
		final File file = new File( "samples/FakeTracks.xml" );
		final TmXmlReader reader = new TmXmlReader( file );
		if ( !reader.isReadingOk() )
		{
			System.out.println( reader.getErrorMessage() );
			return;
		}

		final Model model = reader.getModel();
		final SpotCollection spots = model.getSpots();

		final Settings settings = new Settings();
		reader.readSettings( settings, null, null, null, null, null );

		final KalmanTracker tracker = new KalmanTracker( spots, 15d, 2, 15d );
		if (!tracker.checkInput() || !tracker.process()	) {
			System.out.println(tracker.getErrorMessage());
			return;
		}

		final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph= tracker.getResult();
		model.setTracks( graph, false );

		ij.ImageJ.main( args );
		final SelectionModel selectionModel = new SelectionModel( model );
		final ImagePlus imp = settings.imp;
		final HyperStackDisplayer view = new HyperStackDisplayer( model, selectionModel, imp );
		final SpotColorGenerator scg = new SpotColorGenerator( model );
		scg.setFeature( Spot.QUALITY );
		view.setDisplaySettings( TrackMateModelView.KEY_SPOT_COLORING, scg );
		view.render();
	}

	private static final class ComparableMatrix extends Matrix implements Comparable< ComparableMatrix >
	{
		private static final long serialVersionUID = 1L;

		public ComparableMatrix( final double[][] A )
		{
			super( A );
		}

		/**
		 * Sort based on X, Y, Z
		 */
		@Override
		public int compareTo( final ComparableMatrix o )
		{
			int i = 0;
			while ( i < getRowDimension() )
			{
				if ( get( i, 0 ) != o.get( i, 0 ) ) { return ( int ) Math.signum( get( i, 0 ) - o.get( i, 0 ) ); }
				i++;
			}
			return 0;
		}
	}

	/**
	 * Cost function that returns the square distance between a KF state and a
	 * spots.
	 */
	private static final CostFunction< ComparableMatrix, Spot > CF = new CostFunction< ComparableMatrix, Spot >()
			{

		@Override
		public double linkingCost( final ComparableMatrix state, final Spot spot )
		{
			final double dx = state.get( 0, 0 ) - spot.getDoublePosition( 0 );
			final double dy = state.get( 1, 0 ) - spot.getDoublePosition( 1 );
			final double dz = state.get( 2, 0 ) - spot.getDoublePosition( 2 );
			return dx * dx + dy * dy + dz * dz + Double.MIN_NORMAL;
			// So that it's never 0
		}
			};

}
