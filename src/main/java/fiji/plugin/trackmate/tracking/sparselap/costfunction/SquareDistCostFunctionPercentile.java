package fiji.plugin.trackmate.tracking.sparselap.costfunction;

import net.imglib2.util.Util;
import fiji.plugin.trackmate.Spot;


/**
 * A cost function that returns cost equal to the square distance. Suited to
 * Brownian motion. Alternative cost is calculated through a percentile of the
 * specified costs.
 * 
 * @author Jean-Yves Tinevez - 2014
 * 
 */
public class SquareDistCostFunctionPercentile extends SquareDistCostFunction
{

	private final double percentile;

	public SquareDistCostFunctionPercentile( final double alternativeCostFactor, final double percentile )
	{
		super( alternativeCostFactor );
		if ( percentile < 0 || percentile > 1 ) { throw new IllegalArgumentException( "The percentile must be between 0 and 1. Was: " + percentile + "." ); }
		this.percentile = percentile;
	}

	@Override
	public double linkingCost( final Spot source, final Spot target )
	{
		return source.squareDistanceTo( target );
	}

	@Override
	public double aternativeCost( final double[] allCosts )
	{
		return alternativeCostFactor * Util.computePercentile( allCosts, percentile );
	}

}
