package fiji.plugin.trackmate.util.config;

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
import static fiji.plugin.trackmate.detection.ThresholdDetectorFactory.KEY_SIMPLIFY_CONTOURS;
import static fiji.plugin.trackmate.detection.ThresholdDetectorFactory.KEY_SMOOTHING_SCALE;

import org.scijava.ui.config.Configurator;
import org.scijava.ui.config.Parameters.BooleanParam;
import org.scijava.ui.config.Parameters.DoubleParam;
import org.scijava.ui.config.Parameters.IntParam;

/**
 * An intermediate class to facilitate adding common TrackMate parameters to a
 * {@link Configurator}.
 */
public abstract class TrackMateConfigurator extends Configurator
{

	protected TrackMateConfigurator( final String name, final String help )
	{
		super( name, help );
	}

	protected BooleanParam addSimplifyContour()
	{
		final BooleanParam param = addBooleanParameter()
				.key( KEY_SIMPLIFY_CONTOURS )
				.defaultValue( true )
				.name( "Simplify contour" )
				.help( "If true contours will be simplified with fewer control points." )
				.visible( true )
				.get();
		param.set( true );
		return param;
	}

	protected DoubleParam addSmoothContour( final String units )
	{
		final DoubleParam param = addDoubleParameter()
				.key( KEY_SMOOTHING_SCALE )
				.defaultValue( 0. )
				.min( 0. )
				.max( 20. )
				.units( units )
				.name( "Smooth contour" )
				.help( "If > 0, contours will be smoothed over this radius." )
				.visible( true )
				.get();
		param.set( 0. );
		// No need to translate: TrackMate expects physical units
		return param;
	}

	/**
	 * Creates an argument used to specify on what channel in the input image to
	 * operate on and add it to this configurator.
	 * <p>
	 * The channel index is 1-based.
	 *
	 * @param nChannels
	 *            how many channels in the input image.
	 * @return the integer target channel argument.
	 */
	protected IntParam addTargetChannel( final int nChannels )
	{
		final IntParam param = addIntParameter()
				.key( KEY_TARGET_CHANNEL )
				.defaultValue( DEFAULT_TARGET_CHANNEL )
				.name( "Target channel" )
				.help( "Index of the channel to process." )
				.visible( true )
				.min( 1 ) // 1-based
				.max( Integer.valueOf( nChannels ) )
				.get();
		param.set( DEFAULT_TARGET_CHANNEL );
		return param;
	}

	protected DoubleParam addRadius( final String units )
	{
		final DoubleParam param = addDoubleParameter()
				.key( KEY_RADIUS )
				.defaultValue( DEFAULT_RADIUS )
				.units( units )
				.name( "Radius" )
				.help( "Radius of the objects to detect, in " + units + "." )
				.visible( true )
				.get();
		param.set( DEFAULT_RADIUS );
		return param;
	}

	/**
	 * Adds a diameter argument to this configurator.
	 * <p>
	 * Here there is a gotcha: the value displayed in the UI is the diameter,
	 * but the value stored and returned is the radius. Therefore, we add a
	 * translator that divides the value by 2.
	 *
	 * @param units
	 *            the units of the diameter to display.
	 * @return the diameter double argument.
	 */
	protected DoubleParam addDiameter( final String units )
	{
		final DoubleParam param = addDoubleParameter()
				.key( KEY_RADIUS )
				.defaultValue( DEFAULT_RADIUS )
				.units( units )
				.name( "Diameter" )
				.help( "Diameter of the objects to detect." )
				.visible( true )
				.get();
		param.set( DEFAULT_RADIUS );
		// Add a translator from radius (stored) to diameter (displayed).
		setDisplayTranslator( param, r -> r * 2., d -> d / 2. );
		return param;
	}

	protected DoubleParam addThreshold()
	{
		final DoubleParam param = addDoubleParameter()
				.key( KEY_THRESHOLD )
				.defaultValue( DEFAULT_THRESHOLD )
				.name( "Threshold" )
				.help( "The threshold to apply to the detector." )
				.visible( true )
				.get();
		param.set( DEFAULT_THRESHOLD );
		return param;
	}

	protected BooleanParam addSubpixelLocalization()
	{
		final BooleanParam param = addBooleanParameter()
				.key( KEY_DO_SUBPIXEL_LOCALIZATION )
				.defaultValue( DEFAULT_DO_SUBPIXEL_LOCALIZATION )
				.name( "Sub-pixel localization" )
				.help( "If true, the detector will try to localize spots with sub-pixel accuracy." )
				.visible( true )
				.get();
		param.set( DEFAULT_DO_SUBPIXEL_LOCALIZATION );
		return param;
	}

	protected BooleanParam addMedianFiltering()
	{
		final BooleanParam flag = addBooleanParameter()
				.key( KEY_DO_MEDIAN_FILTERING )
				.defaultValue( DEFAULT_DO_MEDIAN_FILTERING )
				.name( "Median filtering" )
				.help( "If true, the detector will apply a median filter to the image before detection." )
				.visible( true )
				.get();
		flag.set( DEFAULT_DO_MEDIAN_FILTERING );
		return flag;
	}
}
