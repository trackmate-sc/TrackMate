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
package fiji.plugin.trackmate.visualization.trackscheme;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;

public class SpotImageUpdater
{

	private int previousFrame;

	private int previousChannel;

	private SpotIconGrabber< ? > grabber;

	private final Settings settings;

	/**
	 * Instantiates a new spot image updater.
	 *
	 * @param settings
	 *            the {@link Settings} object from which we read the raw image
	 *            and the target channel.
	 */
	public SpotImageUpdater( final Settings settings )
	{
		this.settings = settings;
		this.previousFrame = -1;
		this.previousChannel = -1;
	}

	/**
	 * Returns the image string of the given spot, based on the raw images
	 * contained in the given model. For performance, the image at target frame
	 * is stored for subsequent calls of this method. So it is a good idea to
	 * group calls to this method for spots that belong to the same frame.
	 *
	 * @param spot
	 *            the spot.
	 * @param radiusFactor
	 *            a factor that determines the size of the thumbnail. The
	 *            thumbnail will have a size equal to the spot diameter times
	 *            this radius.
	 * @return the image string.
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public String getImageString( final Spot spot, final double radiusFactor )
	{
		final int frame = spot.getFeature( Spot.FRAME ).intValue();
		final int targetChannel = settings.imp.getC() - 1;
		if ( frame == previousFrame && targetChannel == previousChannel )
		{
			// Keep the same image than in memory
		}
		else
		{
			final ImgPlus img = TMUtils.rawWraps( settings.imp );
			final ImgPlus< ? > imgCT = TMUtils.hyperSlice( img, targetChannel, frame );

			grabber = new SpotIconGrabber( imgCT );
			previousFrame = frame;
			previousChannel = targetChannel;
		}
		return grabber.getImageString( spot, radiusFactor );
	}
}
