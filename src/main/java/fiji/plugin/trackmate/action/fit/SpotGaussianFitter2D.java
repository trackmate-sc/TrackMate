package fiji.plugin.trackmate.action.fit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
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
import fiji.plugin.trackmate.util.TMUtils;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imglib2.Cursor;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * 2D case!
 */
public class SpotGaussianFitter2D< T extends RealType< T > > implements MultiThreaded, Benchmark
{

	private final ImgPlus< T > img;

	private final int channel;

	private final double[] calibration;

	private final ConcurrentHashMap< Integer, RandomAccessibleInterval< T > > hyperslices = new ConcurrentHashMap<>();

	private int numThreads;

	private long processingTime = -1;

	private final LevenbergMarquardtOptimizer optimizer;

	@SuppressWarnings( "unchecked" )
	public SpotGaussianFitter2D( final ImagePlus imp, final int channel )
	{
		assert DetectionUtils.is2D( imp );
		this.channel = channel;
		this.img = TMUtils.rawWraps( imp );
		this.calibration = TMUtils.getSpatialCalibration( imp );
		// Least-square fitting.
		this.optimizer = new LevenbergMarquardtOptimizer()
				.withCostRelativeTolerance( 1.0e-12 )
				.withParameterRelativeTolerance( 1.0e-12 );
		setNumThreads();
	}

	public void process( final Iterable< Spot > spots, final Logger logger )
	{
		logger.log( String.format( "Starting 2D Gaussian fitting with %d threads.\n", numThreads ) );
		logger.setStatus( "Gaussian fitting" );
		final long start = System.currentTimeMillis();
		final ExecutorService executorService = Executors.newFixedThreadPool( numThreads );
		final List< Future< ? > > futures = new ArrayList<>();
		for ( final Spot spot : spots )
			futures.add( executorService.submit( () -> fit( spot ) ) );

		final int nspots = futures.size();
		try
		{
			int i = 0;
			for ( final Future< ? > future : futures )
			{
				future.get();
				logger.setProgress( ( double ) i++ / nspots );
			}
		}
		catch ( InterruptedException | ExecutionException e )
		{
			e.printStackTrace();
		}
		executorService.shutdown();

		final long stop = System.currentTimeMillis();
		this.processingTime = stop - start;
		logger.log( String.format( "Fit completed for %d spots in %.1f s.\n", nspots, processingTime / 1000. ) );
		logger.setStatus( "" );
		logger.setProgress( 0. );
	}

	public void fit( final Spot spot )
	{
		final int frame = spot.getFeature( Spot.FRAME ).intValue();
		final RandomAccessibleInterval< T > slice = hyperslices.computeIfAbsent(
				Integer.valueOf( frame ),
				t -> TMUtils.hyperSlice( img, channel, t ) );

		final double sigma = spot.getFeature( Spot.RADIUS ) / Math.sqrt( 2. );
		final double pixelSigma = sigma / calibration[ 0 ];
		final double x0 = spot.getDoublePosition( 0 ) / calibration[ 0 ];
		final double y0 = spot.getDoublePosition( 1 ) / calibration[ 1 ];
		final long span = ( long ) Math.ceil( 2. * pixelSigma ) + 1;
		final Observation obs = gatherObservationData( slice, new Point( ( long ) x0, ( long ) y0 ), new long[] { span, span } );

		final MyGaussian2D gauss = new MyGaussian2D( obs.pos );
		final double bstart = 1 / ( 2 * pixelSigma * pixelSigma );
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

	@Override
	public void setNumThreads()
	{
		setNumThreads( Math.max( 1, Runtime.getRuntime().availableProcessors() / 2 ) );
	}

	@Override
	public void setNumThreads( final int numThreads )
	{
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	private static < T extends RealType< T > > Observation gatherObservationData( final RandomAccessibleInterval< T > img, final Point point, final long[] span )
	{
		final int ndims = point.numDimensions();
		// Create interval.
		final long[] min = new long[ point.numDimensions() ];
		final long[] max = new long[ point.numDimensions() ];
		for ( int d = 0; d < max.length; d++ )
		{
			min[ d ] = Math.max( point.getLongPosition( d ) - span[ d ], img.min( 0 ) );
			max[ d ] = Math.min( point.getLongPosition( d ) + span[ d ], img.max( 0 ) );
		}

		// Collect.
		final IntervalView< T > view = Views.interval( img, min, max );
		final int nel = ( int ) view.size();
		final double[] vals = new double[ nel ];
		final long[][] pos = new long[ ndims ][ nel ];
		final Cursor< T > cursor = view.localizingCursor();
		int index = -1;
		while ( cursor.hasNext() )
		{
			index++;
			cursor.fwd();
			vals[ index ] = cursor.get().getRealDouble();
			for ( int d = 0; d < ndims; d++ )
				pos[ d ][ index ] = cursor.getLongPosition( d );
		}

		// Remove background and clip to 0.
		final double bg = Util.median( vals );
		for ( int i = 0; i < vals.length; i++ )
			vals[ i ] -= bg;
		for ( int i = 0; i < vals.length; i++ )
			vals[ i ] = Math.max( 0., vals[ i ] );

		return new Observation( vals, pos );
	}

	private static class Observation
	{

		public final double[] values;

		public final long[][] pos;

		private Observation( final double[] values, final long[][] pos )
		{
			this.values = values;
			this.pos = pos;
		}

		@Override
		public String toString()
		{
			final StringBuilder str = new StringBuilder( super.toString() );
			str.append( "\nvalues: " + Util.printCoordinates( values ) );
			for ( int d = 0; d < pos.length; d++ )
				str.append( "\npos[" + d + "]: " + Util.printCoordinates( pos[ d ] ) );
			return str.toString();
		}
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

		public MyGaussian2D( final long[][] pos )
		{
			this.pos = pos;
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
			params.setEntry( 3, Math.abs( params.getEntry( 3 ) ) );
			return params;
		}
	}
}
