package fiji.plugin.trackmate.visualization.hyperstack.behaviours;

import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.util.Behaviours;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.TMUtils;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;

public class SpotEditBehaviours
{

	public static final void install( final Behaviours behaviours, final Model model, final SelectionModel selectionModel, final ImagePlus imp )
	{
		behaviours.behaviour( new MoveSpotBehaviour( model, imp ), "move-spot", "SPACE" );
		behaviours.behaviour( new ResizeSpotBehaviour( model, imp, true, false ), "increase-spot-radius", "E" );
		behaviours.behaviour( new ResizeSpotBehaviour( model, imp, true, true ), "increase-spot-radius-fast", "shift E" );
		behaviours.behaviour( new ResizeSpotBehaviour( model, imp, false, false ), "decrease-spot-radius", "Q" );
		behaviours.behaviour( new ResizeSpotBehaviour( model, imp, false, true ), "decrease-spot-radius-fast", "shift Q" );
	}

	private static class AbstractSpotEditBehaviour
	{

		protected Spot movedSpot;

		protected final Model model;

		protected final ImagePlus imp;

		protected final double[] calibration;

		public AbstractSpotEditBehaviour( final Model model, final ImagePlus imp )
		{
			this.model = model;
			this.imp = imp;
			this.calibration = TMUtils.getSpatialCalibration( imp );
		}

		protected Spot getSpotAtMouseLocation( final RealLocalizable pos )
		{
			final int frame = imp.getFrame() - 1;
			return model.getSpots().getSpotAt( pos, frame, true );
		}

		protected RealLocalizable toWorldCoords( final int x, final int y )
		{
			final ImageCanvas canvas = imp.getCanvas();
			final double xw = ( -0.5 + canvas.offScreenXD( x ) ) * calibration[ 0 ];
			final double yw = ( -0.5 + canvas.offScreenYD( y ) ) * calibration[ 1 ];
			final double zw = ( imp.getSlice() - 1 ) * calibration[ 2 ];
			return new RealPoint( xw, yw, zw );
		}
	}

	private static class MoveSpotBehaviour extends AbstractSpotEditBehaviour implements DragBehaviour
	{

		/** Offset between mouse click and spot center, in world coordinates. */
		private final double[] delta = new double[ 2 ];

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
}
