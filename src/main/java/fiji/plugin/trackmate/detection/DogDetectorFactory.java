package fiji.plugin.trackmate.detection;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.panels.detector.DogDetectorConfigurationPanel;
import fiji.plugin.trackmate.util.TMUtils;

@Plugin( type = SpotDetectorFactory.class )
public class DogDetectorFactory< T extends RealType< T > & NativeType< T >> extends LogDetectorFactory< T >
{

	/*
	 * CONSTANTS
	 */

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "DOG_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "DoG detector";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>" + "This segmenter is based on an approximation of the LoG operator <br> " + "by differences of gaussian (DoG). Computations are made in direct space. <br>" + "It is the quickest for small spot sizes (< ~5 pixels). " + "<p> " + "Spots found too close are suppressed. This segmenter can do sub-pixel <br>" + "localization of spots using a quadratic fitting scheme. It is based on <br>" + "the scale-space framework made by Stephan Preibisch for ImgLib. " + "</html>";

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
		final double[] calibration = TMUtils.getSpatialCalibration( img );

		final RandomAccessible< T > imFrame = prepareFrameImg( frame );
		final DogDetector< T > detector = new DogDetector< T >( imFrame, interval, calibration, radius, threshold, doSubpixel, doMedian );
		detector.setNumThreads( 1 );
		return detector;
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
	public ConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		return new DogDetectorConfigurationPanel( settings, model, DogDetectorFactory.INFO_TEXT, DogDetectorFactory.NAME );
	}

}
