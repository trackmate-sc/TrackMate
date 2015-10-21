package fiji.plugin.trackmate.detection;

import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_DOWNSAMPLE_FACTOR;
import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_THRESHOLD;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DOWNSAMPLE_FACTOR;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readIntegerAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeDownsamplingFactor;
import static fiji.plugin.trackmate.io.IOUtils.writeRadius;
import static fiji.plugin.trackmate.io.IOUtils.writeTargetChannel;
import static fiji.plugin.trackmate.io.IOUtils.writeThreshold;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jdom2.Element;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.panels.detector.DownSampleLogDetectorConfigurationPanel;
import fiji.plugin.trackmate.util.TMUtils;

@Plugin( type = SpotDetectorFactory.class )
public class DownsampleLogDetectorFactory< T extends RealType< T > & NativeType< T >> extends LogDetectorFactory< T >
{

	/*
	 * CONSTANTS
	 */

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "DOWNSAMPLE_LOG_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Downsample LoG detector";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>" + "This detector is basically identical to the LoG detector, except <br>" + "that images are downsampled before filtering, giving it a good <br>" + "kick in speed, particularly for large spot sizes. It is the fastest for <br>" + "large spot sizes (>&nbsp;~20 pixels), at the cost of precision in localization. " + "</html>";

	/*
	 * METHODS
	 */

	@Override
	public SpotDetector< T > getDetector( final Interval interval, final int frame )
	{
		final double radius = ( Double ) settings.get( KEY_RADIUS );
		final double threshold = ( Double ) settings.get( KEY_THRESHOLD );
		final int downsamplingFactor = ( Integer ) settings.get( KEY_DOWNSAMPLE_FACTOR );
		final double[] calibration = TMUtils.getSpatialCalibration( img );

		final RandomAccessible< T > imFrame = prepareFrameImg( frame );
		final DownsampleLogDetector< T > detector = new DownsampleLogDetector< T >( imFrame, interval, calibration, radius, threshold, downsamplingFactor );
		return detector;
	}

	@Override
	public boolean setTarget( final ImgPlus< T > img, final Map< String, Object > settings )
	{
		this.img = img;
		this.settings = settings;
		return checkSettings( settings );
	}

	@Override
	public String getKey()
	{
		return DETECTOR_KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > settings = new HashMap< String, Object >();
		settings.put( KEY_TARGET_CHANNEL, DEFAULT_TARGET_CHANNEL );
		settings.put( KEY_RADIUS, DEFAULT_RADIUS );
		settings.put( KEY_THRESHOLD, DEFAULT_THRESHOLD );
		settings.put( KEY_DOWNSAMPLE_FACTOR, DEFAULT_DOWNSAMPLE_FACTOR );
		return settings;
	}

	@Override
	public boolean checkSettings( final Map< String, Object > settings )
	{
		boolean ok = true;
		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & checkParameter( settings, KEY_TARGET_CHANNEL, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_RADIUS, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_THRESHOLD, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_DOWNSAMPLE_FACTOR, Integer.class, errorHolder );
		final List< String > mandatoryKeys = new ArrayList< String >();
		mandatoryKeys.add( KEY_TARGET_CHANNEL );
		mandatoryKeys.add( KEY_RADIUS );
		mandatoryKeys.add( KEY_THRESHOLD );
		mandatoryKeys.add( KEY_DOWNSAMPLE_FACTOR );
		ok = ok & checkMapKeys( settings, mandatoryKeys, null, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
		}
		return ok;
	}

	@Override
	public ConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		return new DownSampleLogDetectorConfigurationPanel( settings, model );
	}

	@Override
	public boolean marshall( final Map< String, Object > settings, final Element element )
	{
		final StringBuilder errorHolder = new StringBuilder();
		final boolean ok = writeTargetChannel( settings, element, errorHolder ) && writeRadius( settings, element, errorHolder ) && writeThreshold( settings, element, errorHolder ) && writeDownsamplingFactor( settings, element, errorHolder );
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
		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = true;
		ok = ok & readDoubleAttribute( element, settings, KEY_RADIUS, errorHolder );
		ok = ok & readDoubleAttribute( element, settings, KEY_THRESHOLD, errorHolder );
		ok = ok & readIntegerAttribute( element, settings, KEY_DOWNSAMPLE_FACTOR, errorHolder );
		ok = ok & readIntegerAttribute( element, settings, KEY_TARGET_CHANNEL, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
			return false;
		}
		return checkSettings( settings );
	}

}
