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

import java.util.NoSuchElementException;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.outofbounds.Bounded;
import net.imglib2.outofbounds.OutOfBounds;
import net.imglib2.view.ExtendedRandomAccessibleInterval;

public class SquareNeighborhoodCursor3x3< T > implements Cursor< T >, Bounded
{

	private final ExtendedRandomAccessibleInterval< T, RandomAccessibleInterval< T > > source;

	private final long[] center;

	private final OutOfBounds< T > ra;

	private int index = -1;

	private boolean hasNext;

	/*
	 * CONSTRUCTOR
	 */

	public SquareNeighborhoodCursor3x3( final ExtendedRandomAccessibleInterval< T, RandomAccessibleInterval< T > > extendedSource, final long[] center )
	{
		this.source = extendedSource;
		this.center = center;
		this.ra = extendedSource.randomAccess();
		reset();
	}

	/*
	 * METHODS
	 */

	@Override
	public void localize( final float[] position )
	{
		ra.localize( position );
	}

	@Override
	public void localize( final double[] position )
	{
		ra.localize( position );
	}

	@Override
	public float getFloatPosition( final int d )
	{
		return ra.getFloatPosition( d );
	}

	@Override
	public double getDoublePosition( final int d )
	{
		return ra.getDoublePosition( d );
	}

	@Override
	public int numDimensions()
	{
		return source.numDimensions();
	}

	@Override
	public T get()
	{
		return ra.get();
	}

	@Override
	public Cursor< T > copy()
	{
		return new SquareNeighborhoodCursor3x3<>( source, center );
	}

	@Override
	public void jumpFwd( final long steps )
	{
		for ( int i = 0; i < steps; i++ )
			fwd();
	}

	@Override
	public void fwd()
	{
		index++;

		switch ( index )
		{
		case 0:
			// already in place
			break;

		case 1:
			ra.bck( 1 );
			break;

		case 2:
			ra.bck( 0 );
			break;

		case 3:
			ra.fwd( 1 );
			break;

		case 4:
			ra.fwd( 1 );
			break;

		case 5:
			ra.fwd( 0 );
			break;

		case 6:
			ra.fwd( 0 );
			break;

		case 7:
			ra.bck( 1 );
			break;

		case 8:
			ra.bck( 1 );
			hasNext = false;
			break;

		default:
			throw new NoSuchElementException( "SquareNeighborhood3x3 exhausted" );
		}
	}

	@Override
	public void reset()
	{
		index = -1;
		hasNext = true;
		ra.setPosition( center );
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
		return ra.get();
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException( "remove() is not implemented for SquareNeighborhoodCursor" );
	}

	@Override
	public void localize( final int[] position )
	{
		ra.localize( position );
	}

	@Override
	public void localize( final long[] position )
	{
		ra.localize( position );
	}

	@Override
	public int getIntPosition( final int d )
	{
		return ra.getIntPosition( d );
	}

	@Override
	public long getLongPosition( final int d )
	{
		return ra.getLongPosition( d );
	}

	@Override
	public boolean isOutOfBounds()
	{
		return ra.isOutOfBounds();
	}
}
