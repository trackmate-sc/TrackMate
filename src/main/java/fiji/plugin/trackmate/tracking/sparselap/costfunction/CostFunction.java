package fiji.plugin.trackmate.tracking.sparselap.costfunction;

/**
 * Interface representing a function that can calculate the cost to link a
 * source object to a target object.
 * 
 * @author Jean-Yves Tinevez - 2014
 * 
 * @param <K>
 *            the type of the sources.
 * @param <J>
 *            the type of the targets.
 */
public interface CostFunction< K, J >
{

	/**
	 * Returns the cost to link two objects.
	 * 
	 * @param source
	 *            the source object.
	 * @param target
	 *            the target object.
	 * @return the cost as a double.
	 */
	public double linkingCost( K source, J target );

}
