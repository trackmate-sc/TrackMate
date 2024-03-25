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

import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.RectangleInsets;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.util.ExportableChartPanel;
import fiji.plugin.trackmate.util.TMUtils;

public abstract class AbstractFeatureGrapher
{

	protected final String xFeature;

	protected final List< String > yFeatures;

	private final Color bgColor = new Color( 220, 220, 220 );

	private final Dimension xDimension;

	private final Map< String, Dimension > yDimensions;

	private final Map< String, String > featureNames;

	private final String spaceUnits;

	private final String timeUnits;

	public AbstractFeatureGrapher(
			final String xFeature,
			final List< String > yFeatures,
			final Dimension xDimension,
			final Map< String, Dimension > yDimensions,
			final Map< String, String > featureNames,
			final String spaceUnits,
			final String timeUnits )
	{
		this.xFeature = xFeature;
		this.yFeatures = yFeatures;
		this.xDimension = xDimension;
		this.yDimensions = yDimensions;
		this.featureNames = featureNames;
		this.spaceUnits = spaceUnits;
		this.timeUnits = timeUnits;
	}

	/**
	 * Draws and renders the graph in a new JFrame.
	 * 
	 * @return a new JFrame, not shown yet.
	 */
	public JFrame render()
	{
		// X label
		final String xAxisLabel = featureNames.get( xFeature ) + " (" + TMUtils.getUnitsFor( xDimension, spaceUnits, timeUnits ) + ")";

		// Find how many different dimensions
		final Set< Dimension > dimensions = getUniqueValues( yFeatures, yDimensions );

		// Generate one panel per different dimension
		final ArrayList< ExportableChartPanel > chartPanels = new ArrayList<>( dimensions.size() );
		for ( final Dimension dimension : dimensions )
		{

			// Y label
			final String yAxisLabel = TMUtils.getUnitsFor( dimension, spaceUnits, timeUnits );

			// Collect suitable feature for this dimension
			final List< String > featuresThisDimension = getCommonKeys( dimension, yFeatures, yDimensions );

			// Title
			final String title = buildPlotTitle( featuresThisDimension, featureNames );

			// Dataset.
			final ModelDataset dataset = buildMainDataSet( featuresThisDimension );
			final XYItemRenderer renderer = dataset.getRenderer();

			// The chart
			final JFreeChart chart = ChartFactory.createXYLineChart( title, xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL, true, true, false );
			chart.getTitle().setFont( FONT );
			chart.getLegend().setItemFont( SMALL_FONT );
			chart.setBackgroundPaint( bgColor );
			chart.setBorderVisible( false );
			chart.getLegend().setBackgroundPaint( bgColor );

			// The plot
			final XYPlot plot = chart.getXYPlot();
			plot.setRenderer( renderer );
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

	protected abstract ModelDataset buildMainDataSet( final List< String > targetYFeatures );

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
		final Set< V > mapping = new LinkedHashSet<>();
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
	private static final < K, V > List< K > getCommonKeys( final V targetValue, final Iterable< K > keys, final Map< K, V > map )
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
