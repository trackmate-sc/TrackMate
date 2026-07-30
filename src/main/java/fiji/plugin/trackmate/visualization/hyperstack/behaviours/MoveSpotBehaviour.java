package fiji.plugin.trackmate.visualization.hyperstack.behaviours;

import org.scijava.ui.behaviour.DragBehaviour;

import fiji.plugin.trackmate.Model;
import ij.ImagePlus;
import net.imglib2.RealLocalizable;

public class MoveSpotBehaviour extends AbstractSpotEditBehaviour implements DragBehaviour
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
