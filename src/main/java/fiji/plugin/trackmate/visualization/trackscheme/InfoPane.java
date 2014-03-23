package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;
import ij.measure.ResultsTable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionChangeEvent;
import fiji.plugin.trackmate.SelectionChangeListener;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.SpotFeatureGrapher;
import fiji.plugin.trackmate.tracking.TrackableObjectUtils;
import fiji.plugin.trackmate.util.OnRequestUpdater;
import fiji.plugin.trackmate.util.OnRequestUpdater.Refreshable;
import fiji.plugin.trackmate.util.TMUtils;

public class InfoPane extends JPanel implements SelectionChangeListener
{

	private static final long serialVersionUID = -1L;

	private FeaturePlotSelectionPanel featureSelectionPanel;

	private JTable table;

	private JScrollPane scrollTable;

	private final boolean doHighlightSelection = true;

	private final Model model;

	private final SelectionModel selectionModel;

	/**
	 * A copy of the last spot collection highlighted in this infopane, sorted
	 * by frame order.
	 */
	private Collection< Spot > spotSelection;

	private final OnRequestUpdater updater;

	/** The table headers, taken from spot feature names. */
	private final String[] headers;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * Creates a new Info pane that displays information on the current spot
	 * selection in a table.
	 *
	 * @param model
	 *            the {@link Model} from which the spot collection is taken.
	 * @param settings
	 *            the {@link Settings} object we use to retrieve spot feature
	 *            names.
	 */
	public InfoPane( final Model model, final SelectionModel selectionModel )
	{
		this.model = model;
		this.selectionModel = selectionModel;
		final List< String > features = new ArrayList< String >( model.getFeatureModel().getSpotFeatures() );
		final Map< String, String > featureNames = model.getFeatureModel().getSpotFeatureShortNames();
		final List< String > headerList = TMUtils.getArrayFromMaping( features, featureNames );
		headerList.add( 0, "Track ID" );
		headers = headerList.toArray( new String[] {} );

		this.updater = new OnRequestUpdater( new Refreshable()
		{
			@Override
			public void refresh()
			{
				SwingUtilities.invokeLater( new Runnable()
				{
					@Override
					public void run()
					{
						update();
					}
				} );
			}
		} );
		// Add a listener to ensure we remove this panel from the listener list
		// of the model
		addAncestorListener( new AncestorListener()
		{
			@Override
			public void ancestorRemoved( final AncestorEvent event )
			{
				InfoPane.this.selectionModel.removeSelectionChangeListener( InfoPane.this );
			}

			@Override
			public void ancestorMoved( final AncestorEvent event )
			{}

			@Override
			public void ancestorAdded( final AncestorEvent event )
			{}
		} );
		selectionModel.addSelectionChangeListener( this );
		init();
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public void selectionChanged( final SelectionChangeEvent event )
	{
		// Echo changed in a different thread for performance
		new Thread( "TrackScheme info pane thread" )
		{
			@Override
			public void run()
			{
				highlightSpots( selectionModel.getSpotSelection() );
			}
		}.start();
	}

	/**
	 * Show the given spot selection as a table displaying their individual
	 * features.
	 */
	private void highlightSpots( final Collection< Spot > spots )
	{
		if ( !doHighlightSelection )
			return;
		if ( spots.size() == 0 )
		{
			scrollTable.setVisible( false );
			return;
		}

		// Copy and sort selection by frame
		spotSelection = spots;
		updater.doUpdate();
	}

	private void update()
	{
		/*
		 * Sort using a list; TreeSet does not allow several identical frames,
		 * which is likely to happen.
		 */
		final List< Spot > sortedSpots = new ArrayList< Spot >( spotSelection );
		Collections.sort( sortedSpots, TrackableObjectUtils.frameComparator() );

		@SuppressWarnings( "serial" )
		final DefaultTableModel dm = new DefaultTableModel()
		{ // Un-editable model
			@Override
			public boolean isCellEditable( final int row, final int column )
			{
				return false;
			}
		};

		final List< String > features = new ArrayList< String >( model.getFeatureModel().getSpotFeatures() );
		for ( final Spot spot : sortedSpots )
		{
			if ( null == spot )
			{
				continue;
			}
			final Object[] columnData = new Object[ features.size() + 1 ];
			columnData[ 0 ] = String.format( "%d", model.getTrackModel().trackIDOf( spot ) );
			for ( int i = 1; i < columnData.length; i++ )
			{
				final String feature = features.get( i - 1 );
				final Double feat = spot.getFeature( feature );
				if ( null == feat )
				{
					columnData[ i ] = "";
				}
				else if ( model.getFeatureModel().getSpotFeatureIsInt().get( feature ).booleanValue() )
				{
					columnData[ i ] = "" + feat.intValue();
				}
				else
				{
					columnData[ i ] = String.format( "%.4g", feat.doubleValue() );
				}
			}
			dm.addColumn( spot.toString(), columnData );
		}
		table.setModel( dm );

		// Tune look
		@SuppressWarnings( "serial" )
		final DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer()
		{
			@Override
			public boolean isOpaque()
			{
				return false;
			};

			@Override
			public Color getBackground()
			{
				return Color.BLUE;
			}
		};
		headerRenderer.setBackground( Color.RED );
		headerRenderer.setFont( FONT );

		final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
		renderer.setOpaque( false );
		renderer.setHorizontalAlignment( SwingConstants.RIGHT );
		renderer.setFont( SMALL_FONT );

		final FontMetrics fm = table.getGraphics().getFontMetrics( FONT );
		for ( int i = 0; i < table.getColumnCount(); i++ )
		{
			table.setDefaultRenderer( table.getColumnClass( i ), renderer );
			// Set width auto
			table.getColumnModel().getColumn( i ).setWidth( fm.stringWidth( dm.getColumnName( i ) ) );
		}
		for ( final Component c : scrollTable.getColumnHeader().getComponents() )
		{
			c.setBackground( getBackground() );
		}
		scrollTable.getColumnHeader().setOpaque( false );
		scrollTable.setVisible( true );
		validate();
	}

	/*
	 * PRIVATE METHODS
	 */

	private void displayPopupMenu( final Point point )
	{
		// Prepare menu
		final JPopupMenu menu = new JPopupMenu( "Selection table" );
		final JMenuItem exportItem = menu.add( "Export to ImageJ table" );
		exportItem.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				exportTableToImageJ();
			}
		} );
		// Display it
		menu.show( table, ( int ) point.getX(), ( int ) point.getY() );
	}

	private void exportTableToImageJ()
	{
		final ResultsTable table = new ResultsTable();
		final FeatureModel fm = model.getFeatureModel();
		final List< String > features = new ArrayList< String >( fm.getSpotFeatures() );

		final int ncols = spotSelection.size();
		final int nrows = headers.length;
		final Spot[] spotArray = spotSelection.toArray( new Spot[] {} );

		/*
		 * Track ID
		 */

		table.incrementCounter();
		table.setLabel( "TRACK_ID", 0 );
		for ( int i = 0; i < ncols; i++ )
		{
			final Spot spot = spotArray[ i ];
			final Integer trackID = model.getTrackModel().trackIDOf( spot );
			if ( null == trackID )
			{
				table.addValue( spot.getName(), "None" );
			}
			else
			{
				table.addValue( spot.getName(), "" + trackID.intValue() );
			}
		}

		/*
		 * Other features
		 */

		for ( int j = 0; j < nrows - 1; j++ )
		{
			table.incrementCounter();
			final String feature = features.get( j );
			table.setLabel( feature, j + 1 );
			for ( int i = 0; i < ncols; i++ )
			{
				final Spot spot = spotArray[ i ];
				final Double val = spot.getFeature( feature );
				if ( val == null )
				{
					table.addValue( spot.getName(), "None" );
				}
				else
				{
					if ( fm.getSpotFeatureIsInt().get( feature ) )
					{
						table.addValue( spot.getName(), "" + val.intValue() );
					}
					else
					{
						table.addValue( spot.getName(), val.doubleValue() );
					}
				}
			}
		}

		table.show( "TrackMate Selection" );
	}

	private void init()
	{

		@SuppressWarnings( "serial" )
		final AbstractListModel lm = new AbstractListModel()
		{
			@Override
			public int getSize()
			{
				return headers.length;
			}

			@Override
			public Object getElementAt( final int index )
			{
				return headers[ index ];
			}
		};

		table = new JTable();
		table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		table.setOpaque( false );
		table.setFont( SMALL_FONT );
		table.setPreferredScrollableViewportSize( new Dimension( 120, 400 ) );
		table.getTableHeader().setOpaque( false );
		table.setSelectionForeground( Color.YELLOW.darker().darker() );
		table.setGridColor( TrackScheme.GRID_COLOR );
		table.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mousePressed( final MouseEvent e )
			{
				if ( e.isPopupTrigger() )
					displayPopupMenu( e.getPoint() );
			}

			@Override
			public void mouseReleased( final MouseEvent e )
			{
				if ( e.isPopupTrigger() )
					displayPopupMenu( e.getPoint() );
			}
		} );

		final JList rowHeader = new JList( lm );
		rowHeader.setFixedCellHeight( table.getRowHeight() );
		rowHeader.setCellRenderer( new RowHeaderRenderer( table ) );
		rowHeader.setBackground( getBackground() );

		scrollTable = new JScrollPane( table );
		scrollTable.setRowHeaderView( rowHeader );
		scrollTable.getRowHeader().setOpaque( false );
		scrollTable.setOpaque( false );
		scrollTable.getViewport().setOpaque( false );
		scrollTable.setVisible( false ); // for now

		final List< String > features = new ArrayList< String >( model.getFeatureModel().getSpotFeatures() );
		final Map< String, String > featureNames = model.getFeatureModel().getSpotFeatureShortNames();
		featureSelectionPanel = new FeaturePlotSelectionPanel( Spot.POSITION_T, features, featureNames );

		setLayout( new BorderLayout() );
		add( scrollTable, BorderLayout.CENTER );
		add( featureSelectionPanel, BorderLayout.SOUTH );

		// Add listener for plot events
		featureSelectionPanel.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				final String xFeature = featureSelectionPanel.getXKey();
				final Set< String > yFeatures = featureSelectionPanel.getYKeys();
				plotSelectionData( xFeature, yFeatures );
			}
		} );

	}

	/**
	 * Reads the content of the current spot selection and plot the selected
	 * features in this {@link InfoPane} for the target spots.
	 *
	 * @param xFeature
	 *            the feature to use as X axis.
	 * @param yFeatures
	 *            the features to plot as Y axis.
	 */
	private void plotSelectionData( final String xFeature, final Set< String > yFeatures )
	{
		final Set< Spot > spots = selectionModel.getSpotSelection();
		if ( yFeatures.isEmpty() || spots.isEmpty() ) { return; }

		final SpotFeatureGrapher grapher = new SpotFeatureGrapher( xFeature, yFeatures, spots, model );
		grapher.render();
	}

	/*
	 * INNER CLASS
	 */

	private class RowHeaderRenderer extends JLabel implements ListCellRenderer, Serializable
	{

		private static final long serialVersionUID = -1L;

		RowHeaderRenderer( final JTable table )
		{
			final JTableHeader header = table.getTableHeader();
			setOpaque( false );
			setBorder( UIManager.getBorder( "TableHeader.cellBorder" ) );
			setForeground( header.getForeground() );
			setBackground( header.getBackground() );
			setFont( SMALL_FONT.deriveFont( 9.0f ) );
			setHorizontalAlignment( SwingConstants.LEFT );
		}

		@Override
		public Component getListCellRendererComponent( final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus )
		{
			setText( ( value == null ) ? "" : value.toString() );
			return this;
		}
	}
}
