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
package fiji.plugin.trackmate.tracking.jaqaman.costmatrix;

import java.util.Arrays;

public class ResizableDoubleArray
{

	/*
	 * PUBLIC FIELDS
	 */

	public double[] data;

	public int size;

	/*
	 * CONSTRUCTORS
	 */

	public ResizableDoubleArray( final double[] data )
	{
		this.data = data;
		this.size = data.length;
	}

	public ResizableDoubleArray( final int initialCapacity )
	{
		this.data = new double[ initialCapacity ];
		this.size = 0;
	}

	/**
	 * Creates an empty ResizableIntArray with the a initial capacity of 10.
	 */
	public ResizableDoubleArray()
	{
		this( 10 );
	}

	/*
	 * METHODS
	 */

	public void trimToSize()
	{
		final int oldCapacity = data.length;
		if ( size < oldCapacity )
			data = Arrays.copyOf( data, size );
	}

	public void ensureCapacity( final int minCapacity )
	{
		final int oldCapacity = data.length;
		if ( minCapacity > oldCapacity )
		{
			// The heuristics of ArrayList
			int newCapacity = ( oldCapacity * 3 ) / 2 + 1;
			if ( newCapacity < minCapacity )
				newCapacity = minCapacity;

			data = Arrays.copyOf( data, newCapacity );
		}
	}

	/**
	 * Returns {@code true} if this list contains no elements.
	 * 
	 * @return {@code true} if this list contains no elements
	 */
	public boolean isEmpty()
	{
		return size == 0;
	}

	public void add( final double val )
	{
		ensureCapacity( size + 1 );
		data[ size ] = val;
		size++;
	}

	@Override
	public String toString()
	{
		if ( isEmpty() )
			return "()";

		final StringBuilder str = new StringBuilder();
		str.append( '(' );
		for ( int i = 0; i < size - 1; i++ )
			str.append( data[ i ] + ", " );

		str.append( data[ size - 1 ] + "), size = " + size );
		return str.toString();
	}
}
