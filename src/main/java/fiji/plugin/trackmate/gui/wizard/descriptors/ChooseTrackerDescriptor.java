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

import java.util.Map;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.components.ModuleChooserPanel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor;
import fiji.plugin.trackmate.io.SettingsPersistence;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;
import fiji.plugin.trackmate.tracking.jaqaman.SimpleSparseLAPTrackerFactory;

public class ChooseTrackerDescriptor extends WizardPanelDescriptor
{

	private static final String KEY = "ChooseTracker";

	private final TrackMate trackmate;

	private final TrackerProvider trackerProvider;

	private final DisplaySettings displaySettings;

	public ChooseTrackerDescriptor(
			final TrackerProvider trackerProvider,
			final TrackMate trackmate,
			final DisplaySettings displaySettings )
	{
		super( KEY );
		this.trackmate = trackmate;
		this.trackerProvider = trackerProvider;
		this.displaySettings = displaySettings;

		String selectedTracker = SimpleSparseLAPTrackerFactory.THIS2_TRACKER_KEY; // default
		if ( null != trackmate.getSettings().trackerFactory )
			selectedTracker = trackmate.getSettings().trackerFactory.getKey();

		this.targetPanel = new ModuleChooserPanel<>( trackerProvider, "tracker", selectedTracker );
	}

	private void setCurrentChoiceFromPlugin()
	{
		String key = SimpleSparseLAPTrackerFactory.THIS2_TRACKER_KEY; // default
		if ( null != trackmate.getSettings().trackerFactory )
			key = trackmate.getSettings().trackerFactory.getKey();

		@SuppressWarnings( "unchecked" )
		final ModuleChooserPanel< SpotTrackerFactory > component = ( fiji.plugin.trackmate.gui.components.ModuleChooserPanel< SpotTrackerFactory > ) targetPanel;
		component.setSelectedModuleKey( key );
	}

	@Override
	public void displayingPanel()
	{
		setCurrentChoiceFromPlugin();
	}

	@Override
	public void aboutToHidePanel()
	{
		// Configure the detector provider with choice made in panel
		@SuppressWarnings( "unchecked" )
		final ModuleChooserPanel< SpotTrackerFactory > component = ( fiji.plugin.trackmate.gui.components.ModuleChooserPanel< SpotTrackerFactory > ) targetPanel;
		final String trackerKey = component.getSelectedModuleKey();

		// Configure trackmate settings with selected detector
		final SpotTrackerFactory factory = trackerProvider.getFactory( trackerKey );

		if ( null == factory )
		{
			trackmate.getModel().getLogger().error( "[ChooseTrackerDescriptor] Cannot find tracker named " + trackerKey + " in current TrackMate modules." );
			return;
		}
		trackmate.getSettings().trackerFactory = factory;

		/*
		 * Compare current settings with default ones, and substitute default
		 * ones only if the old ones are absent or not compatible with it.
		 */
		final Map< String, Object > currentSettings = trackmate.getSettings().trackerSettings;
		if ( !factory.checkSettingsValidity( currentSettings ) )
		{
			final Map< String, Object > defaultSettings = factory.getDefaultSettings();
			trackmate.getSettings().trackerSettings = defaultSettings;
		}

		// Settings persistence.
		SettingsPersistence.saveLastUsedSettings( trackmate.getSettings(), trackmate.getModel().getLogger() );
	}

	@Override
	public Runnable getBackwardRunnable()
	{
		// Delete tracks and put back default coloring if needed.
		return () -> {
			if ( displaySettings.getSpotColorByType() == TrackMateObject.TRACKS
					|| displaySettings.getSpotColorByType() == TrackMateObject.EDGES )
				displaySettings.setSpotColorBy( TrackMateObject.DEFAULT, FeatureUtils.USE_UNIFORM_COLOR_KEY );
			trackmate.getModel().clearTracks( true );
		};
	}
}
