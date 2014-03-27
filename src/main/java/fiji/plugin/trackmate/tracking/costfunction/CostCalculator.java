package fiji.plugin.trackmate.tracking.costfunction;

import java.util.Map;

import fiji.plugin.trackmate.tracking.TrackableObject;

/**
 * Calculator to calculate the linking cost for two TrackableObjects
 * 
 * @author Christian Dietz, University of Konstanz
 */
public interface CostCalculator<T extends TrackableObject> {
	double computeLinkingCostFor(final T t0, final T t1,
			final double distanceCutOff, final double blockingValue,
			final Map<String, Double> featurePenalties);
}