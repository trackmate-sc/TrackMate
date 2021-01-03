package fiji.plugin.trackmate.visualization.table;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.opencsv.CSVWriter;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.displaysettings.ColorIcon;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import gnu.trove.map.hash.TObjectIntHashMap;

public class TablePanel< O >
{

	private static final int ROW_HEIGHT = 26;

	private final JTable table;

	private final List< Class< ? > > columnClasses;

	private final List< String > mapToTooltip;

	private final Function< O, String > labelGenerator;

	private final BiConsumer< O, String > labelSetter;

	private final List< O > objects;

	private final List< String > features;

	private final BiFunction< O, String, Double > featureFun;

	private final Map< String, String > featureNames;

	private final Map< String, String > featureShortNames;

	private final Map< String, String > featureUnits;

	private final TObjectIntHashMap< O > map;

	private final Supplier< FeatureColorGenerator< O > > colorSupplier;

	private final String manualColorFeature;

	private boolean useColoring;

	private final JPanel panel;

	public TablePanel(
			final Iterable< O > objects,
			final List< String > features,
			final BiFunction< O, String, Double > featureFun,
			final Map< String, String > featureNames,
			final Map< String, String > featureShortNames,
			final Map< String, String > featureUnits,
			final Map< String, Boolean > isInts,
			final Map< String, String > infoTexts,
			final Supplier< FeatureColorGenerator< O > > colorSupplier,
			final Function< O, String > labelGenerator,
			final BiConsumer< O, String > labelSetter )
	{
		this( objects,
				features,
				featureFun,
				featureNames,
				featureShortNames,
				featureUnits,
				isInts,
				infoTexts,
				colorSupplier,
				labelGenerator,
				labelSetter,
				null,
				null );
	}

	public TablePanel(
			final Iterable< O > objects,
			final List< String > features,
			final BiFunction< O, String, Double > featureFun,
			final Map< String, String > featureNames,
			final Map< String, String > featureShortNames,
			final Map< String, String > featureUnits,
			final Map< String, Boolean > isInts,
			final Map< String, String > infoTexts,
			final Supplier< FeatureColorGenerator< O > > colorSupplier,
			final Function< O, String > labelGenerator,
			final BiConsumer< O, String > labelSetter,
			final String manualColorFeature,
			final BiConsumer< O, Color > colorSetter )
	{
		this.panel = new JPanel();
		this.featureFun = featureFun;
		this.featureNames = featureNames;
		this.featureShortNames = featureShortNames;
		this.featureUnits = featureUnits;
		this.colorSupplier = colorSupplier;
		this.manualColorFeature = manualColorFeature;
		this.objects = new ArrayList<>();
		this.map = new TObjectIntHashMap<>( 10, 0.5f, -1 );
		this.features = features;
		this.labelGenerator = labelGenerator;
		this.labelSetter = labelSetter;
		this.columnClasses = new ArrayList<>();
		this.mapToTooltip = new ArrayList<>();

		// Table column model.
		final MyTableModel tableModel = new MyTableModel();

		final DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
		this.table = new JTable( tableModel, tableColumnModel )
		{
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable( final int row, final int viewcolumn )
			{
				final int column = convertColumnIndexToModel( viewcolumn );
				// Only label and colors are editable.
				return ( labelSetter != null && column == 0 )
						|| ( colorSetter != null && column > 0 && features.get( column - 1 ).equals( manualColorFeature ) );
			}
		};
		table.setColumnModel( tableColumnModel );
		setObjects( objects );

		table.putClientProperty( "JTable.autoStartsEdit", Boolean.FALSE );
		table.getInputMap().put( KeyStroke.getKeyStroke( KeyEvent.VK_ENTER, 0 ), "startEditing" );

		table.setRowHeight( ROW_HEIGHT );
		table.getSelectionModel().setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		table.setGridColor( table.getTableHeader().getBackground() );

		// Class of columns.
		columnClasses.clear();
		// Last line of header is for units.
		final List< String > headerLine = new ArrayList<>();
		// Map from column index to tooltip strings.
		mapToTooltip.clear();

		// Provide tooltips on the fly.
		table.getTableHeader().addMouseMotionListener( new MyTableToolTipProvider() );

		int colIndex = 0;
		// First column is label.
		headerLine.add( "<html><b>Label<br> <br></html>" );
		mapToTooltip.add( "Object name" );

		columnClasses.add( String.class );
		tableColumnModel.addColumn( new TableColumn( colIndex++ ) );
		// Units for feature columns.
		for ( final String feature : features )
		{
			final Class< ? > pclass;
			if ( feature.equals( manualColorFeature ) )
				pclass = Color.class;
			else if ( isInts.get( feature ) )
				pclass = Integer.class;
			else
				pclass = Double.class;
			columnClasses.add( pclass );

			String tooltipStr = "<html>" + featureNames.get( feature );
			final String infoText = infoTexts.get( feature );
			if ( infoText != null )
				tooltipStr += "<p>" + infoText + "</p>";
			tooltipStr += "</html>";
			mapToTooltip.add( tooltipStr );
			final String units = featureUnits.get( feature );

			String headerStr = "<html><center><b>"
					+ featureShortNames.get( feature )
					+ "</b><br>";
			headerStr += ( units == null || units.isEmpty() ) ? "<br> </html>" : "(" + units + ")</html>";
			headerLine.add( headerStr );
			tableColumnModel.addColumn( new TableColumn( colIndex++ ) );
		}


		// Sorting.
		final TableRowSorter< MyTableModel > sorter = new TableRowSorter<>( tableModel );
		table.setRowSorter( sorter );
		for ( int c = 0; c < columnClasses.size(); c++ )
		{
			if ( columnClasses.get( c ).equals( Integer.class ) )
				sorter.setComparator( c, ( i1, i2 ) -> Integer.compare( ( int ) i1, ( int ) i2 ) );
			else if ( columnClasses.get( c ).equals( Double.class ) )
				sorter.setComparator( c, ( d1, d2 ) -> Double.compare( ( double ) d1, ( double ) d2 ) );
			else if ( columnClasses.get( c ).equals( Color.class ) )
				sorter.setComparator( c, ( c1, c2 ) -> c1.toString().compareTo( c2.toString() ) );
			else
				sorter.setComparator( c, Comparator.naturalOrder() );
		}

		// Pass last line to column headers and set cell renderer.
		final MyTableCellRenderer cellRenderer = new MyTableCellRenderer();
		final int colorcolumn = features.indexOf( manualColorFeature ) + 1;
		for ( int c = 0; c < tableColumnModel.getColumnCount(); c++ )
		{
			final TableColumn column = tableColumnModel.getColumn( c );
			column.setHeaderValue( headerLine.get( c ) );
			column.setCellRenderer( cellRenderer );
			if ( c == colorcolumn && null != colorSetter )
				column.setCellEditor( new MyColorEditor( colorSetter ) );
			else if ( c== 0 && null != labelSetter)
				column.setCellEditor( new MyLabelEditor() );
		}

		final JScrollPane scrollPane = new JScrollPane( table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

		panel.setLayout( new BorderLayout() );
		panel.add( scrollPane, BorderLayout.CENTER );
	}

	public void setUseColoring( final boolean useColoring )
	{
		this.useColoring = useColoring;
	}

	@SuppressWarnings( "unchecked" )
	public void setObjects( final Iterable< O > objects )
	{
		this.objects.clear();
		this.map.clear();
		int index = 0;
		for ( final O o : objects )
		{
			this.objects.add( o );
			map.put( o, index++ );
		}
		( ( MyTableModel ) table.getModel() ).fireTableDataChanged();
	}

	/**
	 * The panel in which the table is displayed. This is the component to add
	 * to client UI.
	 * 
	 * @return the main panel.
	 */
	public JPanel getPanel()
	{
		return panel;
	}

	/**
	 * Exposes the {@link JTable} in which the data is displayed.
	 *
	 * @return the table.
	 */
	public JTable getTable()
	{
		return table;
	}

	/**
	 * Returns the object listed at the specified <b>view</b> row index.
	 *
	 * @param viewRowIndex
	 *            the row to query.
	 * @return the object or <code>null</code> if the view row does not
	 *         correspond to an object currently displayed.
	 */
	public O getObjectForViewRow( final int viewRowIndex )
	{
		if ( viewRowIndex < 0 )
			return null;
		final int modelRow = table.convertRowIndexToModel( viewRowIndex );
		return objects.get( modelRow );
	}

	public int getViewRowForObject( final O o )
	{
		final int modelRow = map.get( o );
		if ( modelRow < 0 ) // Object not in table.
			return -1;
		try
		{
			return table.convertRowIndexToView( modelRow );
		}
		catch ( final IndexOutOfBoundsException e )
		{
			// Table has been cleared.
			return -1;
		}
	}

	public void scrollToObject( final O o )
	{
		final Rectangle rect = table.getVisibleRect();
		final int row = getViewRowForObject( o );
		final Rectangle cellRect = table.getCellRect( row, 0, true );
		cellRect.setLocation( rect.x, cellRect.y );
		table.scrollRectToVisible( cellRect );
	}

	public void exportToCsv( final File file ) throws IOException
	{
		try (CSVWriter writer = new CSVWriter( new FileWriter( file ), CSVWriter.DEFAULT_SEPARATOR ))
		{
			final int nCols = table.getColumnCount();

			/*
			 * Header.
			 */

			final String[] content = new String[ nCols ];

			// Header 1st line.
			content[ 0 ] = "NAME";
			for ( int i = 1; i < content.length; i++ )
				content[ i ] = features.get( i - 1 );
			writer.writeNext( content );

			// Header 2nd line.
			content[ 0 ] = "";
			for ( int i = 1; i < content.length; i++ )
				content[ i ] = featureNames.get( features.get( i - 1 ) );
			writer.writeNext( content );

			// Header 3rd line.
			content[ 0 ] = "";
			for ( int i = 1; i < content.length; i++ )
				content[ i ] = featureShortNames.get( features.get( i - 1 ) );
			writer.writeNext( content );

			// Header 4th line.
			content[ 0 ] = "";
			for ( int i = 1; i < content.length; i++ )
			{
				final String feature = features.get( i - 1 );
				final String units = featureUnits.get( feature );
				final String unitsStr = ( units == null || units.isEmpty() ) ? "" : "(" + units + ")";
				content[ i ] = unitsStr;
			}
			writer.writeNext( content );

			/*
			 * Content.
			 */

			final int nRows = table.getRowCount();
			final TableModel model = table.getModel();
			for ( int r = 0; r < nRows; r++ )
			{
				final int row = table.convertRowIndexToModel( r );
				for ( int col = 0; col < nCols; col++ )
				{
					final Object obj = model.getValueAt( row, col );
					if ( null == obj )
						content[ col ] = "";
					else if ( obj instanceof Integer )
						content[ col ] = Integer.toString( ( Integer ) obj );
					else if ( obj instanceof Double )
						content[ col ] = Double.toString( ( Double ) obj );
					else if ( obj instanceof Boolean )
						content[ col ] = ( ( Boolean ) obj ) ? "1" : "0";
					else if ( obj instanceof Color )
					{
						final Color color = ( Color ) obj;
						content[ col ] = String.format( "r=%d;g=%d;b=%d", color.getRed(), color.getGreen(), color.getBlue() );
					}
					else
						content[ col ] = obj.toString();
				}
				writer.writeNext( content );
			}
		}
	}

	/*
	 * INNER CLASSES
	 */

	private class MyTableToolTipProvider extends MouseMotionAdapter
	{
		private int previousCol = -1;

		@Override
		public void mouseMoved( final MouseEvent evt )
		{
			final TableColumnModel tableColumnModel = table.getColumnModel();
			final int col = tableColumnModel.getColumnIndexAtX( evt.getX() );
			final int vColIndex = table.convertColumnIndexToModel( col );
			if ( vColIndex != previousCol )
			{
				if ( vColIndex >= 0 && vColIndex < mapToTooltip.size() )
				{
					table.getTableHeader().setToolTipText( mapToTooltip.get( vColIndex ) );
					previousCol = vColIndex;
				}
				else
				{
					table.getTableHeader().setToolTipText( "" );
				}
			}
		}
	}

	private class MyTableModel extends AbstractTableModel
	{

		private static final long serialVersionUID = 1L;

		@Override
		public int getRowCount()
		{
			return objects.size();
		}

		@Override
		public int getColumnCount()
		{
			return columnClasses.size();
		}

		@Override
		public Object getValueAt( final int rowIndex, final int columnIndex )
		{
			if ( rowIndex < 0 )
				return null;

			final O o = objects.get( rowIndex );
			if ( null == o )
				return null;

			if ( columnIndex == 0 )
				return labelGenerator.apply( o );
			else
			{
				final String feature = features.get( columnIndex - 1 );
				final Double val = featureFun.apply( o, feature );

				if ( feature.equals( manualColorFeature ) )
					return val == null ? null : new Color( val.intValue(), true );

				if ( val == null )
					return null;

				if ( columnClasses.get( columnIndex ).equals( Integer.class ) )
					return Integer.valueOf( val.intValue() );
				else
					return val;
			}
		}

		@Override
		public void setValueAt( final Object aValue, final int rowIndex, final int columnIndex )
		{
			if ( labelSetter == null )
				return;

			if ( columnIndex == 0 )
			{
				final O o = objects.get( rowIndex );
				if ( null == o )
					return;
				labelSetter.accept( o, ( String ) aValue );
			}
		}
	}

	private class MyTableCellRenderer extends DefaultTableCellRenderer
	{

		private final Border normalBorder;

		private final DecimalFormat nf;

		private static final long serialVersionUID = 1L;

		private final FeatureColorGenerator< O > defaultColoring = o -> Color.WHITE;

		private final ColorIcon colorIcon;

		public MyTableCellRenderer()
		{
			this.normalBorder = ( ( JLabel ) super.getTableCellRendererComponent( table, "", false, false, 0, 0 ) ).getBorder();
			this.nf = new DecimalFormat();
			final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
			formatSymbols.setNaN( "NaN" );
			nf.setDecimalFormatSymbols( formatSymbols );
			colorIcon = new ColorIcon( new Color( 0 ) );
		}

		@Override
		public Component getTableCellRendererComponent( final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column )
		{
			final FeatureColorGenerator< O > coloring = useColoring ? colorSupplier.get() : defaultColoring;
			final JLabel c = ( JLabel ) super.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
			c.setBorder( normalBorder );

			final O o = getObjectForViewRow( row );
			if ( isSelected )
			{
				c.setBackground( table.getSelectionBackground() );
				c.setForeground( table.getSelectionForeground() );
			}
			else
			{
				final Color bgColor = coloring.color( o );
				c.setBackground( bgColor );
				c.setForeground( GuiUtils.textColorForBackground( bgColor ) );
			}

			if ( hasFocus )
			{
				c.setBackground( table.getSelectionBackground().darker().darker() );
				c.setForeground( table.getSelectionForeground() );
			}

			c.setIcon( null );
			if ( value instanceof Double )
			{
				setHorizontalAlignment( JLabel.RIGHT );
				final Double doubleValue = ( Double ) value;
				setText( nf.format( doubleValue.doubleValue() ) );
			}
			else if ( value instanceof Number )
			{
				setHorizontalAlignment( JLabel.RIGHT );
			}
			else if ( value instanceof Color )
			{
				colorIcon.setColor( ( Color ) value );
				c.setIcon( colorIcon );
				c.setText( null );
				setHorizontalAlignment( JLabel.CENTER );
			}
			else
			{
				setHorizontalAlignment( JLabel.LEFT );
			}

			return c;
		}
	}

	private class MyColorEditor extends AbstractCellEditor implements TableCellEditor
	{

		private final JColorChooser colorChooser = new JColorChooser();

		private static final long serialVersionUID = 1L;

		private final BiConsumer< O, Color > colorSetter;

		public MyColorEditor( final BiConsumer< O, Color > colorSetter )
		{
			this.colorSetter = colorSetter;
		}

		@Override
		public Object getCellEditorValue()
		{
			return null;
		}

		@Override
		public Component getTableCellEditorComponent(
				final JTable table,
				final Object value,
				final boolean isSelected,
				final int row,
				final int column )
		{
			final ColorIcon icon = new ColorIcon( ( Color ) value, 16, 0 );
			final JButton button = new JButton( icon );
			button.setHorizontalAlignment( JLabel.CENTER );
			button.addActionListener( e -> {
				colorChooser.setColor( ( Color ) value );
				final JDialog d = JColorChooser.createDialog( button, "Choose a color", true, colorChooser, new ActionListener()
				{
					@Override
					public void actionPerformed( final ActionEvent arg0 )
					{
						final Color c = colorChooser.getColor();
						if ( c != null )
						{
							final int[] rows = TablePanel.this.getTable().getSelectedRows();
							if ( rows.length > 1 )
							{
								for ( final int r : rows )
								{
									final O o = TablePanel.this.getObjectForViewRow( r );
									colorSetter.accept( o, c );
								}
								panel.repaint();
							}
							else
							{
								final O o = TablePanel.this.getObjectForViewRow( row );
								colorSetter.accept( o, c );
							}
							icon.setColor( c );
						}
					}
				}, null );
				d.setVisible( true );
			} );
			return button;
		}
	}

	/**
	 * A cell editor for text that selects all when triggered for editing.
	 */
	private static class MyLabelEditor extends DefaultCellEditor
	{

		private static final long serialVersionUID = 1L;

		public MyLabelEditor()
		{
			super( new JTextField() );
		}

		@Override
		public Component getTableCellEditorComponent( final JTable table, final Object value, final boolean isSelected, final int row, final int column )
		{
			final JTextField textfield = ( JTextField ) super.getTableCellEditorComponent( table, value, isSelected, row, column );
			SwingUtilities.invokeLater( () -> textfield.selectAll() );
			return textfield;
		}
	}
}
