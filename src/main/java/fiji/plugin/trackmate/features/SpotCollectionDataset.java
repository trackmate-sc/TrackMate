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

public class SpotCollectionDataset extends ModelDataset implements XYDataset
{

	private static final long serialVersionUID = 1L;

	private final List< Spot > spots;

	private final Map< Integer, Set< DefaultWeightedEdge > > edgeMap;

	public SpotCollectionDataset(
			final Model model,
			final SelectionModel selectionModel,
			final DisplaySettings ds,
			final String xFeature,
			final List< String > yFeatures,
			final List< Spot > spots,
			final boolean addLines )
	{
		super( model, selectionModel, ds, xFeature, yFeatures );
		this.spots = spots;
		this.edgeMap = addLines ? createEdgeMap( spots, model.getTrackModel() ) : null;
	}

	/**
	 * Precompute the edges we will have to plot as lines.
	 * 
	 * @param spots
	 *            the spot collection.
	 * @param trackModel
	 *            the graph.
	 * @return a new map.
	 */
	private static Map< Integer, Set< DefaultWeightedEdge > > createEdgeMap( final List< Spot > spots, final TrackModel trackModel )
	{
		final Map< Integer, Set< DefaultWeightedEdge > > edgeMap = new HashMap<>();
		for ( int i = 0; i < spots.size(); i++ )
		{
			final Spot source = spots.get( i );
			final Set< DefaultWeightedEdge > edges = new HashSet<>();
			for ( final Spot target : spots )
			{
				if ( source.getFeature( Spot.FRAME ).intValue() > target.getFeature( Spot.FRAME ).intValue() )
					continue;

				if ( !trackModel.containsEdge( source, target ) )
					continue;

				edges.add( trackModel.getEdge( source, target ) );
			}
			if ( !edges.isEmpty() )
				edgeMap.put( Integer.valueOf( i ), edges );
		}
		return edgeMap;
	}

	@Override
	public int getItemCount( final int series )
	{
		return spots.size();
	}

	@Override
	public String getItemLabel( final int item )
	{
		return spots.get( item ).getName();
	}

	@Override
	public void setItemLabel( final int item, final String label )
	{
		spots.get( item ).setName( label );
	}

	@Override
	public String getSeriesKey( final int series )
	{
		if ( ( series < 0 ) || ( series >= getSeriesCount() ) )
			throw new IllegalArgumentException( "Series index out of bounds" );
		return model.getFeatureModel().getSpotFeatureShortNames().get( yFeatures.get( series ) );
	}

	@Override
	public Number getX( final int series, final int item )
	{
		return spots.get( item ).getFeature( xFeature );
	}

	@Override
	public Number getY( final int series, final int item )
	{
		return spots.get( item ).getFeature( yFeatures.get( series ) );
	}
	@Override
	public XYItemRenderer getRenderer()
	{
		return new MyXYItemRenderer();
	}

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
			final Spot sourceSpot = spots.get( item );
			final Set< DefaultWeightedEdge > edges = edgeMap.get( Integer.valueOf( item ) );
			for ( final DefaultWeightedEdge edge : edges )
			{
				final Spot targetSpot = model.getTrackModel().getEdgeTarget( edge );

				final Double x1 = targetSpot.getFeature( xFeature );
				final Double y1 = targetSpot.getFeature( yFeature );
				if ( x1 == null || y1 == null || x1.isNaN() || y1.isNaN() )
					continue;

				final Double x0 = sourceSpot.getFeature( xFeature );
				final Double y0 = sourceSpot.getFeature( yFeature );
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
			final Spot spot = spots.get( item );
			if ( selectionModel != null && selectionModel.getSpotSelection().contains( spot ) )
				return ds.getHighlightColor();

			final FeatureColorGenerator< Spot > spotColorGenerator = FeatureUtils.createSpotColorGenerator( model, ds );
			return spotColorGenerator.color( spots.get( item ) );
		}

		@Override
		public Stroke getItemStroke( final int series, final int item )
		{
			final Spot spot = spots.get( item );
			if ( selectionModel != null && selectionModel.getSpotSelection().contains( spot ) )
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
