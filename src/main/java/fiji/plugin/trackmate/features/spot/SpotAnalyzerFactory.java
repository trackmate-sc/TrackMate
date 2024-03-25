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
package fiji.plugin.trackmate.features.spot;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Interface for factories that can generate a {@link SpotAnalyzer} configured
 * to operate on a specific frame of a model.
 * <p>
 * Concrete implementation should declare what features they can compute
 * numerically, and make this info available in the
 * {@link fiji.plugin.trackmate.providers.SpotAnalyzerProvider} that returns
 * them.
 * <p>
 * Feature key names are for historical reason all capitalized in an enum
 * manner. For instance: POSITION_X, MAX_INTENSITY, etc... They must be suitable
 * to be used as a attribute key in an xml file.
 *
 * @author Jean-Yves Tinevez - 2012
 */
public interface SpotAnalyzerFactory< T extends RealType< T > & NativeType< T > > extends SpotAnalyzerFactoryBase< T >
{}
