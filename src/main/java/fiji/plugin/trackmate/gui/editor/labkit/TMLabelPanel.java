package fiji.plugin.trackmate.gui.editor.labkit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import bdv.viewer.ViewerPanel;
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

	private final LabelingModel model;

	private ViewerPanel viewer;

	public TMLabelPanel( final LabelingModel model, final ViewerPanel viewerPanel )
	{
		super( new BorderLayout() );
		this.model = model;
		this.viewer = viewerPanel;

		final JList< Label > list = new JList<>( new MyListModel( model.labeling() ) );
		list.setCellRenderer( new LabelListCellRenderer() );
		list.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		list.addListSelectionListener( e -> {
			if ( !e.getValueIsAdjusting() )
			{
				final Label l = list.getSelectedValue();
				if ( l != null )
				{
					localizeLabel( l );
				}
			}
		} );

		final JScrollPane scrollPane = new JScrollPane( list );
		scrollPane.setBorder( null );
		add( scrollPane );
		setPreferredSize( new Dimension( 150, 150 ) );
	}

	public void localizeLabel( final Label label )
	{
		Interval interval = getBoundingBox( model.labeling().get().iterableRegions()
				.get( label ) );
		if ( interval == null )
			return;
		interval = Intervals.expand( interval, Math.max( interval.dimension( 0 ), 20 ), 0 );
		interval = Intervals.expand( interval, Math.max( interval.dimension( 1 ), 20 ), 1 );
		model.transformationModel().transformToShowInterval( interval, model
				.labelTransformation() );

		// TODO Can we get animation to work?
//		final AffineTransform3D c = new AffineTransform3D();
//		viewer.state().getViewerTransform( c );
//		final double cX = viewer.getDisplayComponent().getWidth() / 2.0;
//		final double cY = viewer.getDisplayComponent().getHeight() / 2.0;
//		c.set( c.get( 0, 3 ) - cX, 0, 3 );
//		c.set( c.get( 1, 3 ) - cY, 1, 3 );
//		model.selectedLabel().set( label );
//		viewer.setTransformAnimator( new SimilarityTransformAnimator( c, model
//				.labelTransformation(), cX, cY, 300 ) );

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

	private static class LabelListCellRenderer extends JLabel implements ListCellRenderer< Label >
	{
		private static final long serialVersionUID = 1L;

		private final Dimension preferredSize;

		private final Color bg;

		public LabelListCellRenderer()
		{
			super( "", SwingConstants.CENTER );
			final Font smallerFont = getFont().deriveFont( getFont().getSize() - 4f );
			setFont( smallerFont );
			this.preferredSize = super.getPreferredSize();
			preferredSize.height += 25;
			this.bg = UIManager.getColor( "ScrollBar.background" );
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
	};

	private static final long serialVersionUID = 1L;

}
