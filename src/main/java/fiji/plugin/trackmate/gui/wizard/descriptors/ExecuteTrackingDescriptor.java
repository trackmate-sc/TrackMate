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
package fiji.plugin.trackmate.gui.wizard.descriptors;

import java.util.IntSummaryStatistics;

import org.scijava.Cancelable;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.gui.components.LogPanel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor;

public class ExecuteTrackingDescriptor extends WizardPanelDescriptor
{

	public static final String KEY = "ExecuteTracking";

	private final TrackMate trackmate;

	private final DisplaySettings displaySettings;

	public ExecuteTrackingDescriptor( final TrackMate trackmate, final LogPanel logPanel, final DisplaySettings displaySettings )
	{
		super( KEY );
		this.trackmate = trackmate;
		this.targetPanel = logPanel;
		this.displaySettings = displaySettings;
	}

	@Override
	public Runnable getForwardRunnable()
	{
		return () -> {
			final long start = System.currentTimeMillis();
			trackmate.execTracking();
			final long end = System.currentTimeMillis();

			final Logger logger = trackmate.getModel().getLogger();
			logger.log( String.format( "Tracking done in %.1f s.\n", ( end - start ) / 1e3f ) );
			final TrackModel trackModel = trackmate.getModel().getTrackModel();
			final int nTracks = trackModel.nTracks( false );
			final IntSummaryStatistics stats = trackModel.unsortedTrackIDs( false ).stream()
					.mapToInt( id -> trackModel.trackSpots( id ).size() )
					.summaryStatistics();
			logger.log( "Found " + nTracks + " tracks.\n" );
			logger.log( String.format( "  - avg size: %.1f spots.\n", stats.getAverage() ) );
			logger.log( String.format( "  - min size: %d spots.\n", stats.getMin() ) );
			logger.log( String.format( "  - max size: %d spots.\n", stats.getMax() ) );

			// Possibly tweak display settings: color spots by track id.
			if ( displaySettings.getSpotColorByType() == TrackMateObject.DEFAULT )
				if ( displaySettings.getSpotColorByFeature().equals( FeatureUtils.USE_UNIFORM_COLOR_KEY ) )
					displaySettings.setSpotColorBy( TrackMateObject.TRACKS, TrackIndexAnalyzer.TRACK_INDEX );
		};
	}

	@Override
	public Cancelable getCancelable()
	{
		return trackmate;
	}
}
