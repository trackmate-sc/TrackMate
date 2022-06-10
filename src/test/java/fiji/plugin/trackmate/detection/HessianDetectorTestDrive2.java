package fiji.plugin.trackmate.detection;

import fiji.plugin.trackmate.TrackMatePlugIn;
import fiji.plugin.trackmate.gui.GuiUtils;
import ij.ImageJ;
import ij.plugin.frame.RoiManager;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class HessianDetectorTestDrive2
{
	public static < T extends RealType< T > & NativeType< T > > void main( final String[] args )
	{
		ImageJ.main( args );
		GuiUtils.setSystemLookAndFeel();
		final RoiManager roiManager = RoiManager.getRoiManager();
		roiManager.runCommand( "Open", "samples/20220131-1435_Lv4TetOinCuO-C4_t-000-106_p005.ome_ALN_MarginsCropped-rois2.zip" );
		new TrackMatePlugIn().run( "samples/20220131-1435_Lv4TetOinCuO-C4_t-000-106_p005.ome_ALN_MarginsCropped-2.tif" );
	}
}
