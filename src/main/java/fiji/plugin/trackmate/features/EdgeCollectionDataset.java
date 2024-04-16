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
package fiji.plugin.trackmate.features;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.util.LineUtils;
import org.jfree.data.xy.XYDataset;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;

public class EdgeCollectionDataset extends ModelDataset
{

	private static final long serialVersionUID = 1L;


	private final List< DefaultWeightedEdge > edges;

	private final Map< Integer, Set< DefaultWeightedEdge > > edgeMap;

	private final Function< DefaultWeightedEdge, String > labelGenerator;

	public EdgeCollectionDataset(
			final Model model,
			final SelectionModel selectionModel,
			final DisplaySettings ds,
			final String xFeature,
			final List< String > yFeatures,
			final List< DefaultWeightedEdge > edges,
			final boolean addLines )
	{
		super( model, selectionModel, ds, xFeature, yFeatures );
		this.edges = edges;
		this.edgeMap = addLines ? createEdgeMap( edges, model.getTrackModel() ) : null;
		this.labelGenerator = edge -> String.format( "%s → %s",
				model.getTrackModel().getEdgeSource( edge ).getName(), model.getTrackModel().getEdgeTarget( edge ).getName() );
	}

	/**
	 * Pre-computes the edges we will have to plot as lines.
	 * 
	 * @param edges
	 *            the spot collection.
	 * @param trackModel
	 *            the graph.
	 * @return a new map.
	 */
	private static Map< Integer, Set< DefaultWeightedEdge > > createEdgeMap( final List< DefaultWeightedEdge > edges, final TrackModel trackModel )
	{
		final Map< Integer, Set< DefaultWeightedEdge > > edgeMap = new HashMap<>();
		for ( int i = 0; i < edges.size(); i++ )
		{
			final Set< DefaultWeightedEdge > successors = new HashSet<>();
			final DefaultWeightedEdge edge = edges.get( i );
			final Spot target = trackModel.getEdgeTarget( edge );
			for ( final DefaultWeightedEdge edgeCandidate : edges )
			{
				final Spot source = trackModel.getEdgeSource( edgeCandidate );
				if ( source == target )
					successors.add( edgeCandidate );

			}
			if ( !successors.isEmpty() )
				edgeMap.put( Integer.valueOf( i ), successors );
		}
		return edgeMap;
	}

	@Override
	public int getItemCount( final int series )
	{
		return edges.size();
	}

	@Override
	public String getSeriesKey( final int series )
	{
		if ( ( series < 0 ) || ( series >= getSeriesCount() ) )
			throw new IllegalArgumentException( "Series index out of bounds" );
		return model.getFeatureModel().getEdgeFeatureShortNames().get( yFeatures.get( series ) );
	}

	@Override
	public Number getX( final int series, final int item )
	{
		return model.getFeatureModel().getEdgeFeature( edges.get( item ), xFeature );
	}

	@Override
	public Number getY( final int series, final int item )
	{
		return model.getFeatureModel().getEdgeFeature( edges.get( item ), yFeatures.get( series ) );
	}

	@Override
	public XYItemRenderer getRenderer()
	{
		return new MyXYItemRenderer();
	}

	@Override
	public String getItemLabel( final int item )
	{
		final DefaultWeightedEdge edge = edges.get( item );
		return labelGenerator.apply( edge );
	}

	@Override
	public void setItemLabel( final int item, final String label )
	{}

	private final class MyXYItemRenderer extends XYLineAndShapeRenderer
	{

		private static final long serialVersionUID = 1L;

		@Override
		protected void drawPrimaryLine(
				final XYItemRendererState state,
				final Graphics2D g2,
				final XYPlot plot,
				final XYDataset dataset,
				final int pass,
				final int series,
				final int item,
				final ValueAxis domainAxis,
				final ValueAxis rangeAxis,
				final Rectangle2D dataArea )
		{
			if ( edgeMap == null || !edgeMap.containsKey( Integer.valueOf( item ) ) )
				return;

			final RectangleEdge xAxisLocation = plot.getDomainAxisEdge();
			final RectangleEdge yAxisLocation = plot.getRangeAxisEdge();
			final PlotOrientation orientation = plot.getOrientation();
			final String yFeature = yFeatures.get( series );
			final DefaultWeightedEdge sourceEdge = edges.get( item );
			final Set< DefaultWeightedEdge > edges = edgeMap.get( Integer.valueOf( item ) );
			for ( final DefaultWeightedEdge targetEdge : edges )
			{

				final Double x1 = model.getFeatureModel().getEdgeFeature( targetEdge, xFeature );
				final Double y1 = model.getFeatureModel().getEdgeFeature( targetEdge, yFeature );
				if ( x1 == null || y1 == null || x1.isNaN() || y1.isNaN() )
					continue;

				final Double x0 = model.getFeatureModel().getEdgeFeature( sourceEdge, xFeature );
				final Double y0 = model.getFeatureModel().getEdgeFeature( sourceEdge, yFeature );
				if ( x0 == null || y0 == null || x0.isNaN() || y0.isNaN() )
					continue;

				final double transX0 = domainAxis.valueToJava2D( x0, dataArea, xAxisLocation );
				final double transY0 = rangeAxis.valueToJava2D( y0, dataArea, yAxisLocation );

				final double transX1 = domainAxis.valueToJava2D( x1, dataArea, xAxisLocation );
				final double transY1 = rangeAxis.valueToJava2D( y1, dataArea, yAxisLocation );

				if ( Double.isNaN( transX0 ) || Double.isNaN( transY0 ) || Double.isNaN( transX1 ) || Double.isNaN( transY1 ) )
					continue;

				if ( orientation == PlotOrientation.HORIZONTAL )
					state.workingLine.setLine( transY0, transX0, transY1, transX1 );
				else if ( orientation == PlotOrientation.VERTICAL )
					state.workingLine.setLine( transX0, transY0, transX1, transY1 );
				final boolean visible = LineUtils.clipLine( state.workingLine, dataArea );
				if ( visible )
					drawFirstPassShape( g2, pass, series, item, state.workingLine );
			}
		}

		@Override
		public Paint getItemPaint( final int series, final int item )
		{
			final DefaultWeightedEdge edge = edges.get( item );
			if ( selectionModel != null && selectionModel.getEdgeSelection().contains( edge ) )
				return ds.getHighlightColor();

			final FeatureColorGenerator< DefaultWeightedEdge > edgeColorGenerator = FeatureUtils.createTrackColorGenerator( model, ds );
			return edgeColorGenerator.color( edges.get( item ) );
		}

		@Override
		public Stroke getItemStroke( final int series, final int item )
		{
			final DefaultWeightedEdge edge = edges.get( item );
			if ( selectionModel != null && selectionModel.getEdgeSelection().contains( edge ) )
				return selectionStroke;
			return stroke;
		}

		@Override
		public LegendItem getLegendItem( final int datasetIndex, final int series )
		{
			final LegendItem legendItem = super.getLegendItem( datasetIndex, series );
			legendItem.setFillPaint( Color.BLACK );
			legendItem.setLinePaint( Color.BLACK );
			legendItem.setOutlinePaint( Color.BLACK );
			return legendItem;
		}
	}
}
