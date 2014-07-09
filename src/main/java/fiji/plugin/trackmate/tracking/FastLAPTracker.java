package fiji.plugin.trackmate.tracking;

import java.util.Map;

import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.hungarian.AssignmentAlgorithm;
import fiji.plugin.trackmate.tracking.hungarian.JonkerVolgenantAlgorithm;

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
