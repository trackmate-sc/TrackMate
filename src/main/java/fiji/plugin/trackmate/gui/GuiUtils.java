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
