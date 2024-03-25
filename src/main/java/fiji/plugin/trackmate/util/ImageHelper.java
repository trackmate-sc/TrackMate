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
package fiji.plugin.trackmate.util;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * Taken from
 * https://www.java-forums.org/blogs/ozzyman/1141-simple-frame-transitions-without-complex-code.html
 *
 * @author Ozzy
 */
public class ImageHelper
{

	/**
	 * Capture a Swing Component and return as a BufferedImage
	 *
	 * @param component
	 *            the component to capture.
	 * @return a new image.
	 */
	public static BufferedImage captureComponent( final Component component )
	{
		final BufferedImage image = new BufferedImage( component.getWidth(), component.getHeight(),
				BufferedImage.TYPE_INT_RGB );
		component.paint( image.getGraphics() );
		return image;
	}

	/**
	 * Some constant to define how I'd like to merge my images
	 */
	public static final int SIDE_BY_SIDE = 0;
	public static final int BOTTOM_TO_TOP = 1;

	/**
	 * Helper method to combine two images, in the specified format.
	 *
	 * @param img1
	 *            the first image to combine.
	 * @param img2
	 *            the second image to combine.
	 * @param renderHint
	 *            how to combine them.
	 * @return a new image.
	 * @see #SIDE_BY_SIDE
	 * @see #BOTTOM_TO_TOP
	 */
	public static BufferedImage combineImages( final BufferedImage img1, final BufferedImage img2, final int renderHint )
	{
		switch ( renderHint )
		{
		default:
		case SIDE_BY_SIDE:
		{
			/*
			 * Create a new image that is the width of img1+img2. Take the
			 * height of the taller image Paint the two images side-by-side.
			 */
			final BufferedImage combined = new BufferedImage( img1.getWidth() + img2.getWidth(),
					Math.max( img1.getHeight(), img2.getHeight() ), BufferedImage.TYPE_INT_RGB );
			final Graphics g = combined.getGraphics();
			g.drawImage( img1, 0, 0, null );
			g.drawImage( img2, img1.getWidth(), 0, null );
			return combined;
		}
		case BOTTOM_TO_TOP:
		{
			final BufferedImage combined = new BufferedImage(
					Math.max( img1.getWidth(), img2.getWidth() ),
					img1.getHeight() + img2.getHeight(),
					BufferedImage.TYPE_INT_RGB );
			final Graphics g = combined.getGraphics();
			g.drawImage( img1, 0, 0, null );
			g.drawImage( img2, 0, img1.getHeight(), null );
			return combined;
		}
		}
	}

}
