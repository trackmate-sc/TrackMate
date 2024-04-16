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
package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.gui.Icons.TRACK_TABLES_ICON;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.io.TmXmlReader;
import ij.ImageJ;
import ij.ImagePlus;

public class MotilityLabExporter extends AbstractTMAction
{

	public static final String NAME = "Export to MotilityLab spreadsheet";

	public static final String KEY = "MOTILITYLAB_EXPORTER";

	public static final String INFO_TEXT = "<html>"
			+ "Display the visible tracks in a spreadsheet that can be "
			+ "copy-pasted directly into the "
			+ "<a url=\"http://www.motilitylab.net/import/data-import.php\"> MotilityLab website</a> "
			+ "for further track analysis.";

	private static final int ROW_HEIGHT = 22;

	private final static List< String > HEADERS = Arrays.asList(
			"Tracking ID",
			"Timepoint",
			"Time (sec)",
			"X pos (µm)",
			"Y pos (µm)",
			"Z pos (µm)" );

	private final static List< Class< ? > > CLASSES = Arrays.asList(
			Integer.class,
			Integer.class,
			Double.class,
			Double.class,
			Double.class,
			Double.class );

	@Override
	public void execute(
			final TrackMate trackmate,
			final SelectionModel selectionModel,
			final DisplaySettings displaySettings,
			final Frame parent )
	{
		final JPanel panel = createSpotTable( trackmate.getModel() );
		
		final JFrame frame = new JFrame( "TrackMate MotilityLab table export" );
		frame.setIconImage( TRACK_TABLES_ICON.getImage() );
		frame.getContentPane().add( panel );
		frame.setLocationRelativeTo( parent );
		frame.pack();
		frame.setVisible( true );
	}

	private final JPanel createSpotTable( final Model model )
	{
		// Objects.
		final List< Spot > spots = new ArrayList<>( model.getTrackModel().vertexSet() );

		// Table.
		final MyTableModel tableModel = new MyTableModel( spots, model.getTrackModel() );
		final JTable table = new JTable( tableModel );

		// Renderer.
		final MyTableCellRenderer cellRenderer = new MyTableCellRenderer();
		for ( int c = 0; c < table.getColumnModel().getColumnCount(); c++ )
		{
			final TableColumn column = table.getColumnModel().getColumn( c );
			column.setCellRenderer( cellRenderer );
		}

		// Look.
		table.setRowHeight( ROW_HEIGHT );
		table.getSelectionModel().setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		table.setGridColor( table.getTableHeader().getBackground() );
		table.getTableHeader().setPreferredSize( new Dimension( 100, ( int ) ( 1.5 * ROW_HEIGHT ) ) );

		// Sorting.
		final TableRowSorter< MyTableModel > sorter = new TableRowSorter<>( tableModel );
		for ( int c = 0; c < CLASSES.size(); c++ )
		{
			if ( CLASSES.get( c ).equals( Integer.class ) )
				sorter.setComparator( c, ( i1, i2 ) -> Integer.compare( ( int ) i1, ( int ) i2 ) );
			else if ( CLASSES.get( c ).equals( Double.class ) )
				sorter.setComparator( c, ( d1, d2 ) -> Double.compare( ( double ) d1, ( double ) d2 ) );
			else if ( CLASSES.get( c ).equals( Color.class ) )
				sorter.setComparator( c, ( c1, c2 ) -> c1.toString().compareTo( c2.toString() ) );
			else
				sorter.setComparator( c, Comparator.naturalOrder() );
		}

		table.setRowSorter( sorter );
		sorter.setSortKeys( Arrays.asList( new SortKey( 0, SortOrder.ASCENDING ), new SortKey( 1, SortOrder.ASCENDING ) ) );
		sorter.sort();

		// Scrollpane.
		final JScrollPane scrollPane = new JScrollPane( table,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );

		// Main panel.
		final JPanel panel = new JPanel();
		panel.setLayout( new BorderLayout() );
		panel.add( scrollPane, BorderLayout.CENTER );
		return panel;
	}

	/*
	 * INNER CLASSES.
	 */

	private class MyTableModel extends AbstractTableModel
	{

		private static final long serialVersionUID = 1L;

		private final List< Spot > spots;

		private final TrackModel trackModel;

		public MyTableModel( final List< Spot > spots, final TrackModel trackModel )
		{
			this.spots = spots;
			this.trackModel = trackModel;
		}

		@Override
		public int getRowCount()
		{
			return spots.size();
		}

		@Override
		public int getColumnCount()
		{
			return 6;
		}

		@Override
		public String getColumnName( final int column )
		{
			return HEADERS.get( column );
		}

		@Override
		public Object getValueAt( final int rowIndex, final int columnIndex )
		{
			if ( rowIndex < 0 || rowIndex >= spots.size() )
				return null;

			final Spot spot = spots.get( rowIndex );
			if ( null == spot )
				return null;

			switch ( columnIndex )
			{
			case 0:
				return trackModel.trackIDOf( spot );
			case 1:
				return spot.getFeature( Spot.FRAME ).intValue();
			case 2:
				return spot.getFeature( Spot.POSITION_T );
			case 3:
				return spot.getFeature( Spot.POSITION_X );
			case 4:
				return spot.getFeature( Spot.POSITION_Y );
			case 5:
				return spot.getFeature( Spot.POSITION_Z );
			default:
				throw new IllegalArgumentException( "Undefined column index: " + columnIndex );
			}
		}
	}

	private class MyTableCellRenderer extends DefaultTableCellRenderer
	{

		private final DecimalFormat nf;

		private static final long serialVersionUID = 1L;

		public MyTableCellRenderer()
		{
			this.nf = new DecimalFormat();
			final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
			formatSymbols.setNaN( "NaN" );
			nf.setDecimalFormatSymbols( formatSymbols );
		}

		@Override
		public Component getTableCellRendererComponent( final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column )
		{
			final JLabel c = ( JLabel ) super.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );

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

	@Plugin( type = TrackMateActionFactory.class, enabled = true )
	public static class Factory implements TrackMateActionFactory
	{

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getName()
		{
			return NAME;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public TrackMateAction create()
		{
			return new MotilityLabExporter();
		}

		@Override
		public ImageIcon getIcon()
		{
			return TRACK_TABLES_ICON;
		}
	}

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		
		final TmXmlReader reader = new TmXmlReader(  new File("samples/MAX_Merged.xml" ));
		final Model model = reader.getModel();
		final ImagePlus imp = reader.readImage();
		final Settings settings = reader.readSettings( imp );
		new MotilityLabExporter().execute(
				new TrackMate( model, settings ),
				new SelectionModel( model ),
				DisplaySettingsIO.readUserDefault(),
				null );
	}

}
