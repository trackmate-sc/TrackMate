package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.detection.DetectionUtils;
import ij.ImageJ;
import ij.ImagePlus;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;

import java.io.File;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.fft2.FFTConvolution;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.meta.Axes;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.view.HyperSliceImgPlus;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class NewLogDetectorTestDrive {


	private static RandomAccessibleInterval df;

	public static void main(final String[] args) throws ImgIOException, IncompatibleTypeException {

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

		final Img<FloatType> kernel = DetectionUtils.createLoGKernel(4, new HyperSliceImgPlus(new HyperSliceImgPlus(img, timeDim, 0), cDim, 0));
		ImageJFunctions.show(kernel);

		final RandomAccessibleInterval<FloatType> output = new ArrayImgFactory().create(frame, new FloatType());
		FFTConvolution.convolve(Views.extendZero(frame), frame, Views.extendZero(kernel), kernel, output, new ArrayImgFactory());
		//		final FFTConvolution fftConvolution = new FFTConvolution(frame, kernel, new ArrayImgFactory());
		//		fftConvolution.run();

		ImageJFunctions.show(output);

		final long end = System.currentTimeMillis();
		//		final int npoints = peaks.size();
		System.out.println("Detection done in " + (end - start) + " ms. Found " + 2 + " spots.");
		//
		//		final float[] oy = new float[npoints];
		//		final float[] ox = new float[npoints];
		//		for (int i = 0; i < ox.length; i++) {
		//			ox[i] = peaks.get(i).getFloatPosition(0);
		//			oy[i] = peaks.get(i).getFloatPosition(1);
		//		}
		//		final PointRoi roi = new PointRoi(ox, oy, npoints);
		//		imp.setRoi(roi);

	}

}
