package fiji.plugin.trackmate.visualization;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.scijava.ui.behaviour.MouseAndKeyHandler;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;
import org.scijava.ui.behaviour.util.WrappedActionMap;
import org.scijava.ui.behaviour.util.WrappedInputMap;

import bdv.ui.keymap.Keymap;
import bdv.ui.keymap.Keymap.UpdateListener;
import bvv.core.VolumeViewerFrame;
import fiji.plugin.trackmate.gui.GuiModel;
import fiji.plugin.trackmate.visualization.ui.KeyConfigContexts;

public abstract class AbstractTrackMateModelBvvView extends AbstractTrackMateModelView
{

	protected final Behaviours behaviours;

	protected final Actions actions;

	protected AbstractTrackMateModelBvvView( final GuiModel guiModel, final String... keyConfigContexts )
	{
		super( guiModel );
		final Set< String > c = new LinkedHashSet<>( Arrays.asList( KeyConfigContexts.TRACKMATE ) );
		c.addAll( Arrays.asList( keyConfigContexts ) );
		final String[] kccs = c.toArray( new String[] {} );
		final Keymap keymap = guiModel.getKeymapManager().getForwardSelectedKeymap();
		this.behaviours = new Behaviours( keymap.getConfig(), kccs );
		this.actions = new Actions( keymap.getConfig(), kccs );
	}

	protected void setWindow( final VolumeViewerFrame viewerFrame )
	{
		// We add our actions and behaviours to the following object, so that
		// they can override those defined in the BVV.
		final InputActionBindings keybindings = viewerFrame.getKeybindings();
		final TriggerBehaviourBindings triggerbindings = viewerFrame.getTriggerbindings();
		actions.install( keybindings, "view2" );
		behaviours.install( triggerbindings, "view2" );

		final Keymap keymap = guiModel.getKeymapManager().getForwardSelectedKeymap();
		final UpdateListener updateListener = () -> {
			behaviours.updateKeyConfig( keymap.getConfig() );
			actions.updateKeyConfig( keymap.getConfig() );
		};
		keymap.updateListeners().add( updateListener );
		onClose( () -> keymap.updateListeners().remove( updateListener ) );

		final MouseAndKeyHandler mouseAndKeyHandler = new MouseAndKeyHandler();
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
}
