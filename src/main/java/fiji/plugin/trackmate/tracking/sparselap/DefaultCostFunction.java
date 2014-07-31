package fiji.plugin.trackmate.tracking.sparselap;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.tracking.sparselap.linker.CostFunction;


/**
 * A cost function that returns cost equal to the square distance. Suited to
 * Brownian motion.
 * 
 * @author Jean-Yves Tinevez - 2014
 * 
 */
public class DefaultCostFunction implements CostFunction< Spot >
{

	@Override
	public double linkingCost( final Spot source, final Spot target )
	{
		return source.squareDistanceTo( target );
	}

}
