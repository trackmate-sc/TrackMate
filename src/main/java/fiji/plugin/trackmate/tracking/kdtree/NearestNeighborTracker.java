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
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.scijava.Cancelable;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.graph.GraphUtils;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.util.Threads;
import net.imglib2.KDTree;
import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;
import net.imglib2.neighborsearch.KNearestNeighborSearchOnKDTree;

public class NearestNeighborTracker extends MultiThreadedBenchmarkAlgorithm implements SpotTracker, Cancelable
{

	/*
	 * FIELDS
	 */

	protected final SpotCollection spots;

	protected final Map< String, Object > settings;

	protected Logger logger = Logger.VOID_LOGGER;

	protected SimpleDirectedWeightedGraph< Spot, DefaultWeightedEdge > graph;

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
		final ExecutorService executors = Threads.newFixedThreadPool( numThreads );
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

					/*
					 * Create kD-Tree and NN search.
					 */
					final Iterable< Spot > targetSpots = spots.iterable( targetFrame, true );
					final KDTree< Spot > tree = new KDTree< Spot >( nTargetSpots, targetSpots, targetSpots );
					final KNearestNeighborSearchOnKDTree< Spot > search = new KNearestNeighborSearchOnKDTree<>( tree, nTargetSpots );

					/*
					 * For each spot in the source frame, find its nearest
					 * neighbor in the target frame.
					 */
					final Iterator< Spot > sourceIt = spots.iterator( sourceFrame, true );
					SOURCE: while ( sourceIt.hasNext() )
					{
						final Spot source = sourceIt.next();
						search.search( source );

						/*
						 * Loop over target spots in nearest neighbor order.
						 */
						int iNeighbor = -1;
						TARGET: while ( ++iNeighbor < nTargetSpots )
						{
							/*
							 * Is the closest we could find too far? If yes, we
							 * skip this source spot and do not create a link.
							 */
							final double squareDist = search.getSquareDistance( iNeighbor );
							if ( squareDist > maxDistSquare )
								continue SOURCE;

							/*
							 * Is the closest one already taken? Has it already
							 * an incoming edge?
							 */
							final Spot target = search.getSampler( iNeighbor ).get();
							if ( graph.inDegreeOf( target ) > 0 )
							{
								/*
								 * In that case we need to test the next nearest
								 * neighbor (next target spot).
								 */
								continue TARGET;
							}

							/*
							 * Everything is ok. This node is free and below max
							 * dist. We create a link and loop to the next
							 * source spot.
							 */
							synchronized ( graph )
							{
								final DefaultWeightedEdge edge = graph.addEdge( source, target );
								graph.setEdgeWeight( edge, squareDist );
							}
							break TARGET;
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
		return GraphUtils.convertToSimpleWeightedGraph( graph );
	}

	public void reset()
	{
		graph = new SimpleDirectedWeightedGraph<>( DefaultWeightedEdge.class );
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
