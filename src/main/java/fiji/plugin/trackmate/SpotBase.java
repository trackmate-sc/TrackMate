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
package fiji.plugin.trackmate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import fiji.plugin.trackmate.util.SpotNeighborhood;
import net.imglib2.AbstractEuclideanSpace;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RealLocalizable;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * A {@link RealLocalizable} implementation of {@link Spot}, used in TrackMate
 * to represent a detection. This concrete implementation has the simplest
 * shape: a spot is a sphere of fixed radius.
 * <p>
 * On top of being a {@link RealLocalizable}, it can store additional numerical
 * named features, with a {@link Map}-like syntax. Constructors enforce the
 * specification of the spot location in 3D space (if Z is unused, put 0), the
 * spot radius, and the spot quality. This somewhat cumbersome syntax is made to
 * avoid any bad surprise with missing features in a subsequent use. The spot
 * temporal features ({@link #FRAME} and {@link #POSITION_T}) are set upon
 * adding to a {@link SpotCollection}.
 * <p>
 * Each spot received at creation a unique ID (as an <code>int</code>), used
 * later for saving, retrieving and loading. Interfering with this value will
 * predictively cause undesired behavior.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class SpotBase extends AbstractEuclideanSpace implements Spot
{

	/*
	 * FIELDS
	 */

	public static AtomicInteger IDcounter = new AtomicInteger( -1 );

	/** Store the individual features, and their values. */
	private final ConcurrentHashMap< String, Double > features = new ConcurrentHashMap<>();

	/** A user-supplied name for this spot. */
	private String name;

	/** This spot ID. */
	private final int ID;

	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Creates a new spot.
	 *
	 * @param x
	 *            the spot X coordinates, in image units.
	 * @param y
	 *            the spot Y coordinates, in image units.
	 * @param z
	 *            the spot Z coordinates, in image units.
	 * @param radius
	 *            the spot radius, in image units.
	 * @param quality
	 *            the spot quality.
	 * @param name
	 *            the spot name.
	 */
	public SpotBase( final double x, final double y, final double z, final double radius, final double quality, final String name )
	{
		super( 3 );
		this.ID = IDcounter.incrementAndGet();
		putFeature( POSITION_X, Double.valueOf( x ) );
		putFeature( POSITION_Y, Double.valueOf( y ) );
		putFeature( POSITION_Z, Double.valueOf( z ) );
		putFeature( RADIUS, Double.valueOf( radius ) );
		putFeature( QUALITY, Double.valueOf( quality ) );
		if ( null == name )
		{
			this.name = "ID" + ID;
		}
		else
		{
			this.name = name;
		}
	}

	/**
	 * Creates a new spot, and gives it a default name.
	 *
	 * @param x
	 *            the spot X coordinates, in image units.
	 * @param y
	 *            the spot Y coordinates, in image units.
	 * @param z
	 *            the spot Z coordinates, in image units.
	 * @param radius
	 *            the spot radius, in image units.
	 * @param quality
	 *            the spot quality.
	 */
	public SpotBase( final double x, final double y, final double z, final double radius, final double quality )
	{
		this( x, y, z, radius, quality, null );
	}

	/**
	 * Creates a new spot, taking its 3D coordinates from a
	 * {@link RealLocalizable}. The {@link RealLocalizable} must have at least 3
	 * dimensions, and must return coordinates in image units.
	 *
	 * @param location
	 *            the {@link RealLocalizable} that contains the spot locatiob.
	 * @param radius
	 *            the spot radius, in image units.
	 * @param quality
	 *            the spot quality.
	 * @param name
	 *            the spot name.
	 */
	public SpotBase( final RealLocalizable location, final double radius, final double quality, final String name )
	{
		this( location.getDoublePosition( 0 ), location.getDoublePosition( 1 ), location.getDoublePosition( 2 ), radius, quality, name );
	}

	/**
	 * Creates a new spot, taking its 3D coordinates from a
	 * {@link RealLocalizable}. The {@link RealLocalizable} must have at least 3
	 * dimensions, and must return coordinates in image units. The spot will get
	 * a default name.
	 *
	 * @param location
	 *            the {@link RealLocalizable} that contains the spot locatiob.
	 * @param radius
	 *            the spot radius, in image units.
	 * @param quality
	 *            the spot quality.
	 */
	public SpotBase( final RealLocalizable location, final double radius, final double quality )
	{
		this( location, radius, quality, null );
	}

	/**
	 * Creates a new spot, taking its location, its radius, its quality value
	 * and its name from the specified spot.
	 *
	 * @param oldSpot
	 *            the spot to read from.
	 */
	public SpotBase( final Spot oldSpot )
	{
		this( oldSpot, oldSpot.getFeature( RADIUS ), oldSpot.getFeature( QUALITY ), oldSpot.getName() );
	}

	/**
	 * Blank constructor meant to be used when loading a spot collection from a
	 * file. <b>Will</b> mess with the {@link #IDcounter} field, so this
	 * constructor <u>should not be used for normal spot creation</u>.
	 *
	 * @param ID
	 *            the spot ID to set
	 */
	public SpotBase( final int ID )
	{
		super( 3 );
		this.ID = ID;
		synchronized ( IDcounter )
		{
			if ( IDcounter.get() < ID )
			{
				IDcounter.set( ID );
			}
		}
	}

	/*
	 * PUBLIC METHODS
	 */
	
	@Override
	public SpotBase copy()
	{
		final SpotBase o = new SpotBase( this );
		o.copyFeaturesFrom( this );
		return o;
	}

	@Override
	public void scale( final double alpha )
	{
		final double radius = getFeature( Spot.RADIUS );
		final double newRadius = radius * alpha;
		putFeature( Spot.RADIUS, newRadius );
	}

	@Override
	public int hashCode()
	{
		return ID;
	}

	@Override
	public boolean equals( final Object other )
	{
		if ( other == null )
			return false;
		if ( other == this )
			return true;
		if ( !( other instanceof SpotBase ) )
			return false;
		final SpotBase os = ( SpotBase ) other;
		return os.ID == this.ID;
	}

	@Override
	public String getName()
	{
		return this.name;
	}

	@Override
	public void setName( final String name )
	{
		this.name = name;
	}

	@Override
	public int ID()
	{
		return ID;
	}

	@Override
	public String toString()
	{
		String str;
		if ( null == name || name.equals( "" ) )
			str = "ID" + ID;
		else
			str = name;
		return str;
	}

	/*
	 * FEATURE RELATED METHODS
	 */

	@Override
	public Map< String, Double > getFeatures()
	{
		return features;
	}

	@Override
	public Double getFeature( final String feature )
	{
		return features.get( feature );
	}

	@Override
	public void putFeature( final String feature, final Double value )
	{
		features.put( feature, value );
	}

	@Override
	public double realMin( final int d )
	{
		return getDoublePosition( d ) - getFeature( SpotBase.RADIUS );
	}

	@Override
	public double realMax( final int d )
	{
		return getDoublePosition( d ) + getFeature( SpotBase.RADIUS );
	}

	@Override
	public < T extends RealType< T > > IterableInterval< T > iterable( final RandomAccessible< T > ra, final double[] calibration )
	{
		final double r = features.get( Spot.RADIUS ).doubleValue();
		if ( r / calibration[ 0 ] <= 1. && r / calibration[ 2 ] <= 1. )
			return makeSinglePixelIterable( this, ra, calibration );

		return new SpotNeighborhood<>( this, ra, calibration );
	}

	private static < T > IterableInterval< T > makeSinglePixelIterable( final RealLocalizable center, final RandomAccessible< T > img, final double[] calibration )
	{
		final long[] min = new long[ img.numDimensions() ];
		final long[] max = new long[ img.numDimensions() ];
		for ( int d = 0; d < min.length; d++ )
		{
			final long cx = Math.round( center.getDoublePosition( d ) / calibration[ d ] );
			min[ d ] = cx;
			max[ d ] = cx + 1;
		}

		final Interval interval = new FinalInterval( min, max );
		return Views.interval( img, interval );
	}
}
