package fiji.plugin.trackmate.tracking.sparselap.costmatrix;

import fiji.plugin.trackmate.tracking.sparselap.linker.SparseCostMatrix;

import java.util.List;

import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.OutputAlgorithm;

/**
 * Interface for function that can generate a {@link SparseCostMatrix} from
 * assignment candidates.
 * 
 * @author Jean-Yves Tinevez
 * 
 */
public interface CostMatrixCreator< K extends Comparable< K >, J extends Comparable< J >> extends Benchmark, OutputAlgorithm< SparseCostMatrix >
{

	/**
	 * Returns the list of sources in the generated cost matrix.
	 * 
	 * @return the list of object, such that <code>sourceList.get( i )</code> is
	 *         the source corresponding to the row <code>i</code> in the
	 *         generated cost matrix.
	 * @see #getTargetList()
	 * @see #getResult()
	 */
	public List< K > getSourceList();

	/**
	 * Returns the list of targets in the generated cost matrix.
	 * 
	 * @return the list of objects, such that <code>targetList.get( j )</code>
	 *         is the target corresponding to the column <code>j</code> in the
	 *         generated cost matrix.
	 * @see #getSourceList()
	 * @see #getResult()
	 */
	public List< J > getTargetList();

	/**
	 * Returns the value of the no-linking alternative cost for the specified
	 * source.
	 * 
	 * @param source
	 *            the source object.
	 * @return the alternative cost. Belongs to the list returned by
	 *         {@link #getSourceList()}.
	 */
	public double getAlternativeCostForSource( K source );

	/**
	 * Returns the value of the no-linking alternative cost for the specified
	 * target.
	 * 
	 * @param target
	 *            the target object. Belongs to the list returned by
	 *            {@link #getTargetList()}.
	 * @return the alternative cost.
	 */
	public double getAlternativeCostForTarget( J target );

}
