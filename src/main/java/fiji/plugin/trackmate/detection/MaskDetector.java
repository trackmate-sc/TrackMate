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

import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class MaskDetector< T extends RealType< T > & NativeType< T > > extends ThresholdDetector< T >
{

	private final static String BASE_ERROR_MESSAGE = "MaskDetector: ";

	/*
	 * CONSTRUCTORS
	 */

	public MaskDetector(
			final RandomAccessible< T > input,
			final Interval interval,
			final double[] calibration,
			final boolean simplify,
			final double smoothingScale )
	{
		super( input, interval, calibration, Double.NaN, simplify, smoothingScale );
		baseErrorMessage = BASE_ERROR_MESSAGE;
	}


	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();
		spots = MaskUtils.fromMaskWithROI(
				input,
				interval,
				calibration,
				simplify,
				smoothingScale,
				numThreads,
				null );
		final long end = System.currentTimeMillis();
		this.processingTime = end - start;
		return true;
	}
}
