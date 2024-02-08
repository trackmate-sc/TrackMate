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

import static fiji.plugin.trackmate.SpotCollection.VISIBILITY;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableMap;

import fiji.plugin.trackmate.util.AlphanumComparator;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imglib2.EuclideanSpace;
import net.imglib2.IterableInterval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessible;
import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

/**
 * Interface for spots, used in TrackMate to represent a detection, or an object
 * to be tracked.
 * <p>
 * This interface privileges a map of String-&gt;Double organization of
 * numerical feature, with the X, Y and Z coordinates stored in this map. This
 * allows for default implementations for many of the {@link RealLocalizable}
 * and {@link RealPositionable} methods of this interface.
 * <p>
 * They are mainly a 3D {@link RealLocalizable}, that store the object position
 * in physical coordinates (um, mm, etc). 2D detections are treated by setting
 * the Z coordinate to 0. Time is treated separately, as a feature.
 *
 * @author Jean-Yves Tinevez
 */
public interface Spot extends RealLocalizable, RealPositionable, RealInterval, Comparable< Spot >, EuclideanSpace
{

	/*
	 * FIELDS
	 */

	public static AtomicInteger IDcounter = new AtomicInteger( -1 );

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public default int compareTo( final Spot o )
	{
		return ID() - o.ID();
	}


	/**
	 * Returns a copy of this spot. The class and all fields will be identical,
	 * except for the {@link #ID()}.
	 * 
	 * @return a new spot.
	 */
	public Spot copy();

	/**
	 * Scales the size of this spot by the specified ratio.
	 * 
	 * @param alpha
	 *            the scale.
	 */
	public void scale( double alpha );

	/**
	 * Returns an iterable that will iterate over all the pixels contained in
	 * this spot.
	 * 
	 * @param ra
	 *            the {@link RandomAccessible} to iterate over. It's the caller
	 *            responsibility to ensure that the {@link RandomAccessible} can
	 *            return values over all the pixels in this spot.
	 * @param calibration
	 *            the pixel size array, use to map pixel integer coordinates to
	 *            the spot physical coordinates.
	 * @param <T>
	 *            the type of pixels in the {@link RandomAccessible}.
	 * @return an iterable.
	 */
	public < T extends RealType< T > > IterableInterval< T > iterable( RandomAccessible< T > ra, double calibration[] );

	/**
	 * Returns an iterable that will iterate over all the pixels contained in
	 * this spot.
	 * 
	 * @param img
	 *            the ImgPlus to iterate over.
	 * @param <T>
	 *            the type of pixels in the {@link RandomAccessible}.
	 * @return an iterable.
	 */
	public default < T extends RealType< T > > IterableInterval< T > iterable( final ImgPlus< T > img )
	{
		return iterable( Views.extendMirrorSingle( img ), TMUtils.getSpatialCalibration( img ) );
	}

	/**
	 * @return the name for this Spot.
	 */
	public String getName();

	/**
	 * Set the name of this Spot.
	 *
	 * @param name
	 *            the name to use.
	 */
	public void setName( final String name );

	/**
	 * Returns the unique ID of this spot. The ID is unique within a session.
	 * 
	 * @return the spot ID.
	 */
	public int ID();

	/**
	 * Return a string representation of this spot, with calculated features.
	 *
	 * @return a string representation of the spot.
	 */
	public default String echo()
	{
		final StringBuilder s = new StringBuilder();
		final String name = getName();
		// Name
		if ( null == name )
			s.append( "Spot: <no name>\n" );
		else
			s.append( "Spot: " + name + "\n" );

		// Frame
		s.append( "Time: " + getFeature( POSITION_T ) + '\n' );

		// Coordinates
		final double[] coordinates = new double[ 3 ];
		localize( coordinates );
		s.append( "Position: " + Util.printCoordinates( coordinates ) + "\n" );

		// Feature list
		final Map< String, Double > features = getFeatures();
		if ( null == features || features.size() < 1 )
			s.append( "No features calculated\n" );
		else
		{
			s.append( "Feature list:\n" );
			double val;
			for ( final String key : features.keySet() )
			{
				s.append( "\t" + key.toString() + ": " );
				val = features.get( key );
				if ( val >= 1e4 )
					s.append( String.format( "%.1g", val ) );
				else
					s.append( String.format( "%.1f", val ) );
				s.append( '\n' );
			}
		}
		return s.toString();
	}

	/*
	 * FEATURE RELATED METHODS
	 */

	/**
	 * Exposes the storage map of features for this spot. Altering the returned
	 * map will alter the spot.
	 *
	 * @return a map of {@link String}s to {@link Double}s.
	 */
	public Map< String, Double > getFeatures();

	/**
	 * Returns the value corresponding to the specified spot feature.
	 *
	 * @param feature
	 *            The feature string to retrieve the stored value for.
	 * @return the feature value, as a {@link Double}. Will be <code>null</code>
	 *         if it has not been set.
	 */
	public Double getFeature( final String feature );

	/**
	 * Stores the specified feature value for this spot.
	 *
	 * @param feature
	 *            the name of the feature to store, as a {@link String}.
	 * @param value
	 *            the value to store, as a {@link Double}. Using
	 *            <code>null</code> will have unpredicted outcomes.
	 */
	public void putFeature( final String feature, final Double value );

	/**
	 * Copy some of the features values of the specified spot to this spot.
	 * 
	 * @param src
	 *            the spot to copy feature values from.
	 * @param features
	 *            the collection of feature keys to copy.
	 */
	public default void copyFeaturesFrom( final Spot src, final Collection< String > features )
	{
		if ( null == features || features.isEmpty() )
			return;

		for ( final String feat : features )
			putFeature( feat, src.getFeature( feat ) );
	}

	/**
	 * Copy all the features value from the specified spot to this spot.
	 * 
	 * @param src
	 *            the spot to copy feature values from.
	 */
	public default void copyFeaturesFrom( final Spot src )
	{
		copyFeaturesFrom( src, src.getFeatures().keySet() );
	}

	/**
	 * Returns the difference of the feature value for this spot with the one of
	 * the specified spot. By construction, this operation is anti-symmetric (
	 * <code>A.diffTo(B) = - B.diffTo(A)</code>).
	 * <p>
	 * Will generate a {@link NullPointerException} if one of the spots does not
	 * store the named feature.
	 *
	 * @param s
	 *            the spot to compare to.
	 * @param feature
	 *            the name of the feature to use for calculation.
	 * @return the difference in feature value.
	 */
	public default double diffTo( final Spot s, final String feature )
	{
		final double f1 = getFeature( feature ).doubleValue();
		final double f2 = s.getFeature( feature ).doubleValue();
		return f1 - f2;
	}

	/**
	 * Returns the absolute normalized difference of the feature value of this
	 * spot with the one of the given spot.
	 * <p>
	 * If <code>a</code> and <code>b</code> are the feature values, then the
	 * absolute normalized difference is defined as
	 * <code>Math.abs( a - b) / ( (a+b)/2 )</code>.
	 * <p>
	 * By construction, this operation is symmetric (
	 * <code>A.normalizeDiffTo(B) =
	 * B.normalizeDiffTo(A)</code>).
	 * <p>
	 * Will generate a {@link NullPointerException} if one of the spots does not
	 * store the named feature.
	 *
	 * @param s
	 *            the spot to compare to.
	 * @param feature
	 *            the name of the feature to use for calculation.
	 * @return the absolute normalized difference feature value.
	 */
	public default double normalizeDiffTo( final Spot s, final String feature )
	{
		final double a = getFeature( feature ).doubleValue();
		final double b = s.getFeature( feature ).doubleValue();
		if ( a == -b )
			return 0d;

		return Math.abs( a - b ) / ( ( a + b ) / 2 );
	}

	/**
	 * Returns the square distance from this spot to the specified spot.
	 *
	 * @param s
	 *            the spot to compute the square distance to.
	 * @return the square distance as a <code>double</code>.
	 */
	public default double squareDistanceTo( final RealLocalizable s )
	{
		double sumSquared = 0d;
		for ( int d = 0; d < 3; d++ )
		{
			final double dx = this.getDoublePosition( d ) - s.getDoublePosition( d );
			sumSquared += dx * dx;
		}
		return sumSquared;
	}

	/*
	 * PUBLIC UTILITY CONSTANTS
	 */

	/*
	 * STATIC KEYS
	 */

	/** The name of the spot quality feature. */
	public static final String QUALITY = "QUALITY";

	/** The name of the radius spot feature. */
	public static final String RADIUS = "RADIUS";

	/** The name of the spot X position feature. */
	public static final String POSITION_X = "POSITION_X";

	/** The name of the spot Y position feature. */
	public static final String POSITION_Y = "POSITION_Y";

	/** The name of the spot Z position feature. */
	public static final String POSITION_Z = "POSITION_Z";

	/** The name of the spot T position feature. */
	public static final String POSITION_T = "POSITION_T";

	/** The name of the frame feature. */
	public static final String FRAME = "FRAME";

	/** The position features. */
	public final static String[] POSITION_FEATURES = new String[] { POSITION_X, POSITION_Y, POSITION_Z };

	/**
	 * The 8 privileged spot features that must be set by a spot detector:
	 * {@link #QUALITY}, {@link #POSITION_X}, {@link #POSITION_Y},
	 * {@link #POSITION_Z}, {@link #POSITION_Z}, {@link #RADIUS},
	 * {@link #FRAME}, {@link SpotCollection#VISIBILITY}.
	 */
	public final static Collection< String > FEATURES = Arrays.asList( QUALITY,
			POSITION_X, POSITION_Y, POSITION_Z, POSITION_T, FRAME, RADIUS, SpotCollection.VISIBILITY );

	/** The 8 privileged spot feature names. */
	public final static Map< String, String > FEATURE_NAMES = ImmutableMap.of(
			POSITION_X, "X",
			POSITION_Y, "Y",
			POSITION_Z, "Z",
			POSITION_T, "T",
			FRAME, "Frame",
			RADIUS, "Radius",
			QUALITY, "Quality",
			VISIBILITY, "Visibility" );

	/** The 8 privileged spot feature short names. */
	public final static Map< String, String > FEATURE_SHORT_NAMES = ImmutableMap.of(
			POSITION_X, "X",
			POSITION_Y, "Y",
			POSITION_Z, "Z",
			POSITION_T, "T",
			FRAME, "Frame",
			RADIUS, "R",
			QUALITY, "Quality",
			VISIBILITY, "Visibility" );

	/** The 8 privileged spot feature dimensions. */
	public final static Map< String, Dimension > FEATURE_DIMENSIONS = ImmutableMap.of(
			POSITION_X, Dimension.POSITION,
			POSITION_Y, Dimension.POSITION,
			POSITION_Z, Dimension.POSITION,
			POSITION_T, Dimension.TIME,
			FRAME, Dimension.NONE,
			RADIUS, Dimension.LENGTH,
			QUALITY, Dimension.QUALITY,
			VISIBILITY, Dimension.NONE );

	/** The 8 privileged spot feature isInt flags. */
	public final static Map< String, Boolean > IS_INT = ImmutableMap.of(
			POSITION_X, Boolean.FALSE,
			POSITION_Y, Boolean.FALSE,
			POSITION_Z, Boolean.FALSE,
			POSITION_T, Boolean.FALSE,
			FRAME, Boolean.TRUE,
			RADIUS, Boolean.FALSE,
			QUALITY, Boolean.FALSE,
			VISIBILITY, Boolean.TRUE );

	/*
	 * REALPOSITIONABLE, REAlLOCALIZABLE
	 */

	@Override
	default int numDimensions()
	{
		return 3;
	}

	@Override
	public default void move( final float distance, final int d )
	{
		putFeature( POSITION_FEATURES[ d ], getFeature( POSITION_FEATURES[ d ] ) + distance );
	}

	@Override
	public default void move( final double distance, final int d )
	{
		putFeature( POSITION_FEATURES[ d ], getFeature( POSITION_FEATURES[ d ] ) + distance );
	}

	@Override
	public default void move( final RealLocalizable distance )
	{
		for ( int d = 0; d < 3; d++ )
			putFeature( POSITION_FEATURES[ d ], getFeature( POSITION_FEATURES[ d ] ) + distance.getDoublePosition( d ) );
	}

	@Override
	public default void move( final float[] distance )
	{
		for ( int d = 0; d < 3; d++ )
			putFeature( POSITION_FEATURES[ d ], getFeature( POSITION_FEATURES[ d ] ) + distance[ d ] );
	}

	@Override
	public default void move( final double[] distance )
	{
		for ( int d = 0; d < 3; d++ )
			putFeature( POSITION_FEATURES[ d ], getFeature( POSITION_FEATURES[ d ] ) + distance[ d ] );
	}

	@Override
	public default void setPosition( final RealLocalizable position )
	{
		for ( int d = 0; d < 3; d++ )
			putFeature( POSITION_FEATURES[ d ], position.getDoublePosition( d ) );
	}

	@Override
	public default void setPosition( final float[] position )
	{
		for ( int d = 0; d < 3; d++ )
			putFeature( POSITION_FEATURES[ d ], ( double ) position[ d ] );
	}

	@Override
	public default void setPosition( final double[] position )
	{
		for ( int d = 0; d < 3; d++ )
			putFeature( POSITION_FEATURES[ d ], position[ d ] );
	}

	@Override
	public default void setPosition( final float position, final int d )
	{
		putFeature( POSITION_FEATURES[ d ], ( double ) position );
	}

	@Override
	public default void setPosition( final double position, final int d )
	{
		putFeature( POSITION_FEATURES[ d ], position );
	}

	@Override
	public default void fwd( final int d )
	{
		move( 1., d );
	}

	@Override
	public default void bck( final int d )
	{
		move( -1., d );
	}

	@Override
	public default void move( final int distance, final int d )
	{
		move( ( double ) distance, d );
	}

	@Override
	public default void move( final long distance, final int d )
	{
		move( ( double ) distance, d );
	}

	@Override
	public default void move( final Localizable distance )
	{
		move( ( RealLocalizable ) distance );
	}

	@Override
	public default void move( final int[] distance )
	{
		for ( int d = 0; d < 3; d++ )
			putFeature( POSITION_FEATURES[ d ], getFeature( POSITION_FEATURES[ d ] + distance[ d ] ) );
	}

	@Override
	public default void move( final long[] distance )
	{
		for ( int d = 0; d < 3; d++ )
			putFeature( POSITION_FEATURES[ d ], getFeature( POSITION_FEATURES[ d ] + distance[ d ] ) );
	}

	@Override
	public default void setPosition( final Localizable position )
	{
		setPosition( ( RealLocalizable ) position );
	}

	@Override
	public default void setPosition( final int[] position )
	{
		for ( int d = 0; d < 3; d++ )
			putFeature( POSITION_FEATURES[ d ], ( double ) position[ d ] );
	}

	@Override
	public default void setPosition( final long[] position )
	{
		for ( int d = 0; d < 3; d++ )
			putFeature( POSITION_FEATURES[ d ], ( double ) position[ d ] );
	}

	@Override
	public default void setPosition( final int position, final int d )
	{
		putFeature( POSITION_FEATURES[ d ], ( double ) position );
	}

	@Override
	public default void setPosition( final long position, final int d )
	{
		putFeature( POSITION_FEATURES[ d ], ( double ) position );
	}

	@Override
	public default void localize( final float[] position )
	{
		for ( int d = 0; d < 3; ++d )
			position[ d ] = getFloatPosition( d );
	}

	@Override
	public default void localize( final double[] position )
	{
		for ( int d = 0; d < 3; ++d )
			position[ d ] = getDoublePosition( d );
	}

	@Override
	public default float getFloatPosition( final int d )
	{
		return ( float ) getDoublePosition( d );
	}

	@Override
	public default double getDoublePosition( final int d )
	{
		return getFeature( POSITION_FEATURES[ d ] );
	}

	/*
	 * STATIC UTILITY
	 */

	/**
	 * A comparator used to sort spots by ascending feature values.
	 *
	 * @param feature
	 *            the feature to use for comparison. It is the caller
	 *            responsibility to ensure that all spots have the target
	 *            feature.
	 * @return a new {@link Comparator}.
	 */
	public static Comparator< Spot > featureComparator( final String feature )
	{
		final Comparator< Spot > comparator = new Comparator< Spot >()
		{
			@Override
			public int compare( final Spot o1, final Spot o2 )
			{
				final double diff = o2.diffTo( o1, feature );
				if ( diff == 0 )
					return 0;
				else if ( diff < 0 )
					return 1;
				else
					return -1;
			}
		};
		return comparator;
	}

	/** A comparator used to sort spots by ascending time feature. */
	public final static Comparator< Spot > timeComparator = featureComparator( POSITION_T );

	/** A comparator used to sort spots by ascending frame. */
	public final static Comparator< Spot > frameComparator = featureComparator( FRAME );

	/**
	 * A comparator used to sort spots by name. The comparison uses numerical
	 * natural sorting, So that "Spot_4" comes before "Spot_122".
	 */
	public final static Comparator< Spot > nameComparator = new Comparator< Spot >()
	{
		private final Comparator< String > comparator = AlphanumComparator.instance;

		@Override
		public int compare( final Spot o1, final Spot o2 )
		{
			return comparator.compare( o1.getName(), o2.getName() );
		}
	};
}
