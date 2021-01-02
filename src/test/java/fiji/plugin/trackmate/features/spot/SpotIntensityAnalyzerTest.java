package fiji.plugin.trackmate.features.spot;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.SpotNeighborhood;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class SpotIntensityAnalyzerTest
{

	private static final int TEST_VAL = 1000;

	private static final double RADIUS = 2; // physical units

	private static final double[] CENTER = new double[] { 10, 10, 20 };

	private static final double[] CALIBRATION = new double[] { 0.2, 0.2, 1 };

	private ImgPlus< UnsignedShortType > img2D;

	private Spot spot;

	/**
	 * Create a 2D image
	 */
	@Before
	public void setUp() throws Exception
	{
		final long[] dims = new long[] { ( long ) ( 2 * CENTER[ 0 ] / CALIBRATION[ 0 ] ), ( long ) ( 2 * CENTER[ 1 ] / CALIBRATION[ 1 ] ) };
		final Img< UnsignedShortType > img = ArrayImgs.unsignedShorts( dims );
		img2D = new ImgPlus< >( img, "2D", new AxisType[] { Axes.X, Axes.Y }, new double[] { CALIBRATION[ 0 ], CALIBRATION[ 1 ] } );

		// We paint MANUALLY a square in the middle of the image
		final RandomAccess< UnsignedShortType > ra = img.randomAccess();
		for ( int j = ( int ) ( ( CENTER[ 1 ] - RADIUS ) / CALIBRATION[ 1 ] ); j < ( CENTER[ 1 ] + RADIUS ) / CALIBRATION[ 1 ] + 1; j++ )
		{
			ra.setPosition( j, 1 );
			for ( int i = ( int ) ( ( CENTER[ 0 ] - RADIUS ) / CALIBRATION[ 0 ] ); i < ( CENTER[ 0 ] + RADIUS ) / CALIBRATION[ 0 ] + 1; i++ )
			{
				ra.setPosition( i, 0 );
				ra.get().set( TEST_VAL );

			}

		}

		spot = new Spot( CENTER[ 0 ], CENTER[ 1 ], CENTER[ 2 ], RADIUS, -1d, "1" );
	}

	@Test
	public void testProcessSpot2D()
	{
		final SpotIntensityMultiCAnalyzer< UnsignedShortType > analyzer = new SpotIntensityMultiCAnalyzer<>( img2D, 0 );
		analyzer.process( spot );

		assertEquals( TEST_VAL, spot.getFeature( SpotIntensityMultiCAnalyzerFactory.MEAN_INTENSITY + '1' ).doubleValue(), 1e-10 );
		assertEquals( TEST_VAL, spot.getFeature( SpotIntensityMultiCAnalyzerFactory.MAX_INTENSITY + '1' ).doubleValue(), 1e-10 );
		assertEquals( TEST_VAL, spot.getFeature( SpotIntensityMultiCAnalyzerFactory.MIN_INTENSITY + '1' ).doubleValue(), 1e-10 );
	}

	/**
	 * Interactive test.
	 */
	public static void main( final String[] args ) throws Exception
	{
		final SpotIntensityAnalyzerTest test = new SpotIntensityAnalyzerTest();
		test.setUp();

		final Spot tmpSpot = new Spot( CENTER[ 0 ], CENTER[ 1 ], CENTER[ 2 ], RADIUS, -1d );
		final SpotNeighborhood< UnsignedShortType > disc = new SpotNeighborhood< >( tmpSpot, test.img2D );
		for ( final UnsignedShortType pixel : disc )
		{
			pixel.set( 1500 );
		}

		ij.ImageJ.main( args );
		net.imglib2.img.display.imagej.ImageJFunctions.show( test.img2D );

	}

}
