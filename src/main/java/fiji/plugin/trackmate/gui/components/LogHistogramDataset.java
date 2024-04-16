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
package fiji.plugin.trackmate.gui.components;

import org.jfree.data.statistics.HistogramDataset;

/**
 * A {@link HistogramDataset} that returns the log of the count in each bin
 * (plus one), so as to have a logarithmic plot.
 * 
 * @author Jean-Yves Tinevez &lt;jeanyves.tinevez@gmail.com&gt; Dec 28, 2010
 *
 */
public class LogHistogramDataset extends HistogramDataset
{

	private static final long serialVersionUID = 6012084169414194555L;

	@Override
	public Number getY( int series, int item )
	{
		Number val = super.getY( series, item );
		return Math.log( 1 + val.doubleValue() );
	}

}
