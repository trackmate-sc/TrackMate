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
package fiji.plugin.trackmate.action.meshtools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
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

	private static final long TIME_OUT_DELAY = 2;

	private static final TimeUnit TIME_OUT_UNITS = TimeUnit.HOURS;

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


	public List< Spot > undo()
	{
		logger.setStatus( "Undoing mesh smoothing" );
		final Set< SpotMesh > keys = undoMap.keySet();
		final int nSpots = keys.size();
		int i = 0;
		logger.log( "Undoing mesh smoothing for " + nSpots + " spots.\n" );
		final List< Spot > modifiedSpots = new ArrayList<>();
		for ( final SpotMesh sm : keys )
		{
			final ValuePair< BufferMesh, double[] > old = undoMap.get( sm );
			sm.setMesh( old.getA() );
			sm.setPosition( old.getB() );
			modifiedSpots.add( sm );
			logger.setProgress( ( double ) ( ++i ) / nSpots );
		}
		logger.setStatus( "" );
		logger.log( "Done.\n" );
		return modifiedSpots;
	}

	public List< Spot > smooth( final MeshSmootherModel smootherModel, final Iterable< Spot > spots )
	{
		final double mu = smootherModel.getMu();
		final double lambda = smootherModel.getLambda();
		final int nIters = smootherModel.getNIters();
		final TaubinWeightType weightType = smootherModel.getWeightType();

		final int nSpots = count( spots );
		logger.setStatus( "Taubin smoothing" );
		logger.log( "Started Taubin smoothing over " + nSpots + " spots with parameters:\n" );
		logger.log( String.format( " - %s: %.2f\n", "µ", mu ) );
		logger.log( String.format( " - %s: %.2f\n", "λ", lambda ) );
		logger.log( String.format( " - %s: %d\n", "N iterations", nIters ) );
		logger.log( String.format( " - %s: %s\n", "weights", weightType ) );

		final AtomicInteger ai = new AtomicInteger( 0 );
		final ExecutorService executors = Threads.newFixedThreadPool( numThreads );
		final List< Spot > modifiedSpots = new ArrayList<>();
		for ( final Spot spot : spots )
		{
			if ( SpotMesh.class.isInstance( spot ) )
			{
				final SpotMesh sm = ( SpotMesh ) spot;
				executors.execute( process( sm, nIters, mu, lambda, weightType, ai, nSpots ) );
				modifiedSpots.add( sm );
			}
		}

		executors.shutdown();
		try
		{
			final boolean ok = executors.awaitTermination( TIME_OUT_DELAY, TIME_OUT_UNITS );
			if ( !ok )
				logger.error( "Timeout of " + TIME_OUT_DELAY + " " + TIME_OUT_UNITS + " reached while smoothing.\n" );

			logger.log( "Done.\n" );
		}
		catch ( final InterruptedException e )
		{
			logger.error( e.getMessage() );
			e.printStackTrace();
		}
		finally
		{
			logger.setProgress( 1 );
			logger.setStatus( "" );
		}
		return modifiedSpots;
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
