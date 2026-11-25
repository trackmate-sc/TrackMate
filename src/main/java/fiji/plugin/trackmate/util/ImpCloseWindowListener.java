
package fiji.plugin.trackmate.util;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.function.BooleanSupplier;

import ij.gui.ImageWindow;

/**
 * Intercepts the ImageJ window closing event and prevents the image from being
 * closed when the user clicks on the close button. Then asks for confirmation.
 */
public class ImpCloseWindowListener implements WindowListener
{
	
	/**
	 * Adds a listener to the given ImageWindow that intercepts the close event
	 * and asks for confirmation before closing the window. If the user
	 * confirms, the window is closed and the onClosed runnable is executed.
	 * 
	 * @param window
	 *            the ImageWindow to wrap.
	 * @param confirmClose
	 *            a BooleanSupplier that returns true if the window should be
	 *            closed, false otherwise. It can be build from a JOptionPane or
	 *            any other confirmation dialog.
	 * @param onClosed
	 *            what to do when the window is closed. Can be null.
	 */
	public static void wrap( final ImageWindow window, final BooleanSupplier confirmClose, final Runnable onClosed )
	{
		new ImpCloseWindowListener( window, confirmClose, onClosed );
	}

	private final WindowListener nativeListener;

	private final Runnable onClosed;

	private final BooleanSupplier confirmClose;

	private ImpCloseWindowListener(
			final ImageWindow win,
			final BooleanSupplier confirmClose,
			final Runnable onClosed )
	{
		this.confirmClose = confirmClose;
		this.onClosed = onClosed;
		// Identify and remove IJ close listener
		WindowListener tmp = null;
		final WindowListener[] listeners = win.getWindowListeners();
		for ( final WindowListener listener : listeners )
		{
			if ( ImageWindow.class.isAssignableFrom( listener.getClass() ) )
			{
				tmp = listener;
				win.removeWindowListener( tmp );
				break;
			}
		}
		if ( tmp == null )
			throw new IllegalStateException( "Could not find native ImageWindow listener." );
		this.nativeListener = tmp;
		win.addWindowListener( this );
	}

	@Override
	public void windowOpened( final WindowEvent e )
	{
		nativeListener.windowOpened( e );
	}

	@Override
	public void windowClosing( final WindowEvent e )
	{
		if ( confirmClose.getAsBoolean() )
			nativeListener.windowClosing( e );
	}

	@Override
	public void windowClosed( final WindowEvent e )
	{
		nativeListener.windowClosed( e );
		if ( onClosed != null )
			onClosed.run();
	}

	@Override
	public void windowIconified( final WindowEvent e )
	{
		nativeListener.windowIconified( e );
	}

	@Override
	public void windowDeiconified( final WindowEvent e )
	{
		nativeListener.windowDeiconified( e );
	}

	@Override
	public void windowActivated( final WindowEvent e )
	{
		nativeListener.windowActivated( e );
	}

	@Override
	public void windowDeactivated( final WindowEvent e )
	{
		nativeListener.windowDeactivated( e );
	}
}
