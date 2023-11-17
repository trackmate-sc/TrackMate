package fiji.plugin.trackmate.action.meshtools;

import net.imglib2.mesh.alg.TaubinSmoothing.TaubinWeightType;

public class MeshSmootherModel
{

	private int nIters = 10;

	private double mu = 0.5;

	private double lambda = -0.53;

	private TaubinWeightType weightType = TaubinWeightType.NAIVE;

	public void setWeightType( final TaubinWeightType weightType )
	{
		this.weightType = weightType;
	}

	public void setMu( final double mu )
	{
		this.mu = Math.min( 1., Math.max( 0, mu ) );
	}

	public void setLambda( final double lambda )
	{
		this.lambda = Math.min( 0., Math.max( -1., lambda ) );
	}

	public void setNIters( final int nIters )
	{
		this.nIters = Math.max( 0, nIters );
	}

	public double getMu()
	{
		return mu;
	}

	public double getLambda()
	{
		return lambda;
	}

	public int getNIters()
	{
		return nIters;
	}

	public TaubinWeightType getWeightType()
	{
		return weightType;
	}

	/**
	 * Ad-hoc method setting parameters for little smoothing (close to 0) or a
	 * lot of smoothing (close to 1).
	 * 
	 * @param smoothing
	 *            the smoothing parameter.
	 */
	public void setSmoothing( final double smoothing )
	{
		setMu( Math.max( 0, Math.min( 0.97, smoothing ) ) );
		setLambda( -mu - 0.03 );
	}
}
