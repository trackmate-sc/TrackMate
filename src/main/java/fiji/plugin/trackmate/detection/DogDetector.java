package fiji.plugin.trackmate.detection;

import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.dog.DifferenceOfGaussian;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class DogDetector< T extends RealType< T > & NativeType< T >> extends LogDetector< T >
{

	/*
	 * CONSTANTS
	 */

	public final static String BASE_ERROR_MESSAGE = "DogDetector: ";

	/*
	 * CONSTRUCTOR
	 */

	public DogDetector( final RandomAccessible< T > img, final Interval interval, final double[] calibration, final double radius, final double threshold, final boolean doSubPixelLocalization, final boolean doMedianFilter )
	{
		super( img, interval, calibration, radius, threshold, doSubPixelLocalization, doMedianFilter );
		this.baseErrorMessage = BASE_ERROR_MESSAGE;
	}

	/*
	 * METHODS
	 */

	@Override
	public boolean process()
	{

		final long start = System.currentTimeMillis();

		RandomAccessibleInterval< T > view = Views.interval( img, interval );

		/*
		 * Do median filtering (or not).
		 */

		if ( doMedianFilter )
		{
			view = DetectionUtils.applyMedianFilter( view );
			if ( null == view )
			{
				errorMessage = BASE_ERROR_MESSAGE + "Failed to apply median filter.";
				return false;
			}
		}

		/*
		 * Do DoG computation.
		 */

		final RandomAccessible< T > extended = Views.extendMirrorSingle( view );
		// We need to shift coordinates by -min[] to have the correct location.
		final long[] min = new long[ interval.numDimensions() ];
		interval.min( min );
		for ( int d = 0; d < min.length; d++ )
		{
			min[ d ] = -min[ d ];
		}

		final FloatType type = new FloatType();
		final RandomAccessibleInterval< FloatType > dog = Views.offset( Util.getArrayOrCellImgFactory( interval, type ).create( interval ), min );
		final RandomAccessibleInterval< FloatType > dog2 = Views.offset( Util.getArrayOrCellImgFactory( interval, type ).create( interval ), min );

		final double sigma1 = radius / Math.sqrt( interval.numDimensions() ) * 0.9;
		final double sigma2 = radius / Math.sqrt( interval.numDimensions() ) * 1.1;

		/*
		 * Gotcha: The calibration array used as input for
		 * DifferenceOfGaussian#computeSigmas() must be of the same dimension
		 * that the input image.
		 */
		final double[] cal = new double[ img.numDimensions() ];
		for ( int d = 0; d < cal.length; d++ )
			cal[ d ] = calibration[ d ];
		final double[][] sigmas = DifferenceOfGaussian.computeSigmas( 0.5, 2, cal, sigma1, sigma2 );
		try
		{
			Gauss3.gauss( sigmas[ 1 ], extended, dog2, numThreads );
			Gauss3.gauss( sigmas[ 0 ], extended, dog, numThreads );
		}
		catch ( final IncompatibleTypeException e )
		{
			e.printStackTrace();
		}

		final IterableInterval< FloatType > dogIterable = Views.iterable( dog );
		final IterableInterval< FloatType > tmpIterable = Views.iterable( dog2 );
		final Cursor< FloatType > dogCursor = dogIterable.cursor();
		final Cursor< FloatType > tmpCursor = tmpIterable.cursor();
		while ( dogCursor.hasNext() )
			dogCursor.next().sub( tmpCursor.next() );

		spots = DetectionUtils.findLocalMaxima( dog, threshold, calibration, radius, doSubPixelLocalization, numThreads );

		final long end = System.currentTimeMillis();
		processingTime = end - start;

		return true;
	}
}
