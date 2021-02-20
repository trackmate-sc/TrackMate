package fiji.plugin.trackmate.gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;

import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.util.NumberParser;

/**
 *
 * Revised December 2020.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class FilterPanel extends javax.swing.JPanel
{

	static final Font FONT = new Font( "Arial", Font.PLAIN, 11 );

	static final Font SMALL_FONT = FONT.deriveFont( 10f );

	private static final Color annotationColor = new java.awt.Color( 252, 117, 0 );

	private static final long serialVersionUID = 1L;

	private static final String DATA_SERIES_NAME = "Data";

	private final ChangeEvent CHANGE_EVENT = new ChangeEvent( this );

	private final XYPlot plot;

	private final IntervalMarker intervalMarker;

	private double threshold;

	private final Function< String, double[] > valueCollector;

	private final XYTextSimpleAnnotation annotation;

	private final ArrayList< ChangeListener > listeners = new ArrayList<>();

	final JRadioButton rdbtnAbove;

	final JRadioButton rdbtnBelow;

	final JComboBox< String > cmbboxFeatureKeys;


	/*
	 * CONSTRUCTOR
	 */

	public FilterPanel(
			final Map< String, String > keyNames,
			final Function< String, double[] > valueCollector,
			final FeatureFilter filter )
	{
		this.valueCollector = valueCollector;

		final Dimension panelSize = new java.awt.Dimension( 250, 140 );
		final Dimension panelMaxSize = new java.awt.Dimension( 1000, 140 );
		final GridBagLayout thisLayout = new GridBagLayout();
		thisLayout.rowWeights = new double[] { 0.0, 1.0, 0.0 };
		thisLayout.rowHeights = new int[] { 10, 7, 15 };
		thisLayout.columnWeights = new double[] { 0.0, 0.0, 1.0 };
		thisLayout.columnWidths = new int[] { 7, 20, 7 };
		this.setLayout( thisLayout );
		this.setPreferredSize( panelSize );
		this.setMaximumSize( panelMaxSize );
		this.setBorder( new LineBorder( annotationColor, 1, true ) );

		/*
		 * Feature selection box.
		 */

		final ComboBoxModel< String > cmbboxFeatureNameModel = new DefaultComboBoxModel<>( keyNames.keySet().toArray( new String[] {} ) );
		cmbboxFeatureKeys = new JComboBox<>( cmbboxFeatureNameModel );
		cmbboxFeatureKeys.setRenderer( new DefaultListCellRenderer()
		{

			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent( final JList< ? > list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus )
			{
				final JLabel lbl = ( JLabel ) super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
				lbl.setText( keyNames.get( value ) );
				return lbl;
			}
		} );
		cmbboxFeatureKeys.setFont( FONT );
		this.add( cmbboxFeatureKeys, new GridBagConstraints( 0, 0, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 2, 5, 2, 5 ), 0, 0 ) );

		/*
		 * Create histogram plot.
		 */

		final LogHistogramDataset dataset = new LogHistogramDataset();
		final JFreeChart chart = ChartFactory.createHistogram( null, null, null, dataset, PlotOrientation.VERTICAL, false, false, false );

		plot = chart.getXYPlot();
		final XYBarRenderer renderer = ( XYBarRenderer ) plot.getRenderer();
		renderer.setShadowVisible( false );
		renderer.setMargin( 0 );
		renderer.setBarPainter( new StandardXYBarPainter() );
		renderer.setDrawBarOutline( true );
		renderer.setSeriesOutlinePaint( 0, new Color( 0.2f, 0.2f, 0.2f ) );
		renderer.setSeriesPaint( 0, new Color( 0.3f, 0.3f, 0.3f, 0.5f ) );

		plot.setBackgroundPaint( new Color( 1, 1, 1, 0 ) );
		plot.setOutlineVisible( false );
		plot.setDomainCrosshairVisible( false );
		plot.setDomainGridlinesVisible( false );
		plot.setRangeCrosshairVisible( false );
		plot.setRangeGridlinesVisible( false );

		plot.getRangeAxis().setVisible( false );
		plot.getDomainAxis().setVisible( false );

		chart.setBorderVisible( false );
		chart.setBackgroundPaint( new Color( 0.6f, 0.6f, 0.7f ) );

		intervalMarker = new IntervalMarker( 0, 0, new Color( 0.3f, 0.5f, 0.8f ), new BasicStroke(), new Color( 0, 0, 0.5f ), new BasicStroke( 1.5f ), 0.5f );
		plot.addDomainMarker( intervalMarker );

		final ChartPanel chartPanel = new ChartPanel( chart );
		final MouseListener[] mls = chartPanel.getMouseListeners();
		for ( final MouseListener ml : mls )
			chartPanel.removeMouseListener( ml );

		chartPanel.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseClicked( final MouseEvent e )
			{
				chartPanel.requestFocusInWindow();
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
		chartPanel.setFocusable( true );
		chartPanel.addFocusListener( new FocusListener()
		{

			@Override
			public void focusLost( final FocusEvent e )
			{
				annotation.setColor( annotationColor.darker() );
			}

			@Override
			public void focusGained( final FocusEvent e )
			{
				annotation.setColor( Color.RED.darker() );
			}
		} );
		chartPanel.addKeyListener( new MyKeyListener() );

		annotation = new XYTextSimpleAnnotation( chartPanel );
		annotation.setFont( SMALL_FONT.deriveFont( Font.BOLD ) );
		annotation.setColor( annotationColor.darker() );
		plot.addAnnotation( annotation );

		chartPanel.setPreferredSize( new Dimension( 0, 0 ) );
		this.add( chartPanel, new GridBagConstraints( 0, 1, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		chartPanel.setOpaque( false );

		/*
		 * Threshold.
		 */

		final JButton btnAutoThreshold = new JButton();
		this.add( btnAutoThreshold, new GridBagConstraints( 2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets( 0, 0, 0, 10 ), 0, 0 ) );
		btnAutoThreshold.setText( "Auto" );
		btnAutoThreshold.setFont( SMALL_FONT );
		btnAutoThreshold.addActionListener( e -> autoThreshold() );

		rdbtnAbove = new JRadioButton();
		this.add( rdbtnAbove, new GridBagConstraints( 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets( 0, 10, 0, 0 ), 0, 0 ) );
		rdbtnAbove.setText( "Above" );
		rdbtnAbove.setFont( SMALL_FONT );
		rdbtnAbove.addActionListener( e -> redrawThresholdMarker() );

		rdbtnBelow = new JRadioButton();
		this.add( rdbtnBelow, new GridBagConstraints( 1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets( 0, 5, 0, 0 ), 0, 0 ) );
		rdbtnBelow.setText( "Below" );
		rdbtnBelow.addActionListener( e -> redrawThresholdMarker() );
		rdbtnBelow.setFont( SMALL_FONT );

		final ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add( rdbtnAbove );
		buttonGroup.add( rdbtnBelow );

		/*
		 * Listeners & co.
		 */

		cmbboxFeatureKeys.addActionListener( e -> comboBoxSelectionChanged() );

		/*
		 * Current values.
		 */

		cmbboxFeatureKeys.setSelectedItem( filter.feature );
		rdbtnAbove.setSelected( filter.isAbove );
		rdbtnBelow.setSelected( !filter.isAbove );
		if ( Double.isNaN( filter.value ) )
			autoThreshold();
		else
			this.threshold = filter.value;
		redrawThresholdMarker();
	}

	/*
	 * PUBLIC METHODS
	 */

	public FeatureFilter getFilter()
	{
		return new FeatureFilter( ( String ) cmbboxFeatureKeys.getSelectedItem(), threshold, rdbtnAbove.isSelected() );
	}

	/**
	 * Add an {@link ChangeListener} to this panel. The {@link ChangeListener}
	 * will be notified when a change happens to the threshold displayed by this
	 * panel, whether due to the slider being move, the auto-threshold button
	 * being pressed, or the combo-box selection being changed.
	 */
	public void addChangeListener( final ChangeListener listener )
	{
		listeners.add( listener );
	}

	/**
	 * Remove an ChangeListener.
	 *
	 * @return true if the listener was in listener collection of this instance.
	 */
	public boolean removeChangeListener( final ChangeListener listener )
	{
		return listeners.remove( listener );
	}

	public Collection< ChangeListener > getChangeListeners()
	{
		return listeners;
	}

	/**
	 * Refreshes the histogram content. Call this method when the values in the
	 * values map changed to update histogram display.
	 */
	public void refresh()
	{
		final double old = threshold;
		final String key = ( String ) cmbboxFeatureKeys.getSelectedItem();
		final double[] values = valueCollector.apply( key );

		final LogHistogramDataset dataset;
		if ( null == values || 0 == values.length )
		{
			dataset = new LogHistogramDataset();
			annotation.setLocation( 0.5f, 0.5f );
			annotation.setText( "No data" );
		}
		else
		{
			final int nBins = TMUtils.getNBins( values, 8, 100 );
			dataset = new LogHistogramDataset();
			if ( nBins > 1 )
				dataset.addSeries( DATA_SERIES_NAME, values, nBins );
		}
		plot.setDataset( dataset );
		threshold = old;
		repaint();
		redrawThresholdMarker();
	}

	/*
	 * PRIVATE METHODS
	 */

	private void fireThresholdChanged()
	{
		for ( final ChangeListener al : listeners )
			al.stateChanged( CHANGE_EVENT );
	}

	private void comboBoxSelectionChanged()
	{
		final String key = ( String ) cmbboxFeatureKeys.getSelectedItem();
		final double[] values = valueCollector.apply( key );

		final LogHistogramDataset dataset;
		if ( null == values || 0 == values.length )
		{
			dataset = new LogHistogramDataset();
			threshold = Double.NaN;
			annotation.setLocation( 0.5f, 0.5f );
			annotation.setText( "No data" );
			fireThresholdChanged();
		}
		else
		{
			final int nBins = TMUtils.getNBins( values, 8, 100 );
			dataset = new LogHistogramDataset();

			if ( nBins > 1 )
				dataset.addSeries( DATA_SERIES_NAME, values, nBins );
		}
		plot.setDataset( dataset );
		resetAxes();
		autoThreshold(); // Will fire the fireThresholdChanged();
	}

	private void autoThreshold()
	{
		final String key = ( String ) cmbboxFeatureKeys.getSelectedItem();
		final double[] values = valueCollector.apply( key );
		if ( null != values && values.length > 0 )
		{
			threshold = TMUtils.otsuThreshold( values );
			redrawThresholdMarker();
		}
	}

	private double getXFromChartEvent( final MouseEvent mouseEvent, final ChartPanel chartPanel )
	{
		final Rectangle2D plotArea = chartPanel.getScreenDataArea();
		return plot.getDomainAxis().java2DToValue( mouseEvent.getX(), plotArea, plot.getDomainAxisEdge() );
	}

	private void redrawThresholdMarker()
	{
		final String key = ( String ) cmbboxFeatureKeys.getSelectedItem();
		final double[] values = valueCollector.apply( key );
		if ( null == values )
			return;

		if ( rdbtnAbove.isSelected() )
		{
			intervalMarker.setStartValue( threshold );
			intervalMarker.setEndValue( plot.getDomainAxis().getUpperBound() );
		}
		else
		{
			intervalMarker.setStartValue( plot.getDomainAxis().getLowerBound() );
			intervalMarker.setEndValue( threshold );
		}

		final float x;
		if ( threshold > 0.85 * plot.getDomainAxis().getUpperBound() )
			x = ( float ) ( threshold - 0.15 * plot.getDomainAxis().getRange().getLength() );
		else
			x = ( float ) ( threshold + 0.05 * plot.getDomainAxis().getRange().getLength() );

		final float y = ( float ) ( 0.85 * plot.getRangeAxis().getUpperBound() );
		annotation.setText( String.format( "%.2f", threshold ) );
		annotation.setLocation( x, y );
		fireThresholdChanged();
	}

	private void resetAxes()
	{
		plot.getRangeAxis().setLowerMargin( 0 );
		plot.getRangeAxis().setUpperMargin( 0 );
		plot.getDomainAxis().setLowerMargin( 0 );
		plot.getDomainAxis().setUpperMargin( 0 );
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

				if ( !dotAdded && e.getKeyChar() == '.' )
				{
					// User added a decimal dot for the first and only time
					dotAdded = true;
				}
				else
				{
					return;
				}
			}

			if ( ex == null )
			{
				// Create new waiting line
				ex = Executors.newSingleThreadScheduledExecutor();
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
