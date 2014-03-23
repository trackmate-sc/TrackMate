package fiji.plugin.trackmate.tracking.factories;

import java.util.Map;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.tracking.spot.DefaultSpotCollection;
import fiji.plugin.trackmate.tracking.spot.SpotCostCalculator;
import fiji.plugin.trackmate.tracking.trackers.FastLAPTracker;
import fiji.plugin.trackmate.tracking.trackers.Tracker;

@Plugin(type = TrackerFactory.class)
public class FastLAPTrackerFactory extends
		LAPTrackerFactory {
	public static final String TRACKER_KEY = "FAST_LAP_TRACKER";

	public static final String NAME = "LAP Tracker";

	public static final String INFO_TEXT = "<html>"
			+ "This tracker is based on the Linear Assignment Problem mathematical framework. <br>"
			+ "Its implementation is adapted from the following paper: <br>"
			+ "<i>Robust single-particle tracking in live-cell time-lapse sequences</i> - <br>"
			+ "Jaqaman <i> et al.</i>, 2008, Nature Methods. <br>"
			+ "<p>"
			+ "Tracking happens in 2 steps: First spots are linked from frame to frame to <br>"
			+ "build track segments. These track segments are investigated in a second step <br>"
			+ "for gap-closing (missing detection), splitting and merging events.  <br> "
			+ "<p>"
			+ "Linking costs are proportional to the square distance between source and  <br> "
			+ "target spots, which makes this tracker suitable for Brownian motion.  <br> "
			+ "Penalties can be set to favor linking between spots that have similar  <br> "
			+ "features. " + "<p>"
			+ "Solving the LAP relies on the Jonker-Volgenant solver, <br> "
			+ "that solves an assignment problem in O(n^3) instead of O(n^4)."
			+ " </html>";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getKey() {
		return TRACKER_KEY;
	}

	@Override
	public String getInfoText() {
		return INFO_TEXT;
	}

	@Override
	public Tracker<Spot> create(final DefaultSpotCollection spots,
			final Map<String, Object> settings) {
		return new FastLAPTracker<Spot>(new SpotCostCalculator(), spots, settings);
	}
}
