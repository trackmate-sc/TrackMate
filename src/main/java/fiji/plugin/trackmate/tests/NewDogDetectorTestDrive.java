package fiji.plugin.trackmate.tests;

import ij.ImageJ;
import ij.ImagePlus;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;

import java.io.File;
import java.util.ArrayList;

import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.meta.Axes;
import net.imglib2.meta.ImgPlus;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class NewDogDetectorTestDrive {


	private static RandomAccessibleInterval df;

	public static void main(final String[] args) throws ImgIOException {

		ImageJ.main(args);
		final File file = new File("/Users/tinevez/Projects/JYTinevez/ISBI/ChallengeData/VIRUS/VIRUS snr 7 density mid.tif");
		final ImgPlus img = new ImgOpener().openImg(file.getAbsolutePath());

		final int timeDim = img.dimensionIndex(Axes.TIME);
		final int cDim = img.dimensionIndex(Axes.CHANNEL);

		final IntervalView frame = Views.hyperSlice(Views.hyperSlice(img, timeDim, 0), cDim, 0);

		final ImagePlus imp = ImageJFunctions.show(frame);
		System.out.println("Source has " + img.numDimensions() + " dims. Hyperslice has " + frame.numDimensions() + " dims.");

		final double[] calibration = new double[3];
		calibration[0] = 1; // img.averageScale(0);
		calibration[1] = 1; //img.averageScale(1);
		calibration[2] = 1; //img.averageScale(2);
		final long start = System.currentTimeMillis();
		final DogDetection dog = new DogDetection(Views.extendMirrorSingle(frame), frame, calibration, 0.11, 2.2, DogDetection.ExtremaType.MAXIMA, 2000) {
			@Override
			public ArrayList getPeaks() {
				final ArrayList peaks = super.getPeaks();
				df = dogImg;
				return peaks;
			}
		};
		dog.setNumThreads(1);
		final ArrayList<Point> peaks = dog.getPeaks();
		ImageJFunctions.show(df);
		final long end = System.currentTimeMillis();
		final int npoints = peaks.size();
		System.out.println("Detection done in " + (end - start) + " ms. Found " + npoints + " spots.");

		final float[] oy = new float[npoints];
		final float[] ox = new float[npoints];
		for (int i = 0; i < ox.length; i++) {
			ox[i] = peaks.get(i).getFloatPosition(0);
			oy[i] = peaks.get(i).getFloatPosition(1);
		}
		//		final PointRoi roi = new PointRoi(ox, oy, npoints);
		//		imp.setRoi(roi);

	}

}
