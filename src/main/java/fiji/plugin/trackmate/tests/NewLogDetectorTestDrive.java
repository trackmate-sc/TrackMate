package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.detection.DetectionUtils;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import io.scif.img.ImgIOException;

import java.io.File;
import java.util.ArrayList;

import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.fft2.FFTConvolution;
import net.imglib2.algorithm.localextrema.LocalExtrema;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.meta.Axes;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class NewLogDetectorTestDrive {


	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public static void main(final String[] args) throws ImgIOException, IncompatibleTypeException {

		// final File file = new File(
		// "/Users/JeanYves/Desktop/Data/FakeTracks.tif" );
		// final File file = new
		// File("/Users/tinevez/Projects/JYTinevez/ISBI/ChallengeData/VIRUS/VIRUS snr 7 density mid.tif");
		// final File file = new File(
		// "/Users/JeanYves/Desktop/Data/FakeTracks.tif" );
		final File file = new File( "/Users/JeanYves/Documents/Projects/ISBI/VIRUS/VIRUS snr 4 density mid.tif" );

		final ImgPlus img = ImagePlusAdapter.wrapImgPlus( new ImagePlus( file.getAbsolutePath() ) );

		final int timeDim = img.dimensionIndex(Axes.TIME);
		final IntervalView frame = Views.hyperSlice( img, timeDim, 0 );

		ImageJ.main( args );
		final ImagePlus imp = ImageJFunctions.show(frame);
		System.out.println("Source has " + img.numDimensions() + " dims. Hyperslice has " + frame.numDimensions() + " dims.");

		final double[] calibration = new double[3];
		calibration[0] = 1; // img.averageScale(0);
		calibration[1] = 1; //img.averageScale(1);
		calibration[2] = 1; //img.averageScale(2);
		final long start = System.currentTimeMillis();

		final Img< FloatType > kernel = DetectionUtils.createLoGKernel( 4, frame.numDimensions(), calibration );
		final RandomAccessibleInterval<FloatType> output = new ArrayImgFactory().create(frame, new FloatType());
		FFTConvolution.convolve(Views.extendZero(frame), frame, Views.extendZero(kernel), kernel, output, new ArrayImgFactory());

		final long t1 = System.currentTimeMillis();
		System.out.println( "Convolution done in " + ( t1 - start ) + " ms." );

		final ArrayList< Point > peaks = LocalExtrema.findLocalExtrema( output, new LocalExtrema.MaximumCheck( new FloatType( 1f ) ), 1 );

		final long end = System.currentTimeMillis();
		final int npoints = peaks.size();
		System.out.println( "Extrema finding done in " + ( end - t1 ) + " ms." );
		System.out.println( "Detection done in " + ( end - start ) + " ms. Found " + peaks.size() + " spots." );

		final float[] oy = new float[ npoints ];
		final float[] ox = new float[ npoints ];
		for ( int i = 0; i < ox.length; i++ )
		{
			ox[ i ] = peaks.get( i ).getFloatPosition( 0 );
			oy[ i ] = peaks.get( i ).getFloatPosition( 1 );
		}
		final PointRoi roi = new PointRoi( ox, oy, npoints );
		imp.setRoi( roi );

	}

}
