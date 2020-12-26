package fiji.plugin.trackmate.detection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.util.MedianFilter2D;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.localextrema.LocalExtrema;
import net.imglib2.algorithm.localextrema.LocalExtrema.LocalNeighborhoodCheck;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.algorithm.localextrema.SubpixelLocalization;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class DetectionUtils
{

	/**
	 * Preview a detection results.
	 * <p>
	 * This method returns immediately and execute the detection in a separate
	 * thread. It executes the detection in one frame only and writes the
	 * results in the specified model object.
	 * 
	 * @param model
	 *            the model to write detection results in.
	 * @param settings
	 *            the settings to use for the image input and the ROI input.
	 * @param detectorFactory
	 *            the detector factory to use for detection.
	 * @param detectorSettings
	 *            the settings for the detection, specific to the detector
	 *            factory/
	 * @param frame
	 *            the frame (0-based) to execute the detection in.
	 * @param logger
	 *            a logger to write results and error messages to.
	 * @param buttonEnabler
	 *            a consumer that will receive <code>false</code> at the
	 *            beginning of the preview, and <code>true</code> at its end.
	 *            Can be used to disable GUI elements.
	 */
	public static final void preview(
			final Model model,
			final Settings settings,
			final SpotDetectorFactoryBase< ? > detectorFactory,
			final Map< String, Object > detectorSettings,
			final int frame,
			final Logger logger,
			final Consumer< Boolean > buttonEnabler
			)
	{
		buttonEnabler.accept( false );
		new Thread( "TrackMate preview detection thread" )
		{
			@Override
			public void run()
			{
				try
				{

					final Settings lSettings = new Settings();
					lSettings.setFrom( settings.imp );
					lSettings.tstart = frame;
					lSettings.tend = frame;
					lSettings.roi = settings.roi;

					lSettings.detectorFactory = detectorFactory;
					lSettings.detectorSettings = detectorSettings;

					final TrackMate trackmate = new TrackMate( lSettings );
					trackmate.getModel().setLogger( logger );

					final boolean detectionOk = trackmate.execDetection();
					if ( !detectionOk )
					{
						logger.error( trackmate.getErrorMessage() );
						return;
					}
					logger.log( "Found " + trackmate.getModel().getSpots().getNSpots( false ) + " spots." );

					// Wrap new spots in a list.
					final SpotCollection newspots = trackmate.getModel().getSpots();
					final Iterator< Spot > it = newspots.iterator( frame, false );
					final ArrayList< Spot > spotsToCopy = new ArrayList<>( newspots.getNSpots( frame, false ) );
					while ( it.hasNext() )
						spotsToCopy.add( it.next() );

					// Pass new spot list to model.
					model.getSpots().put( frame, spotsToCopy );
					// Make them visible
					for ( final Spot spot : spotsToCopy )
						spot.putFeature( SpotCollection.VISIBILITY, SpotCollection.ONE );

					// Generate event for listener to reflect changes.
					model.setSpots( model.getSpots(), true );

				}
				catch ( final Exception e )
				{
					logger.error( e.getMessage() );
					e.printStackTrace();
				}
				finally
				{
					buttonEnabler.accept( true );
				}
			}
		}.start();
	}

	/**
	 * Returns <code>true</code> if the specified image is 2D. It can have
	 * multiple channels and multiple time-points; this method only looks at
	 * whether several Z-slices can be found.
	 * 
	 * @param img
	 *            the image.
	 * @return
	 */
	public static final boolean is2D( final ImgPlus< ? > img )
	{
		return img.dimensionIndex( Axes.Z ) < 0
				|| img.dimension( img.dimensionIndex( Axes.Z ) ) <= 1;
	}

	public static final boolean is2D( final ImagePlus imp )
	{
		return imp.getNSlices() <= 1;
	}

	/**
	 * Creates a laplacian of gaussian (LoG) kernel tuned for blobs with a
	 * radius specified <b>using calibrated units</b>. The specified calibration
	 * is used to determine the dimensionality of the kernel and to map it on a
	 * pixel grid.
	 * 
	 * @param radius
	 *            the blob radius (in image unit).
	 * @param nDims
	 *            the dimensionality of the desired kernel. Must be 1, 2 or 3.
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

		// Work in image coordinates
		while ( c.hasNext() )
		{
			c.fwd();
			c.localize( coords );

			double sumx2 = 0.;
			double mantissa = 0.;
			for ( int d = 0; d < coords.length; d++ )
			{
				final double x = calibration[ d ] * ( coords[ d ] - middle[ d ] );
				sumx2 += ( x * x );
				mantissa += 1. / sigmaPixels[ d ] / sigmaPixels[ d ] * ( x * x / sigma / sigma - 1 );
			}
			final double exponent = -sumx2 / 2. / sigma / sigma;

			/*
			 * LoG normalization factor, so that the filtered peak have the
			 * maximal value for spots that have the size this kernel is tuned
			 * to. With this value, the peak value will be of the same order of
			 * magnitude than the raw spot (if it has the right size). This
			 * value also ensures that if the image has its calibration changed,
			 * one will retrieve the same peak value than before scaling.
			 * However, I (JYT) could not derive the exact formula if the image
			 * is scaled differently across X, Y and Z.
			 */
			final double C = 1. / Math.PI / sigmaPixels[ 0 ] / sigmaPixels[ 0 ];

			c.get().setReal( -C * mantissa * Math.exp( exponent ) );
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
	public static final < T extends RealType< T > > Img< FloatType > copyToFloatImg( final RandomAccessible< T > img, final Interval interval, final ImgFactory< FloatType > factory )
	{
		final Img< FloatType > output = factory.create( interval );
		final RandomAccess< T > in = Views.zeroMin( Views.interval( img, interval ) ).randomAccess();
		final Cursor< FloatType > out = output.cursor();
		final RealFloatConverter< T > c = new RealFloatConverter<>();
		
		while ( out.hasNext() )
		{
			out.fwd();
			in.setPosition( out );
			c.convert( in.get(), out.get() );
		}
		return output;
	}

	/**
	 * Returns a new {@link Interval}, built by squeezing out singleton
	 * dimensions from the specified interval.
	 *
	 * @param interval
	 *            the interval to squeeze.
	 * @return a new interval.
	 */
	public static final Interval squeeze( final Interval interval )
	{
		int nNonSingletonDimensions = 0;
		for ( int d = nNonSingletonDimensions; d < interval.numDimensions(); d++ )
		{
			if ( interval.dimension( d ) > 1 )
			{
				nNonSingletonDimensions++;
			}
		}

		final long[] min = new long[ nNonSingletonDimensions ];
		final long[] max = new long[ nNonSingletonDimensions ];
		int index = 0;
		for ( int d = 0; d < interval.numDimensions(); d++ )
		{
			if ( interval.dimension( d ) > 1 )
			{
				min[ index ] = interval.min( d );
				max[ index ] = interval.max( d );
				index++;
			}
		}
		return new FinalInterval( min, max );
	}

	/**
	 * Apply a simple 3x3 median filter to the target image.
	 */
	public static final < R extends RealType< R > & NativeType< R > > Img< R > applyMedianFilter( final RandomAccessibleInterval< R > image )
	{
		final MedianFilter2D< R > medFilt = new MedianFilter2D<>( image, 1 );
		if ( !medFilt.checkInput() || !medFilt.process() )
		{ return null; }
		return medFilt.getResult();
	}

	public static final List< Spot > findLocalMaxima( final RandomAccessibleInterval< FloatType > source, final double threshold, final double[] calibration, final double radius, final boolean doSubPixelLocalization, final int numThreads )
	{
		/*
		 * Find maxima.
		 */

		final FloatType val = new FloatType();
		val.setReal( threshold );
		final LocalNeighborhoodCheck< Point, FloatType > localNeighborhoodCheck = new LocalExtrema.MaximumCheck<>( val );
		final IntervalView< FloatType > dogWithBorder = Views.interval( Views.extendMirrorSingle( source ), Intervals.expand( source, 1 ) );
		final ExecutorService service = Executors.newFixedThreadPool( numThreads );
		List< Point > peaks;
		try
		{
			peaks = LocalExtrema.findLocalExtrema( dogWithBorder, localNeighborhoodCheck, new RectangleShape( 1, true ), service, numThreads );
		}
		catch ( InterruptedException | ExecutionException e )
		{
			e.printStackTrace();
			peaks = Collections.emptyList();
		}
		service.shutdown();

		if ( peaks.isEmpty() )
		{ return Collections.emptyList(); }

		final List< Spot > spots;
		if ( doSubPixelLocalization )
		{

			/*
			 * Sub-pixel localize them.
			 */

			final SubpixelLocalization< Point, FloatType > spl = new SubpixelLocalization<>( source.numDimensions() );
			spl.setNumThreads( numThreads );
			spl.setReturnInvalidPeaks( true );
			spl.setCanMoveOutside( true );
			spl.setAllowMaximaTolerance( true );
			spl.setMaxNumMoves( 10 );
			final ArrayList< RefinedPeak< Point > > refined = spl.process( peaks, dogWithBorder, source );

			spots = new ArrayList<>( refined.size() );
			final RandomAccess< FloatType > ra = source.randomAccess();

			/*
			 * Deal with different dimensionality manually. Profound comment:
			 * this is the proof that this part of the code is sloppy. ImgLib2
			 * is supposed to be dimension-generic. I just did not use properly
			 * here.
			 */

			if ( source.numDimensions() > 2 )
			{ // 3D
				for ( final RefinedPeak< Point > refinedPeak : refined )
				{
					ra.setPosition( refinedPeak.getOriginalPeak() );
					final double quality = ra.get().getRealDouble();
					final double x = refinedPeak.getDoublePosition( 0 ) * calibration[ 0 ];
					final double y = refinedPeak.getDoublePosition( 1 ) * calibration[ 1 ];
					final double z = refinedPeak.getDoublePosition( 2 ) * calibration[ 2 ];
					final Spot spot = new Spot( x, y, z, radius, quality );
					spots.add( spot );
				}
			}
			else if ( source.numDimensions() > 1 )
			{ // 2D
				final double z = 0;
				for ( final RefinedPeak< Point > refinedPeak : refined )
				{
					ra.setPosition( refinedPeak.getOriginalPeak() );
					final double quality = ra.get().getRealDouble();
					final double x = refinedPeak.getDoublePosition( 0 ) * calibration[ 0 ];
					final double y = refinedPeak.getDoublePosition( 1 ) * calibration[ 1 ];
					final Spot spot = new Spot( x, y, z, radius, quality );
					spots.add( spot );
				}
			}
			else
			{ // 1D
				final double z = 0;
				final double y = 0;
				for ( final RefinedPeak< Point > refinedPeak : refined )
				{
					ra.setPosition( refinedPeak.getOriginalPeak() );
					final double quality = ra.get().getRealDouble();
					final double x = refinedPeak.getDoublePosition( 0 ) * calibration[ 0 ];
					final Spot spot = new Spot( x, y, z, radius, quality );
					spots.add( spot );
				}

			}
		}
		else
		{
			spots = new ArrayList<>( peaks.size() );
			final RandomAccess< FloatType > ra = source.randomAccess();
			if ( source.numDimensions() > 2 )
			{ // 3D
				for ( final Point peak : peaks )
				{
					ra.setPosition( peak );
					final double quality = ra.get().getRealDouble();
					final double x = peak.getDoublePosition( 0 ) * calibration[ 0 ];
					final double y = peak.getDoublePosition( 1 ) * calibration[ 1 ];
					final double z = peak.getDoublePosition( 2 ) * calibration[ 2 ];
					final Spot spot = new Spot( x, y, z, radius, quality );
					spots.add( spot );
				}
			}
			else if ( source.numDimensions() > 1 )
			{ // 2D
				final double z = 0;
				for ( final Point peak : peaks )
				{
					ra.setPosition( peak );
					final double quality = ra.get().getRealDouble();
					final double x = peak.getDoublePosition( 0 ) * calibration[ 0 ];
					final double y = peak.getDoublePosition( 1 ) * calibration[ 1 ];
					final Spot spot = new Spot( x, y, z, radius, quality );
					spots.add( spot );
				}
			}
			else
			{ // 1D
				final double z = 0;
				final double y = 0;
				for ( final Point peak : peaks )
				{
					ra.setPosition( peak );
					final double quality = ra.get().getRealDouble();
					final double x = peak.getDoublePosition( 0 ) * calibration[ 0 ];
					final Spot spot = new Spot( x, y, z, radius, quality );
					spots.add( spot );
				}

			}
		}

		return spots;
	}

	/**
	 * Return a view of the specified input image, at the specified channel
	 * (0-based) and the specified frame (0-based too).
	 * 
	 * @param <T>
	 *            the type of the input image.
	 * @param img
	 *            the input image.
	 * @param channel
	 *            the channel to extract.
	 * @param frame
	 *            the frame to extract.
	 * @return a view of the input image.
	 */
	public static final < T extends Type< T > > RandomAccessibleInterval< T > prepareFrameImg(
			final ImgPlus< T > img,
			final int channel,
			final int frame )
	{
		final ImgPlus< T > singleTimePoint;
		if ( img.dimensionIndex( Axes.TIME ) < 0 )
			singleTimePoint = img;
		else
			singleTimePoint = ImgPlusViews.hyperSlice( img, img.dimensionIndex( Axes.TIME ), frame );

		final ImgPlus< T > singleChannel;
		if ( singleTimePoint.dimensionIndex( Axes.CHANNEL ) < 0 )
			singleChannel = singleTimePoint;
		else
			singleChannel = ImgPlusViews.hyperSlice( singleTimePoint, singleTimePoint.dimensionIndex( Axes.CHANNEL ), channel );
		return singleChannel;
	}
}
