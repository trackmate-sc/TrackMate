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
package fiji.plugin.trackmate.action.fit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.Threads;
import fiji.plugin.trackmate.util.TMUtils;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imglib2.Cursor;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public abstract class AbstractSpotFitter implements SpotFitter
{

	@SuppressWarnings( "rawtypes" )
	protected final ImgPlus< RealType > img;

	protected final int channel;

	protected final double[] calibration;

	protected final LevenbergMarquardtOptimizer optimizer;

	@SuppressWarnings( "rawtypes" )
	private final ConcurrentHashMap< Integer, RandomAccessibleInterval< RealType > > hyperslices = new ConcurrentHashMap<>();

	private int numThreads;

	private long processingTime = -1;

	public AbstractSpotFitter( final ImagePlus imp, final int channel )
	{
		this.channel = channel;
		this.img = TMUtils.rawWraps( imp );
		this.calibration = TMUtils.getSpatialCalibration( imp );
		// Least-square fitting.
		this.optimizer = new LevenbergMarquardtOptimizer()
				.withCostRelativeTolerance( 1.0e-12 )
				.withParameterRelativeTolerance( 1.0e-12 );
		setNumThreads();
	}

	@Override
	public void process( final Iterable< Spot > spots, final Logger logger )
	{
		logger.log( String.format( "Starting fitting with %d threads.\n", numThreads ) );
		logger.setStatus( "Spot fitting" );
		final long start = System.currentTimeMillis();
		final ExecutorService executorService = Threads.newFixedThreadPool( numThreads );
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
		logger.setStatus( "" );
		logger.setProgress( 0. );
		logger.log( String.format( "Fit completed for %d spots in %.1f s.\n", nspots, processingTime / 1000. ) );
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	protected RandomAccessibleInterval< RealType > getSlice( final int frame )
	{
		return hyperslices
				.computeIfAbsent(
				Integer.valueOf( frame ),
				t -> TMUtils.hyperSlice( img, channel, t ) );
	}

	@Override
	public abstract void fit( final Spot spot );

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

	protected static void clipBackground( final Observation obs )
	{
		// Remove background and clip to 0.
		final double bg = Util.median( obs.values );
		for ( int i = 0; i < obs.values.length; i++ )
			obs.values[ i ] -= bg;
		for ( int i = 0; i < obs.values.length; i++ )
			obs.values[ i ] = Math.max( 0., obs.values[ i ] );
	}

	protected Observation gatherObservationData( final Point point, final long[] span, final int frame )
	{
		@SuppressWarnings( "rawtypes" )
		final RandomAccessibleInterval< RealType > slice = getSlice( frame );
		final int ndims = point.numDimensions();
		// Create interval.
		final long[] min = new long[ point.numDimensions() ];
		final long[] max = new long[ point.numDimensions() ];
		for ( int d = 0; d < max.length; d++ )
		{
			min[ d ] = Math.max( point.getLongPosition( d ) - span[ d ], slice.min( 0 ) );
			max[ d ] = Math.min( point.getLongPosition( d ) + span[ d ], slice.max( 0 ) );
		}

		// Collect.
		@SuppressWarnings( "rawtypes" )
		final IntervalView< RealType > view = Views.interval( slice, min, max );
		final int nel = ( int ) view.size();
		final double[] vals = new double[ nel ];
		final long[][] pos = new long[ ndims ][ nel ];
		@SuppressWarnings( "rawtypes" )
		final Cursor< RealType > cursor = view.localizingCursor();
		int index = -1;
		while ( cursor.hasNext() )
		{
			index++;
			cursor.fwd();
			vals[ index ] = cursor.get().getRealDouble();
			for ( int d = 0; d < ndims; d++ )
				pos[ d ][ index ] = cursor.getLongPosition( d );
		}
		return new Observation( vals, pos );
	}

	protected static class Observation
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

		public Img< DoubleType > toImg()
		{
			final long[] dims = new long[ pos.length ];
			for ( int d = 0; d < dims.length; d++ )
			{
				final long max = Arrays.stream( pos[ d ] ).max().getAsLong();
				final long min = Arrays.stream( pos[ d ] ).min().getAsLong();
				dims[ d ] = max - min + 1;
			}
			final ArrayImg< DoubleType, DoubleArray > img = ArrayImgs.doubles( values, dims );
			return img;
		}
	}
}
