package fiji.plugin.trackmate.tracking.costfunction;

import java.util.List;

import Jama.Matrix;
import fiji.plugin.trackmate.tracking.TrackableObject;

/**
 * Interface for cost function classes, which take a {@link Matrix} and
 * fill in values according to the cost function.
 * 
 * @author Nicholas Perry
 *
 */
public interface CostFunctions<T extends TrackableObject> {

	/** 
	 * Return a cost matrix using the information given at construction for
	 * the two Spot lists given here.
	 * <p>
	 * We need to use a list, for the matrix index will reflect the spot position
	 * in the lists.
	 */
	public Matrix getCostFunction(final List<T> t0, final List<T> t1);
	
}
