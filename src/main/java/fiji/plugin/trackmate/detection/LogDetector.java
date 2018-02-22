package fiji.plugin.trackmate.detection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fiji.plugin.trackmate.Spot;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.fft2.FFTConvolution;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class LogDetector< T extends RealType< T > & NativeType< T >> implements SpotDetector< T >, MultiThreaded
{

	/*
	 * FIELDS
	 */

	private final static String BASE_ERROR_MESSAGE = "LogDetector: ";

	/** The image to segment. Will not modified. */
	protected RandomAccessible< T > img;

	protected double radius;

	protected double threshold;

	protected boolean doSubPixelLocalization;

	protected boolean doMedianFilter;

	protected String baseErrorMessage;

	protected String errorMessage;

	/** The list of {@link Spot} that will be populated by this detector. */
	protected List< Spot > spots = new ArrayList<>();

	/** The processing time in ms. */
	protected long processingTime;

	protected int numThreads;

	protected final Interval interval;

	protected final double[] calibration;

	/*
	 * CONSTRUCTORS
	 */

	public LogDetector( final RandomAccessible< T > img, final Interval interval, final double[] calibration, final double radius, final double threshold, final boolean doSubPixelLocalization, final boolean doMedianFilter )
	{
		this.img = img;
		this.interval = DetectionUtils.squeeze( interval );
		this.calibration = calibration;
		this.radius = radius;
		this.threshold = threshold;
		this.doSubPixelLocalization = doSubPixelLocalization;
		this.doMedianFilter = doMedianFilter;
		this.baseErrorMessage = BASE_ERROR_MESSAGE;
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
			errorMessage = baseErrorMessage + "Image is null.";
			return false;
		}
		if ( img.numDimensions() > 3 )
		{
			errorMessage = baseErrorMessage + "Image must be 1D, 2D or 3D, got " + img.numDimensions() + "D.";
			return false;
		}
		return true;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		/*
		 * Copy to float for convolution.
		 */

		final ImgFactory< FloatType > factory = Util.getArrayOrCellImgFactory( interval, new FloatType() );
		Img< FloatType > floatImg = DetectionUtils.copyToFloatImg( img, interval, factory );

		/*
		 * Do median filtering (or not).
		 */

		if ( doMedianFilter )
		{
			floatImg = DetectionUtils.applyMedianFilter( floatImg );
			if ( null == floatImg )
			{
				errorMessage = BASE_ERROR_MESSAGE + "Failed to apply median filter.";
				return false;
			}
		}


		// Squeeze singleton dimensions
		int ndims = interval.numDimensions();
		for ( int d = 0; d < interval.numDimensions(); d++ )
			if ( interval.dimension( d ) <= 1 )
				ndims--;

		final Img< FloatType > kernel = DetectionUtils.createLoGKernel( radius, ndims, calibration );
		final FFTConvolution< FloatType > fftconv = new FFTConvolution<>( floatImg, kernel );

		/*
		 * Determine the right img factory for FFT calculation.
		 */
		Interval fftinterval = floatImg;
		for ( int d = 0; d < kernel.numDimensions(); d++ )
			fftinterval = Intervals.expand( fftinterval, kernel.dimension( d ), d );
		final ImgFactory< ComplexFloatType > imgFactory = Util.getArrayOrCellImgFactory( fftinterval, new ComplexFloatType() );
		fftconv.setFFTImgFactory( imgFactory );

		final ExecutorService service = Executors.newFixedThreadPool(numThreads);
		fftconv.setExecutorService(service);

		fftconv.convolve();
		service.shutdown();

		final long[] minopposite = new long[ interval.numDimensions() ];
		interval.min( minopposite );
		for ( int d = 0; d < minopposite.length; d++ )
			minopposite[ d ] = -minopposite[ d ];

		final IntervalView< FloatType > to = Views.offset( floatImg, minopposite );
		spots = DetectionUtils.findLocalMaxima( to, threshold, calibration, radius, doSubPixelLocalization, numThreads );

		final long end = System.currentTimeMillis();
		this.processingTime = end - start;

		return true;
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
		this.numThreads = Runtime.getRuntime().availableProcessors();
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
