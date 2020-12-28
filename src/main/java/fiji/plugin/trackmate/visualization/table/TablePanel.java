package fiji.plugin.trackmate.visualization.table;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.opencsv.CSVWriter;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import gnu.trove.map.hash.TObjectIntHashMap;

public class TablePanel< O > extends JPanel
{

	private static final long serialVersionUID = 1L;

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

	private boolean useColoring;

	public TablePanel(
			final Iterable< O > objects,
			final List< String > features,
			final BiFunction< O, String, Double > featureFun,
			final Map< String, String > featureNames,
			final Map< String, String > featureShortNames,
			final Map< String, String > featureUnits,
			final Map< String, Boolean > isInts,
			final Map< String, String > infoTexts,
			final Function< O, String > labelGenerator,
			final BiConsumer< O, String > labelSetter,
			final Supplier< FeatureColorGenerator< O > > colorSupplier )
	{
		this.featureFun = featureFun;
		this.featureNames = featureNames;
		this.featureShortNames = featureShortNames;
		this.featureUnits = featureUnits;
		this.colorSupplier = colorSupplier;
		this.objects = new ArrayList<>();
		this.map = new TObjectIntHashMap<>();
		setObjects( objects );
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
			public boolean isCellEditable( final int row, final int column )
			{
				// Only label and tags are editable.
				return ( labelSetter != null && column == 0 );
			}
		};
		table.setColumnModel( tableColumnModel );

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

			final Class< ? > pclass;
			if ( isInts.get( feature ) )
				pclass = Integer.class;
			else
				pclass = Double.class;
			columnClasses.add( pclass );
		}

		// Pass last line to column headers and set cell renderer.
		final MyTableCellRenderer cellRenderer = new MyTableCellRenderer();
		for ( int c = 0; c < tableColumnModel.getColumnCount(); c++ )
		{
			final TableColumn column = tableColumnModel.getColumn( c );
			column.setHeaderValue( headerLine.get( c ) );
			column.setCellRenderer( cellRenderer );
		}

		tableModel.fireTableStructureChanged();

		final TableRowSorter< MyTableModel > sorter = new TableRowSorter<>( tableModel );
		table.setRowSorter( sorter );

		final JScrollPane scrollPane = new JScrollPane( table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

		setLayout( new BorderLayout() );
		add( scrollPane, BorderLayout.CENTER );
	}

	public void setUseColoring( final boolean useColoring )
	{
		this.useColoring = useColoring;
	}

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
		return table.convertRowIndexToView( modelRow );
	}

	public void scrollToObject( final O o )
	{
		final Rectangle rect = table.getVisibleRect();
		final int row = getViewRowForObject( o );
		final Rectangle cellRect = table.getCellRect( row, 0, true );
		cellRect.setLocation( rect.x, cellRect.y );
		table.scrollRectToVisible( cellRect );
	}

	/**
	 * Starts editing the label of the object currently selected in the table.
	 * Has not effect if the table focus is not on the first column (the label
	 * column).
	 */
	public void editCurrentLabel()
	{
		final int col = table.getSelectedColumn();
		final int row = table.getSelectedRow();
		if ( col != 0 || row < 0 )
			return;
		table.editCellAt( row, col );
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
			final int vColIndex = tableColumnModel.getColumnIndexAtX( evt.getX() );
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

		public MyTableCellRenderer()
		{
			this.normalBorder = ( ( JLabel ) super.getTableCellRendererComponent( table, "", false, false, 0, 0 ) ).getBorder();
			this.nf = new DecimalFormat();
			final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
			formatSymbols.setNaN( "NaN" );
			nf.setDecimalFormatSymbols( formatSymbols );
		}

		@Override
		public Component getTableCellRendererComponent( final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column )
		{
			final FeatureColorGenerator< O > coloring = useColoring ? colorSupplier.get() : defaultColoring;
			final JComponent c = ( JComponent ) super.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
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
			else
			{
				setHorizontalAlignment( JLabel.LEFT );
			}

			return c;
		}
	}

}
