package fiji.plugin.trackmate.detection;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.RandomAccess;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.fft2.FFTConvolution;
import net.imglib2.algorithm.math.PickImagePeaks;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.subpixel.QuadraticSubpixelLocalization;
import fiji.plugin.trackmate.detection.subpixel.SubPixelLocalization;
import fiji.plugin.trackmate.detection.subpixel.SubPixelLocalization.LocationType;
import fiji.plugin.trackmate.detection.util.MedianFilter3x3;
import fiji.plugin.trackmate.util.TMUtils;

public class LogDetector <T extends RealType<T>  & NativeType<T>> implements SpotDetector<T>, MultiThreaded {

	/*
	 * FIELDS
	 */

	private final static String BASE_ERROR_MESSAGE = "LogDetector: ";

	/** The image to segment. Will not modified. */
	protected ImgPlus<T> img;
	protected double radius;
	protected double threshold;
	protected boolean doSubPixelLocalization;
	protected boolean doMedianFilter;
	protected String baseErrorMessage;
	protected String errorMessage;
	/** The list of {@link Spot} that will be populated by this detector. */
	protected List<Spot> spots = new ArrayList<Spot>();
	/** The processing time in ms. */
	protected long processingTime;
	protected int numThreads;

	/*
	 * CONSTRUCTORS
	 */

	public LogDetector(final ImgPlus<T> img, final double radius, final double threshold, final boolean doSubPixelLocalization, final boolean doMedianFilter) {
		this.img = img;
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
	public boolean checkInput() {
		if (null == img) {
			errorMessage = baseErrorMessage + "Image is null.";
			return false;
		}
		if (!(img.numDimensions() == 2 || img.numDimensions() == 3)) {
			errorMessage = baseErrorMessage + "Image must be 2D or 3D, got " + img.numDimensions() +"D.";
			return false;
		}
		return true;
	};


	@Override
	public boolean process() {

		final long start = System.currentTimeMillis();


		/*
		 * Copy to float for convolution.
		 */

		ImgFactory<FloatType> factory = null;
		try {
			factory = img.factory().imgFactory(new FloatType());
		} catch (final IncompatibleTypeException e) {
			errorMessage = baseErrorMessage + "Failed creating float image factory: " + e.getMessage();
			return false;
		}
		Img<FloatType> floatImg = DetectionUtils.copyToFloatImg(img, factory);

		// Deal with median filter:
		if (doMedianFilter) {
			floatImg = applyMedianFilter(floatImg);
			if (null == floatImg) {
				return false;
			}
		}

		final Img<FloatType> kernel = DetectionUtils.createLoGKernel(radius, img);
		final FFTConvolution<FloatType> fftconv = new FFTConvolution<FloatType>(floatImg, kernel);
		fftconv.run();

		final PickImagePeaks<FloatType> peakPicker = new PickImagePeaks<FloatType>(floatImg);
		final double[] suppressionRadiuses = new double[img.numDimensions()];
		final double[] calibration = TMUtils.getSpatialCalibration(img);
		for (int i = 0; i < img.numDimensions(); i++)
			suppressionRadiuses[i] = radius / calibration [i];
		peakPicker.setSuppression(suppressionRadiuses); // in pixels
		peakPicker.setAllowBorderPeak(true);

		if (!peakPicker.checkInput() || !peakPicker.process()) {
			errorMessage = baseErrorMessage +"Could not run the peak picker algorithm:\n" + peakPicker.getErrorMessage();
			return false;
		}

		// Get peaks location and values
		final ArrayList<long[]> centers = peakPicker.getPeakList();
		final RandomAccess<FloatType> cursor = floatImg.randomAccess();
		// Prune values lower than threshold
		final List<SubPixelLocalization<FloatType>> peaks = new ArrayList<SubPixelLocalization<FloatType>>();
		final List<FloatType> pruned_values = new ArrayList<FloatType>();
		final LocationType specialPoint = LocationType.MAX;
		for (int i = 0; i < centers.size(); i++) {
			final long[] center = centers.get(i);
			cursor.setPosition(center);
			final FloatType value = cursor.get().copy();
			if (value.getRealDouble() < threshold) {
				break; // because peaks are sorted, we can exit loop here
			}
			final SubPixelLocalization<FloatType> peak = new SubPixelLocalization<FloatType>(center, value, specialPoint);
			peaks.add(peak);
			pruned_values.add(value);
		}

		// Do sub-pixel localization
		if (doSubPixelLocalization && !peaks.isEmpty()) {
			// Create localizer and apply it to the list. The list object will be updated
			final QuadraticSubpixelLocalization<FloatType> locator = new QuadraticSubpixelLocalization<FloatType>(floatImg, peaks);
			locator.setNumThreads(numThreads);
			locator.setCanMoveOutside(true);
			if ( !locator.checkInput() || !locator.process() )	{
				errorMessage = baseErrorMessage + locator.getErrorMessage();
				return false;
			}
		}

		// Create spots
		spots = new ArrayList<Spot>(peaks.size());
		for (int j = 0; j < peaks.size(); j++) {

			final SubPixelLocalization<FloatType> peak = peaks.get(j);
			final double[] coords = new double[3];
			for (int i = 0; i < img.numDimensions(); i++) {
				coords[i] = peak.getDoublePosition(i) * calibration[i];
			}
			final Spot spot = new Spot(coords);
			spot.putFeature(Spot.QUALITY, Double.valueOf(peak.getValue().get()));
			spot.putFeature(Spot.RADIUS, Double.valueOf(radius));
			spots.add(spot);
		}
		// Prune overlapping spots
		spots = TMUtils.suppressSpots(spots, Spot.QUALITY);

		final long end = System.currentTimeMillis();
		processingTime = end - start;

		return true;
	}

	/**
	 * Apply a simple 3x3 median filter to the target image.
	 */
	protected <R extends RealType<R>> Img<R> applyMedianFilter(final Img<R> image) {
		final MedianFilter3x3<R> medFilt = new MedianFilter3x3<R>(image);
		if (!medFilt.checkInput() || !medFilt.process()) {
			errorMessage = baseErrorMessage + "Failed in applying median filter";
			return null;
		}
		return medFilt.getResult();
	}

	@Override
	public List<Spot> getResult() {
		return spots;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage ;
	}


	@Override
	public long getProcessingTime() {
		return processingTime;
	}

	@Override
	public void setNumThreads() {
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void setNumThreads(final int numThreads) {
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads() {
		return numThreads;
	}
}
