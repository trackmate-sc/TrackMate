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
package fiji.plugin.trackmate.util;

import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultXYDataset;

import com.itextpdf.text.DocumentException;

public class ChartEporterExample
{

	public static void main( final String[] args ) throws IOException, DocumentException
	{
		exportToPDF( args );
		exportToSVG( args );
	}

	private static Object[] createDummyChart()
	{
		// Collect data
		final int nPoints = 200;
		final double[][] data = new double[ 2 ][ nPoints ];

		int index = 0;
		for ( int i = 0; i < nPoints; i++ )
		{
			data[ 0 ][ index ] = Math.random() * 100;
			data[ 1 ][ index ] = Math.random() * 10;
			index++;
		}

		// Plot data
		final String xAxisLabel = "Time (s)";
		final String yAxisLabel = "N spots";
		final String title = "Nspots vs Time for something.";
		final DefaultXYDataset dataset = new DefaultXYDataset();
		dataset.addSeries( "Nspots", data );

		final JFreeChart chart = ChartFactory.createXYLineChart( title, xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL, true, true, false );
		chart.getTitle().setFont( FONT );
		chart.getLegend().setItemFont( SMALL_FONT );

		// The plot
		final XYPlot plot = chart.getXYPlot();
		// plot.setRenderer(0, pointRenderer);
		plot.getRangeAxis().setLabelFont( FONT );
		plot.getRangeAxis().setTickLabelFont( SMALL_FONT );
		plot.getDomainAxis().setLabelFont( FONT );
		plot.getDomainAxis().setTickLabelFont( SMALL_FONT );

		final ExportableChartPanel panel = new ExportableChartPanel( chart );
		panel.setPreferredSize( new Dimension( 300, 200 ) );

		final Object[] out = new Object[ 2 ];
		out[ 0 ] = chart;
		out[ 1 ] = panel;

		return out;
	}

	public static void exportToPDF( final String[] args ) throws IOException, DocumentException
	{
		final Object[] window1 = createDummyChart();
		final ChartPanel panel1 = ( ChartPanel ) window1[ 1 ];
		final JFreeChart chart1 = ( JFreeChart ) window1[ 0 ];
		chart1.setTitle( "1" );
		final Object[] window2 = createDummyChart();
		final ChartPanel panel2 = ( ChartPanel ) window2[ 1 ];
		final JFreeChart chart2 = ( JFreeChart ) window2[ 0 ];
		chart2.setTitle( "2" );

		// The Panel
		final JPanel panel = new JPanel();
		final BoxLayout panelLayout = new BoxLayout( panel, BoxLayout.Y_AXIS );
		panel.setLayout( panelLayout );
		panel.add( panel1 );
		panel.add( Box.createVerticalStrut( 5 ) );
		panel.add( panel2 );

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
		frame.setVisible( true );

		ChartExporter.exportChartAsPDF( new File( "/Users/tinevez/Desktop/ExportTest1.pdf" ), panel1.getChart(), panel1.getWidth(), panel1.getHeight() );
		ChartExporter.exportChartAsPDF( new File( "/Users/tinevez/Desktop/ExportTest2.pdf" ), panel2.getChart(), panel2.getWidth(), panel1.getHeight() );
	}

	public static void exportToSVG( final String[] args ) throws IOException, DocumentException
	{
		final Object[] window1 = createDummyChart();
		final ChartPanel panel1 = ( ChartPanel ) window1[ 1 ];
		final JFreeChart chart1 = ( JFreeChart ) window1[ 0 ];
		chart1.setTitle( "1" );
		final Object[] window2 = createDummyChart();
		final ChartPanel panel2 = ( ChartPanel ) window2[ 1 ];
		final JFreeChart chart2 = ( JFreeChart ) window2[ 0 ];
		chart2.setTitle( "2" );

		// The Panel
		final JPanel panel = new JPanel();
		final BoxLayout panelLayout = new BoxLayout( panel, BoxLayout.Y_AXIS );
		panel.setLayout( panelLayout );
		panel.add( panel1 );
		panel.add( Box.createVerticalStrut( 5 ) );
		panel.add( panel2 );

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
		frame.setVisible( true );

		ChartExporter.exportChartAsSVG( new File( "/Users/tinevez/Desktop/ExportTest1.svg" ), panel1.getChart(), panel1.getWidth(), panel1.getHeight() );
		ChartExporter.exportChartAsSVG( new File( "/Users/tinevez/Desktop/ExportTest2.svg" ), panel2.getChart(), panel2.getWidth(), panel1.getHeight() );
	}
}
