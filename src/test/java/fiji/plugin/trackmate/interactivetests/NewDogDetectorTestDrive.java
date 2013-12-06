package fiji.plugin.trackmate.interactivetests;

import fiji.plugin.trackmate.util.TMUtils;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.dog.DifferenceOfGaussian;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.localextrema.LocalExtrema;
import net.imglib2.algorithm.localextrema.LocalExtrema.LocalNeighborhoodCheck;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.algorithm.localextrema.SubpixelLocalization;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.meta.Axes;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.view.HyperSliceImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class NewDogDetectorTestDrive {

	public static <T extends NumericType<T> & NativeType<T>> void main(final String[] args) throws ImgIOException {

		final double minPeakValue = 7;

		final File file = new File("/Users/tinevez/Projects/JYTinevez/ISBI/ChallengeData/VIRUS/VIRUS snr 7 density mid.tif");
		@SuppressWarnings("unchecked")
		final ImgPlus<T> img = new ImgOpener().openImg(file.getAbsolutePath());

		//		// warmup
		//		measure(img, 20, minPeakValue);
		//		System.out.println("nThreds\ttime(s)");
		//		for (int numThreads = 1; numThreads <= Runtime.getRuntime().availableProcessors(); numThreads++) {
		//
		//			System.out.println(numThreads + "\t" + measure(img, numThreads, minPeakValue));
		//
		//		}
		// warmup
		measureSplitWork(img, 12, 2, minPeakValue);
		System.out.println("nTPoints\tnSplits\ttime(s)");
		final int[] nt = new int[] { 24, 12, 8, 6, 4, 3, 2, 1 };
		final int[] ns = new int[] { 1, 2, 3, 4, 6, 8, 12, 24 };
		for (int i = 0; i < ns.length; i++) {

			System.out.println(nt[i] + "\t" + ns[i] + "\t" + measureSplitWork(img, nt[i], ns[i], minPeakValue));

		}

	}

	public static final <T extends NumericType<T> & NativeType<T>> double measureSplitWork(final ImgPlus<T> img, final int nSimultTimePoints, final int nSplits, final double minPeakValue) {

		final int timeDim = img.dimensionIndex(Axes.TIME);
		final int cDim = img.dimensionIndex(Axes.CHANNEL);
		final ExecutorService ex = Executors.newFixedThreadPool(nSimultTimePoints);

		final long start = System.currentTimeMillis();

		for (int t = 0; t < img.dimension(timeDim); t++) {

			final int ft = t;

			final Runnable command = new Runnable() {
				@SuppressWarnings("unused")
				@Override
				public void run() {

					final HyperSliceImgPlus<T> frame = new HyperSliceImgPlus<T>(new HyperSliceImgPlus<T>(img, timeDim, ft), cDim, 0);

					final long start = System.currentTimeMillis();
					final FloatType type = new FloatType();
					final RandomAccessibleInterval<FloatType> dog = Util.getArrayOrCellImgFactory(frame, type).create(frame, type);
					final RandomAccessibleInterval<FloatType> dog2 = Util.getArrayOrCellImgFactory(frame, type).create(frame, type);

					final double[] pixelSize = TMUtils.getSpatialCalibration(img);

					final double radius = 2;
					final double sigma1 = radius / Math.sqrt(frame.numDimensions()) * 0.9;
					final double sigma2 = radius / Math.sqrt(frame.numDimensions()) * 1.1;
					final double[][] sigmas = DifferenceOfGaussian.computeSigmas(0.5, 2, pixelSize, sigma1, sigma2);
					try {
						Gauss3.gauss(sigmas[1], Views.extendMirrorSingle(frame), dog2, nSplits);
						Gauss3.gauss(sigmas[0], Views.extendMirrorSingle(frame), dog, nSplits);
					} catch (final IncompatibleTypeException e) {
						e.printStackTrace();
					}

					final IterableInterval<FloatType> dogIterable = Views.iterable(dog);
					final IterableInterval<FloatType> tmpIterable = Views.iterable(dog2);
					final Cursor<FloatType> dogCursor = dogIterable.cursor();
					final Cursor<FloatType> tmpCursor = tmpIterable.cursor();
					while (dogCursor.hasNext())
						dogCursor.next().sub(tmpCursor.next());

					final long t1 = System.currentTimeMillis();
					//					System.out.println(ft + ": DoG computed in " + (t1 - start) + " ms.");

					final FloatType val = new FloatType();
					val.setReal(minPeakValue * (sigma2 / sigma1 - 1.0));
					final IntervalView<FloatType> dogWithBorder = Views.interval(Views.extendZero(dog), Intervals.expand(dog, 1));
					final LocalNeighborhoodCheck<Point, FloatType> localNeighborhoodCheck = new LocalExtrema.MaximumCheck<FloatType>(val);
					final ArrayList<Point> peaks = LocalExtrema.findLocalExtrema(dogWithBorder, localNeighborhoodCheck, nSplits);
					final long t2 = System.currentTimeMillis();
					//					System.out.println(ft + ": Maxima found in " + (t2 - t1) + " ms. " + peaks.size() + " maxima found.");

					final SubpixelLocalization<Point, FloatType> spl = new SubpixelLocalization<Point, FloatType>(dog.numDimensions());
					spl.setNumThreads(nSplits);
					spl.setReturnInvalidPeaks(true);
					spl.setCanMoveOutside(true);
					spl.setAllowMaximaTolerance(true);
					spl.setMaxNumMoves(10);
					final ArrayList<RefinedPeak<Point>> refined = spl.process(peaks, dogWithBorder, dog);
					final long end = System.currentTimeMillis();
					//					System.out.println(ft + ": Sup-pixel localization done in " + (end - t2) + " ms. " + refined.size() + " refined peaks returned.");
					//					System.out.println(ft + ": Detection done in " + (end - start) + " ms. ");

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

	public static final <T extends NumericType<T> & NativeType<T>> double measure(final ImgPlus<T> img, final int numThreads, final double minPeakValue) {

		final int timeDim = img.dimensionIndex(Axes.TIME);
		final int cDim = img.dimensionIndex(Axes.CHANNEL);
		final ExecutorService ex = Executors.newFixedThreadPool(numThreads);

		final long start = System.currentTimeMillis();

		for (int t = 0; t < img.dimension(timeDim); t++) {

			final int ft = t;

			final Runnable command = new Runnable() {
				@SuppressWarnings("unused")
				@Override
				public void run() {

					final HyperSliceImgPlus<T> frame = new HyperSliceImgPlus<T>(new HyperSliceImgPlus<T>(img, timeDim, ft), cDim, 0);

					final long start = System.currentTimeMillis();
					final FloatType type = new FloatType();
					final RandomAccessibleInterval<FloatType> dog = Util.getArrayOrCellImgFactory(frame, type).create(frame, type);
					final RandomAccessibleInterval<FloatType> dog2 = Util.getArrayOrCellImgFactory(frame, type).create(frame, type);

					final double[] pixelSize = TMUtils.getSpatialCalibration(img);

					final double radius = 2;
					final double sigma1 = radius / Math.sqrt(frame.numDimensions()) * 0.9;
					final double sigma2 = radius / Math.sqrt(frame.numDimensions()) * 1.1;
					final double[][] sigmas = DifferenceOfGaussian.computeSigmas(0.5, 2, pixelSize, sigma1, sigma2);
					try {
						Gauss3.gauss(sigmas[1], Views.extendMirrorSingle(frame), dog2, 1);
						Gauss3.gauss(sigmas[0], Views.extendMirrorSingle(frame), dog, 1);
					} catch (final IncompatibleTypeException e) {
						e.printStackTrace();
					}

					final IterableInterval<FloatType> dogIterable = Views.iterable(dog);
					final IterableInterval<FloatType> tmpIterable = Views.iterable(dog2);
					final Cursor<FloatType> dogCursor = dogIterable.cursor();
					final Cursor<FloatType> tmpCursor = tmpIterable.cursor();
					while (dogCursor.hasNext())
						dogCursor.next().sub(tmpCursor.next());

					final long t1 = System.currentTimeMillis();
					//					System.out.println(ft + ": DoG computed in " + (t1 - start) + " ms.");

					final FloatType val = new FloatType();
					val.setReal(minPeakValue * (sigma2 / sigma1 - 1.0));
					final IntervalView<FloatType> dogWithBorder = Views.interval(Views.extendZero(dog), Intervals.expand(dog, 1));
					final LocalNeighborhoodCheck<Point, FloatType> localNeighborhoodCheck = new LocalExtrema.MaximumCheck<FloatType>(val);
					final ArrayList<Point> peaks = LocalExtrema.findLocalExtrema(dogWithBorder, localNeighborhoodCheck, 1);
					final long t2 = System.currentTimeMillis();
					//					System.out.println(ft + ": Maxima found in " + (t2 - t1) + " ms. " + peaks.size() + " maxima found.");

					final SubpixelLocalization<Point, FloatType> spl = new SubpixelLocalization<Point, FloatType>(dog.numDimensions());
					spl.setNumThreads(1);
					spl.setReturnInvalidPeaks(true);
					spl.setCanMoveOutside(true);
					spl.setAllowMaximaTolerance(true);
					spl.setMaxNumMoves(10);
					final ArrayList<RefinedPeak<Point>> refined = spl.process(peaks, dogWithBorder, dog);
					final long end = System.currentTimeMillis();
					//					System.out.println(ft + ": Sup-pixel localization done in " + (end - t2) + " ms. " + refined.size() + " refined peaks returned.");
					//					System.out.println(ft + ": Detection done in " + (end - start) + " ms. ");


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