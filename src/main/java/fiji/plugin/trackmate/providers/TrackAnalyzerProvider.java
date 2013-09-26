package fiji.plugin.trackmate.providers;

import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.features.track.TrackBranchingAnalyzer;
import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.features.track.TrackLocationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackSpeedStatisticsAnalyzer;

/**
 * A provider for the track analyzers provided in the GUI.
 * <p>
 * Concrete implementation must declare what features they can compute numerically,
 * using the method {@link #getFeaturesForKey(String)}.
 * <p>
 * Feature key names are for historical reason all capitalized in an enum manner. For instance: POSITION_X,
 * MAX_INTENSITY, etc... They must be suitable to be used as a attribute key in an xml file.
 */
public class TrackAnalyzerProvider {


	/** The detector names, in the order they will appear in the GUI.
	 * These names will be used as keys to access relevant track analyzer classes.  */
	protected List<String> names;
	/**
	 * The {@link TrackIndexAnalyzer} has an internal state useful for lazy
	 * computation of track features.
	 */
	protected TrackIndexAnalyzer trackIndexAnalyzer;
	protected TrackDurationAnalyzer trackDurationAnalyzer;
	protected TrackBranchingAnalyzer trackBranchingAnalyzer;
	protected TrackSpeedStatisticsAnalyzer trackSpeedStatisticsAnalyzer;
	protected TrackLocationAnalyzer trackLocationAnalyzer;

	/*
	 * BLANK CONSTRUCTOR
	 */

	/**
	 * This provider provides the GUI with the model trackFeatureAnalyzers currently available in the
	 * TrackMate trackmate. Each trackFeatureAnalyzer is identified by a key String, which can be used
	 * to retrieve new instance of the trackFeatureAnalyzer.
	 * <p>
	 * If you want to add custom trackFeatureAnalyzers to TrackMate, a simple way is to extend this
	 * factory so that it is registered with the custom trackFeatureAnalyzers and provide this
	 * extended factory to the {@link TrackMate} trackmate.
	 */
	public TrackAnalyzerProvider() {
		registerTrackFeatureAnalyzers();
	}


	/*
	 * METHODS
	 */

	/**
	 * Instantiates and registers the standard trackFeatureAnalyzes shipped with
	 * TrackMate.
	 */
	protected void registerTrackFeatureAnalyzers() {
		this.trackIndexAnalyzer = new TrackIndexAnalyzer();
		this.trackDurationAnalyzer = new TrackDurationAnalyzer();
		this.trackBranchingAnalyzer = new TrackBranchingAnalyzer();
		this.trackSpeedStatisticsAnalyzer = new TrackSpeedStatisticsAnalyzer();
		this.trackLocationAnalyzer = new TrackLocationAnalyzer();
		// Names
		names = new ArrayList<String>(4);
		names.add(TrackBranchingAnalyzer.KEY);
		names.add(TrackDurationAnalyzer.KEY);
		names.add(TrackSpeedStatisticsAnalyzer.KEY);
		names.add(TrackLocationAnalyzer.KEY);
		names.add(TrackIndexAnalyzer.KEY);
	}

	/**
	 * Returns the instance of the target trackFeatureAnalyzer identified by the
	 * key parameter. If the key is unknown to this factory, <code>null</code>
	 * is returned.
	 */
	public TrackAnalyzer getTrackFeatureAnalyzer(final String key) {

		if (key.equals(TrackDurationAnalyzer.KEY)) {
			return trackDurationAnalyzer;

		} else if (key.equals(TrackBranchingAnalyzer.KEY)) {
			return trackBranchingAnalyzer;

		} else if (key.equals(TrackSpeedStatisticsAnalyzer.KEY)) {
			return trackSpeedStatisticsAnalyzer;

		} else if (key.equals(TrackLocationAnalyzer.KEY)) {
			return trackLocationAnalyzer;

		} else if (key.equals(TrackIndexAnalyzer.KEY)) {
			return trackIndexAnalyzer;

		} else {
			return null;
		}
	}

	/**
	 * Returns a list of the trackFeatureAnalyzer names available through this
	 * provider.
	 */
	public List<String> getAvailableTrackFeatureAnalyzers() {
		return names;
	}

}
