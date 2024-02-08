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
package fiji.plugin.trackmate.detection;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotRoi;
import ij.gui.PolygonRoi;
import ij.process.FloatPolygon;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.BooleanType;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

/**
 * Utility classes to create 2D {@link fiji.plugin.trackmate.SpotRoi}s from
 * single time-point, single channel images.
 * 
 * @author Jean-Yves Tinevez, 2023
 */
public class SpotRoiUtils
{

	/** Smoothing interval for ROIs. */
	private static final double SMOOTH_INTERVAL = 2.;

	/** Douglas-Peucker polygon simplification max distance. */
	private static final double DOUGLAS_PEUCKER_MAX_DISTANCE = 0.5;

	public static < T extends RealType< T > & NativeType< T >, S extends RealType< S > > List< Spot > from2DThresholdWithROI(
			final RandomAccessibleInterval< T > input,
			final double[] origin,
			final double[] calibration,
			final double threshold,
			final boolean simplify,
			final RandomAccessibleInterval< S > qualityImage )
	{
		final ImgLabeling< Integer, IntType > labeling = MaskUtils.toLabeling(
				input,
				threshold,
				1 );
		return from2DLabelingWithROI(
				labeling,
				origin,
				calibration,
				simplify,
				-1.,
				qualityImage );
	}

	/**
	 * Creates spots <b>with ROIs</b> from a <b>2D</b> label image. The quality
	 * value is read from a secondary image, by taking the max value in each
	 * ROI.
	 *
	 * @param <R>
	 *            the type that backs-up the labeling.
	 * @param <S>
	 *            the type of the quality image. Must be real, scalar.
	 * @param labeling
	 *            the labeling, must be zero-min and 2D.
	 * @param origin
	 *            the origin (min pos) of the interval the labeling was
	 *            generated from, used to reposition the spots from the zero-min
	 *            labeling to the proper coordinates.
	 * @param calibration
	 *            the physical calibration.
	 * @param simplify
	 *            if <code>true</code> the polygon will be post-processed to be
	 *            smoother and contain less points.
	 * @param smoothingScale
	 *            if strictly larger than 0, the mask will be smoothed before
	 *            creating the mesh, resulting in smoother meshes. The scale
	 *            value sets the (Gaussian) filter radius and is specified in
	 *            physical units. If 0 or lower than 0, no smoothing is applied.
	 * @param qualityImage
	 *            the image in which to read the quality value.
	 * @return a list of spots, with ROI.
	 */
	public static < R extends IntegerType< R >, S extends RealType< S > > List< Spot > from2DLabelingWithROI(
			final ImgLabeling< Integer, R > labeling,
			final double[] origin,
			final double[] calibration,
			final boolean simplify,
			final double smoothingScale,
			final RandomAccessibleInterval< S > qualityImage )
	{
		if ( labeling.numDimensions() != 2 )
			throw new IllegalArgumentException( "Can only process 2D images with this method, but got " + labeling.numDimensions() + "D." );

		final LabelRegions< Integer > regions = new LabelRegions< Integer >( labeling );

		final double[] sigmas = new double[ 2 ];
		for ( int d = 0; d < sigmas.length; d++ )
			sigmas[ d ] = smoothingScale / Math.sqrt( 2. ) / calibration[ d ];

		
		// Parse regions to create polygons on boundaries.
		final List< Polygon > polygons = new ArrayList<>( regions.getExistingLabels().size() );
		final Iterator< LabelRegion< Integer > > iterator = regions.iterator();
		while ( iterator.hasNext() )
		{
			final LabelRegion< Integer > region = iterator.next();

			// Possibly smooth labels.
			final RandomAccessibleInterval< BoolType > mask;
			if (smoothingScale > 0.)
			{
				// Filter.
				final Img< FloatType > filtered = Util.getArrayOrCellImgFactory( region, new FloatType() ).create( region );
				Gauss3.gauss( sigmas, region, filtered );

				// To mask.
				final double threshold = 0.5;
				final Converter< FloatType, BoolType > converter = ( a, b ) -> b.set( a.getRealDouble() > threshold );
				mask = Converters.convertRAI( filtered, converter, new BoolType() );
			}
			else
			{
				mask = region;
			}
			
			// Analyze in zero-min region.
			final List< Polygon > pp = maskToPolygons( Views.zeroMin( mask ) );
			// Translate back to interval coords.
			for ( final Polygon polygon : pp )
				polygon.translate( ( int ) mask.min( 0 ), ( int ) mask.min( 1 ) );

			polygons.addAll( pp );
		}

		// Quality image.
		final List< Spot > spots = new ArrayList<>( polygons.size() );

		// Simplify them and compute a quality.
		for ( final Polygon polygon : polygons )
		{
			final PolygonRoi roi = new PolygonRoi( polygon, PolygonRoi.POLYGON );

			// Create Spot ROI.
			final PolygonRoi fRoi;
			if ( simplify )
				fRoi = simplify( roi, SMOOTH_INTERVAL, DOUGLAS_PEUCKER_MAX_DISTANCE );
			else
				fRoi = roi;

			// Don't include ROIs that have been shrunk to < 1 pixel.
			if ( fRoi.getNCoordinates() < 3 || fRoi.getStatistics().area <= 0. )
				continue;

			// Create spot without quality value yet.
			final Polygon fPolygon = fRoi.getPolygon();
			final double[] xpoly = new double[ fPolygon.npoints ];
			final double[] ypoly = new double[ fPolygon.npoints ];
			for ( int i = 0; i < fPolygon.npoints; i++ )
			{
				xpoly[ i ] = calibration[ 0 ] * ( origin[ 0 ] + fPolygon.xpoints[ i ] - 0.5 );
				ypoly[ i ] = calibration[ 1 ] * ( origin[ 1 ] + fPolygon.ypoints[ i ] - 0.5 );
			}
			final SpotRoi spot = SpotRoi.createSpot( xpoly, ypoly, -1. );

			// Measure quality.
			final double quality;
			if ( null == qualityImage )
			{
				quality = fRoi.getStatistics().area;
			}
			else
			{
				final IterableInterval< S > iterable = spot.iterable( Views.extendZero( qualityImage ), calibration );
				double max = Double.NEGATIVE_INFINITY;
				for ( final S s : iterable )
				{
					final double val = s.getRealDouble();
					if ( val > max )
						max = val;
				}
				quality = max;
			}
			spot.putFeature( Spot.QUALITY, quality );
			spots.add( spot );
		}
		return spots;
	}

	private static final double distanceSquaredBetweenPoints( final double vx, final double vy, final double wx, final double wy )
	{
		final double deltax = ( vx - wx );
		final double deltay = ( vy - wy );
		return deltax * deltax + deltay * deltay;
	}

	private static final double distanceToSegmentSquared( final double px, final double py, final double vx, final double vy, final double wx, final double wy )
	{
		final double l2 = distanceSquaredBetweenPoints( vx, vy, wx, wy );
		if ( l2 == 0 )
			return distanceSquaredBetweenPoints( px, py, vx, vy );
		final double t = ( ( px - vx ) * ( wx - vx ) + ( py - vy ) * ( wy - vy ) ) / l2;
		if ( t < 0 )
			return distanceSquaredBetweenPoints( px, py, vx, vy );
		if ( t > 1 )
			return distanceSquaredBetweenPoints( px, py, wx, wy );
		return distanceSquaredBetweenPoints( px, py, ( vx + t * ( wx - vx ) ), ( vy + t * ( wy - vy ) ) );
	}

	private static final double perpendicularDistance( final double px, final double py, final double vx, final double vy, final double wx, final double wy )
	{
		return Math.sqrt( distanceToSegmentSquared( px, py, vx, vy, wx, wy ) );
	}

	private static final void douglasPeucker( final List< double[] > list, final int s, final int e, final double epsilon, final List< double[] > resultList )
	{
		// Find the point with the maximum distance
		double dmax = 0;
		int index = 0;

		final int start = s;
		final int end = e - 1;
		for ( int i = start + 1; i < end; i++ )
		{
			// Point
			final double px = list.get( i )[ 0 ];
			final double py = list.get( i )[ 1 ];
			// Start
			final double vx = list.get( start )[ 0 ];
			final double vy = list.get( start )[ 1 ];
			// End
			final double wx = list.get( end )[ 0 ];
			final double wy = list.get( end )[ 1 ];
			final double d = perpendicularDistance( px, py, vx, vy, wx, wy );
			if ( d > dmax )
			{
				index = i;
				dmax = d;
			}
		}
		// If max distance is greater than epsilon, recursively simplify
		if ( dmax > epsilon )
		{
			// Recursive call
			douglasPeucker( list, s, index, epsilon, resultList );
			douglasPeucker( list, index, e, epsilon, resultList );
		}
		else
		{
			if ( ( end - start ) > 0 )
			{
				resultList.add( list.get( start ) );
				resultList.add( list.get( end ) );
			}
			else
			{
				resultList.add( list.get( start ) );
			}
		}
	}

	/**
	 * Given a curve composed of line segments find a similar curve with fewer
	 * points.
	 * <p>
	 * The Ramer–Douglas–Peucker algorithm (RDP) is an algorithm for reducing
	 * the number of points in a curve that is approximated by a series of
	 * points.
	 * <p>
	 *
	 * @see <a href=
	 *      "https://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm">Ramer–Douglas–Peucker
	 *      Algorithm (Wikipedia)</a>
	 * @author Justin Wetherell
	 * @param list
	 *            List of double[] points (x,y)
	 * @param epsilon
	 *            Distance dimension
	 * @return Similar curve with fewer points
	 */
	public static final List< double[] > douglasPeucker( final List< double[] > list, final double epsilon )
	{
		final List< double[] > resultList = new ArrayList<>();
		douglasPeucker( list, 0, list.size(), epsilon, resultList );
		return resultList;
	}

	public static final PolygonRoi simplify( final PolygonRoi roi, final double smoothInterval, final double epsilon )
	{
		final FloatPolygon fPoly = roi.getInterpolatedPolygon( smoothInterval, true );

		final List< double[] > points = new ArrayList<>( fPoly.npoints );
		for ( int i = 0; i < fPoly.npoints; i++ )
			points.add( new double[] { fPoly.xpoints[ i ], fPoly.ypoints[ i ] } );

		final List< double[] > simplifiedPoints = douglasPeucker( points, epsilon );
		final float[] sX = new float[ simplifiedPoints.size() ];
		final float[] sY = new float[ simplifiedPoints.size() ];
		for ( int i = 0; i < sX.length; i++ )
		{
			sX[ i ] = ( float ) simplifiedPoints.get( i )[ 0 ];
			sY[ i ] = ( float ) simplifiedPoints.get( i )[ 1 ];
		}
		final FloatPolygon simplifiedPolygon = new FloatPolygon( sX, sY );
		final PolygonRoi fRoi = new PolygonRoi( simplifiedPolygon, PolygonRoi.POLYGON );
		return fRoi;
	}

	/**
	 * Parse a 2D mask and return a list of polygons for the external contours
	 * of white objects.
	 * <p>
	 * Warning: cannot deal with holes, they are simply ignored.
	 * <p>
	 * Copied and adapted from ImageJ1 code by Wayne Rasband.
	 *
	 * @param <T>
	 *            the type of the mask.
	 * @param mask
	 *            the mask image.
	 * @return a new list of polygons.
	 */
	private static final < T extends BooleanType< T > > List< Polygon > maskToPolygons( final RandomAccessibleInterval< T > mask )
	{
		final int w = ( int ) mask.dimension( 0 );
		final int h = ( int ) mask.dimension( 1 );
		final RandomAccess< T > ra = mask.randomAccess( mask );

		final List< Polygon > polygons = new ArrayList<>();
		boolean[] prevRow = new boolean[ w + 2 ];
		boolean[] thisRow = new boolean[ w + 2 ];
		final Outline[] outline = new Outline[ w + 1 ];

		for ( int y = 0; y <= h; y++ )
		{
			ra.setPosition( y, 1 );

			final boolean[] b = prevRow;
			prevRow = thisRow;
			thisRow = b;
			int xAfterLowerRightCorner = -1;
			Outline oAfterLowerRightCorner = null;

			ra.setPosition( 0, 0 );
			thisRow[ 1 ] = y < h ? ra.get().get() : false;

			for ( int x = 0; x <= w; x++ )
			{
				// we need to read one pixel ahead
				ra.setPosition( x + 1, 0 );
				if ( y < h && x < w - 1 )
					thisRow[ x + 2 ] = ra.get().get();
				else if ( x < w - 1 )
					thisRow[ x + 2 ] = false;

				if ( thisRow[ x + 1 ] )
				{ // i.e., pixel (x,y) is selected
					if ( !prevRow[ x + 1 ] )
					{
						// Upper edge of selected area:
						// - left and right outlines are null: new outline
						// - left null: append (line to left)
						// - right null: prepend (line to right), or
						// prepend&append (after lower right corner, two borders
						// from one corner)
						// - left == right: close (end of hole above) unless we
						// can continue at the right
						// - left != right: merge (prepend) unless we can
						// continue at the right
						if ( outline[ x ] == null )
						{
							if ( outline[ x + 1 ] == null )
							{
								outline[ x + 1 ] = outline[ x ] = new Outline();
								outline[ x ].append( x + 1, y );
								outline[ x ].append( x, y );
							}
							else
							{
								outline[ x ] = outline[ x + 1 ];
								outline[ x + 1 ] = null;
								outline[ x ].append( x, y );
							}
						}
						else if ( outline[ x + 1 ] == null )
						{
							if ( x == xAfterLowerRightCorner )
							{
								outline[ x + 1 ] = outline[ x ];
								outline[ x ] = oAfterLowerRightCorner;
								outline[ x ].append( x, y );
								outline[ x + 1 ].prepend( x + 1, y );
							}
							else
							{
								outline[ x + 1 ] = outline[ x ];
								outline[ x ] = null;
								outline[ x + 1 ].prepend( x + 1, y );
							}
						}
						else if ( outline[ x + 1 ] == outline[ x ] )
						{
							if ( x < w - 1 && y < h && x != xAfterLowerRightCorner
									&& !thisRow[ x + 2 ] && prevRow[ x + 2 ] )
							{ // at lower right corner & next pxl deselected
								outline[ x ] = null;
								// outline[x+1] unchanged
								outline[ x + 1 ].prepend( x + 1, y );
								xAfterLowerRightCorner = x + 1;
								oAfterLowerRightCorner = outline[ x + 1 ];
							}
							else
							{
								// MINUS (add inner hole)
								// We cannot handle holes in TrackMate.
//								polygons.add( outline[ x ].getPolygon() );
								outline[ x + 1 ] = null;
								outline[ x ] = ( x == xAfterLowerRightCorner ) ? oAfterLowerRightCorner : null;
							}
						}
						else
						{
							outline[ x ].prepend( outline[ x + 1 ] );
							for ( int x1 = 0; x1 <= w; x1++ )
								if ( x1 != x + 1 && outline[ x1 ] == outline[ x + 1 ] )
								{
									outline[ x1 ] = outline[ x ];
									outline[ x + 1 ] = null;
									outline[ x ] = ( x == xAfterLowerRightCorner ) ? oAfterLowerRightCorner : null;
									break;
								}
							if ( outline[ x + 1 ] != null )
								throw new RuntimeException( "assertion failed" );
						}
					}
					if ( !thisRow[ x ] )
					{
						// left edge
						if ( outline[ x ] == null )
							throw new RuntimeException( "assertion failed" );
						outline[ x ].append( x, y + 1 );
					}
				}
				else
				{ // !thisRow[x + 1], i.e., pixel (x,y) is deselected
					if ( prevRow[ x + 1 ] )
					{
						// Lower edge of selected area:
						// - left and right outlines are null: new outline
						// - left == null: prepend
						// - right == null: append, or append&prepend (after
						// lower right corner, two borders from one corner)
						// - right == left: close unless we can continue at the
						// right
						// - right != left: merge (append) unless we can
						// continue at the right
						if ( outline[ x ] == null )
						{
							if ( outline[ x + 1 ] == null )
							{
								outline[ x ] = outline[ x + 1 ] = new Outline();
								outline[ x ].append( x, y );
								outline[ x ].append( x + 1, y );
							}
							else
							{
								outline[ x ] = outline[ x + 1 ];
								outline[ x + 1 ] = null;
								outline[ x ].prepend( x, y );
							}
						}
						else if ( outline[ x + 1 ] == null )
						{
							if ( x == xAfterLowerRightCorner )
							{
								outline[ x + 1 ] = outline[ x ];
								outline[ x ] = oAfterLowerRightCorner;
								outline[ x ].prepend( x, y );
								outline[ x + 1 ].append( x + 1, y );
							}
							else
							{
								outline[ x + 1 ] = outline[ x ];
								outline[ x ] = null;
								outline[ x + 1 ].append( x + 1, y );
							}
						}
						else if ( outline[ x + 1 ] == outline[ x ] )
						{
							// System.err.println("add " + outline[x]);
							if ( x < w - 1 && y < h && x != xAfterLowerRightCorner
									&& thisRow[ x + 2 ] && !prevRow[ x + 2 ] )
							{ // at lower right corner & next pxl selected
								outline[ x ] = null;
								// outline[x+1] unchanged
								outline[ x + 1 ].append( x + 1, y );
								xAfterLowerRightCorner = x + 1;
								oAfterLowerRightCorner = outline[ x + 1 ];
							}
							else
							{
								polygons.add( outline[ x ].getPolygon() );
								outline[ x + 1 ] = null;
								outline[ x ] = x == xAfterLowerRightCorner ? oAfterLowerRightCorner : null;
							}
						}
						else
						{
							if ( x < w - 1 && y < h && x != xAfterLowerRightCorner
									&& thisRow[ x + 2 ] && !prevRow[ x + 2 ] )
							{ // at lower right corner && next pxl selected
								outline[ x ].append( x + 1, y );
								outline[ x + 1 ].prepend( x + 1, y );
								xAfterLowerRightCorner = x + 1;
								oAfterLowerRightCorner = outline[ x ];
								// outline[x + 1] unchanged (the one at the
								// right-hand side of (x, y-1) to the top)
								outline[ x ] = null;
							}
							else
							{
								outline[ x ].append( outline[ x + 1 ] ); // merge
								for ( int x1 = 0; x1 <= w; x1++ )
									if ( x1 != x + 1 && outline[ x1 ] == outline[ x + 1 ] )
									{
										outline[ x1 ] = outline[ x ];
										outline[ x + 1 ] = null;
										outline[ x ] = ( x == xAfterLowerRightCorner ) ? oAfterLowerRightCorner : null;
										break;
									}
								if ( outline[ x + 1 ] != null )
									throw new RuntimeException( "assertion failed" );
							}
						}
					}
					if ( thisRow[ x ] )
					{
						// right edge
						if ( outline[ x ] == null )
							throw new RuntimeException( "assertion failed" );
						outline[ x ].prepend( x, y + 1 );
					}
				}
			}
		}
		return polygons;
	}

	/**
	 * This class implements a Cartesian polygon in progress. The edges are
	 * supposed to be parallel to the x or y axis. It is implemented as a deque
	 * to be able to add points to both sides.
	 */
	private static class Outline
	{

		private int[] x, y;

		private int first, last, reserved;

		/**
		 * Default extra (spare) space when enlarging arrays (similar
		 * performance with 6-20)
		 */
		private final int GROW = 10;

		public Outline()
		{
			reserved = GROW;
			x = new int[ reserved ];
			y = new int[ reserved ];
			first = last = GROW / 2;
		}

		/**
		 * Makes sure that enough free space is available at the beginning and
		 * end of the list, by enlarging the arrays if required
		 */
		private void needs( final int neededAtBegin, final int neededAtEnd )
		{
			if ( neededAtBegin > first || neededAtEnd > reserved - last )
			{
				final int extraSpace = Math.max( GROW, Math.abs( x[ last - 1 ] - x[ first ] ) );
				final int newSize = reserved + neededAtBegin + neededAtEnd + extraSpace;
				final int newFirst = neededAtBegin + extraSpace / 2;
				final int[] newX = new int[ newSize ];
				final int[] newY = new int[ newSize ];
				System.arraycopy( x, first, newX, newFirst, last - first );
				System.arraycopy( y, first, newY, newFirst, last - first );
				x = newX;
				y = newY;
				last += newFirst - first;
				first = newFirst;
				reserved = newSize;
			}
		}

		/** Adds point x, y at the end of the list */
		public void append( final int x, final int y )
		{
			if ( last - first >= 2 && collinear( this.x[ last - 2 ], this.y[ last - 2 ], this.x[ last - 1 ], this.y[ last - 1 ], x, y ) )
			{
				this.x[ last - 1 ] = x; // replace previous point
				this.y[ last - 1 ] = y;
			}
			else
			{
				needs( 0, 1 ); // new point
				this.x[ last ] = x;
				this.y[ last ] = y;
				last++;
			}
		}

		/** Adds point x, y at the beginning of the list */
		public void prepend( final int x, final int y )
		{
			if ( last - first >= 2 && collinear( this.x[ first + 1 ], this.y[ first + 1 ], this.x[ first ], this.y[ first ], x, y ) )
			{
				this.x[ first ] = x; // replace previous point
				this.y[ first ] = y;
			}
			else
			{
				needs( 1, 0 ); // new point
				first--;
				this.x[ first ] = x;
				this.y[ first ] = y;
			}
		}

		/**
		 * Merge with another Outline by adding it at the end. Thereafter, the
		 * other outline must not be used any more.
		 */
		public void append( final Outline o )
		{
			final int size = last - first;
			final int oSize = o.last - o.first;
			if ( size <= o.first && oSize > reserved - last )
			{ // we don't have enough space in our own array but in that of 'o'
				System.arraycopy( x, first, o.x, o.first - size, size );
				System.arraycopy( y, first, o.y, o.first - size, size );
				x = o.x;
				y = o.y;
				first = o.first - size;
				last = o.last;
				reserved = o.reserved;
			}
			else
			{ // append to our own array
				needs( 0, oSize );
				System.arraycopy( o.x, o.first, x, last, oSize );
				System.arraycopy( o.y, o.first, y, last, oSize );
				last += oSize;
			}
		}

		/**
		 * Merge with another Outline by adding it at the beginning. Thereafter,
		 * the other outline must not be used any more.
		 */
		public void prepend( final Outline o )
		{
			final int size = last - first;
			final int oSize = o.last - o.first;
			if ( size <= o.reserved - o.last && oSize > first )
			{ /*
				 * We don't have enough space in our own array but in that of
				 * 'o' so append our own data to that of 'o'
				 */
				System.arraycopy( x, first, o.x, o.last, size );
				System.arraycopy( y, first, o.y, o.last, size );
				x = o.x;
				y = o.y;
				first = o.first;
				last = o.last + size;
				reserved = o.reserved;
			}
			else
			{ // prepend to our own array
				needs( oSize, 0 );
				first -= oSize;
				System.arraycopy( o.x, o.first, x, first, oSize );
				System.arraycopy( o.y, o.first, y, first, oSize );
			}
		}

		public Polygon getPolygon()
		{
			/*
			 * optimize out intermediate points of straight lines (created,
			 * e.g., by merging outlines)
			 */
			int i, j = first + 1;
			for ( i = first + 1; i + 1 < last; j++ )
			{
				if ( collinear( x[ j - 1 ], y[ j - 1 ], x[ j ], y[ j ], x[ j + 1 ], y[ j + 1 ] ) )
				{
					// merge i + 1 into i
					last--;
					continue;
				}
				if ( i != j )
				{
					x[ i ] = x[ j ];
					y[ i ] = y[ j ];
				}
				i++;
			}
			// wraparound
			if ( collinear( x[ j - 1 ], y[ j - 1 ], x[ j ], y[ j ], x[ first ], y[ first ] ) )
				last--;
			else
			{
				x[ i ] = x[ j ];
				y[ i ] = y[ j ];
			}
			if ( last - first > 2 && collinear( x[ last - 1 ], y[ last - 1 ], x[ first ], y[ first ], x[ first + 1 ], y[ first + 1 ] ) )
				first++;

			final int count = last - first;
			final int[] xNew = new int[ count ];
			final int[] yNew = new int[ count ];
			System.arraycopy( x, first, xNew, 0, count );
			System.arraycopy( y, first, yNew, 0, count );
			return new Polygon( xNew, yNew, count );
		}

		/** Returns whether three points are on one straight line */
		public boolean collinear( final int x1, final int y1, final int x2, final int y2, final int x3, final int y3 )
		{
			return ( x2 - x1 ) * ( y3 - y2 ) == ( y2 - y1 ) * ( x3 - x2 );
		}

		@Override
		public String toString()
		{
			String res = "[first:" + first + ",last:" + last +
					",reserved:" + reserved + ":";
			if ( last > x.length )
				System.err.println( "ERROR!" );
			int nmax = 10; // don't print more coordinates than this
			for ( int i = first; i < last && i < x.length; i++ )
			{
				if ( last - first > nmax && i - first > nmax / 2 )
				{
					i = last - nmax / 2;
					res += "...";
					nmax = last - first; // dont check again
				}
				else
					res += "(" + x[ i ] + "," + y[ i ] + ")";
			}
			return res + "]";
		}
	}
}
