/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2022 TrackMate developers.
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

import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_KALMAN_SEARCH_RADIUS;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_KALMAN_SEARCH_RADIUS;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.gui.components.tracker.AdvancedKalmanTrackerSettingsPanel;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;
import fiji.plugin.trackmate.tracking.jaqaman.LAPUtils;
import fiji.plugin.trackmate.tracking.jaqaman.SegmentTrackerFactory;

/***
 * Kalman tracker factory with features cost addition and segment splitting /
 * merging.
 * 
 * @author G. Letort (Institut Pasteur)
 */
@Plugin( type = SpotTrackerFactory.class )
public class AdvancedKalmanTrackerFactory extends SegmentTrackerFactory
{

	public static final String THIS_TRACKER_KEY = "ADVANCED_KALMAN_TRACKER";

	public static final String THIS_NAME = "Advanced Kalman Tracker";

	public static final String THIS_INFO_TEXT = "<html>"
			+ "This tracker is an extended version of the Kalman tracker, that adds "
			+ "the possibility to customize linking costs and detect track fusion "
			+ "(segments merging) and track division (segments splitting). "
			+ "<p> "
			+ "This tracker is especially well suited to objects that move following "
			+ "a nearly constant velocity vector. The velocity vectors of each object "
			+ "can be completely different from one another. But for the velocity "
			+ "vector of one object need not to change too much from one frame to another. "
			+ "<p> "
			+ "In the frame-to-frame linking step, the classic Kalman tracker "
			+ "infer most likely spot positions in the target frame from growing "
			+ "tracks and link all extrapolated positions against all spots in the "
			+ "target frame, based on the square distance. "
			+ "This advanced version of the tracker allows for penalizing "
			+ "links to spots with different features values "
			+ "using the same framework that of the LAP tracker in TrackMate. "
			+ "Also, after the frame-to-frame linking step, track segments are "
			+ "post-processed to detect splitting and merging events, and perform "
			+ "gap-closing. This is again based on the LAP tracker implementation. "
			+ "</html>";

	@Override
	public String getInfoText()
	{
		return THIS_INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getKey()
	{
		return THIS_TRACKER_KEY;
	}

	@Override
	public String getName()
	{
		return THIS_NAME;
	}

	@Override
	public SpotTracker create( final SpotCollection spots, final Map< String, Object > settings )
	{
		return new AdvancedKalmanTracker( spots, settings );
	}

	@Override
	public AdvancedKalmanTrackerFactory copy()
	{
		return new AdvancedKalmanTrackerFactory();
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > settings = LAPUtils.getDefaultSegmentSettingsMap();
		settings.put( KEY_LINKING_MAX_DISTANCE, DEFAULT_LINKING_MAX_DISTANCE );
		settings.put( KEY_KALMAN_SEARCH_RADIUS, DEFAULT_KALMAN_SEARCH_RADIUS );
		settings.put( KEY_LINKING_FEATURE_PENALTIES, new HashMap<>( DEFAULT_LINKING_FEATURE_PENALTIES ) );
		return settings;
	}

	@Override
	public ConfigurationPanel getTrackerConfigurationPanel( final Model model )
	{
		final String spaceUnits = model.getSpaceUnits();
		final Collection< String > features = model.getFeatureModel().getSpotFeatures();
		final Map< String, String > featureNames = model.getFeatureModel().getSpotFeatureNames();
		return new AdvancedKalmanTrackerSettingsPanel( getName(), spaceUnits, features, featureNames );
	}
}
