package fiji.plugin.trackmate.tracking.kalman;

import Jama.Matrix;

public class LinearMotionKalmanTracker
{

	private final Matrix A;

	private Matrix P;

	private final Matrix Q;

	private final Matrix R;

	/** Current state. */
	private Matrix X;

	private final Matrix H;

	/** Prediction. */
	private Matrix Xp;

	private int nOcclusion;

	public LinearMotionKalmanTracker( final Matrix X0 )
	{
		// Initial state
		X = X0;

		// Evolution matrix
		A = Matrix.identity( 6, 6 );
		for ( int i = 0; i < 3; i++ )
		{
			A.set( i, 3 + i, 1 );
		}

		// Measurement matrix
		H = Matrix.identity( 3, 6 );

		// Covariance
		P = Matrix.identity( 6, 6 ).times( 100d );
		Q = Matrix.identity( 6, 6 ).times( 1e-2 );
		R = Matrix.identity( 3, 3 ).times( 1e-2 );
	}

	public Matrix predict()
	{
		Xp = A.times( X );
		P = A.times( P.times( A.transpose() ) ).plus( Q );
		return Xp;
	}

	public void update( final Matrix Xm )
	{
		if ( null == Xm )
		{
			// Occlusion.
			nOcclusion++;
			X = Xp;
		}
		else
		{
			final Matrix TEMP = H.times( P.times( H.transpose() ) ).plus( R );
			final Matrix K = P.times( H.transpose() ).times( TEMP.inverse() );
			X = Xp.plus( K.times( Xm.minus( H.times( Xp ) ) ) );
		}
	}

	public int getnOcclusion()
	{
		return nOcclusion;
	}
}
