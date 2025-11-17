/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2025 TrackMate developers.
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
package fiji.plugin.trackmate.tracking;

import java.util.Map;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.util.cli.Configurator;
import fiji.plugin.trackmate.util.cli.FactoryGenericConfig;
import fiji.plugin.trackmate.util.cli.GenericConfigurationPanel;
import fiji.plugin.trackmate.util.cli.TrackMateSettingsBuilder;

/**
 * Interface for tracker factories that need to be configured with a
 * {@link Configurator} instance.
 *
 * @author Jean-Yves Tinevez
 *
 * @param <C>
 *            the type of {@link Configurator} used to configure the detector
 *            factory.
 */
public interface SpotTrackerFactoryGenericConfig< C extends Configurator > extends SpotTrackerFactory, FactoryGenericConfig< C >
{

	@Override
	public default ConfigurationPanel getTrackerConfigurationPanel( final Model model )
	{
		final C config = getConfigurator();
		return new GenericConfigurationPanel(
				config,
				getName(),
				getIcon(),
				getUrl() );
	}

	@Override
	default Map< String, Object > getDefaultSettings()
	{
		return TrackMateSettingsBuilder.getDefaultSettings( getConfigurator() );
	}
}
