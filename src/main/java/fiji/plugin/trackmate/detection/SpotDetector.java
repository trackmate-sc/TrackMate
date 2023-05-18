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

import java.util.List;

import fiji.plugin.trackmate.Spot;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.OutputAlgorithm;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Interface for Spot detector classes, that are able to detect or segment spots
 * in a single time-point 2D or 3D image.
 * <p>
 * Concrete implementation can be multithreaded. In that case TrackMate will
 * possible allocate some threads to each instance of this class.
 * 
 * @author Jean-Yves Tinevez, 2010 - 2012
 * 
 */
public interface SpotDetector< T extends RealType< T > & NativeType< T > > extends OutputAlgorithm< List< Spot > >, Benchmark
{}
