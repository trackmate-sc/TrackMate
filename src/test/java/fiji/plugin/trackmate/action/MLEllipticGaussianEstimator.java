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
import net.imglib2.algorithm.localization.EllipticGaussianOrtho;
import net.imglib2.algorithm.localization.Observation;
import net.imglib2.algorithm.localization.StartPointEstimator;
import net.imglib2.util.Util;

/**
 * An fit initializer suitable for the fitting of elliptic orthogonal gaussians
 * ({@link EllipticGaussianOrtho}, ellipse axes must be parallel to image axes)
 * functions on n-dimensional image data. It uses plain maximum-likelihood
 * estimator for a normal distribution.
 * <p>
 * The problem dimensionality is specified at construction by the length of the
 * typical sigma array.
 * <p>
 * The domain span size is simply set to be
 * <code>1 + 2 x ceil(typical_sigma)</code> in each dimension.
 * <p>
 * Parameters estimation returned by
 * {@link #initializeFit(Localizable, Observation)} is based on
 * maximum-likelihood estimator for a normal distribution, which requires the
 * background of the image (out of peaks) to be close to 0. Returned parameters
 * are ordered as follow:
 * 
 * <pre>
 * 0 → ndims-1		x₀ᵢ
 * ndims.			A
 * ndims+1 → 2 × ndims	bᵢ = 1 / σᵢ²
 * </pre>
 * 
 * @see EllipticGaussianOrtho
 * @author Jean-Yves Tinevez - 2013
 * 
 */
public class MLEllipticGaussianEstimator implements StartPointEstimator
{

	private final double[] sigmas;

	private final int nDims;

	private final long[] span;

	/**
	 * Instantiates a new elliptic gaussian estimator.
	 * 
	 * @param typicalSigmas
	 *            the typical sigmas of the peak to estimate (one element per
	 *            dimension).
	 */
	public MLEllipticGaussianEstimator( final double[] typicalSigmas )
	{
		this.sigmas = typicalSigmas;
		this.nDims = sigmas.length;
		this.span = new long[ nDims ];
		for ( int d = 0; d < nDims; d++ )
			span[ d ] = ( long ) Math.ceil( 2 * sigmas[ d ] ) + 1;
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

		final double[] startParam = new double[ 2 * nDims + 1 ];
		final double[][] X = data.X;
		final double[] I = data.I;

		final double[] Xsum = new double[ nDims ];
		for ( int d = 0; d < nDims; d++ )
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

		startParam[ nDims ] = maxI;

		for ( int d = 0; d < nDims; d++ )
			startParam[ d ] = Xsum[ d ] / Isum;

		for ( int d = 0; d < nDims; d++ )
			startParam[ nDims + d + 1 ] = 1. / ( sigmas[ d ] * sigmas[ d ] );

		return startParam;
	}
}
