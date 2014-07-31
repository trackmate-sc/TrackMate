package fiji.plugin.trackmate.tracking.sparselap.linker;

/**
 * Interface representing a function that can calculate the cost to link a
 * source object to a target object.
 * 
 * @author Jean-Yves Tinevez - 2014
 * 
 * @param <K>
 *            the type of the objects to link.
 */
public interface CostFunction< K >
{

	/**
	 * Returns the cost to link two objects.
	 * 
	 * @param source
	 *            the source spot.
	 * @param target
	 *            the target spot.
	 * @return the cost as a double.
	 */
	public double linkingCost( K source, K target );

}
