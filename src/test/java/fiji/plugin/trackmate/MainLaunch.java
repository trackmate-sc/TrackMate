package fiji.plugin.trackmate;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

import fiji.plugin.trackmate.io.geff.GeffTestBase;
import ij.IJ;
import ij.ImageJ;

public class MainLaunch extends GeffTestBase
{

	public static void main( final String[] args )
	{
		try
		{
			if ( IJ.isMacintosh() )
				FlatMacLightLaf.setup();
			else
				FlatLightLaf.setup();
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
