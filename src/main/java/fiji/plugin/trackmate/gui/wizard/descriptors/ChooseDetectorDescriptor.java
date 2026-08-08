/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
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

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.gui.GuiModel;
import fiji.plugin.trackmate.gui.components.ModuleChooserPanel;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor;
import fiji.plugin.trackmate.io.SettingsPersistence;
import fiji.plugin.trackmate.providers.DetectorProvider;

public class ChooseDetectorDescriptor extends WizardPanelDescriptor
{

	private static final String KEY = "ChooseDetector";

	private final DetectorProvider detectorProvider;

	private final GuiModel guiModel;

	public ChooseDetectorDescriptor( final DetectorProvider detectorProvider, final GuiModel guiModel )
	{
		super( KEY );
		this.detectorProvider = detectorProvider;
		this.guiModel = guiModel;

		String selectedDetector = LogDetectorFactory.DETECTOR_KEY; // default
		if ( null != guiModel.getSettings().detectorFactory )
			selectedDetector = guiModel.getSettings().detectorFactory.getKey();

		this.targetPanel = new ModuleChooserPanel<>( detectorProvider, "detector", selectedDetector );
	}

	private void setCurrentChoiceFromPlugin()
	{
		String key = LogDetectorFactory.DETECTOR_KEY; // back to default
		if ( null != guiModel.getSettings().detectorFactory )
			key = guiModel.getSettings().detectorFactory.getKey();

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
		final Model model = guiModel.getModel();
		final Settings settings = guiModel.getSettings();

		// Configure the detector provider with choice made in panel
		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final ModuleChooserPanel< SpotDetectorFactoryBase > component = ( fiji.plugin.trackmate.gui.components.ModuleChooserPanel< SpotDetectorFactoryBase > ) targetPanel;
		final String detectorKey = component.getSelectedModuleKey();

		// Configure trackmate settings with selected detector
		final SpotDetectorFactoryBase< ? > factory = detectorProvider.getFactory( detectorKey );

		if ( null == factory )
		{
			model.getLogger().error( "[ChooseDetectorDescriptor] Cannot find detector named " + detectorKey + " in current TrackMate modules." );
			return;
		}
		settings.detectorFactory = factory;

		/*
		 * Compare current settings with default ones, and substitute default
		 * ones only if the old ones are absent or not compatible with it.
		 */
		final Map< String, Object > currentSettings = settings.detectorSettings;
		if ( factory.checkSettings( currentSettings ) != null )
		{
			final String error = factory.checkSettings( currentSettings );
			if ( error == null )
			{
				settings.detectorSettings = currentSettings;
			}
			else
			{
				final Map< String, Object > defaultSettings = factory.getDefaultSettings();
				settings.detectorSettings = defaultSettings;
			}
		}

		// Settings persistence.
		SettingsPersistence.saveLastUsedSettings( settings, model.getLogger() );
	}

	@Override
	public Runnable getBackwardRunnable()
	{
		return () -> guiModel.getModel().clearSpots( true );
	}
}
