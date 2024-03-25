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
package fiji.plugin.trackmate.detection.util;

import java.util.Iterator;

import net.imglib2.IterableInterval;
import net.imglib2.Localizable;
import net.imglib2.Positionable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPositionable;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

public class SquareNeighborhood3x3< T > implements Positionable, IterableInterval< T >
{

	private final RandomAccessibleInterval< T > source;

	private final long[] center;

	private final ExtendedRandomAccessibleInterval< T, RandomAccessibleInterval< T > > extendedSource;

	/*
	 * CONSTRUCTOR
	 */

	public SquareNeighborhood3x3( final RandomAccessibleInterval< T > source, final OutOfBoundsFactory< T, RandomAccessibleInterval< T > > outOfBounds )
	{
		this.source = source;
		this.center = new long[ source.numDimensions() ];
		this.extendedSource = Views.extend( source, outOfBounds );
	}

	/*
	 * METHODS
	 */

	@Override
	public int numDimensions()
	{
		return source.numDimensions();
	}

	@Override
	public void fwd( final int d )
	{
		center[ d ]++;
	}

	@Override
	public void bck( final int d )
	{
		center[ d ]--;
	}

	@Override
	public void move( final int distance, final int d )
	{
		center[ d ] = center[ d ] + distance;
	}

	@Override
	public void move( final long distance, final int d )
	{
		center[ d ] = center[ d ] + distance;
	}

	@Override
	public void move( final Localizable localizable )
	{
		for ( int i = 0; i < source.numDimensions(); i++ )
			center[ i ] = center[ i ] + localizable.getLongPosition( i );
	}

	@Override
	public void move( final int[] distance )
	{
		for ( int i = 0; i < distance.length; i++ )
			center[ i ] = center[ i ] + distance[ i ];
	}

	@Override
	public void move( final long[] distance )
	{
		for ( int i = 0; i < distance.length; i++ )
			center[ i ] = center[ i ] + distance[ i ];
	}

	@Override
	public void setPosition( final Localizable localizable )
	{
		localizable.localize( center );
	}

	@Override
	public void setPosition( final int[] position )
	{
		for ( int i = 0; i < position.length; i++ )
			center[ i ] = position[ i ];
	}

	@Override
	public void setPosition( final long[] position )
	{
		System.arraycopy( position, 0, center, 0, center.length );
	}

	@Override
	public void setPosition( final int position, final int d )
	{
		center[ d ] = position;
	}

	@Override
	public void setPosition( final long position, final int d )
	{
		center[ d ] = position;
	}

	@Override
	public long size()
	{
		return 9;
	}

	@Override
	public T firstElement()
	{
		final RandomAccess< T > ra = source.randomAccess();
		ra.setPosition( center );
		return ra.get();
	}

	@Override
	public Object iterationOrder()
	{
		return this;
	}

	@Override
	public double realMin( final int d )
	{
		return center[ d ] - 1;
	}

	@Override
	public void realMin( final double[] min )
	{
		for ( int d = 0; d < min.length; d++ )
			min[ d ] = center[ d ] - 1;
	}

	@Override
	public void realMin( final RealPositionable min )
	{
		for ( int d = 0; d < center.length; d++ )
			min.setPosition( center[ d ] - 1, d );
	}

	@Override
	public double realMax( final int d )
	{
		return center[ d ] + 1;
	}

	@Override
	public void realMax( final double[] max )
	{
		for ( int d = 0; d < max.length; d++ )
			max[ d ] = center[ d ] + 1;
	}

	@Override
	public void realMax( final RealPositionable max )
	{
		for ( int d = 0; d < center.length; d++ )
			max.setPosition( center[ d ] + 1, d );
	}

	@Override
	public long min( final int d )
	{
		return center[ d ] - 1;
	}

	@Override
	public void min( final long[] min )
	{
		for ( int d = 0; d < min.length; d++ )
			min[ d ] = center[ d ] - 1;
	}

	@Override
	public void min( final Positionable min )
	{
		for ( int d = 0; d < center.length; d++ )
			min.setPosition( center[ d ] - 1, d );
	}

	@Override
	public long max( final int d )
	{
		return center[ d ] + 1;
	}

	@Override
	public void max( final long[] max )
	{
		for ( int d = 0; d < max.length; d++ )
			max[ d ] = center[ d ] + 1;
	}

	@Override
	public void max( final Positionable max )
	{
		for ( int d = 0; d < center.length; d++ )
			max.setPosition( center[ d ] + 1, d );
	}

	@Override
	public void dimensions( final long[] dimensions )
	{
		dimensions[ 0 ] = 3;
		dimensions[ 1 ] = 3;
		for ( int d = 2; d < dimensions.length; d++ )
			dimensions[ d ] = 1;

	}

	@Override
	public long dimension( final int d )
	{
		if ( d < 2 )
			return 3;
		return 1;
	}

	@Override
	public SquareNeighborhoodCursor3x3< T > cursor()
	{
		return new SquareNeighborhoodCursor3x3<>( extendedSource, center );
	}

	@Override
	public SquareNeighborhoodCursor3x3< T > localizingCursor()
	{
		return new SquareNeighborhoodCursor3x3<>( extendedSource, center );
	}

	@Override
	public Iterator< T > iterator()
	{
		return new SquareNeighborhoodCursor3x3<>( extendedSource, center );
	}

}
