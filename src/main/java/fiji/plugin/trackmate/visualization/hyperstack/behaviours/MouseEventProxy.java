package fiji.plugin.trackmate.visualization.hyperstack.behaviours;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import org.scijava.ui.behaviour.GlobalKeyEventDispatcher;
import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.behaviour.InputTriggerMap;
import org.scijava.ui.behaviour.MouseAndKeyHandler;

/**
 * A proxy wrapper for SciJava's {@link MouseAndKeyHandler} that intercepts AWT
 * mouse event pipelines and selectively calls {@code e.consume()} only if an
 * explicit behavior trigger mapping exists inside the configuration model.
 * <p>
 * We need this because the default {@link MouseAndKeyHandler} does not consume
 * events when a trigger is matched, which can lead to unintended propagation of
 * events to other components in the UI.
 */
public class MouseEventProxy implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener
{
	private final MouseAndKeyHandler delegate;

	private final InputTriggerMap triggerMap;

	private Method getMaskMethod;

	/**
	 * Constructs a new MouseEventProxy.
	 * 
	 * @param delegate
	 *            The active MouseAndKeyHandler driving the interaction updates.
	 * @param triggerMap
	 *            The mapping table containing target behavioral shortcuts.
	 */
	public MouseEventProxy( final MouseAndKeyHandler delegate, final InputTriggerMap triggerMap )
	{
		this.delegate = delegate;
		this.triggerMap = triggerMap;
		try
		{
			// Extract ui-behaviour's internal structural input mask calculator
			// via reflection
			this.getMaskMethod = MouseAndKeyHandler.class.getDeclaredMethod( "getMask", InputEvent.class );
			this.getMaskMethod.setAccessible( true );
		}
		catch ( final Exception ex )
		{
			this.getMaskMethod = null;
		}
	}

	/**
	 * Safely determines if the active hardware interaction features a
	 * behavioral binding without causing NullPointerExceptions in child trigger
	 * maps.
	 */
	private boolean hasMatchingBehaviour( final InputEvent e )
	{
		if ( getMaskMethod == null || triggerMap == null )
			return false;

		try
		{
			// Retrieve the calculated normalization bitmask from the handler
			// instance
			final int mask = ( Integer ) getMaskMethod.invoke( delegate, e );

			// FIX: Keep the primitive TIntSet collection directly without
			// calling .toArray()
			final gnu.trove.set.TIntSet pressedKeys = GlobalKeyEventDispatcher.getInstance().pressedKeys();

			// Safely pull the flattened trigger assignments map from our
			// configuration
			final Map< InputTrigger, Set< String > > bindings = triggerMap.getAllBindings();

			if ( bindings != null )
			{
				for ( final Map.Entry< InputTrigger, Set< String > > entry : bindings.entrySet() )
				{
					final InputTrigger trigger = entry.getKey();
					final Set< String > keys = entry.getValue();

					// Check if trigger conditions match and that it contains
					// actual bound behaviors
					if ( trigger != null && trigger.matches( mask, pressedKeys ) && keys != null && !keys.isEmpty() )
					{ return true; }
				}
			}
		}
		catch ( final Exception ex )
		{
			// Prevent crashes from faulty reflections, letting the event
			// propagate unconsumed
		}
		return false;
	}

	@Override
	public void mouseClicked( final MouseEvent e )
	{
		final boolean shouldConsume = hasMatchingBehaviour( e );
		delegate.mouseClicked( e );
		if ( shouldConsume )
			e.consume();
	}

	@Override
	public void mousePressed( final MouseEvent e )
	{
		final boolean shouldConsume = hasMatchingBehaviour( e );
		delegate.mousePressed( e );
		if ( shouldConsume )
			e.consume();
	}

	@Override
	public void mouseReleased( final MouseEvent e )
	{
		final boolean shouldConsume = hasMatchingBehaviour( e );
		delegate.mouseReleased( e );
		if ( shouldConsume )
			e.consume();
	}

	@Override
	public void mouseEntered( final MouseEvent e )
	{
		delegate.mouseEntered( e );
	}

	@Override
	public void mouseExited( final MouseEvent e )
	{
		delegate.mouseExited( e );
	}

	@Override
	public void mouseDragged( final MouseEvent e )
	{
		final boolean shouldConsume = hasMatchingBehaviour( e );
		delegate.mouseDragged( e );
		if ( shouldConsume )
			e.consume();
	}

	@Override
	public void mouseMoved( final MouseEvent e )
	{
		// Hover coordinates pass straight through unconsumed to ensure legacy
		// UI refreshes continue
		delegate.mouseMoved( e );
	}

	@Override
	public void mouseWheelMoved( final MouseWheelEvent e )
	{
		final boolean shouldConsume = hasMatchingBehaviour( e );
		delegate.mouseWheelMoved( e );
		if ( shouldConsume )
			e.consume();
	}

	@Override
	public void keyPressed( final KeyEvent e )
	{
		final boolean shouldConsume = hasMatchingBehaviour( e );
		delegate.keyPressed( e );
		if ( shouldConsume )
			e.consume();
	}

	@Override
	public void keyReleased( final KeyEvent e )
	{
		final boolean shouldConsume = hasMatchingBehaviour( e );
		delegate.keyReleased( e );
		if ( shouldConsume )
			e.consume();
	}

	@Override
	public void keyTyped( final KeyEvent e )
	{
		final boolean shouldConsume = hasMatchingBehaviour( e );
		delegate.keyTyped( e );
		if ( shouldConsume )
			e.consume();
	}
}
