package fiji.plugin.trackmate.detection;

import java.util.ArrayList;

import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.dog.DogDetection;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.TMUtils;

public class DogDetector <T extends RealType<T>  & NativeType<T>> extends LogDetector<T> {

	/*
	 * CONSTANTS
	 */

	public final static String BASE_ERROR_MESSAGE = "DogDetector: ";

	/*
	 * CONSTRUCTOR
	 */

	public DogDetector(final ImgPlus<T> img, final double radius, final double threshold, final boolean doSubPixelLocalization, final boolean doMedianFilter) {
		super(img, radius, threshold, doSubPixelLocalization, doMedianFilter);
		this.baseErrorMessage = BASE_ERROR_MESSAGE;
	}


	/*
	 * METHODS
	 */


	@Override
	public boolean process() {

		final long start = System.currentTimeMillis();

		/*
		 * Copy to float otherwise it fails.
		 */

		ImgFactory<FloatType> factory = null;
		try {
			factory = img.factory().imgFactory(new FloatType());
		} catch (final IncompatibleTypeException e) {
			errorMessage = baseErrorMessage + "Failed creating float image factory: " + e.getMessage();
			return false;
		}
		Img<FloatType> floatImg = copyToFloatImg(img, factory);

		// Deal with median filter:
		if (doMedianFilter) {
			floatImg = applyMedianFilter(floatImg);
			if (null == floatImg) {
				return false;
			}
		}

		final double s1 = 0.95 * radius / Math.sqrt(img.numDimensions());
		final double s2 = 1.05 * radius / Math.sqrt(img.numDimensions());
		final double[] calibration = TMUtils.getSpatialCalibration(img);

		final Dd<FloatType> dog = new Dd<FloatType>(Views.extendMirrorSingle(floatImg), floatImg, calibration, s2, s1, DogDetection.ExtremaType.MAXIMA, -threshold);
		dog.setNumThreads(numThreads);

		final ArrayList<RefinedPeak<Point>> peaks;
		if (doSubPixelLocalization) {
			peaks = dog.getSubpixelPeaks();
		} else {
			final ArrayList<Point> sp = dog.getPeaks();
			peaks = new ArrayList<RefinedPeak<Point>>(sp.size());
			final RandomAccess<FloatType> ra = dog.getDogImg().randomAccess();
			for (final Point point : sp) {
				ra.setPosition(point);
				peaks.add(new RefinedPeak<Point>(point, point, ra.get().getRealDouble(), true));
			}
		}

		spots = new ArrayList<Spot>(peaks.size());
		for (final RefinedPeak<Point> peak : peaks) {
			final double[] coords = new double[3];
			peak.localize(coords);
			for (int d = 0; d < coords.length; d++) {
				coords[d] *= calibration[d];
			}
			final Spot spot = new Spot(coords);
			spot.putFeature(Spot.QUALITY, Double.valueOf(peak.getValue()));
			spot.putFeature(Spot.RADIUS, radius);
			spots.add(spot);
		}

		// Prune overlapping spots
//		spots = TMUtils.suppressSpots(spots, Spot.QUALITY);

		final long end = System.currentTimeMillis();
		processingTime = end - start;

		return true;
	}

	private static final class Dd<T extends RealType<T> & NativeType<T>> extends DogDetection<T> {

		public Dd(final RandomAccessible<T> input, final Interval interval, final double[] calibration, final double sigma1, final double sigma2, final ExtremaType extremaType, final double minPeakValue) {
			super(input, interval, calibration, sigma1, sigma2, extremaType, minPeakValue);
		}

		private RandomAccessibleInterval<T> getDogImg() {
			return dogImg;
		}
	}
}
