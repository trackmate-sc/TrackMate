package fiji.plugin.trackmate.visualization.hyperstack.behaviours;

import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.util.ArrayList;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.ui.behaviour.util.Actions;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.TrackNavigator;
import ij.ImagePlus;

public class SpotEditActions
{

	private static final String UNDO_ACTION = "undo";
	private static final String REDO_ACTION = "redo";

	private static final String[] UNDO_ACTION_KEYS;
	private static final String[] REDO_ACTION_KEYS;

	private static final String NAVIGATE_TO_PARENT = "navigate to parent";
	private static final String NAVIGATE_TO_CHILD = "navigate to child";
	private static final String NAVIGATE_TO_PREVIOUS_SIBLING = "navigate to previous sibling";
	private static final String NAVIGATE_TO_NEXT_SIBLING = "navigate to next sibling";
	private static final String NAVIGATE_TO_ROOT = "navigate to root";
	private static final String NAVIGATE_TO_LEAF = "navigate to leaf";
	private static final String NAVIGATE_TO_PREVIOUS_TRACK = "navigate to previous track";
	private static final String NAVIGATE_TO_NEXT_TRACK = "navigate to next track";

	private static final String[] NAVIGATE_TO_PARENT_KEYS = new String[] { "UP" };
	private static final String[] NAVIGATE_TO_CHILD_KEYS = new String[] { "DOWN" };
	private static final String[] NAVIGATE_TO_PREVIOUS_SIBLING_KEYS = new String[] { "LEFT" };
	private static final String[] NAVIGATE_TO_NEXT_SIBLING_KEYS = new String[] { "RIGHT" };
	private static final String[] NAVIGATE_TO_ROOT_KEYS = new String[] { "HOME", "meta UP" };
	private static final String[] NAVIGATE_TO_LEAF_KEYS = new String[] { "END", "meta DOWN" };
	private static final String[] NAVIGATE_TO_PREVIOUS_TRACK_KEYS = new String[] { "PAGE_UP" };
	private static final String[] NAVIGATE_TO_NEXT_TRACK_KEYS = new String[] { "PAGE_DOWN" };

	private static final String DELETE_SELECTED_SPOTS = "delete selected spots";
	private static final String[] DELETE_SELECTED_SPOTS_KEYS = new String[] { "BACK_SPACE", "DELETE" };
	
	private static final String TOGGLE_AUTO_LINKING = "toggle auto-linking";
	private static final String[] TOGGLE_AUTO_LINKING_KEYS = new String[] { "ctrl L" };
	
	private static final String NEXT_TIMEPOINT = "next timepoint";
	private static final String PREVIOUS_TIMEPOINT = "previous timepoint";
	private static final String[] NEXT_TIMEPOINT_KEYS = new String[] { "RIGHT" };
	private static final String[] PREVIOUS_TIMEPOINT_KEYS = new String[] { "LEFT" };

	private static final String SEMI_AUTOMATIC_TRACKING = "semi-automatic tracking";
	private static final String[] SEMI_AUTOMATIC_TRACKING_KEYS = new String[] { "shift A" };

	static
	{
		final int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
		final String modifier = ( menuMask == InputEvent.CTRL_DOWN_MASK ) ? "ctrl" : "meta";
		UNDO_ACTION_KEYS = new String[] { modifier + " Z" };
		REDO_ACTION_KEYS = new String[] { modifier + " Y", modifier + " shift Z" };
	}

	public static final void install( final Actions actions, final Model model, final SelectionModel selectionModel, final ImagePlus imp )
	{
		final TrackNavigator trackNavigator = new TrackNavigator( model, selectionModel );

		// Undo / redo
		actions.runnableAction( () -> model.undo(), UNDO_ACTION, UNDO_ACTION_KEYS );
		actions.runnableAction( () -> model.redo(), REDO_ACTION, REDO_ACTION_KEYS );

		// Navigate
		actions.runnableAction( () -> trackNavigator.previousInTime(), NAVIGATE_TO_PARENT, NAVIGATE_TO_PARENT_KEYS );
		actions.runnableAction( () -> trackNavigator.nextInTime(), NAVIGATE_TO_CHILD, NAVIGATE_TO_CHILD_KEYS );
		actions.runnableAction( () -> trackNavigator.previousSibling(), NAVIGATE_TO_PREVIOUS_SIBLING, NAVIGATE_TO_PREVIOUS_SIBLING_KEYS );
		actions.runnableAction( () -> trackNavigator.nextSibling(), NAVIGATE_TO_NEXT_SIBLING, NAVIGATE_TO_NEXT_SIBLING_KEYS );
		actions.runnableAction( () -> trackNavigator.root(), NAVIGATE_TO_ROOT, NAVIGATE_TO_ROOT_KEYS );
		actions.runnableAction( () -> trackNavigator.leaf(), NAVIGATE_TO_LEAF, NAVIGATE_TO_LEAF_KEYS );
		actions.runnableAction( () -> trackNavigator.previousTrack(), NAVIGATE_TO_PREVIOUS_TRACK, NAVIGATE_TO_PREVIOUS_TRACK_KEYS );
		actions.runnableAction( () -> trackNavigator.nextTrack(), NAVIGATE_TO_NEXT_TRACK, NAVIGATE_TO_NEXT_TRACK_KEYS );

		// Delete
		actions.runnableAction( () -> deleteSpotSelection( model, selectionModel ), DELETE_SELECTED_SPOTS, DELETE_SELECTED_SPOTS_KEYS );

		// Toggle auto-linking
		actions.runnableAction( () -> SpotEditBehaviours.autoLinkingmode = !SpotEditBehaviours.autoLinkingmode, TOGGLE_AUTO_LINKING, TOGGLE_AUTO_LINKING_KEYS );

		// Avoid closing the window when pressing W
		actions.runnableAction( () -> {}, "do nothing", new String[] { "W" } );

		// Change timepoint
		actions.runnableAction( () -> imp.setT( imp.getT() + SemiAutoTracking.params.stepwiseTimeBrowsing ), NEXT_TIMEPOINT, NEXT_TIMEPOINT_KEYS );
		actions.runnableAction( () -> imp.setT( imp.getT() - SemiAutoTracking.params.stepwiseTimeBrowsing ), PREVIOUS_TIMEPOINT, PREVIOUS_TIMEPOINT_KEYS );
		
		// Semi-automatic tracking
		final SemiAutoTracking semiAutoTracking = new SemiAutoTracking( model, selectionModel, imp );
		actions.runnableAction( () -> semiAutoTracking.run(), SEMI_AUTOMATIC_TRACKING, SEMI_AUTOMATIC_TRACKING_KEYS );
	}

	private static void deleteSpotSelection( final Model model, final SelectionModel selectionModel )
	{
		final ArrayList< Spot > spotSelection = new ArrayList<>( selectionModel.getSpotSelection() );
		final ArrayList< DefaultWeightedEdge > edgeSelection = new ArrayList<>( selectionModel.getEdgeSelection() );
		model.beginUpdate();
		try
		{
			selectionModel.clearSelection();
			for ( final DefaultWeightedEdge edge : edgeSelection )
				model.removeEdge( edge );
			for ( final Spot spot : spotSelection )
				model.removeSpot( spot );
		}
		finally
		{
			model.endUpdate();
		}
	}

	public static class SpotEditToolParams
	{

		/*
		 * Semi-auto tracking parameters
		 */
		/**
		 * The fraction of the initial quality above which we keep new spots.
		 * The highest, the more intolerant.
		 */
		double qualityThreshold = 0.5;

		/**
		 * How close must be the new spot found to be accepted, in radius units.
		 */
		double distanceTolerance = 2d;

		/**
		 * We process at most nFrames. Make it 0 or negative to have no bounds.
		 */
		int nFrames = 10;

		/**
		 * By how many frames to jump when we do step-wide time browsing.
		 */
		int stepwiseTimeBrowsing = 1;

		@Override
		public String toString()
		{
			return super.toString() + ": " + "QualityThreshold = " + qualityThreshold + ", DistanceTolerance = " + distanceTolerance + ", nFrames = " + nFrames;
		}
	}
}
