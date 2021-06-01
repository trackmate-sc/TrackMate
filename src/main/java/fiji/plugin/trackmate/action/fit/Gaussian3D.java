package fiji.plugin.trackmate.action.fit;

import net.imglib2.algorithm.localization.FitFunction;

/**
 * A n-dimensional Gaussian peak function, representing an elliptical Gaussian,
 * with axis constrained to be aligned with the main axis.
 * <p>
 * This fitting target function is defined over dimension <code>n</code>, by the
 * following <code>6</code> parameters:
 * 
 * <ol start="0">
 * <li>x0
 * <li>y0
 * <li>z0
 * <li>A0
 * <li>sigma_xy
 * <li>sigma_z
 * </ol>
 * 
 * with
 * 
 * <pre>
 * f(x) = A × exp( - S )
 * </pre>
 * 
 * and
 * 
 * <pre>
 * S = A0 * exp( -( x - x0 ) ^ 2 / ( 2 * sigmaxy ^ 2 ) - ( y - y0 ) ^ 2 / ( 2 * sigmaxy ^ 2 ) - ( z - z0 ) ^ 2 / ( 2 * sigmaz ^ 2 ) )
 * </pre>
 * 
 * @author Jean-Yves Tinevez
 *
 */
public class Gaussian3D implements FitFunction
{

	@Override
	public double val( final double[] x, final double[] a )
	{
		return a[ 3 ] * E( x, a );
	}

	@Override
	public double grad( final double[] pos, final double[] a, final int ak )
	{
		final double x0 = a[ 0 ];
		final double y0 = a[ 1 ];
		final double z0 = a[ 2 ];
		final double A0 = a[ 3 ];
		final double sigmaxy = a[ 4 ];
		final double sigmaz = a[ 5 ];
		
		final double x = pos[0];
		final double y = pos[1];
		final double z = pos[2];
		
		final double dx = x - x0;
		final double dy = y - y0;
		final double dz = z - z0;

		final double dx2 = dx*dx;
		final double dy2 = dy*dy;
		final double dz2 = dz*dz;
		final double sigmaxy2 = sigmaxy *sigmaxy; 
		final double sigmaz2 = sigmaz *sigmaz; 

		final double sigmaxy3 = sigmaxy *sigmaxy * sigmaxy; 
		final double sigmaz3 = sigmaz *sigmaz * sigmaz; 

		switch ( ak )
		{
		// x0
		case 0:
			return A0 * E( pos, a ) * dx / sigmaxy2;

		// y0
		case 1:
			return A0 * E( pos, a ) * dy / sigmaxy2;

		// z0
		case 2:
			return A0 * E( pos, a ) * dz / sigmaz2;

		// A0
		case 3:
			return E( pos, a );

		// sigmaxy
		case 4:
			return A0 * E( pos, a ) * ( dx2 / sigmaxy3 + dy2 / sigmaxy3 );
			
		// sigmaz
		case 5:
			return A0 * E( pos, a ) * dz2 / sigmaz3;
			
		default:
			throw new IllegalArgumentException( "There is no parameter #" + ak + " in the 3D Gaussian." );
		}
	}

	@Override
	public double hessian( final double[] pos, final double[] a, final int r, final int c )
	{
		final double x0 = a[ 0 ];
		final double y0 = a[ 1 ];
		final double z0 = a[ 2 ];
		final double A0 = a[ 3 ];
		final double sigmaxy = a[ 4 ];
		final double sigmaz = a[ 5 ];
		
		final double x = pos[0];
		final double y = pos[1];
		final double z = pos[2];
		
		final double dx = x - x0;
		final double dy = y - y0;
		final double dz = z - z0;

		final double dx2 = dx*dx;
		final double dy2 = dy*dy;
		final double dz2 = dz*dz;
		final double sigmaxy2 = sigmaxy *sigmaxy; 
		final double sigmaz2 = sigmaz *sigmaz; 

		final double sigmaxy3 = sigmaxy *sigmaxy * sigmaxy; 
		final double sigmaz3 = sigmaz *sigmaz * sigmaz; 

		final double dz4 = dz * dz * dz * dz;
		final double sigmaxy4 = sigmaxy *sigmaxy * sigmaxy* sigmaxy; 
		final double sigmaz4 = sigmaz *sigmaz * sigmaz* sigmaz; 

		final double sigmaz5 = sigmaz * sigmaz * sigmaz * sigmaz * sigmaz;

		final double sigmaz6 = sigmaz * sigmaz * sigmaz * sigmaz * sigmaz * sigmaz;
		
		if ( r == c )
		{
			/*
			 * Diagonal terms.
			 */
			switch ( r )
			{
			case 0:
				return A0 * E( pos, a ) * 2. * dx2 / ( 4. * sigmaxy4 ) - A0 * E( pos, a ) / sigmaxy2;
				 
			case 1:
				return A0 * E( pos, a ) * 2. * dy2 / ( 4. * sigmaxy4 ) - A0 * E( pos, a ) / sigmaxy2;
						 
			case 2:
				return A0 * E( pos, a ) * 2. * dz2 / ( 4. * sigmaz4 ) - A0 * E( pos, a ) / sigmaz2;

			case 3:
				return 0.;

			case 4:
				return A0 * E( pos, a ) * ( dx2 / sigmaxy3 + dy2 / sigmaxy3 ) * ( dx2 / sigmaxy3 + dy2 / sigmaxy3 )
						- A0 * E( pos, a ) * ( ( 3. * dx2 ) / sigmaxy4 + ( 3. * dy2 ) / sigmaxy4 );

			case 5:
				return A0 * E( pos, a ) * dz4 / sigmaz6 - 3. * A0 * E( pos, a ) / sigmaz4;
						 
			}
		}

		/*
		 * First line.
		 */
		
		if ( ( r == 0 && c == 1 ) || ( r == 1 && c == 0 ) )
		{
			// df / dx0 / dy0
			return A0 * E( pos, a ) * dx * dy / sigmaxy4;
		}
		else if ( ( r == 0 && c == 2 ) || ( r == 2 && c == 0 ) )
		{
			// df / dx0 / dz0
			return ( A0 * E( pos, a ) * ( 2 * dx ) * ( 2 * dz ) ) / ( 4 * sigmaz2 * sigmaxy2 );
		}
		else if ( ( r == 0 && c == 3 ) || ( r == 3 && c == 0 ) )
		{
			// df / dx0 / dA0
			return E( pos, a ) * dx / sigmaxy2;
		}
		else if ( ( r == 0 && c == 4 ) || ( r == 4 && c == 0 ) )
		{
			// df / dx0 / dsigmaxy
			return A0 * E( pos, a ) * 2 * dx * ( dx2 / sigmaxy3 + dy2 / sigmaxy3 ) / ( 2 * sigmaxy2 )
					- A0 * E( pos, a ) / sigmaxy3;
		}
		else if ( ( r == 0 && c == 5 ) || ( r == 5 && c == 0 ) )
		{
			// df / dx0 / dsigmaz
			return A0 * E( pos, a ) * 2 * dx * dz2 / ( 2 * sigmaz3 * sigmaxy2 );
		}

		/*
		 * Second line
		 */

		else if ( ( r == 1 && c == 2 ) || ( r == 2 && c == 1 ) )
		{
			// df / dy0 / dz0
			return A0 * E( pos, a ) * ( 2 * dy ) * ( 2 * dz ) / ( 4 * sigmaz2 * sigmaxy2 );
		}
		else if ( ( r == 1 && c == 3 ) || ( r == 3 && c == 1 ) )
		{
			// df / dy0 / dA0
			return E( pos, a ) * dy / sigmaxy2;
		}
		else if ( ( r == 1 && c == 4 ) || ( r == 4 && c == 1 ) )
		{
			// df / dy0 / dsigmaxy
			return A0 * E( pos, a ) * 2 * dy * ( dx2 / sigmaxy3 + dy2 / sigmaxy3 ) / ( 2 * sigmaxy2 )
					- A0 * E( pos, a ) * 2 * dy / sigmaxy3;
		}
		else if ( ( r == 1 && c == 5 ) || ( r == 5 && c == 1 ) )
		{
			// df / dy0 / dsigmaz
			return A0 * E( pos, a ) * 2 * dy * dz2 / ( 2 * sigmaz3 * sigmaxy2 );
		}

		/*
		 * Third line
		 */
		else if ( ( r == 2 && c == 3 ) || ( r == 3 && c == 2 ) )
		{
			// df / dz0 / dA0
			return E( pos, a ) / ( 2 * sigmaz2 );
		}
		else if ( ( r == 2 && c == 4 ) || ( r == 4 && c == 2 ) )
		{
			// df / dz0 / sigmaxy
			return A0 * E( pos, a ) * ( 2 * dz ) * ( dx2 / sigmaxy3 + dy2 / sigmaxy ) / ( 2 * sigmaz2 );
		}
		else if ( ( r == 2 && c == 5 ) || ( r == 5 && c == 2 ) )
		{
			// df / dz0 / sigmaz
			return A0 * E( pos, a ) * ( 2 * dz ) * dz2 / ( 2 * sigmaz5 )
					- A0 * E( pos, a ) * ( 2 * dz ) / sigmaz3;
		}

		/*
		 * Fourth line
		 */
		else if ( ( r == 3 && c == 4 ) || ( r == 4 && c == 3 ) )
		{
			// df / dA0 / sigmaxy
			return E( pos, a ) * ( dx2 / sigmaxy3 + dy2 / sigmaxy3 );
		}
		else if ( ( r == 3 && c == 5 ) || ( r == 5 && c == 3 ) )
		{
			// df / dA0 / sigmaz
			return E( pos, a ) * dz2 / sigmaz3;
		}

		/*
		 * Fifth line
		 */
		else if ( ( r == 4 && c == 5 ) || ( r == 5 && c == 4 ) )
		{
			// df / dsigmaxy / sigmaz
			return A0 * E( pos, a ) * dz2 * ( dx2 / sigmaxy3 + dy2 / sigmaxy3 ) / sigmaz3;
		}

		else
		{
			throw new IllegalArgumentException( "Cannot compute Hessian for parameter indices #" + r + " and " + c );
		}
	}

	/*
	 * PRIVATE METHODS
	 */

	private static final double E( final double[] x, final double[] a )
	{
		final double sigmaxy = a[4];
		final double sigmaz = a[5];
		final double dx = x[ 0 ] - a[ 0 ];
		final double dy = x[ 1 ] - a[ 1 ];
		final double dz = x[ 2 ] - a[ 2 ];
		final double sum = ( dx * dx + dy * dy ) / ( 2 * sigmaxy * sigmaxy ) + ( dz * dz ) / ( 2 * sigmaz * sigmaz );
		return Math.exp( -sum );
	}

}
