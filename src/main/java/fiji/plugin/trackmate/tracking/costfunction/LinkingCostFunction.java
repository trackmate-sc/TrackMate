package fiji.plugin.trackmate.tracking.costfunction;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_BLOCKING_VALUE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;

import java.util.List;
import java.util.Map;

import Jama.Matrix;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.tracking.LAPTracker;
import fiji.plugin.trackmate.tracking.LAPUtils;
import fiji.plugin.trackmate.tracking.TrackableObject;

/**
 * <p>
 * Linking cost function used with {@link LAPTracker}.
 * 
 * <p>
 * The <b>cost function</b> is determined by the default equation in the
 * TrackMate trackmate, see below.
 * <p>
 * It slightly differs from the Jaqaman article, see equation (3) in the paper.
 * 
 * @see LAPUtils#computeLinkingCostFor(Spot, Spot, double, double,
 *      java.util.Map)
 * 
 * @author Nicholas Perry
 * @author Jean-Yves Tinevez
 * 
 */
public class LinkingCostFunction<T extends TrackableObject> implements
		CostFunctions<T> {

	protected final double maxDist;
	protected final Map<String, Double> featurePenalties;
	protected final double blockingValue;
	protected CostCalculator<T> costCalculator;

	@SuppressWarnings("unchecked")
	public LinkingCostFunction(final CostCalculator<T> costCalculator,
			final Map<String, Object> settings) {
		this.maxDist = (Double) settings.get(KEY_LINKING_MAX_DISTANCE);
		this.featurePenalties = (Map<String, Double>) settings
				.get(KEY_LINKING_FEATURE_PENALTIES);
		this.blockingValue = (Double) settings.get(KEY_BLOCKING_VALUE);
		this.costCalculator = costCalculator;
	}

	@Override
	public Matrix getCostFunction(final List<T> t0, final List<T> t1) {
		T s0 = null; // Spot in t0
		T s1 = null; // Spot in t1
		final Matrix m = new Matrix(t0.size(), t1.size());

		for (int i = 0; i < t0.size(); i++) {

			s0 = t0.get(i);

			for (int j = 0; j < t1.size(); j++) {

				s1 = t1.get(j);
				final double cost = costCalculator.computeLinkingCostFor(s0,
						s1, maxDist, blockingValue, featurePenalties);
				m.set(i, j, cost);
			}
		}

		return m;
	}

}
