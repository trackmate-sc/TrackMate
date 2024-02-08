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

import java.util.Iterator;

import fiji.plugin.trackmate.SpotMesh;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessible;

public class SpotMeshIterable< T > implements IterableInterval< T >, Localizable
{

	private final double[] calibration;

	private final RandomAccessible< T > img;

	private final SpotMesh sm;

	public SpotMeshIterable(
			final RandomAccessible< T > img,
			final SpotMesh sm,
			final double[] calibration )
	{
		this.img = img;
		this.sm = sm;
		this.calibration = calibration;
	}

	@Override
	public int numDimensions()
	{
		return 3;
	}

	@Override
	public long getLongPosition( final int d )
	{
		return Math.round( sm.getDoublePosition( d ) / calibration[ d ] );
	}

	@Override
	public long size()
	{
		// Costly!
		long size = 0;
		for ( @SuppressWarnings( "unused" )
		final T t : this )
			size++;

		return size;
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
	public Iterator< T > iterator()
	{
		return cursor();
	}

	@Override
	public long min( final int d )
	{
		return Math.round( ( sm.getBoundingBox().realMin( d ) + sm.getFloatPosition( d ) ) / calibration[ d ] );
	}

	@Override
	public long max( final int d )
	{
		return Math.round( ( sm.getBoundingBox().realMax( d ) + sm.getFloatPosition( d ) ) / calibration[ d ] );
	}

	@Override
	public Cursor< T > cursor()
	{
		return new SpotMeshCursor<>( img.randomAccess(), sm, calibration );
	}

	@Override
	public Cursor< T > localizingCursor()
	{
		return cursor();
	}
}
