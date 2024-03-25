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
package fiji.plugin.trackmate.action.fit;

import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math3.fitting.leastsquares.ParameterValidator;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.DetectionUtils;
import ij.ImagePlus;
import net.imglib2.Point;
import net.imglib2.util.Util;

/**
 * 2D case!
 */
public class SpotGaussianFitter2D extends AbstractSpotFitter
{

	public SpotGaussianFitter2D( final ImagePlus imp, final int channel )
	{
		super( imp, channel );
		assert DetectionUtils.is2D( imp );
	}

	@Override
	public void process( final Iterable< Spot > spots, final Logger logger )
	{
		super.process( spots, logger );
	}

	@Override
	public void fit( final Spot spot )
	{
		final int frame = spot.getFeature( Spot.FRAME ).intValue();
		final double sigma = spot.getFeature( Spot.RADIUS ) / Math.sqrt( 2. );
		final double pixelSigma = sigma / calibration[ 0 ];
		final double x0 = spot.getDoublePosition( 0 ) / calibration[ 0 ];
		final double y0 = spot.getDoublePosition( 1 ) / calibration[ 1 ];
		final long span = ( long ) Math.ceil( 2. * pixelSigma ) + 1;
		final Observation obs = gatherObservationData(
				new Point(
						Math.round( x0 ),
						Math.round( y0 ) ),
				new long[] { span, span },
				frame );
		clipBackground( obs );

		final double bstart = 1. / ( 2 * pixelSigma * pixelSigma );
		final double maxSigma = 2. * pixelSigma;
		final double minB = 1. / ( 2 * maxSigma * maxSigma );
		final MyGaussian2D gauss = new MyGaussian2D( obs.pos, minB );
		final double ampstart = Util.max( obs.values );
		final LeastSquaresProblem lsq = new LeastSquaresBuilder()
				.start( new double[] { x0, y0, ampstart, bstart } )
				.model( gauss )
				.parameterValidator( gauss )
				.target( obs.values )
				.lazyEvaluation( false )
				.maxEvaluations( 1000 )
				.maxIterations( 1000 )
				.build();

		try
		{
			final LeastSquaresOptimizer.Optimum optimum = optimizer.optimize( lsq );
			final RealVector fit = optimum.getPoint();

			final double fitX = fit.getEntry( 0 ) * calibration[ 0 ];
			final double fitY = fit.getEntry( 1 ) * calibration[ 1 ];
			final double fitSigma = 1. / Math.sqrt( 2. * fit.getEntry( 3 ) );
			final double fitRadius = fitSigma * Math.sqrt( 2. ) * calibration[ 0 ];

			spot.putFeature( Spot.POSITION_X, fitX );
			spot.putFeature( Spot.POSITION_Y, fitY );
			spot.putFeature( Spot.RADIUS, fitRadius );
		}
		catch ( final TooManyEvaluationsException tme )
		{}

	}

	/**
	 * <pre>
	 k = 0  	- x
	 k = 1  	- y
	 k = 2     	- A
	 k = 3 		- b
	 f(x) = A × exp( - S )
	 S = b × ∑ (xᵢ - x₀ᵢ)²
	 * </pre>
	 */
	private static class MyGaussian2D implements MultivariateJacobianFunction, ParameterValidator
	{

		private final long[][] pos;

		private final double minB;

		public MyGaussian2D( final long[][] pos, final double minB )
		{
			this.pos = pos;
			this.minB = minB;
		}

		@Override
		public Pair< RealVector, RealMatrix > value( final RealVector point )
		{
			// Unpack values
			final double x0 = point.getEntry( 0 );
			final double y0 = point.getEntry( 1 );
			final double A = point.getEntry( 2 );
			final double b = point.getEntry( 3 );

			// Function & Grad values.
			final double[] vals = new double[ pos[ 0 ].length ];
			final double[][] grad = new double[ pos[ 0 ].length ][ 4 ];
			for ( int i = 0; i < vals.length; i++ )
			{
				final long x = pos[ 0 ][ i ];
				final long y = pos[ 1 ][ i ];
				final double dx = ( x - x0 );
				final double dy = ( y - y0 );
				final double sumSq = ( dx * dx + dy * dy );
				final double E = Math.exp( -b * sumSq );
				vals[ i ] = A * E;

				// With respect to x0
				grad[ i ][ 0 ] = A * b * E * 2. * dx;
				// With respect to y0
				grad[ i ][ 1 ] = A * b * E * 2. * dy;
				// With respect to A
				grad[ i ][ 2 ] = E;
				// With respect to b
				grad[ i ][ 3 ] = -A * E * sumSq;
			}
			final ArrayRealVector out = new ArrayRealVector( vals );
			final Array2DRowRealMatrix jacobian = new Array2DRowRealMatrix( grad, false );
			return new Pair<>( out, jacobian );
		}

		@Override
		public RealVector validate( final RealVector params )
		{
			params.setEntry( 2, Math.abs( params.getEntry( 2 ) ) );
			final double b = Math.max( minB, Math.abs( params.getEntry( 3 ) ) );
			params.setEntry( 3, b );
			return params;
		}
	}
}
