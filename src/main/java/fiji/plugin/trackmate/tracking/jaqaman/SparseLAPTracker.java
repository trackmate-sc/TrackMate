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
package fiji.plugin.trackmate.tracking.jaqaman;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;

import java.util.HashMap;
import java.util.Map;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.scijava.Cancelable;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Logger.SlaveLogger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.SpotTracker;
import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;

public class SparseLAPTracker extends MultiThreadedBenchmarkAlgorithm implements SpotTracker, Cancelable
{
	private final static String BASE_ERROR_MESSAGE = "[SparseLAPTracker] ";

	private SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph;

	private Logger logger = Logger.VOID_LOGGER;

	private final SpotCollection spots;

	private final Map< String, Object > settings;

	private boolean isCanceled;

	private String cancelReason;

	private Cancelable cancelable;

	/*
	 * CONSTRUCTOR
	 */

	public SparseLAPTracker( final SpotCollection spots, final Map< String, Object > settings )
	{
		this.spots = spots;
		this.settings = settings;
	}

	/*
	 * METHODS
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
		isCanceled = false;
		cancelReason = null;
		cancelable = null;

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

		/*
		 * Process: 1. Frame to frame linking...
		 */

		final long start = System.currentTimeMillis();

		// Prepare settings object
		final Map< String, Object > ftfSettings = new HashMap<>();
		ftfSettings.put( KEY_LINKING_MAX_DISTANCE, settings.get( KEY_LINKING_MAX_DISTANCE ) );
		ftfSettings.put( KEY_ALTERNATIVE_LINKING_COST_FACTOR, settings.get( KEY_ALTERNATIVE_LINKING_COST_FACTOR ) );
		ftfSettings.put( KEY_LINKING_FEATURE_PENALTIES, settings.get( KEY_LINKING_FEATURE_PENALTIES ) );

		final SparseLAPFrameToFrameTracker frameToFrameLinker = new SparseLAPFrameToFrameTracker( spots, ftfSettings );
		cancelable = frameToFrameLinker;
		frameToFrameLinker.setNumThreads( numThreads );
		final SlaveLogger ftfLogger = new SlaveLogger( logger, 0, 0.5 );
		frameToFrameLinker.setLogger( ftfLogger );

		if ( !frameToFrameLinker.checkInput() || !frameToFrameLinker.process() )
		{
			errorMessage = frameToFrameLinker.getErrorMessage();
			return false;
		}

		graph = frameToFrameLinker.getResult();
		cancelable = null;

		/*
		 * 2. Gap-closing, merging and splitting.
		 */
		final SegmentTracker segmentLinker = new SegmentTracker( graph, settings, logger );
		if ( !segmentLinker.checkInput() || !segmentLinker.process() )
		{
			errorMessage = segmentLinker.getErrorMessage();
			return false;
		}
		// graph = segmentLinker.getResult();

		logger.setStatus( "" );
		logger.setProgress( 1d );
		final long end = System.currentTimeMillis();
		processingTime = end - start;

		return true;
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
		if ( cancelable != null )
			cancelable.cancel( reason );
	}

	@Override
	public String getCancelReason()
	{
		return cancelReason;
	}
}
