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