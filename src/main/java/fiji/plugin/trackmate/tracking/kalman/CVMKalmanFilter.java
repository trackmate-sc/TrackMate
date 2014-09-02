package fiji.plugin.trackmate.tracking.kalman;

import Jama.Matrix;

/**
 * A Kalman filter that deals with a single particle motion in 3D with a
 * constant velocity vector.
 * 
 * @author Jean-Yves Tinevez - 2014
 * 
 */
public class CVMKalmanFilter
{

	/**
	 * The evolution matrix, or state transition matrix. In our case, it is the
	 * matrix that links position evolution and velocity through
	 * <code><b>x</b>(k+1) = <b>x</b>(k) + <b>v</b> × dt</code>. We assume
	 * <code><b>v</b></code> is constant and measured in unit of frames, so
	 * <code>dt = 1</code>.
	 */
	private final Matrix A;

	/**
	 * The <i>a posteriori</i> error covariance matrix, measure the accuracy of
	 * the state estimate.
	 */
	private Matrix P;

	/**
	 * Covariance matrix of the process noise. Determine how noisy the process
	 * is.
	 */
	private final Matrix Q;

	/**
	 * Covariance matrix of the observation noise. Determine how noisy our
	 * measurements are.
	 */
	private final Matrix R;

	/** Current state. */
	private Matrix X;

	private final Matrix H;

	/** Prediction. */
	private Matrix Xp;

	/**
	 * Number of occlusions (no measurements) that happened so far.
	 */
	private int nOcclusion;

	/**
	 * Initialize a new Kalman filter with the specified initial state.
	 * 
	 * 
	 * @param X0
	 *            initial state estimate. Must a 6 elements
	 *            <code>double[]</code> array with
	 *            <code>x0, y0, z0, vx0, vy0, vz0</code> with velocity in
	 *            <code>length/frame</code> units.
	 * @param initStateCovariance
	 *            the initial state covariance. Give it a large value if you do
	 *            not trust the initial state estimate (<i>e.g.</i> 100), a
	 *            small value otherwise (<i>e.g.</i>1e-2).
	 * @param positionProcessStd
	 *            the std of the additive white gaussian noise affecting the
	 *            <b>position</b> evolution. Large values means that the
	 *            position undergoes heavy fluctuations.
	 * @param velocityProcessStd
	 *            the std of the additive white gaussian noise affecting the
	 *            <b>velocity</b> evolution. Careful, we expect it to be in
	 *            units of
	 *            <code>length/frame<code>. Large values means that the velocity
	 *            undergoes heavy fluctuations.
	 * @param positionMeasurementStd
	 *            the std of the additive white gaussian noise affecting the
	 *            position <b>measurement</b>. Large values means that the
	 *            positions measured are not accurate.
	 */
	public CVMKalmanFilter( final double[] X0, final double initStateCovariance, final double positionProcessStd, final double velocityProcessStd, final double positionMeasurementStd )
	{
		// Initial state
		X = new Matrix( X0, 6 );

		// Evolution matrix
		A = Matrix.identity( 6, 6 );
		for ( int i = 0; i < 3; i++ )
		{
			A.set( i, 3 + i, 1 );
		}

		// Measurement matrix
		H = Matrix.identity( 3, 6 );

		// State covariance
		P = Matrix.identity( 6, 6 ).times( initStateCovariance );

		// Process covariance
		Q = Matrix.identity( 6, 6 );
		for ( int i = 0; i < 3; i++ )
		{
			Q.set( i, i, positionProcessStd * positionProcessStd );
			Q.set( 3 + i, 3 + i, velocityProcessStd * velocityProcessStd );
		}

		R = Matrix.identity( 3, 3 ).times( positionMeasurementStd * positionMeasurementStd );
	}

	/**
	 * Runs the prediction step of the Kalman filter and returns the state
	 * predicted by the evolution process.
	 * 
	 * @return a new <code>double[]</code> of 6 elements containing the
	 *         predicted state: <code>x, y, z, vx, vy, vz</code> with velocity
	 *         in <code>length/frame</code> units.
	 * 
	 */
	public double[] predict()
	{
		Xp = A.times( X );
		P = A.times( P.times( A.transpose() ) ).plus( Q );
		return Xp.getColumnPackedCopy();
	}

	/**
	 * Runs the update step of the Kalman filter based on the specified
	 * measurement.
	 * 
	 * @param Xm
	 *            the measured position, must be specified as a 3 elements
	 *            <code>double[]</code>array, containing the measured
	 *            <code>x, y, z</code> position. It can be <code>null</code>;
	 *            the filter then assumes an occlusion occurred, and update its
	 *            state based on solely the prediction step.
	 */
	public void update( final double[] Xm )
	{
		if ( null == Xm )
		{
			// Occlusion.
			nOcclusion++;
			X = Xp;
		}
		else
		{
			final Matrix XM = new Matrix( Xm, 3 );
			final Matrix TEMP = H.times( P.times( H.transpose() ) ).plus( R );
			final Matrix K = P.times( H.transpose() ).times( TEMP.inverse() );
			// State
			X = Xp.plus( K.times( XM.minus( H.times( Xp ) ) ) );
			// Covariance
			P = ( Matrix.identity( 6, 6 ).minus( K.times( H ) ) ).times( P );
		}
	}

	/**
	 * Return the root mean square error on position estimated through the state
	 * covariance matrix.
	 * 
	 * @return the estimated error on position.
	 */
	public double getPositionError()
	{
		return Math.sqrt( ( P.get( 0, 0 ) + P.get( 1, 1 ) + P.get( 2, 2 ) ) / 3d );
	}

	/**
	 * Return the root mean square error on velocity estimated through the state
	 * covariance matrix.
	 * 
	 * @return the estimated error on velocity, in <code>length/frame</code>
	 *         units.
	 */
	public double getVelocityError()
	{
		return Math.sqrt( ( P.get( 3, 3 ) + P.get( 4, 4 ) + P.get( 5, 5 ) ) / 3d );
	}


	/**
	 * Returns the number of occlusion events that occurred since the
	 * instantiation of this filter.
	 * 
	 * @return the number of occlusions.
	 */
	public int getNOcclusion()
	{
		return nOcclusion;
	}
}
