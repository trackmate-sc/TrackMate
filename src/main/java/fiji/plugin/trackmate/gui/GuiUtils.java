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
package fiji.plugin.trackmate.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;

public class GuiUtils
{

	private static final FocusListener selectAllFocusListener = new FocusListener()
	{

		@Override
		public void focusLost( final FocusEvent e )
		{}

		@Override
		public void focusGained( final FocusEvent fe )
		{
			if ( !( fe.getSource() instanceof JTextField ) )
				return;
			final JTextField txt = ( JTextField ) fe.getSource();
			SwingUtilities.invokeLater( () -> txt.selectAll() );
		}
	};

	public static final void selectAllOnFocus( final JTextField tf )
	{
		tf.addFocusListener( selectAllFocusListener );
	}

	/**
	 * Returns the black color or white color depending on the specified
	 * background color, to ensure proper readability of the text on said
	 * background.
	 *
	 * @param backgroundColor
	 *            the background color.
	 * @return the black or white color.
	 */
	public static Color textColorForBackground( final Color backgroundColor )
	{
		if ( ( backgroundColor.getRed() * 0.299
				+ backgroundColor.getGreen() * 0.587
				+ backgroundColor.getBlue() * 0.114 ) > 150 )
			return Color.BLACK;
		else
			return Color.WHITE;
	}

	/**
	 * Positions a JFrame more or less cleverly next a {@link Component}.
	 */
	public static void positionWindow( final JFrame gui, final Component component )
	{

		if ( null != component )
		{
			// Get total size of all screens
			final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			final GraphicsDevice[] gs = ge.getScreenDevices();
			int screenWidth = 0;
			for ( int i = 0; i < gs.length; i++ )
			{
				final DisplayMode dm = gs[ i ].getDisplayMode();
				screenWidth += dm.getWidth();
			}

			final Point windowLoc = component.getLocation();
			final Dimension windowSize = component.getSize();
			final Dimension guiSize = gui.getSize();
			if ( guiSize.width > windowLoc.x )
			{
				if ( guiSize.width > screenWidth - ( windowLoc.x + windowSize.width ) )
				{
					gui.setLocationRelativeTo( null ); // give up
				}
				else
				{
					// put it to the right
					gui.setLocation( windowLoc.x + windowSize.width, windowLoc.y );
				}
			}
			else
			{
				// put it to the left
				gui.setLocation( windowLoc.x - guiSize.width, windowLoc.y );
			}

		}
		else
		{
			gui.setLocationRelativeTo( null );
		}
	}

	public static final void userCheckImpDimensions( final ImagePlus imp )
	{
		final int[] dims = imp.getDimensions();
		if ( dims[ 4 ] == 1 && dims[ 3 ] > 1 )
		{
			switch ( JOptionPane.showConfirmDialog( null,
					"It appears this image has 1 timepoint but "
							+ dims[ 3 ]
							+ " slices.\n"
							+ "Do you want to swap Z and T?",
					"Z/T swapped?", JOptionPane.YES_NO_CANCEL_OPTION ) )
			{
			case JOptionPane.YES_OPTION:
				imp.setDimensions( dims[ 2 ], dims[ 4 ], dims[ 3 ] );
				final Calibration calibration = imp.getCalibration();
				if ( 0. == calibration.frameInterval )
				{
					calibration.frameInterval = 1;
					calibration.setTimeUnit( "frame" );
				}
				break;

			case JOptionPane.CANCEL_OPTION:
				return;
			}
		}
	}

	public static void setSystemLookAndFeel()
	{
		if ( IJ.isMacOSX() || IJ.isWindows() )
		{
			try
			{
				UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
			}
			catch ( ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e )
			{
				e.printStackTrace();
			}

		}
	}
}
