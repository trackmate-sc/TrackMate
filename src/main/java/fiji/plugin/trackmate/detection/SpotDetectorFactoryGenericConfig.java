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
package fiji.plugin.trackmate.detection;

import java.util.Map;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.util.cli.Configurator;
import fiji.plugin.trackmate.util.cli.FactoryGenericConfig;
import fiji.plugin.trackmate.util.cli.GenericDetectionConfigurationPanel;
import fiji.plugin.trackmate.util.cli.TrackMateSettingsBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Interface for detector factories that need to be configured with a
 * {@link Configurator} instance.
 *
 * @author Jean-Yves Tinevez
 *
 * @param <T>
 *            the type of pixels in the input image, which must implement
 *            {@link RealType} and {@link NativeType}.
 * @param <C>
 *            the type of {@link Configurator} used to configure the detector
 *            factory.
 */
public interface SpotDetectorFactoryGenericConfig< T extends RealType< T > & NativeType< T >, C extends Configurator > extends SpotDetectorFactoryBase< T >, FactoryGenericConfig< C >
{

	@Override
	public default ConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		final C config = getConfigurator( settings.imp );
		return new GenericDetectionConfigurationPanel(
				settings,
				model,
				config,
				getName(),
				getIcon(),
				getUrl(),
				() -> this );
	}

	@Override
	default Map< String, Object > getDefaultSettings()
	{
		return TrackMateSettingsBuilder.getDefaultSettings( getConfigurator() );
	}
}
