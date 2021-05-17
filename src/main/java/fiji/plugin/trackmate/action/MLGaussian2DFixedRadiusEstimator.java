/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2020 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Grant Harris, Stefan Helfrich, Mark Hiner,
 * Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey, Melissa Linkert,
 * Mark Longair, Brian Northan, Nick Perry, Curtis Rueden, Johannes Schindelin,
 * Jean-Yves Tinevez and Michael Zinsmaier.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.action;

import net.imglib2.Localizable;
import net.imglib2.algorithm.localization.Observation;
import net.imglib2.algorithm.localization.StartPointEstimator;
import net.imglib2.util.Util;

/**
 * An fit initializer suitable for the fitting of elliptic orthogonal gaussians
 * ({@link Gaussian3D}.
 */
public class MLGaussian2DFixedRadiusEstimator implements StartPointEstimator
{

	private final long[] span;

	/**
	 * Instantiates a new elliptic gaussian estimator.
	 * 
	 * @param typicalSigma
	 *            the typical sigmas of the peak to estimate (one element per
	 *            dimension).
	 */
	public MLGaussian2DFixedRadiusEstimator( final double typicalSigma )
	{
		this.span = new long[ 2 ];
		for ( int d = 0; d < 2; d++ )
			span[ d ] = ( long ) Math.ceil( 2. * typicalSigma ) + 1;
	}

	/*
	 * METHODS
	 */

	@Override
	public String toString()
	{
		return "Maximum-likelihood estimator for orthogonal elliptic gaussian peaks. Span = " + Util.printCoordinates( span );
	}

	@Override
	public long[] getDomainSpan()
	{
		return span;
	}

	@Override
	public double[] initializeFit( final Localizable point, final Observation data )
	{

		final double[] startParam = new double[ 3 ];
		final double[][] X = data.X;
		final double[] I = data.I;

		final double[] Xsum = new double[ 2 ];
		for ( int d = 0; d < 2; d++ )
		{
			Xsum[ d ] = 0;
			for ( int i = 0; i < X.length; i++ )
				Xsum[ d ] += X[ i ][ d ] * I[ i ];
		}

		double Isum = 0.;
		double maxI = Double.NEGATIVE_INFINITY;
		for ( int i = 0; i < X.length; i++ )
		{
			Isum += I[ i ];
			if ( I[ i ] > maxI )
				maxI = I[ i ];
		}

		startParam[ 2 ] = maxI;

		for ( int d = 0; d < 2; d++ )
			startParam[ d ] = Xsum[ d ] / Isum;

		return startParam;
	}
}
