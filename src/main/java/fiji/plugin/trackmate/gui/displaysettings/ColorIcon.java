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
package fiji.plugin.trackmate.gui.displaysettings;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;

/**
 * Adapted from http://stackoverflow.com/a/3072979/230513
 */
public class ColorIcon implements Icon
{
	private static final int DEFAULT_PAD = 0;

	private static final int DEFAUL_SIZE = 16;

	private final int size;

	private Color color;

	private final int pad;

	public ColorIcon( final Color color, final int size, final int pad )
	{
		this.color = color;
		this.size = size;
		this.pad = pad;
	}

	public ColorIcon( final Color color, final int size )
	{
		this( color, size, DEFAULT_PAD );
	}

	public ColorIcon( final Color color )
	{
		this( color, DEFAUL_SIZE );
	}

	@Override
	public void paintIcon( final Component c, final Graphics g, final int x, final int y )
	{
		final Graphics2D g2d = ( Graphics2D ) g;
		g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		g2d.setColor( color );
		final RoundRectangle2D.Float shape = new RoundRectangle2D.Float( x + pad, y + pad, size, size, 5, 5 );
		g2d.fill( shape );
		g2d.setColor( Color.BLACK );
		g2d.draw( shape );
	}

	public void setColor( final Color color )
	{
		this.color = color;
	}

	@Override
	public int getIconWidth()
	{
		return size + 2 * pad;
	}

	@Override
	public int getIconHeight()
	{
		return size + 2 * pad;
	}
}
