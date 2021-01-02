package fiji.plugin.trackmate.features.spot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import fiji.plugin.trackmate.Spot;
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

		final ExecutorService executorService = Executors.newFixedThreadPool( numThreads );
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
