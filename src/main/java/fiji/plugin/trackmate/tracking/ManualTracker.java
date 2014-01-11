package fiji.plugin.trackmate.tracking;

import java.util.Map;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;

@Plugin(type = SpotTracker.class)
public class ManualTracker implements SpotTracker {

	public static final String TRACKER_KEY = "MANUAL_TRACKER";
	public static final String NAME = "Manual tracking";
	public static final String INFO_TEXT =  "<html>" +
			"Choosing this tracker skips the automated tracking step <br>" +
			"and keeps the current annotation.</html>";


	@Override
	public SimpleWeightedGraph<Spot, DefaultWeightedEdge> getResult() {
		return null;
	}

	@Override
	public boolean checkInput() {
		return true;
	}

	@Override
	public boolean process() {
		return true;
	}

	@Override
	public String getErrorMessage() {
		return null;
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
	public String toString() {
		return NAME;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void setTarget(final SpotCollection spots, final Map<String, Object> settings) {}

	@Override
	public void setLogger(final Logger logger) {
		// not needed
	}

	@Override
	public void toString(Map<String, Object> sm, StringBuilder str) {
		str.append("  Manual tracking.\n");
	}

}
