/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
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
