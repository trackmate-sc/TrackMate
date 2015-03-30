package fiji.plugin.trackmate.detection.findmaxima;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.localextrema.LocalExtrema;
import net.imglib2.algorithm.localextrema.LocalExtrema.MaximumCheck;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.detection.SpotDetector;

public class FindMaximaSpotDetector< T extends RealType< T > & NativeType< T >> implements SpotDetector< T >, MultiThreaded
{

	private static final String BASE_ERROR_MESSAGE = "[FindMaximaSpotDetector] ";

	private static final double DEFAULT_RADIUS = 2.5;

	private final RandomAccessible< T > img;

	private final Interval interval;

	private final double[] calibration;

	private final double threshold;

	private int numThreads;

	private long processingTime;

	private String errorMessage;

	private List< Spot > spots;

	private final double radius;

	/*
	 * CONSTRUCTORS
	 */

	public FindMaximaSpotDetector( final RandomAccessible< T > img, final Interval interval, final double[] calibration, final double threshold )
	{
		this.img = img;
		this.interval = DetectionUtils.squeeze( interval );
		this.calibration = calibration;
		this.threshold = threshold;
		this.radius = DEFAULT_RADIUS;
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
		return true;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		/*
		 * Find maxima with plain ImgLib2 algos. Emulate the MaximaFinder
		 * plugin, with no maxima on edges.
		 */

		final ImgFactory< FloatType > factory = Util.getArrayOrCellImgFactory( interval, new FloatType() );
		final Img< FloatType > source = DetectionUtils.copyToFloatImg( img, interval, factory );
		final FloatType minPeakVal = new FloatType( ( float ) threshold );
		// Threshold plays the role of tolerance.
		minPeakVal.setReal( threshold );
		final MaximumCheck< FloatType > check = new LocalExtrema.MaximumCheck< FloatType >( minPeakVal );
		final ExecutorService service = Executors.newFixedThreadPool( numThreads );
		final ArrayList< Point > peaks = LocalExtrema.findLocalExtrema( source, check, service );
		service.shutdown();

		/*
		 * Convert to spots.
		 */

		spots = new ArrayList< Spot >( peaks.size() );
		final RandomAccess< T > ra = img.randomAccess();
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
