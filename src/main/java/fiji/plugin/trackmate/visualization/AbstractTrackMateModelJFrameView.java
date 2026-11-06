/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
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
package fiji.plugin.trackmate.visualization;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.scijava.ui.behaviour.MouseAndKeyHandler;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;
import org.scijava.ui.behaviour.util.WrappedActionMap;
import org.scijava.ui.behaviour.util.WrappedInputMap;

import bdv.ui.keymap.Keymap;
import bdv.ui.keymap.Keymap.UpdateListener;
import fiji.plugin.trackmate.gui.GuiModel;
import fiji.plugin.trackmate.visualization.ui.KeyConfigContexts;

/**
 * An abstract class for TrackMate views that display content in a
 * {@link JFrame}.
 *
 * @author Jean-Yves Tinevez
 */
public abstract class AbstractTrackMateModelJFrameView extends AbstractTrackMateModelView
{

	private final InputActionBindings keybindings;

	private final TriggerBehaviourBindings triggerbindings;

	private final MouseAndKeyHandler mouseAndKeyHandler;

	protected final Actions actions;

	protected final Behaviours behaviours;

	protected AbstractTrackMateModelJFrameView( final GuiModel guiModel, final String... keyConfigContexts )
	{
		super( guiModel );
		final Set< String > c = new LinkedHashSet<>( Arrays.asList( KeyConfigContexts.TRACKMATE ) );
		c.addAll( Arrays.asList( keyConfigContexts ) );
		final String[] kccs = c.toArray( new String[] {} );

		this.keybindings = new InputActionBindings();
		this.triggerbindings = new TriggerBehaviourBindings();

		final Keymap keymap = guiModel.getKeymapManager().getForwardSelectedKeymap();

		this.actions = new Actions( keymap.getConfig(), kccs );
		actions.install( keybindings, "view" );

		this.behaviours = new Behaviours( keymap.getConfig(), kccs );
		behaviours.install( triggerbindings, "view" );

		final UpdateListener updateListener = () -> {
			behaviours.updateKeyConfig( keymap.getConfig() );
			actions.updateKeyConfig( keymap.getConfig() );
		};
		keymap.updateListeners().add( updateListener );
		onClose( () -> keymap.updateListeners().remove( updateListener ) );

		this.mouseAndKeyHandler = new MouseAndKeyHandler();
		mouseAndKeyHandler.setInputMap( triggerbindings.getConcatenatedInputTriggerMap() );
		mouseAndKeyHandler.setBehaviourMap( triggerbindings.getConcatenatedBehaviourMap() );

		// Register global actions, if any.
		final Actions globalActions = guiModel.getGlobalActions();
		if ( globalActions != null )
		{
			keybindings.addActionMap( "global", new WrappedActionMap( globalActions.getActionMap() ) );
			keybindings.addInputMap( "global", new WrappedInputMap( globalActions.getInputMap() ) );
		}
	}

	protected void setWindow( final JFrame frame )
	{
		frame.addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				close();
			}
		} );
		attachKeybindings( frame.getRootPane() );
	}

	private void attachKeybindings( final JComponent component )
	{
		SwingUtilities.replaceUIActionMap( component, keybindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap( component, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );
		mouseAndKeyHandler.setKeypressManager( guiModel.getKeyPressedManager(), component );
		component.addKeyListener( mouseAndKeyHandler );
		component.addMouseListener( mouseAndKeyHandler );
		component.addMouseMotionListener( mouseAndKeyHandler );
		component.addMouseWheelListener( mouseAndKeyHandler );
		component.addFocusListener( mouseAndKeyHandler );
	}
}
