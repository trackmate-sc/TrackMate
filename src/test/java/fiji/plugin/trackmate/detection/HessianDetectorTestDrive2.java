package fiji.plugin.trackmate.detection;

import fiji.plugin.trackmate.TrackMatePlugIn;
import fiji.plugin.trackmate.gui.GuiUtils;
import ij.ImageJ;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class HessianDetectorTestDrive2
{
	public static < T extends RealType< T > & NativeType< T > > void main( final String[] args )
	{
		ImageJ.main( args );
		GuiUtils.setSystemLookAndFeel();
		new TrackMatePlugIn().run( "samples/TSabateCell-movie.tif" );
	}
}
