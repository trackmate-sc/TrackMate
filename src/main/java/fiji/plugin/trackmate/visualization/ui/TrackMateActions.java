package fiji.plugin.trackmate.visualization.ui;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.Actions;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.TrackNavigator;

public class TrackMateActions
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

	private static final String DELETE_SELECTION = "delete selection";
	private static final String[] DELETE_SELECTION_KEYS = new String[] { "BACK_SPACE", "DELETE" };
	
	private static final String SELECT_ALL = "select all";
	private static final String SELECT_ALL_SPOTS = "select all spots";
	private static final String SELECT_ALL_LINKS = "select all links";
	private static final String[] SELECT_ALL_KEYS;
	private static final String[] SELECT_ALL_SPOTS_KEYS;
	private static final String[] SELECT_ALL_LINKS_KEYS;
	
	static
	{
		int menuMask;
		try
		{
			menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
		}
		catch ( final HeadlessException e )
		{
			// Default to Ctrl in headless environments (CI, tests)
			menuMask = InputEvent.CTRL_DOWN_MASK;
		}
		final String modifier = ( menuMask == InputEvent.CTRL_DOWN_MASK ) ? "ctrl" : "meta";
		UNDO_ACTION_KEYS = new String[] { modifier + " Z" };
		REDO_ACTION_KEYS = new String[] { modifier + " Y", modifier + " shift Z" };
		SELECT_ALL_KEYS = new String[] { modifier + " A" };
		SELECT_ALL_SPOTS_KEYS = new String[] { modifier + " shift A" };
		SELECT_ALL_LINKS_KEYS = new String[] { modifier + " alt A" };
	}

	public static final void install( final Actions actions, final Model model, final SelectionModel selectionModel )
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

		// Delete selection
		actions.runnableAction( () -> deleteSelection( model, selectionModel ), DELETE_SELECTION, DELETE_SELECTION_KEYS );

		// Select all
		actions.runnableAction( () -> selectAll( model, selectionModel ), SELECT_ALL, SELECT_ALL_KEYS );
		actions.runnableAction( () -> selectAllSpots( model, selectionModel ), SELECT_ALL_SPOTS, SELECT_ALL_SPOTS_KEYS );
		actions.runnableAction( () -> selectAllLinks( model, selectionModel ), SELECT_ALL_LINKS, SELECT_ALL_LINKS_KEYS );
	}

	private static void selectAll( final Model model, final SelectionModel selectionModel )
	{
		selectAllSpots( model, selectionModel );
		selectAllLinks( model, selectionModel );
	}

	private static void selectAllSpots( final Model model, final SelectionModel selectionModel )
	{
		final List< Spot > spotsToAdd = new ArrayList<>();
		model.getSpots().iterable( true ).forEach( spotsToAdd::add );
		selectionModel.addSpotToSelection( spotsToAdd );
	}

	private static void selectAllLinks( final Model model, final SelectionModel selectionModel )
	{
		final List< DefaultWeightedEdge > edgesToAdd = new ArrayList<>();
		model.getTrackModel().edgeSet().forEach( edgesToAdd::add );
		selectionModel.addEdgeToSelection( edgesToAdd );
	}

	private static void deleteSelection( final Model model, final SelectionModel selectionModel )
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

	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigContexts.KEY_CONFIG_SCOPE, KeyConfigContexts.TRACKMATE );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( UNDO_ACTION, UNDO_ACTION_KEYS, "Undo the last edit." );
			descriptions.add( REDO_ACTION, REDO_ACTION_KEYS, "Redo the last undone edit." );

			descriptions.add( NAVIGATE_TO_PARENT, NAVIGATE_TO_PARENT_KEYS, "Navigate to the parent of the selected spot." );
			descriptions.add( NAVIGATE_TO_CHILD, NAVIGATE_TO_CHILD_KEYS, "Navigate to the child of the selected spot." );
			descriptions.add( NAVIGATE_TO_PREVIOUS_SIBLING, NAVIGATE_TO_PREVIOUS_SIBLING_KEYS, "Navigate to the previous sibling of the selected spot." );
			descriptions.add( NAVIGATE_TO_NEXT_SIBLING, NAVIGATE_TO_NEXT_SIBLING_KEYS, "Navigate to the next sibling of the selected spot." );
			descriptions.add( NAVIGATE_TO_ROOT, NAVIGATE_TO_ROOT_KEYS, "Navigate to the root of the current track." );
			descriptions.add( NAVIGATE_TO_LEAF, NAVIGATE_TO_LEAF_KEYS, "Navigate to the leaf of the current track." );
			descriptions.add( NAVIGATE_TO_PREVIOUS_TRACK, NAVIGATE_TO_PREVIOUS_TRACK_KEYS, "Navigate to the previous track." );
			descriptions.add( NAVIGATE_TO_NEXT_TRACK, NAVIGATE_TO_NEXT_TRACK_KEYS, "Navigate to the next track." );

			descriptions.add( DELETE_SELECTION, DELETE_SELECTION_KEYS, "Delete the selected spots and edges." );
			descriptions.add( SELECT_ALL, SELECT_ALL_KEYS, "Select all spots and edges." );
			descriptions.add( SELECT_ALL_SPOTS, SELECT_ALL_SPOTS_KEYS, "Select all spots." );
			descriptions.add( SELECT_ALL_LINKS, SELECT_ALL_LINKS_KEYS, "Select all edges." );
		}
	}
}
