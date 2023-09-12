/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2023 TrackMate developers.
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
package fiji.plugin.trackmate.features.spot;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Special interface for spot analyzers that can compute feature values based on
 * the 2D contour of spots.
 *
 * @author Jean-Yves Tinevez - 2020
 */
public interface Spot2DMorphologyAnalyzerFactory< T extends RealType< T > & NativeType< T > > extends SpotAnalyzerFactoryBase< T >
{}
