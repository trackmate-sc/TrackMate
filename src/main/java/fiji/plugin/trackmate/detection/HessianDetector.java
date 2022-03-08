package fiji.plugin.trackmate.detection;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.ToDoubleFunction;

import fiji.plugin.trackmate.Spot;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.gradient.HessianMatrix;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.outofbounds.OutOfBoundsBorderFactory;
import net.imglib2.parallel.TaskExecutor;
import net.imglib2.parallel.TaskExecutors;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import net.imglib2.view.composite.CompositeIntervalView;
import net.imglib2.view.composite.RealComposite;

public class HessianDetector< T extends RealType< T > & NativeType< T > > implements SpotDetector< T >, MultiThreaded
{

	/*
	 * FIELDS
	 */

	private final static String BASE_ERROR_MESSAGE = "HessianDetector: ";

	private final RandomAccessible< T > img;

	private final Interval interval;

	private final double[] calibration;

	private final double radiusXY;

	private final double radiusZ;

	private final double threshold;

	private final boolean doSubPixelLocalization;

	private String errorMessage;

	private List< Spot > spots;

	private long processingTime;

	private int numThreads;

	private final boolean normalize;

	/*
	 * CONSTRUCTOR
	 */

	public HessianDetector(
			final RandomAccessible< T > img,
			final Interval interval,
			final double[] calibration,
			final double radiusXY,
			final double radiusZ,
			final double threshold,
			final boolean normalize,
			final boolean doSubPixelLocalization )
	{
		this.img = img;
		this.interval = DetectionUtils.squeeze( interval );
		this.calibration = calibration;
		this.radiusXY = radiusXY;
		this.radiusZ = radiusZ;
		this.threshold = threshold;
		this.normalize = normalize;
		this.doSubPixelLocalization = doSubPixelLocalization;
		setNumThreads();
	}

	/*
	 * METHODS
	 */

	@Override
	public boolean checkInput()
	{
		if ( null == img )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Image is null.";
			return false;
		}
		if ( img.numDimensions() > 3 || img.numDimensions() < 2 )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Image must be 2D or 3D, got " + img.numDimensions() + "D.";
			return false;
		}
		return true;
	}

	@Override
	public boolean process()
	{
		spots = null;
		errorMessage = null;

		final long start = System.currentTimeMillis();
		try
		{
			// Compute Hessian.
			final Img< FloatType > det = computeHessianDeterminant( new FloatType() );

			// Normalize from 0 to 1.
			if ( normalize )
				DetectionUtils.normalize( det );

			// Translate back with respect to ROI.
			final long[] minopposite = new long[ interval.numDimensions() ];
			interval.min( minopposite );
			final IntervalView< FloatType > to = Views.translate( det, minopposite );

			// Find spots.
			spots = DetectionUtils.findLocalMaxima( to, threshold, calibration, radiusXY, doSubPixelLocalization, numThreads );
		}
		catch ( final IncompatibleTypeException | InterruptedException | ExecutionException e )
		{
			errorMessage = BASE_ERROR_MESSAGE + e.getMessage();
			e.printStackTrace();
			return false;
		}

		final long end = System.currentTimeMillis();
		this.processingTime = end - start;
		return true;
	}

	private final < R extends RealType< R > & NativeType< R > > Img< R > computeHessianDeterminant( final R type ) throws IncompatibleTypeException, InterruptedException, ExecutionException
	{
		// Squeeze singleton dimensions
		final int n = interval.numDimensions();

		// Sigmas in pixel units.
		final double[] radius = new double[] { radiusXY, radiusXY, radiusZ };
		final double[] sigmas = new double[ n ];
		for ( int d = 0; d < n; d++ )
		{
			final double cal = d < calibration.length ? calibration[ d ] : 1;
			sigmas[ d ] = radius[ d ] / cal / Math.sqrt( n );
		}

		// Get a suitable image factory.
		final long[] gradientDims = new long[ n + 1 ];
		final long[] hessianDims = new long[ n + 1 ];
		for ( int d = 0; d < n; d++ )
		{
			hessianDims[ d ] = interval.dimension( d );
			gradientDims[ d ] = interval.dimension( d );
		}
		hessianDims[ n ] = n * ( n + 1 ) / 2;
		gradientDims[ n ] = n;
		final Dimensions hessianDimensions = FinalDimensions.wrap( hessianDims );
		final FinalDimensions gradientDimensions = FinalDimensions.wrap( gradientDims );
		final ImgFactory< R > factory = Util.getArrayOrCellImgFactory( hessianDimensions, type );
		final Img< R > hessian = factory.create( hessianDimensions );
		final Img< R > gradient = factory.create( gradientDimensions );
		final Img< R > gaussian = factory.create( interval );

		// Handle multithreading.
		final ExecutorService es = Executors.newFixedThreadPool( numThreads );
		// Hessian calculation.
		final IntervalView< T > input = Views.zeroMin( Views.interval( img, interval ) );
		HessianMatrix.calculateMatrix( input, gaussian,
				gradient, hessian, new OutOfBoundsBorderFactory<>(), numThreads, es,
				sigmas );

		// Normalize for pixel size.
		final IntervalView< R > H = HessianMatrix.scaleHessianMatrix( hessian, sigmas );

		// Compute determinant.
		final ToDoubleFunction< RealComposite< R > > detcalc;
		if ( n == 2 )
		{
			detcalc = ( c ) -> {
				final double a00 = c.get( 0 ).getRealDouble();
				final double a01 = c.get( 1 ).getRealDouble();
				final double a11 = c.get( 2 ).getRealDouble();
				final double det = a00 * a11 - a01 * a01;
				return det;
			};
		}
		else // n == 3
		{
			detcalc = ( c ) -> {
				final double a00 = c.get( 0 ).getRealDouble();
				final double a01 = c.get( 1 ).getRealDouble();
				final double a02 = c.get( 2 ).getRealDouble();
				final double a11 = c.get( 3 ).getRealDouble();
				final double a12 = c.get( 4 ).getRealDouble();
				final double a22 = c.get( 5 ).getRealDouble();

				final double x = a11 * a22 - a12 * a12;
				final double y = a01 * a22 - a02 * a12;
				final double z = a01 * a12 - a02 * a11;

				final double det = a00 * x - a01 * y + a02 * z;
				return -det;
				// Change sign so that bright detections have positive values.
			};
		}

		final CompositeIntervalView< R, RealComposite< R > > composite = Views.collapseReal( H );
		final Img< R > det = factory.create( interval );

		final TaskExecutor taskExecutor = TaskExecutors.forExecutorServiceAndNumTasks( es, numThreads );
		LoopBuilder.setImages( composite, det ).multiThreaded( taskExecutor ).forEachPixel(
				( c, d ) -> d.setReal( detcalc.applyAsDouble( c ) ) );

		return det;
	}

	@Override
	public List< Spot > getResult()
	{
		return spots;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public void setNumThreads()
	{
		this.numThreads = Runtime.getRuntime().availableProcessors() / 2;
	}

	@Override
	public void setNumThreads( final int numThreads )
	{
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}
}
