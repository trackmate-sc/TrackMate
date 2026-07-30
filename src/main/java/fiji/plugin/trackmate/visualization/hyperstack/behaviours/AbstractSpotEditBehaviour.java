package fiji.plugin.trackmate.visualization.hyperstack.behaviours;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.TMUtils;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;

public class AbstractSpotEditBehaviour
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
