package fiji.plugin.trackmate.mesh;

import fiji.plugin.trackmate.TrackMatePlugIn;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

public class Demo3DMeshTrackMate
{

	public static void main( final String[] args )
	{
		try
		{

			ImageJ.main( args );
			final String filePath = "samples/CElegans3D-smoothed-mask-orig-t7.tif";
//			final String filePath = "samples/mesh/CElegansMask3D.tif";
			final ImagePlus imp = IJ.openImage( filePath );
			imp.show();

			new TrackMatePlugIn().run( null );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}
}
