package fiji.plugin.trackmate.tracking;

import java.util.Map;

import fiji.plugin.trackmate.tracking.costfunction.CostCalculator;
import fiji.plugin.trackmate.tracking.hungarian.AssignmentAlgorithm;
import fiji.plugin.trackmate.tracking.hungarian.JonkerVolgenantAlgorithm;

public class FastLAPTracker<T extends TrackableObject> extends LAPTracker<T> {

	public FastLAPTracker( final CostCalculator<T> costCalculator, final TrackableObjectCollection<T> spots, final Map< String, Object > settings )
	{
		super( costCalculator, spots, settings );
	}

	@Override
	protected AssignmentAlgorithm createAssignmentProblemSolver() {
		return new JonkerVolgenantAlgorithm();
	}
}
