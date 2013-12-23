package fiji.plugin.trackmate.detection;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.localextrema.LocalExtrema;
import net.imglib2.algorithm.localextrema.LocalExtrema.LocalNeighborhoodCheck;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.algorithm.localextrema.SubpixelLocalization;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.util.MedianFilter;

public class DetectionUtils
{

	/**
	 * Creates a laplacian of gaussian (LoG) kernel tuned for blobs with a
	 * radius specified <b>using calibrated units</b>. The specified calibration
	 * is used to determine the dimensionality of the kernel and to map it on a
	 * pixel grid.
	 *
	 * @param radius
	 *            the blob radius (in image unit).
	 * @param nDims
	 *            the dimensionality of the desired kernel. Must be 2 or 3.
	 * @param calibration
	 *            the pixel sizes, specified as <code>double[]</code> array.
	 * @return a new image containing the LoG kernel.
	 */
	public static final Img< FloatType > createLoGKernel( final double radius, final int nDims, final double[] calibration )
	{
		// Optimal sigma for LoG approach and dimensionality.
		final double sigma = radius / Math.sqrt( nDims );
		final double[] sigmaPixels = new double[ nDims ];
		for ( int i = 0; i < sigmaPixels.length; i++ )
		{
			sigmaPixels[ i ] = sigma / calibration[ i ];
		}

		final int n = sigmaPixels.length;
		final long[] sizes = new long[ n ];
		final long[] middle = new long[ n ];
		for ( int d = 0; d < n; ++d )
		{
			// From Tobias Gauss3
			final int hksizes = Math.max( 2, ( int ) ( 3 * sigmaPixels[ d ] + 0.5 ) + 1 );
			sizes[ d ] = 3 + 2 * hksizes;
			middle[ d ] = 1 + hksizes;

		}
		final ArrayImg< FloatType, FloatArray > kernel = ArrayImgs.floats( sizes );

		final ArrayCursor< FloatType > c = kernel.cursor();
		final long[] coords = new long[ nDims ];

		/*
		 * The gaussian normalization factor, divided by a constant value. This
		 * is a fudge factor, that more or less put the quality values close to
		 * the maximal value of a blob of optimal radius.
		 */
		final double C = 1d / 20d * Math.pow( 1d / sigma / Math.sqrt( 2 * Math.PI ), nDims );

		// Work in image coordinates
		while ( c.hasNext() )
		{
			c.fwd();
			c.localize( coords );

			double mantissa = 0;
			double exponent = 0;
			for ( int d = 0; d < coords.length; d++ )
			{
				final double x = calibration[ d ] * ( coords[ d ] - middle[ d ] );
				mantissa += -C * ( x * x / sigma / sigma - 1d );
				exponent += -x * x / 2d / sigma / sigma;
			}
			c.get().setReal( mantissa * Math.exp( exponent ) );
		}

		return kernel;
	}

	/**
	 * Copy an interval of the specified source image on a float image.
	 *
	 * @param img
	 *            the source image.
	 * @param interval
	 *            the interval in the source image to copy.
	 * @param factory
	 *            a factory used to build the float image.
	 * @return a new float Img. Careful: even if the specified interval does not
	 *         start at (0, 0), the new image will have its first pixel at
	 *         coordinates (0, 0).
	 */
	public static final < T extends RealType< T >> Img< FloatType > copyToFloatImg( final RandomAccessible< T > img, final Interval interval, final ImgFactory< FloatType > factory )
	{
		final Img< FloatType > output = factory.create( interval, new FloatType() );
		final long[] min = new long[ interval.numDimensions() ];
		interval.min( min );
		final RandomAccess< T > in = Views.offset( img, min ).randomAccess();
		final Cursor< FloatType > out = output.cursor();
		final RealFloatConverter< T > c = new RealFloatConverter< T >();

		while ( out.hasNext() )
		{
			out.fwd();
			in.setPosition( out );
			c.convert( in.get(), out.get() );
		}

		return output;
	}

	/**
	 * Apply a simple 3x3 median filter to the target image.
	 */
	public static final < R extends RealType< R > & NativeType< R >> Img< R > applyMedianFilter( final RandomAccessibleInterval< R > image )
	{
		final MedianFilter< R > medFilt = new MedianFilter< R >( image, 1 );
		if ( !medFilt.checkInput() || !medFilt.process() )
		{
			return null;
		}
		return medFilt.getResult();
	}

	public static final List< Spot > findLocalMaxima( final RandomAccessibleInterval< FloatType > source, final double threshold, final double[] calibration, final double radius, final boolean doSubPixelLocalization, final int numThreads )
	{
		/*
		 * Find maxima.
		 */

		final FloatType val = new FloatType();
		val.setReal( threshold );
		final LocalNeighborhoodCheck< Point, FloatType > localNeighborhoodCheck = new LocalExtrema.MaximumCheck< FloatType >( val );
		final IntervalView< FloatType > dogWithBorder = Views.interval( Views.extendMirrorSingle(  source ), Intervals.expand( source, 1 ) );
		final ArrayList< Point > peaks = LocalExtrema.findLocalExtrema( dogWithBorder, localNeighborhoodCheck, numThreads );

		final ArrayList< Spot > spots;
		if ( doSubPixelLocalization )
		{

			/*
			 * Sub-pixel localize them.
			 */

			final SubpixelLocalization< Point, FloatType > spl = new SubpixelLocalization< Point, FloatType >( source.numDimensions() );
			spl.setNumThreads( numThreads );
			spl.setReturnInvalidPeaks( true );
			spl.setCanMoveOutside( true );
			spl.setAllowMaximaTolerance( true );
			spl.setMaxNumMoves( 10 );
			final ArrayList< RefinedPeak< Point >> refined = spl.process( peaks, dogWithBorder, source );

			spots = new ArrayList< Spot >( refined.size() );
			final RandomAccess< FloatType > ra = source.randomAccess();
			for ( final RefinedPeak< Point > refinedPeak : refined )
			{
				ra.setPosition( refinedPeak.getOriginalPeak() );
				final double quality = ra.get().getRealDouble();
				final Spot spot = new Spot( refinedPeak.getDoublePosition( 0 ) * calibration[ 0 ], refinedPeak.getDoublePosition( 1 ) * calibration[ 1 ], refinedPeak.getDoublePosition( 2 ) * calibration[ 2 ], radius, quality );
				spots.add( spot );
			}

		}
		else
		{
			spots = new ArrayList< Spot >( peaks.size() );
			final RandomAccess< FloatType > ra = source.randomAccess();
			for ( final Point peak : peaks )
			{
				ra.setPosition( peak );
				final double quality = ra.get().getRealDouble();
				final Spot spot = new Spot( peak.getDoublePosition( 0 ) * calibration[ 0 ], peak.getDoublePosition( 1 ) * calibration[ 1 ], peak.getDoublePosition( 2 ) * calibration[ 2 ], radius, quality );
				spots.add( spot );
			}

		}

		return spots;
	}
}
