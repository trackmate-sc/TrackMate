package fiji.plugin.trackmate.undo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.Spot.SpotVisitor;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.SpotRoi;

public class UndoRedoStack implements ModelChangeListener
{

	private final Model model;

	private boolean paused = false;

	private final Deque< ModelUndoableCommand > undoStack = new ArrayDeque<>();

	private final Deque< ModelUndoableCommand > redoStack = new ArrayDeque<>();

	private final Map< Spot, Map< String, Double > > spotFeatureValuesBefore = new HashMap<>();

	private final Map< SpotRoi, double[][] > spotPolygonValuesBefore = new HashMap<>();

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

		if ( event.getEventID() == ModelChangeEvent.MODEL_MODIFIED )
		{
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
				final Map< String, Double > previousFeatureValues = spotFeatureValuesBefore.get( spot );
				command.spotFeatureValuesBefore.put( spot, previousFeatureValues );
				command.spotFeatureValuesAfter.put( spot, new HashMap<>( spot.getFeatures() ) );
				if ( spot instanceof SpotRoi )
				{
					final SpotRoi spotRoi = ( SpotRoi ) spot;
					command.spotPolygonValuesBefore.put( spotRoi, spotPolygonValuesBefore.get( spotRoi ) );
					command.spotPolygonValuesAfter.put( spotRoi, toPolygon( spotRoi ) );
				}
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
		spotFeatureValuesBefore.clear();
		return command;
	}

	private static class ModelUndoableCommand
	{


		private static record EdgeRep( Spot source, Spot target, double weight )
		{}

		private final List< EdgeRep > edgesRemoved = new ArrayList<>();

		private final List< EdgeRep > edgesAdded = new ArrayList<>();

		private final List< Spot > spotsAdded = new ArrayList<>();

		private final List< Spot > spotsRemoved = new ArrayList<>();

		private final Map< Spot, Map< String, Double > > spotFeatureValuesBefore = new HashMap<>();

		private final Map< Spot, Map< String, Double > > spotFeatureValuesAfter = new HashMap<>();

		public Map< SpotRoi, double[][] > spotPolygonValuesBefore = new HashMap<>();

		public Map< SpotRoi, double[][] > spotPolygonValuesAfter = new HashMap<>();

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

				for ( final Spot spot : spotFeatureValuesBefore.keySet() )
				{
					spotFeatureValuesBefore.get( spot ).forEach( ( key, value ) -> spot.putFeature( key, value ) );
					if ( spot instanceof SpotRoi )
					{
						final SpotRoi spotRoi = ( SpotRoi ) spot;
						final double[][] polygonBefore = spotPolygonValuesBefore.get( spotRoi );
						updatePolygon( spotRoi, polygonBefore );
					}
					model.updateFeatures( spot );
				}
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

				for ( final Spot spot : spotFeatureValuesAfter.keySet() )
				{
					spotFeatureValuesAfter.get( spot ).forEach( ( key, value ) -> spot.putFeature( key, value ) );
					if ( spot instanceof SpotRoi )
					{
						final SpotRoi spotRoi = ( SpotRoi ) spot;
						final double[][] polygonAfter = spotPolygonValuesAfter.get( spotRoi );
						updatePolygon( spotRoi, polygonAfter );
					}
					model.updateFeatures( spot );
				}
			}
			finally
			{
				model.endUpdate();
			}
			model.resumeUndo();
		}
	}

	private class UndoStorer implements SpotVisitor
	{

		@Override
		public void visit( final SpotBase spot )
		{
			spotFeatureValuesBefore.put( spot, new HashMap<>( spot.getFeatures() ) );
		}

		@Override
		public void visit( final SpotRoi spot )
		{
			visit( ( SpotBase ) spot );
			spotPolygonValuesBefore.put( spot, toPolygon( spot ) );
		}
	}

	private final UndoStorer undoStorer = new UndoStorer();

	private static final double[][] toPolygon( final SpotRoi spot )
	{
		final int nPoints = spot.nPoints();
		final double[] x = new double[ nPoints ];
		final double[] y = new double[ nPoints ];
		for ( int i = 0; i < nPoints; i++ )
		{
			x[ i ] = spot.xr( i );
			y[ i ] = spot.yr( i );
		}
		return new double[][] { x, y };
	}

	private static final void updatePolygon( final SpotRoi spot, final double[][] polygon )
	{
		final int nPoints = spot.nPoints();
		for ( int i = 0; i < nPoints; i++ )
		{
			spot.setXr( i, polygon[ 0 ][ i ] );
			spot.setYr( i, polygon[ 1 ][ i ] );
		}
	}

	public void flagForUndo( final Spot spot )
	{
		spot.accept( undoStorer );
	}
}
