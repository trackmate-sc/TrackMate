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

import fiji.plugin.trackmate.util.cli.Configurator.DoubleArgument;
import fiji.plugin.trackmate.util.cli.Configurator.Flag;
import fiji.plugin.trackmate.util.cli.Configurator.IntArgument;

/**
 * Arguments that are commonly used by TrackMate, to add to custom
 * {@link fiji.plugin.trackmate.util.cli.CLIConfigurator}.
 */
public class CommonTrackMateArguments
{

	/**
	 * Creates an argument used to specify on what channel in the input image to
	 * operate on and add it to the given configurator.
	 *
	 * @param nChannels
	 *            how many channels in the input image.
	 * @return
	 */
	public static IntArgument addTargetChannel( final Configurator config, final int nChannels )
	{
		final IntArgument arg = config.addIntArgument()
				.key( KEY_TARGET_CHANNEL )
				.defaultValue( DEFAULT_TARGET_CHANNEL )
				.name( "Target channel" )
				.help( "Index of the channel to process." )
				.inCLI( false )
				.visible( true )
				.min( 1 ) // 1-based
				.max( Integer.valueOf( nChannels ) )
				.get();
		arg.set( DEFAULT_TARGET_CHANNEL );
		return arg;
	}

	public static Flag addSimplifyContour( final Configurator config )
	{
		final Flag arg = config.addFlag()
				.key( KEY_SIMPLIFY_CONTOURS )
				.defaultValue( true )
				.name( "Simplify contour" )
				.help( "If true contours will be simplified with fewer control points." )
				.inCLI( false )
				.visible( true )
				.get();
		arg.set( true );
		return arg;
	}

	public static DoubleArgument addRadius( final Configurator config, final String units )
	{
		final DoubleArgument arg = config.addDoubleArgument()
				.key( KEY_RADIUS )
				.argument( "--radius" )
				.defaultValue( DEFAULT_RADIUS )
				.units( units )
				.name( "Radius" )
				.help( "Radius of the objects to detect, in " + units + "." )
				.visible( true )
				.get();
		arg.set( DEFAULT_RADIUS );
		return arg;
	}

	/**
	 * Adds a diameter argument to the given configurator.
	 * <p>
	 * Here there is a gotcha: the value displayed in the UI is the diameter,
	 * but the value stored and returned is the radius. Therefore, we add a
	 * translator that divides the value by 2.
	 *
	 * @param config
	 *            the config to which to add the argument.
	 * @param units
	 *            the units of the diameter to display.
	 */
	public static DoubleArgument addDiameter( final Configurator config, final String units )
	{
		final DoubleArgument arg = config.addDoubleArgument()
				.key( KEY_RADIUS )
				.argument( "--radius" )
				.defaultValue( DEFAULT_RADIUS )
				.units( units )
				.name( "Diameter" )
				.help( "Diameter of the objects to detect." )
				.visible( true )
				.get();
		arg.set( DEFAULT_RADIUS );
		// Add a translator from radius (stored) to diameter (displayed).
		config.setDisplayTranslator( arg, r -> r * 2., d -> d / 2. );
		return arg;
	}

	public static DoubleArgument addThreshold( final Configurator config )
	{
		final DoubleArgument arg = config.addDoubleArgument()
				.key( KEY_THRESHOLD )
				.defaultValue( DEFAULT_THRESHOLD )
				.name( "Threshold" )
				.help( "The threshold to apply to the detector." )
				.visible( true )
				.get();
		arg.set( DEFAULT_THRESHOLD );
		return arg;
	}

	public static Flag addSubpixelLocalization( final Configurator config )
	{
		final Flag flag = config.addFlag()
				.key( KEY_DO_SUBPIXEL_LOCALIZATION )
				.defaultValue( DEFAULT_DO_SUBPIXEL_LOCALIZATION )
				.name( "Sub-pixel localization" )
				.help( "If true, the detector will try to localize spots with sub-pixel accuracy." )
				.visible( true )
				.get();
		flag.set( DEFAULT_DO_SUBPIXEL_LOCALIZATION );
		return flag;
	}

	public static Flag addMedianFiltering( final Configurator config )
	{
		final Flag flag = config.addFlag()
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
