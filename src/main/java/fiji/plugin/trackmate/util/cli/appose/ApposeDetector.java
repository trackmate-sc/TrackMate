package fiji.plugin.trackmate.util.cli.appose;

import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.ThresholdDetectorFactory.KEY_SIMPLIFY_CONTOURS;
import static fiji.plugin.trackmate.detection.ThresholdDetectorFactory.KEY_SMOOTHING_SCALE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apposed.appose.Appose;
import org.apposed.appose.Environment;
import org.apposed.appose.NDArray;
import org.apposed.appose.Service;
import org.apposed.appose.Service.Task;
import org.apposed.appose.Service.TaskStatus;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.LabelImageDetectorFactory;
import fiji.plugin.trackmate.detection.SpotGlobalDetector;
import fiji.plugin.trackmate.util.TMUtils;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.appose.NDArrays;
import net.imglib2.appose.ShmImg;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class ApposeDetector< T extends RealType< T > & NativeType< T > > implements SpotGlobalDetector< T >, MultiThreaded
{

	private final ApposeConfigurator configurator;

	private final ImgPlus< T > img;

	private final Interval interval;

	/** The processing time in ms. */
	private long processingTime;

	private String errorMessage;

	private SpotCollection spots;

	private final boolean simplifyContour;

	private final double smoothingScale;

	private Logger logger = Logger.VOID_LOGGER;

	private int numThreads;

	public ApposeDetector(
			final ApposeConfigurator configurator,
			final ImgPlus< T > img,
			final Interval interval,
			final boolean simplifyContour,
			final double smoothingScale )
	{
		this.configurator = configurator;
		this.img = img;
		this.interval = interval;
		this.simplifyContour = simplifyContour;
		this.smoothingScale = smoothingScale;
		setNumThreads();
	}

	@Override
	public SpotCollection getResult()
	{
		return spots;
	}

	@Override
	public boolean checkInput()
	{
		if ( null == img )
		{
			errorMessage = getBaseErrorMessage() + "Image is null.";
			return false;
		}
		return true;
	}

	@Override
	public boolean process()
	{
		// Print os and arch info
		logger.log( "Running ApposeDetector on: " );
		logger.log( configurator.getClass().getSimpleName() + '\n', Logger.GREEN_COLOR );
		logger.log( "This machine: " + System.getProperty( "os.name" ) + " / " + System.getProperty( "os.arch" ) + '\n' );

		// The environment spec (e.g. environment.yml or pixi.toml).
		final String envContent = configurator.getEnvConfig();
		logger.log( "Env file content:\n" );
		logger.log( indent( envContent ) + "\n\n", Logger.BLUE_COLOR );

		// Get the script
		final String script = configurator.makeScript();
		logger.log( "Python script:\n" );
		logger.log( indent( script ) + "\n\n", Logger.BLUE_COLOR );

		// Regrow the interval to include all channels if needed.
		final Interval matchedInterval;
		final int cAxis = img.dimensionIndex( Axes.CHANNEL );
		if ( cAxis >= 0 )
		{
			final long[] max = new long[ img.numDimensions() ];
			final long[] min = new long[ img.numDimensions() ];
			// Dimensions before cAxis are unchanged.
			for ( int d = 0; d < cAxis; d++ )
			{
				min[ d ] = interval.min( d );
				max[ d ] = interval.max( d );
			}
			// cAxis spans all channels.
			min[ cAxis ] = 0;
			max[ cAxis ] = img.dimension( cAxis ) - 1;
			// Dimensions after cAxis are unchanged.
			for ( int d = cAxis + 1; d < img.numDimensions(); d++ )
			{
				min[ d ] = interval.min( d - 1 );
				max[ d ] = interval.max( d - 1 );
			}
			matchedInterval = new FinalInterval( min, max );
		}
		else
		{
			matchedInterval = interval;
		}

		// Get info on the dimensions we will get back.
		final int tAxis = img.dimensionIndex( Axes.TIME );
		final long minT = ( tAxis < 0 ) ? 0 : interval.min( interval.numDimensions() - 1 );
		final double frameInterval = ( tAxis < 0 ) ? 1. : img.averageScale( tAxis );
		final long nframes = ( tAxis >= 0 ) ? matchedInterval.dimension( tAxis ) : 1;
		final int zAxis = img.dimensionIndex( Axes.Z );
		final long nslices = ( zAxis >= 0 ) ? matchedInterval.dimension( zAxis ) : 1;

		// Wrap input image as NDArray. CAREFUL! The dimensions will be flipped
		final NDArray ndArray = NDArrays.asNDArray( Views.interval( img, matchedInterval ) );

		// Get axes order.
		final Map< String, Integer > axesOrder = new HashMap<>();
		for ( int d = 0; d < img.numDimensions(); d++ )
		{
			final int flippedD = img.numDimensions() - d - 1;
			axesOrder.put( img.axis( d ).type().getLabel(), flippedD );
		}

		// Copy the input to a shared memory image.
		final Map< String, Object > inputs = new HashMap<>();
		inputs.put( "image", ndArray );
		inputs.put( "axes", axesOrder );

		// Create or retrieve the environment.
		Environment env;
		try
		{
			env = Appose
					.pixi()
					.content( envContent )
					.logDebug()
					.build();
		}
		catch ( final IOException e )
		{
			errorMessage = getBaseErrorMessage() + "Failed to create Appose environment: " + e.getMessage() + '\n';
			e.printStackTrace();
			return false;
		}

		try (Service python = env.python())
		{
			final Task task = python.task( script, inputs ).listen( e -> {
				final String msg = e.message;
				if ( msg != null )
					logger.log( msg + "\n" );
				else
					logger.setProgress( ( double ) e.current / e.maximum );
			} );
			logger.setStatus( "Starting task" );
			final long start = System.currentTimeMillis();
			task.start();
			task.waitFor();

			// Verify that it worked.
			if ( task.status != TaskStatus.COMPLETE )
			{
				errorMessage = getBaseErrorMessage() + "Python script failed with error: " + task.error + '\n';
				return false;
			}

			// Unwrap output.
			// TODO: What if this outputs is not there? Make this class specific
			// to masks?
			final NDArray maskArr = ( NDArray ) task.outputs.get( "masks" );
			final Img< T > maskImg = new ShmImg<>( maskArr );
			final ImagePlus maskImp = ImageJFunctions.wrap( maskImg, "Masks_" + img.getName() );

			// Copy calibration.
			final double[] calibration = TMUtils.getSpatialCalibration( img );
			maskImp.getCalibration().pixelWidth = calibration[ 0 ];
			maskImp.getCalibration().pixelHeight = calibration[ 1 ];
			maskImp.getCalibration().pixelDepth = calibration[ 2 ];
			maskImp.setDimensions( 1, ( int ) nslices, ( int ) nframes );
			maskImp.setOpenAsHyperStack( true );

			// Run in the label detector.
			logger.log( "Converting masks to spots.\n" );
			final Settings labelImgSettings = new Settings( maskImp );
			final LabelImageDetectorFactory< ? > labeImageDetectorFactory = new LabelImageDetectorFactory<>();
			final Map< String, Object > detectorSettings = labeImageDetectorFactory.getDefaultSettings();
			detectorSettings.put( KEY_TARGET_CHANNEL, DEFAULT_TARGET_CHANNEL );
			detectorSettings.put( KEY_SIMPLIFY_CONTOURS, simplifyContour );
			detectorSettings.put( KEY_SMOOTHING_SCALE, smoothingScale );
			labelImgSettings.detectorFactory = labeImageDetectorFactory;
			labelImgSettings.detectorSettings = detectorSettings;

			final TrackMate labelImgTrackMate = new TrackMate( labelImgSettings );
			labelImgTrackMate.setNumThreads( getNumThreads() );
			if ( !labelImgTrackMate.execDetection() )
			{
				errorMessage = getBaseErrorMessage() + labelImgTrackMate.getErrorMessage();
				return false;
			}
			final SpotCollection tmpSpots = labelImgTrackMate.getModel().getSpots();

			/*
			 * Reposition spots with respect to the interval and time.
			 */
			final List< Spot > slist = new ArrayList<>();
			for ( final Spot spot : tmpSpots.iterable( false ) )
			{
				for ( int d = 0; d < interval.numDimensions() - 1; d++ )
				{
					final double pos = spot.getDoublePosition( d ) + interval.min( d ) * calibration[ d ];
					spot.putFeature( Spot.POSITION_FEATURES[ d ], Double.valueOf( pos ) );
				}
				// Shift in time.
				final int frame = spot.getFeature( Spot.FRAME ).intValue() + ( int ) minT;
				spot.putFeature( Spot.POSITION_T, frame * frameInterval );
				spot.putFeature( Spot.FRAME, Double.valueOf( frame ) );
				slist.add( spot );
			}
			spots = SpotCollection.fromCollection( slist );

			/*
			 * End.
			 */

			final long end = System.currentTimeMillis();
			this.processingTime = end - start;
			return true;
		}
		catch ( final InterruptedException | IOException e )
		{
			e.printStackTrace();
		}
		return false;
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

	private String getBaseErrorMessage()
	{
		return "[ApposeDetector" + configurator.getClass().getSimpleName() + "] ";
	}

	@Override
	public void setLogger( final Logger logger )
	{
		this.logger = logger;
	}

	// --- Multithreaded methods ---

	@Override
	public void setNumThreads()
	{
		this.numThreads = Runtime.getRuntime().availableProcessors() / 2;
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

	private static String indent( final String script )
	{
		final String[] split = script.split( "\n" );
		String out = "";
		for ( final String string : split )
			out += "    " + string + "\n";
		return out;
	}
}
