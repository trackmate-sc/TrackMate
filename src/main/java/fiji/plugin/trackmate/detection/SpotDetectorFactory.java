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
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * For detectors that process one time-point at a time, independently, and for
 * which we can therefore propose multithreading.
 * <p>
 * These classes are able to configure a {@link SpotDetector} to operate on a
 * single time-point of the target <code>ImgPlus</code>.
 * 
 * @author Jean-Yves Tinevez
 *
 * @param <T>
 *            the pixel type in the image processed by the detector.
 */
public interface SpotDetectorFactory< T extends RealType< T > & NativeType< T > > extends SpotDetectorFactoryBase< T >
{

	/**
	 * Returns a new {@link SpotDetector} configured to operate on the given
	 * target frame. This factory must be first given the <code>ImgPlus</code>
	 * and the settings map, through the <code>#setTarget(ImgPlus, Map)</code>
	 * method.
	 *
	 * @param interval
	 *            the interval that determines the region in the source image to
	 *            operate on. This must <b>not</b> have a dimension for time
	 *            (<i>e.g.</i> if the source image is 2D+T (3D), then the
	 *            interval must be 2D; if the source image is 3D without time,
	 *            then the interval must be 3D).
	 * @param frame
	 *            the frame index in the source image to operate on
	 * @return a new detector.
	 */
	public SpotDetector< T > getDetector( final Interval interval, int frame );
}
