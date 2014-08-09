package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;

import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphOutline;
import com.mxgraph.swing.handler.mxRubberband;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.util.TrackNavigator;

public class TrackSchemeFrame extends JFrame
{

	/*
	 * CONSTANTS
	 */

	private static final long serialVersionUID = 1L;

	/*
	 * FIELDS
	 */

	/** The side pane in which spot selection info will be displayed. */
	private InfoPane infoPane;

	private JGraphXAdapter graph;

	private final TrackScheme trackScheme;

	/** The graph component in charge of painting the graph. */
	TrackSchemeGraphComponent graphComponent;

	/** The {@link Logger} that sends messages to the TrackScheme status bar. */
	final Logger logger;

	/*
	 * CONSTRUCTORS
	 */

	public TrackSchemeFrame( final TrackScheme trackScheme )
	{
		this.trackScheme = trackScheme;
		this.graph = trackScheme.getGraph();

		// Frame look
		setIconImage( TrackScheme.TRACK_SCHEME_ICON.getImage() );

		// Layout
		getContentPane().setLayout( new BorderLayout() );

		// Add a ToolBar
		getContentPane().add( createToolBar(), BorderLayout.NORTH );

		// Add the status bar
		final JPanel statusPanel = new JPanel();
		getContentPane().add( statusPanel, BorderLayout.SOUTH );

		statusPanel.setLayout( new FlowLayout( FlowLayout.RIGHT ) );

		final JLabel statusLabel = new JLabel( " " );
		statusLabel.setFont( SMALL_FONT );
		statusLabel.setHorizontalAlignment( JLabel.RIGHT );
		statusLabel.setPreferredSize( new Dimension( 200, 12 ) );
		statusPanel.add( statusLabel );

		final JProgressBar progressBar = new JProgressBar();
		progressBar.setPreferredSize( new Dimension( 146, 12 ) );
		statusPanel.add( progressBar );

		this.logger = new Logger()
		{
			@Override
			public void log( final String message, final Color color )
			{
				statusLabel.setText( message );
				statusLabel.setForeground( color );
			}

			@Override
			public void error( final String message )
			{
				log( message, Color.RED );
			}

			@Override
			public void setProgress( final double val )
			{
				progressBar.setValue( ( int ) ( val * 100 ) );
			}

			@Override
			public void setStatus( final String status )
			{
				log( status, Logger.BLUE_COLOR );
			}
		};
	}

	/*
	 * PUBLIC METHODS
	 */

	public void init( final JGraphXAdapter graph )
	{
		this.graph = graph;
		// GraphComponent
		graphComponent = createGraphComponent();

		// Add the info pane
		infoPane = new InfoPane( trackScheme.getModel(), trackScheme.getSelectionModel() );

		// Add the graph outline
		final mxGraphOutline graphOutline = new mxGraphOutline( graphComponent );

		final JSplitPane inner = new JSplitPane( JSplitPane.VERTICAL_SPLIT, infoPane, graphOutline );

		final JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, inner, graphComponent );
		splitPane.setDividerLocation( 170 );
		getContentPane().add( splitPane, BorderLayout.CENTER );

	}

	/*
	 * Selection management
	 */

	public void centerViewOn( final mxICell cell )
	{
		graphComponent.scrollCellToVisible( cell, true );
	}

	/**
	 * Instantiate the graph component in charge of painting the graph. Hook for
	 * sub-classers.
	 */
	private TrackSchemeGraphComponent createGraphComponent()
	{
		final TrackSchemeGraphComponent gc = new TrackSchemeGraphComponent( graph, trackScheme );
		gc.getVerticalScrollBar().setUnitIncrement( 16 );
		gc.getHorizontalScrollBar().setUnitIncrement( 16 );

		final JPanel rowheader = new JPanel()
		{
			@Override
			public void paintComponent( final Graphics g )
			{

				final Graphics2D g2d = ( Graphics2D ) g;
				final double scale = graph.getView().getScale();

				final float fontScale = ( float ) ( 12 * Math.min( 1d, scale ) );
				g.setFont( FONT.deriveFont( fontScale ).deriveFont( Font.BOLD ) );
				g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
				final Rectangle paintBounds = g.getClipBounds();

				final int height = gc.getViewport().getView().getSize().height;

				// Scaled sizes
				final double xcs = TrackScheme.X_COLUMN_SIZE * Math.min( 1d, scale );
				final double ycs = TrackScheme.Y_COLUMN_SIZE * scale;

				// Alternating row color
				g.setColor( TrackSchemeGraphComponent.BACKGROUND_COLOR_1 );
				g.fillRect( paintBounds.x, paintBounds.y, paintBounds.width, paintBounds.height );

				g.setColor( TrackSchemeGraphComponent.BACKGROUND_COLOR_2 );
				double y = 0;
				while ( y < height )
				{
					if ( y > paintBounds.y - ycs && y < paintBounds.y + paintBounds.height )
					{
						g.fillRect( 0, ( int ) y, ( int ) xcs, ( int ) ycs );
					}
					y += 2d * ycs;
				}

				// Header separator
				g.setColor( TrackSchemeGraphComponent.LINE_COLOR );
				if ( ycs > paintBounds.y && ycs < paintBounds.y + paintBounds.height )
				{
					g.drawLine( paintBounds.x, ( int ) ycs, paintBounds.x + paintBounds.width, ( int ) ycs );
				}
				if ( xcs > paintBounds.x && xcs < paintBounds.x + paintBounds.width )
				{
					g.drawLine( ( int ) xcs, paintBounds.y, ( int ) xcs, paintBounds.y + paintBounds.height );
				}

				// Row headers
				final double x = xcs / 4d;
				y = 3 * ycs / 2d;

				if ( xcs > paintBounds.x )
				{
					while ( y < height )
					{
						if ( y > paintBounds.y - ycs && y < paintBounds.y + paintBounds.height )
						{
							final int frame = ( int ) ( y / ycs - 1 );
							g.drawString( String.format( "frame %d", frame ), ( int ) x, ( int ) Math.round( y + 12 * scale ) );
						}
						y += ycs;
					}
				}
			}

			@Override
			public Dimension getPreferredSize()
			{

				final double scale = Math.min( 1, graph.getView().getScale() );
				final double xcs = TrackScheme.X_COLUMN_SIZE * scale + 1;
				return new Dimension( ( int ) xcs, ( int ) gc.getViewport().getPreferredSize().getHeight() );
			}
		};
		gc.setRowHeaderView( rowheader );

		/*
		 * gc.setExportEnabled(true); Seems to be required to have a preview
		 * when we move cells. Also give the ability to export a cell as an
		 * image clipping
		 */
		gc.getConnectionHandler().setEnabled( TrackScheme.DEFAULT_LINKING_ENABLED );
		/*
		 * By default, can be changed in the track scheme toolbar
		 */

		new mxRubberband( gc );
		// new mxKeyboardHandler(gc);
		new TrackSchemeKeyboardHandler( gc, new TrackNavigator( trackScheme.getModel(), trackScheme.getSelectionModel() ) );

		// Popup menu
		gc.getGraphControl().addMouseListener( new MouseAdapter()
		{
			@Override
			public void mousePressed( final MouseEvent e )
			{
				if ( e.isPopupTrigger() )
				{
					displayPopupMenu( gc.getCellAt( e.getX(), e.getY(), false ), e.getPoint() );
				}
			}

			@Override
			public void mouseReleased( final MouseEvent e )
			{
				if ( e.isPopupTrigger() )
				{
					displayPopupMenu( gc.getCellAt( e.getX(), e.getY(), false ), e.getPoint() );
				}
			}
		} );

		gc.setKeepSelectionVisibleOnZoom( true );

		return gc;
	}

	/**
	 * Instantiate the toolbar of the track scheme.
	 */
	private JToolBar createToolBar()
	{
		return new TrackSchemeToolbar( trackScheme );
	}

	/**
	 * PopupMenu
	 */
	private void displayPopupMenu( final Object cell, final Point point )
	{
		final TrackSchemePopupMenu menu = new TrackSchemePopupMenu( trackScheme, cell, point );
		menu.show( graphComponent.getViewport().getView(), ( int ) point.getX(), ( int ) point.getY() );
	}

}
