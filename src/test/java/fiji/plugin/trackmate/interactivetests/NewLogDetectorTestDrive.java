package fiji.plugin.trackmate.interactivetests;

import fiji.plugin.trackmate.detection.DetectionUtils;
import ij.ImagePlus;
import io.scif.img.ImgIOException;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.fft2.FFTConvolution;
import net.imglib2.algorithm.localextrema.LocalExtrema;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.algorithm.localextrema.SubpixelLocalization;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.Axes;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class NewLogDetectorTestDrive {


	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public static < T extends NumericType< T > & NativeType< T >> void main( final String[] args ) throws ImgIOException, IncompatibleTypeException
	{

		final int nThreads = 1; //Runtime.getRuntime().availableProcessors();

		// final File file = new File(
		// "/Users/JeanYves/Desktop/Data/FakeTracks.tif" );
		final File file = new
		File("/Users/tinevez/Projects/JYTinevez/ISBI/ChallengeData/VIRUS/VIRUS snr 7 density mid.tif");
		// final File file = new File(
		// "/Users/JeanYves/Desktop/Data/FakeTracks.tif" );
		//		final File file = new File( "/Users/JeanYves/Documents/Projects/ISBI/VIRUS/VIRUS snr 4 density mid.tif" );

		final ImgPlus< T > img = ImagePlusAdapter.wrapImgPlus( new ImagePlus( file.getAbsolutePath() ) );

		final int timeDim = img.dimensionIndex(Axes.TIME);
		final long nTimepoints = img.dimension( timeDim );

		final double[] calibration = new double[ 3 ];
		calibration[ 0 ] = img.averageScale( 0 );
		calibration[ 1 ] = img.averageScale( 1 );
		calibration[ 2 ] = img.averageScale( 2 );
		// No visible benefit to do that out of the loop.

		final ExecutorService threadPool = Executors.newFixedThreadPool( nThreads );
		for ( int i = 0; i < nTimepoints; i++ )
		{

			final long timepoint = i;
			final Runnable runnable = new Runnable()
			{
				@Override
				public void run()
				{
					final IntervalView frame = Views.hyperSlice( img, timeDim, timepoint );

					final long start = System.currentTimeMillis();

					final Img<FloatType> kernel = DetectionUtils.createLoGKernel(4, img.numDimensions() - 1, calibration);
					final RandomAccessibleInterval<FloatType> output = new ArrayImgFactory().create(frame, new FloatType());
					FFTConvolution.convolve(Views.extendZero(frame), frame, Views.extendZero(kernel), kernel, output, new ArrayImgFactory());

					final long t1 = System.currentTimeMillis();
					System.out.println( timepoint + ": Convolution done in " + ( t1 - start ) + " ms." );

					final ArrayList<Point> peaks = LocalExtrema.findLocalExtrema(output, new LocalExtrema.MaximumCheck(new FloatType(1f)), 1);
					final long t2 = System.currentTimeMillis();
					System.out.println(timepoint + ": Extrema finding done in " + (t2 - t1) + " ms.");

					final SubpixelLocalization<Point, FloatType> spl = new SubpixelLocalization<Point, FloatType>(output.numDimensions());
					spl.setAllowMaximaTolerance(true);
					spl.setMaxNumMoves(10);
					spl.setNumThreads(1);
					final ArrayList<RefinedPeak<Point>> refined = spl.process(peaks, output, output);
					final long end = System.currentTimeMillis();
					System.out.println(timepoint + ": Sub-pixel refinment done in " + (end - t2) + " ms.");
					System.out.println(timepoint + ": Detection done in " + (end - start) + " ms. Found " + refined.size() + " spots.");
				}
			};
			threadPool.execute( runnable );

		}

		threadPool.shutdown();
		try
		{
			threadPool.awaitTermination( 1000, TimeUnit.DAYS );
		}
		catch ( final InterruptedException e )
		{
			e.printStackTrace();
		}


	}

}
