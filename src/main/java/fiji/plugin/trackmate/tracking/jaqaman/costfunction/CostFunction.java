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
package fiji.plugin.trackmate.tracking.jaqaman.costfunction;

/**
 * Interface representing a function that can calculate the cost to link a
 * source object to a target object.
 * 
 * @author Jean-Yves Tinevez - 2014
 * 
 * @param <K>
 *            the type of the sources.
 * @param <J>
 *            the type of the targets.
 */
public interface CostFunction< K, J >
{

	/**
	 * Returns the cost to link two objects.
	 * 
	 * @param source
	 *            the source object.
	 * @param target
	 *            the target object.
	 * @return the cost as a double.
	 */
	public double linkingCost( K source, J target );

}
