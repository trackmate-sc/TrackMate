package fiji.plugin.trackmate.detection;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.fft2.FFTConvolution;
import net.imglib2.algorithm.math.PickImagePeaks;
import net.imglib2.display.RealFloatConverter;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
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

	private final static Img<FloatType> laplacianKernel2D = createLaplacianKernel(2);
	private final static Img<FloatType> laplacianKernel3D = createLaplacianKernel(3);

	/** The image to segment. Will not modified. */
	protected ImgPlus<T> img;
	protected double radius;
	protected double threshold;
	protected boolean doSubPixelLocalization;
	protected boolean doMedianFilter;
	protected String baseErrorMessage;
	protected String errorMessage;
	/** The list of {@link Spot} that will be populated by this detector. */
	protected List<Spot> spots = new ArrayList<Spot>(); // because this implementation is fast to add elements at the end of the list
	/** The processing time in ms. */
	protected long processingTime;
	private int numThreads;

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
		Img<FloatType> floatImg = toFloatImg(img, factory);

		// Deal with median filter:
		if (doMedianFilter) {
			floatImg = applyMedianFilter(floatImg);
			if (null == floatImg) {
				return false;
			}
		}

		/*
		 * Build gaussian kernel.
		 */

		final double sigma = radius / Math.sqrt(img.numDimensions()); // optimal sigma for LoG approach and dimensionality
		// Turn it in pixel coordinates
		final double[] calibration = TMUtils.getSpatialCalibration(img);
		final double[] sigmas = new double[img.numDimensions()];
		for (int i = 0; i < sigmas.length; i++) {
			sigmas[i] = sigma / calibration[i];
		}

		final Img<FloatType> gaussianKernel = createGaussianKernel(factory, sigmas);
		//		ImageJFunctions.showFloat(floatImg.copy(), "Source"); // DEBUG
		//		ImageJFunctions.showFloat(gaussianKernel, "Gaussian kernel"); // DEBUG

		final FFTConvolution<FloatType> fftconv1 = new FFTConvolution<FloatType>(floatImg, gaussianKernel);
		fftconv1.run();
		//		ImageJFunctions.showFloat(floatImg.copy(), "Conv by Gaussian kernel"); // DEBUG

		final Img<FloatType> laplacianKernel;
		switch (img.numDimensions()) {
			case 2:
				laplacianKernel = laplacianKernel2D;
				break;
			case 3:
				laplacianKernel = laplacianKernel3D;
				break;
			default:
				errorMessage = baseErrorMessage + "Cannot deal with dimensionality " + img.numDimensions() + "D for single frames.";
				return false;
		}
		//		ImageJFunctions.showFloat(laplacianKernel, "Laplacian kernel"); // DEBUG
		final FFTConvolution<FloatType> fftconv2 = new FFTConvolution<FloatType>(floatImg, laplacianKernel);
		fftconv2.run();
		//		ImageJFunctions.showFloat(floatImg.copy(), "Conv by Log"); // DEBUG

		final PickImagePeaks<FloatType> peakPicker = new PickImagePeaks<FloatType>(floatImg);
		final double[] suppressionRadiuses = new double[img.numDimensions()];
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
		spots.clear();
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

		final long end = System.currentTimeMillis();
		processingTime = end - start;

		return true;
	}


	/*
	 * PRIVATE METHODS
	 */


	private static final Img<FloatType> createLaplacianKernel(final int numDim) {
		Img<FloatType> laplacianKernel = null;
		final ArrayImgFactory<FloatType> factory = new ArrayImgFactory<FloatType>();
		if (numDim == 3) {
			final float laplacianArray[][][] = new float[][][]{ { {0,-1/18,0},{-1/18,-1/18,-1/18},{0,-1/18,0} }, { {-1/18,-1/18,-1/18}, {-1/18,1,-1/18}, {-1/18,-1/18,-1/18} }, { {0,-1/18,0},{-1/18,-1/18,-1/18},{0,-1/18,0} } }; // laplace kernel found here: http://en.wikipedia.org/wiki/Discrete_Laplace_operator
			laplacianKernel = factory.create(new int[] { 3, 3, 3 }, new FloatType());
			quickKernel3D(laplacianArray, laplacianKernel);
		} else if (numDim == 2) {
			final float laplacianArray[][] = new float[][]{ {-1/8,-1/8,-1/8},{-1/8,1,-1/8},{-1/8,-1/8,-1/8} }; // laplace kernel found here: http://en.wikipedia.org/wiki/Discrete_Laplace_operator
			laplacianKernel = factory.create(new int[] { 3, 3 }, new FloatType());
			quickKernel2D(laplacianArray, laplacianKernel);
		}
		return laplacianKernel;
	}

	/**
	 * Apply a simple 3x3 median filter to the target image.
	 */
	protected Img<FloatType> applyMedianFilter(final Img<FloatType> image) {
		final MedianFilter3x3<FloatType> medFilt = new MedianFilter3x3<FloatType>(image);
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

	private static final <T extends RealType<T>> Img<FloatType> toFloatImg(final Img<T> input, final ImgFactory<FloatType> factory)
 {
		final Img<FloatType> output = factory.create(input, new FloatType());
		final Cursor<T> in = input.cursor();
		final Cursor<FloatType> out = output.cursor();
		final RealFloatConverter<T> c = new RealFloatConverter<T>();

		while (in.hasNext())
		{
			in.fwd();
			out.fwd();
			c.convert(in.get(), out.get());
		}
		return output;
	}

	private static final Img<FloatType> createGaussianKernel(final ImgFactory<FloatType> factory, final double[] sigmas) {
		final int numDimensions = sigmas.length;

		final int[] imageSize = new int[numDimensions];
		final double[][] kernel = new double[numDimensions][];

		for (int d = 0; d < numDimensions; ++d) {
			kernel[d] = Util.createGaussianKernel1DDouble(sigmas[d], true);
			imageSize[d] = kernel[d].length;
		}

		final Img<FloatType> kernelImg = factory.create(imageSize, new FloatType());

		final Cursor<FloatType> cursor = kernelImg.localizingCursor();
		final int[] position = new int[numDimensions];

		while (cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(position);

			float value = 1;

			for (int d = 0; d < numDimensions; ++d)
				value *= kernel[d][position[d]];

			cursor.get().set(value);
		}

		return kernelImg;
	}

	private static void quickKernel3D(final float[][][] vals, final Img<FloatType> kern) {
		final RandomAccess<FloatType> cursor = kern.randomAccess();
		final int[] pos = new int[3];

		for (int i = 0; i < vals.length; ++i)
			for (int j = 0; j < vals[i].length; ++j)
				for (int k = 0; k < vals[j].length; ++k) {
					pos[0] = i;
					pos[1] = j;
					pos[2] = k;
					cursor.setPosition(pos);
					cursor.get().set(vals[i][j][k]);
				}
	}

	private static final void quickKernel2D(final float[][] vals, final Img<FloatType> kern) {
		final RandomAccess<FloatType> cursor = kern.randomAccess();
		final int[] pos = new int[2];

		for (int i = 0; i < vals.length; ++i)
			for (int j = 0; j < vals[i].length; ++j) {
				pos[0] = i;
				pos[1] = j;
				cursor.setPosition(pos);
				//				System.out.println(vals[i][j]);// DEBUG
				cursor.get().setReal(vals[i][j]);
			}
	}

}
