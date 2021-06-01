/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2021 Fiji developers.
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
package fiji.plugin.trackmate.util;

import org.jfree.data.general.Series;
import org.jfree.data.xy.XYSeries;

public class XYEdgeSeries extends Series {

	/*
	 * FIELDS
	 */
	
	private static final long serialVersionUID = -3716934680176727207L;
	private XYSeries startSeries = new XYSeries("StartPoints", false, true);
	private XYSeries endSeries = new XYSeries("EndPoints", false, true);
	
	/*
	 * CONSTRUCTOR
	 */
	
	@SuppressWarnings("rawtypes")
	public XYEdgeSeries(Comparable key) {
		super(key);
	}
	
	@SuppressWarnings("rawtypes")
	public XYEdgeSeries(Comparable key, String description) {
		super(key, description);
	}
	
	/*
	 * PUBLIC METHODS
	 */

	@Override
	public int getItemCount() {
		return startSeries.getItemCount();
	}
	
	public void addEdge(double x0, double y0, double x1, double y1) {
		startSeries.add(x0, y0, false);
		endSeries.add(x1, y1, false);
	}
	
	public Number getEdgeXStart(int index) {
		return startSeries.getX(index);
	}
	
	public Number getEdgeYStart(int index) {
		return startSeries.getY(index);
	}
	
	public Number getEdgeXEnd(int index) {
		return endSeries.getX(index);
	}
	
	public Number getEdgeYEnd(int index) {
		return endSeries.getY(index);
	}
	
	
	
	
	

}
