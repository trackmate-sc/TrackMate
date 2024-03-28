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

import fiji.plugin.trackmate.Spot;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ThresholdDetector< T extends RealType< T > & NativeType< T > > implements SpotDetector< T >, MultiThreaded
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

	protected final double threshold;

	/** If <code>true</code>, the contours will be simplified. */
	protected final boolean simplify;

	/**
	 * If strictly larger than 0, the mask will be smoothed before creating the
	 * mesh, resulting in smoother meshes. The scale value sets the (Gaussian)
	 * filter radius and is specified in physical units. If 0 or lower than 0,
	 * no smoothing is applied.
	 */
	protected final double smoothingScale;

	/*
	 * CONSTRUCTORS
	 */

	public ThresholdDetector(
			final RandomAccessible< T > input,
			final Interval interval,
			final double[] calibration,
			final double threshold,
			final boolean simplify,
			final double smoothingScale )
	{
		this.input = input;
		this.smoothingScale = smoothingScale;
		this.interval = DetectionUtils.squeeze( interval );
		this.calibration = calibration;
		this.threshold = threshold;
		this.simplify = simplify;
	}

	@Override
	public List< Spot > getResult()
	{
		return spots;
	}

	@Override
	public boolean checkInput()
	{
		if ( null == input )
		{
			errorMessage = baseErrorMessage + "Image is null.";
			return false;
		}
		if ( input.numDimensions() > 3 || input.numDimensions() < 2 )
		{
			errorMessage = baseErrorMessage + "Image must be 2D or 3D, got " + input.numDimensions() + "D.";
			return false;
		}
		return true;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();
		spots = MaskUtils.fromThresholdWithROI(
				input,
				interval,
				calibration,
				threshold,
				simplify,
				smoothingScale,
				numThreads,
				null );
		final long end = System.currentTimeMillis();
		this.processingTime = end - start;

		return true;
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
}
