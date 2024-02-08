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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class WrapLayout extends FlowLayout
{
	private static final long serialVersionUID = 1L;

	public WrapLayout()
	{
		super();
	}

	public WrapLayout( final int align )
	{
		super( align );
	}

	public WrapLayout( final int align, final int hgap, final int vgap )
	{
		super( align, hgap, vgap );
	}

	@Override
	public Dimension preferredLayoutSize( final Container target )
	{
		return layoutSize( target, true );
	}

	@Override
	public Dimension minimumLayoutSize( final Container target )
	{
		final Dimension minimum = layoutSize( target, false );
		minimum.width -= ( getHgap() + 1 );
		return minimum;
	}

	private Dimension layoutSize( final Container target, final boolean preferred )
	{
		synchronized ( target.getTreeLock() )
		{
			int targetWidth = target.getSize().width;

			if ( targetWidth == 0 )
				targetWidth = Integer.MAX_VALUE;

			final int hgap = getHgap();
			final int vgap = getVgap();
			final Insets insets = target.getInsets();
			final int horizontalInsetsAndGap = insets.left + insets.right + ( hgap * 2 );
			final int maxWidth = targetWidth - horizontalInsetsAndGap;

			final Dimension dim = new Dimension( 0, 0 );
			int rowWidth = 0;
			int rowHeight = 0;

			final int nmembers = target.getComponentCount();

			for ( int i = 0; i < nmembers; i++ )
			{
				final Component m = target.getComponent( i );

				if ( m.isVisible() )
				{
					final Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();

					if ( rowWidth + d.width > maxWidth )
					{
						addRow( dim, rowWidth, rowHeight );
						rowWidth = 0;
						rowHeight = 0;
					}

					if ( rowWidth != 0 )
					{
						rowWidth += hgap;
					}

					rowWidth += d.width;
					rowHeight = Math.max( rowHeight, d.height );
				}
			}

			addRow( dim, rowWidth, rowHeight );

			dim.width += horizontalInsetsAndGap;
			dim.height += insets.top + insets.bottom + vgap * 2;

			final Container scrollPane = SwingUtilities.getAncestorOfClass( JScrollPane.class, target );
			if ( scrollPane != null )
			{
				dim.width -= ( hgap + 1 );
			}

			return dim;
		}
	}

	private void addRow( final Dimension dim, final int rowWidth, final int rowHeight )
	{
		dim.width = Math.max( dim.width, rowWidth );

		if ( dim.height > 0 )
		{
			dim.height += getVgap();
		}

		dim.height += rowHeight;
	}
}
