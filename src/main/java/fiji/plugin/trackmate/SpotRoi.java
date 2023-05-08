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
package fiji.plugin.trackmate;

import java.util.Arrays;

import gnu.trove.list.array.TDoubleArrayList;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.roi.IterableRegion;
import net.imglib2.roi.Masks;
import net.imglib2.roi.Regions;
import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.roi.geom.real.WritablePolygon2D;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.RealType;

public class SpotRoi extends SpotBase
{

	/** Polygon points X coordinates, in physical units, centered (0,0). */
	public final double[] x;

	/** Polygon points Y coordinates, in physical units, centered (0,0). */
	public final double[] y;

	public SpotRoi(
			final double xc,
			final double yc,
			final double zc,
			final double r,
			final double quality,
			final String name,
			final double[] x,
			final double[] y )
	{
		super( xc, yc, zc, r, quality, name );
		this.x = x;
		this.y = y;
	}

	/**
	 * This constructor is only used for deserializing a model from a TrackMate
	 * file. It messes with the ID of the spots and should be not used
	 * otherwise.
	 * 
	 * @param ID
	 * @param x
	 * @param y
	 */
	public SpotRoi(
			final int ID,
			final double[] x,
			final double[] y )
	{
		super( ID ); 
		this.x = x;
		this.y = y;
	}

	@Override
	public SpotRoi copy()
	{
		final double xc = getDoublePosition( 0 );
		final double yc = getDoublePosition( 1 );
		final double zc = getDoublePosition( 2 );
		final double r = getFeature( Spot.RADIUS );
		final double quality = getFeature( Spot.QUALITY );
		return new SpotRoi( xc, yc, zc, r, quality, getName(), x.clone(), y.clone() );
	}

	/**
	 * Returns a new <code>int</code> array containing the X pixel coordinates
	 * to which to paint this polygon.
	 *
	 * @param calibration
	 *            the pixel size in X, to convert physical coordinates to pixel
	 *            coordinates.
	 * @param xcorner
	 *            the top-left X corner of the view in the image to paint.
	 * @param magnification
	 *            the magnification of the view.
	 * @return a new <code>int</code> array.
	 */
	public double[] toPolygonX( final double calibration, final double xcorner, final double spotXCenter, final double magnification )
	{
		final double[] xp = new double[ x.length ];
		for ( int i = 0; i < xp.length; i++ )
		{
			final double xc = ( spotXCenter + x[ i ] ) / calibration;
			xp[ i ] = ( xc - xcorner ) * magnification;
		}
		return xp;
	}

	/**
	 * Returns a new <code>int</code> array containing the Y pixel coordinates
	 * to which to paint this polygon.
	 *
	 * @param calibration
	 *            the pixel size in Y, to convert physical coordinates to pixel
	 *            coordinates.
	 * @param ycorner
	 *            the top-left Y corner of the view in the image to paint.
	 * @param magnification
	 *            the magnification of the view.
	 * @return a new <code>int</code> array.
	 */
	public double[] toPolygonY( final double calibration, final double ycorner, final double spotYCenter, final double magnification )
	{
		final double[] yp = new double[ y.length ];
		for ( int i = 0; i < yp.length; i++ )
		{
			final double yc = ( spotYCenter + y[ i ] ) / calibration;
			yp[ i ] = ( yc - ycorner ) * magnification;
		}
		return yp;
	}

	/**
	 * Writes the X AND Y pixel coordinates of the contour of the ROI inside a
	 * double list, cleared first when this method is called. Similar to
	 * {@link #toPolygonX(double, double, double, double)} and
	 * {@link #toPolygonY(double, double, double, double)} but allocation-free.
	 *
	 * @param calibration
	 *            the pixel sizes, to convert physical coordinates to pixel
	 *            coordinates.
	 * @param xcorner
	 *            the top-left X corner of the view in the image to paint.
	 * @param magnification
	 *            the magnification of the view.
	 * @param cx
	 *            the list in which to write the contour X coordinates. First
	 *            reset when called.
	 * @param cy
	 *            the list in which to write the contour Y coordinates. First
	 *            reset when called.
	 */
	public void toPolygon(
			final double calibration[],
			final double xcorner,
			final double ycorner,
			final double spotXCenter,
			final double spotYCenter,
			final double magnification,
			final TDoubleArrayList cx,
			final TDoubleArrayList cy )
	{
		cx.resetQuick();
		cy.resetQuick();
		for ( int i = 0; i < x.length; i++ )
		{
			final double xc = ( spotXCenter + x[ i ] ) / calibration[ 0 ];
			final double xp = ( xc - xcorner ) * magnification;
			cx.add( xp );
			final double yc = ( spotYCenter + y[ i ] ) / calibration[ 1 ];
			final double yp = ( yc - ycorner ) * magnification;
			cy.add( yp );
		}
	}
	
	@Override
	public < T extends RealType< T > > IterableInterval< T > iterable( final RandomAccessible< T > ra, final double[] calibration )
	{
		final double[] xp = toPolygonX( calibration[ 0 ], 0, this.getDoublePosition( 0 ), 1. );
		final double[] yp = toPolygonY( calibration[ 1 ], 0, this.getDoublePosition( 1 ), 1. );
		final WritablePolygon2D polygon = GeomMasks.closedPolygon2D( xp, yp );
		final IterableRegion< BoolType > region = Masks.toIterableRegion( polygon );
		return Regions.sample( region, ra );
	}

	private static double radius( final double[] x, final double[] y )
	{
		return Math.sqrt( area( x, y ) / Math.PI );
	}

	private static double area( final double[] x, final double[] y )
	{
		return Math.abs( signedArea( x, y ) );
	}

	public double area()
	{
		return area( x, y );
	}

	@Override
	public void scale( final double alpha )
	{
		for ( int i = 0; i < x.length; i++ )
		{
			final double x = this.x[ i ];
			final double y = this.y[ i ];
			final double r = Math.sqrt( x * x + y * y );
			final double costheta = x / r;
			final double sintheta = y / r;
			this.x[ i ] = costheta * r * alpha;
			this.y[ i ] = sintheta * r * alpha;
		}
	}

	public static SpotRoi createSpot( final double[] x, final double[] y, final double quality )
	{
		// Put polygon coordinates with respect to centroid.
		final double[] centroid = centroid( x, y );
		final double xc = centroid[ 0 ];
		final double yc = centroid[ 1 ];
		final double[] xr = Arrays.stream( x ).map( x0 -> x0 - xc ).toArray();
		final double[] yr = Arrays.stream( y ).map( y0 -> y0 - yc ).toArray();

		// Create spot.
		final double z = 0.;
		final double r = radius( xr, yr );
		return new SpotRoi( xc, yc, z, r, quality, null, xr, yr );
	}

	/*
	 * UTILS.
	 */

	private static final double[] centroid( final double[] x, final double[] y )
	{
		final double area = signedArea( x, y );
		double ax = 0.0;
		double ay = 0.0;
		final int n = x.length;
		for ( int i = 0; i < n - 1; i++ )
		{
			final double w = x[ i ] * y[ i + 1 ] - x[ i + 1 ] * y[ i ];
			ax += ( x[ i ] + x[ i + 1 ] ) * w;
			ay += ( y[ i ] + y[ i + 1 ] ) * w;
		}

		final double w0 = x[ n - 1 ] * y[ 0 ] - x[ 0 ] * y[ n - 1 ];
		ax += ( x[ n - 1 ] + x[ 0 ] ) * w0;
		ay += ( y[ n - 1 ] + y[ 0 ] ) * w0;
		return new double[] { ax / 6. / area, ay / 6. / area };
	}

	private static final double signedArea( final double[] x, final double[] y )
	{
		final int n = x.length;
		double a = 0.0;
		for ( int i = 0; i < n - 1; i++ )
			a += x[ i ] * y[ i + 1 ] - x[ i + 1 ] * y[ i ];

		return ( a + x[ n - 1 ] * y[ 0 ] - x[ 0 ] * y[ n - 1 ] ) / 2.0;
	}
}
