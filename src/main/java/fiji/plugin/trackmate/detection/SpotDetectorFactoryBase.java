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

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateFactoryBase;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Mother interface for detector factories. It is also responsible for
 * loading/saving the detector parameters from/to XML, and of generating a
 * suitable configuration panel for GUI interaction. Several methods have
 * default implementations that cover most use cases, but subclasses are free to
 * override them if needed.
 */
public interface SpotDetectorFactoryBase< T extends RealType< T > & NativeType< T > > extends TrackMateFactoryBase< SpotDetectorFactoryBase< T > >
{

	/**
	 * Returns a new GUI panel able to configure the settings suitable for this
	 * specific detector factory.
	 *
	 * @param settings
	 *            the current settings, used to get info to display on the GUI
	 *            panel.
	 * @param model
	 *            the current model, used to get info to display on the GUI
	 *            panel.
	 * @return a new configuration panel.
	 */
	public ConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model );

	/**
	 * Return <code>true</code> for the detectors that can provide a spot with a
	 * 2D <code>SpotRoi</code> when they operate on 2D images.
	 * <p>
	 * This flag may be used by clients to exploit the fact that the spots
	 * created with this detector will have a contour that can be used
	 * <i>e.g.</i> to compute morphological features. The default is
	 * <code>false</code>, indicating that this detector provides spots as a X,
	 * Y, Z, radius tuple.
	 *
	 * @return <code>true</code> if the spots created by this detector have a 2D
	 *         contour.
	 */
	public default boolean has2Dsegmentation()
	{
		return false;
	}
}
