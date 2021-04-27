/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2020 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Grant Harris, Stefan Helfrich, Mark Hiner,
 * Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey, Melissa Linkert,
 * Mark Longair, Brian Northan, Nick Perry, Curtis Rueden, Johannes Schindelin,
 * Jean-Yves Tinevez and Michael Zinsmaier.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package fiji.plugin.trackmate.action;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.OutputAlgorithm;
import net.imglib2.algorithm.localization.FitFunction;
import net.imglib2.algorithm.localization.FunctionFitter;
import net.imglib2.algorithm.localization.LocalizationUtils;
import net.imglib2.algorithm.localization.Observation;
import net.imglib2.algorithm.localization.StartPointEstimator;
import net.imglib2.type.numeric.RealType;

/**
 * @author Jean-Yves Tinevez - 2013
 */
public class PeakFitter< T extends RealType< T > > implements MultiThreaded, OutputAlgorithm< Map< Localizable, double[] > >, Benchmark
{

	private static final String BASE_ERROR_MESSAGE = "PeakFitter: ";

	private final RandomAccessibleInterval< T > image;

	private final Collection< Localizable > peaks;

	private final FunctionFitter fitter;

	private final FitFunction peakFunction;

	private final StartPointEstimator estimator;

	private ConcurrentHashMap< Localizable, double[] > results;

	private int numThreads;

	private final StringBuffer errorHolder = new StringBuffer();

	private long processingTime;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * @param image
	 *            the image to operate on.
	 */
	public PeakFitter( final RandomAccessibleInterval< T > image, final Collection< Localizable > peaks, final FunctionFitter fitter, final FitFunction peakFunction, final StartPointEstimator estimator )
	{
		this.image = image;
		this.fitter = fitter;
		this.peakFunction = peakFunction;
		this.estimator = estimator;
		this.peaks = peaks;
		setNumThreads();
	}

	/*
	 * METHODS
	 */

	@Override
	public String toString()
	{
		return "PeakFitter configured to:\n" +
				" - fit a " + peakFunction.toString() + "\n" +
				" - on " + peaks.size() + " peaks\n" +
				" - in image " + image + "\n" +
				" - using " + estimator.toString() + "\n" +
				" - and " + fitter.toString() + "\n" +
				" - allocating " + numThreads + " threads.";
	}

	@Override
	public boolean checkInput()
	{
		if ( null == image )
		{
			errorHolder.append( BASE_ERROR_MESSAGE + "Image is null." );
			return false;
		}
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorHolder.toString();
	}

	@Override
	public boolean process()
	{

		final long start = System.currentTimeMillis();

		results = new ConcurrentHashMap< Localizable, double[] >( peaks.size() );
		final long[] padSize = estimator.getDomainSpan();

		final ExecutorService workers = Executors.newFixedThreadPool( numThreads );
		for ( final Localizable peak : peaks )
		{
			final Runnable task = new Runnable()
			{

				@Override
				public void run()
				{
					final Observation data = LocalizationUtils.gatherObservationData( image, peak, padSize );
					final double[] params = estimator.initializeFit( peak, data );
					try
					{
						final double[][] X = data.X;
						final double[] I = data.I;
						fitter.fit( X, I, params, peakFunction );
					}
					catch ( final Exception e )
					{
						errorHolder.append( BASE_ERROR_MESSAGE +
								"Problem fitting around " + peak +
								": " + e.getMessage() + ".\n" );
					}
					results.put( peak, params );
				}

			};
			workers.execute( task );
		}

		workers.shutdown();
		boolean ok = true;
		try
		{
			ok = workers.awaitTermination( 1, TimeUnit.HOURS );
			if ( !ok )
			{
				System.err.println( "Timeout reached while processing." );
			}
		}
		catch ( final InterruptedException e )
		{
			e.printStackTrace();
		}

		final long end = System.currentTimeMillis();
		processingTime = end - start;

		return ok;
	}

	@Override
	public Map< Localizable, double[] > getResult()
	{
		return results;
	}

	@Override
	public void setNumThreads()
	{
		this.numThreads = Runtime.getRuntime().availableProcessors();
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
}
