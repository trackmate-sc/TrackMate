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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.util.Threads;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.mesh.Mesh;
import net.imglib2.mesh.alg.TaubinSmoothing;
import net.imglib2.mesh.alg.TaubinSmoothing.TaubinWeightType;
import net.imglib2.mesh.impl.nio.BufferMesh;
import net.imglib2.mesh.view.TranslateMesh;

public class MeshSmoother implements MultiThreaded
{

	private static final long TIME_OUT_DELAY = 2;

	private static final TimeUnit TIME_OUT_UNITS = TimeUnit.HOURS;

	private final Logger logger;

	private int numThreads;

	private final Model model;

	public MeshSmoother( final Model model, final Logger logger )
	{
		this.model = model;
		this.logger = logger;
		setNumThreads();
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

		model.beginUpdate();
		try
		{
			final AtomicInteger ai = new AtomicInteger( 0 );
			final ExecutorService executors = Threads.newFixedThreadPool( numThreads );
			final List< Spot > modifiedSpots = new ArrayList<>();
			for ( final Spot spot : spots )
			{
				if ( spot instanceof SpotMesh )
				{
					final SpotMesh sm = ( SpotMesh ) spot;
					model.beforeEdit( sm );
					executors.execute( process( sm, nIters, mu, lambda, weightType, ai, nSpots ) );
					modifiedSpots.add( sm );
				}
			}

			executors.shutdown();
			final boolean ok = executors.awaitTermination( TIME_OUT_DELAY, TIME_OUT_UNITS );
			if ( !ok )
				logger.error( "Timeout of " + TIME_OUT_DELAY + " " + TIME_OUT_UNITS + " reached while smoothing.\n" );

			logger.log( "Done.\n" );
			return modifiedSpots;
		}
		catch ( final InterruptedException e )
		{
			logger.error( e.getMessage() );
			Thread.currentThread().interrupt();
		}
		finally
		{
			logger.setProgress( 1 );
			logger.setStatus( "" );
			model.endUpdate();
		}
		return null;
	}

	private static final int count( final Iterable< Spot > spots )
	{
		if ( spots instanceof Collection )
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
				final Mesh mesh = sm.getMesh();
				final BufferMesh smoothedMesh = TaubinSmoothing.smooth( mesh, nIters, lambda, mu, weightType );
				sm.setMesh( TranslateMesh.translate( smoothedMesh, sm ) );

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
