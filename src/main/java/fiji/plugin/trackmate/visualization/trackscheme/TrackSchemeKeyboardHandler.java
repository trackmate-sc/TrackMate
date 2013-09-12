package fiji.plugin.trackmate.visualization.trackscheme;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.handler.mxKeyboardHandler;
import com.mxgraph.swing.util.mxGraphActions;

public class TrackSchemeKeyboardHandler extends mxKeyboardHandler {

	public TrackSchemeKeyboardHandler(final mxGraphComponent graphComponent) {
		super(graphComponent);
	}

	@Override
	protected InputMap getInputMap(final int condition) {
		InputMap map = null;

		if (condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) {
			map = (InputMap) UIManager.get("ScrollPane.ancestorInputMap");
		} else if (condition == JComponent.WHEN_FOCUSED) {
			map = new InputMap();
		}

		map.put(KeyStroke.getKeyStroke("F2"), "edit");
		map.put(KeyStroke.getKeyStroke("DELETE"), "delete");

		map.put(KeyStroke.getKeyStroke("HOME"), "home");
		map.put(KeyStroke.getKeyStroke("END"), "end");

		map.put(KeyStroke.getKeyStroke("ADD"), "zoomIn");
		map.put(KeyStroke.getKeyStroke("EQUALS"), "zoomIn");
		map.put(KeyStroke.getKeyStroke("EQUALS"), "zoomIn");
		map.put(KeyStroke.getKeyStroke("SUBTRACT"), "zoomOut");
		map.put(KeyStroke.getKeyStroke("MINUS"), "zoomOut");
		map.put(KeyStroke.getKeyStroke("shift EQUALS"), "resetZoom");

		map.put(KeyStroke.getKeyStroke("control A"), "selectAll");
		map.put(KeyStroke.getKeyStroke("control shift A"), "selectNone");

		return map;
	}

	/**
	 * Return the mapping between JTree's input map and JGraph's actions.
	 */
	@Override
	protected ActionMap createActionMap() {
		final ActionMap map = (ActionMap) UIManager.get("ScrollPane.actionMap");

		map.put("edit", TrackSchemeActions.getEditAction());
		map.put("delete", mxGraphActions.getDeleteAction());

		map.put("home", TrackSchemeActions.getHomeAction());
		map.put("end", TrackSchemeActions.getEndAction());

		map.put("zoomIn", TrackSchemeActions.getZoomInAction());
		map.put("zoomOut", TrackSchemeActions.getZoomOutAction());
		map.put("resetZoom", TrackSchemeActions.getResetZoomAction());

		map.put("selectNone", TrackSchemeActions.getSelectNoneAction());
		map.put("selectAll", TrackSchemeActions.getSelectAllAction());

		return map;
	}
}
