package fiji.plugin.trackmate;

import fiji.plugin.trackmate.io.geff.GeffTestBase;
import ij.ImageJ;

public class MainLaunch extends GeffTestBase
{

	public static void main( final String[] args )
	{
//		GuiUtils.setSystemLookAndFeel();
		ImageJ.main( args );
//		new TrackMatePlugIn().run( "samples/Stack.tif" );
//		new TrackMatePlugIn().run( "samples/Merged.tif" );
//		new TrackMatePlugIn().run( "samples/MAX_Merged.tif" );
//		new TrackMatePlugIn().run( "samples/Mask.tif" );
		new TrackMatePlugIn().run( "samples/FakeTracks.tif" );
	}
}
