package fiji.plugin.trackmate.providers;

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
public class TrackAnalyzerProvider extends AbstractFeatureAnalyzerProvider<TrackAnalyzer> {

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
	 * This provider provides the GUI with the model trackFeatureAnalyzers
	 * currently available in the TrackMate trackmate. Each trackFeatureAnalyzer
	 * is identified by a key String, which can be used to retrieve new instance
	 * of the trackFeatureAnalyzer.
	 * <p>
	 * If you want to add custom trackFeatureAnalyzers to TrackMate, a simple
	 * way is to extend this factory so that it is registered with the custom
	 * trackFeatureAnalyzers and provide this extended factory to the
	 * {@link TrackMate} trackmate.
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
		this.trackBranchingAnalyzer = new TrackBranchingAnalyzer();
		this.trackSpeedStatisticsAnalyzer = new TrackSpeedStatisticsAnalyzer();
		this.trackLocationAnalyzer = new TrackLocationAnalyzer();
		// Duration analyzer is currently disabled.
		this.trackDurationAnalyzer = new TrackDurationAnalyzer();

		registerAnalyzer(TrackBranchingAnalyzer.KEY, trackBranchingAnalyzer);
		registerAnalyzer(TrackSpeedStatisticsAnalyzer.KEY, trackSpeedStatisticsAnalyzer);
		registerAnalyzer(TrackLocationAnalyzer.KEY, trackLocationAnalyzer);
		registerAnalyzer(TrackIndexAnalyzer.KEY, trackIndexAnalyzer);
	}

}
