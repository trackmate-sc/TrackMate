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

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_KALMAN_SEARCH_RADIUS;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.jaqaman.LAPUtils.checkFeatureMap;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.scijava.Cancelable;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.jaqaman.SegmentTracker;
import net.imglib2.algorithm.Benchmark;

/***
 * Kalman tracker factory with features cost addition and segment splitting /
 * merging.
 * 
 * @author G. Letort (Institut Pasteur)
 */
public class AdvancedKalmanTracker implements SpotTracker, Benchmark, Cancelable
{

	private static final String BASE_ERROR_MSG = "[AdvancedKalmanTracker] ";

	private SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph;

	private String errorMessage;

	private Logger logger = Logger.VOID_LOGGER;

	private final SpotCollection spots;

	protected final Map< String, Object > settings;

	private long processingTime;

	private boolean isCanceled;

	private String cancelReason;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * @param spots
	 *            the spots to track.
	 * @param settings
	 *            tracker specific settings list
	 */
	public AdvancedKalmanTracker( final SpotCollection spots, final Map< String, Object > settings )
	{
		this.spots = spots;
		this.settings = settings;
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
		 * Parameters specifiques au Kalman
		 */

		final StringBuilder errorHolder = new StringBuilder();
		// Build settings for Kalman filter part
		final Map< String, Object > kalSettings = new HashMap<>();
		kalSettings.put( KEY_KALMAN_SEARCH_RADIUS, settings.get( KEY_KALMAN_SEARCH_RADIUS ) );
		kalSettings.put( KEY_LINKING_MAX_DISTANCE, settings.get( KEY_LINKING_MAX_DISTANCE ) );
		kalSettings.put( KEY_GAP_CLOSING_MAX_FRAME_GAP, settings.get( KEY_GAP_CLOSING_MAX_FRAME_GAP ) );
		kalSettings.put( KEY_LINKING_FEATURE_PENALTIES, settings.get( KEY_LINKING_FEATURE_PENALTIES ) );

		// check these parameters
		if ( !checkSettingsValidity( kalSettings, errorHolder ) )
		{
			errorMessage = BASE_ERROR_MSG + errorHolder.toString();
			return false;
		}

		/*
		 * 1. Apply Kalman with feature penalties.
		 */
		final double maxSearchRadius = ( Double ) kalSettings.get( KEY_KALMAN_SEARCH_RADIUS );
		final double initialSearchRadius = ( Double ) kalSettings.get( KEY_LINKING_MAX_DISTANCE );
		final int maxFrameGap = ( Integer ) kalSettings.get( KEY_GAP_CLOSING_MAX_FRAME_GAP );
		@SuppressWarnings( "unchecked" )
		final Map< String, Double > featurePenalties = ( Map< String, Double > ) kalSettings.get( KEY_LINKING_FEATURE_PENALTIES );

		final KalmanTracker kalmanTracker = new KalmanTracker( spots, maxSearchRadius, maxFrameGap, initialSearchRadius, featurePenalties );
		kalmanTracker.setLogger( logger );
		if ( !kalmanTracker.checkInput() || !kalmanTracker.process() )
		{
			errorMessage = kalmanTracker.getErrorMessage();
			return false;
		}

		graph = kalmanTracker.getResult();

		/*
		 * 2. Merging and splitting.
		 */
		final SegmentTracker segmentLinker = new SegmentTracker( graph, settings, logger );
		if ( !segmentLinker.checkInput() || !segmentLinker.process() )
		{
			errorMessage = segmentLinker.getErrorMessage();
			return false;
		}

		this.processingTime = System.currentTimeMillis() - start;

		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
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

	protected boolean checkSettingsValidity( final Map< String, Object > settings, final StringBuilder str )
	{
		if ( null == settings )
		{
			str.append( "Settings map is null.\n" );
			return false;
		}

		boolean ok = true;
		final StringBuilder errorHolder = new StringBuilder();
		// Kalman
		ok = ok & checkParameter( settings, KEY_LINKING_MAX_DISTANCE, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_KALMAN_SEARCH_RADIUS, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_GAP_CLOSING_MAX_FRAME_GAP, Integer.class, errorHolder );
		ok = ok & checkFeatureMap( settings, KEY_LINKING_FEATURE_PENALTIES, errorHolder );
		// Check keys
		final List< String > mandatoryKeys = new ArrayList<>();
		mandatoryKeys.add( KEY_KALMAN_SEARCH_RADIUS );
		mandatoryKeys.add( KEY_LINKING_MAX_DISTANCE );
		mandatoryKeys.add( KEY_GAP_CLOSING_MAX_FRAME_GAP );

		final List< String > optionalKeys = new ArrayList<>();
		optionalKeys.add( KEY_LINKING_FEATURE_PENALTIES );
		ok = ok & checkMapKeys( settings, mandatoryKeys, optionalKeys, str );
		return ok;
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
