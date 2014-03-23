package fiji.plugin.trackmate.tracking;

import java.util.Map;

import fiji.plugin.trackmate.tracking.hungarian.AssignmentAlgorithm;
import fiji.plugin.trackmate.tracking.hungarian.JonkerVolgenantAlgorithm;
import fiji.plugin.trackmate.tracking.spot.SpotCollection;

public class FastLAPTracker extends LAPTracker {

	public FastLAPTracker( final SpotCollection spots, final Map< String, Object > settings )
	{
		super( spots, settings );
	}

	@Override
	protected AssignmentAlgorithm createAssignmentProblemSolver() {
		return new JonkerVolgenantAlgorithm();
	}
}
