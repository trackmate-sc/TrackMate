package fiji.plugin.trackmate;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;

import fiji.plugin.trackmate.action.ISBIChallengeExporter;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;
import fiji.plugin.trackmate.tracking.sparselap.SimpleSparseLAPTrackerFactory;


public class TrackMateBatch
{

	public static void main( final String[] args )
	{
		final Logger logger = Logger.DEFAULT_LOGGER;

		final File rootFolder = new File( "/Users/tinevez/Projects/JYTinevez/ISBI/GroundTruth/MICROTUBULE" );

		final File exportFolder = new File( "/Users/tinevez/Projects/JYTinevez/ISBI/ISBI_scoring/TrackMate" );

		final FilenameFilter filter = new FilenameFilter()
		{
			@Override
			public boolean accept( final File dir, final String name )
			{
				return name.toLowerCase().startsWith( "microtubule" ) & name.toLowerCase().endsWith( "tm.xml" );
			}
		};

		final String[] trackmateFiles = rootFolder.list( filter );

		final DetectorProvider detectorProvider = new DetectorProvider();
		final TrackerProvider trackerProvider = new TrackerProvider();
		final EdgeAnalyzerProvider edgeAnalyzerProvider = new EdgeAnalyzerProvider();
		final TrackAnalyzerProvider trackAnalyzerProvider = new TrackAnalyzerProvider();

		for ( final String trackmateFile : trackmateFiles )
		{
			// Open file
			final File path = new File( rootFolder.getAbsolutePath(), trackmateFile );
			logger.log( "\n" + path + '\n' );

			// Check if already done.
			final File exportPath = new File( exportFolder, trackmateFile );
			if ( exportPath.exists() )
			{
				logger.flush();
				logger.log( "Exported file exists. Skipping.\n" );
				continue;
			}


			final TmXmlReader reader = new TmXmlReader( path );
			if ( !reader.isReadingOk() )
			{
				logger.flush();
				logger.error( reader.getErrorMessage() );
				continue;
			}

			// Read model
			final Model model = reader.getModel();
			if ( !reader.isReadingOk() )
			{
				logger.flush();
				logger.error( reader.getErrorMessage() );
				continue;
			}
			model.setLogger( logger );

			// Read settings
			final Settings settings = new Settings();

			// Create the spot analyzer provider now, that we know the source image.
			final SpotAnalyzerProvider spotAnalyzerProvider = new SpotAnalyzerProvider( settings.imp );


			reader.readSettings( settings, detectorProvider, trackerProvider, spotAnalyzerProvider, edgeAnalyzerProvider, trackAnalyzerProvider );
			if ( !reader.isReadingOk() )
			{
				logger.flush();
				logger.error( reader.getErrorMessage() );
				continue;
			}

			// Edit settings
			final SpotTrackerFactory tf = new SimpleSparseLAPTrackerFactory();
			final Map< String, Object > ts = tf.getDefaultSettings();
			ts.put( KEY_LINKING_MAX_DISTANCE, 10d );
			ts.put( KEY_GAP_CLOSING_MAX_DISTANCE, 15d );
			ts.put( KEY_GAP_CLOSING_MAX_FRAME_GAP, 2 );

			settings.trackerFactory = tf;
			settings.trackerSettings = ts;

			// Re-run TrackMate for the tracking part
			final TrackMate trackmate = new TrackMate( model, settings );
			final boolean trackingOk = trackmate.execTracking();
			if ( !trackingOk )
			{
				logger.flush();
				logger.error( trackmate.getErrorMessage() );
			}

			// Then export to ISBI
			ISBIChallengeExporter.exportToFile( model, settings, exportPath );
			logger.flush();

		}

		logger.log( "\nDone." );

	}

}
