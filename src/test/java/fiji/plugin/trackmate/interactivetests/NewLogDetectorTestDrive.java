package fiji.plugin.trackmate.interactivetests;

import fiji.plugin.trackmate.detection.LogDetector;
import fiji.plugin.trackmate.util.TMUtils;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.imglib2.meta.Axes;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.view.HyperSliceImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class NewLogDetectorTestDrive {


	public static < T extends RealType< T > & NativeType< T >> void main( final String[] args ) throws ImgIOException
	{

		final double minPeakValue = 1;

		final File file = new File("/Users/tinevez/Projects/JYTinevez/ISBI/ChallengeData/VIRUS/VIRUS snr 7 density mid.tif");
		final ImgPlus< ? > raw = new ImgOpener().openImg( file.getAbsolutePath() );
		@SuppressWarnings("unchecked")
		final ImgPlus< T > img = ( ImgPlus< T > ) raw;

		// warmup
		measure( img, 20, minPeakValue );
		System.out.println( "nThreads\ttime(s)" );
		for ( int numThreads = 1; numThreads <= Runtime.getRuntime().availableProcessors(); numThreads++ )
		{
			System.out.println( numThreads + "\t" + measure( img, numThreads, minPeakValue ) );
		}

		// measureSplitWork(img, 12, 2, minPeakValue);
		// System.out.println("nTPoints\tnSplits\ttime(s)");
		// final int[] nt = new int[] { 24, 12, 8, 6, 4, 3, 2, 1 };
		// final int[] ns = new int[] { 1, 2, 3, 4, 6, 8, 12, 24 };
		// for (int i = 0; i < ns.length; i++) {
		// System.out.println(nt[i] + "\t" + ns[i] + "\t" +
		// measureSplitWork(img, nt[i], ns[i], minPeakValue));
		// }

	}

	public static final < T extends RealType< T > & NativeType< T >> double measureSplitWork( final ImgPlus< T > img, final int nSimultTimePoints, final int nSplits, final double minPeakValue )
	{

		final int timeDim = img.dimensionIndex(Axes.TIME);
		final int cDim = img.dimensionIndex(Axes.CHANNEL);
		final double[] calibration = TMUtils.getSpatialCalibration( img );
		final double radius = 2;
		final ExecutorService ex = Executors.newFixedThreadPool(nSimultTimePoints);

		final long start = System.currentTimeMillis();
		for (int t = 0; t < img.dimension(timeDim); t++) {

			final int ft = t;

			final Runnable command = new Runnable() {
				@Override
				public void run() {

					final HyperSliceImgPlus<T> frame = new HyperSliceImgPlus<T>(new HyperSliceImgPlus<T>(img, timeDim, ft), cDim, 0);
					final LogDetector< T > detector = new LogDetector< T >( frame, frame, calibration, radius, minPeakValue, false, false );
					detector.setNumThreads( 1 );
					if ( !detector.checkInput() || !detector.process() )
					{
						System.out.println( "Frame " + ft + ": " + detector.getErrorMessage() );
					}
					else
					{
						System.out.println( "Frame " + ft + ": found " + detector.getResult().size() + " spots." );
					}
				}
			};

			ex.execute(command);

		}

		ex.shutdown();
		try {
			ex.awaitTermination(1000, TimeUnit.DAYS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

		final long end = System.currentTimeMillis();
		return ((end - start) / 1e3d);
	}

	public static final < T extends RealType< T > & NativeType< T >> double measure( final ImgPlus< T > img, final int numThreads, final double minPeakValue )
	{

		final int timeDim = img.dimensionIndex(Axes.TIME);
		final double[] calibration = TMUtils.getSpatialCalibration( img );
		final double radius = 2;

		final ExecutorService ex = Executors.newFixedThreadPool(numThreads);

		final long start = System.currentTimeMillis();

		for (int t = 0; t < img.dimension(timeDim); t++) {

			final int ft = t;

			final Runnable command = new Runnable() {
				@Override
				public void run() {

					final IntervalView< T > frame = Views.hyperSlice( img, timeDim, ft );

					final LogDetector< T > detector = new LogDetector< T >( frame, frame, calibration, radius, minPeakValue, false, false );
					detector.setNumThreads( 1 );
					if ( !detector.checkInput() || !detector.process() )
					{
						System.out.println( "Frame " + ft + ": " + detector.getErrorMessage() );
					}
				}
			};

			ex.execute(command);

		}

		ex.shutdown();
		try {
			ex.awaitTermination(1000, TimeUnit.DAYS);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

		final long end = System.currentTimeMillis();
		return ((end - start) / 1e3d);
	}

}