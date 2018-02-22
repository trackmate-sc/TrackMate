package fiji.plugin.trackmate.interactivetests;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.threedviewer.SpotGroupNode;
import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.Image3DUniverse;

import java.awt.Color;
import java.util.HashMap;
import java.util.Random;
import java.util.TreeMap;

import org.scijava.vecmath.Color4f;
import org.scijava.vecmath.Point4d;

public class SpotGroupNodeTestDrive
{

	/*
	 * STATIC METHODS
	 */

	public static void main( final String args[] ) throws InterruptedException
	{
		final int N_BLOBS = 100;
		final int WIDTH = 200;
		final int HEIGHT = 200;
		final int DEPTH = 50;
		final int RADIUS = 10;

		final Random ran = new Random();
		final HashMap< Spot, Point4d > centers = new HashMap< >( N_BLOBS );
		final HashMap< Spot, Color4f > colors = new HashMap< >( N_BLOBS );
		Point4d center;
		Color4f color;
		Spot spot;
		for ( int i = 0; i < N_BLOBS; i++ )
		{
			final double x = WIDTH * ran.nextDouble();
			final double y = HEIGHT * ran.nextDouble();
			final double z = DEPTH * ran.nextDouble();

			center = new Point4d( x, y, z, RADIUS + ran.nextGaussian() );
			color = new Color4f( new Color( Color.HSBtoRGB( ran.nextFloat(), 1, 1 ) ) );
			color.w = ran.nextFloat();
			spot = new Spot( x, y, z, 1d, -1d );
			centers.put( spot, center );
			colors.put( spot, color );
		}

		final SpotGroupNode< Spot > sg = new SpotGroupNode< >( centers, colors );
		// sg.setName("spots");
		final ContentInstant ci = new ContentInstant( "t0" );
		ci.display( sg );
		final TreeMap< Integer, ContentInstant > instants = new TreeMap< >();
		instants.put( 0, ci );
		final Content c = new Content( "instants", instants );

		ij.ImageJ.main( args );
		final Image3DUniverse universe = new Image3DUniverse();
		universe.show();
		universe.addContentLater( c );

		for ( final Spot key : centers.keySet() )
		{
			sg.setVisible( key, false );
			Thread.sleep( 2000 / N_BLOBS );
		}

		for ( final Spot key : centers.keySet() )
		{
			sg.setVisible( key, true );
			Thread.sleep( 2000 / N_BLOBS );
		}

		final Spot thisSpot = centers.keySet().iterator().next();

		for ( int i = 1; i < WIDTH; i++ )
		{
			sg.setRadius( thisSpot, i );
			Thread.sleep( 2000 / WIDTH );
		}

		final Point4d p = centers.get( thisSpot );
		for ( int i = 0; i < WIDTH; i++ )
		{
			p.x = i;
			p.y = i;
			sg.setCenter( thisSpot, p );
			Thread.sleep( 2000 / WIDTH );
		}

		for ( int i = 1; i <= 100; i++ )
		{
			sg.setTransparency( thisSpot, ( float ) i / 100 );
			Thread.sleep( 2000 / 100 );
		}

		final Color4f col = colors.get( thisSpot );
		for ( int i = 100; i >= 1; i-- )
		{
			col.w = ( float ) i / 100;
			col.x = ( float ) i / 100;
			sg.setColor( thisSpot, col );
			Thread.sleep( 2000 / 100 );
		}
	}

}
