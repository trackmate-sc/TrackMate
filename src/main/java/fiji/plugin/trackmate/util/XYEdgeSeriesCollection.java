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

import java.util.ArrayList;
import java.util.List;

import org.jfree.data.DomainOrder;
import org.jfree.data.UnknownKeyException;
import org.jfree.data.general.AbstractSeriesDataset;
import org.jfree.data.xy.XYDataset;

public class XYEdgeSeriesCollection extends AbstractSeriesDataset implements XYDataset {

	/*
	 * FIELDS
	 */

	private static final long serialVersionUID = 1157323153460912998L;
	private ArrayList<XYEdgeSeries> seriesList = new ArrayList<>();
	
	/*
	 * PUBLIC METHODS
	 */
	
	/**
	 * Print out the content of this dataset
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(XYEdgeSeries series : seriesList) {
			sb.append(series.getKey() + ":\n");
			for (int i = 0; i < series.getItemCount(); i++) {
				sb.append("  x = " + series.getEdgeXStart(i) + " -> " + series.getEdgeXEnd(i) + ", y = " + series.getEdgeYStart(i) + " -> " + series.getEdgeYEnd(i) + '\n');
			}
		}
		return sb.toString();
	}

	@Override
	public int getSeriesCount() {
		return seriesList.size();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Comparable getSeriesKey(int seriesIndex) {
		return seriesList.get(seriesIndex).getKey();
	}
	
	public XYEdgeSeries getSeries(int seriesIndex) {
		return seriesList.get(seriesIndex);
	}
	
	public void addSeries(XYEdgeSeries series) {
		seriesList.add(series);
	}

	@SuppressWarnings("rawtypes")
	public XYEdgeSeries getSeries(Comparable key) {
		for(XYEdgeSeries s : seriesList) 
			if (s.getKey().equals(key)) 
				return s;
        throw new UnknownKeyException("Key not found: " + key);
	}
	
	public List<XYEdgeSeries> getSeries() {
		return seriesList;
	}
	
	/*
	 * XYDATASET METHODS
	 */

	@Override
	public DomainOrder getDomainOrder() {
		return DomainOrder.ASCENDING;
	}

	@Override
	public int getItemCount(int series) {
		return seriesList.get(series).getItemCount();
	}

	@Override
	public Number getX(int series, int item) {
		return seriesList.get(series).getEdgeXStart(item);
	}

	@Override
	public double getXValue(int series, int item) {
		return seriesList.get(series).getEdgeXStart(item).doubleValue();
	}

	@Override
	public Number getY(int series, int item) {
		return seriesList.get(series).getEdgeYStart(item);
	}

	@Override
	public double getYValue(int series, int item) {
		return seriesList.get(series).getEdgeYStart(item).doubleValue();
	}
	
}
