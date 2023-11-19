package fiji.plugin.trackmate.action.meshtools;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.util.Threads;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.mesh.Meshes;
import net.imglib2.mesh.alg.TaubinSmoothing;
import net.imglib2.mesh.alg.TaubinSmoothing.TaubinWeightType;
import net.imglib2.mesh.impl.nio.BufferMesh;
import net.imglib2.util.ValuePair;

public class MeshSmoother implements MultiThreaded
{

	/** Stores initial position and mesh of the spot. */

	private final ConcurrentHashMap< SpotMesh, ValuePair< BufferMesh, double[] > > undoMap;

	private final Logger logger;

	private int numThreads;


	public MeshSmoother( final Logger logger )
	{
		this.logger = logger;
		this.undoMap = new ConcurrentHashMap<>();
		setNumThreads();
	}


	public void undo()
	{
		logger.setStatus( "Undoing mesh smoothing" );
		final Set< SpotMesh > keys = undoMap.keySet();
		final int nSpots = keys.size();
		int i = 0;
		logger.log( "Undoing mesh smoothing for " + nSpots + " spots.\n" );
		for ( final SpotMesh sm : keys )
		{
			final ValuePair< BufferMesh, double[] > old = undoMap.get( sm );
			sm.setMesh( old.getA() );
			sm.setPosition( old.getB() );
			logger.setProgress( ( double ) ( ++i ) / nSpots );
		}
		logger.setStatus( "" );
		logger.log( "Done.\n" );
	}

	public void smooth(
			final Iterable< Spot > spots,
			final int nIters,
			final double mu,
			final double lambda,
			final TaubinWeightType weightType )
	{
		final int nSpots = count( spots );
		logger.setStatus( "Taubin smoothing" );
		logger.log( "Started Taubin smoothing over " + nSpots + " spots with parameters:\n" );
		logger.log( String.format( " - %14s: %5.2f\n", "µ", mu ) );
		logger.log( String.format( " - %14s: %5.2f\n", "λ", lambda ) );
		logger.log( String.format( " - %14s: %5d\n", "N iterations", nIters ) );
		logger.log( String.format( " - %14s: %s\n", "weights", weightType ) );

		final AtomicInteger ai = new AtomicInteger( 0 );
		final ExecutorService executors = Threads.newFixedThreadPool( numThreads );
		for ( final Spot spot : spots )
		{
			if ( SpotMesh.class.isInstance( spot ) )
			{
				final SpotMesh sm = ( SpotMesh ) spot;
				executors.execute( process( sm, nIters, mu, lambda, weightType, ai, nSpots ) );
			}
		}

		logger.setStatus( "" );
		logger.log( "Done.\n" );
	}

	private static final int count( final Iterable< Spot > spots )
	{
		if ( Collection.class.isInstance( spots ) )
			return ( ( Collection< ? > ) spots ).size();

		int n = 0;
		for ( @SuppressWarnings( "unused" )
		final Spot spot : spots )
			n++;
		return n;
	}

	private Runnable process(
			final SpotMesh sm,
			final int nIters,
			final double mu,
			final double lambda,
			final TaubinWeightType weightType,
			final AtomicInteger ai,
			final int nSpots )
	{
		return new Runnable()
		{
			@Override
			public void run()
			{
				final BufferMesh mesh = sm.getMesh();
				final double[] center = new double[ 3 ];
				sm.localize( center );

				// Store for undo.
				if ( !undoMap.containsKey( sm ) )
				{
					final ValuePair< BufferMesh, double[] > pair = new ValuePair<>( mesh, center );
					undoMap.put( sm, pair );
				}

				// Process.
				Meshes.translate( mesh, center );
				final BufferMesh smoothedMesh = TaubinSmoothing.smooth( mesh, nIters, lambda, mu, weightType );
				sm.setMesh( smoothedMesh );

				logger.setProgress( ( double ) ai.incrementAndGet() / nSpots );
			}
		};
	}

	@Override
	public void setNumThreads()
	{
		this.numThreads = Math.max( 1, Runtime.getRuntime().availableProcessors() / 2 );
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
}
