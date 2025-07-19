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

import java.util.Map;

import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Interface for detector factories that need to process all the time-points at
 * once. They return a detector that will be given all the time-points and will
 * have to manage multi-threading by itself.
 *
 * @author Jean-Yves Tinevez
 *
 * @param <T>
 */
public interface SpotGlobalDetectorFactory< T extends RealType< T > & NativeType< T > > extends SpotDetectorFactoryBase< T >
{

	/**
	 * Returns a new {@link SpotGlobalDetector} configured to operate on all the
	 * time-points.
	 *
	 * @param img
	 *            the {@link ImgPlus} to operate on, possibly 5D.
	 * @param settings
	 *            the settings map, used to configure the detector.
	 * @param interval
	 *            the interval that determines the region in the source image to
	 *            operate on. This must <b>not</b> have a dimension for time
	 *            (<i>e.g.</i> if the source image is 2D+T (3D), then the
	 *            interval must be 2D; if the source image is 3D without time,
	 *            then the interval must be 3D), not channel.
	 */
	public SpotGlobalDetector< T > getDetector( final ImgPlus< T > img, final Map< String, Object > settings, final Interval interval );

}
