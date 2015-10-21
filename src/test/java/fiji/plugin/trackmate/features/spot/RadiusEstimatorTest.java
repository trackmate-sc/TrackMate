package fiji.plugin.trackmate.features.spot;

import static org.junit.Assert.assertEquals;
import ij.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.junit.Test;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.SpotNeighborhood;

public class RadiusEstimatorTest< T extends NativeType< T > & RealType< T >>
{

	/** We want to retrieve real radius with a tolerance of: */
	private static final double TOLERANCE = 0.05;

	/**
	 * We test that this estimator can retrieve the actual radius of perfect
	 * sphere with an accuracy at least 5%.
	 */
	@Test
	public void testEstimatorOnPerfectSpheres()
	{

		final double[] radiuses = new double[] { 12, 20, 32 };
		final byte on = ( byte ) 255;
		final Spot s1 = new Spot( 100d, 100d, 100d, radiuses[ 0 ], -1d );
		final Spot s2 = new Spot( 100d, 100d, 200d, radiuses[ 1 ], -1d );
		final Spot s3 = new Spot( 100d, 100d, 300d, radiuses[ 2 ], -1d );
		final Spot[] spots = new Spot[] { s1, s2, s3 };
		final double[] calibration = new double[] { 1, 1, 1 };
		final AxisType[] axes = new AxisType[] { Axes.X, Axes.Y, Axes.Z };

		// Create 3 spots image
		final Img< UnsignedByteType > img = new ArrayImgFactory< UnsignedByteType >().create( new int[] { 200, 200, 400 }, new UnsignedByteType() );
		final ImgPlus< UnsignedByteType > testImage = new ImgPlus< UnsignedByteType >( img, "Test", axes, calibration );

		for ( final Spot s : spots )
		{
			final SpotNeighborhood< UnsignedByteType > sphere = new SpotNeighborhood< UnsignedByteType >( s, testImage );
			for ( final UnsignedByteType pixel : sphere )
			{
				pixel.set( on );
			}
		}

		// Apply the estimator
		final SpotRadiusEstimator< UnsignedByteType > es = new SpotRadiusEstimator< UnsignedByteType >( testImage, null );

		Spot s;
		double r;
		for ( int i = 0; i < spots.length; i++ )
		{
			s = spots[ i ];
			r = radiuses[ i ];
			es.process( s );
			assertEquals( 2 * r, s.getFeatures().get( SpotRadiusEstimatorFactory.ESTIMATED_DIAMETER ), 2 * r * TOLERANCE );
		}

	}

	public void exampleEstimation()
	{

		final byte on = ( byte ) 255;
		final double[] radiuses = new double[] { 12, 20, 32 };
		final Spot s1 = new Spot( 100, 100, 100, radiuses[ 0 ], -1d );
		final Spot s2 = new Spot( 100, 100, 200, radiuses[ 1 ], -1d );
		final Spot s3 = new Spot( 100, 100, 300, radiuses[ 2 ], -1d );
		final Spot[] spots = new Spot[] { s1, s2, s3 };
		final double[] calibration = new double[] { 1, 1, 1 };
		final AxisType[] axes = new AxisType[] { Axes.X, Axes.Y, Axes.Z };

		// Create 3 spots image
		final Img< UnsignedByteType > img = new ArrayImgFactory< UnsignedByteType >().create( new int[] { 200, 200, 400 }, new UnsignedByteType() );
		final ImgPlus< UnsignedByteType > testImage = new ImgPlus< UnsignedByteType >( img, "Test", axes, calibration );

		for ( final Spot s : spots )
		{
			final SpotNeighborhood< UnsignedByteType > sphere = new SpotNeighborhood< UnsignedByteType >( s, testImage );
			for ( final UnsignedByteType pixel : sphere )
			{
				pixel.set( on );
			}
		}

		final ij.ImagePlus imp = ImageJFunctions.wrap( testImage, testImage.toString() );
		imp.show();

		// Apply the estimator
		final SpotRadiusEstimator< UnsignedByteType > es = new SpotRadiusEstimator< UnsignedByteType >( testImage, null );

		Spot s;
		double r;
		long start, stop;
		for ( int i = 0; i < spots.length; i++ )
		{
			s = spots[ i ];
			r = radiuses[ i ];
			start = System.currentTimeMillis();
			es.process( s );
			stop = System.currentTimeMillis();
			System.out.println( String.format( "For spot %d, found diameter %.1f, real value was %.1f.", i, s.getFeatures().get( SpotRadiusEstimatorFactory.ESTIMATED_DIAMETER ), 2 * r ) );
			System.out.println( "Computing time: " + ( stop - start ) + " ms." );
		}
	}

	public static < T extends NativeType< T > & RealType< T >> void main( final String[] args )
	{
		ImageJ.main( args );
		new RadiusEstimatorTest< T >().exampleEstimation();
	}
}
