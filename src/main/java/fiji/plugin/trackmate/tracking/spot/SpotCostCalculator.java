package fiji.plugin.trackmate.tracking.spot;

import java.util.Map;

import fiji.plugin.trackmate.FeatureHolderUtils;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.tracking.TrackableObjectUtils;
import fiji.plugin.trackmate.tracking.costfunction.CostCalculator;

/**
 * Compute the cost to link two spots, in the default way for the TrackMate
 * trackmate.
 * <p>
 * This cost is calculated as follow:
 * <ul>
 * <li>The distance between the two spots <code>D</code> is calculated
 * <li>If the spots are separated by more than the distance cutoff, the cost is
 * set to be the blocking value. If not,
 * <li>For each feature in the map, a penalty <code>p</code> is calculated as
 * <code>p = 3 × α × |f1-f2| / (f1+f2)</code>, where <code>α</code> is the
 * factor associated to the feature in the map. This expression is such that:
 * <ul>
 * <li>there is no penalty if the 2 feature values <code>f1</code> and
 * <code>f2</code> are the same;
 * <li>that, with a factor of 1, the penalty if 1 is one value is the double of
 * the other;
 * <li>the penalty is 2 if one is 5 times the other one.
 * </ul>
 * <li>All penalties are summed, to form <code>P = (1 + ∑ p )</code>
 * <li>The cost is set to the square of the product: <code>C = ( D × P )²</code>
 * </ul>
 * For instance: if 2 spots differ by twice the value in a feature which is in
 * the penalty map with a factor of 1, they will <i>look</i> as if they were
 * twice as far.
 * 
 */
public class SpotCostCalculator implements CostCalculator<Spot> {

	@Override
	public double computeLinkingCostFor(Spot s0, Spot s1,
			double distanceCutOff, double blockingValue,
			Map<String, Double> featurePenalties) {

		final double d2 = TrackableObjectUtils.squareDistanceTo(s0, s1);

		// Distance threshold
		if (d2 > distanceCutOff * distanceCutOff) {
			return blockingValue;
		}

		double penalty = 1;
		for (final String feature : featurePenalties.keySet()) {
			final double ndiff = FeatureHolderUtils.normalizeDiffTo(s0, s1,
					feature);
			if (Double.isNaN(ndiff))
				continue;
			final double factor = featurePenalties.get(feature);
			penalty += factor * 1.5 * ndiff;
		}

		// Set score
		return d2 * penalty * penalty;
	}

}