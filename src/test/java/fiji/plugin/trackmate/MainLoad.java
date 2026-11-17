package fiji.plugin.trackmate;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

import fiji.plugin.trackmate.io.geff.GeffTestBase;
import ij.IJ;
import ij.ImageJ;

/**
 * Main entry point to launch TrackMate by loading a file.
 */
public class MainLoad extends GeffTestBase
{

	/*
	 * MAIN METHOD
	 */

	public static void main( final String[] args )
	{
		try
		{
			if ( IJ.isMacintosh() )
				FlatMacLightLaf.setup();
			else
				FlatLightLaf.setup();
			ImageJ.main( args );
			final LoadTrackMatePlugIn plugIn = new LoadTrackMatePlugIn();
//			plugIn.run( "" );
//			plugIn.run( "samples/test-cube-mesh.geff" );
//			plugIn.run( "samples/FakeTracks.geff" );
//			plugIn.run( "samples/MAX_Merged.geff" );
			plugIn.run( "samples/CElegans3D-smoothed-mask-orig.geff" );
		}
		catch ( final Throwable t )
		{
			t.printStackTrace();
		}
	}
}
