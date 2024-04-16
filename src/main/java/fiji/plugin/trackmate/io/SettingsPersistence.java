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
package fiji.plugin.trackmate.io;

import java.io.File;
import java.io.IOException;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.tracking.jaqaman.SimpleSparseLAPTrackerFactory;
import ij.ImagePlus;

/**
 * Static utilities to have some persistence of settings across runs.
 */
public class SettingsPersistence
{

	private static File lastSettingsFile = new File( new File( System.getProperty( "user.home" ), ".trackmate" ), "lastusedsettings.xml" );

	public static final Settings readLastUsedSettings( final ImagePlus imp, final Logger logger )
	{
		final TmXmlReader reader = new TmXmlReader( lastSettingsFile );
		if ( !reader.isReadingOk() )
		{
			logger.error( "Could not read the last used settings file at " + lastSettingsFile + ".\n" );
			logger.error( "Using built-in defaults.\n" );
			return getDefaultSettings( imp );
		}

		final Settings settings = reader.readSettings( null );
		if ( imp == null )
			return settings;
		final Settings newSettings = settings.copyOn( imp );
		return newSettings;
	}

	public static final void saveLastUsedSettings( final Settings settings, final Logger logger )
	{
		if ( !lastSettingsFile.exists() )
			lastSettingsFile.getParentFile().mkdirs();

		final TmXmlWriter writer = new TmXmlWriter( lastSettingsFile );
		writer.appendSettings( settings );
		try
		{
			writer.writeToFile();
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
