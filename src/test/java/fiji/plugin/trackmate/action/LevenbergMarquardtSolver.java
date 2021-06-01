/*
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2021 Fiji developers.
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

package fiji.plugin.trackmate.action;

import Jama.Matrix;
import net.imglib2.algorithm.localization.FitFunction;
import net.imglib2.algorithm.localization.FunctionFitter;

/**
 * A plain implementation of Levenberg-Marquardt least-square curve fitting algorithm.
 * This solver makes use of only the function value and its gradient. That is:
 * candidate functions need only to implement the {@link FitFunction#val(double[], double[])}
 * and {@link FitFunction#grad(double[], double[], int)} methods to operate with this
 * solver.
 * <p>
 * It was adapted and stripped from jplewis (www.idiom.com/~zilla) and released under 
 * the GPL. There are various small tweaks for robustness and speed.
 *
 * @author Jean-Yves Tinevez 2011 - 2013
 */
public class LevenbergMarquardtSolver implements FunctionFitter {
	
	private final int maxIteration;
	private final double lambda;
	private final double termEpsilon;
	
	/**
	 * Creates a new Levenberg-Marquardt solver for least-square curve fitting problems. 
	 * @param lambda blend between steepest descent (lambda high) and
	 *	jump to bottom of quadratic (lambda zero). Start with 0.001.
	 * @param termEpsilon termination accuracy (0.01)
	 * @param maxIteration stop and return after this many iterations if not done
	 */
	public LevenbergMarquardtSolver(final int maxIteration, final double lambda, final double termEpsilon) {
		this.maxIteration = maxIteration;
		this.lambda = lambda;
		this.termEpsilon = termEpsilon;
	}
	
	/*
	 * METHODS
	 */
	
	@Override
	public String toString() {
		return "Levenberg-Marquardt least-square curve fitting algorithm";
	}
	
	/**
	 * Creates a new Levenberg-Marquardt solver for least-square curve fitting problems,
	 * with default parameters set to:
	 * <ul>
	 * 	<li> <code>lambda  = 1e-3</code>
	 * 	<li> <code>epsilon = 1e-1</code>
	 * 	<li> <code>maxIter = 300</code>
	 * </ul>
	 */
	public LevenbergMarquardtSolver() {
		this( 1700, 1e-12d, 1e-12d );
	}
	
	/*
	 * MEETHODS
	 */
	
	
	@Override
	public void fit(final double[][] x, final double[] y, final double[] a, final FitFunction f) throws Exception {
		solve(x, a, y, f, lambda, termEpsilon, maxIteration);
	}
	
	
	
	/*
	 * STATIC METHODS
	 */
	
	/**
	 * Calculate the current sum-squared-error
	 */
	public static final double chiSquared(final double[][] x, final double[] a, final double[] y, final FitFunction f)  {
		final int npts = y.length;
		double sum = 0.;

		for( int i = 0; i < npts; i++ ) {
			final double d = y[i] - f.val(x[i], a);
			sum = sum + (d*d);
		}

		return sum;
	} //chiSquared

	/**
	 * Minimize E = sum {(y[k] - f(x[k],a)) }^2
	 * Note that function implements the value and gradient of f(x,a),
	 * NOT the value and gradient of E with respect to a!
	 * 
	 * @param x array of domain points, each may be multidimensional
	 * @param y corresponding array of values
	 * @param a the parameters/state of the model
	 * @param lambda blend between steepest descent (lambda high) and
	 *	jump to bottom of quadratic (lambda zero). Start with 0.001.
	 * @param termepsilon termination accuracy (0.01)
	 * @param maxiter	stop and return after this many iterations if not done
	 *
	 * @return the number of iteration used by minimization
	 */
	public static final int solve(final double[][] x, final double[] a, final double[] y, final FitFunction f,
			double lambda, final double termepsilon, final int maxiter) throws Exception  {
		final int npts = y.length;
		final int nparm = a.length;
	
		double e0 = chiSquared(x, a, y, f);
		boolean done = false;

		// g = gradient, H = hessian, d = step to minimum
		// H d = -g, solve for d
		final double[][] H = new double[nparm][nparm];
		final double[] g = new double[nparm];

		int iter = 0;
		int term = 0;	// termination count test

		do {
			++iter;

			// hessian approximation
			for( int r = 0; r < nparm; r++ ) {
				for( int c = 0; c < nparm; c++ ) {
					H[r][c] = 0.;
					for( int i = 0; i < npts; i++ ) {
						final double[] xi = x[i];
						H[r][c] += f.grad(xi, a, r) * f.grad(xi, a, c);
					}  //npts
				} //c
			} //r

			// boost diagonal towards gradient descent
			for( int r = 0; r < nparm; r++ )
				H[r][r] *= (1. + lambda);

			// gradient
			for( int r = 0; r < nparm; r++ ) {
				g[r] = 0.;
				for( int i = 0; i < npts; i++ ) {
					final double[] xi = x[i];
					g[r] += (y[i]-f.val(xi,a)) * f.grad(xi, a, r);
				}
			} //npts
			
			double[] d = null;
            try {
                    d = (new Matrix(H)).lu().solve(new Matrix(g, nparm)).getRowPackedCopy();
            } catch (final RuntimeException re) {
                    // Matrix is singular
                    lambda *= 10.;
                    continue;
            }
            final double[] na = (new Matrix(a, nparm)).plus(new Matrix(d,nparm)).getRowPackedCopy();
            final double e1 = chiSquared(x, na, y, f);
			
			// termination test (slightly different than NR)
			if (Math.abs(e1-e0) > termepsilon) {
				term = 0;
			}
			else {
				term++;
				if (term == 4) {
					done = true;
				}
			}
			if (iter >= maxiter) done = true;

			// in the C++ version, found that changing this to e1 >= e0
			// was not a good idea.  See comment there.
			//
			if (e1 > e0 || Double.isNaN(e1)) { // new location worse than before
				lambda *= 10.;
			}
			else {		// new location better, accept new parameters
				lambda *= 0.1;
				e0 = e1;
				// simply assigning a = na will not get results copied back to caller
				for( int i = 0; i < nparm; i++ ) {
					a[i] = na[i];
				}
			}

		} while(!done);

		return iter;
	} //solve

	
}
