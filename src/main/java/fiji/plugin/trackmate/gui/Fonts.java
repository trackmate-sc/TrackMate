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
package fiji.plugin.trackmate.gui;

import java.awt.Dimension;
import java.awt.Font;

public class Fonts
{
	public static final Font FONT = new Font( "Arial", Font.PLAIN, 10 );

	public static final Font BIG_FONT = new Font( "Arial", Font.PLAIN, 14 );

	public static final Font SMALL_FONT = FONT.deriveFont( 8 );

	public static final Font SMALL_FONT_MONOSPACED = new Font( "Courier New", Font.PLAIN, 11 );

	public static final Dimension TEXTFIELD_DIMENSION = new Dimension( 40, 18 );
}
