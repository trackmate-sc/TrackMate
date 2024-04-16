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
package fiji.plugin.trackmate.features.track;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.util.Threads;

/**
 * Abstract class for track analyzers that are local and not manual. Offers
 * multithreading.
 */
@Plugin( type = TrackAnalyzer.class, enabled = false )
public abstract class AbstractTrackAnalyzer implements TrackAnalyzer
{

	private int numThreads;

	private long processingTime;

	private final String key;

	private final String name;

	private final List< String > features;

	private final Map< String, String > featureNames;

	private final Map< String, String > featureShortNames;

	private final Map< String, Dimension > featureDimensions;

	private final Map< String, Boolean > isInts;

	public AbstractTrackAnalyzer(
			final String key,
			final String name,
			final List<String> features, 
			final Map<String,String> featureNames, 
			final Map<String,String> featureShortNames, 
			final Map<String,Dimension> featureDimensions, 
			final Map< String, Boolean > isInts )
	{
		this.key = key;
		this.name = name;
		this.features = features;
		this.featureNames = featureNames;
		this.featureShortNames = featureShortNames;
		this.featureDimensions = featureDimensions;
		this.isInts = isInts;
		setNumThreads();
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public final boolean isManualFeature()
	{
		return false;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getInfoText()
	{
		return null;
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
	public final boolean isLocal()
	{
		return true;
	}

	@Override
	public String getKey()
	{
		return key;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public Map< String, Dimension > getFeatureDimensions()
	{
		return featureDimensions;
	}

	@Override
	public Map< String, String > getFeatureNames()
	{
		return featureNames;
	}

	@Override
	public List< String > getFeatures()
	{
		return features;
	}

	@Override
	public Map< String, String > getFeatureShortNames()
	{
		return featureShortNames;
	}

	@Override
	public Map< String, Boolean > getIsIntFeature()
	{
		return isInts;
	}

	@Override
	public void process( final Collection< Integer > trackIDs, final Model model )
	{
		if ( trackIDs.isEmpty() )
			return;

		final long start = System.currentTimeMillis();

		// Create tasks.
		final List< Callable< Void > > tasks = new ArrayList<>( trackIDs.size() );
		for ( final Integer trackID : trackIDs )
		{
			final Callable< Void > task = new Callable< Void >()
			{

				@Override
				public Void call() throws Exception
				{
					analyze( trackID, model );
					return null;
				}
			};
			tasks.add( task );
		}

		final ExecutorService executorService = Threads.newFixedThreadPool( numThreads );
		List< Future< Void > > futures;
		try
		{
			futures = executorService.invokeAll( tasks );
			for ( final Future< Void > future : futures )
				future.get();
		}
		catch ( InterruptedException | ExecutionException e )
		{
			e.printStackTrace();
		}
		executorService.shutdown();

		final long end = System.currentTimeMillis();
		processingTime = end - start;
	}

	protected abstract void analyze( final Integer trackID, final Model model );
}
