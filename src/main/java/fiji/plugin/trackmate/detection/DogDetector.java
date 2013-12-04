package fiji.plugin.trackmate.detection;

import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.dog.DifferenceOfGaussian;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.localextrema.LocalExtrema;
import net.imglib2.algorithm.localextrema.LocalExtrema.LocalNeighborhoodCheck;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.algorithm.localextrema.SubpixelLocalization;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.TMUtils;

public class DogDetector< T extends RealType< T > & NativeType< T >> extends LogDetector< T >
{

	/*
	 * CONSTANTS
	 */

	public final static String BASE_ERROR_MESSAGE = "DogDetector: ";

	/*
	 * CONSTRUCTOR
	 */

	public DogDetector( final ImgPlus< T > img, final double radius, final double threshold, final boolean doSubPixelLocalization, final boolean doMedianFilter )
	{
		super( img, radius, threshold, doSubPixelLocalization, doMedianFilter );
		this.baseErrorMessage = BASE_ERROR_MESSAGE;
	}

	/*
	 * METHODS
	 */

	@Override
	public boolean process()
	{

		final long start = System.currentTimeMillis();

		/*
		 * Do median filtering (or not).
		 */

		Img< T > frame = img;
		if ( doMedianFilter )
		{
			frame = applyMedianFilter( frame );
			if ( null == frame ) { return false; }
		}

		/*
		 * Do DoG computation.
		 */

		final double[] calibration = TMUtils.getSpatialCalibration( img );

		final FloatType type = new FloatType();
		final RandomAccessibleInterval< FloatType > dog = Util.getArrayOrCellImgFactory( frame, type ).create( frame, type );
		final RandomAccessibleInterval< FloatType > dog2 = Util.getArrayOrCellImgFactory( frame, type ).create( frame, type );

		final double sigma1 = radius / Math.sqrt( frame.numDimensions() ) * 0.9;
		final double sigma2 = radius / Math.sqrt( frame.numDimensions() ) * 1.1;
		final double[][] sigmas = DifferenceOfGaussian.computeSigmas( 0.5, 2, calibration, sigma1, sigma2 );
		try
		{
			Gauss3.gauss( sigmas[ 1 ], Views.extendMirrorSingle( frame ), dog2, numThreads );
			Gauss3.gauss( sigmas[ 0 ], Views.extendMirrorSingle( frame ), dog, numThreads );
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

		/*
		 * Find DoG maxima.
		 */

		final FloatType val = new FloatType();
		val.setReal( threshold * ( sigma2 / sigma1 - 1.0 ) );
		final IntervalView< FloatType > dogWithBorder = Views.interval( Views.extendZero( dog ), Intervals.expand( dog, 1 ) );
		final LocalNeighborhoodCheck< Point, FloatType > localNeighborhoodCheck = new LocalExtrema.MaximumCheck< FloatType >( val );
		final ArrayList< Point > peaks = LocalExtrema.findLocalExtrema( dogWithBorder, localNeighborhoodCheck, numThreads );

		if ( doSubPixelLocalization )
		{

			/*
			 * Sub-pixel localize them.
			 */

			final SubpixelLocalization< Point, FloatType > spl = new SubpixelLocalization< Point, FloatType >( dog.numDimensions() );
			spl.setNumThreads( numThreads );
			spl.setReturnInvalidPeaks( true );
			spl.setCanMoveOutside( true );
			spl.setAllowMaximaTolerance( true );
			spl.setMaxNumMoves( 10 );
			final ArrayList< RefinedPeak< Point >> refined = spl.process( peaks, dogWithBorder, dog );

			spots = new ArrayList< Spot >( refined.size() );
			final RandomAccess< FloatType > ra = dog.randomAccess();
			for ( final RefinedPeak< Point > refinedPeak : refined )
			{
				ra.setPosition( refinedPeak.getOriginalPeak() );
				final double quality = ra.get().getRealDouble();
				if ( quality < threshold )
					continue;

				final double[] coords = new double[ 3 ];
				for ( int i = 0; i < img.numDimensions(); i++ )
					coords[ i ] = refinedPeak.getDoublePosition( i ) * calibration[ i ];
				final Spot spot = new Spot( coords );
				spot.putFeature( Spot.QUALITY, Double.valueOf( quality ) );
				spot.putFeature( Spot.RADIUS, radius );
				spots.add( spot );
			}

		}
		else
		{
			spots = new ArrayList< Spot >( peaks.size() );
			final RandomAccess< FloatType > ra = dog.randomAccess();
			for ( final Point peak : peaks )
			{
				ra.setPosition( peak );
				final double quality = ra.get().getRealDouble();
				if ( quality < threshold )
					continue;

				final double[] coords = new double[ 3 ];
				for ( int i = 0; i < img.numDimensions(); i++ )
					coords[ i ] = peak.getDoublePosition( i ) * calibration[ i ];
				final Spot spot = new Spot( coords );
				spot.putFeature( Spot.QUALITY, Double.valueOf( quality ) );
				spot.putFeature( Spot.RADIUS, radius );
				spots.add( spot );
			}

		}

		final long end = System.currentTimeMillis();
		processingTime = end - start;

		return true;
	}
}
