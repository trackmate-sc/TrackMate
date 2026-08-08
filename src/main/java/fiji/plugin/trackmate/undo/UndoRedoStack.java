package fiji.plugin.trackmate.undo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.Spot.SpotVisitor;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.TrackModel;

public class UndoRedoStack implements ModelChangeListener
{

	private final Model model;

	private boolean paused = false;

	private final Deque< ModelUndoableCommand > undoStack = new ArrayDeque<>();

	private final Deque< ModelUndoableCommand > redoStack = new ArrayDeque<>();

	private final Map< Spot, Map< String, Double > > spotFeatureValuesBefore = new HashMap<>();

	private final Map< SpotRoi, double[][] > spotPolygonValuesBefore = new HashMap<>();

	private final Map< DefaultWeightedEdge, Map< String, Double > > edgeFeatureValuesBefore = new HashMap<>();

	private final Map< Spot, String > spotNameBefore = new HashMap<>();

	/**
	 * Track states before the operation, keyed by track ID. Only holds "before"
	 * values.
	 */
	private final Map< Integer, TrackState > trackStatesBefore = new HashMap<>();

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
		if ( event.getEventID() != ModelChangeEvent.MODEL_MODIFIED )
		{
			// Only deal with model modified events. Other events are not
			// undoable and results in clearing the undo/redo stack.

			undoStack.clear();
			redoStack.clear();
			spotFeatureValuesBefore.clear();
			edgeFeatureValuesBefore.clear();
			spotPolygonValuesBefore.clear();
			spotNameBefore.clear();
			trackStatesBefore.clear();
			return;
		}

		if ( paused )
			return;

		final ModelUndoableCommand command = toCommand( event );
		redoStack.clear();
		if ( undoStack.size() >= maxSize )
			undoStack.removeFirst();

		undoStack.addLast( command );
	}

	private ModelUndoableCommand toCommand( final ModelChangeEvent event )
	{
		final ModelUndoableCommand command = new ModelUndoableCommand();

		// First, process track states that were flagged via flagTrackForUndo()
		// (e.g., for track renames or before merge operations)
		// We need to fill in the "after" state
		for ( final Map.Entry< Integer, TrackState > entry : trackStatesBefore.entrySet() )
		{
			final Integer trackId = entry.getKey();
			final TrackState beforeState = entry.getValue();
			final TrackModel trackModel = model.getTrackModel();

			// Capture "after" state (current state after the modification)
			// Track may no longer exist if it was removed (all spots deleted)
			final String nameAfter = trackModel.name( trackId );
			// Check if track still exists before getting visibility (isVisible
			// throws NPE for non-existent tracks)
			final Boolean visibilityAfter = nameAfter != null ? trackModel.isVisible( trackId ) : null;
			final Set< Spot > spotsAfter = trackModel.trackSpots( trackId );

			final TrackState fullState = new TrackState(
					beforeState.nameBefore,
					beforeState.visibilityBefore,
					nameAfter != null ? nameAfter : beforeState.nameBefore,
					visibilityAfter != null ? visibilityAfter : beforeState.visibilityBefore,
					spotsAfter != null ? new HashSet<>( spotsAfter ) : new HashSet<>( beforeState.spots ) );
			command.trackStatesBefore.put( trackId, fullState );
		}

		// Also capture track states for tracks affected by edge/spot operations
		// (these only have "before" state captured, "after" will be same as
		// before for now)
		for ( final Integer trackId : event.getTrackUpdated() )
		{
			if ( !command.trackStatesBefore.containsKey( trackId ) )
			{
				final TrackModel trackModel = model.getTrackModel();
				final String name = trackModel.name( trackId );
				final boolean visibility = trackModel.isVisible( trackId );
				final Set< Spot > spots = new HashSet<>( trackModel.trackSpots( trackId ) );
				final TrackState state = new TrackState( name, visibility, name, visibility, spots );
				command.trackStatesBefore.put( trackId, state );
			}
		}

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
				final Map< String, Double > previousFeatureValues = spotFeatureValuesBefore.get( spot );
				command.spotFeatureValuesBefore.put( spot, previousFeatureValues );
				command.spotFeatureValuesAfter.put( spot, new HashMap<>( spot.getFeatures() ) );
				final String previousName = spotNameBefore.get( spot );
				command.spotNameBefore.put( spot, previousName );
				command.spotNameAfter.put( spot, spot.getName() );
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
				command.edgeFeatureValuesBefore.put( edge, edgeFeatureValuesBefore.get( edge ) );
				command.edgeFeatureValuesAfter.put( edge, copyEdgeFeatures( edge ) );
			}
		}
		spotFeatureValuesBefore.clear();
		edgeFeatureValuesBefore.clear();
		spotPolygonValuesBefore.clear();
		spotNameBefore.clear();
		trackStatesBefore.clear();
		return command;
	}

	/**
	 * Holds the state of a track before and after an operation. Used to restore
	 * track names and visibility after undo/redo.
	 */
	private static class TrackState
	{
		final String nameBefore;

		final String nameAfter;

		final boolean visibilityBefore;

		final boolean visibilityAfter;

		final Set< Spot > spots;

		TrackState( final String nameBefore, final boolean visibilityBefore,
				final String nameAfter, final boolean visibilityAfter,
				final Set< Spot > spots )
		{
			this.nameBefore = nameBefore;
			this.visibilityBefore = visibilityBefore;
			this.nameAfter = nameAfter;
			this.visibilityAfter = visibilityAfter;
			this.spots = spots;
		}
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

		private final Map< SpotRoi, double[][] > spotPolygonValuesBefore = new HashMap<>();

		private final Map< SpotRoi, double[][] > spotPolygonValuesAfter = new HashMap<>();

		private final Map< DefaultWeightedEdge, Map< String, Double > > edgeFeatureValuesBefore = new HashMap<>();

		private final Map< DefaultWeightedEdge, Map< String, Double > > edgeFeatureValuesAfter = new HashMap<>();

		private final Map< Spot, String > spotNameAfter = new HashMap<>();

		private final Map< Spot, String > spotNameBefore = new HashMap<>();

		/** Track states before the operation, keyed by track ID. */
		private final Map< Integer, TrackState > trackStatesBefore = new HashMap<>();

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
					model.beforeEdit( spot ); // to notify about update
					spot.setName( spotNameBefore.get( spot ) );
					spotFeatureValuesBefore.get( spot ).forEach( ( key, value ) -> spot.putFeature( key, value ) );
					if ( spot instanceof SpotRoi )
					{
						final SpotRoi spotRoi = ( SpotRoi ) spot;
						final double[][] polygonBefore = spotPolygonValuesBefore.get( spotRoi );
						updatePolygon( spotRoi, polygonBefore );
					}
				}
				for ( final DefaultWeightedEdge edge : edgeFeatureValuesBefore.keySet() )
					edgeFeatureValuesBefore.get( edge ).forEach( ( key, value ) -> {
						if ( value == null )
							model.getFeatureModel().removeEdgeFeature( edge, key );
						else
							model.getFeatureModel().putEdgeFeature( edge, key, value );
					} );

				// Restore track names and visibility after topology is rebuilt
				// (undo = restore before state)
				restoreTrackStatesFromCommand( model, true );
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
					model.beforeEdit( spot ); // to notify about update
					spot.setName( spotNameAfter.get( spot ) );
					spotFeatureValuesAfter.get( spot ).forEach( ( key, value ) -> spot.putFeature( key, value ) );
					if ( spot instanceof SpotRoi )
					{
						final SpotRoi spotRoi = ( SpotRoi ) spot;
						final double[][] polygonAfter = spotPolygonValuesAfter.get( spotRoi );
						updatePolygon( spotRoi, polygonAfter );
					}
				}
				for ( final DefaultWeightedEdge edge : edgeFeatureValuesAfter.keySet() )
					edgeFeatureValuesAfter.get( edge ).forEach( ( key, value ) -> {
						if ( value == null )
							model.getFeatureModel().removeEdgeFeature( edge, key );
						else
							model.getFeatureModel().putEdgeFeature( edge, key, value );
					} );

				// Restore track names and visibility after topology is rebuilt
				// (redo = restore after state)
				restoreTrackStatesFromCommand( model, false );
			}
			finally
			{
				model.endUpdate();
			}
			model.resumeUndo();
		}

		/**
		 * Restores track names and visibility from the captured states in the
		 * command. After an undo or redo operation, the track topology is
		 * rebuilt by the {@link TrackModel.MyGraphListener}, but track names
		 * and visibility are not restored. This method finds the tracks that
		 * contain the spots from the captured states and restores their names
		 * and visibility.
		 *
		 * @param model
		 *            the model
		 * @param restoreBefore
		 *            if true, restore the "before" state (undo); if false,
		 *            restore the "after" state (redo)
		 */
		private void restoreTrackStatesFromCommand( final Model model, final boolean restoreBefore )
		{
			final TrackModel trackModel = model.getTrackModel();

			// Build a map from spot ID to current track ID
			final Map< Integer, Integer > spotToCurrentTrackId = new HashMap<>();
			for ( final Integer trackId : trackModel.trackIDs( false ) )
			{
				for ( final Spot spot : trackModel.trackSpots( trackId ) )
					spotToCurrentTrackId.put( spot.ID(), trackId );
			}

			// Group captured track states by the current track they map to
			// This handles the case where multiple tracks (e.g., from a split)
			// merge back into one
			final Map< Integer, Map.Entry< Integer, TrackState > > currentTrackToBestState = new HashMap<>();

			for ( final Map.Entry< Integer, TrackState > entry : trackStatesBefore.entrySet() )
			{
				final Integer oldTrackId = entry.getKey();
				final TrackState state = entry.getValue();

				// Find a spot from the old track that still exists
				Spot referenceSpot = null;
				for ( final Spot spot : state.spots )
				{
					if ( trackModel.vertexSet().contains( spot ) )
					{
						referenceSpot = spot;
						break;
					}
				}

				if ( referenceSpot != null )
				{
					final Integer currentTrackId = spotToCurrentTrackId.get( referenceSpot.ID() );
					if ( currentTrackId != null )
					{
						// If this current track already has a state, keep the
						// one with the lowest oldTrackId
						// (the original track before split)
						if ( !currentTrackToBestState.containsKey( currentTrackId ) || oldTrackId < currentTrackToBestState.get( currentTrackId ).getKey() )
							currentTrackToBestState.put( currentTrackId, entry );
					}
				}
			}

			// Now restore names - each current track gets at most one name
			// restoration
			for ( final Map.Entry< Integer, Map.Entry< Integer, TrackState > > e : currentTrackToBestState.entrySet() )
			{
				final Integer currentTrackId = e.getKey();
				final Map.Entry< Integer, TrackState > stateEntry = e.getValue();
				final TrackState state = stateEntry.getValue();

				// Choose which state to restore based on restoreBefore flag
				final String nameToRestore = restoreBefore ? state.nameBefore : state.nameAfter;
				final boolean visibilityToRestore = restoreBefore ? state.visibilityBefore : state.visibilityAfter;

				// Restore name and visibility
				trackModel.setName( currentTrackId, nameToRestore );
				trackModel.setVisibility( currentTrackId, visibilityToRestore );
			}

			// Don't clear trackStatesBefore - it's part of the command and may
			// be needed for redo
		}
	}

	private class UndoStorer implements SpotVisitor
	{

		@Override
		public void visit( final SpotBase spot )
		{
			// Spot features.
			spotFeatureValuesBefore.put( spot, new HashMap<>( spot.getFeatures() ) );
			// Touching edge features.
			final TrackModel trackModel = model.getTrackModel();
			final Set< DefaultWeightedEdge > touchingEdges = trackModel.edgesOf( spot );
			for ( final DefaultWeightedEdge edge : touchingEdges )
				edgeFeatureValuesBefore.put( edge, copyEdgeFeatures( edge ) );
			// Spot name.
			spotNameBefore.put( spot, spot.getName() );
		}

		@Override
		public void visit( final SpotRoi spot )
		{
			visit( ( SpotBase ) spot );
			spotPolygonValuesBefore.put( spot, toPolygon( spot ) );
		}
	}

	private final Map< String, Double > copyEdgeFeatures( final DefaultWeightedEdge edge )
	{
		final FeatureModel featureModel = model.getFeatureModel();
		final Collection< String > edgeFeatures = featureModel.getEdgeFeatures();
		final Map< String, Double > featureValues = new HashMap<>();
		for ( final String feature : edgeFeatures )
		{
			final Double value = featureModel.getEdgeFeature( edge, feature );
			featureValues.put( feature, value );
		}
		return featureValues;
	}

	private final UndoStorer undoStorer = new UndoStorer();

	public void flagForUndo( final Spot spot )
	{
		if ( paused )
			return;
		spot.accept( undoStorer );
	}

	/**
	 * Flags a track for undo by capturing its current name and visibility. This
	 * should be called before modifying a track's name or structure.
	 *
	 * @param trackId
	 *            the track ID to flag for undo
	 */
	public void flagTrackForUndo( final Integer trackId )
	{
		if ( paused )
		{

			return;
		}
		final TrackModel trackModel = model.getTrackModel();
		final String currentName = trackModel.name( trackId );
		final boolean currentVisibility = trackModel.isVisible( trackId );
		final Set< Spot > currentSpots = new HashSet<>( trackModel.trackSpots( trackId ) );
		// Capture "before" state; "after" state will be filled in toCommand()
		trackStatesBefore.put( trackId, new TrackState( currentName, currentVisibility, null, false, currentSpots ) );
	}

	/**
	 * Flags all tracks for undo by capturing their current names and
	 * visibility. This should be called before operations that may restructure
	 * tracks (e.g., adding/removing edges that may merge or split tracks).
	 */
	public void flagAllTracksForUndo()
	{
		if ( paused )
			return;
		final TrackModel trackModel = model.getTrackModel();
		for ( final Integer trackId : trackModel.trackIDs( false ) )
		{
			final String currentName = trackModel.name( trackId );
			final boolean currentVisibility = trackModel.isVisible( trackId );
			final Set< Spot > currentSpots = new HashSet<>( trackModel.trackSpots( trackId ) );
			// Capture "before" state; "after" will be filled in toCommand()
			trackStatesBefore.put( trackId, new TrackState( currentName, currentVisibility, null, false, currentSpots ) );
		}
	}

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
}
