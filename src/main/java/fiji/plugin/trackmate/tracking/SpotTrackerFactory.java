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
package fiji.plugin.trackmate.tracking;

import java.util.Map;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateFactoryBase;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;

public interface SpotTrackerFactory extends TrackMateFactoryBase< SpotTrackerFactory >
{

	/**
	 * Instantiates and returns a new {@link SpotTracker} configured to operate
	 * on the specified {@link SpotCollection}, using the specified settins map.
	 *
	 * @param spots
	 *            the {@link SpotCollection} containing the spots to track.
	 * @param settings
	 *            the settings map configuring the tracker.
	 * @return a new {@link SpotTracker} instance.
	 */
	public SpotTracker create( final SpotCollection spots, final Map< String, Object > settings );

	/**
	 * Returns a new GUI panel able to configure the settings suitable for the
	 * target tracker identified by the key parameter.
	 *
	 * @param model
	 *            the model that will be modified by the target tracker.
	 * @return a new configuration panel.
	 */
	public ConfigurationPanel getTrackerConfigurationPanel( final Model model );

}
