/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
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
package fiji.plugin.trackmate.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.io.geff.TmGeffReader;
import fiji.plugin.trackmate.io.geff.TmGeffWriter;
import fiji.plugin.trackmate.tracking.jaqaman.SimpleSparseLAPTrackerFactory;
import ij.ImagePlus;

/**
 * Static utilities to have some persistence of settings across runs.
 */
public class SettingsPersistence
{

	private static File lastSettingsXMLFile = new File( new File( System.getProperty( "user.home" ), ".trackmate" ), "lastusedsettings.xml" );

	private static String lastSettingsFile = new File( new File( System.getProperty( "user.home" ), ".trackmate" ),
			"lastusedsettings.geff" ).getAbsolutePath();

	@SuppressWarnings( "deprecation" )
	public static final Settings readLastUsedSettings( final ImagePlus imp, final Logger logger )
	{
		if ( !Files.exists( Paths.get( lastSettingsFile ) ) )
		{
			// Try to read the old XML file, if it exists.
			final TmXmlReader reader = new TmXmlReader( lastSettingsXMLFile );
			if ( !reader.isReadingOk() )
			{
				logger.log( "No last-used settings found in '" + lastSettingsFile + "'.\n" );
				logger.log( "Using built-in defaults.\n" );
				return getDefaultSettings( imp );
			}
			final Settings settings = reader.readSettings( null );
			if ( imp == null )
				return settings;
			final Settings newSettings = settings.copyOn( imp );
			return newSettings;
		}

		final TmGeffReader reader = new TmGeffReader( lastSettingsFile );
		final Settings settings = reader.readSettings( null );
		if ( imp == null )
			return settings;
		final Settings newSettings = settings.copyOn( imp );
		return newSettings;
	}

	public static final void saveLastUsedSettings( final Settings settings, final Logger logger )
	{
		if ( !Files.exists( Paths.get( lastSettingsFile ) ) )
			try
			{
				Files.createDirectories( Paths.get( lastSettingsFile ).getParent() );
			}
			catch ( final IOException e )
			{
				logger.error( "Problem creating the directory for the last used settings file at " + lastSettingsFile + ".\n" );
				e.printStackTrace();
				return;
			}

		final TmGeffWriter writer = new TmGeffWriter( lastSettingsFile );
		writer.appendSettings( settings );
		try
		{
			writer.write();
		}
		catch ( final IOException e )
		{
			logger.error( "Problem writing to the last used settings file at " + lastSettingsFile + ".\n" );
		}
	}

	public static Settings getDefaultSettings( final ImagePlus imp )
	{
		final Settings settings = new Settings( imp );
		settings.detectorFactory = new LogDetectorFactory<>();
		settings.detectorSettings = settings.detectorFactory.getDefaultSettings();
		settings.trackerFactory = new SimpleSparseLAPTrackerFactory();
		settings.trackerSettings = settings.trackerFactory.getDefaultSettings();
		settings.addAllAnalyzers();
		return settings;
	}
}
