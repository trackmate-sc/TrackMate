package fiji.plugin.trackmate.io.geff.imglib2;

import static java.lang.invoke.MethodHandles.collectArguments;
import static java.lang.invoke.MethodType.methodType;
import static org.mastodon.geff.imglib2.ElementType.NODE;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.mastodon.geff.imglib2.GeffProperties;
import org.mastodon.geff.imglib2.GeffProperty;
import org.mastodon.geff.imglib2.IoUtils;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.SpotCollection;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Cast;

public class ReadTrackMateSpotsDemo
{

	public static void main( final String[] args ) throws Throwable
	{
		final String path = "samples/FakeTracks.geff";

		final Model model = new Model();
		try (final N5Reader n5 = new N5ZarrReader( path ))
		{
			final GeffProperties props = IoUtils.loadProperties( n5, NODE );
			final SpotBaseBuilder2D nodeBuilder = new SpotBaseBuilder2D( model.getSpots() );
			buildNodes( nodeBuilder, props );
		}

		System.out.println( "Deserialized model: " + model.getSpots() );
		System.out.println( "Example spot:\n" + model.getSpots().iterable( true ).iterator().next().echo() );
	}

	static class SpotBaseBuilder2D
	{

		private final SpotCollection spots;

		public SpotBaseBuilder2D( final SpotCollection spots )
		{
			this.spots = spots;
		}

		public void addVertex(
				@FromId( ) final long id,
				@FromProperty( "POSITION_X" ) final double x,
				@FromProperty( "POSITION_Y" ) final double y,
				@FromProperty( "FRAME" ) final long t,
				@FromProperty( "radius" ) final double r,
				@FromProperty( "name" ) final String name,
				@AllProperties( ) final GeffProperties properties )
		{
			final SpotBase spot = new SpotBase( ( int ) id );
			spot.setPosition( x, 0 );
			spot.setPosition( y, 1 );
			spot.setPosition( 0., 2 );
			spot.putFeature( Spot.RADIUS, r );
			spot.putFeature( Spot.FRAME, Double.valueOf( t ) );
			spot.setName( name );

			// Add to collection
			spots.add( spot, ( int ) t );

			// Deserialize other properties
			final Map< String, GeffProperty< ? > > props = properties.properties();
			for ( final String feature : props.keySet() )
			{
				final GeffProperty< ? > prop = props.get( feature );
				if ( prop.type() instanceof RealType< ? > )
				{
					@SuppressWarnings( "rawtypes" )
					final double val = ( ( RealType ) prop.getAt() ).getRealDouble();
					spot.putFeature( feature, val );
					continue;
				}
				System.out.println( "TrackMate Spot cannot store non scalar properties like " + feature );
			}
		}
	}

	// -------------------------------

	@Retention( RetentionPolicy.RUNTIME )
	@Target( ElementType.PARAMETER )
	public @interface AllProperties
	{}

	@Retention( RetentionPolicy.RUNTIME )
	@Target( ElementType.PARAMETER )
	public @interface FromProperty
	{
		String value();
	}

	@Retention( RetentionPolicy.RUNTIME )
	@Target( ElementType.PARAMETER )
	public @interface FromId
	{}

	public static void buildNodes( final Object target, final GeffProperties properties ) throws Throwable
	{
		final Method[] methods = target.getClass().getMethods();
		final Method method = Arrays.stream( methods ).filter( m -> m.getName().equals( "addVertex" ) ).findFirst().get();
		final AttributeParam[] attributeParams = getAttributes( method );
		final MethodHandles.Lookup lk = MethodHandles.lookup();
		MethodHandle mh = lk.unreflect( method ).bindTo( target );

		for ( int i = 0; i < attributeParams.length; i++ )
		{
			final AttributeParam param = attributeParams[ i ];
			final MethodHandle supplier = propertyHandle( lk, properties, param );
			mh = collectArguments( mh, 0, supplier );
		}

		for ( int i = 0; i < properties.numElements(); i++ )
		{
			properties.elementIndex().index( i );
			mh.invokeExact();
		}
	}

	private static MethodHandle propertyHandle(
			final MethodHandles.Lookup lookup,
			final GeffProperties properties,
			final AttributeParam param ) throws NoSuchMethodException, IllegalAccessException
	{
		final GeffProperty< ? > property = param.isId()
				? properties.id()
				: properties.properties().get( param.identifier() );
		final Type rawType = getRawType( param.type() );
		if ( rawType == long.class )
		{
			final GeffProperty< ? > longProperty = property.type() instanceof GenericLongType ? property : property.convert( LongType::new );
//            checkTypeMatch(property.type().getClass(), UnsignedLongType.class);
			final LongSupplier s = asLongSupplier( Cast.unchecked( longProperty ) );
			return lookup.findVirtual( LongSupplier.class, "getAsLong", methodType( long.class ) ).bindTo( s );
		}
		else if ( rawType == double.class )
		{
			checkTypeMatch( property.type().getClass(), DoubleType.class );
			final DoubleSupplier s = asDoubleSupplier( Cast.unchecked( property ) );
			return lookup.findVirtual( DoubleSupplier.class, "getAsDouble", methodType( double.class ) ).bindTo( s );
		}
		else if ( rawType == double[].class )
		{
			checkTypeMatch( property.type().getClass(), DoubleType.class );
			final Supplier< double[] > s = asDoubleArraySupplier( Cast.unchecked( property ) );
			return lookup.findVirtual( Supplier.class, "get", methodType( Object.class ) ).bindTo( s )
					.asType( methodType( double[].class ) );
		}
		else if ( rawType == GeffProperty.class )
		{
			final Supplier< GeffProperty< ? > > s = () -> property;
			return lookup.findVirtual( Supplier.class, "get", methodType( Object.class ) ).bindTo( s )
					.asType( methodType( GeffProperty.class ) );
		}
		else if ( rawType == Optional.class )
		{
			final Type type = ( ( ParameterizedType ) param.type() ).getActualTypeArguments()[ 0 ];
			if ( type == double[].class )
			{
				checkTypeMatch( property.type().getClass(), DoubleType.class );
				final Supplier< Optional< double[] > > s = asOptionalDoubleArraySupplier( Cast.unchecked( property ) );
				return lookup.findVirtual( Supplier.class, "get", methodType( Object.class ) ).bindTo( s )
						.asType( methodType( byte[].class ) );

			}
		}
		else if ( rawType == String.class )
		{
			// Strings are saved as byte arrays
			checkTypeMatch( property.type().getClass(), UnsignedByteType.class );
			final Supplier< String > s = asStringSupplier( Cast.unchecked( property ) );
			return lookup.findVirtual( Supplier.class, "get", methodType( Object.class ) ).bindTo( s )
					.asType( methodType( String.class ) );
		}
		else if ( rawType == GeffProperties.class )
		{
			return MethodHandles.constant( GeffProperties.class, properties );
		}

		System.out.println( "param = " + param );
		throw new IllegalArgumentException( "TODO" );
	}

	private static Class< ? > getRawType( final Type t )
	{
		if ( t instanceof Class< ? > )
			return ( Class< ? > ) t;
		if ( t instanceof ParameterizedType )
			return ( Class< ? > ) ( ( ParameterizedType ) t ).getRawType();
		throw new IllegalArgumentException( "TODO" );
	}

	private static < T extends GenericLongType< T > > LongSupplier asLongSupplier( final GeffProperty< T > property )
	{
		return () -> property.getAt().getLong();
	}

	private static DoubleSupplier asDoubleSupplier( final GeffProperty< DoubleType > property )
	{
		return () -> property.getAt().get();
	}

	private static Supplier< double[] > asDoubleArraySupplier( final GeffProperty< DoubleType > property )
	{
		return () -> {
			final int len = ( int ) property.values().dimension( 0 );
			final double[] array = new double[ len ];
			for ( int i = 0; i < len; i++ )
				array[ i ] = property.getAt( i ).get();
			return array;
		};
	}

	private static Supplier< String > asStringSupplier( final GeffProperty< UnsignedByteType > property )
	{
		return () -> {
			final int len = ( int ) property.values().dimension( 0 );
			final byte[] array = new byte[ len ];
			for ( int i = 0; i < len; i++ )
				array[ i ] = property.getAt( i ).getByte();
			return new String( array, java.nio.charset.StandardCharsets.UTF_8 );
		};
	}

	private static Supplier< Optional< double[] > > asOptionalDoubleArraySupplier( final GeffProperty< DoubleType > property )
	{
		return () -> {
			if ( property.isMissing() )
				return Optional.empty();
			final int len = ( int ) property.values().dimension( 0 );
			final double[] array = new double[ len ];
			for ( int i = 0; i < len; i++ )
				array[ i ] = property.getAt( i ).get();
			return Optional.of( array );
		};
	}

	private static void checkTypeMatch( final Class< ? > expected, final Class< ? > actual )
	{
		if ( !expected.isAssignableFrom( actual ) )
			throw new IllegalArgumentException( "wrong property type: " + actual.getSimpleName() + " (expected " + expected.getSimpleName() + ")" );
	}

	record AttributeParam( Type type, String identifier, boolean isId )
	{}

	private static AttributeParam[] getAttributes( final Method method )
	{
		final Type[] types = method.getGenericParameterTypes();
		final Annotation[][] annotations = method.getParameterAnnotations();
		final AttributeParam[] params = new AttributeParam[ types.length ];
		Arrays.setAll( params, i -> resolveParameter( types[ i ], annotations[ i ] ) );
		return params;
	}

	private static AttributeParam resolveParameter(
			final Type parameterType,
			final Annotation[] parameterAnnotations )
	{

		final String error = "Every parameter must have exactly one @FromProperty or @FromId annotation";
		AttributeParam result = null;
		for ( final Annotation a : parameterAnnotations )
		{
			if ( a instanceof FromProperty )
			{
				if ( result != null )
					throw new IllegalStateException( error );
				result = new AttributeParam( parameterType, ( ( FromProperty ) a ).value(), false );
			}
			else if ( a instanceof FromId )
			{
				if ( result != null )
					throw new IllegalStateException( error );
				result = new AttributeParam( parameterType, "id", true );
			}
			else if ( a instanceof AllProperties )
			{
				if ( result != null )
					throw new IllegalStateException( error );
				result = new AttributeParam( parameterType, "properties", false );
			}
		}
		if ( result == null )
			throw new IllegalStateException( error );
		return result;
	}
}
