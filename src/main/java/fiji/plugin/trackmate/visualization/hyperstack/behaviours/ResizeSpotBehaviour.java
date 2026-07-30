package fiji.plugin.trackmate.visualization.hyperstack.behaviours;

import org.scijava.ui.behaviour.ClickBehaviour;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import ij.ImagePlus;

public class ResizeSpotBehaviour extends AbstractSpotEditBehaviour implements ClickBehaviour
{

	/**
	 * Fall back default radius when the settings does not give a default radius
	 * to use.
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
