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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.util.Threads;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.algorithm.labeling.ConnectedComponents.StructuringElement;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.parallel.Parallelization;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegionCursor;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class MaskUtils
{

	public static final < T extends RealType< T > > double otsuThreshold( final RandomAccessibleInterval< T > img )
	{
		// Min & max
		final T t = Util.getTypeFromInterval( img );
		final T max = t.createVariable();
		max.setReal( Double.NEGATIVE_INFINITY );
		final T min = t.createVariable();
		min.setReal( Double.POSITIVE_INFINITY );

		for ( final T pixel : Views.iterable( img ) )
		{
			if ( pixel.compareTo( min ) < 0 )
				min.set( pixel );

			if ( pixel.compareTo( max ) > 0 )
				max.set( pixel );
		}

		// Histogram.
		final Real1dBinMapper< T > mapper = new Real1dBinMapper<>( min.getRealDouble(), max.getRealDouble(), 256, false );
		final Histogram1d< T > histogram = new Histogram1d<>( Views.iterable( img ), mapper );

		// Threshold.
		final long k = getThreshold( histogram );
		final T val = t.createVariable();
		mapper.getCenterValue( k, val );
		return val.getRealDouble();
	}

	public static final long getThreshold( final Histogram1d< ? > hist )
	{
		final long[] histogram = hist.toLongArray();
		// Otsu's threshold algorithm
		// C++ code by Jordan Bevik <Jordan.Bevic@qtiworld.com>
		// ported to ImageJ plugin by G.Landini
		int k, kStar; // k = the current threshold; kStar = optimal threshold
		final int L = histogram.length; // The total intensity of the image
		long N1, N; // N1 = # points with intensity <=k; N = total number of
		// points
		long Sk; // The total intensity for all histogram points <=k
		long S;
		double BCV, BCVmax; // The current Between Class Variance and maximum
		// BCV
		double num, denom; // temporary bookkeeping

		// Initialize values:
		S = 0;
		N = 0;
		for ( k = 0; k < L; k++ )
		{
			S += k * histogram[ k ]; // Total histogram intensity
			N += histogram[ k ]; // Total number of data points
		}

		Sk = 0;
		N1 = histogram[ 0 ]; // The entry for zero intensity
		BCV = 0;
		BCVmax = 0;
		kStar = 0;

		// Look at each possible threshold value,
		// calculate the between-class variance, and decide if it's a max
		for ( k = 1; k < L - 1; k++ )
		{ // No need to check endpoints k = 0 or k = L-1
			Sk += k * histogram[ k ];
			N1 += histogram[ k ];

			// The float casting here is to avoid compiler warning about loss of
			// precision and
			// will prevent overflow in the case of large saturated images
			denom = ( double ) ( N1 ) * ( N - N1 ); // Maximum value of denom is
			// (N^2)/4 =
			// approx. 3E10

			if ( denom != 0 )
			{
				// Float here is to avoid loss of precision when dividing
				num = ( ( double ) N1 / N ) * S - Sk; // Maximum value of num =
				// 255*N =
				// approx 8E7
				BCV = ( num * num ) / denom;
			}
			else
				BCV = 0;

			if ( BCV >= BCVmax )
			{ // Assign the best threshold found so far
				BCVmax = BCV;
				kStar = k;
			}
		}
		// kStar += 1; // Use QTI convention that intensity -> 1 if intensity >=
		// k
		// (the algorithm was developed for I-> 1 if I <= k.)
		return kStar;
	}

	/**
	 * Creates a zero-min label image from a thresholded input image.
	 *
	 * @param <T>
	 *            the type of the input image. Must be real, scalar.
	 * @param input
	 *            the input image.
	 * @param interval
	 *            the interval in the input image to analyze.
	 * @param threshold
	 *            the threshold to apply to the input image.
	 * @param numThreads
	 *            how many threads to use for multithreaded computation.
	 * @return a new label image.
	 */
	public static final < T extends RealType< T > > ImgLabeling< Integer, IntType > toLabeling(
			final RandomAccessibleInterval< T > input,
			final double threshold,
			final int numThreads )
	{
		// To mask.
		final Converter< T, BitType > converter = ( a, b ) -> b.set( a.getRealDouble() > threshold );
		final RandomAccessible< BitType > bitMask = Converters.convertRAI( input, converter, new BitType() );

		// Prepare output.
		final ImgFactory< IntType > factory = Util.getArrayOrCellImgFactory( input, new IntType() );
		final Img< IntType > out = factory.create( input );
		final ImgLabeling< Integer, IntType > labeling = new ImgLabeling<>( out );

		// Structuring element.
		final StructuringElement se = StructuringElement.FOUR_CONNECTED;

		// Get connected components.
		final ExecutorService executorService = numThreads > 1
				? Threads.newFixedThreadPool( numThreads )
				: Threads.newSingleThreadExecutor();

		ConnectedComponents.labelAllConnectedComponents(
				bitMask,
				labeling,
				MaskUtils.labelGenerator(),
				se,
				executorService );
		executorService.shutdown();
		return labeling;
	}

	/**
	 * Creates spots from a grayscale image, thresholded to create a mask. A
	 * spot is created for each connected-component of the mask, with a size
	 * that matches the mask size.
	 *
	 * @param <T>
	 *            the type of the input image. Must be real, scalar.
	 * @param input
	 *            the input image.
	 * @param interval
	 *            the interval in the input image to analyze.
	 * @param calibration
	 *            the physical calibration.
	 * @param threshold
	 *            the threshold to apply to the input image.
	 * @param numThreads
	 *            how many threads to use for multithreaded computation.
	 * @return a list of spots, without ROI.
	 */
	public static < T extends RealType< T > > List< Spot > fromThreshold(
			final RandomAccessible< T > input,
			final Interval interval,
			final double[] calibration,
			final double threshold,
			final int numThreads )
	{
		/*
		 * Crop.
		 */
		final IntervalView< T > crop = Views.interval( input, interval );
		final IntervalView< T > in = Views.zeroMin( crop );

		// Get labeling from mask.
		final ImgLabeling< Integer, IntType > labeling = toLabeling(
				in,
				threshold,
				numThreads );
		return fromLabeling(
				labeling,
				interval.minAsDoubleArray(),
				calibration );
	}

	/**
	 * Creates spots from a label image.
	 *
	 * @param <R>
	 *            the type that backs-up the labeling.
	 * @param labeling
	 *            the labeling, must be zero-min.
	 * @param origin
	 *            the origin (min pos) of the interval the labeling was
	 *            generated from, used to reposition the spots from the zero-min
	 *            labeling to the proper coordinates.
	 * @param calibration
	 *            the physical calibration.
	 * @return a list of spots, without ROI.
	 */
	public static < R extends IntegerType< R > > List< Spot > fromLabeling(
			final ImgLabeling< Integer, R > labeling,
			final double[] origin,
			final double[] calibration )
	{
		// Parse each component.
		final LabelRegions< Integer > regions = new LabelRegions<>( labeling );
		final Iterator< LabelRegion< Integer > > iterator = regions.iterator();
		final List< Spot > spots = new ArrayList<>( regions.getExistingLabels().size() );
		while ( iterator.hasNext() )
		{
			final LabelRegion< Integer > region = iterator.next();
			final LabelRegionCursor cursor = region.localizingCursor();
			final int[] cursorPos = new int[ labeling.numDimensions() ];
			final long[] sum = new long[ 3 ];
			while ( cursor.hasNext() )
			{
				cursor.fwd();
				cursor.localize( cursorPos );
				for ( int d = 0; d < sum.length; d++ )
					sum[ d ] += cursorPos[ d ];
			}

			final double[] pos = new double[ 3 ];
			for ( int d = 0; d < pos.length; d++ )
				pos[ d ] = sum[ d ] / ( double ) region.size();

			final double x = calibration[ 0 ] * ( origin[ 0 ] + pos[ 0 ] );
			final double y = calibration[ 1 ] * ( origin[ 1 ] + pos[ 1 ] );
			final double z = calibration[ 2 ] * ( origin[ 2 ] + pos[ 2 ] );

			double volume = region.size();
			for ( int d = 0; d < calibration.length; d++ )
				if ( calibration[ d ] > 0 )
					volume *= calibration[ d ];
			final double radius = ( labeling.numDimensions() == 2 )
					? Math.sqrt( volume / Math.PI )
					: Math.pow( 3. * volume / ( 4. * Math.PI ), 1. / 3. );
			final double quality = region.size();
			spots.add( new SpotBase( x, y, z, radius, quality ) );
		}

		return spots;
	}

	/**
	 * Creates spots by thresholding a grayscale image. A spot is created for
	 * each connected-component object in the thresholded input, with a size
	 * that matches the mask size. The quality of the spots is read from another
	 * image, by taking the max pixel value of this image with the ROI.
	 *
	 * @param <T>
	 *            the pixel type of the input image. Must be real, scalar.
	 * @param <R>
	 *            the pixel type of the quality image. Must be real, scalar.
	 * @param input
	 *            the input image.
	 * @param interval
	 *            the interval in the input image to analyze.
	 * @param calibration
	 *            the physical calibration.
	 * @param threshold
	 *            the threshold to apply to the input image.
	 * @param numThreads
	 *            how many threads to use for multithreaded computation.
	 * @param qualityImage
	 *            the image in which to read the quality value.
	 * @return a list of spots, without ROI.
	 */
	public static < T extends RealType< T >, R extends RealType< R > > List< Spot > fromThreshold(
			final RandomAccessible< T > input,
			final Interval interval,
			final double[] calibration,
			final double threshold,
			final int numThreads,
			final RandomAccessibleInterval< R > qualityImage )
	{
		// Crop.
		final IntervalView< T > crop = Views.interval( input, interval );
		final IntervalView< T > in = Views.zeroMin( crop );

		// Get labeling from mask.
		final ImgLabeling< Integer, IntType > labeling = toLabeling(
				in,
				threshold,
				numThreads );

		// Crop of the quality image.
		final IntervalView< R > cropQuality = Views.interval( qualityImage, interval );
		final IntervalView< R > inQuality = Views.zeroMin( cropQuality );
		final RandomAccess< R > raQuality = inQuality.randomAccess( inQuality );

		// Parse each component.
		final LabelRegions< Integer > regions = new LabelRegions<>( labeling );
		final Iterator< LabelRegion< Integer > > iterator = regions.iterator();
		final List< Spot > spots = new ArrayList<>( regions.getExistingLabels().size() );
		while ( iterator.hasNext() )
		{
			final LabelRegion< Integer > region = iterator.next();
			final LabelRegionCursor cursor = region.localizingCursor();
			final int[] cursorPos = new int[ labeling.numDimensions() ];
			final long[] sum = new long[ 3 ];
			double quality = Double.NEGATIVE_INFINITY;
			while ( cursor.hasNext() )
			{
				cursor.fwd();

				// Position.
				cursor.localize( cursorPos );
				for ( int d = 0; d < sum.length; d++ )
					sum[ d ] += cursorPos[ d ];

				// Quality
				raQuality.setPosition( cursor );
				final double q = raQuality.get().getRealDouble();
				if ( q > quality )
					quality = q;
			}

			final double[] pos = new double[ 3 ];
			for ( int d = 0; d < pos.length; d++ )
				pos[ d ] = sum[ d ] / ( double ) region.size();

			final double x = calibration[ 0 ] * ( interval.min( 0 ) + pos[ 0 ] );
			final double y = calibration[ 1 ] * ( interval.min( 1 ) + pos[ 1 ] );
			final double z = calibration[ 2 ] * ( interval.min( 2 ) + pos[ 2 ] );

			double volume = region.size();
			for ( int d = 0; d < calibration.length; d++ )
				if ( calibration[ d ] > 0 )
					volume *= calibration[ d ];

			final double radius = ( labeling.numDimensions() == 2 )
					? Math.sqrt( volume / Math.PI )
					: Math.pow( 3. * volume / ( 4. * Math.PI ), 1. / 3. );
			spots.add( new SpotBase( x, y, z, radius, quality ) );
		}

		return spots;
	}

	/**
	 * Creates spots <b>with their ROIs or meshes</b> from a <b>2D or 3D</b>
	 * mask. A spot is created for each connected-component of the mask, with a
	 * size that matches the mask size. The quality of the spots is read from
	 * another image, by taking the max pixel value of this image with the ROI.
	 *
	 * @param <T>
	 *            the type of the input image. Must be real, scalar.
	 * @param <S>
	 *            the type of the quality image. Must be real, scalar.
	 * @param input
	 *            the input mask image. Can be 2D or 3D. It does not have to be
	 *            of boolean type: every pixel with a real value strictly larger
	 *            than 0.5 will be considered <code>true</code> and
	 *            <code>false</code> otherwise.
	 * @param interval
	 *            the interval in the input image to analyze.
	 * @param calibration
	 *            the physical calibration.
	 * @param simplify
	 *            if <code>true</code> the polygon will be post-processed to be
	 *            smoother and contain less points.
	 * @param numThreads
	 *            how many threads to use for multithreaded computation.
	 * @param smoothingScale
	 *            if strictly larger than 0, the mask will be smoothed before
	 *            creating the mesh, resulting in smoother meshes. The scale
	 *            value sets the (Gaussian) filter radius and is specified in
	 *            physical units. If 0 or lower than 0, no smoothing is applied.
	 * @param qualityImage
	 *            the image in which to read the quality value.
	 * @return a list of spots, with ROI.
	 */
	public static < T extends RealType< T > & NativeType< T >, S extends RealType< S > > List< Spot > fromMaskWithROI(
			final RandomAccessible< T > input, 
			final Interval interval, 
			final double[] calibration, 
			final boolean simplify, 
			final double smoothingScale,
			final int numThreads, 
			final RandomAccessibleInterval< S > qualityImage )
	{
		final double threshold = 0.5;
		return fromThresholdWithROI(
				input,
				interval,
				calibration,
				threshold,
				simplify,
				smoothingScale,
				numThreads,
				qualityImage );
	}

	/**
	 * Creates spots <b>with their ROIs or meshes</b> from a <b>2D or 3D</b> by
	 * thresholding a grayscale image. A spot is created for each object in the
	 * thresholded image. The quality of the spots is read from another image,
	 * by taking the max pixel value of this image with the ROI.
	 *
	 * @param <T>
	 *            the type of the input image. Must be real, scalar.
	 * @param <S>
	 *            the type of the quality image. Must be real, scalar.
	 * @param input
	 *            the input image. Can be 2D or 3D.
	 * @param interval
	 *            the interval in the input image to analyze.
	 * @param calibration
	 *            the physical calibration.
	 * @param threshold
	 *            the threshold to apply to the input image.
	 * @param simplify
	 *            if <code>true</code> the polygon will be post-processed to be
	 *            smoother and contain less points.
	 * @param smoothingScale
	 *            if strictly larger than 0, the input will be smoothed before
	 *            creating the contour, resulting in smoother contours. The
	 *            scale value sets the (Gaussian) filter radius and is specified
	 *            in physical units. If 0 or lower than 0, no smoothing is
	 *            applied.
	 * @param numThreads
	 *            how many threads to use for multithreaded computation.
	 * @param qualityImage
	 *            the image in which to read the quality value.
	 * @return a list of spots, with ROI.
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static final < T extends RealType< T > & NativeType< T >, S extends RealType< S > > List< Spot > fromThresholdWithROI(
			final RandomAccessible< T > input,
			final Interval interval,
			final double[] calibration,
			final double threshold,
			final boolean simplify,
			final double smoothingScale,
			final int numThreads,
			final RandomAccessibleInterval< S > qualityImage )
	{
		/*
		 * Crop.
		 */
		final IntervalView< T > crop = Views.interval( input, interval );
		final IntervalView< T > in = Views.zeroMin( crop );

		/*
		 * Possibly filter.
		 */
		final RandomAccessibleInterval< T > filtered;
		if ( smoothingScale > 0. )
		{
			final double[] sigmas = new double[ in.numDimensions() ];
			for ( int d = 0; d < sigmas.length; d++ )
				sigmas[ d ] = smoothingScale / Math.sqrt( in.numDimensions() ) / calibration[ d ];

			filtered = ( RandomAccessibleInterval ) Util.getArrayOrCellImgFactory( in, new FloatType() ).create( in );
			Parallelization.runWithNumThreads( numThreads,
					() -> Gauss3.gauss( sigmas, Views.extendMirrorDouble( in ), filtered ) );
		}
		else
		{
			filtered = in;
		}

		if ( input.numDimensions() == 2 )
		{
			/*
			 * In 2D: Threshold, make a labeling, then create contours.
			 */
			return SpotRoiUtils.from2DThresholdWithROI(
					filtered,
					interval.minAsDoubleArray(),
					calibration,
					threshold,
					simplify,
					qualityImage );
		}
		else if ( input.numDimensions() == 3 )
		{
			/*
			 * In 3D: Directly operate on grayscale to create a big mesh,
			 * separate it in connected components, remerge them based on
			 * bounding-box before creating spots. We want to use the grayscale
			 * version of marching-cubes to have nice, smooth meshes.
			 */
			return SpotMeshUtils.from3DThresholdWithROI(
					filtered,
					interval.minAsDoubleArray(),
					calibration,
					threshold,
					simplify,
					qualityImage );
		}
		else
		{
			throw new IllegalArgumentException( "Can only process 2D or 3D images with this method, but got " + input.numDimensions() + "D." );
		}
	}

	/**
	 * Start at 1.
	 *
	 * @return a new iterator that goes like 1, 2, 3, ...
	 */
	public static final Iterator< Integer > labelGenerator()
	{
		return new Iterator< Integer >()
		{

			private int currentVal = 0;

			@Override
			public Integer next()
			{
				currentVal++;
				return Integer.valueOf( currentVal );
			}

			@Override
			public boolean hasNext()
			{
				return true;
			}
		};
	}
}
