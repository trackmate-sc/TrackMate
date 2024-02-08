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
package fiji.plugin.trackmate.visualization.hyperstack;

import java.awt.Graphics2D;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;

public abstract class TrackMatePainter< T extends Spot >
{

	protected final double[] calibration;

	protected final DisplaySettings displaySettings;

	protected final ImagePlus imp;

	public TrackMatePainter( final ImagePlus imp, final double[] calibration, final DisplaySettings displaySettings )
	{
		this.imp = imp;
		this.calibration = calibration;
		this.displaySettings = displaySettings;
	}

	public abstract int paint( final Graphics2D g2d, final T spot );

	/**
	 * Returns <code>true</code> if the specified bounding-box, shifted by the
	 * Specified amount, intersects with the display window.
	 *
	 * @param boundingBox
	 *            the bounding box, centered at (0,0), in physical coordinates.
	 * @param center
	 *            the center of the bounding-box, in physical coordinates.
	 * @return if the specified bounding-box intersects with the display window.
	 */
	protected boolean intersect( final RealInterval boundingBox, final RealLocalizable center )
	{
		final ImageCanvas canvas = imp.getCanvas();
		if ( canvas == null )
			return false;

		if ( toScreenX( boundingBox.realMin( 0 ) + center.getDoublePosition( 0 ) ) > canvas.getWidth() )
			return false;
		if ( toScreenX( boundingBox.realMax( 0 ) + center.getDoublePosition( 0 ) ) < 0 )
			return false;
		if ( toScreenY( boundingBox.realMin( 1 ) + center.getDoublePosition( 1 ) ) > canvas.getHeight() )
			return false;
		if ( toScreenY( boundingBox.realMax( 1 ) + center.getDoublePosition( 1 ) ) < 0 )
			return false;
		return true;
	}

	/**
	 * Returns <code>true</code> if the specified bounding-box intersects with
	 * the display window.
	 *
	 * @param boundingBox
	 *            the bounding box, in physical coordinates.
	 * @return <code>true</code> if the specified bounding-box intersects with
	 *         the display window.
	 */
	protected boolean intersect( final RealInterval boundingBox )
	{
		final ImageCanvas canvas = imp.getCanvas();
		if ( canvas == null )
			return false;

		if ( toScreenX( boundingBox.realMin( 0 ) ) > canvas.getWidth() )
			return false;
		if ( toScreenX( boundingBox.realMax( 0 ) ) < 0 )
			return false;
		if ( toScreenY( boundingBox.realMin( 1 ) ) > canvas.getHeight() )
			return false;
		if ( toScreenY( boundingBox.realMax( 1 ) ) < 0 )
			return false;
		return true;
	}

	/**
	 * Converts a X position in physical units (possible um) to screen
	 * coordinates to be used with the graphics object.
	 *
	 * @param x
	 *            the X position to convert.
	 * @return the screen X coordinate.
	 */
	protected double toScreenX( final double x )
	{
		final ImageCanvas canvas = imp.getCanvas();
		if ( canvas == null )
			return Double.NaN;

		final double xp = x / calibration[ 0 ] + 0.5; // pixel coords
		return canvas.screenXD( xp );
	}

	/**
	 * Converts a Y position in physical units (possible um) to screen
	 * coordinates to be used with the graphics object.
	 *
	 * @param y
	 *            the Y position to convert.
	 * @return the screen Y coordinate.
	 */
	protected double toScreenY( final double y )
	{
		final ImageCanvas canvas = imp.getCanvas();
		if ( canvas == null )
			return Double.NaN;

		final double yp = y / calibration[ 0 ] + 0.5; // pixel coords
		return canvas.screenYD( yp );
	}

	protected void paintOutOfFocus( final Graphics2D g2d, final double xs, final double ys )
	{
		final double magnification = getMagnification();
		g2d.fillOval(
				( int ) Math.round( xs - 2 * magnification ),
				( int ) Math.round( ys - 2 * magnification ),
				( int ) Math.round( 4 * magnification ),
				( int ) Math.round( 4 * magnification ) );
	}

	protected double getMagnification()
	{
		if ( imp.getCanvas() == null )
			return 1.;
		return imp.getCanvas().getMagnification();
	}
}
