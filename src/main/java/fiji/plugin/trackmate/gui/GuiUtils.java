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

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Window;
import java.awt.color.ColorSpace;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

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

	public static final void setFont( final JComponent panel, final Font font )
	{
		for ( final Component c : panel.getComponents() )
			c.setFont( font );
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
	 * Distance between two colors.
	 * <p>
	 * Adapted from
	 * https://stackoverflow.com/questions/9018016/how-to-compare-two-colors-for-similarity-difference
	 * and
	 * https://stackoverflow.com/questions/4593469/java-how-to-convert-rgb-color-to-cie-lab
	 * 
	 * @param a
	 *            the first color.
	 * @param b
	 *            the second color.
	 * @return the distance between the two colors.
	 */
	public static double colorDistance( final Color a, final Color b )
	{
		final float[] labA = toCIELab( a );
		final float[] labB = toCIELab( b );
		final float deltaL = labA[ 0 ] - labB[ 0 ];
		final float deltaA = labA[ 1 ] - labB[ 1 ];
		final float deltaB = labA[ 2 ] - labB[ 2 ];
		final double c1 = Math.sqrt( labA[ 1 ] * labA[ 1 ] + labA[ 2 ] * labA[ 2 ] );
		final double c2 = Math.sqrt( labB[ 1 ] * labB[ 1 ] + labB[ 2 ] * labB[ 2 ] );
		final double deltaC = c1 - c2;
		double deltaH = deltaA * deltaA + deltaB * deltaB - deltaC * deltaC;
		deltaH = deltaH < 0 ? 0 : Math.sqrt( deltaH );
		final double sc = 1.0 + 0.045 * c1;
		final double sh = 1.0 + 0.015 * c1;
		final double deltaLKlsl = deltaL / ( 1.0 );
		final double deltaCkcsc = deltaC / ( sc );
		final double deltaHkhsh = deltaH / ( sh );
		final double i = deltaLKlsl * deltaLKlsl + deltaCkcsc * deltaCkcsc + deltaHkhsh * deltaHkhsh;
		return i < 0 ? 0 : Math.sqrt( i );
	}

	private static final float[] fromCIEXYZ( final float[] colorvalue )
	{
		final double l = f( colorvalue[ 1 ] );
		final double L = 116.0 * l - 16.0;
		final double a = 500.0 * ( f( colorvalue[ 0 ] ) - l );
		final double b = 200.0 * ( l - f( colorvalue[ 2 ] ) );
		return new float[] { ( float ) L, ( float ) a, ( float ) b };
	}

	private static double f( final double x )
	{
		final double N = 4.0 / 29.0;
		if ( x > 216.0 / 24389.0 )
			return Math.cbrt( x );
		else
			return ( 841.0 / 108.0 ) * x + N;
	}

	public static final float[] toCIELab( final Color color )
	{
		final float[] rgbvalue = color.getColorComponents( null );
		final ColorSpace CIEXYZ = ColorSpace.getInstance( ColorSpace.CS_CIEXYZ );
		final float[] xyz = CIEXYZ.fromRGB( rgbvalue );
		return fromCIEXYZ( xyz );
	}

	public static final Color invert( final Color color )
	{
		return new Color( 255 - color.getRed(),
				255 - color.getGreen(),
				255 - color.getBlue() );
	}

	/**
	 * Positions a window more or less cleverly next a {@link Component}.
	 * 
	 * @param gui
	 *            the window to position.
	 * @param component
	 *            the component to position the window with respect to.
	 */
	public static void positionWindow( final Window gui, final Component component )
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

	public static void addOnClosingEvent( final JComponent component, final Runnable runnable )
	{
		component.addAncestorListener( new AncestorListener()
		{

			@Override
			public void ancestorRemoved( final AncestorEvent event )
			{}

			@Override
			public void ancestorMoved( final AncestorEvent event )
			{}

			@Override
			public void ancestorAdded( final AncestorEvent event )
			{
				SwingUtilities.getWindowAncestor( component ).addWindowListener( new WindowAdapter()
				{
					@Override
					public void windowClosing( final java.awt.event.WindowEvent e )
					{
						runnable.run();
					};
				} );
			}
		} );
	}

	/**
	 * Returns an editor pane suitable to display information about a TrackMate
	 * module. It expects to receive HTML strings, including some links. The
	 * links will be clickable and open the target URL in a browser. Nice for
	 * documentation and links to papers.
	 * 
	 * @return a new {@link JEditorPane}.
	 */
	public static JEditorPane infoDisplay()
	{
		return infoDisplay( "" );
	}

	public static JEditorPane infoDisplay( final String html )
	{
		return infoDisplay( html, true );
	}

	/**
	 * Returns an editor pane suitable to display information about a TrackMate
	 * module. It expects to receive HTML strings, including some links. The
	 * links will be clickable and open the target URL in a browser. Nice for
	 * documentation and links to papers.
	 * 
	 * @param html
	 *            the text to display.
	 * @param justify
	 *            if <code>true</code> the text will be justified.
	 * 
	 * @return a new {@link JEditorPane}.
	 */
	public static JEditorPane infoDisplay( final String html, final boolean justify )
	{
		final JEditorPane jep = new JEditorPane()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void setText( final String t )
			{
				final String text = justify
						? t.replace( "<br>", "" )
								.replace( "<p>", "<p align=\"justify\">" )
								.replace( "<html>", "<html><p align=\"justify\">" )
						: t;
				super.setText( text );
			}
		};
		jep.putClientProperty( JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE );
		jep.setContentType( "text/html" );
		jep.setText( html );
		jep.setEditable( false );
		jep.setOpaque( false );
		jep.setFont( Fonts.FONT.deriveFont( Font.ITALIC ) );
		jep.addHyperlinkListener( new HyperlinkListener()
		{
			@Override
			public void hyperlinkUpdate( final HyperlinkEvent hle )
			{
				if ( HyperlinkEvent.EventType.ACTIVATED.equals( hle.getEventType() ) )
				{
					final Desktop desktop = Desktop.getDesktop();
					try
					{
						desktop.browse( hle.getURL().toURI() );
					}
					catch ( final Exception ex )
					{
						ex.printStackTrace();
					}
				}
			}
		} );
		return jep;
	}

	public static JScrollPane textInScrollPanel( final JComponent component )
	{
		final JScrollPane scrollPane = new JScrollPane( component,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		scrollPane.setOpaque( false );
		scrollPane.getViewport().setOpaque( false );
		scrollPane.setBorder( null );
		return scrollPane;
	}
}
