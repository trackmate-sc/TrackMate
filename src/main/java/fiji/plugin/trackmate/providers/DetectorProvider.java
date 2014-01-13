package fiji.plugin.trackmate.providers;

import java.util.ArrayList;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.DogDetectorFactory;
import fiji.plugin.trackmate.detection.DownsampleLogDetectorFactory;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.detection.ManualDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;

public class DetectorProvider extends AbstractProvider {

	/*
	 * BLANK CONSTRUCTOR
	 */

	protected LogDetectorFactory<?>	logDetectorFactory;
	protected DogDetectorFactory<?>	dogDetectorFactory;
	protected DownsampleLogDetectorFactory<?>	downsampleLogDetectorFactory;
	protected ManualDetectorFactory<?>			manualDetectorFactory;

	/**
	 * This provider provides the GUI with the spot detectors currently available in the
	 * current TrackMate version. Each detector is identified by a key String, which can be used
	 * to retrieve new instance of the detector, settings for the target detector and a
	 * GUI panel able to configure these settings.
	 * <p>
	 * If you want to add custom detectors to TrackMate GUI, a simple way is to extend this
	 * factory so that it is registered with the custom detectors and pass this
	 * extended provider to the {@link TrackMate} trackmate.
	 * @param settings
	 * @param model
	 */
	public DetectorProvider() {
		registerDetectors();
		currentKey = LogDetectorFactory.DETECTOR_KEY;
	}


	/*
	 * METHODS
	 */

	/**
	 * Registers the standard detectors shipped with TrackMate, and instantiates
	 * their factories.
	 */
	@SuppressWarnings("rawtypes")
	protected void registerDetectors() {
		// Instances
		this.dogDetectorFactory = new DogDetectorFactory();
		this.logDetectorFactory = new LogDetectorFactory();
		this.manualDetectorFactory = new ManualDetectorFactory();
		this.downsampleLogDetectorFactory = new DownsampleLogDetectorFactory();
		// keys
		keys = new ArrayList<String>(4);
		keys.add(LogDetectorFactory.DETECTOR_KEY);
		keys.add(DogDetectorFactory.DETECTOR_KEY);
		keys.add(DownsampleLogDetectorFactory.DETECTOR_KEY);
		keys.add(ManualDetectorFactory.DETECTOR_KEY);
		// names
		names = new ArrayList<String>(4);
		names.add(LogDetectorFactory.NAME);
		names.add(DogDetectorFactory.NAME);
		names.add(DownsampleLogDetectorFactory.NAME);
		names.add(ManualDetectorFactory.NAME);
		// infoTexts
		infoTexts = new ArrayList<String>(4);
		infoTexts.add(LogDetectorFactory.INFO_TEXT);
		infoTexts.add(DogDetectorFactory.INFO_TEXT);
		infoTexts.add(DownsampleLogDetectorFactory.INFO_TEXT);
		infoTexts.add(ManualDetectorFactory.INFO_TEXT);
	}

	/**
	 * Returns a new instance of the target detector identified by the key
	 * parameter. If the key is unknown to this provider, return
	 * <code>null</code>.
	 */
	@SuppressWarnings("rawtypes")
	public SpotDetectorFactory getDetectorFactory() {

		if (currentKey.equals(LogDetectorFactory.DETECTOR_KEY)) {
			return logDetectorFactory;

		} else if (currentKey.equals(DogDetectorFactory.DETECTOR_KEY)){
			return dogDetectorFactory;

		} else if (currentKey.equals(DownsampleLogDetectorFactory.DETECTOR_KEY)) {
			return downsampleLogDetectorFactory;

		} else if (currentKey.equals(ManualDetectorFactory.DETECTOR_KEY)) {
			return manualDetectorFactory;

		} else {
			return null;
		}
	}

}
