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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import fiji.plugin.trackmate.Spot;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class LabelImageDetector< T extends RealType< T > & NativeType< T > > implements SpotDetector< T >
{

	private final static String BASE_ERROR_MESSAGE = "ThresholdDetector: ";

	/*
	 * FIELDS
	 */

	/** The mask. */
	protected RandomAccessible< T > input;

	protected String baseErrorMessage = BASE_ERROR_MESSAGE;

	protected String errorMessage;

	/** The list of {@link Spot} that will be populated by this detector. */
	protected List< Spot > spots = new ArrayList<>();

	/** The processing time in ms. */
	protected long processingTime;

	protected int numThreads;

	protected final Interval interval;

	protected final double[] calibration;

	/**
	 * If <code>true</code>, the contours will be smoothed and simplified.
	 */
	protected final boolean simplify;

	private final double smoothingScale;

	/*
	 * CONSTRUCTORS
	 */

	public LabelImageDetector(
			final RandomAccessible< T > input,
			final Interval interval,
			final double[] calibration,
			final boolean simplify,
			final double smoothingScale )
	{
		this.input = input;
		this.interval = DetectionUtils.squeeze( interval );
		this.calibration = calibration;
		this.simplify = simplify;
		this.smoothingScale = smoothingScale;
	}

	@Override
	public boolean checkInput()
	{
		return true;
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();
		final RandomAccessibleInterval< T > rai = Views.interval( input, interval );
		final T type = Util.getTypeFromInterval( rai );

		if ( type instanceof IntegerType )
		{
			processIntegerImg( ( RandomAccessibleInterval ) Views.zeroMin( rai ) );
		}
		else
		{
			final ImgFactory< IntType > factory = Util.getArrayOrCellImgFactory( interval, new IntType() );
			final Img< IntType > img = factory.create( interval );
			LoopBuilder
			.setImages( Views.zeroMin( rai ), img )
			.multiThreaded( false )
			.forEachPixel( ( i, o ) -> o.setReal( i.getRealDouble() ) );
			processIntegerImg( img );
		}
		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	private < R extends IntegerType< R > > void processIntegerImg( final RandomAccessibleInterval< R > rai )
	{
		// Get all labels.
		final AtomicInteger max = new AtomicInteger( 0 );
		Views.iterable( rai ).forEach( p -> {
			final int val = p.getInteger();
			if ( val != 0 && val > max.get() )
				max.set( val );
		} );
		final List< Integer > indices = new ArrayList<>( max.get() );
		for ( int i = 0; i < max.get(); i++ )
			indices.add( Integer.valueOf( i + 1 ) );

		final ImgLabeling< Integer, R > labeling = ImgLabeling.fromImageAndLabels( rai, indices );
		if ( input.numDimensions() == 2 )
		{
			spots = SpotRoiUtils.from2DLabelingWithROI(
					labeling,
					interval.minAsDoubleArray(),
					calibration,
					simplify,
					smoothingScale,
					null );
		}
		else if ( input.numDimensions() == 3 )
		{
			spots = SpotMeshUtils.from3DLabelingWithROI(
					labeling,
					interval.minAsDoubleArray(),
					calibration,
					simplify,
					smoothingScale,
					null );
		}
		else
		{
			spots = MaskUtils.fromLabeling(
					labeling,
					interval.minAsDoubleArray(),
					calibration );
		}
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
}
