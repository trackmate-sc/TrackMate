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
package fiji.plugin.trackmate.gui.featureselector;

import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.scijava.listeners.Listeners;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;

import fiji.plugin.trackmate.gui.Icons;

/**
 *
 * @param <C> collection-of-elements type
 * @param <T> element type
 */
public class FeatureTable< C, T >
{
	public static class Tables implements ListSelectionListener
	{
		private final List< FeatureTable< ?, ? > > tables = new ArrayList<>();

		public void add( final FeatureTable< ?, ? > table )
		{
			tables.add( table );
			table.tables = this;
			table.table.getSelectionModel().addListSelectionListener( this );
		}

		@Override
		public void valueChanged( final ListSelectionEvent event )
		{
			final ListSelectionModel source = ( ListSelectionModel ) event.getSource();
			for ( final FeatureTable< ?, ? > table : tables )
			{
				final ListSelectionModel lsm = table.table.getSelectionModel();
				if ( lsm.equals( source ) )
					continue;

				lsm.removeListSelectionListener( this );
				lsm.clearSelection();
				lsm.addListSelectionListener( this );
			}
		}

		boolean selectNextTable( final FeatureTable< ?, ? > table )
		{
			final int i = tables.indexOf( table );
			if ( i < 0 || i >= tables.size() - 1 )
				return false;

			final JTable next = tables.get( i + 1 ).table;
			if ( next.getRowCount() > 0 )
			{
				table.clearSelectionQuiet();
				next.setRowSelectionInterval( 0, 0 );
				next.requestFocusInWindow();
			}
			return true;
		}

		boolean selectPreviousTable( final FeatureTable< ?, ? > table )
		{
			final int i = tables.indexOf( table );
			if ( i <= 0 )
				return false;

			final JTable previous = tables.get( i - 1 ).table;
			final int rows = previous.getRowCount();
			if ( rows > 0 )
			{
				table.clearSelectionQuiet();
				previous.setRowSelectionInterval( rows - 1, rows - 1 );
				previous.requestFocusInWindow();
			}
			return true;
		}
	}

	private static final ImageIcon UP_TO_DATE_ICON = Icons.BULLET_GREEN_ICON;
	private static final ImageIcon NOT_UP_TO_DATE_ICON = Icons.QUESTION_ICON;

	private C elements;
	private final ToIntFunction< C > size;
	private final BiFunction< C, Integer, T > get;
	private final Function< T, String > getName;
	private final Predicate< T > isSelected;
	private final BiConsumer< T, Boolean > setSelected;
	private final Predicate< T > isUptodate;

	private final Listeners.List< SelectionListener< T > > selectionListeners;

	private final ListSelectionListener listSelectionListener;

	private final MyTableModel tableModel;

	private final JTable table;

	private Tables tables;

	/**
	 * Creates a new feature table.
	 * 
	 * @param elements
	 *            collection of elements.
	 * @param size
	 *            given collection returns number of elements.
	 * @param get
	 *            given collection and index returns element at index.
	 * @param getName
	 *            given element returns name.
	 * @param isSelected
	 *            given element returns whether it is selected.
	 * @param setSelected
	 *            given element and boolean sets selected state of element.
	 * @param isUptodate
	 *            given element returns whether it is up-to-date.
	 */
	public FeatureTable(
			final C elements,
			final ToIntFunction< C > size,
			final BiFunction< C, Integer, T > get,
			final Function< T, String > getName,
			final Predicate< T > isSelected,
			final BiConsumer< T, Boolean > setSelected,
			final Predicate< T > isUptodate )
	{
		this.elements = elements;
		this.size = size;
		this.get = get;
		this.getName = getName;
		this.isSelected = isSelected;
		this.setSelected = setSelected;
		this.isUptodate = isUptodate;

		selectionListeners = new Listeners.SynchronizedList<>();

		tableModel = new MyTableModel();
		table = new JTable( tableModel );
		table.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		table.setTableHeader( null );
		table.setFillsViewportHeight( true );
		table.setAutoResizeMode( JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS );
		table.setRowHeight( 30 );
		listSelectionListener = e -> {
			if ( e.getValueIsAdjusting() )
				return;
			final int row = table.getSelectedRow();
			final T selected = ( this.elements != null && row >= 0 && row < this.size.applyAsInt( this.elements ) )
					? this.get.apply( this.elements, row )
					: null;
			selectionListeners.list.forEach( l -> l.selectionChanged( selected ) );
		};
		table.getSelectionModel().addListSelectionListener( listSelectionListener );
		table.setIntercellSpacing( new Dimension( 0, 0 ) );
		table.setSurrendersFocusOnKeystroke( true );
		table.setFocusTraversalKeys( KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null );
		table.setFocusTraversalKeys( KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null );
		table.getColumnModel().getColumn( 0 ).setMaxWidth( 30 );
		table.getColumnModel().getColumn( 2 ).setMaxWidth( 64 );
		table.getColumnModel().getColumn( 2 ).setCellRenderer( new UpdatedCellRenderer() );
		table.setShowGrid( false );

		final Actions actions = new Actions( table.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ), table.getActionMap(), new InputTriggerConfig() );
		actions.runnableAction( this::toggleSelectedRow, "toggle selected row", "SPACE", "ENTER" );
		actions.runnableAction( this::nextRowOrTable, "select next row or table", "DOWN" );
		actions.runnableAction( this::previousRowOrTable, "select previous row or table", "UP" );

		setElements( elements );
	}

	private void toggleSelectedRow()
	{
		final int row = table.getSelectedRow();
		if ( row >= 0 )
		{
			final T feature = get.apply( elements, row );
			setSelected.accept( feature, !isSelected.test( feature ) );
			tableModel.fireTableCellUpdated( row, 0 );
		}
	}

	private void nextRowOrTable()
	{
		final int row = table.getSelectedRow();
		if ( elements == null || tables == null || row != table.getRowCount() - 1 || !tables.selectNextTable( this ) )
		{
			final Action action = table.getActionMap().get( "selectNextRow" );
			if ( action != null )
				action.actionPerformed( new ActionEvent( table, 0, null ) );
		}
	}

	private void previousRowOrTable()
	{
		final int row = table.getSelectedRow();
		if ( elements == null || tables == null || row != 0 || !tables.selectPreviousTable( this ) )
		{
			final Action action = table.getActionMap().get( "selectPreviousRow" );
			if ( action != null )
				action.actionPerformed( new ActionEvent( table, 0, null ) );
		}
	}

	private void clearSelectionQuiet()
	{
		table.getSelectionModel().removeListSelectionListener( listSelectionListener );
		table.clearSelection();
		table.getSelectionModel().addListSelectionListener( listSelectionListener );

	}

	/**
	 * Exposes the component in which the elements are displayed.
	 *
	 * @return the component.
	 */
	public JComponent getComponent()
	{
		return table;
	}

	/**
	 * Sets the collection of elements to show.
	 *
	 * @param elements
	 *            the collection of elements to show.
	 */
	public void setElements( final C elements )
	{
		this.elements = elements;
		if ( elements == null )
			selectionListeners.list.forEach( l -> l.selectionChanged( null ) );
		else
			tableModel.fireTableDataChanged();
	}

	public void selectFirstRow()
	{
		if ( table.getRowCount() > 0 )
			table.setRowSelectionInterval( 0, 0 );
	}

	public interface SelectionListener< T >
	{
		void selectionChanged( T selected );
	}

	public Listeners< SelectionListener< T > > selectionListeners()
	{
		return selectionListeners;
	}

	private class MyTableModel extends DefaultTableModel
	{

		private static final long serialVersionUID = 1L;

		@Override
		public int getColumnCount()
		{
			return 3;
		}

		@Override
		public int getRowCount()
		{
			return ( null == elements ) ? 0 : size.applyAsInt( elements );
		}

		public T get( final int index )
		{
			return get.apply( elements, Integer.valueOf( index ) );
		}

		@Override
		public Object getValueAt( final int rowIndex, final int columnIndex )
		{
			switch ( columnIndex )
			{
			case 0:
				return isSelected.test( get( rowIndex ) );
			case 1:
				return getName.apply( get( rowIndex ) );
			case 2:
				return isUptodate.test( get( rowIndex ) );
			}
			throw new IllegalArgumentException( "Cannot return value for colum index larger than " + getColumnCount() );
		}

		@Override
		public Class< ? > getColumnClass( final int columnIndex )
		{
			switch ( columnIndex )
			{
			case 0:
				return Boolean.class;
			case 1:
				return String.class;
			case 2:
				return Boolean.class;
			}
			throw new IllegalArgumentException( "Cannot return value for colum index larger than " + getColumnCount() );
		}

		@Override
		public boolean isCellEditable( final int rowIndex, final int columnIndex )
		{
			return columnIndex == 0;
		}

		@Override
		public void setValueAt( final Object aValue, final int rowIndex, final int columnIndex )
		{
			final boolean selected =  (columnIndex == 0)
					? ( boolean ) aValue
					: !isSelected.test( get( rowIndex ) );
			if ( selected != isSelected.test( get( rowIndex ) ) )
			{
				setSelected.accept( get( rowIndex ), ( Boolean ) aValue );
				fireTableRowsUpdated( rowIndex, rowIndex );
				for ( final SelectionListener< T > listener : selectionListeners.list )
					listener.selectionChanged( get( rowIndex ) );
			}
		}
	}

	private class UpdatedCellRenderer implements TableCellRenderer
	{

		private final DefaultTableCellRenderer renderer;

		public UpdatedCellRenderer()
		{
			this.renderer = new DefaultTableCellRenderer();
			final JLabel label = ( JLabel ) renderer.getTableCellRendererComponent( null, null, false, false, 0, 0 );
			label.setHorizontalAlignment( SwingConstants.CENTER );
		}

		@Override
		public Component getTableCellRendererComponent(
				final JTable table,
				final Object value,
				final boolean isSelected,
				final boolean hasFocus,
				final int row,
				final int column )
		{
			final JLabel label = ( JLabel ) renderer.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
			label.setIcon( isUptodate.test( get.apply( elements, Integer.valueOf( row ) ) )
					? UP_TO_DATE_ICON
					: NOT_UP_TO_DATE_ICON );
			label.setText( "" );
			return label;
		}
	}
}
