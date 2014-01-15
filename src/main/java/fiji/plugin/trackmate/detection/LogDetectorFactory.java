package fiji.plugin.trackmate.detection;

import static fiji.plugin.trackmate.detection.DetectionUtils.readBooleanAttribute;
import static fiji.plugin.trackmate.detection.DetectionUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.detection.DetectionUtils.readIntegerAttribute;
import static fiji.plugin.trackmate.detection.DetectionUtils.writeDoMedian;
import static fiji.plugin.trackmate.detection.DetectionUtils.writeDoSubPixel;
import static fiji.plugin.trackmate.detection.DetectionUtils.writeRadius;
import static fiji.plugin.trackmate.detection.DetectionUtils.writeTargetChannel;
import static fiji.plugin.trackmate.detection.DetectionUtils.writeThreshold;
import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_DO_SUBPIXEL_LOCALIZATION;
import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_THRESHOLD;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import static fiji.plugin.trackmate.detection.DetectorKeys.XML_ATTRIBUTE_DETECTOR_NAME;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.jdom2.Element;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.panels.detector.LogDetectorConfigurationPanel;
import fiji.plugin.trackmate.util.TMUtils;

@Plugin( type = SpotDetectorFactory.class )
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
	public boolean setTarget( final ImgPlus< T > img, final Map< String, Object > settings )
	{
		this.img = img;
		this.settings = settings;
		return checkSettings( settings );
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
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public boolean checkSettings( final Map< String, Object > settings )
	{
		boolean ok = true;
		final StringBuilder errorHolder = new StringBuilder();
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
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
		}
		return ok;
	}

	@Override
	public boolean marshall( final Map< String, Object > settings, final Element element )
	{
		element.setAttribute( XML_ATTRIBUTE_DETECTOR_NAME, DETECTOR_KEY );
		final StringBuilder errorHolder = new StringBuilder();
		final boolean ok = writeTargetChannel( settings, element, errorHolder ) && writeRadius( settings, element, errorHolder ) && writeThreshold( settings, element, errorHolder ) && writeDoMedian( settings, element, errorHolder ) && writeDoSubPixel( settings, element, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
		}
		return ok;
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > settings )
	{
		settings.clear();

		final String detectorKey = element.getAttributeValue( XML_ATTRIBUTE_DETECTOR_NAME );
		if ( null == detectorKey )
		{
			errorMessage = "Detector element not found.\n";
			return false;
		}

		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = true;
		ok = ok & readDoubleAttribute( element, settings, KEY_RADIUS, errorHolder );
		ok = ok & readDoubleAttribute( element, settings, KEY_THRESHOLD, errorHolder );
		ok = ok & readBooleanAttribute( element, settings, KEY_DO_SUBPIXEL_LOCALIZATION, errorHolder );
		ok = ok & readBooleanAttribute( element, settings, KEY_DO_MEDIAN_FILTERING, errorHolder );
		ok = ok & readIntegerAttribute( element, settings, KEY_TARGET_CHANNEL, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
			return false;
		}
		return checkSettings( settings );
	}

	@Override
	public ConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		return new LogDetectorConfigurationPanel( settings.imp, INFO_TEXT, NAME, model );
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > settings = new HashMap< String, Object >();
		settings.put( KEY_TARGET_CHANNEL, DEFAULT_TARGET_CHANNEL );
		settings.put( KEY_RADIUS, DEFAULT_RADIUS );
		settings.put( KEY_THRESHOLD, DEFAULT_THRESHOLD );
		settings.put( KEY_DO_MEDIAN_FILTERING, DEFAULT_DO_MEDIAN_FILTERING );
		settings.put( KEY_DO_SUBPIXEL_LOCALIZATION, DEFAULT_DO_SUBPIXEL_LOCALIZATION );
		return settings;
	}


}
