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
package fiji.plugin.trackmate.detection;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.ToDoubleFunction;

import org.scijava.thread.ThreadService;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.Threads;
import fiji.plugin.trackmate.util.TMUtils;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.gradient.HessianMatrix;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.outofbounds.OutOfBoundsBorderFactory;
import net.imglib2.parallel.TaskExecutor;
import net.imglib2.parallel.TaskExecutors;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import net.imglib2.view.composite.CompositeIntervalView;
import net.imglib2.view.composite.RealComposite;

public class HessianDetector< T extends RealType< T > & NativeType< T > > implements SpotDetector< T >, MultiThreaded
{

	/*
	 * FIELDS
	 */

	private final static String BASE_ERROR_MESSAGE = "HessianDetector: ";

	private final RandomAccessible< T > img;

	private final Interval interval;

	private final double[] calibration;

	private final double radiusXY;

	private final double radiusZ;

	private final double threshold;

	private final boolean doSubPixelLocalization;

	private String errorMessage;

	private List< Spot > spots;

	private long processingTime;

	private int nTasks;

	private final boolean normalize;

	private final ExecutorService es;

	/*
	 * CONSTRUCTOR
	 */

	public HessianDetector(
			final RandomAccessible< T > img,
			final Interval interval,
			final double[] calibration,
			final double radiusXY,
			final double radiusZ,
			final double threshold,
			final boolean normalize,
			final boolean doSubPixelLocalization )
	{
		this.img = img;
		this.interval = DetectionUtils.squeeze( interval );
		this.calibration = calibration;
		this.radiusXY = radiusXY;
		this.radiusZ = radiusZ;
		this.threshold = threshold;
		this.normalize = normalize;
		this.doSubPixelLocalization = doSubPixelLocalization;
		setNumThreads();
		// Handle multithreading.
		final ThreadService threadService = TMUtils.getContext().getService( ThreadService.class );
		if ( threadService == null )
			es = Threads.newCachedThreadPool();
		else
			es = threadService.getExecutorService();
	}

	/*
	 * METHODS
	 */

	@Override
	public boolean checkInput()
	{
		if ( null == img )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Image is null.";
			return false;
		}
		if ( img.numDimensions() > 3 || img.numDimensions() < 2 )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Image must be 2D or 3D, got " + img.numDimensions() + "D.";
			return false;
		}
		return true;
	}

	@Override
	public boolean process()
	{
		spots = null;
		errorMessage = null;

		final long start = System.currentTimeMillis();

		final RoiManager roiManager = RoiManager.getInstance();
		boolean ok = true;
		if ( roiManager == null || roiManager.getCount() == 0 )
		{
			// Roi manager not shown or empty -> process all.
			spots = processInterval( interval );
			if ( spots == null )
				ok = false;
		}
		else
		{
			// We have individual Rois -> process them one by one separately.
			spots = new ArrayList<>();
			for ( final Roi roi : roiManager.getRoisAsArray() )
			{
				// Create interval from ROI.
				final Rectangle bounds = roi.getBounds();
				final long[] max = new long[ img.numDimensions() ];
				final long[] min = new long[ img.numDimensions() ];

				min[ 0 ] = bounds.x;
				max[ 0 ] = bounds.x + bounds.width;
				min[ 1 ] = bounds.y;
				max[ 1 ] = bounds.y + bounds.height;
				if ( interval.numDimensions() > 2 )
				{
					min[ 2 ] = interval.min( 2 );
					max[ 2 ] = interval.max( 2 );
				}
				final FinalInterval intervalroi = FinalInterval.wrap( min, max );
				final FinalInterval intersect = Intervals.intersect( interval, intervalroi );
				if ( Intervals.isEmpty( intersect ) )
					continue;

				// Process interval.
				final List< Spot > spotsThisRoi = processInterval( intersect );
				if ( spotsThisRoi == null )
				{
					ok = false;
					continue;
				}

				// Remove spots out of the Roi.
				final ArrayList< Spot > prunedSpots = new ArrayList<>();
				for ( final Spot spot : spotsThisRoi )
				{
					if ( roi.contains(
							( int ) Math.round( spot.getFeature( Spot.POSITION_X ) / calibration[ 0 ] ),
							( int ) Math.round( spot.getFeature( Spot.POSITION_Y ) / calibration[ 1 ] ) ) )
						prunedSpots.add( spot );
				}
				spots.addAll( prunedSpots );
			}
		}

		final long end = System.currentTimeMillis();
		this.processingTime = end - start;
		return ok;
	}

	private final List< Spot > processInterval( final Interval crop )
	{
		try
		{
			// Compute Hessian.
			final Img< FloatType > det = computeHessianDeterminant( crop, new FloatType() );

			// Normalize from 0 to 1.
			if ( normalize )
				DetectionUtils.normalize( det );

			// Translate back with respect to ROI.
			final long[] minopposite = new long[ crop.numDimensions() ];
			crop.min( minopposite );
			final IntervalView< FloatType > to = Views.translate( det, minopposite );

			// Find spots.
			return DetectionUtils.findLocalMaxima( to, threshold, calibration, radiusXY, doSubPixelLocalization, nTasks );
		}
		catch ( final IncompatibleTypeException | InterruptedException | ExecutionException e )
		{
			errorMessage = BASE_ERROR_MESSAGE + e.getMessage();
			e.printStackTrace();
			return null;
		}
	}

	private final < R extends RealType< R > & NativeType< R > > Img< R > computeHessianDeterminant( final Interval crop, final R type ) throws IncompatibleTypeException, InterruptedException, ExecutionException
	{
		// Squeeze singleton dimensions
		final int n = crop.numDimensions();

		// Sigmas in pixel units.
		final double[] radius = new double[] { radiusXY, radiusXY, radiusZ };
		final double[] sigmas = new double[ n ];
		for ( int d = 0; d < n; d++ )
		{
			final double cal = d < calibration.length ? calibration[ d ] : 1;
			sigmas[ d ] = radius[ d ] / cal / Math.sqrt( n );
		}

		// Get a suitable image factory.
		final long[] gradientDims = new long[ n + 1 ];
		final long[] hessianDims = new long[ n + 1 ];
		for ( int d = 0; d < n; d++ )
		{
			hessianDims[ d ] = crop.dimension( d );
			gradientDims[ d ] = crop.dimension( d );
		}
		hessianDims[ n ] = n * ( n + 1 ) / 2;
		gradientDims[ n ] = n;
		final Dimensions hessianDimensions = FinalDimensions.wrap( hessianDims );
		final FinalDimensions gradientDimensions = FinalDimensions.wrap( gradientDims );
		final ImgFactory< R > factory = Util.getArrayOrCellImgFactory( hessianDimensions, type );
		final Img< R > hessian = factory.create( hessianDimensions );
		final Img< R > gradient = factory.create( gradientDimensions );
		final Img< R > gaussian = factory.create( crop );

		// Hessian calculation.
		final IntervalView< T > input = Views.zeroMin( Views.interval( img, crop ) );
		HessianMatrix.calculateMatrix( input, gaussian,
				gradient, hessian, new OutOfBoundsBorderFactory<>(), nTasks, es,
				sigmas );

		// Normalize for pixel size.
		final IntervalView< R > H = HessianMatrix.scaleHessianMatrix( hessian, sigmas );

		// Compute determinant.
		final ToDoubleFunction< RealComposite< R > > detcalc;
		if ( n == 2 )
		{
			detcalc = ( c ) -> {
				final double a00 = c.get( 0 ).getRealDouble();
				final double a01 = c.get( 1 ).getRealDouble();
				final double a11 = c.get( 2 ).getRealDouble();
				final double det = a00 * a11 - a01 * a01;
				return det;
			};
		}
		else // n == 3
		{
			detcalc = ( c ) -> {
				final double a00 = c.get( 0 ).getRealDouble();
				final double a01 = c.get( 1 ).getRealDouble();
				final double a02 = c.get( 2 ).getRealDouble();
				final double a11 = c.get( 3 ).getRealDouble();
				final double a12 = c.get( 4 ).getRealDouble();
				final double a22 = c.get( 5 ).getRealDouble();

				final double x = a11 * a22 - a12 * a12;
				final double y = a01 * a22 - a02 * a12;
				final double z = a01 * a12 - a02 * a11;

				final double det = a00 * x - a01 * y + a02 * z;
				return -det;
				// Change sign so that bright detections have positive values.
			};
		}

		final CompositeIntervalView< R, RealComposite< R > > composite = Views.collapseReal( H );
		final Img< R > det = factory.create( crop );

		final TaskExecutor taskExecutor = TaskExecutors.forExecutorServiceAndNumTasks( es, nTasks );
		LoopBuilder.setImages( composite, det ).multiThreaded( taskExecutor ).forEachPixel(
				( c, d ) -> d.setReal( detcalc.applyAsDouble( c ) ) );

		return det;
	}

	@Override
	public List< Spot > getResult()
	{
		return spots;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public void setNumThreads()
	{
		this.nTasks = Runtime.getRuntime().availableProcessors() / 2;
	}

	@Override
	public void setNumThreads( final int nTasks )
	{
		this.nTasks = nTasks;
	}

	@Override
	public int getNumThreads()
	{
		return nTasks;
	}
}
