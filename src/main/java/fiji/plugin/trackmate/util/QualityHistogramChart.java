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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleConsumer;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.ui.RectangleInsets;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.gui.Fonts;
import fiji.plugin.trackmate.gui.components.LogHistogramDataset;
import fiji.plugin.trackmate.gui.components.XYTextSimpleAnnotation;
import fiji.util.NumberParser;
import net.imglib2.util.Util;

public class QualityHistogramChart extends JPanel
{

	private static final long serialVersionUID = 1L;

	private static final Color ANNOTATION_COLOR = new java.awt.Color( 252, 117, 0 );

	private static final String DATA_SERIES_NAME = "Data";

	private final JFreeChart chart;

	private final XYPlot plot;

	private final XYTextSimpleAnnotation annotation;

	private final IntervalMarker intervalMarker;

	private final ChartPanel chartPanel;

	private double threshold;

	private double autoThreshold = Double.NaN;

	private final DoubleConsumer thresholdSetter;


	public QualityHistogramChart( final DoubleConsumer thresholdSetter, final String axisLabel )
	{
		this.thresholdSetter = thresholdSetter;
		this.chart = ChartFactory.createHistogram( null, null, null, null, PlotOrientation.VERTICAL, false, false, false );
		this.plot = chart.getXYPlot();
		this.threshold = 0.;
		final XYBarRenderer renderer = ( XYBarRenderer ) plot.getRenderer();
		renderer.setShadowVisible( false );
		renderer.setMargin( 0 );
		renderer.setBarPainter( new StandardXYBarPainter() );
		renderer.setDrawBarOutline( true );
		renderer.setSeriesOutlinePaint( 0, Color.BLACK );
		renderer.setSeriesPaint( 0, new Color( 1, 1, 1, 0 ) );

		plot.setBackgroundPaint( null );
		plot.setOutlineVisible( false );
		plot.setDomainCrosshairVisible( false );
		plot.setDomainGridlinesVisible( false );
		plot.setRangeCrosshairVisible( false );
		plot.setRangeGridlinesVisible( false );
		plot.setRangeAxisLocation( AxisLocation.TOP_OR_RIGHT );

		plot.getRangeAxis().setVisible( true );
		plot.getRangeAxis().setTickMarksVisible( false );
		plot.getRangeAxis().setTickLabelsVisible( false );
		plot.getRangeAxis().setLabelPaint( Logger.NORMAL_COLOR );
		plot.getRangeAxis().setLabelFont( Fonts.SMALL_FONT );
		plot.getRangeAxis().setLabel( ( axisLabel == null ) ? "Quality histogram" : axisLabel );
		plot.getRangeAxis().setLabelInsets( new RectangleInsets( 0., 0., 0., 0. ) );
		plot.getRangeAxis().setTickLabelInsets( new RectangleInsets( 0., 0., 0., 0. ) );
		plot.getRangeAxis().setAxisLineVisible( false );
		plot.getDomainAxis().setVisible( true );
		plot.getDomainAxis().setTickLabelsVisible( true );
		plot.getDomainAxis().setTickLabelPaint( Logger.NORMAL_COLOR );
		plot.getDomainAxis().setTickLabelFont( Fonts.SMALL_FONT );
		( ( NumberAxis ) plot.getDomainAxis() ).setNumberFormatOverride( new DecimalFormat( "#.###" ) );

		chart.setBorderVisible( false );
		chart.setBackgroundPaint( null );
		this.chartPanel = new ChartPanel( chart );

		this.intervalMarker = new IntervalMarker(
				Double.NEGATIVE_INFINITY, 0.,
				chartPanel.getBackground(),
				new BasicStroke(),
				chartPanel.getForeground(),
				new BasicStroke(), 0.8f );

		this.annotation = new XYTextSimpleAnnotation( chartPanel, true );
		annotation.setFont( Fonts.SMALL_FONT );
		annotation.setColor( ANNOTATION_COLOR.darker() );

		plot.setDataset( null );
		chartPanel.setVisible( false );
		chartPanel.setMinimumDrawHeight( 80 );
		chartPanel.setMinimumDrawWidth( 80 );

		/*
		 * Listeners.
		 */

		final MouseListener[] mls = chartPanel.getMouseListeners();
		for ( final MouseListener ml : mls )
			chartPanel.removeMouseListener( ml );

		chartPanel.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseClicked( final MouseEvent e )
			{
				chartPanel.requestFocusInWindow();
				if ( e.getButton() == MouseEvent.BUTTON3 && !Double.isNaN( autoThreshold ) )
					threshold = autoThreshold;
				else
					threshold = getXFromChartEvent( e, chartPanel );
				redrawThresholdMarker();
			}
		} );
		chartPanel.addMouseMotionListener( new MouseAdapter()
		{
			@Override
			public void mouseDragged( final MouseEvent e )
			{
				threshold = getXFromChartEvent( e, chartPanel );
				redrawThresholdMarker();
			}
		} );
		chartPanel.addMouseWheelListener( new MouseWheelListener()
		{

			@Override
			public void mouseWheelMoved( final MouseWheelEvent e )
			{
				moveThreshold( e.getWheelRotation() );
			}
		} );
		chartPanel.setFocusable( true );
		chartPanel.addKeyListener( new MyKeyListener() );

		setLayout( new BorderLayout() );
		add( chartPanel, BorderLayout.CENTER );
	}

	public void displayHistogram( final double[] values )
	{
		displayHistogram( values, Double.NaN );
	}

	public void displayHistogram( final double[] values, final double threshold )
	{
		this.threshold = threshold;
		this.autoThreshold = TMUtils.otsuThreshold( values );
		if ( values.length > 0 )
		{
			final int nBins = getNBins( values, 8, 100 );
			if ( nBins > 1 )
			{
				final LogHistogramDataset dataset = new LogHistogramDataset();
				dataset.addSeries( DATA_SERIES_NAME, values, nBins );
				plot.setDataset( dataset );

				plot.removeDomainMarker( intervalMarker );
				plot.removeAnnotation( annotation );
				if ( !Double.isNaN( threshold ) )
				{
					redrawThresholdMarker();
					plot.addDomainMarker( intervalMarker );
					plot.addAnnotation( annotation );
				}

				chartPanel.setVisible( true );
				return;
			}
		}
		chartPanel.setVisible( false );
		plot.setDataset( null );
	}

	private double getXFromChartEvent( final MouseEvent mouseEvent, final ChartPanel chartPanel )
	{
		final Rectangle2D plotArea = chartPanel.getScreenDataArea();
		return plot.getDomainAxis().java2DToValue( mouseEvent.getX(), plotArea, plot.getDomainAxisEdge() );
	}

	private void moveThreshold( final int amount )
	{
		if ( Double.isNaN( threshold ) )
			return;

		threshold += ( double ) amount / 100 * plot.getDomainAxis().getRange().getLength();
		redrawThresholdMarker();

	}

	private void redrawThresholdMarker()
	{
		if ( Double.isNaN( threshold ) )
			return;

		intervalMarker.setEndValue( threshold );

		final float x;
		if ( threshold > 0.85 * plot.getDomainAxis().getUpperBound() )
			x = ( float ) ( threshold - 0.15 * plot.getDomainAxis().getRange().getLength() );
		else
			x = ( float ) ( threshold + 0.05 * plot.getDomainAxis().getRange().getLength() );

		final float y = ( float ) ( 0.85 * plot.getRangeAxis().getUpperBound() );
		annotation.setText( String.format( "%.1f", threshold ) );
		annotation.setLocation( x, y );
		thresholdChanged();
	}

	private void thresholdChanged()
	{
		if ( thresholdSetter != null )
			thresholdSetter.accept( threshold );
	}

	/**
	 * Return the optimal bin number for a histogram of the data given in array,
	 * using the Freedman and Diaconis rule (bin_space = 2*IQR/n^(1/3)). It is
	 * ensured that the bin number returned is not smaller and no bigger than
	 * the bounds given in argument.
	 *
	 * @param values
	 *            the values to bin.
	 * @param minBinNumber
	 *            the minimal desired number of bins.
	 * @param maxBinNumber
	 *            the maximal desired number of bins.
	 * @return the number of bins.
	 */
	private static final int getNBins( final double[] values, final int minBinNumber, final int maxBinNumber )
	{
		final int size = values.length;
		final double q1 = Util.percentile( values, 0.25 );
		final double q3 = Util.percentile( values, 0.75 );
		final double iqr = q3 - q1;
		final double binWidth = 2 * iqr * Math.pow( size, -0.33 );

		final double max = Util.max( values );
		final double min = Util.min( values );
		final double range = max - min;

		int nBin = ( int ) ( range / binWidth + 1 );
		if ( nBin > maxBinNumber )
			nBin = maxBinNumber;
		else if ( nBin < minBinNumber )
			nBin = minBinNumber;
		return nBin;
	}

	/**
	 * A class that listen to the user typing a number, building a string
	 * representation as he types, then converting the string to a double after
	 * a wait time. The number typed is used to set the threshold in the chart
	 * panel.
	 *
	 * @author Jean-Yves Tinevez
	 */
	private final class MyKeyListener implements KeyListener
	{

		private static final long WAIT_DELAY = 1; // s

		private static final double INCREASE_FACTOR = 0.1;

		private static final double SLOW_INCREASE_FACTOR = 0.005;

		private String strNumber = "";

		private ScheduledExecutorService ex;

		private ScheduledFuture< ? > future;

		private boolean dotAdded = false;

		private final Runnable command = new Runnable()
		{
			@Override
			public void run()
			{
				// Convert to double and pass it to threshold value
				try
				{
					final double typedThreshold = NumberParser.parseDouble( strNumber );
					threshold = typedThreshold;
					redrawThresholdMarker();
				}
				catch ( final NumberFormatException nfe )
				{}
				// Reset
				ex = null;
				strNumber = "";
				dotAdded = false;
			}
		};

		@Override
		public void keyPressed( final KeyEvent e )
		{
			// Is it arrow keys?
			if ( e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_KP_LEFT )
			{
				threshold -= ( e.isControlDown() ? SLOW_INCREASE_FACTOR : INCREASE_FACTOR ) * plot.getDomainAxis().getRange().getLength();
				redrawThresholdMarker();
				return;
			}
			else if ( e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_KP_RIGHT )
			{
				threshold += ( e.isControlDown() ? SLOW_INCREASE_FACTOR : INCREASE_FACTOR ) * plot.getDomainAxis().getRange().getLength();
				redrawThresholdMarker();
				return;
			}
			else if ( e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_KP_UP )
			{
				threshold = plot.getDomainAxis().getRange().getUpperBound();
				redrawThresholdMarker();
				return;
			}
			else if ( e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_KP_DOWN )
			{
				threshold = plot.getDomainAxis().getRange().getLowerBound();
				redrawThresholdMarker();
				return;
			}
		}

		@Override
		public void keyReleased( final KeyEvent e )
		{}

		@Override
		public void keyTyped( final KeyEvent e )
		{

			if ( e.getKeyChar() < '0' || e.getKeyChar() > '9' )
			{
				// Ok then it's number
				// User added a decimal dot for the first and only time
				if ( !dotAdded && e.getKeyChar() == '.' )
					dotAdded = true;
				else
					return;
			}

			if ( ex == null )
			{
				// Create new waiting line
				ex = Threads.newSingleThreadScheduledExecutor();
				future = ex.schedule( command, WAIT_DELAY, TimeUnit.SECONDS );
			}
			else
			{
				// Reset waiting line
				future.cancel( false );
				future = ex.schedule( command, WAIT_DELAY, TimeUnit.SECONDS );
			}
			strNumber += e.getKeyChar();
		}
	}
}
