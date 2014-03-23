package fiji.plugin.trackmate.tracking.costfunction;

import java.util.Map;

import fiji.plugin.trackmate.tracking.TrackableObject;
import fiji.plugin.trackmate.tracking.TrackableObjectUtils;

/**
 * Simple Cost-Calculation based on square distance between two objects.
 */
public class SquareDistanceCostCalculator<T extends TrackableObject> implements
		CostCalculator<T> {

	@Override
	public double computeLinkingCostFor(T s0, T s1, double distanceCutOff,
			double blockingValue, Map<String, Double> featurePenalties) {

		return TrackableObjectUtils.squareDistanceTo(s0, s1);
	}

}