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
package fiji.plugin.trackmate.features;

import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYSeriesCollection;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.util.ExportableChartPanel;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.util.XYEdgeSeriesCollection;

public class SpotFeatureGrapher extends AbstractFeatureGrapher
{

	private final List< Spot > spots;

	private final SelectionModel selectionModel;

	public SpotFeatureGrapher(
			final String xFeature,
			final List< String > yFeatures,
			final List< Spot > spots,
			final Model model,
			final SelectionModel selectionModel,
			final DisplaySettings displaySettings )
	{
		super(
				model,
				displaySettings,
				xFeature,
				yFeatures,
				model.getFeatureModel().getSpotFeatureDimensions().get( xFeature ),
				model.getFeatureModel().getSpotFeatureDimensions(),
				model.getFeatureModel().getSpotFeatureNames() );
		this.spots = spots;
		this.selectionModel = selectionModel;
	}

	/**
	 * Draws and renders the graph in a new JFrame.
	 * 
	 * @return a new JFrame, not shown yet.
	 */
	@Override
	public JFrame render()
	{
		// X label
		final String xAxisLabel = xFeature + " (" + TMUtils.getUnitsFor( xDimension, model.getSpaceUnits(), model.getTimeUnits() ) + ")";

		// Find how many different dimensions
		final Set< Dimension > dimensions = getUniqueValues( yFeatures, yDimensions );

		// Generate one panel per different dimension
		final ArrayList< ExportableChartPanel > chartPanels = new ArrayList<>( dimensions.size() );
		for ( final Dimension dimension : dimensions )
		{

			// Y label
			final String yAxisLabel = TMUtils.getUnitsFor( dimension, model.getSpaceUnits(), model.getTimeUnits() );

			// Collect suitable feature for this dimension
			final List< String > featuresThisDimension = getCommonKeys( dimension, yFeatures, yDimensions );

			// Title
			final String title = buildPlotTitle( featuresThisDimension, featureNames );

			// Data-set for points (easy)
			final SpotCollectionDataset pointDataset = new SpotCollectionDataset(
					title,
					spots,
					xFeature,
					featuresThisDimension,
					model,
					selectionModel,
					displaySettings );

			// Point renderer
			final XYItemRenderer pointRenderer = pointDataset.getRenderer();

			// The chart
			final JFreeChart chart = ChartFactory.createXYLineChart( title, xAxisLabel, yAxisLabel, pointDataset, PlotOrientation.VERTICAL, true, true, false );
			chart.getTitle().setFont( FONT );
			chart.getLegend().setItemFont( SMALL_FONT );
			chart.setBackgroundPaint( bgColor );
			chart.setBorderVisible( false );
			chart.getLegend().setBackgroundPaint( bgColor );

			// The plot
			final XYPlot plot = chart.getXYPlot();
			plot.setRenderer( pointRenderer );
			plot.getRangeAxis().setLabelFont( FONT );
			plot.getRangeAxis().setTickLabelFont( SMALL_FONT );
			plot.getDomainAxis().setLabelFont( FONT );
			plot.getDomainAxis().setTickLabelFont( SMALL_FONT );
			plot.setOutlineVisible( false );
			plot.setDomainCrosshairVisible( false );
			plot.setDomainGridlinesVisible( false );
			plot.setRangeCrosshairVisible( false );
			plot.setRangeGridlinesVisible( false );
			plot.setBackgroundAlpha( 0f );

			// Plot range.
			( ( NumberAxis ) plot.getRangeAxis() ).setAutoRangeIncludesZero( false );

			// Ticks. Fewer of them.
			plot.getRangeAxis().setTickLabelInsets( new RectangleInsets( 20, 10, 20, 10 ) );
			plot.getDomainAxis().setTickLabelInsets( new RectangleInsets( 10, 20, 10, 20 ) );

			// The panel
			final ExportableChartPanel chartPanel = new ExportableChartPanel( chart );
			chartPanel.setPreferredSize( new java.awt.Dimension( 500, 270 ) );
			chartPanels.add( chartPanel );
		}

		return renderCharts( chartPanels );
	}

	/**
	 * Returns a new dataset that contains the values, specified from the given
	 * feature, and extracted from the spots given in constructor.
	 * 
	 * @return a new dataset.
	 */
	@Override
	protected XYSeriesCollection buildMainDataSet( final Iterable< String > targetYFeatures )
	{
		return null;
	}

	/**
	 * Returns a new dataset that contains the values, specified from the given
	 * feature, and extracted from all the given spots. The dataset returned is
	 * a {@link XYEdgeSeriesCollection}, made to plot the lines between 2 points
	 * representing 2 spots.
	 * 
	 * @return a new dataset.
	 */
	@Override
	protected XYEdgeSeriesCollection buildConnectionDataSet( final Iterable< String > targetYFeatures )
	{
		return null;
	}

	/**
	 * Returns the list of links that have their source and target in the given
	 * spot list.
	 * 
	 * @return a new list.
	 */
	private final List< DefaultWeightedEdge > getInsideEdges( final Collection< Spot > spots )
	{
		final int nspots = spots.size();
		final ArrayList< DefaultWeightedEdge > edges = new ArrayList<>( nspots );
		final TrackModel trackModel = model.getTrackModel();
		for ( final DefaultWeightedEdge edge : trackModel.edgeSet() )
		{
			final Spot source = trackModel.getEdgeSource( edge );
			final Spot target = trackModel.getEdgeTarget( edge );
			if ( spots.contains( source ) && spots.contains( target ) )
				edges.add( edge );

		}
		return edges;
	}
}
