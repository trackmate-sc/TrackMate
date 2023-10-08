package fiji.plugin.trackmate;

import org.scijava.Context;

import fiji.plugin.trackmate.util.TMUtils;
import ij.IJ;
import ij.ImagePlus;

class TestTrackMatePlugin extends TrackMatePlugIn {

	@SuppressWarnings("unused")
	public void setUp() {
		final ImagePlus imp = IJ.createImage("Test Image", 256, 256, 10, 8);
		final Settings settings = createSettings(imp);
		final Model model = createModel(imp);
		final TrackMate trackMate = createTrackMate(model, settings);
	}

	public Context getLocalContext() {
		return TMUtils.getContext();
	}
}