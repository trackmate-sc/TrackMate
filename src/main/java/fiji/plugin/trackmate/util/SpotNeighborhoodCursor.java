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

import net.imglib2.Cursor;
import net.imglib2.type.numeric.RealType;

public class SpotNeighborhoodCursor< T extends RealType< T > > implements Cursor< T >
{

	/*
	 * FIELDs
	 */

	protected final Cursor< T > cursor;

	protected final double[] calibration;

	protected final long[] center;

	/** A utility holder to store position everytime required. */
	private final long[] pos;

	/*
	 * CONSTRUCTOR
	 */

	public SpotNeighborhoodCursor( final SpotNeighborhood< T > sn )
	{
		this.cursor = sn.neighborhood.cursor();
		this.calibration = sn.calibration;
		this.center = sn.center;
		this.pos = new long[ cursor.numDimensions() ];
		reset();
	}

	/*
	 * METHODS These methods are specific and are mainly focused on the use of
	 * calibrated units.
	 */

	/**
	 * Stores the relative <b>calibrated</b> position with respect to the
	 * neighborhood center.
	 * 
	 * @param position
	 *            an array to store to relative position in.
	 */
	public void getRelativePosition( final double[] position )
	{
		cursor.localize( pos );
		for ( int d = 0; d < center.length; d++ )
			position[ d ] = calibration[ d ] * ( pos[ d ] - center[ d ] );
	}

	/**
	 * Returns the square distance measured from the center of the domain to the
	 * current cursor position, in <b>calibrated</b> units.
	 * 
	 * @return the square distance.
	 */
	public double getDistanceSquared()
	{
		cursor.localize( pos );
		double sum = 0;
		double dx = 0;
		for ( int d = 0; d < pos.length; d++ )
		{
			dx = calibration[ d ] * ( pos[ d ] - center[ d ] );
			sum += ( dx * dx );
		}
		return sum;
	}

	/**
	 * Returns the current inclination with respect to this spot center. Will be
	 * in the range [0, π].
	 * <p>
	 * In spherical coordinates, the inclination is the angle between the Z axis
	 * and the line OM where O is the sphere center and M is the point location.
	 * 
	 * @return the inclination.
	 */
	public double getTheta()
	{
		if ( numDimensions() < 2 )
			return 0;
		final double dx = calibration[ 2 ] * ( cursor.getDoublePosition( 2 ) - center[ 2 ] );
		return Math.acos( dx / Math.sqrt( getDistanceSquared() ) );
	}

	/**
	 * Returns the azimuth of the spherical coordinates of this cursor, with
	 * respect to its center. Will be in the range ]-π, π].
	 * <p>
	 * In spherical coordinates, the azimuth is the angle measured in the plane
	 * XY between the X axis and the line OH where O is the sphere center and H
	 * is the orthogonal projection of the point M on the XY plane.
	 * 
	 * @return the azimuth.
	 */
	public double getPhi()
	{
		final double dx = calibration[ 0 ] * ( cursor.getDoublePosition( 0 ) - center[ 0 ] );
		final double dy = calibration[ 1 ] * ( cursor.getDoublePosition( 1 ) - center[ 1 ] );
		return Math.atan2( dy, dx );
	}

	/*
	 * CURSOR METHODS We delegate to the wrapped cursor
	 */

	@Override
	public void localize( final float[] position )
	{
		cursor.localize( position );
	}

	@Override
	public void localize( final double[] position )
	{
		cursor.localize( position );
	}

	@Override
	public float getFloatPosition( final int d )
	{
		return cursor.getFloatPosition( d );
	}

	@Override
	public double getDoublePosition( final int d )
	{
		return cursor.getDoublePosition( d );
	}

	@Override
	public int numDimensions()
	{
		return cursor.numDimensions();
	}

	@Override
	public T get()
	{
		return cursor.get();
	}

	@Override
	public Cursor< T > copy()
	{
		return cursor.copy();
	}

	@Override
	public void jumpFwd( final long steps )
	{
		cursor.jumpFwd( steps );
	}

	@Override
	public void fwd()
	{
		cursor.fwd();
	}

	@Override
	public void reset()
	{
		cursor.reset();
	}

	@Override
	public boolean hasNext()
	{
		return cursor.hasNext();
	}

	@Override
	public T next()
	{
		return cursor.next();
	}

	@Override
	public void remove()
	{
		cursor.remove();
	}

	@Override
	public void localize( final int[] position )
	{
		cursor.localize( position );
	}

	@Override
	public void localize( final long[] position )
	{
		cursor.localize( position );
	}

	@Override
	public int getIntPosition( final int d )
	{
		return cursor.getIntPosition( d );
	}

	@Override
	public long getLongPosition( final int d )
	{
		return cursor.getLongPosition( d );
	}
}
