package fiji.plugin.trackmate.tracking.oldlap;

import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.oldlap.hungarian.AssignmentAlgorithm;
import fiji.plugin.trackmate.tracking.oldlap.hungarian.JonkerVolgenantAlgorithm;

import java.util.Map;

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
