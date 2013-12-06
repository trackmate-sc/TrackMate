package fiji.plugin.trackmate.detection;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
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
import net.imglib2.img.array.ArrayRandomAccess;
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
	public static final < R extends RealType< R >> Img< FloatType > createLoGKernel( final double radius, final int nDims, final double[] calibration )
	{
		// optimal sigma for LoG approach and dimensionality
		final double sigma = radius / Math.sqrt( nDims );
		// Turn it in pixel coordinates
		final double[] sigmas = new double[ nDims ];
		for ( int i = 0; i < sigmas.length; i++ )
		{
			sigmas[ i ] = sigma / calibration[ i ];
		}

		final int[] hksizes = Gauss3.halfkernelsizes( sigmas );
		final long[] sizes = new long[ hksizes.length ];
		final long[] middle = new long[ hksizes.length ];
		for ( int d = 0; d < sizes.length; d++ )
		{
			sizes[ d ] = 3 + 2 * hksizes[ d ];
			middle[ d ] = 1 + hksizes[ d ];
		}
		final ArrayImg< FloatType, FloatArray > kernel = ArrayImgs.floats( sizes );

		final ArrayCursor< FloatType > c = kernel.cursor();
		final long[] coords = new long[ nDims ];

		while ( c.hasNext() )
		{
			c.fwd();
			c.localize( coords );

			double mantissa = 0;
			double exponent = 0;
			for ( int d = 0; d < coords.length; d++ )
			{
				final double x = ( coords[ d ] - middle[ d ] );
				mantissa += x * x / sigmas[ d ] / sigmas[ d ] / sigmas[ d ] / sigmas[ d ] - 1d / sigmas[ d ] / sigmas[ d ];
				exponent += -x * x / 2d / sigmas[ d ] / sigmas[ d ];
			}
			c.get().setReal( -mantissa * Math.exp( exponent ) );
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
		final IntervalView< FloatType > dogWithBorder = Views.interval( Views.extendZero( source ), Intervals.expand( source, 1 ) );
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
				final double[] coords = new double[ 3 ];
				for ( int i = 0; i < source.numDimensions(); i++ )
					coords[ i ] = refinedPeak.getDoublePosition( i ) * calibration[ i ];
				final Spot spot = new Spot( coords );
				spot.putFeature( Spot.QUALITY, Double.valueOf( quality ) );
				spot.putFeature( Spot.RADIUS, Double.valueOf( radius ) );
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
				final double[] coords = new double[ 3 ];
				for ( int i = 0; i < source.numDimensions(); i++ )
					coords[ i ] = peak.getDoublePosition( i ) * calibration[ i ];
				final Spot spot = new Spot( coords );
				spot.putFeature( Spot.QUALITY, Double.valueOf( quality ) );
				spot.putFeature( Spot.RADIUS, Double.valueOf( radius ) );
				spots.add( spot );
			}

		}

		return spots;
	}

	private static final void writeLaplacianKernel( final ArrayImg< FloatType, FloatArray > kernel )
	{
		final int numDim = kernel.numDimensions();
		final long midx = kernel.dimension( 0 ) / 2;
		final long midy = kernel.dimension( 1 ) / 2;
		final ArrayRandomAccess< FloatType > ra = kernel.randomAccess();
		if ( numDim == 3 )
		{
			final float laplacianArray[][][] = new float[][][] { { { 0f, -3f / 96f, 0f }, { -3f / 96f, -10f / 96f, -3f / 96f }, { 0f, -3f / 96f, 0f }, }, { { -3f / 96f, -10f / 96f, -3f / 96f }, { -10f / 96f, 1f, -10f / 96f }, { -3f / 96f, -10f / 96f, -3f / 96f } }, { { 0f, -3f / 96f, 0f }, { -3f / 96f, -10f / 96f, -3f / 96f }, { 0f, -3f / 96f, 0f }, } };
			final long midz = kernel.dimension( 2 ) / 2;
			for ( int z = 0; z < 3; z++ )
			{
				ra.setPosition( midz + z - 1, 2 );
				for ( int y = 0; y < 3; y++ )
				{
					ra.setPosition( midy + y - 1, 1 );
					for ( int x = 0; x < 3; x++ )
					{
						ra.setPosition( midx + x - 1, 0 );
						ra.get().set( laplacianArray[ x ][ y ][ z ] );
					}
				}
			}

		}
		else if ( numDim == 2 )
		{
			final float laplacianArray[][] = new float[][] { { -0.05f, -0.2f, -0.05f }, { -0.2f, 1f, -0.2f }, { -0.05f, -0.2f, -0.05f } };
			for ( int y = 0; y < 3; y++ )
			{
				ra.setPosition( midy + y - 1, 1 );
				for ( int x = 0; x < 3; x++ )
				{
					ra.setPosition( midx + x - 1, 0 );
					ra.get().set( laplacianArray[ x ][ y ] );
				}
			}
		}
	}
}
