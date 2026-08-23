package fiji.plugin.trackmate.io.geff.imglib2;

import static org.mastodon.geff.imglib2.ElementType.NODE;

import java.util.Optional;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.mastodon.geff.imglib2.Construction.FromId;
import org.mastodon.geff.imglib2.Construction.FromProperty;
import org.mastodon.geff.imglib2.Construction.NodeConstructor;
import org.mastodon.geff.imglib2.ElementCreator;
import org.mastodon.geff.imglib2.GeffProperties;
import org.mastodon.geff.imglib2.IoUtils;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.io.geff.GeffTestBase;

public class ReadTrackMateSpotsDemo extends GeffTestBase
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

	public static class SpotBaseBuilder2D
	{

		private final SpotCollection spots;

		public SpotBaseBuilder2D( final SpotCollection spots )
		{
			this.spots = spots;
		}

		@NodeConstructor
		public void addVertex(
				@FromId( ) final long id,
				@FromProperty( "POSITION_X" ) final double x,
				@FromProperty( "POSITION_Y" ) final double y,
				@FromProperty( "FRAME" ) final long t,
				@FromProperty( "radius" ) final double r,
				@FromProperty( "name" ) final Optional< String > name )
		{
			final SpotBase spot = new SpotBase( ( int ) id );
			spot.setPosition( x, 0 );
			spot.setPosition( y, 1 );
			spot.setPosition( 0., 2 );
			spot.putFeature( Spot.RADIUS, r );
			spot.putFeature( Spot.FRAME, Double.valueOf( t ) );
			spot.setName( name.orElse( null ) );

			// Add to collection
			spots.add( spot, ( int ) t );
		}
	}

	public static void buildNodes( final Object target, final GeffProperties properties ) throws Throwable
	{
		final ElementCreator c = ElementCreator.of( target, properties );
		for ( int i = 0; i < properties.numElements(); i++ )
			c.createElement( i );
	}
}
