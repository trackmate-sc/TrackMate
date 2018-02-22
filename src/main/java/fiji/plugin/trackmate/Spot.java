package fiji.plugin.trackmate;

import static fiji.plugin.trackmate.SpotCollection.VISIBLITY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import fiji.plugin.trackmate.util.AlphanumComparator;
import net.imglib2.AbstractEuclideanSpace;
import net.imglib2.RealLocalizable;
import net.imglib2.util.Util;

/**
 * A {@link RealLocalizable} implementation, used in TrackMate to represent a
 * detection.
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
 * @author Jean-Yves Tinevez &lt;jeanyves.tinevez@gmail.com&gt; 2010, 2013
 *
 */
public class Spot extends AbstractEuclideanSpace implements RealLocalizable, Comparable< Spot >
{

	/*
	 * FIELDS
	 */

	public static AtomicInteger IDcounter = new AtomicInteger( -1 );

	/** Store the individual features, and their values. */
	private final ConcurrentHashMap< String, Double > features = new ConcurrentHashMap< >();

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
	public Spot( final double x, final double y, final double z, final double radius, final double quality, final String name )
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
	public Spot( final double x, final double y, final double z, final double radius, final double quality )
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
	public Spot( final RealLocalizable location, final double radius, final double quality, final String name )
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
	public Spot( final RealLocalizable location, final double radius, final double quality )
	{
		this( location, radius, quality, null );
	}

	/**
	 * Creates a new spot, taking its location, its radius, its quality value
	 * and its name from the specified spot.
	 *
	 * @param spot
	 *            the spot to read from.
	 */
	public Spot( final Spot spot )
	{
		this( spot, spot.getFeature( RADIUS ), spot.getFeature( QUALITY ), spot.getName() );
	}

	/**
	 * Blank constructor meant to be used when loading a spot collection from a
	 * file. <b>Will</b> mess with the {@link #IDcounter} field, so this
	 * constructor <u>should not be used for normal spot creation</u>.
	 *
	 * @param ID
	 *            the spot ID to set
	 */
	public Spot( final int ID )
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
	public int hashCode()
	{
		return ID;
	}

	@Override
	public int compareTo( final Spot o )
	{
		return ID - o.ID;
	}

	@Override
	public boolean equals( final Object other )
	{
		if ( other == null )
			return false;
		if ( other == this )
			return true;
		if ( !( other instanceof Spot ) )
			return false;
		final Spot os = ( Spot ) other;
		return os.ID == this.ID;
	}

	/**
	 * @return the name for this Spot.
	 */
	public String getName()
	{
		return this.name;
	}

	/**
	 * Set the name of this Spot.
	 * 
	 * @param name
	 *            the name to use.
	 */
	public void setName( final String name )
	{
		this.name = name;
	}

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

	/**
	 * Return a string representation of this spot, with calculated features.
	 * 
	 * @return a string representation of the spot.
	 */
	public String echo()
	{
		final StringBuilder s = new StringBuilder();

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
	public Map< String, Double > getFeatures()
	{
		return features;
	}

	/**
	 * Returns the value corresponding to the specified spot feature.
	 *
	 * @param feature
	 *            The feature string to retrieve the stored value for.
	 * @return the feature value, as a {@link Double}. Will be <code>null</code>
	 *         if it has not been set.
	 */
	public final Double getFeature( final String feature )
	{
		return features.get( feature );
	}

	/**
	 * Stores the specified feature value for this spot.
	 *
	 * @param feature
	 *            the name of the feature to store, as a {@link String}.
	 * @param value
	 *            the value to store, as a {@link Double}. Using
	 *            <code>null</code> will have unpredicted outcomes.
	 */
	public final void putFeature( final String feature, final Double value )
	{
		features.put( feature, value );
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
	public double diffTo( final Spot s, final String feature )
	{
		final double f1 = features.get( feature ).doubleValue();
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
	public double normalizeDiffTo( final Spot s, final String feature )
	{
		final double a = features.get( feature ).doubleValue();
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
	public double squareDistanceTo( final Spot s )
	{
		double sumSquared = 0d;
		double thisVal, otherVal;

		for ( final String f : POSITION_FEATURES )
		{
			thisVal = features.get( f ).doubleValue();
			otherVal = s.getFeature( f ).doubleValue();
			sumSquared += ( otherVal - thisVal ) * ( otherVal - thisVal );
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
	 * The 7 privileged spot features that must be set by a spot detector:
	 * {@link #QUALITY}, {@link #POSITION_X}, {@link #POSITION_Y},
	 * {@link #POSITION_Z}, {@link #POSITION_Z}, {@link #RADIUS}, {@link #FRAME}
	 * .
	 */
	public final static Collection< String > FEATURES = new ArrayList< >( 7 );

	/** The 7 privileged spot feature names. */
	public final static Map< String, String > FEATURE_NAMES = new HashMap< >( 7 );

	/** The 7 privileged spot feature short names. */
	public final static Map< String, String > FEATURE_SHORT_NAMES = new HashMap< >( 7 );

	/** The 7 privileged spot feature dimensions. */
	public final static Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap< >( 7 );

	/** The 7 privileged spot feature isInt flags. */
	public final static Map< String, Boolean > IS_INT = new HashMap< >( 7 );

	static
	{
		FEATURES.add( QUALITY );
		FEATURES.add( POSITION_X );
		FEATURES.add( POSITION_Y );
		FEATURES.add( POSITION_Z );
		FEATURES.add( POSITION_T );
		FEATURES.add( FRAME );
		FEATURES.add( RADIUS );
		FEATURES.add( SpotCollection.VISIBLITY );

		FEATURE_NAMES.put( POSITION_X, "X" );
		FEATURE_NAMES.put( POSITION_Y, "Y" );
		FEATURE_NAMES.put( POSITION_Z, "Z" );
		FEATURE_NAMES.put( POSITION_T, "T" );
		FEATURE_NAMES.put( FRAME, "Frame" );
		FEATURE_NAMES.put( RADIUS, "Radius" );
		FEATURE_NAMES.put( QUALITY, "Quality" );
		FEATURE_NAMES.put( VISIBLITY, "Visibility" );

		FEATURE_SHORT_NAMES.put( POSITION_X, "X" );
		FEATURE_SHORT_NAMES.put( POSITION_Y, "Y" );
		FEATURE_SHORT_NAMES.put( POSITION_Z, "Z" );
		FEATURE_SHORT_NAMES.put( POSITION_T, "T" );
		FEATURE_SHORT_NAMES.put( FRAME, "Frame" );
		FEATURE_SHORT_NAMES.put( RADIUS, "R" );
		FEATURE_SHORT_NAMES.put( QUALITY, "Quality" );
		FEATURE_SHORT_NAMES.put( VISIBLITY, "Visibility" );

		FEATURE_DIMENSIONS.put( POSITION_X, Dimension.POSITION );
		FEATURE_DIMENSIONS.put( POSITION_Y, Dimension.POSITION );
		FEATURE_DIMENSIONS.put( POSITION_Z, Dimension.POSITION );
		FEATURE_DIMENSIONS.put( POSITION_T, Dimension.TIME );
		FEATURE_DIMENSIONS.put( FRAME, Dimension.NONE );
		FEATURE_DIMENSIONS.put( RADIUS, Dimension.LENGTH );
		FEATURE_DIMENSIONS.put( QUALITY, Dimension.QUALITY );
		FEATURE_DIMENSIONS.put( VISIBLITY, Dimension.NONE );

		IS_INT.put( POSITION_X, Boolean.FALSE );
		IS_INT.put( POSITION_Y, Boolean.FALSE );
		IS_INT.put( POSITION_Z, Boolean.FALSE );
		IS_INT.put( POSITION_T, Boolean.FALSE );
		IS_INT.put( FRAME, Boolean.TRUE );
		IS_INT.put( RADIUS, Boolean.FALSE );
		IS_INT.put( QUALITY, Boolean.FALSE );
		IS_INT.put( VISIBLITY, Boolean.TRUE );
	}

	@Override
	public void localize( final float[] position )
	{
		assert ( position.length >= n );
		for ( int d = 0; d < n; ++d )
			position[ d ] = getFloatPosition( d );
	}

	@Override
	public void localize( final double[] position )
	{
		assert ( position.length >= n );
		for ( int d = 0; d < n; ++d )
			position[ d ] = getDoublePosition( d );
	}

	@Override
	public float getFloatPosition( final int d )
	{
		return ( float ) getDoublePosition( d );
	}

	@Override
	public double getDoublePosition( final int d )
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
	public final static Comparator< Spot > featureComparator( final String feature )
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
		private final AlphanumComparator comparator = AlphanumComparator.instance;

		@Override
		public int compare( final Spot o1, final Spot o2 )
		{
			return comparator.compare( o1.getName(), o2.getName() );
		}
	};
}
