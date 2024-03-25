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

import java.util.function.Function;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.components.InitFilterPanel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor;
import fiji.plugin.trackmate.io.SettingsPersistence;

public class InitFilterDescriptor extends WizardPanelDescriptor
{

	public static final String KEY = "InitialFiltering";

	private final TrackMate trackmate;

	public InitFilterDescriptor( final TrackMate trackmate, final FeatureFilter filter )
	{
		super( KEY );
		this.trackmate = trackmate;
		final Function< String, double[] > valuesCollector = key -> FeatureUtils.collectFeatureValues(
				Spot.QUALITY, TrackMateObject.SPOTS, trackmate.getModel(), false );
		this.targetPanel = new InitFilterPanel( filter, valuesCollector );
	}

	@Override
	public Runnable getForwardRunnable()
	{
		return new Runnable()
		{

			@Override
			public void run()
			{
				trackmate.getModel().getLogger().log( "\nComputing spot quality histogram...\n", Logger.BLUE_COLOR );
				final InitFilterPanel component = ( InitFilterPanel ) targetPanel;
				component.refresh();
			}
		};
	}

	@Override
	public void aboutToHidePanel()
	{
		final InitFilterPanel component = ( InitFilterPanel ) targetPanel;
		trackmate.getSettings().initialSpotFilterValue = component.getFeatureThreshold().value;

		// Settings persistence.
		SettingsPersistence.saveLastUsedSettings( trackmate.getSettings(), trackmate.getModel().getLogger() );
	}
}
