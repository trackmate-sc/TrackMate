package fiji.plugin.trackmate.tracking.sparselap.costmatrix;

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
		{
			data = Arrays.copyOf( data, size );
		}
	}

	public void ensureCapacity( final int minCapacity )
	{
		final int oldCapacity = data.length;
		if ( minCapacity > oldCapacity )
		{
			// The heuristics of ArrayList
			int newCapacity = ( oldCapacity * 3 ) / 2 + 1;
			if ( newCapacity < minCapacity )
			{
				newCapacity = minCapacity;
			}
			data = Arrays.copyOf( data, newCapacity );
		}
	}

	/**
	 * Returns <tt>true</tt> if this list contains no elements.
	 * 
	 * @return <tt>true</tt> if this list contains no elements
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
		if ( isEmpty() ) { return "()"; }
		final StringBuilder str = new StringBuilder();
		str.append( '(' );
		for ( int i = 0; i < size - 1; i++ )
		{
			str.append( data[ i ] + ", " );
		}
		str.append( data[ size - 1 ] + "), size = " + size );
		return str.toString();
	}
}
