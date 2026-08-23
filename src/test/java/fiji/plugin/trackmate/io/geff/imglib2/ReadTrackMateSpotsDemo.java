package fiji.plugin.trackmate.io.geff.imglib2;

import static org.mastodon.geff.imglib2.ElementType.NODE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.mastodon.geff.GeffAxis;
import org.mastodon.geff.GeffMetadata;
import org.mastodon.geff.imglib2.Construction.FromId;
import org.mastodon.geff.imglib2.Construction.FromProperty;
import org.mastodon.geff.imglib2.Construction.NodeConstructor;
import org.mastodon.geff.imglib2.ElementCreator;
import org.mastodon.geff.imglib2.GeffException;
import org.mastodon.geff.imglib2.GeffProperties;
import org.mastodon.geff.imglib2.IoUtils;
import org.mastodon.geff.imglib2.Maybe.MaybeDouble;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.io.geff.GeffTestBase;
import fiji.plugin.trackmate.io.geff.GeffTestDeserializationTest;
import fiji.plugin.trackmate.io.geff.TmGeffIODemo;
import fiji.plugin.trackmate.io.geff.imglib2.heuristics.AxisIdentifierHeuristic;
import fiji.plugin.trackmate.io.geff.imglib2.heuristics.RadiusIdentifierHeuristic;

public class ReadTrackMateSpotsDemo extends GeffTestBase
{

	public static void main( final String[] args ) throws Throwable
	{
		/*
		 * RESAVE TO GEFF
		 */

		final String xmlPath = GeffTestDeserializationTest.SPOT_MIXED_PATH;
		final String path = TmGeffIODemo.writeToGeff( xmlPath );

		/*
		 * METADATA
		 */

		final GeffMetadata metadata = GeffMetadata.readFromZarr( path );

		// Collect spatial axes names
		final List< String > names = new ArrayList<>( 3 );
		metadata.getGeffAxesList().stream()
				.filter( axis -> axis.getType().equalsIgnoreCase( GeffAxis.TYPE_SPACE ) )
				.forEach( axis -> names.add( axis.getName() ) );
		final Map< Character, String > spatialAxesMap = AxisIdentifierHeuristic.identify( names );

		if ( names.size() < 2 )
			throw new GeffException( "Expected at least 2 spatial axes, got " + names.size() );

		// Collect first temporal axis name
		final String temporalAxisName = metadata.getGeffAxesList().stream()
				.filter( axis -> axis.getType().equalsIgnoreCase( GeffAxis.TYPE_TIME ) )
				.findFirst()
				.map( axis -> axis.getName() )
				.orElseThrow( () -> new GeffException( "No temporal axis found in GEFF metadata." ) );

		final Model model = new Model();
		try (final N5Reader n5 = new N5ZarrReader( path ))
		{
			final GeffProperties props = IoUtils.loadProperties( n5, NODE );

			// Rename spatial axes properties
			spatialAxesMap.forEach( ( axis, name ) -> props.rename( name, "" + axis ) );

			// Rename temporal axis property
			props.rename( temporalAxisName, "t" );

			// Identify radius property
			final String radiusPropertyName = RadiusIdentifierHeuristic.identify( props.properties().keySet() );
			if ( radiusPropertyName == null )
				throw new GeffException( "Could not identify a property representing the radius." );
			// TODO: Actually support cases where we have cov, polygons, etc.
			props.rename( radiusPropertyName, "sphere" );

			final boolean is2D = !props.properties().containsKey( "z" );
			final boolean hasPolygons = props.properties().containsKey( "polygon" );

			final Object nodeBuilder;
			if ( is2D )
				if ( hasPolygons )
					nodeBuilder = new SpotRoiBuilder2D( model.getSpots() );
				else
					nodeBuilder = new SpotBaseBuilder2D( model.getSpots() );
			else
				// TODO: Implement 3D support, with and without meshes.
				throw new UnsupportedOperationException( "Only 2D spots are supported for now." );
			/*
			 * TODO: Implement support for other shape specifications, like the
			 * covariance.
			 */
			/*
			 * TODO: The color feature is in the GEFF specs (at least in the
			 * 'expected file structure' example), so we should handle it
			 * explicitly.
			 */

			buildNodes( nodeBuilder, props );
		}

		System.out.println( "Deserialized model: " + model.getSpots() );
		final Spot example = model.getSpots().iterable( true ).iterator().next();
		System.out.println( "Example spot"
				+ " - class = " + example.getClass().getName() + "\n"
				+ example.echo() );
	}

	public static class AbstractSpotBuilder
	{
		protected final SpotCollection spots;

		public AbstractSpotBuilder( final SpotCollection spots )
		{
			this.spots = spots;
		}

		protected void setUp(
				final Spot spot,
				final double x,
				final double y,
				final double z,
				final int t,
				final MaybeDouble r,
				final Optional< String > name )
		{
			spot.setPosition( x, 0 );
			spot.setPosition( y, 1 );
			spot.setPosition( z, 2 );
			if ( r.isPresent() )
				spot.putFeature( Spot.RADIUS, r.get() );
			spot.putFeature( Spot.FRAME, Double.valueOf( t ) );
			spot.setName( name.orElse( null ) );
		}
	}

	public static class SpotBaseBuilder2D extends AbstractSpotBuilder
	{

		public SpotBaseBuilder2D( final SpotCollection spots )
		{
			super( spots );
		}

		@NodeConstructor
		public void addVertex(
				@FromId( ) final long id,
				@FromProperty( "x" ) final double x,
				@FromProperty( "y" ) final double y,
				@FromProperty( "t" ) final int t,
				@FromProperty( "sphere" ) final MaybeDouble r,
				@FromProperty( "name" ) final Optional< String > name )
		{
			/*
			 * TODO: normally we want to use the TRACKMATE ID, not the GEFF id.
			 * But we still want to keep track of the mapping GEFF id -> Spot to
			 * reconstruct the graph later.
			 */
			/*
			 * TODO: check if we can include the VISIBILITY feature somewhere,
			 * in an elegant manner if it is missing.
			 */
			final SpotBase spot = new SpotBase( ( int ) id );
			setUp( spot, x, y, 0., t, r, name );
			spots.add( spot, t );
			// TODO: How to elegantly add the other properties as features?
		}
	}

	public static class SpotRoiBuilder2D extends AbstractSpotBuilder
	{

		public SpotRoiBuilder2D( final SpotCollection spots )
		{
			super( spots );
		}

		@NodeConstructor
		public void addVertex(
				@FromId( ) final long id,
				@FromProperty( "x" ) final double x,
				@FromProperty( "y" ) final double y,
				@FromProperty( "t" ) final int t,
				@FromProperty( "sphere" ) final MaybeDouble r,
				@FromProperty( "polygon" ) final Optional< double[] > polygon,
				@FromProperty( "name" ) final Optional< String > name )
		{
			final Spot spot;
			if ( !polygon.isPresent() )
			{
				spot = new SpotBase( ( int ) id );
			}
			else
			{
				/*
				 * TODO: Double check if the specs state the polygon coords are
				 * relative to the center or absolute.
				 */
				final double[] p = polygon.get();
				final int nPoints = p.length / 2;
				final double[] xp = new double[ nPoints ];
				final double[] yp = new double[ nPoints ];
				for ( int i = 0; i < nPoints; i++ )
				{
					xp[ i ] = p[ 2 * i ];
					yp[ i ] = p[ 2 * i + 1 ];
				}
				spot = new SpotRoi( ( int ) id, xp, yp );
			}
			setUp( spot, x, y, 0., t, r, name );
			spots.add( spot, t );
		}
	}

	public static void buildNodes( final Object target, final GeffProperties properties ) throws Throwable
	{
		final ElementCreator c = ElementCreator.of( target, properties );
		for ( int i = 0; i < properties.numElements(); i++ )
			c.createElement( i );
	}
}
