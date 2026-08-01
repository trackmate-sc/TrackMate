package fiji.plugin.trackmate.visualization.hyperstack.behaviours;

import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
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

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import org.scijava.ui.behaviour.BehaviourMap;
import org.scijava.ui.behaviour.GlobalKeyEventDispatcher;
import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.behaviour.InputTriggerMap;
import org.scijava.ui.behaviour.MouseAndKeyHandler;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.ui.keymap.Keymap;
import bdv.ui.keymap.KeymapManager;
import gnu.trove.set.TIntSet;
import ij.ImagePlus;
import ij.gui.ImageCanvas;

/**
 * Adapter class that connects a {@link ImagePlus} to the SciJava Behaviours
 * framework, and provides access to the {@link Actions} and {@link Behaviours}
 * objects.
 * <p>
 * The trick is that the scijava ui-behaviour framework is designed for Swing
 * components, and the ImagePlus canvas is an AWT component. This class bridges
 * the gap by using a proxy to intercept mouse and key events and route them to
 * the appropriate actions and behaviours.
 * <p>
 * The actions and behaviours are positioned 'before' the original ImageJ key
 * listener, so that the key bindings defined in the keymap override the default
 * ImageJ key bindings. However, if a key binding exists for ImageJ but not for
 * the actions and behaviours created here, the event will be passed to the
 * original ImageJ key listener.
 *
 * @author Jean-Yves Tinevez
 */
public class ImagePlusBehavioursAdapter
{

	private final Actions actions;

	private final Behaviours behaviours;

	public ImagePlusBehavioursAdapter( final ImagePlus imp, final KeymapManager keymapManager, final String[] keyConfigContexts )
	{
		final ImageCanvas canvas = imp.getCanvas();
		canvas.setFocusable( true );

		// Initialize configuration and binding registries
		final InputActionBindings actionBindings = new InputActionBindings();
		final TriggerBehaviourBindings behaviourBindings = new TriggerBehaviourBindings();
		keymapManager.discoverCommandDescriptions();
		final InputTriggerConfig config = keymapManager.getForwardSelectedKeymap().getConfig();

		// Behaviours
		this.behaviours = new Behaviours( config, keyConfigContexts );
		behaviours.install( behaviourBindings, keyConfigContexts[ 0 ] + "-behaviours" );

		// Actions
		final InputMap inputMap = actionBindings.getConcatenatedInputMap();
		final ActionMap actionMap = actionBindings.getConcatenatedActionMap();
		this.actions = new Actions( config, keyConfigContexts );
		actions.install( actionBindings, keyConfigContexts[ 0 ] + "-actions" );

		final Keymap keymap = keymapManager.getForwardSelectedKeymap();
		keymap.updateListeners().add( () -> {
			actions.updateKeyConfig( keymap.getConfig() );
			behaviours.updateKeyConfig( keymap.getConfig() );
		} );
		actions.updateKeyConfig( keymap.getConfig() );
		behaviours.updateKeyConfig( keymap.getConfig() );

		// Initialize the handler
		final MouseAndKeyHandler handler = new MouseAndKeyHandler();
		final InputTriggerMap inputTriggerMap = behaviours.getInputTriggerMap();
		final BehaviourMap behaviourMap = behaviours.getBehaviourMap();
		handler.setInputMap( inputTriggerMap );
		handler.setBehaviourMap( behaviourMap );

		// Put the IJ key listener at the end of the chain, so that we can
		// intercept events before they reach it.
		final KeyListener[] keyListeners = canvas.getKeyListeners();
		for ( final KeyListener keyListener : keyListeners )
			canvas.removeKeyListener( keyListener );

		/*
		 * Connect the handler to the AWT component for Mouse handling We need
		 * the proxy because the default MouseAndKeyHandler does not consume
		 * events when a trigger is matched, and without it the events are sent
		 * to the ImageJ original KeyListener.
		 */
		final MouseEventProxy proxy = new MouseEventProxy( handler, inputTriggerMap );
		canvas.addKeyListener( proxy );
		canvas.addMouseListener( proxy );
		canvas.addMouseMotionListener( proxy );
		canvas.addMouseWheelListener( proxy );

		/*
		 * Direct Key Event Proxy Bridge. Because an AWT Canvas bypasses Swing's
		 * ActionMap dispatch pipeline, we manually intercept the KeyStrokes and
		 * route them to our Action map.
		 */
		canvas.addKeyListener( new KeyAdapter()
		{
			@Override
			public void keyPressed( final KeyEvent e )
			{
				// Get the keystroke matching this precise key event
				final KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent( e );
				if ( keyStroke == null )
					return;

				// Lookup the unique Action ID string assigned to this key combo
				final Object actionKey = inputMap.get( keyStroke );
				if ( actionKey != null )
				{
					// Pull the runnable action associated with that ID
					final Action action = actionMap.get( actionKey );
					if ( action != null && action.isEnabled() )
					{
						// Fire the shortcut action directly on the EDT
						action.actionPerformed( null );
						e.consume(); // Block from propagating to IJ listener
					}
				}
			}
		} );

		// Re-add the original ImageJ KeyListener after all proxies
		for ( final KeyListener keyListener : keyListeners )
			canvas.addKeyListener( keyListener );
	}

	public Actions actions()
	{
		return actions;
	}

	public Behaviours behaviours()
	{
		return behaviours;
	}

	/**
	 * A proxy that intercepts mouse and key events, and routes them to the
	 * appropriate actions and behaviours. It also consumes the events if they
	 * match a trigger, preventing them from reaching the original ImageJ key
	 * listener.
	 */
	private static class MouseEventProxy implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener
	{
		private final MouseAndKeyHandler delegate;

		private final InputTriggerMap triggerMap;

		private Method getMaskMethod;

		/**
		 * Constructs a new MouseEventProxy.
		 * 
		 * @param delegate
		 *            The active MouseAndKeyHandler driving the interaction
		 *            updates.
		 * @param triggerMap
		 *            The mapping table containing target behavioral shortcuts.
		 */
		public MouseEventProxy( final MouseAndKeyHandler delegate, final InputTriggerMap triggerMap )
		{
			this.delegate = delegate;
			this.triggerMap = triggerMap;
			try
			{
				// Extract ui-behaviour's internal structural input mask
				// calculator via reflection
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
		 * behavioral binding without causing NullPointerExceptions in child
		 * trigger maps.
		 */
		private boolean hasMatchingBehaviour( final InputEvent e )
		{
			if ( getMaskMethod == null || triggerMap == null )
				return false;

			try
			{
				// Retrieve the calculated normalization bitmask from the
				// handler
				// instance
				final int mask = ( Integer ) getMaskMethod.invoke( delegate, e );

				// Keep the primitive TIntSet collection directly without
				// calling .toArray()
				final TIntSet pressedKeys = GlobalKeyEventDispatcher.getInstance().pressedKeys();

				// Safely pull the flattened trigger assignments map from our
				// configuration
				final Map< InputTrigger, Set< String > > bindings = triggerMap.getAllBindings();

				if ( bindings != null )
				{
					for ( final Map.Entry< InputTrigger, Set< String > > entry : bindings.entrySet() )
					{
						final InputTrigger trigger = entry.getKey();
						final Set< String > keys = entry.getValue();

						// Check if trigger conditions match and that it
						// contains actual bound behaviors
						if ( trigger != null && trigger.matches( mask, pressedKeys ) && keys != null && !keys.isEmpty() )
							return true;
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
			// Hover coordinates pass straight through unconsumed to ensure
			// legacy UI refreshes continue
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
}
