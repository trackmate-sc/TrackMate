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
import java.util.Iterator;

import gnu.trove.list.array.TDoubleArrayList;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class SpotRoi extends SpotBase
{

	/** Polygon points X coordinates, in physical units, centered (0,0). */
	private final double[] x;

	/** Polygon points Y coordinates, in physical units, centered (0,0). */
	private final double[] y;

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
	 *            the ID to use when creating the spot.
	 * @param x
	 *            the spot contour X coordinates.
	 * @param y
	 *            the spot contour Y coordinates.
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
	 * Returns the X coordinates of the ith vertex of the polygon, in physical
	 * coordinates.
	 * 
	 * @param i
	 *            the index of the vertex.
	 * @return the vertex X position.
	 */
	public double x( final int i )
	{
		return x[ i ] + getDoublePosition( 0 );
	}

	/**
	 * Returns the Y coordinates of the ith vertex of the polygon, in physical
	 * coordinates.
	 * 
	 * @param i
	 *            the index of the vertex.
	 * @return the vertex Y position.
	 */
	public double y( final int i )
	{
		return y[ i ] + getDoublePosition( 1 );
	}

	/**
	 * Returns the X coordinates of the ith vertex of the polygon, <i>relative
	 * to the spot center</i>, in physical coordinates.
	 * 
	 * @param i
	 *            the index of the vertex.
	 * @return the vertex X position.
	 */
	public double xr( final int i )
	{
		return x[ i ];
	}

	/**
	 * Returns the Y coordinates of the ith vertex of the polygon, <i>relative
	 * to the spot center</i>, in physical coordinates.
	 * 
	 * @param i
	 *            the index of the vertex.
	 * @return the vertex Y position.
	 */
	public double yr( final int i )
	{
		return y[ i ];
	}

	public int nPoints()
	{
		return x.length;
	}

	@Override
	public double realMin( final int d )
	{
		final double[] arr = ( d == 0 ) ? x : y;
		return getDoublePosition( d ) + Util.min( arr );
	}

	@Override
	public double realMax( final int d )
	{
		final double[] arr = ( d == 0 ) ? x : y;
		return getDoublePosition( d ) + Util.max( arr );
	}

	/**
	 * Convenience method that returns the X and Y coordinates of the polygon on
	 * this spot, possibly shifted and scale by a specified amount. Such that:
	 * 
	 * <pre>
	 * xout = x * sx + cx
	 * yout = y * sy + cy
	 * </pre>
	 * 
	 * @param cx
	 *            the shift in X to apply after scaling coordinates.
	 * @param cy
	 *            the shift in Y to apply after scaling coordinates.
	 * @param sx
	 *            the scale to apply in X.
	 * @param sy
	 *            the scale to apply in Y.
	 * @param xout
	 *            a list in which to write resulting X coordinates. Reset by
	 *            this call.
	 * @param yout
	 *            a list in which to write resulting Y coordinates. Reset by
	 *            this call.
	 */
	public void toArray( final double cx, final double cy, final double sx, final double sy, final TDoubleArrayList xout, final TDoubleArrayList yout )
	{
		xout.resetQuick();
		yout.resetQuick();
		for ( int i = 0; i < x.length; i++ )
		{
			xout.add( x( i ) + sx + cx );
			yout.add( y( i ) + sx + cy );
		}
	}

	/**
	 * Convenience method that returns the X and Y coordinates of the polygon on
	 * this spot, possibly shifted and scale by a specified amount. Such that:
	 * 
	 * <pre>
	 * xout = x * sx + cx
	 * yout = y * sy + cy
	 * </pre>
	 * 
	 * @param cx
	 *            the shift in X to apply after scaling coordinates.
	 * @param cy
	 *            the shift in Y to apply after scaling coordinates.
	 * @param sx
	 *            the scale to apply in X.
	 * @param sy
	 *            the scale to apply in Y.
	 * @return a new 2D double array, with the array of X values as the first
	 *         element, and the array of Y values as a second element.
	 */
	public double[][] toArray( final double cx, final double cy, final double sx, final double sy )
	{
		final double[] xout = new double[ x.length ];
		final double[] yout = new double[ x.length ];
		for ( int i = 0; i < x.length; i++ )
		{
			xout[ i ] = x( i ) * sx + cx;
			yout[ i ] = y( i ) * sy + cy;
		}
		return new double[][] { xout, yout };
	}

	@Override
	public < T extends RealType< T > > IterableInterval< T > iterable( final RandomAccessible< T > ra, final double[] calibration )
	{
		return new SpotRoiIterable<>( this, ra, calibration );
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
		int i;
		int j;
		for ( i = 0, j = n - 1; i < n; j = i++ )
		{
			final double w = x[ j ] * y[ i ] - x[ i ] * y[ j ];
			ax += ( x[ j ] + x[ i ] ) * w;
			ay += ( y[ j ] + y[ i ] ) * w;
		}
		return new double[] { ax / 6. / area, ay / 6. / area };
	}

	private static final double signedArea( final double[] x, final double[] y )
	{
		final int n = x.length;
		double a = 0.0;
		int i;
		int j;
		for ( i = 0, j = n - 1; i < n; j = i++ )
			a += x[ j ] * y[ i ] - x[ i ] * y[ j ];

		return a / 2.;
	}

	/*
	 * ITERABLE and ITERATOR.
	 */

	private static final class SpotRoiIterable< T extends RealType< T > > implements IterableInterval< T >
	{

		private final FinalInterval interval;

		/** Polygon X coords in pixel units. */
		private final double[] x;

		/** Polygon Y coords in pixel units. */
		private final double[] y;

		private final RandomAccessible< T > ra;

		public SpotRoiIterable( final SpotRoi roi, final RandomAccessible< T > ra, final double[] calibration )
		{
			this.ra = ra;
			final double[][] xy = roi.toArray( 0., 0., 1 / calibration[ 0 ], 1 / calibration[ 1 ] );
			this.x = xy[ 0 ];
			this.y = xy[ 1 ];
			final long minX = ( long ) Math.floor( Util.min( x ) );
			final long maxX = ( long ) Math.ceil( Util.max( x ) );
			final long minY = ( long ) Math.floor( Util.min( y ) );
			final long maxY = ( long ) Math.ceil( Util.max( y ) );
			interval = Intervals.createMinMax( minX, minY, maxX, maxY );
		}

		@Override
		public long size()
		{
			int n = 0;
			final Cursor< T > cursor = cursor();
			while ( cursor.hasNext() )
			{
				cursor.fwd();
				n++;
			}
			return n;
		}

		@Override
		public T firstElement()
		{
			return cursor().next();
		}

		@Override
		public Object iterationOrder()
		{
			return this;
		}

		@Override
		public double realMin( final int d )
		{
			return interval.realMin( d );
		}

		@Override
		public double realMax( final int d )
		{
			return interval.realMax( d );
		}

		@Override
		public int numDimensions()
		{
			return 2;
		}

		@Override
		public long min( final int d )
		{
			return interval.min( d );
		}

		@Override
		public long max( final int d )
		{
			return interval.max( d );
		}

		@Override
		public Cursor< T > cursor()
		{
			return new MyCursor< T >( x, y, ra );
		}

		@Override
		public Cursor< T > localizingCursor()
		{
			return cursor();
		}

		@Override
		public Iterator< T > iterator()
		{
			return cursor();
		}
	}

	/**
	 * Iterates inside a close polygon given by X & Y in pixel coordinates.
	 * 
	 * @param <T>
	 *            the type of pixel in the image.
	 */
	private static final class MyCursor< T extends RealType< T > > implements Cursor< T >
	{

		private final FinalInterval interval;

		private Cursor< T > cursor;

		private final double[] x;

		private final double[] y;

		private boolean hasNext;

		private final RandomAccessible< T > rae;

		private RandomAccess< T > ra;

		public MyCursor( final double[] x, final double[] y, final RandomAccessible< T > rae )
		{
			this.x = x;
			this.y = y;
			this.rae = rae;
			final long minX = ( long ) Math.floor( Util.min( x ) );
			final long maxX = ( long ) Math.ceil( Util.max( x ) );
			final long minY = ( long ) Math.floor( Util.min( y ) );
			final long maxY = ( long ) Math.ceil( Util.max( y ) );
			interval = Intervals.createMinMax( minX, minY, maxX, maxY );
			reset();
		}

		@Override
		public T get()
		{
			return ra.get();
		}

		@Override
		public void fwd()
		{
			ra.setPosition( cursor );
			fetch();
		}

		private void fetch()
		{
			while ( cursor.hasNext() )
			{
				cursor.fwd();
				if ( isInside( cursor, x, y ) )
				{
					hasNext = cursor.hasNext();
					return;
				}
			}
			hasNext = false;
		}

		private static final boolean isInside( final Localizable localizable, final double[] x, final double[] y )
		{
			// Taken from Imglib2-roi GeomMaths. No edge case.
			final double xl = localizable.getDoublePosition( 0 );
			final double yl = localizable.getDoublePosition( 1 );

			int i;
			int j;
			boolean inside = false;
			for ( i = 0, j = x.length - 1; i < x.length; j = i++ )
			{
				final double xj = x[ j ];
				final double yj = y[ j ];

				final double xi = x[ i ];
				final double yi = y[ i ];

				if ( ( yi > yl ) != ( yj > yl ) && ( xl < ( xj - xi ) * ( yl - yi ) / ( yj - yi ) + xi ) )
					inside = !inside;
			}
			return inside;
		}

		@Override
		public void reset()
		{
			final IntervalView< T > view = Views.interval( rae, interval );
			cursor = view.localizingCursor();
			ra = rae.randomAccess( interval );
			fetch();
		}

		@Override
		public double getDoublePosition( final int d )
		{
			return ra.getDoublePosition( d );
		}

		@Override
		public int numDimensions()
		{
			return 2;
		}

		@Override
		public void jumpFwd( final long steps )
		{
			for ( int i = 0; i < steps; i++ )
				fwd();
		}

		@Override
		public boolean hasNext()
		{
			return hasNext;
		}

		@Override
		public T next()
		{
			fwd();
			return get();
		}

		@Override
		public long getLongPosition( final int d )
		{
			return ra.getLongPosition( d );
		}

		@Override
		public Cursor< T > copy()
		{
			return new MyCursor<>( x, y, rae );
		}

		@Override
		public Cursor< T > copyCursor()
		{
			return copy();
		}
	}
}
