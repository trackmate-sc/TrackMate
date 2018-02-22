package fiji.plugin.trackmate.interactivetests;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTracker;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.ImagePlus;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.scijava.util.AppUtils;

public class NNTrackerTest {

	/*
	 * MAIN METHOD
	 */

	public static void main(final String args[]) {


		final File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.xml");

		// 1 - Load test spots
		System.out.println("Opening file: "+file.getAbsolutePath());
		final TmXmlReader reader = new TmXmlReader(file);
		final Model model = reader.getModel();
		final Settings gs = new Settings();
		reader.readSettings(gs, null, null, null, null, null);

		System.out.println("Spots: "+ model.getSpots());
		System.out.println("Found "+model.getTrackModel().nTracks(false)+" tracks in the file:");
		System.out.println("Track features: ");
		System.out.println();

		// 2 - Track the test spots
		final long start = System.currentTimeMillis();
		final Map<String, Object> settings = new HashMap<>();
		settings.put(KEY_LINKING_MAX_DISTANCE, 15d);
		final NearestNeighborTracker tracker = new NearestNeighborTracker( model.getSpots(), settings );
		tracker.setLogger(Logger.DEFAULT_LOGGER);

		if (!tracker.checkInput())
			System.err.println("Error checking input: "+tracker.getErrorMessage());
		if (!tracker.process())
			System.err.println("Error in process: "+tracker.getErrorMessage());
		final long end = System.currentTimeMillis();
		model.setTracks(tracker.getResult(), true);

		// 3 - Print out results for testing
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println("Found " + model.getTrackModel().nTracks(false) + " final tracks.");
		System.out.println("Whole tracking done in "+(end-start)+" ms.");
		System.out.println();


		// 5 - Display tracks
		// Load Image
		ij.ImageJ.main(args);

		final ImagePlus imp = gs.imp;
		final TrackMateModelView sd2d = new HyperStackDisplayer(model, new SelectionModel(model), imp);
		sd2d.render();
		sd2d.setDisplaySettings(TrackMateModelView.KEY_TRACK_DISPLAY_MODE, TrackMateModelView.TRACK_DISPLAY_MODE_WHOLE);
	}


}
