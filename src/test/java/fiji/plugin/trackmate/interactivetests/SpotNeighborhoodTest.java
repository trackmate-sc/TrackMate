package fiji.plugin.trackmate.interactivetests;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.SpotNeighborhood;
import fiji.plugin.trackmate.util.SpotNeighborhoodCursor;
import ij.ImageJ;
import net.imagej.ImgPlus;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class SpotNeighborhoodTest
{

	public static void main( final String[] args )
	{
		ImageJ.main( args );

		// 3D
		final ArrayImg< UnsignedShortType, ShortArray > image = ArrayImgs.unsignedShorts( 100, 100, 100 );
		final ImgPlus< UnsignedShortType > img = new ImgPlus< >( image );
		final Spot spot = new Spot( 50d, 50d, 50d, 30d, -1d );
		final SpotNeighborhood< UnsignedShortType > neighborhood = new SpotNeighborhood< >( spot, img );
		final SpotNeighborhoodCursor< UnsignedShortType > cursor = neighborhood.cursor();
		while ( cursor.hasNext() )
		{
			cursor.next().set( ( int ) cursor.getDistanceSquared() );
		}
		System.out.println( "Finished" );
		ImageJFunctions.wrap( img, "3D" ).show();

		// 2D
		final ArrayImg< UnsignedShortType, ShortArray > image2 = ArrayImgs.unsignedShorts( 100, 100 );
		final ImgPlus< UnsignedShortType > img2 = new ImgPlus< >( image2 );
		final Spot spot2 = new Spot( 50d, 50d, 0d, 30d, -1d );
		final SpotNeighborhood< UnsignedShortType > neighborhood2 = new SpotNeighborhood< >( spot2, img2 );
		final SpotNeighborhoodCursor< UnsignedShortType > cursor2 = neighborhood2.cursor();
		while ( cursor2.hasNext() )
		{
			cursor2.next().set( ( int ) cursor2.getDistanceSquared() );
		}
		System.out.println( "Finished" );
		ImageJFunctions.wrap( img2, "3D" ).show();
	}
}
