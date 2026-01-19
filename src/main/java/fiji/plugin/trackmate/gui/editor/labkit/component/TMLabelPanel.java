/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
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
package fiji.plugin.trackmate.gui.editor.labkit.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

import org.scijava.ui.behaviour.util.RunnableAction;

import fiji.plugin.trackmate.gui.GuiUtils;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.roi.IterableRegion;
import net.imglib2.type.logic.BitType;
import net.imglib2.util.Intervals;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.labeling.Labeling;
import sc.fiji.labkit.ui.models.Holder;
import sc.fiji.labkit.ui.models.LabelingModel;

public class TMLabelPanel extends JPanel
{

	public static final int CELL_HEIGHT = 30;

	private final LabelingModel model;

	private final MyListModel listModel;

	public TMLabelPanel( final LabelingModel model )
	{
		super( new BorderLayout() );
		this.model = model;
		this.listModel = new MyListModel( model.labeling() );
		final JList< Label > list = new JList<>( listModel );
		list.setCellRenderer( new LabelListCellRenderer() );
		list.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		// When the user clicks on the list.
		list.addListSelectionListener( e -> {
			if ( !e.getValueIsAdjusting() )
			{
				final Label l = list.getSelectedValue();
				if ( l != null )
				{
					// Select the label.
					model.selectedLabel().set( l );
					// Repaint the list.
					list.repaint();
				}
			}
		} );
		list.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseClicked( final java.awt.event.MouseEvent e )
			{
				if ( SwingUtilities.isLeftMouseButton( e ) && e.isShiftDown() )
				{
					final int index = list.locationToIndex( e.getPoint() );
					list.setSelectedIndex( index );
					e.consume();
					final Label label = list.getModel().getElementAt( index );
					localizeLabel( label );
				}
			}
		} );

		// Redraw and scroll the list when the selected label is changed elsewhere.
		model.selectedLabel().notifier().addListener( () -> {
			final Label label = model.selectedLabel().get();
			final int i = model.labeling().get().getLabels().indexOf( label );
			if ( i < 0 )
				return;
			final int w = getWidth();
			final int h = CELL_HEIGHT;
			final int y = h * i;
			final Rectangle rectangle = new Rectangle( 0, y, w, h );
			list.scrollRectToVisible( rectangle );
			list.repaint();
		} );

		// The list
		final JScrollPane scrollPane = new JScrollPane( list );
		list.setOpaque( false );
		scrollPane.setOpaque( false );
		scrollPane.getViewport().setOpaque( false );
		scrollPane.setBorder( null );
		add( scrollPane, BorderLayout.CENTER );

		// The add label button
		add( initializeAddLabelButton(), BorderLayout.SOUTH );

		setPreferredSize( new Dimension( 150, 150 ) );
		setMinimumSize( new Dimension( 150, 150 ) );
	}

	private void addLabel()
	{
		final Holder< Labeling > holder = model.labeling();
		final Labeling labeling = holder.get();
		final String newName = suggestName( labeling.getLabels().stream().map( Label::name ).collect( Collectors.toList() ) );
		if ( newName == null )
			return;

		final Label newLabel = labeling.addLabel( newName );
		model.selectedLabel().set( newLabel );
		listModel.notifyElementAdded();
		model.labeling().notifier().notifyListeners();
	}

	public void localizeLabel( final Label label )
	{
		Interval interval = getBoundingBox( model.labeling().get().iterableRegions()
				.get( label ) );
		if ( interval == null )
			return;
		interval = Intervals.expand( interval, Math.max( interval.dimension( 0 ), 20 ), 0 );
		interval = Intervals.expand( interval, Math.max( interval.dimension( 1 ), 20 ), 1 );

		model.transformationModel().transformToShowInterval( interval, model.labelTransformation() );
	}

	private static Interval getBoundingBox( final IterableRegion< BitType > region )
	{
		final int numDimensions = region.numDimensions();
		final Cursor< ? > cursor = region.inside().cursor();
		if ( !cursor.hasNext() )
			return null;
		final long[] min = new long[ numDimensions ];
		final long[] max = new long[ numDimensions ];
		cursor.fwd();
		cursor.localize( min );
		cursor.localize( max );
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			for ( int i = 0; i < numDimensions; i++ )
			{
				final int pos = cursor.getIntPosition( i );
				min[ i ] = Math.min( min[ i ], pos );
				max[ i ] = Math.max( max[ i ], pos );
			}
		}
		return new FinalInterval( min, max );
	}

	private class LabelListCellRenderer extends JLabel implements ListCellRenderer< Label >
	{
		private static final long serialVersionUID = 1L;

		private final Dimension preferredSize;

		private final Color bg;

		private final CompoundBorder border;

		public LabelListCellRenderer()
		{
			super( "", SwingConstants.CENTER );
			final Font smallerFont = getFont().deriveFont( getFont().getSize() - 4f );
			setFont( smallerFont );
			this.preferredSize = super.getPreferredSize();
			preferredSize.height = CELL_HEIGHT;
			this.bg = UIManager.getColor( "Panel.background" );
			final Border black = BorderFactory.createLineBorder( bg, 4, true );
			final Border white = BorderFactory.createLineBorder( Color.BLACK, 4, true );
			this.border = BorderFactory.createCompoundBorder( black, white );
		}

		@Override
		public Component getListCellRendererComponent(
				final JList< ? extends Label > list,
				final Label label,
				final int index,
				final boolean isSelected,
				final boolean cellHasFocus )
		{
			setText( label.name() );
			final Color bg = new Color( label.color().get() );
			final Color fg = ( bg == null ) ? null : GuiUtils.textColorForBackground( bg );
			setOpaque( true );
			setBackground( bg );
			setForeground( fg );
			final boolean selected = model.selectedLabel().get().equals( label );
			if ( selected )
			{
				setFont( getFont().deriveFont( Font.BOLD ) );
				setBorder( border );
			}
			else
			{
				setFont( getFont().deriveFont( Font.PLAIN ) );
				setBorder( null );
			}
			return this;
		}

		@Override
		protected void paintComponent( final Graphics g )
		{
			final Graphics2D g2d = ( Graphics2D ) g;
			g2d.setColor( bg );
			g2d.fillRect( 0, 0, getWidth(), getHeight() );
			super.paintComponent( g );
			g2d.setColor( bg );
			g2d.drawRect( 0, 0, getWidth(), getHeight() );
			g2d.drawRect( 1, 1, getWidth() - 1, getHeight() - 1 );
			g2d.drawRoundRect( 1, 1, getWidth() - 1, getHeight() - 1, 5, 5 );
		}

		@Override
		public Dimension getPreferredSize()
		{
			return preferredSize;
		}
	}

	private static final class MyListModel extends AbstractListModel< Label >
	{
		private static final long serialVersionUID = 1L;

		private final Holder< Labeling > labeling;

		public MyListModel( final Holder< Labeling > labeling )
		{
			this.labeling = labeling;
		}

		@Override
		public Label getElementAt( final int index )
		{
			return labeling.get().getLabels().get( index );
		}

		@Override
		public int getSize()
		{
			return labeling.get().getLabels().size();
		}

		public void notifyElementAdded()
		{
			final int n = getSize() - 1;
			fireIntervalAdded( this, n, n );
		}
	}

	private JButton initializeAddLabelButton()
	{
		final RunnableAction addLabelAction = new RunnableAction( "Add label", this::addLabel );
		final JButton button = sc.fiji.labkit.ui.panel.GuiUtils.createActionIconButton( "Add label", addLabelAction, "add.png" );
		button.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW )
				.put( KeyStroke.getKeyStroke( "ctrl A" ), "create new label" );
		button.getActionMap().put( "create new label", addLabelAction );
		button.setToolTipText( "<html><small>Keyboard shortcut:</small></html>" );
		return button;
	}

	private static String suggestName( final List< String > labels )
	{
		for ( int i = 1; i < 10000; i++ )
		{
			final String label = "Label_" + i;
			if ( !labels.contains( label ) )
				return label;
		}
		return null;
	}

	private static final long serialVersionUID = 1L;

}
