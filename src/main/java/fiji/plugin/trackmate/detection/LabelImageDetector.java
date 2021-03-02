package fiji.plugin.trackmate.detection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import fiji.plugin.trackmate.Spot;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class LabelImageDetector< T extends RealType< T > & NativeType< T > > implements SpotDetector< T >
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

	/**
	 * If <code>true</code>, the contours will be smoothed and simplified.
	 */
	protected final boolean simplify;

	/*
	 * CONSTRUCTORS
	 */

	public LabelImageDetector(
			final RandomAccessible< T > input,
			final Interval interval,
			final double[] calibration,
			final boolean simplify )
	{
		this.input = input;
		this.interval = DetectionUtils.squeeze( interval );
		this.calibration = calibration;
		this.simplify = simplify;
	}

	@Override
	public boolean checkInput()
	{
		final T type = Util.getTypeFromInterval( Views.interval( input, interval ) );
		if ( type instanceof IntegerType )
			return true;

		return false;
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();
		final RandomAccessibleInterval< T > rai = Views.interval( input, interval );
		final T type = Util.getTypeFromInterval( rai );

		if ( type instanceof IntegerType )
		{
			processIntegerImg( ( RandomAccessibleInterval< IntegerType > ) Views.zeroMin( rai ) );
		}
		else
		{
			final ImgFactory< IntType > factory = Util.getArrayOrCellImgFactory( interval, new IntType() );
			final Img< IntType > img = factory.create( interval );
			LoopBuilder
					.setImages( Views.zeroMin( rai ), img )
					.multiThreaded( false )
					.forEachPixel( ( i, o ) -> o.setReal( i.getRealDouble() ) );
			processIntegerImg( img );
		}
		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	private < R extends IntegerType< R > > void processIntegerImg( final RandomAccessibleInterval< R > rai )
	{
		// Get all labels.
		final AtomicInteger max = new AtomicInteger( 0 );
		Views.iterable( rai ).forEach( p -> {
			final int val = p.getInteger();
			if ( val != 0 && val > max.get() )
				max.set( val );
		} );
		final List< Integer > indices = new ArrayList<>( max.get() );
		for ( int i = 0; i < max.get(); i++ )
			indices.add( Integer.valueOf( i + 1 ) );

		final ImgLabeling< Integer, R > labeling = ImgLabeling.fromImageAndLabels( rai, indices );
		if ( input.numDimensions() == 2 )
			spots = MaskUtils.fromLabelingWithROI( labeling, interval, calibration, simplify, null );
		else
			spots = MaskUtils.fromLabeling( labeling, interval, calibration );
	}

	@Override
	public List< Spot > getResult()
	{
		return spots;
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
}
