package fiji.plugin.trackmate;

import fiji.Debug;

public class Main
{
	public static void main( final String... args )
	{
		Debug.runFilter( "samples/FakeTracks.tif", "TrackMate", null );
	}
}
