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
package fiji.plugin.trackmate.util.mesh;

import fiji.plugin.trackmate.SpotMesh;
import gnu.trove.list.array.TDoubleArrayList;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RealInterval;
import net.imglib2.mesh.alg.zslicer.Slice;

/**
 * A {@link Cursor} that iterates over the pixels inside a mesh.
 * <p>
 * It is based on an implementation of the ray casting algorithm, with some
 * optimization to avoid querying the mesh for every single pixel. It does its
 * best to ensure that the pixels iterated inside a mesh created from a mask are
 * exactly the pixels of the original mask, but does not succeed fully (yet).
 *
 * @author Jean-Yves Tinevez
 *
 * @param <T>
 *            the types of the pixels iterated.
 */
public class SpotMeshCursor< T > implements Cursor< T >
{

	private final double[] cal;

	private final int minX;

	private final int maxX;

	private final int minY;

	private final int maxY;

	private final int minZ;

	private final int maxZ;

	private final RandomAccess< T > ra;

	private final SpotMesh sm;

	private boolean hasNext;

	private int iy;

	private int iz;

	private int ix;

	/**
	 * List of resolved X positions where we enter / exit the mesh. Set by the
	 * ray casting algorithm.
	 */
	private final TDoubleArrayList intersectionXs = new TDoubleArrayList();

	private Slice slice;

	public SpotMeshCursor( final RandomAccess< T > ra, final SpotMesh sm, final double[] cal )
	{
		this.ra = ra;
		this.sm = sm;
		this.cal = cal;
		final RealInterval bb = sm.getBoundingBox();
		this.minX = ( int ) Math.floor( ( bb.realMin( 0 ) + sm.getDoublePosition( 0 ) ) / cal[ 0 ] );
		this.maxX = ( int ) Math.ceil( ( bb.realMax( 0 ) + sm.getDoublePosition( 0 ) ) / cal[ 0 ] );
		this.minY = ( int ) Math.floor( ( bb.realMin( 1 ) + sm.getDoublePosition( 1 ) ) / cal[ 1 ] );
		this.maxY = ( int ) Math.ceil( ( bb.realMax( 1 ) + sm.getDoublePosition( 1 ) ) / cal[ 1 ] );
		this.minZ = ( int ) Math.floor( ( bb.realMin( 2 ) + sm.getDoublePosition( 2 ) ) / cal[ 2 ] );
		this.maxZ = ( int ) Math.ceil( ( bb.realMax( 2 ) + sm.getDoublePosition( 2 ) ) / cal[ 2 ] );
		reset();
	}

	@Override
	public void reset()
	{
		this.ix = maxX; // To force a new ray cast when we call fwd()
		this.iy = minY - 1; // Then we will move to minY.
		this.iz = minZ;
		this.slice = sm.getZSlice( iz, cal[ 0 ], cal[ 2 ] );
		this.hasNext = true;
		preFetch();
	}

	@Override
	public void fwd()
	{
		ra.setPosition( ix, 0 );
		ra.setPosition( iy, 1 );
		ra.setPosition( iz, 2 );
		preFetch();
	}

	private void preFetch()
	{
		hasNext = false;
		while ( true )
		{
			// Find next position.
			ix++;
			if ( ix > maxX )
			{
				ix = minX;
				while ( true )
				{
					// Next Y line, we will need to ray cast again.
					ix = minX;
					iy++;
					if ( iy > maxY )
					{
						iy = minY;
						iz++;
						if ( iz > maxZ )
							return; // Finished!
						slice = sm.getZSlice( iz, cal[ 0 ], cal[ 2 ] );
					}
					if ( slice == null )
						continue;

					// New ray cast, relative to slice center
					final double y = iy * cal[ 1 ] - sm.getDoublePosition( 1 );
					slice.xRayCast( y, intersectionXs, cal[ 1 ] );

					// No intersection?
					if ( !intersectionXs.isEmpty() )
						break;

					// No intersection on this line, move to the next.
				}
			}
			// We have found the next position.

			// Is it inside?
			final double x = ix * cal[ 0 ] - sm.getDoublePosition( 0 );

			// Special case: only one intersection.
			if ( intersectionXs.size() == 1 )
			{
				if ( x == intersectionXs.getQuick( 0 ) )
				{
					hasNext = true;
					return;
				}
				else
				{
					continue;
				}
			}

			final int i = intersectionXs.binarySearch( x );
			if ( i >= 0 )
			{
				// Fall on an intersection exactly.
				hasNext = true;
				return;
			}
			final int ip = -( i + 1 );
			// Odd or even?
			if ( ip % 2 != 0 )
			{
				// Odd. We are inside.
				hasNext = true;
				return;
			}

			// Not inside, move to the next point.
		}
	}

	@Override
	public boolean hasNext()
	{
		return hasNext;
	}

	@Override
	public void jumpFwd( final long steps )
	{
		for ( int i = 0; i < steps; i++ )
			fwd();
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
	public Cursor< T > copyCursor()
	{
		return new SpotMeshCursor<>(
				ra.copy(),
				sm.copy(),
				cal.clone() );
	}

	@Override
	public Cursor< T > copy()
	{
		return copyCursor();
	}

	@Override
	public int numDimensions()
	{
		return 3;
	}

	@Override
	public T get()
	{
		return ra.get();
	}
}
