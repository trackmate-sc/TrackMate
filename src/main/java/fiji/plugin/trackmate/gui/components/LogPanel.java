/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2021 Fiji developers.
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
package fiji.plugin.trackmate.gui.components;

import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import fiji.plugin.trackmate.Logger;

/**
 * A panel using s {@link JTextPane} to log events.
 * 
 * @author Jean-Yves Tinevez - September 2010 - January 2011.
 */
public class LogPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	public static final String DESCRIPTOR = "LogPanel";

	private final JTextPane textPane;

	private final Logger logger;

	public LogPanel()
	{

		final BorderLayout layout = new BorderLayout();
		this.setLayout( layout );
		this.setPreferredSize( new java.awt.Dimension( 270, 500 ) );

		final JPanel panelProgressBar = new JPanel();
		final BorderLayout jPanelProgressBarLayout = new BorderLayout();
		panelProgressBar.setLayout( jPanelProgressBarLayout );
		this.add( panelProgressBar, BorderLayout.NORTH );
		panelProgressBar.setPreferredSize( new java.awt.Dimension( 270, 32 ) );

		final JProgressBar progressBar = new JProgressBar();
		panelProgressBar.add( progressBar, BorderLayout.CENTER );
		progressBar.setPreferredSize( new java.awt.Dimension( 270, 20 ) );
		progressBar.setStringPainted( true );
		progressBar.setFont( SMALL_FONT );

		final JScrollPane scrollPane = new JScrollPane();
		this.add( scrollPane );
		scrollPane.setPreferredSize( new java.awt.Dimension( 262, 136 ) );

		textPane = new JTextPane();
		textPane.setEditable( true );
		textPane.setFont( SMALL_FONT );
		scrollPane.setViewportView( textPane );
		textPane.setBackground( this.getBackground() );

		logger = new Logger()
		{

			@Override
			public void error( final String message )
			{
				log( message, Logger.ERROR_COLOR );
			}

			@Override
			public void log( final String message, final Color color )
			{
				SwingUtilities.invokeLater( new Runnable()
				{
					@Override
					public void run()
					{
						final StyleContext sc = StyleContext.getDefaultStyleContext();
						final AttributeSet aset = sc.addAttribute( SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color );
						final int len = textPane.getDocument().getLength();
						textPane.setCaretPosition( len );
						textPane.setCharacterAttributes( aset, false );
						textPane.replaceSelection( message );
					}
				} );
			}

			@Override
			public void setStatus( final String status )
			{
				SwingUtilities.invokeLater( new Runnable()
				{
					@Override
					public void run()
					{
						progressBar.setString( status );
					}
				} );
			}

			@Override
			public void setProgress( double val )
			{
				if ( val < 0 )
					val = 0;
				if ( val > 1 )
					val = 1;
				final int intVal = ( int ) ( val * 100 );
				SwingUtilities.invokeLater( new Runnable()
				{
					@Override
					public void run()
					{
						progressBar.setValue( intVal );
					}
				} );
			}
			
			@Override
			public String toString()
			{
				return getTextContent();
			}
		};
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * @return a {@link Logger} object that will log all events to this log
	 *         panel.
	 */
	public Logger getLogger()
	{
		return logger;
	}

	/**
	 * @return the text content currently displayed in the log panel.
	 */
	public String getTextContent()
	{
		return textPane.getText();
	}

	/**
	 * Set the text content currently displayed in the log panel.
	 * 
	 * @param log
	 *            the text to display.
	 */
	public void setTextContent( final String log )
	{
		textPane.setText( log );
	}
}
