package fiji.plugin.trackmate.visualization.hyperstack.behaviours;

import java.util.Set;

import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.Behaviours;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import ij.ImagePlus;
import net.imglib2.RealLocalizable;

public class SpotEditBehaviours
{
	
	private static final String MOVE_SPOT = "move spot";
	private static final String INCREASE_SPOT_RADIUS = "increase spot radius";
	private static final String INCREASE_SPOT_RADIUS_FAST = "increase spot radius fast";
	private static final String DECREASE_SPOT_RADIUS = "decrease spot radius";
	private static final String DECREASE_SPOT_RADIUS_FAST = "decrease spot radius fast";
	private static final String ADD_SPOT = "add spot";
	private static final String DELETE_SPOT = "delete spot";
	private static final String LINK_SPOTS = "link spots";
	private static final String LINK_SPOTS_BACKWARD = "link spots backward";
	private static final String CLICK_SELECT_SPOT = "click select spot";
	private static final String CLICK_SELECT_ADD_SPOT = "click select add spot";
	
	private static final String[] MOVE_SPOT_KEYS = new String[] { "SPACE" };
	private static final String[] INCREASE_SPOT_RADIUS_KEYS = new String[] { "E" };
	private static final String[] INCREASE_SPOT_RADIUS_FAST_KEYS = new String[] { "shift E" };
	private static final String[] DECREASE_SPOT_RADIUS_KEYS = new String[] { "Q" };
	private static final String[] DECREASE_SPOT_RADIUS_FAST_KEYS = new String[] { "shift Q" };
	private static final String[] ADD_SPOT_KEYS = new String[] { "A" };
	private static final String[] DELETE_SPOT_KEYS = new String[] { "D" };
	private static final String[] LINK_SPOTS_KEYS = new String[] { "L" };
	private static final String[] LINK_SPOTS_BACKWARD_KEYS = new String[] { "shift L" };
	private static final String[] CLICK_SELECT_SPOT_KEYS = new String[] { "button1" };
	private static final String[] CLICK_SELECT_ADD_SPOT_KEYS = new String[] { "shift button1" };

	static boolean autoLinkingmode = false;

	public static final void install( final Behaviours behaviours, final Model model, final SelectionModel selectionModel, final ImagePlus imp )
	{
		behaviours.behaviour( new MoveSpotBehaviour( model, imp ), MOVE_SPOT, MOVE_SPOT_KEYS );

		behaviours.behaviour( new ResizeSpotBehaviour( model, imp, true, false ), INCREASE_SPOT_RADIUS, INCREASE_SPOT_RADIUS_KEYS );
		behaviours.behaviour( new ResizeSpotBehaviour( model, imp, true, true ), INCREASE_SPOT_RADIUS_FAST, INCREASE_SPOT_RADIUS_FAST_KEYS );
		behaviours.behaviour( new ResizeSpotBehaviour( model, imp, false, false ), DECREASE_SPOT_RADIUS, DECREASE_SPOT_RADIUS_KEYS );
		behaviours.behaviour( new ResizeSpotBehaviour( model, imp, false, true ), DECREASE_SPOT_RADIUS_FAST, DECREASE_SPOT_RADIUS_FAST_KEYS );

		behaviours.behaviour( new AddSpotBehaviour( model, selectionModel, imp ), ADD_SPOT, ADD_SPOT_KEYS );
		behaviours.behaviour( new DeleteSpotBehaviour( model, selectionModel, imp ), DELETE_SPOT, DELETE_SPOT_KEYS );

		behaviours.behaviour( new LinkSpotsBehaviour( model, imp, false ), LINK_SPOTS, LINK_SPOTS_KEYS );
		behaviours.behaviour( new LinkSpotsBehaviour( model, imp, true ), LINK_SPOTS_BACKWARD, LINK_SPOTS_BACKWARD_KEYS );

		behaviours.behaviour( new ClickSelectSpotBehaviour( model, selectionModel, imp ), CLICK_SELECT_SPOT, CLICK_SELECT_SPOT_KEYS );
		behaviours.behaviour( new ClickSelectAddSpotBehaviour( model, selectionModel, imp ), CLICK_SELECT_ADD_SPOT, CLICK_SELECT_ADD_SPOT_KEYS );
	}

	private static class ClickSelectAddSpotBehaviour extends AbstractSpotEditBehaviour implements ClickBehaviour
	{

		private final SelectionModel selectionModel;

		public ClickSelectAddSpotBehaviour( final Model model, final SelectionModel selectionModel, final ImagePlus imp )
		{
			super( model, imp );
			this.selectionModel = selectionModel;
		}

		@Override
		public void click( final int x, final int y )
		{
			final RealLocalizable pos = toWorldCoords( x, y );
			final Spot target = getSpotAtMouseLocation( pos );
			if ( null == target )
				return;
			if ( selectionModel.getSpotSelection().contains( target ) )
				selectionModel.removeSpotFromSelection( target );
			else
				selectionModel.addSpotToSelection( target );
		}
	}

	private static class ClickSelectSpotBehaviour extends AbstractSpotEditBehaviour implements ClickBehaviour
	{

		private final SelectionModel selectionModel;

		public ClickSelectSpotBehaviour( final Model model, final SelectionModel selectionModel, final ImagePlus imp )
		{
			super( model, imp );
			this.selectionModel = selectionModel;
		}

		@Override
		public void click( final int x, final int y )
		{
			selectionModel.clearSelection();
			final RealLocalizable pos = toWorldCoords( x, y );
			final Spot target = getSpotAtMouseLocation( pos );
			if ( null == target )
				return;
			selectionModel.addSpotToSelection( target );
		}
	}

	private static class DeleteSpotBehaviour extends AbstractSpotEditBehaviour implements ClickBehaviour
	{

		private final SelectionModel selectionModel;

		public DeleteSpotBehaviour( final Model model, final SelectionModel selectionModel, final ImagePlus imp )
		{
			super( model, imp );
			this.selectionModel = selectionModel;
		}

		@Override
		public void click( final int x, final int y )
		{
			final RealLocalizable pos = toWorldCoords( x, y );
			final Spot target = getSpotAtMouseLocation( pos );
			if ( null == target )
				return;

			selectionModel.removeSpotFromSelection( target );
			model.beginUpdate();
			try
			{
				model.removeSpot( target );
			}
			finally
			{
				model.endUpdate();
			}
		}
	}

	private static class AddSpotBehaviour extends AbstractSpotEditBehaviour implements ClickBehaviour
	{

		private final SelectionModel selectionModel;

		public AddSpotBehaviour( final Model model, final SelectionModel selectionModel, final ImagePlus imp )
		{
			super( model, imp );
			this.selectionModel = selectionModel;
		}

		@Override
		public void click( final int x, final int y )
		{
			final RealLocalizable pos = toWorldCoords( x, y );
			// Forbid adding a spot if there is already one at this location.
			if ( getSpotAtMouseLocation( pos ) != null )
				return;

			final double radius = ResizeSpotBehaviour.previousRadius;
			final SpotBase newSpot = new SpotBase( pos, radius, -1. );

			final double dt = imp.getCalibration().frameInterval;
			final int frame = imp.getFrame() - 1;
			newSpot.putFeature( Spot.POSITION_T, frame * dt );
			newSpot.putFeature( Spot.FRAME, Double.valueOf( frame ) );

			model.beginUpdate();
			try
			{
				model.addSpotTo( newSpot, frame );
			}
			finally
			{
				model.endUpdate();
			}

			/*
			 * If we are in auto-link mode, we create an edge with spot in
			 * selection, if there is just one and if it is in a previous frame
			 */
			if ( autoLinkingmode )
			{
				final Set< Spot > spotSelection = selectionModel.getSpotSelection();
				if ( spotSelection.size() == 1 )
				{
					final Spot source = spotSelection.iterator().next();
					if ( newSpot.diffTo( source, Spot.FRAME ) != 0 )
					{
						model.beginUpdate();
						try
						{
							model.addEdge( source, newSpot, -1 );
						}
						finally
						{
							model.endUpdate();
						}
					}
				}
				selectionModel.clearSpotSelection();
				selectionModel.addSpotToSelection( newSpot );
			}
		}
	}

	private static class MoveSpotBehaviour extends AbstractSpotEditBehaviour implements DragBehaviour
	{

		/**
		 * Offset between mouse click and spot center, in world coordinates.
		 */
		private final double[] delta = new double[ 2 ];

		private Spot movedSpot;

		public MoveSpotBehaviour( final Model model, final ImagePlus imp )
		{
			super( model, imp );
		}

		@Override
		public void init( final int x, final int y )
		{
			if ( null != movedSpot )
				return;
			final RealLocalizable pos = toWorldCoords( x, y );
			movedSpot = getSpotAtMouseLocation( pos );
			if ( null == movedSpot )
				return;
			model.beginUpdate();
			model.beforeEdit( movedSpot );
			delta[ 0 ] = movedSpot.getDoublePosition( 0 ) - pos.getDoublePosition( 0 );
			delta[ 1 ] = movedSpot.getDoublePosition( 1 ) - pos.getDoublePosition( 1 );
		}

		@Override
		public void drag( final int x, final int y )
		{
			final RealLocalizable pos = toWorldCoords( x, y );
			movedSpot.setPosition( pos.getDoublePosition( 0 ) + delta[ 0 ], 0 );
			movedSpot.setPosition( pos.getDoublePosition( 1 ) + delta[ 1 ], 1 );
			imp.updateAndDraw();
		}

		@Override
		public void end( final int x, final int y )
		{
			model.endUpdate();
			movedSpot = null;
			imp.updateAndDraw();
		}
	}

	private static class ResizeSpotBehaviour extends AbstractSpotEditBehaviour implements ClickBehaviour
	{

		/**
		 * Fall back default radius when the settings does not give a default
		 * radius to use.
		 */
		private static final double FALL_BACK_RADIUS = 5.;

		private static final double COARSE_STEP = 2;

		private static final double FINE_STEP = 0.2f;

		private final boolean increase;

		private final boolean fast;

		/** The previous radius to be used for spot creation. */
		static double previousRadius = FALL_BACK_RADIUS;

		public ResizeSpotBehaviour( final Model model, final ImagePlus imp, final boolean increase, final boolean fast )
		{
			super( model, imp );
			this.increase = increase;
			this.fast = fast;
		}

		@Override
		public void click( final int x, final int y )
		{
			final Spot spot = getSpotAtMouseLocation( toWorldCoords( x, y ) );
			if ( null == spot )
				return;

			// Compute new radius.
			final double radius = spot.getFeature( Spot.RADIUS );
			final int factor = ( increase ) ? -1 : 1;
			final double dx = imp.getCalibration().pixelWidth;

			final double newRadius = ( fast )
					? radius + factor * dx * COARSE_STEP
					: radius + factor * dx * FINE_STEP;

			if ( newRadius <= dx )
				return;

			// Actually scale the spot.
			model.beginUpdate();
			try
			{
				model.beforeEdit( spot );
				spot.scale( radius / newRadius );
				// Store new value of radius for next spot creation.
				previousRadius = newRadius;
			}
			catch ( final Exception e )
			{
				e.printStackTrace();
			}
			finally
			{
				model.endUpdate();
			}
		}
	}

	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( TrackMateImpBehaviour.KEY_CONFIG_SCOPE, TrackMateImpBehaviour.KEY_CONFIG_CONTEXT );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( MOVE_SPOT, MOVE_SPOT_KEYS, "Move a spot by dragging it." );
			descriptions.add( INCREASE_SPOT_RADIUS, INCREASE_SPOT_RADIUS_KEYS, "Increase the radius of a spot." );
			descriptions.add( INCREASE_SPOT_RADIUS_FAST, INCREASE_SPOT_RADIUS_FAST_KEYS, "Increase the radius of a spot (fast)." );
			descriptions.add( DECREASE_SPOT_RADIUS, DECREASE_SPOT_RADIUS_KEYS, "Decrease the radius of a spot." );
			descriptions.add( DECREASE_SPOT_RADIUS_FAST, DECREASE_SPOT_RADIUS_FAST_KEYS, "Decrease the radius of a spot (fast)." );
			descriptions.add( ADD_SPOT, ADD_SPOT_KEYS, "Add a new spot at the mouse location." );
			descriptions.add( DELETE_SPOT, DELETE_SPOT_KEYS, "Delete the spot at the mouse location." );
			descriptions.add( LINK_SPOTS, LINK_SPOTS_KEYS, "Link two spots forward in time by dragging from a source spot to a target spot in the next time-point." );
			descriptions.add( LINK_SPOTS_BACKWARD, LINK_SPOTS_BACKWARD_KEYS, "Link two spots backward in time by dragging from a source spot to a target spot in the previous time-point." );
			descriptions.add( CLICK_SELECT_SPOT, CLICK_SELECT_SPOT_KEYS, "Select a spot at the mouse location." );
			descriptions.add( CLICK_SELECT_ADD_SPOT, CLICK_SELECT_ADD_SPOT_KEYS, "Add or remove a spot from the selection at the mouse location." );
		}
	}
}
