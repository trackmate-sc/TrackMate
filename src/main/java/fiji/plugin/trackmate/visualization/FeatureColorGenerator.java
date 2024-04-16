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
package fiji.plugin.trackmate.visualization;

import java.awt.Color;
import java.util.function.Function;

/**
 * Interface for color generator that can color objects based on a feature
 * identified by a String.
 * 
 * @author Jean-Yves Tinevez - 2013
 *
 * @param <K>
 *            the type of object to color.
 */
public interface FeatureColorGenerator< K > extends Function< K, Color >
{

	/**
	 * Returns a color for the given object.
	 *
	 * @param obj
	 *            the object to color.
	 * @return a color for this object.
	 */
	public Color color( K obj );

	@Override
	default Color apply( final K obj )
	{
		return color( obj );
	}

}
