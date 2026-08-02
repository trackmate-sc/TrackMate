package fiji.plugin.trackmate.visualization.trackscheme.behaviours;

import org.scijava.ui.behaviour.util.Actions;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeGraphComponent;

public class TrackSchemeActions
{

	/**
	 * When panning with the keyboard, by how much pixels to move.
	 */
	private static final int PAN_AMOUNT = 100;

	static final String EDIT_NAME = "edit name";
	private static final String[] EDIT_NAME_KEYS = new String[] { "F2" };
	
	private static final String ZOOM_IN = "zoom in";
	private static final String[] ZOOM_IN_KEYS = new String[] { "ADD", "EQUALS" };
	private static final String ZOOM_OUT = "zoom out";
	private static final String[] ZOOM_OUT_KEYS = new String[] { "SUBTRACT", "MINUS" };
	private static final String RESET_ZOOM = "reset zoom";
	private static final String[] RESET_ZOOM_KEYS = new String[] { "shift EQUALS", "R" };
	
	static final String CENTER_ON_FIRST_SELECTED = "center on first selected";
	private static final String[] CENTER_ON_FIRST_SELECTED_KEYS = new String[] { "HOME", "C" };
	static final String CENTER_ON_LAST_SELECTED = "center on last selected";
	private static final String[] CENTER_ON_LAST_SELECTED_KEYS = new String[] { "END", "shift C" };
	
	private static final String PAN_LEFT = "pan left";
	private static final String[] PAN_LEFT_KEYS = new String[] { "NUMPAD4" };
	private static final String PAN_RIGHT = "pan right";
	private static final String[] PAN_RIGHT_KEYS = new String[] { "NUMPAD6" };
	private static final String PAN_UP = "pan up";
	private static final String[] PAN_UP_KEYS = new String[] { "NUMPAD8" };
	private static final String PAN_DOWN = "pan down";
	private static final String[] PAN_DOWN_KEYS = new String[] { "NUMPAD2" };
	private static final String PAN_UP_RIGHT = "pan up right";
	private static final String[] PAN_UP_RIGHT_KEYS = new String[] { "NUMPAD9" };
	private static final String PAN_DOWN_RIGHT = "pan down right";
	private static final String[] PAN_DOWN_RIGHT_KEYS = new String[] { "NUMPAD3" };
	private static final String PAN_DOWN_LEFT = "pan down left";
	private static final String[] PAN_DOWN_LEFT_KEYS = new String[] { "NUMPAD1"	};
	private static final String PAN_UP_LEFT = "pan up left";
	private static final String[] PAN_UP_LEFT_KEYS = new String[] { "NUMPAD7" };
	
	public static final void install( final Actions actions, final Model model, final TrackSchemeGraphComponent graphComponent )
	{
		actions.namedAction( new EditNameAction( model, graphComponent ), EDIT_NAME_KEYS );

		actions.namedAction( new HomingActions.HomeAction( graphComponent ), CENTER_ON_FIRST_SELECTED_KEYS );
		actions.namedAction( new HomingActions.EndAction( graphComponent ), CENTER_ON_LAST_SELECTED_KEYS );

		actions.runnableAction( () -> graphComponent.zoomIn(), ZOOM_IN, ZOOM_IN_KEYS );
		actions.runnableAction( () -> graphComponent.zoomOut(), ZOOM_OUT, ZOOM_OUT_KEYS );
		actions.runnableAction( () -> graphComponent.zoomActual(), RESET_ZOOM, RESET_ZOOM_KEYS );

		actions.namedAction( new PanAction( PAN_LEFT, -PAN_AMOUNT, 0, graphComponent ), PAN_LEFT_KEYS );
		actions.namedAction( new PanAction( PAN_RIGHT, PAN_AMOUNT, 0, graphComponent ), PAN_RIGHT_KEYS );
		actions.namedAction( new PanAction( PAN_UP, 0, -PAN_AMOUNT, graphComponent ), PAN_UP_KEYS );
		actions.namedAction( new PanAction( PAN_DOWN, 0, PAN_AMOUNT, graphComponent ), PAN_DOWN_KEYS );
		actions.namedAction( new PanAction( PAN_UP_RIGHT, PAN_AMOUNT, -PAN_AMOUNT, graphComponent ), PAN_UP_RIGHT_KEYS );
		actions.namedAction( new PanAction( PAN_DOWN_RIGHT, PAN_AMOUNT, PAN_AMOUNT, graphComponent ), PAN_DOWN_RIGHT_KEYS );
		actions.namedAction( new PanAction( PAN_DOWN_LEFT, -PAN_AMOUNT, PAN_AMOUNT, graphComponent ), PAN_DOWN_LEFT_KEYS );
		actions.namedAction( new PanAction( PAN_UP_LEFT, -PAN_AMOUNT, -PAN_AMOUNT, graphComponent ), PAN_UP_LEFT_KEYS );
	}
}
