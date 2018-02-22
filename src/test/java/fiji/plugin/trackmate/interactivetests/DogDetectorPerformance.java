package fiji.plugin.trackmate.interactivetests;


import java.util.Random;

import net.imagej.ImgPlus;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import fiji.plugin.trackmate.detection.DogDetector;
import fiji.plugin.trackmate.util.TMUtils;

public class DogDetectorPerformance {

	public static void main(final String[] args) throws IncompatibleTypeException {
		performance3D();
	}

	protected static void image2DSizePerformance() throws IncompatibleTypeException
	{

		final int nwarmups = 5;
		final int ntests = 20;
		final int nspots = 200;
		final double rad = 3d;

		final int[] sizes = new int[] { 16, 32, 64, 128, 256, 512, 1024, 1536, 2048 };

		System.out.println( "2D performance: finding 200 spots in a uint16 image:" );
		System.out.println( "N(pixels)\tTime(ms)" );
		for ( final int size : sizes )
		{
			final ArrayImg< UnsignedShortType, ShortArray > img = ArrayImgs.unsignedShorts( size, size );
			final ArrayRandomAccess< UnsignedShortType > ra = img.randomAccess();
			final Random ran = new Random();
			for ( int i = 0; i < nspots; i++ )
			{
				ra.setPosition( ran.nextInt( ( int ) img.dimension( 0 ) ), 0 );
				ra.setPosition( ran.nextInt( ( int ) img.dimension( 1 ) ), 1 );
				ra.get().set( 5000 );
			}

			final RandomAccessible< UnsignedShortType > source = Views.extendMirrorSingle( img );
			Gauss3.gauss( rad / Math.sqrt( 2 ), source, img );
			final ImgPlus< UnsignedShortType > imgplus = new ImgPlus< >( img );

			{
				for ( int i = 0; i < nwarmups; i++ )
				{
					execTest( imgplus, rad );
				}
				final long start = System.currentTimeMillis();
				for ( int i = 0; i < ntests; i++ )
				{
					execTest( imgplus, rad );
				}
				final long end = System.currentTimeMillis();
				System.out.println( img.size() + "\t" + ( ( double ) ( end - start ) / ntests ) );
			}
		}
	}

	protected static void image3DSizePerformance() throws IncompatibleTypeException
	{

		final int nwarmups = 2;
		final int ntests = 10;
		final int nspots = 200;
		final double rad = 3d;

		final int[] sizes = new int[] { 16, 32, 64, 128, 256, 512 };

		System.out.println( "3D performance: finding 200 spots in a 3D uint16 image:" );
		System.out.println( "! align=\"left\"|N (pixels)" );
		System.out.println( "!Image size" );
		System.out.println( "!Time (ms)" );
		for ( final int size : sizes )
		{
			final ArrayImg< UnsignedShortType, ShortArray > img = ArrayImgs.unsignedShorts( size, size, size );
			final ArrayRandomAccess< UnsignedShortType > ra = img.randomAccess();
			final Random ran = new Random();
			for ( int i = 0; i < nspots; i++ )
			{
				for ( int d = 0; d < img.numDimensions(); d++ )
				{
					ra.setPosition( ran.nextInt( ( int ) img.dimension( d ) ), d );
				}
				ra.get().set( 5000 );
			}

			final RandomAccessible< UnsignedShortType > source = Views.extendMirrorSingle( img );
			Gauss3.gauss( rad / Math.sqrt( 2 ), source, img );
			final ImgPlus< UnsignedShortType > imgplus = new ImgPlus< >( img );

			{
				for ( int i = 0; i < nwarmups; i++ )
				{
					execTest( imgplus, rad );
				}
				final long start = System.currentTimeMillis();
				for ( int i = 0; i < ntests; i++ )
				{
					execTest( imgplus, rad );
				}
				final long end = System.currentTimeMillis();

				System.out.println( "|-" );
				System.out.println( "|" + img.size() );
				System.out.println( "|" + size + "x" + size + "x" + size );
				System.out.println( "|" + ( ( double ) ( end - start ) / ntests ) );
			}
		}
		System.out.println( "|}" );
	}

	protected static void performance2D() throws IncompatibleTypeException
	{

		final int nwarmups = 5;
		final int ntests = 20;
		final int nspots = 200;

		final double[] radiuses = new double[] { 1.9953, 2.3012, 2.6540, 3.0610, 3.5303, 4.0716, 4.6959, 5.4160, 6.2464, 7.2042, 8.3088, 9.5828, 11.0521, 12.7467, 14.7012, 16.9553, 19.5551, 22.5535, 26.0116, 30.0000 };
		final int width = 1024;
		final int height = width;

		System.out.println( "2D performance: finding 200 spots in a " + width + "x" + height + " uint16 image:" );
		System.out.println( "Radius\tTime(ms)" );
		for ( final double rad : radiuses )
		{
			final ArrayImg< UnsignedShortType, ShortArray > img = ArrayImgs.unsignedShorts( 1024, 1024 );
			final ArrayRandomAccess< UnsignedShortType > ra = img.randomAccess();
			final Random ran = new Random();
			for ( int i = 0; i < nspots; i++ )
			{
				ra.setPosition( ran.nextInt( ( int ) img.dimension( 0 ) ), 0 );
				ra.setPosition( ran.nextInt( ( int ) img.dimension( 1 ) ), 1 );
				ra.get().set( 5000 );
			}

			final RandomAccessible< UnsignedShortType > source = Views.extendMirrorSingle( img );
			Gauss3.gauss( rad / Math.sqrt( 2 ), source, img );
			final ImgPlus< UnsignedShortType > imgplus = new ImgPlus< >( img );

			{
				for ( int i = 0; i < nwarmups; i++ )
				{
					execTest( imgplus, rad );
				}
				final long start = System.currentTimeMillis();
				for ( int i = 0; i < ntests; i++ )
				{
					execTest( imgplus, rad );
				}
				final long end = System.currentTimeMillis();
				System.out.println( rad + "\t" + ( ( double ) ( end - start ) / ntests ) );
			}
		}
	}

	protected static void performance3D() throws IncompatibleTypeException
	{

		final int nwarmups = 1;
		final int ntests = 3;
		final int nspots = 200;

		final double[] radiuses = new double[] { 1.9953, 2.3012, 2.6540, 3.0610, 3.5303, 4.0716, 4.6959, 5.4160, 6.2464, 7.2042, 8.3088, 9.5828, 11.0521, 12.7467, 14.7012, 16.9553, 19.5551, 22.5535, 26.0116, 30.0000 };
		final int width = 256;
		final int height = width;
		final int depth = width;

		System.out.println( "3D performance: finding 200 spots in a " + width + "x" + height + "x" + height + " uint16 image:" );
		System.out.println( "Radius\tTime(ms)" );
		for ( final double rad : radiuses )
		{
			final ArrayImg< UnsignedShortType, ShortArray > img = ArrayImgs.unsignedShorts( width, height, depth );
			final ArrayRandomAccess< UnsignedShortType > ra = img.randomAccess();
			final Random ran = new Random();
			for ( int i = 0; i < nspots; i++ )
			{
				for ( int d = 0; d < img.numDimensions(); d++ )
				{
					ra.setPosition( ran.nextInt( ( int ) img.dimension( d ) ), d );
				}
				ra.get().set( 5000 );
			}

			final RandomAccessible< UnsignedShortType > source = Views.extendMirrorSingle( img );
			Gauss3.gauss( rad / Math.sqrt( img.numDimensions() ), source, img );
			final ImgPlus< UnsignedShortType > imgplus = new ImgPlus< >( img );

			{
				for ( int i = 0; i < nwarmups; i++ )
				{
					execTest( imgplus, rad );
				}
				final long start = System.currentTimeMillis();
				for ( int i = 0; i < ntests; i++ )
				{
					execTest( imgplus, rad );
				}
				final long end = System.currentTimeMillis();
				System.out.println( rad + "\t" + ( ( double ) ( end - start ) / ntests ) );
			}
		}
	}

	private static final void execTest(final ImgPlus<UnsignedShortType> imgplus, final double rad) {
		final DogDetector< UnsignedShortType > detector = new DogDetector< >( imgplus, imgplus, TMUtils.getSpatialCalibration( imgplus ), rad, 1, false, false );
		detector.setNumThreads(1);
		if (!detector.checkInput() || !detector.process()) {
			System.out.println(detector.getErrorMessage());
			return;
		}
		detector.getResult();
	}

}
