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
package fiji.plugin.trackmate.features.spot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.Threads;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.type.numeric.RealType;

public abstract class AbstractSpotFeatureAnalyzer< T extends RealType< T > > implements SpotAnalyzer< T >, MultiThreaded, Benchmark
{

	protected String errorMessage;

	private long processingTime;

	private int numThreads;

	public abstract void process( final Spot spot );

	public AbstractSpotFeatureAnalyzer()
	{
		setNumThreads();
	}

	@Override
	public void process( final Iterable< Spot > spots )
	{
		final long start = System.currentTimeMillis();

		final List< Callable< Void > > tasks = new ArrayList<>();
		for ( final Spot spot : spots )
		{
			final Callable< Void > task = new Callable< Void >()
			{
				@Override
				public Void call() throws Exception
				{
					try
					{
						process( spot );
					}
					catch ( final Exception e )
					{
						e.printStackTrace();
					}
					return null;
				}
			};
			tasks.add( task );
		}

		final ExecutorService executorService = Threads.newFixedThreadPool( numThreads );
		try
		{
			final List< Future< Void > > futures = executorService.invokeAll( tasks );
			for ( final Future< Void > future : futures )
				future.get();
		}
		catch ( final InterruptedException | ExecutionException e )
		{
			e.printStackTrace();
		}

		executorService.shutdown();
		processingTime = System.currentTimeMillis() - start;
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}

	@Override
	public void setNumThreads()
	{
		setNumThreads( Runtime.getRuntime().availableProcessors() / 2 );
	}

	@Override
	public void setNumThreads( final int numThreads )
	{
		this.numThreads = numThreads;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}
}
