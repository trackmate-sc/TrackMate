package fiji.plugin.trackmate.action;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import fiji.plugin.trackmate.LoadTrackMatePlugIn;
import ij.ImageJ;

public class CTCExporterTestDrive
{

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );

		ImageJ.main( args );

		final LoadTrackMatePlugIn plugin = new LoadTrackMatePlugIn();
		plugin.run( "samples/ctc/01_label image_tracking.xml" );
//		plugin.run( "samples/FakeTracks.xml" );
	}
}
