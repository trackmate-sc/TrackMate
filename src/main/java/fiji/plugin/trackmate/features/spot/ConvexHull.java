package fiji.plugin.trackmate.features.spot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fiji.plugin.trackmate.SpotRoi;

/**
 * Adapted from a code by Kirill Artemov,
 * https://github.com/DoctorGester/cia-stats.
 */
public final class ConvexHull
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
		final List< Point > points = new ArrayList<>( roi.x.length );
		for ( int i = 0; i < roi.x.length; i++ )
			points.add( new Point( roi.x[ i ], roi.y[ i ] ) );

		final List< Point > hull = makeHull( points );
		final double[] xhull = new double[ hull.size() ];
		final double[] yhull = new double[ hull.size() ];
		for ( int i = 0; i < yhull.length; i++ )
		{
			xhull[ i ] = hull.get( i ).x;
			yhull[ i ] = hull.get( i ).y;
		}
		return new SpotRoi( xhull, yhull );
	}
}
