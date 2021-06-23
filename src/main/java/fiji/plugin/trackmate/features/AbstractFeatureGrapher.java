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
import static fiji.plugin.trackmate.gui.Icons.TRACK_SCHEME_ICON;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYSeriesCollection;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.displaysettings.Colormap;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.util.ExportableChartPanel;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.util.XYEdgeRenderer;
import fiji.plugin.trackmate.util.XYEdgeSeriesCollection;

public abstract class AbstractFeatureGrapher
{

	protected static final Shape DEFAULT_SHAPE = new Ellipse2D.Double( -3, -3, 6, 6 );

	protected final String xFeature;

	private final Set< String > yFeatures;

	protected final Model model;

	protected final DisplaySettings displaySettings;

	private final Color bgColor = new Color( 220, 220, 220 );

	private final Dimension xDimension;

	private final Map< String, Dimension > yDimensions;

	protected Map< String, String > featureNames;

	public AbstractFeatureGrapher(
			final Model model,
			final DisplaySettings displaySettings,
			final String xFeature,
			final Set< String > yFeatures,
			final Dimension xDimension,
			final Map< String, Dimension > yDimensions,
			final Map< String, String > featureNames )
	{
		this.model = model;
		this.displaySettings = displaySettings;
		this.xFeature = xFeature;
		this.yFeatures = yFeatures;
		this.xDimension = xDimension;
		this.yDimensions = yDimensions;
		this.featureNames = featureNames;
	}

	/**
	 * Draws and renders the graph in a new JFrame.
	 * 
	 * @return a new JFrame, not shown yet.
	 */
	public JFrame render()
	{
		final Colormap colormap = displaySettings.getColormap();

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
			final XYSeriesCollection pointDataset = buildMainDataSet( featuresThisDimension );

			// Point renderer
			final XYLineAndShapeRenderer pointRenderer = new XYLineAndShapeRenderer();

			// Edge renderer
			final XYEdgeRenderer edgeRenderer = new XYEdgeRenderer();

			// Data-set for edges
			final XYEdgeSeriesCollection edgeDataset = buildConnectionDataSet( featuresThisDimension );

			// The chart
			final JFreeChart chart = ChartFactory.createXYLineChart( title, xAxisLabel, yAxisLabel, pointDataset, PlotOrientation.VERTICAL, true, true, false );
			chart.getTitle().setFont( FONT );
			chart.getLegend().setItemFont( SMALL_FONT );
			chart.setBackgroundPaint( bgColor );
			chart.setBorderVisible( false );
			chart.getLegend().setBackgroundPaint( bgColor );

			// The plot
			final XYPlot plot = chart.getXYPlot();
			if ( edgeDataset != null )
			{
				plot.setDataset( 1, edgeDataset );
				plot.setRenderer( 1, edgeRenderer );
			}
			plot.setRenderer( 0, pointRenderer );
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

			// Ticks. Fewer of them.
			plot.getRangeAxis().setTickLabelInsets( new RectangleInsets( 20, 10, 20, 10 ) );
			plot.getDomainAxis().setTickLabelInsets( new RectangleInsets( 10, 20, 10, 20 ) );

			// Paint
			pointRenderer.setUseOutlinePaint( true );
			if ( edgeDataset != null )
			{
				final int nseries = edgeDataset.getSeriesCount();
				for ( int i = 0; i < nseries; i++ )
				{
					pointRenderer.setSeriesOutlinePaint( i, Color.black );
					pointRenderer.setSeriesLinesVisible( i, false );
					pointRenderer.setSeriesShape( i, DEFAULT_SHAPE, false );
					pointRenderer.setSeriesPaint( i, colormap.getPaint( ( double ) i / nseries ), false );
					edgeRenderer.setSeriesPaint( i, colormap.getPaint( ( double ) i / nseries ), false );
				}
			}

			// The panel
			final ExportableChartPanel chartPanel = new ExportableChartPanel( chart );
			chartPanel.setPreferredSize( new java.awt.Dimension( 500, 270 ) );
			chartPanels.add( chartPanel );
		}

		return renderCharts( chartPanels );
	}

	protected abstract XYSeriesCollection buildMainDataSet( final Iterable< String > targetYFeatures );

	protected abstract XYEdgeSeriesCollection buildConnectionDataSet( final Iterable< String > targetYFeatures );

	/*
	 * UTILS
	 */

	/**
	 * Renders and display a frame containing all the char panels, grouped by
	 * dimension.
	 * 
	 * @return a new JFrame, not shown yet.
	 */
	private final JFrame renderCharts( final List< ExportableChartPanel > chartPanels )
	{
		// The Panel
		final JPanel panel = new JPanel();
		final BoxLayout panelLayout = new BoxLayout( panel, BoxLayout.Y_AXIS );
		panel.setLayout( panelLayout );
		for ( final ExportableChartPanel chartPanel : chartPanels )
		{
			panel.add( chartPanel );
			panel.add( Box.createVerticalStrut( 5 ) );
		}

		// Scroll pane
		final JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		scrollPane.setViewportView( panel );
		scrollPane.getVerticalScrollBar().setUnitIncrement( 16 );

		// The frame
		final JFrame frame = new JFrame();
		frame.setTitle( "Feature plot for Track scheme" );
		frame.setIconImage( TRACK_SCHEME_ICON.getImage() );
		frame.getContentPane().add( scrollPane );
		frame.validate();
		frame.setSize( new java.awt.Dimension( 520, 320 ) );
		return frame;
	}

	/**
	 * Returns the unique mapped values in the given map, for the collection of
	 * keys given.
	 * 
	 * @return a new set.
	 */
	private final < K, V > Set< V > getUniqueValues( final Iterable< K > keys, final Map< K, V > map )
	{
		final HashSet< V > mapping = new HashSet<>();
		for ( final K key : keys )
			mapping.add( map.get( key ) );

		return mapping;
	}

	/**
	 * Returns the collection of keys amongst the given ones, that point to the
	 * target value in the given map.
	 * 
	 * @param targetValue
	 *            the common value to search
	 * @param keys
	 *            the keys to inspect
	 * @param map
	 *            the map to search in
	 * @return a new list.
	 */
	private final < K, V > List< K > getCommonKeys( final V targetValue, final Iterable< K > keys, final Map< K, V > map )
	{
		final ArrayList< K > foundKeys = new ArrayList<>();
		for ( final K key : keys )
		{
			if ( map.get( key ).equals( targetValue ) )
				foundKeys.add( key );

		}
		return foundKeys;
	}

	/**
	 * Returns a suitable plot title built from the given target features
	 * 
	 * @return the plot title.
	 */

	private final String buildPlotTitle( final Iterable< String > lYFeatures, final Map< String, String > featureNames )
	{
		final StringBuilder sb = new StringBuilder( "Plot of " );
		final Iterator< String > it = lYFeatures.iterator();
		sb.append( featureNames.get( it.next() ) );
		while ( it.hasNext() )
		{
			sb.append( ", " );
			sb.append( featureNames.get( it.next() ) );
		}
		sb.append( " vs " );
		sb.append( featureNames.get( xFeature ) );
		sb.append( "." );
		return sb.toString();
	}
}
