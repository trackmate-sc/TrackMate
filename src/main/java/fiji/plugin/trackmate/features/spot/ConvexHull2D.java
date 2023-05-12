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
package fiji.plugin.trackmate.features.spot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotRoi;

/**
 * Adapted from a code by Kirill Artemov,
 * https://github.com/DoctorGester/cia-stats.
 */
public final class ConvexHull2D
{

	private static List< Point > makeHull( final List< Point > points )
	{
		final List< Point > newPoints = new ArrayList<>( points );
		Collections.sort( newPoints );
		return makeHullPresorted( newPoints );
	}

	private static List< Point > makeHullPresorted( final List< Point > points )
	{
		if ( points.size() <= 1 )
			return new ArrayList<>( points );

		final List< Point > upperHull = new ArrayList<>();
		for ( final Point p : points )
		{
			while ( upperHull.size() >= 2 )
			{
				final Point q = upperHull.get( upperHull.size() - 1 );
				final Point r = upperHull.get( upperHull.size() - 2 );
				if ( ( q.x - r.x ) * ( p.y - r.y ) >= ( q.y - r.y ) * ( p.x - r.x ) )
					upperHull.remove( upperHull.size() - 1 );
				else
					break;
			}
			upperHull.add( p );
		}
		upperHull.remove( upperHull.size() - 1 );

		final List< Point > lowerHull = new ArrayList<>();
		for ( int i = points.size() - 1; i >= 0; i-- )
		{
			final Point p = points.get( i );
			while ( lowerHull.size() >= 2 )
			{
				final Point q = lowerHull.get( lowerHull.size() - 1 );
				final Point r = lowerHull.get( lowerHull.size() - 2 );
				if ( ( q.x - r.x ) * ( p.y - r.y ) >= ( q.y - r.y ) * ( p.x - r.x ) )
					lowerHull.remove( lowerHull.size() - 1 );
				else
					break;
			}
			lowerHull.add( p );
		}
		lowerHull.remove( lowerHull.size() - 1 );

		if ( !( upperHull.size() == 1 && upperHull.equals( lowerHull ) ) )
			upperHull.addAll( lowerHull );
		return upperHull;
	}

	private static final class Point implements Comparable< Point >
	{

		public final double x;

		public final double y;

		public Point( final double x, final double y )
		{
			this.x = x;
			this.y = y;
		}

		@Override
		public boolean equals( final Object obj )
		{
			if ( !( obj instanceof Point ) )
				return false;
			else
			{
				final Point other = ( Point ) obj;
				return x == other.x && y == other.y;
			}
		}

		@Override
		public int compareTo( final Point other )
		{
			if ( x != other.x )
				return Double.compare( x, other.x );
			else
				return Double.compare( y, other.y );
		}
	}

	public static SpotRoi convexHull( final SpotRoi roi )
	{
		final List< Point > points = new ArrayList<>( roi.nPoints() );
		for ( int i = 0; i < roi.nPoints(); i++ )
			points.add( new Point( roi.xr( i ), roi.yr( i ) ) );

		final List< Point > hull = makeHull( points );
		final double[] xhull = new double[ hull.size() ];
		final double[] yhull = new double[ hull.size() ];
		for ( int i = 0; i < yhull.length; i++ )
		{
			xhull[ i ] = hull.get( i ).x;
			yhull[ i ] = hull.get( i ).y;
		}
		final double xc = roi.getDoublePosition( 0 );
		final double yc = roi.getDoublePosition( 1 );
		final double zc = roi.getDoublePosition( 2 );
		final double r = roi.getFeature( Spot.RADIUS );
		final double quality = roi.getFeature( Spot.QUALITY );
		return new SpotRoi( xc, yc, zc, r, quality, roi.getName(), xhull, yhull );
	}
}
