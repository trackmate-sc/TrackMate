package fiji.plugin.trackmate;

import fiji.plugin.trackmate.io.geff.GeffTestBase;
import ij.ImageJ;

public class MainLaunch extends GeffTestBase
{

	public static void main( final String[] args )
	{
		try
		{
//			GuiUtils.setSystemLookAndFeel();
			ImageJ.main( args );
//			new TrackMatePlugIn().run( "samples/MAX_Merged.tif" );
//			new TrackMatePlugIn().run( "samples/FakeTracks.tif" );
			new TrackMatePlugIn().run( "samples/CElegans3D-smoothed-mask-orig.tif" );
		}
		catch ( final Throwable t )
		{
			t.printStackTrace();
		}
	}
}
