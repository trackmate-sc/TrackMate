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
package fiji.plugin.trackmate.gui.components.detector;

import java.util.Map;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.MaskDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.detection.ThresholdDetectorFactory;

/**
 * Configuration panel for spot detectors based on thresholding operations.
 * 
 * @author Jean-Yves Tinevez, 2020
 */
public class MaskDetectorConfigurationPanel extends ThresholdDetectorConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	/*
	 * CONSTRUCTOR
	 */

	public MaskDetectorConfigurationPanel(
			final Settings settings,
			final Model model )
	{
		super( settings, model, MaskDetectorFactory.INFO_TEXT, MaskDetectorFactory.NAME );
		ftfIntensityThreshold.setVisible( false );
		btnAutoThreshold.setVisible( false );
		lblIntensityThreshold.setVisible( false );
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final Map< String, Object > lSettings = super.getSettings();
		lSettings.remove( ThresholdDetectorFactory.KEY_INTENSITY_THRESHOLD );
		return lSettings;
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		setSettingsNonIntensity( settings );
	}

	/**
	 * Returns a new instance of the {@link SpotDetectorFactory} that this
	 * configuration panels configures. The new instance will in turn be used
	 * for the preview mechanism. Therefore, classes extending this class are
	 * advised to return a suitable implementation of the factory.
	 * 
	 * @return a new {@link SpotDetectorFactory}.
	 */
	@Override
	@SuppressWarnings( "rawtypes" )
	protected SpotDetectorFactory< ? > getDetectorFactory()
	{
		return new MaskDetectorFactory();
	}
}
