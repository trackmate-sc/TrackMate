package fiji.plugin.trackmate.undo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.Spot;

public class UndoRedoStack implements ModelChangeListener
{

	private final Model model;

	private boolean paused = false;

	private final Deque< ModelUndoableCommand > undoStack = new ArrayDeque<>();

	private final Deque< ModelUndoableCommand > redoStack = new ArrayDeque<>();

	private final int maxSize;

	public UndoRedoStack( final Model model )
	{
		this( model, 50 );
	}

	public UndoRedoStack( final Model model, final int maxSize )
	{
		this.model = model;
		this.maxSize = maxSize;
		model.addModelChangeListener( this );
	}

	public void pauseUndo()
	{
		this.paused = true;
	}

	public void resumeUndo()
	{
		this.paused = false;
	}

	public void undo()
	{
		if ( undoStack.isEmpty() )
			return;

		final ModelUndoableCommand command = undoStack.removeLast();
		redoStack.addLast( command );
		command.restoreBefore( model );
	}

	public void redo()
	{
		if ( redoStack.isEmpty() )
			return;

		final ModelUndoableCommand command = redoStack.removeLast();
		undoStack.addLast( command );
		command.restoreAfter( model );
	}

	@Override
	public void modelChanged( final ModelChangeEvent event )
	{
		if ( paused )
			return;

		System.out.println(); // DEBUG
		System.out.println( "UndoRedoStack: model changed" ); // DEBUG
		if ( event.getEventID() == ModelChangeEvent.MODEL_MODIFIED )
		{
			System.out.println( "Model modified" ); // DEBUG
			System.out.println( event ); // DEBUG
			final ModelUndoableCommand command = toCommand( event );

			redoStack.clear();
			if ( undoStack.size() >= maxSize )
				undoStack.removeFirst();

			undoStack.addLast( command );
		}
	}

	private ModelUndoableCommand toCommand( final ModelChangeEvent event )
	{
		final ModelUndoableCommand command = new ModelUndoableCommand();
		for ( final Spot spot : event.getSpots() )
		{
			if ( event.getSpotFlag( spot ) == ModelChangeEvent.FLAG_SPOT_ADDED )
			{
				command.spotsAdded.add( spot );
			}
			else if ( event.getSpotFlag( spot ) == ModelChangeEvent.FLAG_SPOT_REMOVED )
			{
				command.spotsRemoved.add( spot );
			}
			else if ( event.getSpotFlag( spot ) == ModelChangeEvent.FLAG_SPOT_MODIFIED )
			{
				// TODO: Store feature values BEFORE
				System.out.println( "Spot modified: " + spot ); // DEBUG
			}
		}
		for ( final DefaultWeightedEdge edge : event.getEdges() )
		{
			if ( event.getEdgeFlag( edge ) == ModelChangeEvent.FLAG_EDGE_ADDED )
			{
				command.edgesAdded.add( new fiji.plugin.trackmate.undo.UndoRedoStack.ModelUndoableCommand.EdgeRep(
						model.getTrackModel().getEdgeSource( edge ),
						model.getTrackModel().getEdgeTarget( edge ),
						model.getTrackModel().getEdgeWeight( edge ) ) );
			}
			else if ( event.getEdgeFlag( edge ) == ModelChangeEvent.FLAG_EDGE_REMOVED )
			{
				command.edgesRemoved.add( new fiji.plugin.trackmate.undo.UndoRedoStack.ModelUndoableCommand.EdgeRep(
						model.getTrackModel().getEdgeSource( edge ),
						model.getTrackModel().getEdgeTarget( edge ),
						model.getTrackModel().getEdgeWeight( edge ) ) );
			}
			else if ( event.getEdgeFlag( edge ) == ModelChangeEvent.FLAG_EDGE_MODIFIED )
			{
				// TODO: Store feature values BEFORE
				System.out.println( "Edge modified: " + edge ); // DEBUG
			}
		}
		return command;
	}

	private static class ModelUndoableCommand
	{

		static record EdgeRep( Spot source, Spot target, double weight )
		{}

		public final List< EdgeRep > edgesRemoved = new ArrayList<>();

		public final List< EdgeRep > edgesAdded = new ArrayList<>();

		private final List< Spot > spotsAdded = new ArrayList<>();

		private final List< Spot > spotsRemoved = new ArrayList<>();

		public void restoreBefore( final Model model )
		{
			model.pauseUndo();
			model.beginUpdate();
			try
			{
				for ( final EdgeRep edge : edgesAdded )
					model.removeEdge( edge.source, edge.target );

				for ( final Spot spot : spotsAdded )
					model.removeSpot( spot );

				for ( final Spot spot : spotsRemoved )
					model.addSpotTo( spot, spot.getFeature( Spot.FRAME ).intValue() );

				for ( final EdgeRep edge : edgesRemoved )
					model.addEdge( edge.source, edge.target, edge.weight );
			}
			finally
			{
				model.endUpdate();
			}
			model.resumeUndo();
		}

		public void restoreAfter( final Model model )
		{
			model.pauseUndo();
			model.beginUpdate();
			try
			{
				for ( final EdgeRep edge : edgesRemoved )
					model.removeEdge( edge.source, edge.target );

				for ( final Spot spot : spotsRemoved )
					model.removeSpot( spot );

				for ( final Spot spot : spotsAdded )
					model.addSpotTo( spot, spot.getFeature( Spot.FRAME ).intValue() );

				for ( final EdgeRep edge : edgesAdded )
					model.addEdge( edge.source, edge.target, edge.weight );
			}
			finally
			{
				model.endUpdate();
			}
			model.resumeUndo();
		}
	}
}
