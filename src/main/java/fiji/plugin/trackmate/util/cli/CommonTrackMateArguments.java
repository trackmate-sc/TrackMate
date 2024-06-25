package fiji.plugin.trackmate.util.cli;

import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.ThresholdDetectorFactory.KEY_SIMPLIFY_CONTOURS;

import fiji.plugin.trackmate.util.cli.CLIConfigurator.Flag;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.IntArgument;

/**
 * Arguments that are commonly used by TrackMate, to add to custom
 * {@link fiji.plugin.trackmate.util.cli.CLIConfigurator}.
 */
public class CommonTrackMateArguments
{

	/**
	 * Creates an argument used to specify on what channel in the input image to
	 * operate on.
	 * 
	 * @param nChannels
	 *            how many channels in the input image.
	 * @return the target channel argument.
	 */
	public static IntArgument targetChannel( final int nChannels )
	{
		final IntArgument arg = new IntArgument()
				.key( KEY_TARGET_CHANNEL )
				.defaultValue( DEFAULT_TARGET_CHANNEL )
				.name( "Target channel" )
				.help( "Index of the channel to process." )
				.inCLI( false )
				.visible( true )
				.min( 1 ) // 1-based
				.max( Integer.valueOf( nChannels ) );
		arg.set( DEFAULT_TARGET_CHANNEL );
		return arg;
	}

	public static Flag simplyContour()
	{
		final Flag arg = new Flag()
				.key( KEY_SIMPLIFY_CONTOURS )
				.defaultValue( true )
				.name( "Simplify contour" )
				.help( "If true contours will be simplified with fewer control points." )
				.inCLI( false )
				.visible( true );
		arg.set( true );
		return arg;
	}

}
