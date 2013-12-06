package fiji.plugin.trackmate.interactivetests;

import fiji.plugin.trackmate.LoadTrackMatePlugIn_;
import ij.ImageJ;

public class TrackMateLoading_TestDrive {
	
	public static void main(String[] args) {
		
		ImageJ.main(args);
		LoadTrackMatePlugIn_ plugin = new LoadTrackMatePlugIn_();
		plugin.run(null); // launch the GUI;
		
	}
}
