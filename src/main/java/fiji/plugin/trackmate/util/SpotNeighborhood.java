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

import fiji.plugin.trackmate.Spot;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Positionable;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPositionable;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.region.localneighborhood.AbstractNeighborhood;
import net.imglib2.algorithm.region.localneighborhood.EllipseNeighborhood;
import net.imglib2.algorithm.region.localneighborhood.EllipsoidNeighborhood;
import net.imglib2.algorithm.region.localneighborhood.RectangleNeighborhoodGPL;
import net.imglib2.outofbounds.OutOfBoundsMirrorExpWindowingFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class SpotNeighborhood< T extends RealType< T > > implements Neighborhood< T >
{

	/*
	 * FIELDS
	 */

	protected final double[] calibration;

	protected final AbstractNeighborhood< T > neighborhood;

	protected final long[] center;

	/*
	 * CONSTRUCTOR
	 */

	public SpotNeighborhood( final Spot spot, final RandomAccessible< T > ra, final double[] calibration )
	{
		this.calibration = calibration;
		// Center, span and interval.
		this.center = new long[ ra.numDimensions() ];
		final long[] span = new long[ ra.numDimensions() ];
		final long[] min = new long[ra.numDimensions()];
		final long[] max = new long[ ra.numDimensions() ];
		for ( int d = 0; d < center.length; d++ )
		{
			center[ d ] = Math.round( spot.getDoublePosition( d ) / calibration[ d ] );
			span[ d ] = Math.round( spot.getFeature( Spot.RADIUS ) / calibration[ d ] );
			min[d] = center[d] - span[d];
			max[d] = center[d] + span[d];
		}
		final FinalInterval interval = new FinalInterval( min, max );

		// Neighborhood

		/*
		 * We have to detect here whether we were given a 1D image. Trouble is,
		 * since it is an ImgPlus, it will always be of dim at least 2. So we
		 * have to test pedantically.
		 */

		final RandomAccessibleInterval< T > rai = Views.interval( ra, interval );
		final OutOfBoundsMirrorExpWindowingFactory< T, RandomAccessibleInterval< T > > oob = new OutOfBoundsMirrorExpWindowingFactory<>();
		if ( ra.numDimensions() == 1 )
		{
			span[ 0 ] = 0;
			this.neighborhood = new RectangleNeighborhoodGPL<>( rai, oob );
			neighborhood.setPosition( center );
			neighborhood.setSpan( span );
		}
		else if ( ra.numDimensions() == 2 )
		{
			this.neighborhood = new EllipseNeighborhood<>( rai, center, span, oob );
		}
		else if ( ra.numDimensions() == 3 )
		{
			this.neighborhood = new EllipsoidNeighborhood<>( rai, center, span, oob );
		}
		else
		{
			throw new IllegalArgumentException( "Source input must be 1D, 2D or 3D, got nDims = " + ra.numDimensions() );
		}
	}

	/*
	 * METHODS We delegate everything to the wrapped neighborhood
	 */

	@Override
	public final SpotNeighborhoodCursor< T > cursor()
	{
		return new SpotNeighborhoodCursor<>( this );
	}

	@Override
	public SpotNeighborhoodCursor< T > localizingCursor()
	{
		return cursor();
	}

	@Override
	public long size()
	{
		return neighborhood.size();
	}

	@Override
	public T firstElement()
	{
		return neighborhood.firstElement();
	}

	@Override
	public Object iterationOrder()
	{
		return neighborhood.iterationOrder();
	}

	@Override
	public double realMin( final int d )
	{
		return neighborhood.realMin( d );
	}

	@Override
	public void realMin( final double[] min )
	{
		neighborhood.realMin( min );
	}

	@Override
	public void realMin( final RealPositionable min )
	{
		neighborhood.realMin( min );
	}

	@Override
	public double realMax( final int d )
	{
		return neighborhood.realMax( d );
	}

	@Override
	public void realMax( final double[] max )
	{
		neighborhood.realMax( max );
	}

	@Override
	public void realMax( final RealPositionable max )
	{
		neighborhood.realMax( max );
	}

	@Override
	public int numDimensions()
	{
		return neighborhood.numDimensions();
	}

	@Override
	public SpotNeighborhoodCursor< T > iterator()
	{
		return cursor();
	}

	@Override
	public long min( final int d )
	{
		return neighborhood.min( d );
	}

	@Override
	public void min( final long[] min )
	{
		neighborhood.min( min );
	}

	@Override
	public void min( final Positionable min )
	{
		neighborhood.min( min );
	}

	@Override
	public long max( final int d )
	{
		return neighborhood.max( d );
	}

	@Override
	public void max( final long[] max )
	{
		neighborhood.max( max );
	}

	@Override
	public void max( final Positionable max )
	{
		neighborhood.max( max );
	}

	@Override
	public void dimensions( final long[] dimensions )
	{
		neighborhood.dimensions( dimensions );
	}

	@Override
	public long dimension( final int d )
	{
		return neighborhood.dimension( d );
	}

	@Override
	public void localize( final int[] position )
	{
		for ( int d = 0; d < position.length; d++ )
			position[ d ] = ( int ) center[ d ];
	}

	@Override
	public void localize( final long[] position )
	{
		for ( int d = 0; d < position.length; d++ )
			position[ d ] = center[ d ];
	}

	@Override
	public int getIntPosition( final int d )
	{
		return ( int ) center[ d ];
	}

	@Override
	public long getLongPosition( final int d )
	{
		return center[ d ];
	}

	@Override
	public void localize( final float[] position )
	{
		for ( int d = 0; d < position.length; d++ )
			position[ d ] = center[ d ];
	}

	@Override
	public void localize( final double[] position )
	{
		for ( int d = 0; d < position.length; d++ )
			position[ d ] = center[ d ];
	}

	@Override
	public float getFloatPosition( final int d )
	{
		return center[ d ];
	}

	@Override
	public double getDoublePosition( final int d )
	{
		return center[ d ];
	}

	@Override
	public Interval getStructuringElementBoundingBox()
	{
		final long[] min = new long[ numDimensions() ];
		final long[] max = new long[ numDimensions() ];
		min( min );
		max( max );
		final FinalInterval interval = new FinalInterval( min, max );
		return interval;
	}
}
