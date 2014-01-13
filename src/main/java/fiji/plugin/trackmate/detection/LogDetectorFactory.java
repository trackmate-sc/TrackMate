package fiji.plugin.trackmate.detection;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import fiji.plugin.trackmate.util.TMUtils;

public class LogDetectorFactory< T extends RealType< T > & NativeType< T >> implements SpotDetectorFactory< T >
{

	/*
	 * CONSTANTS
	 */

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "LOG_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "LoG detector";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>" + "This detector applies a LoG (Laplacian of Gaussian) filter <br>" + "to the image, with a sigma suited to the blob estimated size. <br>" + "Calculations are made in the Fourier space. The maxima in the <br>" + "filtered image are searched for, and maxima too close from each <br>" + "other are suppressed. A quadratic fitting scheme allows to do <br>" + "sub-pixel localization. " + "</html>";

	/*
	 * FIELDS
	 */

	/** The image to operate on. Multiple frames, single channel. */
	protected ImgPlus< T > img;

	protected Map< String, Object > settings;

	protected String errorMessage;

	/*
	 * METHODS
	 */

	@Override
	public void setTarget( final ImgPlus< T > img, final Map< String, Object > settings )
	{
		this.img = img;
		this.settings = settings;
	}

	@Override
	public SpotDetector< T > getDetector( final Interval interval, final int frame )
	{
		final double radius = ( Double ) settings.get( KEY_RADIUS );
		final double threshold = ( Double ) settings.get( KEY_THRESHOLD );
		final boolean doMedian = ( Boolean ) settings.get( KEY_DO_MEDIAN_FILTERING );
		final boolean doSubpixel = ( Boolean ) settings.get( KEY_DO_SUBPIXEL_LOCALIZATION );
		final double[] calibration = TMUtils.getSpatialCalibration( img );

		final int timeDim = TMUtils.findTAxisIndex( img );
		final RandomAccessible< T > imFrame;
		if ( timeDim < 0 )
		{
			imFrame = img;
		}
		else
		{
			imFrame = Views.hyperSlice( img, timeDim, frame );
		}
		final LogDetector< T > detector = new LogDetector< T >( imFrame, interval, calibration, radius, threshold, doSubpixel, doMedian );
		detector.setNumThreads( 1 ); // in TrackMate context, we use 1 thread
		// per detector but multiple detectors
		return detector;
	}

	@Override
	public String getKey()
	{
		return DETECTOR_KEY;
	}

	@Override
	public String toString()
	{
		return NAME;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public boolean checkInput()
	{
		final StringBuilder errorHolder = new StringBuilder();
		final boolean ok = checkInput( settings, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
		}
		return ok;
	}

	/**
	 * Check that the given settings map is suitable for LoG based detectors.
	 * 
	 * @param settings
	 *            the map to test.
	 * @param errorHolder
	 *            if not suitable, will contain an error message.
	 * @return true if the settings map is valid.
	 */
	public static boolean checkInput( final Map< String, Object > settings, final StringBuilder errorHolder )
	{
		boolean ok = true;
		ok = ok & checkParameter( settings, KEY_TARGET_CHANNEL, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_RADIUS, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_THRESHOLD, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_DO_MEDIAN_FILTERING, Boolean.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_DO_SUBPIXEL_LOCALIZATION, Boolean.class, errorHolder );
		final List< String > mandatoryKeys = new ArrayList< String >();
		mandatoryKeys.add( KEY_TARGET_CHANNEL );
		mandatoryKeys.add( KEY_RADIUS );
		mandatoryKeys.add( KEY_THRESHOLD );
		mandatoryKeys.add( KEY_DO_MEDIAN_FILTERING );
		mandatoryKeys.add( KEY_DO_SUBPIXEL_LOCALIZATION );
		ok = ok & checkMapKeys( settings, mandatoryKeys, null, errorHolder );
		return ok;
	}

}
