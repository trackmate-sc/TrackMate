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

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYDataset;

public class XYEdgeRenderer extends AbstractXYItemRenderer
{

	private static final long serialVersionUID = -4565389588020243812L;

	@Override
	public void drawItem( final Graphics2D g2, final XYItemRendererState state, final Rectangle2D dataArea, final PlotRenderingInfo info, final XYPlot plot,
			final ValueAxis domainAxis, final ValueAxis rangeAxis, final XYDataset dataset, final int series, final int item, final CrosshairState crosshairState, final int pass )
	{

		// get the data point...
		final XYEdgeSeriesCollection edgeDataset = ( XYEdgeSeriesCollection ) dataset;
		final XYEdgeSeries s = edgeDataset.getSeries( series );

		final double x0 = s.getEdgeXStart( item ).doubleValue();
		final double y0 = s.getEdgeYStart( item ).doubleValue();
		final double x1 = s.getEdgeXEnd( item ).doubleValue();
		final double y1 = s.getEdgeYEnd( item ).doubleValue();

		if ( Double.isNaN( y1 ) || Double.isNaN( x1 ) || Double.isNaN( y0 ) || Double.isNaN( x0 ) )
			return;

		final RectangleEdge xAxisLocation = plot.getDomainAxisEdge();
		final RectangleEdge yAxisLocation = plot.getRangeAxisEdge();

		final double transX0 = domainAxis.valueToJava2D( x0, dataArea, xAxisLocation );
		final double transY0 = rangeAxis.valueToJava2D( y0, dataArea, yAxisLocation );

		final double transX1 = domainAxis.valueToJava2D( x1, dataArea, xAxisLocation );
		final double transY1 = rangeAxis.valueToJava2D( y1, dataArea, yAxisLocation );

		// only draw if we have good values
		if ( Double.isNaN( transX0 ) || Double.isNaN( transY0 ) || Double.isNaN( transX1 ) || Double.isNaN( transY1 ) )
			return;

		final PlotOrientation orientation = plot.getOrientation();
		if ( orientation == PlotOrientation.HORIZONTAL )
		{
			state.workingLine.setLine( transY0, transX0, transY1, transX1 );
		}
		else if ( orientation == PlotOrientation.VERTICAL )
		{
			state.workingLine.setLine( transX0, transY0, transX1, transY1 );
		}

		if ( state.workingLine.intersects( dataArea ) )
		{
			g2.setStroke( getItemStroke( series, item ) );
			g2.setPaint( getItemPaint( series, item ) );
			g2.draw( state.workingLine );
		}

	}

}
