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
package fiji.plugin.trackmate.graph;

/**
 * Interface for function that compute values from a single input and store it
 * in an output.
 * 
 * @author Jean-Yves Inevez
 *
 * @param <T1>
 *            type of input instances
 * @param <T2>
 *            type of output instances
 */
public interface Function1< T1, T2 >
{

	/**
	 * Compute a value from the data in input, and store in output.
	 * 
	 * @param input
	 *            the input instance to compute on
	 * @param output
	 *            the output instance that will store the computation result
	 */
	public void compute( final T1 input, final T2 output );

}
