package fiji.plugin.trackmate.tracking;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_MAX_DISTANCE;

import java.util.Map;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.tracking.hungarian.AssignmentAlgorithm;
import fiji.plugin.trackmate.tracking.hungarian.MunkresKuhnAlgorithm;

@Plugin(type = SpotTracker.class)
public class FastLAPTracker extends LAPTracker {

	public static final String TRACKER_KEY = "FAST_LAP_TRACKER";
	public static final String NAME ="LAP Tracker";
	public static final String INFO_TEXT = "<html>" +
			"This tracker is based on the Linear Assignment Problem mathematical framework. <br>" +
			"Its implementation is adapted from the following paper: <br>" +
			"<i>Robust single-particle tracking in live-cell time-lapse sequences</i> - <br>" +
			"Jaqaman <i> et al.</i>, 2008, Nature Methods. <br>" +
			"<p>" +
			"Tracking happens in 2 steps: First spots are linked from frame to frame to <br>" +
			"build track segments. These track segments are investigated in a second step <br>" +
			"for gap-closing (missing detection), splitting and merging events.  <br> " +
			"<p>" +
			"Linking costs are proportional to the square distance between source and  <br> " +
			"target spots, which makes this tracker suitable for Brownian motion.  <br> " +
			"Penalties can be set to favor linking between spots that have similar  <br> " +
			"features. " +
			"<p>" +
			"Solving the LAP relies on the Munkres-Kuhn solver, <br> " +
			"that solves an assignment problem in O(n^3) instead of O(n^4)." +
			" </html>";

	public FastLAPTracker(final Logger logger) {
		super(logger);
	}

	public FastLAPTracker() {
		this(Logger.VOID_LOGGER);
	}

	@Override
	protected AssignmentAlgorithm createAssignmentProblemSolver() {
		return new MunkresKuhnAlgorithm();
	}

	@Override
	public String toString() {
		return NAME;
	}

	@Override
	public String getKey() {
		return TRACKER_KEY;
	}

	@Override
	public String getInfo() {
		return INFO_TEXT;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void toString(Map<String, Object> sm, StringBuilder str) {
		str.append("  Linking conditions:\n");
		str.append(String.format("    - max distance: %.1f\n", (Double) sm.get(KEY_LINKING_MAX_DISTANCE)));
		str.append(LAPUtils.echoFeaturePenalties((Map<String, Double>) sm.get(KEY_LINKING_FEATURE_PENALTIES)));

		if ((Boolean) sm.get(KEY_ALLOW_GAP_CLOSING)) {
			str.append("  Gap-closing conditions:\n");
			str.append(String.format("    - max distance: %.1f\n", (Double) sm.get(KEY_GAP_CLOSING_MAX_DISTANCE)));
			str.append(String.format("    - max frame gap: %d\n", (Integer) sm.get(KEY_GAP_CLOSING_MAX_FRAME_GAP)));
			str.append(LAPUtils.echoFeaturePenalties((Map<String, Double>) sm.get(KEY_GAP_CLOSING_FEATURE_PENALTIES)));
		} else {
			str.append("  Gap-closing not allowed.\n");
		}

		if ((Boolean) sm.get(KEY_ALLOW_TRACK_SPLITTING)) {
			str.append("  Track splitting conditions:\n");
			str.append(String.format("    - max distance: %.1f\n", (Double) sm.get(KEY_SPLITTING_MAX_DISTANCE)));
			str.append(LAPUtils.echoFeaturePenalties((Map<String, Double>) sm.get(KEY_SPLITTING_FEATURE_PENALTIES)));
		} else {
			str.append("  Track splitting not allowed.\n");
		}

		if ((Boolean) sm.get(KEY_ALLOW_TRACK_MERGING)) {
			str.append("  Track merging conditions:\n");
			str.append(String.format("    - max distance: %.1f\n", (Double) sm.get(KEY_MERGING_MAX_DISTANCE)));
			str.append(LAPUtils.echoFeaturePenalties((Map<String, Double>) sm.get(KEY_MERGING_FEATURE_PENALTIES)));
		} else {
			str.append("  Track merging not allowed.\n");
		}
	}

}
