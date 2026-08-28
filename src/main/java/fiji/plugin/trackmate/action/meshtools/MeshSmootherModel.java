/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
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

	@Override
	public String toString()
	{
		final StringBuilder str = new StringBuilder( super.toString() + '\n' );
		str.append( String.format( " - %s: %.2f\n", "µ", mu ) );
		str.append( String.format( " - %s: %.2f\n", "λ", lambda ) );
		str.append( String.format( " - %s: %d\n", "N iterations", nIters ) );
		str.append( String.format( " - %s: %s\n", "weights", weightType ) );
		return str.toString();
	}
}
