package fiji.plugin.trackmate.visualization.hyperstack.behaviours;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import org.scijava.ui.behaviour.BehaviourMap;
import org.scijava.ui.behaviour.InputTriggerMap;
import org.scijava.ui.behaviour.MouseAndKeyHandler;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import ij.ImagePlus;
import ij.gui.ImageCanvas;

public class TrackMateImpBehaviour
{

	/**
	 * Attaches ui-behaviour interaction handling to a given {@link ImagePlus}.
	 * 
	 * @param model
	 *            the model to operate on.
	 * @param selectionModel
	 *            the selection model to read what is selected and to update the
	 *            selection.
	 * @param imp
	 *            the ImagePlus to attach the behaviours to.
	 */
	public static void install( final Model model, final SelectionModel selectionModel, final ImagePlus imp )
	{
		// 1. Ensure the canvas can accept focus for keyboard shortcuts
		final ImageCanvas canvas = imp.getCanvas();
		canvas.setFocusable( true );

		// A. Behaviours framework

		// Initialize configuration and binding registries
		final InputTriggerConfig config = new InputTriggerConfig();
		final InputActionBindings actionBindings = new InputActionBindings();
		final TriggerBehaviourBindings behaviourBindings = new TriggerBehaviourBindings();

		// Initialize the Behaviours framework
		final MouseAndKeyHandler handler = new MouseAndKeyHandler();
		final InputTriggerMap inputTriggerMap = new InputTriggerMap();
		final BehaviourMap behaviourMap = new BehaviourMap();
		handler.setInputMap( inputTriggerMap );
		handler.setBehaviourMap( behaviourMap );

		// Add the TrackMate listener first.
		final KeyListener[] keyListeners = canvas.getKeyListeners();
		final KeyListener ijKeyListener = keyListeners[ 0 ];
		canvas.removeKeyListener( ijKeyListener );

		// Connect the handler to the AWT component for Mouse handling
		// We need the proxy because the default MouseAndKeyHandler does not
		// consume events when a trigger is matched, and without it the events
		// are sent to the ImageJ original KeyListener.
		final MouseEventProxy proxy = new MouseEventProxy( handler, inputTriggerMap );
		canvas.addKeyListener( proxy );
		canvas.addMouseListener( proxy );
		canvas.addMouseMotionListener( proxy );
		canvas.addMouseWheelListener( proxy );

		// The behaviours.
		final Behaviours behaviours = new Behaviours( inputTriggerMap, behaviourMap, config );
		behaviours.install( behaviourBindings, "trackmate-beaviors" );

		// Actions
		final InputMap inputMap = actionBindings.getConcatenatedInputMap();
		final ActionMap actionMap = actionBindings.getConcatenatedActionMap();
		final Actions actions = new Actions( inputMap, actionMap, config );
		actions.install( actionBindings, "trackmate-actions" );

		// This is the debug
		actions.runnableAction( () -> {
			System.out.println( "Reset action triggered!" );
		}, "reset-view", "R" );

		// Direct Key Event Proxy Bridge. This was done with Gemini.
		// Because an AWT Canvas bypasses Swing's ActionMap dispatch pipeline,
		// we manually intercept the KeyStrokes and route them to our Action
		// map.
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
		canvas.addKeyListener( ijKeyListener );

		/*
		 * Flesh out commands.
		 */

		SpotEditBehaviours.install( behaviours, model, selectionModel, imp );
		SpotEditActions.install( actions, model, selectionModel, imp );
	}
}
