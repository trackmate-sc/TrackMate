package fiji.plugin.trackmate;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.scijava.Context;
import org.scijava.object.ObjectService;

import fiji.plugin.trackmate.util.TMUtils;
import ij.IJ;
import ij.ImagePlus;

public class TrackMatePluginTest {

	@Test
	public void testTrackMateRegistration() {
		TestTrackMatePlugin testPlugin = new TestTrackMatePlugin();
		testPlugin.setUp();
		ObjectService objectService = testPlugin.getLocalContext().service(ObjectService.class);
		
		List<TrackMate> trackMateInstances = objectService.getObjects(TrackMate.class);
		assertTrue(trackMateInstances.size() == 1);
		assertTrue(trackMateInstances.get(0) instanceof TrackMate);
	}

	private class TestTrackMatePlugin extends TrackMatePlugIn {

		@SuppressWarnings("unused")
		public void setUp() {
			ImagePlus imp = IJ.createImage("Test Image", 256, 256, 10, 8);
			Settings settings = createSettings(imp);
			Model model = createModel(imp);
			TrackMate trackMate = createTrackMate(model, settings);
		}

		public Context getLocalContext() {
			return TMUtils.getContext();
		}

	}
}
