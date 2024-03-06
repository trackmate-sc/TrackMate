package fiji.plugin.trackmate.tracking.jaqaman;

import java.util.Map;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.jaqaman.costfunction.CostFunction;
import fiji.plugin.trackmate.tracking.jaqaman.costfunction.OverlapFeaturePenaltyCostFunction;
import fiji.plugin.trackmate.tracking.jaqaman.costfunction.OverlapCostFunction;

public class SparseLAPFrameToFrameOverlapTracker extends SparseLAPFrameToFrameTracker 
{

	/*
	 * CONSTRUCTOR
	 */
	
	
	public SparseLAPFrameToFrameOverlapTracker( final SpotCollection spots, final Map< String, Object > settings )
	{
		super(spots, settings);
	}

	/*
	 * METHODS
	 */

	/**
	 * Creates a suitable cost function.
	 *
	 * @param featurePenalties
	 *            feature penalties to base costs on. Can be <code>null</code>.
	 * @return a new {@link CostFunction}
	 */
	protected CostFunction< Spot, Spot > getCostFunction( final Map< String, Double > featurePenalties )
	{
		if ( null == featurePenalties || featurePenalties.isEmpty() )
			return new OverlapCostFunction();

		return new OverlapFeaturePenaltyCostFunction( featurePenalties );
	}
}
