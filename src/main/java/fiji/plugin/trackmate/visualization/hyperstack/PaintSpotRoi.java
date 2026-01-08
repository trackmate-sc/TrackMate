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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.function.DoubleUnaryOperator;

import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import gnu.trove.list.TDoubleList;
import ij.ImagePlus;

/**
 * Utility class to paint the {@link SpotRoi} component of spots.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class PaintSpotRoi extends TrackMatePainter< SpotRoi >
{

	private final java.awt.geom.Path2D.Double polygon;

	public PaintSpotRoi( final ImagePlus imp, final double[] calibration, final DisplaySettings displaySettings )
	{
		super( imp, calibration, displaySettings );
		this.polygon = new Path2D.Double();
	}

	/**
	 * Paint the specified spot using its {@link SpotRoi} field. The latter must
	 * not be <code>null</code>.
	 *
	 * @param g2d
	 *            the graphics object, configured to paint the spot with.
	 * @param spot
	 *            the spot to paint.
	 * @return the text position X indent in pixels to use to paint a string
	 *         next to the painted contour.
	 */
	@Override
	public int paint( final Graphics2D g2d, final SpotRoi spot )
	{
		if ( !intersect( spot ) )
			return -1;

		final double maxTextPos = toPolygon( spot, polygon, this::toScreenX, this::toScreenY );
		if ( displaySettings.isSpotFilled() )
		{
			g2d.fill( polygon );
			g2d.setColor( Color.BLACK );
			g2d.draw( polygon );
		}
		else
		{
			g2d.draw( polygon );
		}

		final double xs = toScreenX( spot.getDoublePosition( 0 ) );
		final int textPos = ( int ) ( maxTextPos - xs );
		return textPos;
	}

	static final double max( final TDoubleList l )
	{
		double max = Double.NEGATIVE_INFINITY;
		for ( int i = 0; i < l.size(); i++ )
		{
			final double v = l.get( i );
			if ( v > max )
				max = v;
		}
		return max;
	}

	/**
	 * Maps the coordinates of this contour to a Path2D polygon, and return the
	 * max X coordinate of the produced shape.
	 *
	 * @param contour
	 *            the contour to convert.
	 * @param polygon
	 *            the polygon to write. Reset by this call.
	 * @param toScreenX
	 *            a function to convert the X coordinate of this contour to
	 *            screen coordinates.
	 * @param toScreenY
	 *            a function to convert the Y coordinate of this contour to
	 *            screen coordinates.
	 * @return the max X position in screen units of this shape.
	 */
	private static final double toPolygon( final SpotRoi roi, final Path2D polygon, final DoubleUnaryOperator toScreenX, final DoubleUnaryOperator toScreenY )
	{
		double maxTextPos = Double.NEGATIVE_INFINITY;
		polygon.reset();
		final double x0 = toScreenX.applyAsDouble( roi.x( 0 ) );
		final double y0 = toScreenY.applyAsDouble( roi.y( 0 ) );
		polygon.moveTo( x0, y0 );
		if ( x0 > maxTextPos )
			maxTextPos = x0;

		for ( int i = 1; i < roi.nPoints(); i++ )
		{
			final double xi = toScreenX.applyAsDouble( roi.x( i ) );
			final double yi = toScreenY.applyAsDouble( roi.y( i ) );
			polygon.lineTo( xi, yi );

			if ( xi > maxTextPos )
				maxTextPos = xi;
		}
		polygon.closePath();
		return maxTextPos;
	}
}
