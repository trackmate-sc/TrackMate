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
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.gui.components.ModuleChooserPanel;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor;
import fiji.plugin.trackmate.io.SettingsPersistence;
import fiji.plugin.trackmate.providers.DetectorProvider;

public class ChooseDetectorDescriptor extends WizardPanelDescriptor
{

	private static final String KEY = "ChooseDetector";

	private final TrackMate trackmate;

	private final DetectorProvider detectorProvider;

	public ChooseDetectorDescriptor( final DetectorProvider detectorProvider, final TrackMate trackmate )
	{
		super( KEY );
		this.trackmate = trackmate;
		this.detectorProvider = detectorProvider;

		String selectedDetector = LogDetectorFactory.DETECTOR_KEY; // default
		if ( null != trackmate.getSettings().detectorFactory )
			selectedDetector = trackmate.getSettings().detectorFactory.getKey();

		this.targetPanel = new ModuleChooserPanel<>( detectorProvider, "detector", selectedDetector );
	}

	private void setCurrentChoiceFromPlugin()
	{
		String key = LogDetectorFactory.DETECTOR_KEY; // back to default
		if ( null != trackmate.getSettings().detectorFactory )
			key = trackmate.getSettings().detectorFactory.getKey();

		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final ModuleChooserPanel< SpotDetectorFactoryBase > component = ( fiji.plugin.trackmate.gui.components.ModuleChooserPanel< SpotDetectorFactoryBase > ) targetPanel;
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
		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final ModuleChooserPanel< SpotDetectorFactoryBase > component = ( fiji.plugin.trackmate.gui.components.ModuleChooserPanel< SpotDetectorFactoryBase > ) targetPanel;
		final String detectorKey = component.getSelectedModuleKey();

		// Configure trackmate settings with selected detector
		final SpotDetectorFactoryBase< ? > factory = detectorProvider.getFactory( detectorKey );

		if ( null == factory )
		{
			trackmate.getModel().getLogger().error( "[ChooseDetectorDescriptor] Cannot find detector named " + detectorKey + " in current TrackMate modules." );
			return;
		}
		trackmate.getSettings().detectorFactory = factory;

		/*
		 * Compare current settings with default ones, and substitute default
		 * ones only if the old ones are absent or not compatible with it.
		 */
		final Map< String, Object > currentSettings = trackmate.getSettings().detectorSettings;
		if ( !factory.checkSettings( currentSettings ) )
		{
			final Map< String, Object > defaultSettings = factory.getDefaultSettings();
			trackmate.getSettings().detectorSettings = defaultSettings;
		}

		// Settings persistence.
		SettingsPersistence.saveLastUsedSettings( trackmate.getSettings(), trackmate.getModel().getLogger() );
	}

	@Override
	public Runnable getBackwardRunnable()
	{
		return () -> trackmate.getModel().clearSpots( true );
	}
}
