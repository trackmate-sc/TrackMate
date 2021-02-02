package fiji.plugin.trackmate.detection;

import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.Spot;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ThresholdDetector< T extends RealType< T > & NativeType< T > > implements SpotDetector< T >, MultiThreaded
{

	private final static String BASE_ERROR_MESSAGE = "ThresholdDetector: ";

	/*
	 * FIELDS
	 */

	/** The mask. */
	protected RandomAccessible< T > input;

	protected String baseErrorMessage = BASE_ERROR_MESSAGE;

	protected String errorMessage;

	/** The list of {@link Spot} that will be populated by this detector. */
	protected List< Spot > spots = new ArrayList<>();

	/** The processing time in ms. */
	protected long processingTime;

	protected int numThreads;

	protected final Interval interval;

	protected final double[] calibration;

	protected final double threshold;

	/**
	 * If <code>true</code>, the contours will be smoothed and simplified.
	 */
	protected final boolean simplify;

	/*
	 * CONSTRUCTORS
	 */

	public ThresholdDetector(
			final RandomAccessible< T > input,
			final Interval interval,
			final double[] calibration,
			final double threshold,
			final boolean simplify )
	{
		this.input = input;
		this.interval = DetectionUtils.squeeze( interval );
		this.calibration = calibration;
		this.threshold = threshold;
		this.simplify = simplify;
	}

	@Override
	public List< Spot > getResult()
	{
		return spots;
	}

	@Override
	public boolean checkInput()
	{
		if ( null == input )
		{
			errorMessage = baseErrorMessage + "Image is null.";
			return false;
		}
		if ( input.numDimensions() > 3 || input.numDimensions() < 2 )
		{
			errorMessage = baseErrorMessage + "Image must be 2D or 3D, got " + input.numDimensions() + "D.";
			return false;
		}
		return true;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();
		if ( input.numDimensions() == 2 )
		{
			/*
			 * 2D: we compute and store the contour.
			 */
			spots = MaskUtils.fromThresholdWithROI( input, interval, calibration, threshold, simplify, numThreads, null );

		}
		else if ( input.numDimensions() == 3 )
		{
			/*
			 * 3D: We create spots of the same volume that of the region.
			 */
			spots = MaskUtils.fromThreshold( input, interval, calibration, threshold, numThreads );
		}
		else
		{
			errorMessage = baseErrorMessage + "Required a 2D or 3D input, got " + input.numDimensions() + "D.";
			return false;
		}

		final long end = System.currentTimeMillis();
		this.processingTime = end - start;

		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public void setNumThreads()
	{
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void setNumThreads( final int numThreads )
	{
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}
}
