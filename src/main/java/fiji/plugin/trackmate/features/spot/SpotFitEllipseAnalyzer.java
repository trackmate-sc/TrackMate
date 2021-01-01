package fiji.plugin.trackmate.features.spot;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import Jama.SingularValueDecomposition;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotRoi;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

public class SpotFitEllipseAnalyzer< T extends RealType< T > > extends AbstractSpotFeatureAnalyzer< T >
{

	private final boolean is2D;

	public SpotFitEllipseAnalyzer( final boolean is2D )
	{
		this.is2D = is2D;
	}

	@Override
	public void process( final Spot spot )
	{
		final double aspectRatio;
		final double major;
		final double minor;
		final double theta;
		final double x0;
		final double y0;

		if ( is2D )
		{
			final SpotRoi roi = spot.getRoi();
			if ( roi != null )
			{
				final double[] Q = fitEllipse( roi.x, roi.y );
				final double[] A = quadraticToCartesian( Q );
				x0 = A[ 0 ];
				y0 = A[ 1 ];
				major = A[ 2 ];
				minor = A[ 3 ];
				theta = A[ 4 ];
				aspectRatio = major / minor;
			}
			else
			{
				final double radius = spot.getFeature( Spot.RADIUS );
				x0 = 0.;
				y0 = 0.;
				major = radius;
				minor = radius;
				theta = 0.;
				aspectRatio = 1.;
			}
		}
		else
		{
			x0 = Double.NaN;
			y0 = Double.NaN;
			major = Double.NaN;
			minor = Double.NaN;
			theta = Double.NaN;
			aspectRatio = Double.NaN;
		}
		spot.putFeature( SpotFitEllipseAnalyzerFactory.X0, x0 );
		spot.putFeature( SpotFitEllipseAnalyzerFactory.Y0, y0 );
		spot.putFeature( SpotFitEllipseAnalyzerFactory.MAJOR, major );
		spot.putFeature( SpotFitEllipseAnalyzerFactory.MINOR, minor );
		spot.putFeature( SpotFitEllipseAnalyzerFactory.THETA, theta );
		spot.putFeature( SpotFitEllipseAnalyzerFactory.ASPECTRATIO, aspectRatio );
	}

	/**
	 * Java port of Chernov's MATLAB implementation of the direct ellipse fit by
	 * Michael Doube in BoneJ. Modified by JYT to use the van der Linde
	 * pseudo-inverse. Otherwise we fail too many times because of singular
	 * matrices.
	 *
	 * @param points
	 *            n * 2 array of 2D coordinates.
	 * @return
	 *         <p>
	 *         6-element array, {a b c d f g}, which are the algebraic
	 *         parameters of the fitting ellipse: <i>ax</i><sup>2</sup> + 2
	 *         <i>bxy</i> + <i>cy</i><sup>2</sup> +2<i>dx</i> + 2<i>fy</i> +
	 *         <i>g</i> = 0. The vector <b>A</b> represented in the array is
	 *         normed, so that ||<b>A</b>||=1.
	 *         </p>
	 *
	 * @see
	 *      <p>
	 *      <a href=
	 *      "http://www.mathworks.co.uk/matlabcentral/fileexchange/22684-ellipse-fit-direct-method"
	 *      >MATLAB script</a>
	 *      </p>
	 * @author Michael Doube
	 */
	private static double[] fitEllipse( final double[] x, final double[] y )
	{
		final int nPoints = x.length;
		final double[] centroid = getCentroid( x, y );
		final double xC = centroid[ 0 ];
		final double yC = centroid[ 1 ];
		final double[][] d1 = new double[ nPoints ][ 3 ];
		for ( int i = 0; i < nPoints; i++ )
		{
			final double xixC = x[ i ] - xC;
			final double yiyC = y[ i ] - yC;
			d1[ i ][ 0 ] = xixC * xixC;
			d1[ i ][ 1 ] = xixC * yiyC;
			d1[ i ][ 2 ] = yiyC * yiyC;
		}
		final Matrix D1 = new Matrix( d1 );
		final double[][] d2 = new double[ nPoints ][ 3 ];
		for ( int i = 0; i < nPoints; i++ )
		{
			d2[ i ][ 0 ] = x[ i ] - xC;
			d2[ i ][ 1 ] = y[ i ] - yC;
			d2[ i ][ 2 ] = 1;
		}
		final Matrix D2 = new Matrix( d2 );
		final Matrix S1 = D1.transpose().times( D1 );
		final Matrix S2 = D1.transpose().times( D2 );
		final Matrix S3 = D2.transpose().times( D2 );
		final Matrix T = ( pinv( S3 ).times( -1 ) ).times( S2.transpose() );
		final Matrix M = S1.plus( S2.times( T ) );

		final double[][] m = M.getArray();
		final double[][] n = { { m[ 2 ][ 0 ] / 2, m[ 2 ][ 1 ] / 2, m[ 2 ][ 2 ] / 2 }, { -m[ 1 ][ 0 ], -m[ 1 ][ 1 ], -m[ 1 ][ 2 ] },
				{ m[ 0 ][ 0 ] / 2, m[ 0 ][ 1 ] / 2, m[ 0 ][ 2 ] / 2 } };

		final Matrix N = new Matrix( n );
		final EigenvalueDecomposition E = N.eig();
		final Matrix eVec = E.getV();

		final Matrix R1 = eVec.getMatrix( 0, 0, 0, 2 );
		final Matrix R2 = eVec.getMatrix( 1, 1, 0, 2 );
		final Matrix R3 = eVec.getMatrix( 2, 2, 0, 2 );

		final Matrix cond = ( R1.times( 4 ) ).arrayTimes( R3 ).minus( R2.arrayTimes( R2 ) );

		int f = 0;
		for ( int i = 0; i < 3; i++ )
		{
			if ( cond.get( 0, i ) > 0 )
			{
				f = i;
				break;
			}
		}
		final Matrix A1 = eVec.getMatrix( 0, 2, f, f );

		Matrix A = new Matrix( 6, 1 );
		A.setMatrix( 0, 2, 0, 0, A1 );
		A.setMatrix( 3, 5, 0, 0, T.times( A1 ) );

		final double[] a = A.getColumnPackedCopy();
		final double a4 = a[ 3 ] - 2 * a[ 0 ] * xC - a[ 1 ] * yC;
		final double a5 = a[ 4 ] - 2 * a[ 2 ] * yC - a[ 1 ] * xC;
		final double a6 = a[ 5 ] + a[ 0 ] * xC * xC + a[ 2 ] * yC * yC + a[ 1 ] * xC * yC - a[ 3 ] * xC - a[ 4 ] * yC;
		A.set( 3, 0, a4 );
		A.set( 4, 0, a5 );
		A.set( 5, 0, a6 );
		A = A.times( 1 / A.normF() );
		return A.getColumnPackedCopy();
	}

	private static double[] getCentroid( final double[] x, final double[] y )
	{
		return new double[] { Util.average( x ), Util.average( y ) };
	}

	/**
	 * Convert to cartesian coordnates for the ellipse. Return [ x0 y0 a b theta
	 * ]. We always have a > b. theta in radians measure the angle of the
	 * ellipse long axis with the x axis, in radians, and positive means
	 * counter-clockwise.
	 * 
	 * Formulas from
	 * https://en.wikipedia.org/wiki/Ellipse#In_Cartesian_coordinates
	 */
	private static final double[] quadraticToCartesian( final double[] Q )
	{
		final double A = Q[ 0 ];
		final double B = Q[ 1 ];
		final double C = Q[ 2 ];
		final double D = Q[ 3 ];
		final double E = Q[ 4 ];
		final double F = Q[ 5 ];

		final double term1 = 2 * ( A * E * E
				+ C * D * D
				- B * D * E
				+ ( B * B - 4 * A * C ) * F );
		final double term2 = ( A + C );
		final double term3 = Math.sqrt( ( A - C ) * ( A - C ) + B * B );
		final double term4 = B * B - 4 * A * C;

		double a = -Math.sqrt( term1 * ( term2 + term3 ) ) / term4;
		double b = -Math.sqrt( term1 * ( term2 - term3 ) ) / term4;

		final double x0 = ( 2 * C * D - B * E ) / term4;
		final double y0 = ( 2 * A * E - B * D ) / term4;

		double theta;
		if ( B != 0 )
			theta = Math.atan( 1. / B * ( C - A - term3 ) );
		else if ( A < 0 )
			theta = 0;
		else
			theta = Math.PI / 2.;

		if ( b > a )
		{
			final double btemp = b;
			b = a;
			a = btemp;
			theta = theta + Math.PI / 2.;
			if ( theta > Math.PI )
				theta = theta - Math.PI;
		}

		return new double[] { x0, y0, a, b, theta };
	}

	/**
	 * Computes the Moore–Penrose pseudoinverse using the SVD method.
	 *
	 * Modified version of the original implementation by Kim van der Linde.
	 */
	public static Matrix pinv( final Matrix x )
	{
		final int rows = x.getRowDimension();
		final int cols = x.getColumnDimension();
		if ( rows < cols )
		{
			Matrix result = pinv( x.transpose() );
			if ( result != null )
				result = result.transpose();
			return result;
		}
		final SingularValueDecomposition svdX = new SingularValueDecomposition( x );
		if ( svdX.rank() < 1 )
			return null;
		final double[] singularValues = svdX.getSingularValues();
		final double tol = Math.max( rows, cols ) * singularValues[ 0 ] * MACHEPS;
		final double[] singularValueReciprocals = new double[ singularValues.length ];
		for ( int i = 0; i < singularValues.length; i++ )
			if ( Math.abs( singularValues[ i ] ) >= tol )
				singularValueReciprocals[ i ] = 1.0 / singularValues[ i ];
		final double[][] u = svdX.getU().getArray();
		final double[][] v = svdX.getV().getArray();
		final int min = Math.min( cols, u[ 0 ].length );
		final double[][] inverse = new double[ cols ][ rows ];
		for ( int i = 0; i < cols; i++ )
			for ( int j = 0; j < u.length; j++ )
				for ( int k = 0; k < min; k++ )
					inverse[ i ][ j ] += v[ i ][ k ] * singularValueReciprocals[ k ] * u[ j ][ k ];
		return new Matrix( inverse );
	}

	private final static double MACHEPS = 2.2204e-16;
}
