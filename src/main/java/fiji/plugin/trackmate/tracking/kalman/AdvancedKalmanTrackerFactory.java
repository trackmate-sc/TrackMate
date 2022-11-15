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
 * @brief Kalman Factory with features cost addition
 * @author G. Letort (Institut Pasteur)
 */
@Plugin( type = SpotTrackerFactory.class )
public class AdvancedKalmanTrackerFactory extends SegmentTrackerFactory
{

	public static final String THIS_TRACKER_KEY = "ADVANCED_KALMAN_TRACKER";

	public static final String THIS_NAME = "Advanced Kalman Tracker";

	public static final String THIS_INFO_TEXT = "<html>"
			+ "This tracker is based on the Linear Assignment Problem mathematical framework. <br>"
			+ "Its implementation is adapted from the following paper: <br>"
			+ "<i>Robust single-particle tracking in live-cell time-lapse sequences</i> - <br>"
			+ "Jaqaman <i> et al.</i>, 2008, Nature Methods. <br>"
			+ "<p>"
			+ "Tracking happens in 2 steps: First spots are linked from frame to frame to <br>"
			+ "build track segments. There is a gap-closing option to bridge spots several frames away to account for missing detections. <br>"
			+ " These track segments are investigated in a second step <br>"
			+ "for splitting (division) and merging (fusion) events.  <br> "
			+ "<p>" + "Linking costs are proportional to the square distance between source and  <br> "
			+ "target spots, which makes this tracker suitable for Brownian motion.  <br> "
			+ "Penalties can be set to favor linking between spots that have similar  <br> "
			+ "features. "
			+ "Then segments can be merged or split."
			+ "<p>"
			+ "Solving the LAP relies on the Jonker-Volgenant solver, and a sparse cost "
			+ "matrix formulation, allowing it to handle very large problems. "
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
