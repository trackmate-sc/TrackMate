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

import javax.swing.Action;

import fiji.plugin.trackmate.gui.components.ConfigureViewsPanel;
import fiji.plugin.trackmate.gui.components.FeatureDisplaySelector;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor;

public class ConfigureViewsDescriptor extends WizardPanelDescriptor
{

	public static final String KEY = "ConfigureViews";

	public ConfigureViewsDescriptor(
			final DisplaySettings ds,
			final FeatureDisplaySelector featureSelector,
			final Action launchBVVAction,
			final Action launchTrackSchemeAction,
			final Action showTrackTablesAction,
			final Action showSpotTableAction,
			final Action launchLabkitAction,
			final String spaceUnits )
	{
		super( KEY );
		this.targetPanel = new ConfigureViewsPanel(
				ds,
				featureSelector,
				spaceUnits,
				launchBVVAction,
				launchTrackSchemeAction,
				showTrackTablesAction,
				showSpotTableAction,
				launchLabkitAction );
	}
}
