package fiji.plugin.trackmate.tracking.kdtree;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.scijava.Cancelable;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.util.TMUtils;
import net.imglib2.KDTree;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;

public class NearestNeighborTracker extends MultiThreadedBenchmarkAlgorithm implements SpotTracker, Cancelable
{

	/*
	 * FIELDS
	 */

	protected final SpotCollection spots;

	protected final Map< String, Object > settings;

	protected Logger logger = Logger.VOID_LOGGER;

	protected SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph;

	private boolean isCanceled;

	private String cancelReason;

	/*
	 * CONSTRUCTOR
	 */

	public NearestNeighborTracker( final SpotCollection spots, final Map< String, Object > settings )
	{
		this.spots = spots;
		this.settings = settings;
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public boolean checkInput()
	{
		final StringBuilder errrorHolder = new StringBuilder();
		final boolean ok = checkInput( settings, errrorHolder );
		if ( !ok )
			errorMessage = errrorHolder.toString();

		return ok;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		isCanceled = false;
		cancelReason = null;

		reset();

		final double maxLinkingDistance = ( Double ) settings.get( KEY_LINKING_MAX_DISTANCE );
		final double maxDistSquare = maxLinkingDistance * maxLinkingDistance;
		final TreeSet< Integer > frames = new TreeSet<>( spots.keySet() );

		// Prepare executors.
		final AtomicInteger progress = new AtomicInteger( 0 );
		final ExecutorService executors = Executors.newFixedThreadPool( numThreads );
		final List< Future< Void > > futures = new ArrayList<>( frames.size() );
		for ( int i = frames.first(); i < frames.last(); i++ )
		{
			final int frame = i;
			final Future< Void > future = executors.submit( new Callable< Void >()
			{

				@Override
				public Void call() throws Exception
				{
					if ( isCanceled() )
						return null;

					// Build frame pair
					final int sourceFrame = frame;
					final int targetFrame = frames.higher( frame );

					final int nTargetSpots = spots.getNSpots( targetFrame, true );
					if ( nTargetSpots < 1 )
					{
						logger.setProgress( progress.incrementAndGet() / ( double ) frames.size() );
						return null;
					}

					final List< RealPoint > targetCoords = new ArrayList<>( nTargetSpots );
					final List< FlagNode< Spot > > targetNodes = new ArrayList<>( nTargetSpots );
					final Iterator< Spot > targetIt = spots.iterator( targetFrame, true );
					while ( targetIt.hasNext() )
					{
						final double[] coords = new double[ 3 ];
						final Spot spot = targetIt.next();
						TMUtils.localize( spot, coords );
						targetCoords.add( new RealPoint( coords ) );
						targetNodes.add( new FlagNode<>( spot ) );
					}

					final KDTree< FlagNode< Spot > > tree = new KDTree<>( targetNodes, targetCoords );
					final NearestNeighborFlagSearchOnKDTree< Spot > search = new NearestNeighborFlagSearchOnKDTree<>( tree );

					/*
					 * For each spot in the source frame, find its nearest
					 * neighbor in the target frame.
					 */
					final Iterator< Spot > sourceIt = spots.iterator( sourceFrame, true );
					while ( sourceIt.hasNext() )
					{
						final Spot source = sourceIt.next();
						final double[] coords = new double[ 3 ];
						TMUtils.localize( source, coords );
						final RealPoint sourceCoords = new RealPoint( coords );
						search.search( sourceCoords );

						final double squareDist = search.getSquareDistance();
						final FlagNode< Spot > targetNode = search.getSampler().get();

						/*
						 * The closest we could find is too far. We skip this
						 * source spot and do not create a link
						 */
						if ( squareDist > maxDistSquare )
							continue;

						/*
						 * Everything is ok. This node is free and below max
						 * dist. We create a link and mark this node as
						 * assigned.
						 */

						targetNode.setVisited( true );
						synchronized ( graph )
						{
							final DefaultWeightedEdge edge = graph.addEdge( source, targetNode.getValue() );
							graph.setEdgeWeight( edge, squareDist );
						}
					}
					logger.setProgress( progress.incrementAndGet() / ( double ) frames.size() );
					return null;
				}
			} );
			futures.add( future );
		}

		logger.setStatus( "Tracking..." );
		logger.setProgress( 0 );

		try
		{
			for ( final Future< Void > future : futures )
				future.get();

			executors.shutdown();
		}
		catch ( InterruptedException | ExecutionException e )
		{
			e.printStackTrace();
			errorMessage = e.getMessage();
			return false;
		}
		finally
		{
			logger.setProgress( 1 );
			logger.setStatus( "" );

			final long end = System.currentTimeMillis();
			processingTime = end - start;
		}
		return true;
	}

	@Override
	public SimpleWeightedGraph< Spot, DefaultWeightedEdge > getResult()
	{
		return graph;
	}

	public void reset()
	{
		graph = new SimpleWeightedGraph<>( DefaultWeightedEdge.class );
		final Iterator< Spot > it = spots.iterator( true );
		while ( it.hasNext() )
			graph.addVertex( it.next() );
	}

	public static boolean checkInput( final Map< String, Object > settings, final StringBuilder errrorHolder )
	{
		boolean ok = checkParameter( settings, KEY_LINKING_MAX_DISTANCE, Double.class, errrorHolder );
		final List< String > mandatoryKeys = new ArrayList<>();
		mandatoryKeys.add( KEY_LINKING_MAX_DISTANCE );
		ok = ok & checkMapKeys( settings, mandatoryKeys, null, errrorHolder );
		return ok;
	}

	@Override
	public void setLogger( final Logger logger )
	{
		this.logger = logger;
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
