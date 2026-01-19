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
package fiji.plugin.trackmate.util.cli;

import fiji.plugin.trackmate.TrackMateModule;
import fiji.plugin.trackmate.visualization.ViewUtils;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;

/**
 * Base interface for detector and tracker factories that need to be configured
 * with a {@link Configurator} instance.
 *
 * @author Jean-Yves Tinevez
 *
 * @param <C>
 *            the type of {@link Configurator} used to configure the factory.
 */
public interface FactoryGenericConfig< C extends Configurator > extends TrackMateModule
{

	/**
	 * Creates a new configurator for this detector factory, based on the
	 * specified image.
	 *
	 * @param imp
	 *            the input image to configure the detector for.
	 * @return a new {@link Configurator}.
	 */
	public C getConfigurator( ImagePlus imp );

	/**
	 * Creates a new configurator for this detector factory, based on the
	 * specified image.
	 *
	 * @param img
	 *            the input image to configure the detector for.
	 * @return a new {@link Configurator}.
	 */
	public default C getConfigurator( final ImgPlus< ? > img )
	{
		@SuppressWarnings( { "unchecked", "rawtypes" } )
		final ImagePlus imp = ImageJFunctions.wrap( ( ImgPlus ) img, "wrapped" );
		return getConfigurator( imp );
	}

	/**
	 * Creates a new configurator for this detector factory.
	 *
	 * @return a new {@link Configurator}.
	 */
	public default C getConfigurator()
	{
		final int  nZ = 2; // Force 3D
		final int nT = 2; // Force timelapse
		final double[] calibration = new double[] { 1., 1., 1. };
		final ImagePlus imp = ViewUtils.makeEmptyImagePlus( 32, 32, nZ, nT, calibration );
		return getConfigurator( imp );
	}
}
