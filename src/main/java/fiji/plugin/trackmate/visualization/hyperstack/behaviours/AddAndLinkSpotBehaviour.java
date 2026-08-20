package fiji.plugin.trackmate.visualization.hyperstack.behaviours;

import java.util.Set;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import ij.ImagePlus;
import net.imglib2.KDTree;
import net.imglib2.RealLocalizable;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;

public class AddAndLinkSpotBehaviour extends LinkSpotsBehaviour
{

	private static final String OVERLAY_NAME = "AddAndLinkSpotActionOverlay";

	private final SelectionModel selectionModel;

	private SpotBase newSpot;

	public AddAndLinkSpotBehaviour( final Model model, final SelectionModel selectionModel, final ImagePlus imp, final boolean backward )
	{
		super( model, imp, backward );
		this.selectionModel = selectionModel;
	}

	@Override
	public void init( final int x, final int y )
	{
		if ( source != null || target != null )
			return;

		final RealLocalizable pos = toWorldCoords( x, y );
		final Spot spot = getSpotAtMouseLocation( pos );
		final int targetFrame;
		if ( spot == null )
		{
			// Create a new spot, that we will move around until the user
			// releases the mouse button.
			this.source = null;
			overlay.source = null;
			targetFrame = imp.getT() - 1;
			this.search = null;

			// Is auto-linking mode enabled?
			if ( SpotEditBehaviours.autoLinkingmode )
			{
				// Find one spot in the selection.
				final Set< Spot > selection = selectionModel.getSpotSelection();
				if ( selection.size() == 1 )
				{
					final Spot ls = selection.iterator().next();
					// Cannot be on the same frame.
					if ( ls.getFeature( Spot.FRAME ).intValue() != targetFrame )
					{
						this.source = ls;
						overlay.source = source;
						final RealLocalizable screenPos = toScreenCoords( source );
						overlay.sourcePixelPos[ 0 ] = ( int ) Math.round( screenPos.getDoublePosition( 0 ) );
						overlay.sourcePixelPos[ 1 ] = ( int ) Math.round( screenPos.getDoublePosition( 1 ) );
					}
				}
				else
				{
					this.source = null;
					overlay.source = null;
				}

			}
		}
		else
		{
			// We have a source, link from it.
			// Cannot link if at first frame and backward, etc.
			final int frame = spot.getFeature( Spot.FRAME ).intValue();
			if ( backward && frame == 0 )
				return;
			if ( !backward && frame == imp.getNFrames() - 1 )
				return;

			// Keep track of the source.
			this.source = spot;
			overlay.source = source;
			final RealLocalizable screenPos = toScreenCoords( spot );
			overlay.sourcePixelPos[ 0 ] = ( int ) screenPos.getDoublePosition( 0 );
			overlay.sourcePixelPos[ 1 ] = ( int ) screenPos.getDoublePosition( 1 );

			// Move to next frame if forward, previous frame if backward.
			targetFrame = backward ? frame - 1 : frame + 1;
			imp.setT( targetFrame + 1 );

			// Build KD-tree of target frame spots.
			final Iterable< Spot > targetSpots = model.getSpots().iterable( targetFrame, true );
			final int nTargetSpots = model.getSpots().getNSpots( targetFrame, true );
			if ( nTargetSpots > 0 )
			{
				final KDTree< Spot > tree = new KDTree< Spot >( nTargetSpots, targetSpots, targetSpots );
				this.search = new NearestNeighborSearchOnKDTree<>( tree );
			}
			else
			{
				this.search = null;
			}
		}

		// Add a new spot at the mouse location in the target frame.
		final double radius = SpotEditBehaviours.ResizeSpotBehaviour.previousRadius;
		this.newSpot = new SpotBase( pos, radius, -1. );
		final double dt = imp.getCalibration().frameInterval;
		newSpot.putFeature( Spot.POSITION_T, targetFrame * dt );
		newSpot.putFeature( Spot.FRAME, Double.valueOf( targetFrame ) );

		target = newSpot;
		overlay.target = newSpot;
		overlay.targetPixelPos[ 0 ] = x;
		overlay.targetPixelPos[ 1 ] = y;
		imp.getOverlay().add( overlay, OVERLAY_NAME );
		imp.updateAndDraw();
	}

	@Override
	public void drag( final int x, final int y )
	{
		final RealLocalizable pos = toWorldCoords( x, y );
		if ( search != null )
		{
			search.search( pos );
			final Spot closestSpot = search.getSampler().get();
			final double r = closestSpot.getFeature( Spot.RADIUS );
			if ( search.getSquareDistance() < r * r )
			{
				target = closestSpot;
				final RealLocalizable screenPos = toScreenCoords( target );
				overlay.targetPixelPos[ 0 ] = ( int ) Math.round( screenPos.getDoublePosition( 0 ) );
				overlay.targetPixelPos[ 1 ] = ( int ) Math.round( screenPos.getDoublePosition( 1 ) );
				overlay.target = closestSpot;

				// Is there an existing link between source and target?
				overlay.crossedLine.crossed = model.getTrackModel().containsEdge( source, target );
				imp.updateAndDraw();
				return;
			}
		}
		// Simply move the created spot.
		newSpot.setPosition( pos.getDoublePosition( 0 ), 0 );
		newSpot.setPosition( pos.getDoublePosition( 1 ), 1 );
		target = newSpot;
		overlay.targetPixelPos[ 0 ] = x;
		overlay.targetPixelPos[ 1 ] = y;
		overlay.target = newSpot;
		overlay.crossedLine.crossed = false;
		imp.updateAndDraw();
	}

	@Override
	public void end( final int x, final int y )
	{
		try
		{
			if ( target == null )
				return;

			model.beginUpdate();
			try
			{
				if ( target == newSpot )
				{
					model.addSpotTo( newSpot, newSpot.getFeature( Spot.FRAME ).intValue() );
					if ( source != null )
						model.addEdge( source, newSpot, -1 );
				}
				else
				{
					// Add or remove link between source and pre-existing
					// target.
					if ( model.getTrackModel().containsEdge( source, target ) )
					{
						model.removeEdge( source, target );
					}
					else
					{
						if ( backward )
							model.addEdge( target, source, -1 );
						else
							model.addEdge( source, target, -1 );
					}
				}
			}
			finally
			{
				model.endUpdate();
				if ( SpotEditBehaviours.autoLinkingmode )
				{
					// Select the newly created spot.
					selectionModel.clearSpotSelection();
					selectionModel.addSpotToSelection( target );
				}
			}
		}
		finally
		{
			source = null;
			target = null;
			search = null;
			overlay.source = null;
			overlay.target = null;
			imp.updateAndDraw();
			imp.getOverlay().remove( OVERLAY_NAME );
		}
	}
}
