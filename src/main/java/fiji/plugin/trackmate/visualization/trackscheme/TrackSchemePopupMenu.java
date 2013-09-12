package fiji.plugin.trackmate.visualization.trackscheme;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.EventObject;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;

import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;

public class TrackSchemePopupMenu extends JPopupMenu {

	private static final long serialVersionUID = -1L;

	/**
	 * The cell where the right-click was made, <code>null</code> if the
	 * right-click is made out of a cell.
	 */
	private final Object cell;
	/** The TrackScheme instance. */
	private final TrackScheme trackScheme;
	/** The right-click location. */
	private final Point point;

	public TrackSchemePopupMenu(final TrackScheme trackScheme, final Object cell, final Point point) {
		this.trackScheme = trackScheme;
		this.cell = cell;
		this.point = point;
		init();
	}

	/*
	 * ACTIONS
	 */

	private void selectWholeTrack(final ArrayList<mxCell> vertices, final ArrayList<mxCell> edges) {
		trackScheme.selectTrack(vertices, edges, 0);
	}

	private void selectTrackDownwards(final ArrayList<mxCell> vertices, final ArrayList<mxCell> edges) {
		trackScheme.selectTrack(vertices, edges, -1);
	}

	private void selectTrackUpwards(final ArrayList<mxCell> vertices, final ArrayList<mxCell> edges) {
		trackScheme.selectTrack(vertices, edges, 1);
	}

	private void editSpotName() {
		trackScheme.getGUI().graphComponent.startEditingAtCell(cell);
	}

	@SuppressWarnings("unused")
	private void toggleBranchFolding() {
		Object parent;
		if (trackScheme.getGraph().isCellFoldable(cell, true))
			parent = cell;
		else
			parent = trackScheme.getGraph().getModel().getParent(cell);
		trackScheme.getGraph().foldCells(!trackScheme.getGraph().isCellCollapsed(parent), false, new Object[] { parent });
	}

	private void multiEditSpotName(final ArrayList<mxCell> vertices, final EventObject triggerEvent) {
		/*
		 * We want to display the editing window in the cell that is the closer
		 * to where the user clicked. That is not perfect, because we can
		 * imagine the click is made for from the selected cells, and that the
		 * editing window will not even be displayed on the screen. No idea for
		 * that yet, because JGraphX is expecting to receive a cell as location
		 * for the editing window.
		 */
		final mxCell tc = getClosestCell(vertices);
		vertices.remove(tc);
		final mxGraphComponent graphComponent = trackScheme.getGUI().graphComponent;
		graphComponent.startEditingAtCell(tc, triggerEvent);
		graphComponent.addListener(mxEvent.LABEL_CHANGED, new mxIEventListener() {

			@Override
			public void invoke(final Object sender, final mxEventObject evt) {
				for (final mxCell cell : vertices) {
					cell.setValue(tc.getValue());
					trackScheme.getGraph().getSpotFor(cell).setName(tc.getValue().toString());
				}
				graphComponent.refresh();
				graphComponent.removeListener(this);
			}
		});
	}

	/**
	 * Return, from the given list of cell, the one which is the closer to the
	 * {@link #point} of this instance.
	 */
	private mxCell getClosestCell(final Iterable<mxCell> vertices) {
		double min_dist = Double.POSITIVE_INFINITY;
		mxCell target_cell = null;
		for (final mxCell cell : vertices) {
			final Point location = cell.getGeometry().getPoint();
			final double dist = location.distanceSq(point);
			if (dist < min_dist) {
				min_dist = dist;
				target_cell = cell;
			}
		}
		return target_cell;
	}

	private void linkSpots() {
		trackScheme.linkSpots();
	}

	private void remove() {
		trackScheme.removeSelectedCells();
	}

	/*
	 * MENU COMPOSITION
	 */

	@SuppressWarnings("serial")
	private void init() {

		// Build selection categories
		final Object[] selection = trackScheme.getGraph().getSelectionCells();
		final ArrayList<mxCell> vertices = new ArrayList<mxCell>();
		final ArrayList<mxCell> edges = new ArrayList<mxCell>();
		for (final Object obj : selection) {
			final mxCell cell = (mxCell) obj;
			if (cell.isVertex())
				vertices.add(cell);
			else if (cell.isEdge())
				edges.add(cell);
		}

		// Select whole tracks
		if (vertices.size() > 0 || edges.size() > 0) {

			add(new AbstractAction("Select whole track") {
				@Override
				public void actionPerformed(final ActionEvent e) {
					selectWholeTrack(vertices, edges);
				}
			});

			add(new AbstractAction("Select track downwards") {
				@Override
				public void actionPerformed(final ActionEvent e) {
					selectTrackDownwards(vertices, edges);
				}
			});

			add(new AbstractAction("Select track upwards") {
				@Override
				public void actionPerformed(final ActionEvent e) {
					selectTrackUpwards(vertices, edges);
				}
			});

		}

		if (cell != null) {
			// Edit
			add(new AbstractAction("Edit spot name") {
				@Override
				public void actionPerformed(final ActionEvent e) {
					editSpotName();
				}
			});

		} else {

			if (vertices.size() > 1) {

				// Multi edit
				add(new AbstractAction("Edit " + vertices.size() + " spot names") {
					@Override
					public void actionPerformed(final ActionEvent e) {
						multiEditSpotName(vertices, e);
					}
				});
			}

			// Link
			final Action linkAction = new AbstractAction("Link " + trackScheme.getSelectionModel().getSpotSelection().size() + " spots") {
				@Override
				public void actionPerformed(final ActionEvent e) {
					linkSpots();
				}
			};
			if (trackScheme.getSelectionModel().getSpotSelection().size() > 1) {
				add(linkAction);
			}
		}

		// Remove
		if (selection.length > 0) {
			final Action removeAction = new AbstractAction("Remove spots and links") {
				@Override
				public void actionPerformed(final ActionEvent e) {
					remove();
				}
			};
			add(removeAction);
		}
	}

}
