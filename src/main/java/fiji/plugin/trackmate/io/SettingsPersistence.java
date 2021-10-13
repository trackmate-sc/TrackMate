package fiji.plugin.trackmate.io;

import java.io.File;
import java.io.IOException;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.tracking.sparselap.SimpleSparseLAPTrackerFactory;
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

		final Settings settings = reader.readSettings( imp );
		return settings;
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

