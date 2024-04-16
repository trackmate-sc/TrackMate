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

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor;
import fiji.plugin.trackmate.io.SettingsPersistence;
import fiji.plugin.trackmate.util.TMUtils;

public class SpotTrackerDescriptor extends WizardPanelDescriptor
{

	public static final String KEY = "ConfigureTracker";

	private final Settings settings;

	private final Logger logger;

	public SpotTrackerDescriptor( final Settings settings, final ConfigurationPanel configurationPanel, final Logger logger )
	{
		super( KEY );
		this.settings = settings;
		this.targetPanel = configurationPanel;
		this.logger = logger;
	}

	@Override
	public void aboutToHidePanel()
	{
		final ConfigurationPanel configurationPanel = ( ConfigurationPanel ) targetPanel;
		settings.trackerSettings = configurationPanel.getSettings();

		logger.log( "\nConfigured tracker " );
		logger.log( settings.trackerFactory.getName(), Logger.BLUE_COLOR );
		logger.log( " with settings:\n" );
		logger.log( TMUtils.echoMap( settings.trackerSettings, 2 ) + "\n" );

		// Settings persistence.
		SettingsPersistence.saveLastUsedSettings( settings, logger );
	}
}
