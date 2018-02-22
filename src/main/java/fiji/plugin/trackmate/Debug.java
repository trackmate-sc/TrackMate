package fiji.plugin.trackmate;

import java.io.File;

import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.kalman.KalmanTracker;

public class Debug
{

	public static void main( final String[] args )
	{
		fiji.Debug.runFilter( "/Users/tinevez/Development/TrackMate/samples/FakeTracks.tif", "TrackMate", "" );
	}

	/**
	 * @param args  
	 */
	public static void main2( final String[] args )
	{
		final File file = new File( "/Users/tinevez/Desktop/CRTD62.xml" );
		final TmXmlReader reader = new TmXmlReader( file );
		if ( !reader.isReadingOk() )
		{
			System.err.println( reader.getErrorMessage() );
			return;
		}

		final Model model = reader.getModel();
		final SpotCollection spots = model.getSpots();

		final double maxSearchRadius = 2;
		final int maxFrameGap = 2;
		final double initialSearchRadius = 1.5;
		final KalmanTracker tracker = new KalmanTracker( spots, maxSearchRadius, maxFrameGap, initialSearchRadius );
		if ( !tracker.checkInput() || !tracker.process() )
		{
			System.err.println( tracker.getErrorMessage() );
			return;
		}

		System.out.println( tracker.getResult() );// DEBUG
	}
}
