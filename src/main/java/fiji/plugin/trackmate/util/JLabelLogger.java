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

import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.Logger;

public class JLabelLogger extends JLabel
{

	private static final long serialVersionUID = 1L;

	private final LoggerInJLabel logger;

	public JLabelLogger()
	{
		this.logger = new LoggerInJLabel( this );
		setHorizontalAlignment( SwingConstants.CENTER );
		setFont( SMALL_FONT );
	}

	public Logger getLogger()
	{
		return logger;
	}

	/*
	 * INNER CLASS
	 */

	private static class LoggerInJLabel extends Logger
	{

		private final JLabel label;

		public LoggerInJLabel( final JLabel label )
		{
			this.label = label;
		}

		@Override
		public void log( final String message, final Color color )
		{

			final String msg;
			if ( null == message )
				msg = "null"; // Help debug.
			else if ( message.startsWith( "<html>" ) )
				msg = message;
			else
				msg = "<html>" + message + "</html>";

			label.setText( msg );
			label.setForeground( color );
		}

		@Override
		public void error( final String message )
		{
			log( message, Logger.ERROR_COLOR );
		}

		/** Ignored. */
		@Override
		public void setProgress( final double val )
		{}

		@Override
		public void setStatus( final String status )
		{
			log( status, Logger.BLUE_COLOR );
		}
	}
}
