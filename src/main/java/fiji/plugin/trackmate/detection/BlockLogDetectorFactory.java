package fiji.plugin.trackmate.detection;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import static fiji.plugin.trackmate.io.IOUtils.readBooleanAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readIntegerAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeDoMedian;
import static fiji.plugin.trackmate.io.IOUtils.writeDoSubPixel;
import static fiji.plugin.trackmate.io.IOUtils.writeRadius;
import static fiji.plugin.trackmate.io.IOUtils.writeTargetChannel;
import static fiji.plugin.trackmate.io.IOUtils.writeThreshold;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jdom2.Element;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.panels.detector.BlockLogDetectorConfigurationPanel;
import fiji.plugin.trackmate.util.TMUtils;

@Plugin( type = SpotDetectorFactory.class )
public class BlockLogDetectorFactory< T extends RealType< T > & NativeType< T >> extends LogDetectorFactory< T >
{
	/*
	 * CONSTANTS
	 */

	/** A string key identifying this factory. */
	public static final String THIS_DETECTOR_KEY = "BLOCK_LOG_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String THIS_NAME = "Block LoG detector";

	/** An html information text. */
	public static final String THIS_INFO_TEXT = "<html>" + "This detector is a version of the LoG detector "
			+ "that splits the image in several blocks and processes them sequentially. "
			+ "<p>"
			+ "This is made to save memory when processing large images. Indeed, the LoG detector "
			+ "has to generate temporary images for processing, which can be a problem when "
			+ "dealing with large source images. This detector splits the source image in to "
			+ "several blocks and process them sequentially. Each block is expected to generate smaller "
			+ "temporary images. Don't forget to configure TrackMate "
			+ "to use onle 1 thread. "
			+ "</html>";

	public static final String KEY_NSPLIT = "NSPLIT";

	private static final int DEFAULT_NSPLIT = 2;

	/*
	 * METHODS
	 */

	@Override
	public SpotDetector< T > getDetector( final Interval interval, final int frame )
	{
		final double radius = ( Double ) settings.get( KEY_RADIUS );
		final double threshold = ( Double ) settings.get( KEY_THRESHOLD );
		final boolean doMedian = ( Boolean ) settings.get( KEY_DO_MEDIAN_FILTERING );
		final boolean doSubpixel = ( Boolean ) settings.get( KEY_DO_SUBPIXEL_LOCALIZATION );
		final int nsplit = ( Integer ) settings.get( KEY_NSPLIT );

		final double[] calibration = TMUtils.getSpatialCalibration( img );
		final RandomAccessible< T > imFrame = prepareFrameImg( frame );

		final BlockLogDetector< T > detector = new BlockLogDetector<>( imFrame, interval, calibration, radius,
				threshold, doSubpixel, doMedian, nsplit );
		detector.setNumThreads( 1 );
		return detector;
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > lSettings = super.getDefaultSettings();
		lSettings.put( KEY_NSPLIT, DEFAULT_NSPLIT );
		return lSettings;
	}

	@Override
	public boolean checkSettings( final Map< String, Object > lSettings )
	{
		boolean ok = true;
		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & checkParameter( lSettings, KEY_TARGET_CHANNEL, Integer.class, errorHolder );
		ok = ok & checkParameter( lSettings, KEY_RADIUS, Double.class, errorHolder );
		ok = ok & checkParameter( lSettings, KEY_THRESHOLD, Double.class, errorHolder );
		ok = ok & checkParameter( lSettings, KEY_DO_MEDIAN_FILTERING, Boolean.class, errorHolder );
		ok = ok & checkParameter( lSettings, KEY_DO_SUBPIXEL_LOCALIZATION, Boolean.class, errorHolder );
		ok = ok & checkParameter( lSettings, KEY_NSPLIT, Integer.class, errorHolder );
		final List< String > mandatoryKeys = new ArrayList<>();
		mandatoryKeys.add( KEY_TARGET_CHANNEL );
		mandatoryKeys.add( KEY_RADIUS );
		mandatoryKeys.add( KEY_THRESHOLD );
		mandatoryKeys.add( KEY_DO_MEDIAN_FILTERING );
		mandatoryKeys.add( KEY_DO_SUBPIXEL_LOCALIZATION );
		mandatoryKeys.add( KEY_NSPLIT );
		ok = ok & checkMapKeys( lSettings, mandatoryKeys, null, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
		}
		return ok;
	}

	@Override
	public boolean marshall( final Map< String, Object > lSettings, final Element element )
	{
		final StringBuilder errorHolder = new StringBuilder();
		final boolean ok = writeTargetChannel( lSettings, element, errorHolder )
				&& writeRadius( lSettings, element, errorHolder )
				&& writeThreshold( lSettings, element, errorHolder )
				&& writeDoMedian( lSettings, element, errorHolder )
				&& writeDoSubPixel( lSettings, element, errorHolder )
				&& writeNSplit( lSettings, element, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
		}
		return ok;
	}

	public static final boolean writeNSplit( final Map< String, Object > settings, final Element element, final StringBuilder errorHolder )
	{
		return writeAttribute( settings, element, KEY_NSPLIT, Integer.class, errorHolder );
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > lSettings )
	{
		lSettings.clear();
		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = true;
		ok = ok & readDoubleAttribute( element, lSettings, KEY_RADIUS, errorHolder );
		ok = ok & readDoubleAttribute( element, lSettings, KEY_THRESHOLD, errorHolder );
		ok = ok & readBooleanAttribute( element, lSettings, KEY_DO_SUBPIXEL_LOCALIZATION, errorHolder );
		ok = ok & readBooleanAttribute( element, lSettings, KEY_DO_MEDIAN_FILTERING, errorHolder );
		ok = ok & readIntegerAttribute( element, lSettings, KEY_TARGET_CHANNEL, errorHolder );
		ok = ok & readIntegerAttribute( element, lSettings, KEY_NSPLIT, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
			return false;
		}
		return checkSettings( lSettings );
	}

	@Override
	public ConfigurationPanel getDetectorConfigurationPanel( final Settings lSettings, final Model model )
	{
		return new BlockLogDetectorConfigurationPanel( lSettings, model, THIS_INFO_TEXT, THIS_NAME );
	}

	@Override
	public String getKey()
	{
		return THIS_DETECTOR_KEY;
	}

	@Override
	public String getName()
	{
		return THIS_NAME;
	}

	@Override
	public String getInfoText()
	{
		return THIS_INFO_TEXT;
	}
}
