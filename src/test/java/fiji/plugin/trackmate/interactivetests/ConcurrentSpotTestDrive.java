package fiji.plugin.trackmate.interactivetests;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.providers.DetectorProvider;
import ij.ImagePlus;
import ij.gui.NewImage;

import java.util.Iterator;

public class ConcurrentSpotTestDrive {

	public static void main(final String[] args) {

		final int nFrames = 20;

		final Model model = new Model();

		// Create blank image
		final ImagePlus imp = NewImage.createByteImage("Noise", 200, 200, nFrames, NewImage.FILL_BLACK);

		// Add noise to it
		for (int i = 0; i < imp.getStackSize(); i++) {
			imp.getStack().getProcessor(i+1).noise(50);
		}

		// Setup calibration
		imp.setDimensions(1, 1, nFrames);

		// Run track mate on it

		// Make settings
		final Settings settings = new Settings();
		settings.setFrom(imp);
		final DetectorProvider provider = new DetectorProvider();
		settings.detectorFactory = provider.getFactory( LogDetectorFactory.DETECTOR_KEY );
		settings.detectorSettings = settings.detectorFactory.getDefaultSettings();

		// Execute detection
		final TrackMate trackmate = new TrackMate(model, settings);
		trackmate.execDetection();

		// Retrieve spots
		final SpotCollection spots = trackmate.getModel().getSpots();

		// Parse spots and detect duplicate IDs
		final int[] IDs = new int[Spot.IDcounter.get() + 1];
		final Iterator<Spot> it = spots.iterator(false);
		while(it.hasNext()) {
			final Spot si = it.next();
			final int id = si.ID();
			IDs[id]++;
		}

		boolean ok = true;
		for (int i = 0; i < IDs.length; i++) {
			if (IDs[i] > 1) {
				System.out.println("Found "+IDs[i]+" spots with the same ID = "+i);
				ok = false;
			}
		}
		if (ok) {
			System.out.println("No duplicate ID found.");
		}

	}

}
