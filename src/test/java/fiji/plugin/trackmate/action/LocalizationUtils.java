/*
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2021 Fiji developers.
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

package fiji.plugin.trackmate.action;

import java.util.Random;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.localization.EllipticGaussianOrtho;
import net.imglib2.algorithm.localization.Gaussian;
import net.imglib2.algorithm.localization.Observation;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * A collection of utility methods for localization algorithms.
 * 
 * @author Jean-Yves Tinevez
 */
public class LocalizationUtils
{

	private static final EllipticGaussianOrtho ellipticGaussian = new EllipticGaussianOrtho();

	private static final Gaussian gaussian = new Gaussian();

	private static final Random ran = new Random();

	public static final < T extends RealType< T >> void addEllipticGaussianSpotToImage( final RandomAccessibleInterval< T > img, final double[] params )
	{
		final IterableInterval< T > iterImg = Views.iterable( img );
		final Cursor< T > lc = iterImg.localizingCursor();
		final double[] position = new double[ img.numDimensions() ];
		double val;
		final T var = iterImg.firstElement().createVariable();
		while ( lc.hasNext() )
		{
			lc.fwd();
			position[ 0 ] = lc.getDoublePosition( 0 );
			position[ 1 ] = lc.getDoublePosition( 1 );
			val = ellipticGaussian.val( position, params );
			var.setReal( val );
			lc.get().add( var );
		}
	}

	public static final < T extends RealType< T >> void addGaussianSpotToImage( final RandomAccessibleInterval< T > img, final double[] params )
	{
		final IterableInterval< T > iterImg = Views.iterable( img );
		final Cursor< T > lc = iterImg.localizingCursor();
		final int nDims = img.numDimensions();
		final double[] position = new double[ nDims ];
		double val;
		final T var = iterImg.firstElement().createVariable();
		while ( lc.hasNext() )
		{
			lc.fwd();
			lc.localize( position );
			val = gaussian.val( position, params );
			var.setReal( val );
			lc.get().add( var );
		}
	}

	public static final < T extends RealType< T >> void addGaussianNoiseToImage( final RandomAccessibleInterval< T > img, final double sigma_noise )
	{
		final IterableInterval< T > iterImg = Views.iterable( img );
		final Cursor< T > lc = iterImg.localizingCursor();
		double val;
		final T var = iterImg.firstElement().createVariable();
		while ( lc.hasNext() )
		{
			lc.fwd();
			val = Math.max( 0, sigma_noise * ran.nextGaussian() );
			var.setReal( val );
			lc.get().add( var );
		}
	}

	/**
	 * Collects the points to build the observation array, by iterating in a
	 * hypercube around the given location. Points found out of the image are
	 * not included.
	 * 
	 * @param image
	 *            the source image to sample.
	 * @param point
	 *            the location around which to collect the samples
	 * @param span
	 *            the span size of the hypercube to sample, such that in
	 *            dimension <code>d</code>, the cube sampled if a of size
	 *            <code>2 x span[d] + 1</code>.
	 * @return an {@link Observation} object containing the sampled data.
	 */
	public static final < T extends RealType< T >> Observation gatherObservationData( final RandomAccessibleInterval< T > image, final Localizable point, final long[] span )
	{
		final int nDims = image.numDimensions();
		final long[] min = new long[ nDims ];
		final long[] max = new long[ nDims ];
		for ( int d = 0; d < max.length; d++ )
		{
			min[ d ] = point.getLongPosition( d ) - span[ d ];
			max[ d ] = point.getLongPosition( d ) + span[ d ];
		}
		final FinalInterval spanInterval = new FinalInterval( min, max );
		final FinalInterval interval = Intervals.intersect( spanInterval, image );
		final IntervalView< T > view = Views.interval( image, interval );

		final int nPixels = ( int ) view.size();
		final double[][] X = new double[ nPixels ][ nDims ];
		final double[] I = new double[ nPixels ];
		
		int index = 0;
		final Cursor< T > cursor = view.localizingCursor();
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			I[ index ] = cursor.get().getRealDouble();
			for ( int d = 0; d < nDims; d++ )
				X[ index ][ d ] = cursor.getDoublePosition( d );

			index++;
		}

		final Observation obs = new Observation();
		obs.I = I;
		obs.X = X;
		return obs;
	}

}
