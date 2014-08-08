package fiji.plugin.trackmate.tracking.sparselap.costfunction;

import net.imglib2.util.Util;
import fiji.plugin.trackmate.Spot;

/**
 * A cost function that returns cost equal to the square distance. Suited to
 * Brownian motion. Alternative cost is calculated as a factor times the max
 * cost.
 * 
 * @author Jean-Yves Tinevez - 2014
 * 
 */
public class SquareDistCostFunction implements CostFunction< Spot, Spot >
{

	protected final double alternativeCostFactor;

	public SquareDistCostFunction( final double alternativeCostFactor )
	{
		if ( alternativeCostFactor <= 0 ) { throw new IllegalArgumentException( "The alternative cost factor must be greater than 0. Was: " + alternativeCostFactor + "." ); }
		this.alternativeCostFactor = alternativeCostFactor;
	}

	@Override
	public double linkingCost( final Spot source, final Spot target )
	{
		return source.squareDistanceTo( target );
	}

	@Override
	public double aternativeCost( final double[] allCosts )
	{
		return alternativeCostFactor * Util.computeMax( allCosts );
	}
}
