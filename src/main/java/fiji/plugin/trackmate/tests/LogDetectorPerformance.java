package fiji.plugin.trackmate.tests;


import java.util.Random;

import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import fiji.plugin.trackmate.detection.LogDetector;
import fiji.plugin.trackmate.detection.OldLogDetector;

public class LogDetectorPerformance {

	public static void main(final String[] args) throws IncompatibleTypeException {

		final int nwarmups = 5;
		final int ntests = 20;
		final int nspots = 200;
		final double rad = 5;
		final ArrayImg<UnsignedShortType, ShortArray> img = ArrayImgs.unsignedShorts(512, 512);
		final ArrayRandomAccess<UnsignedShortType> ra = img.randomAccess();
		final Random ran = new Random();
		for (int i = 0; i < nspots; i++) {
			ra.setPosition(ran.nextInt((int) img.dimension(0)), 0);
			ra.setPosition(ran.nextInt((int) img.dimension(1)), 1);
			ra.get().set(5000);
		}

		final RandomAccessible<UnsignedShortType> source = Views.extendMirrorSingle(img);
		Gauss3.gauss(rad / Math.sqrt(2), source, img);
		final ImgPlus<UnsignedShortType> imgplus = new ImgPlus<UnsignedShortType>(img);


		{
			System.out.println("Old implementation: ");
			for (int i = 0; i < nwarmups; i++) {
				execTestWOld(imgplus, rad);
			}
			final long start = System.currentTimeMillis();
			for (int i = 0; i < ntests; i++) {
				execTestWOld(imgplus, rad);
			}
			final long end = System.currentTimeMillis();
			System.out.println("  Image detection done in " + ((double) (end - start) / ntests) + " ms per run.");
		}


		{
			System.out.println("New implementation: ");
			for (int i = 0; i < nwarmups; i++) {
				execTest(imgplus, rad);
			}
			final long start = System.currentTimeMillis();
			for (int i = 0; i < ntests; i++) {
				execTest(imgplus, rad);
			}
			final long end = System.currentTimeMillis();
			System.out.println("  Image detection done in " + ((double) (end - start) / ntests) + " ms per run.");
		}

	}

	private static final void execTest(final ImgPlus<UnsignedShortType> imgplus, final double rad) {
		final LogDetector<UnsignedShortType> detector = new LogDetector<UnsignedShortType>(imgplus, rad, 0, false, false);
		detector.setNumThreads(1);
		if (!detector.checkInput() || !detector.process()) {
			System.out.println(detector.getErrorMessage());
			return;
		}
		detector.getResult();
	}

	private static final void execTestWOld(final ImgPlus<UnsignedShortType> imgplus, final double rad) {
		final OldLogDetector<UnsignedShortType> detector = new OldLogDetector<UnsignedShortType>(imgplus, rad, 0, false, false);
		detector.setNumThreads(1);
		if (!detector.checkInput() || !detector.process()) {
			System.out.println(detector.getErrorMessage());
			return;
		}
		detector.getResult();
	}

}
