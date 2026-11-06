package fiji.plugin.trackmate.visualization.hyperstack.behaviours;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.TMUtils;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.RoiListener;
import ij.plugin.PlugIn;

public class SelectSpotsWithRoiListener implements PlugIn, RoiListener
{

	private final Model model;

	private final SelectionModel selectionModel;

	private final ImagePlus sourceImp;

	private final double[] calibration;

	public SelectSpotsWithRoiListener( final Model model, final SelectionModel selectionModel, final ImagePlus imp )
	{
		this.model = model;
		this.selectionModel = selectionModel;
		this.sourceImp = imp;
		this.calibration = TMUtils.getSpatialCalibration( sourceImp );
		
		// De-register the listener when the source image is closed
		sourceImp.getWindow().addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				Roi.removeRoiListener( SelectSpotsWithRoiListener.this );
			}
		} );
	}

	@Override
	public void run( final String arg )
	{
		Roi.addRoiListener( this );
	}

	/*
	 * NOTE: For some reason this does not work as expected. For instance, the
	 * rectangular ROI does not fire a RoiListener.COMPLETED when the user
	 * finishes drawing it. The PolygonRoi does fire this event, but the
	 * containsPoint() method does not work. So in effect, this feature only
	 * works with the FreehandRoi.
	 */
	@Override
	public void roiModified( final ImagePlus imp, final int id )
	{
		if ( imp != sourceImp || id != RoiListener.COMPLETED )
			return;

		final Roi roi = imp.getRoi();
		if ( roi == null )
			return;

		final List< Spot > spotsInRoi = new ArrayList<>();

		final Iterable<Spot> iterable; 
		final boolean isShiftDown = ( imp.getCanvas().getModifiers() & 1 ) != 0;
		if ( isShiftDown )
		{
			final int frame = imp.getT() - 1;
			iterable = model.getSpots().iterable( frame, true );
		}
		else
		{
			iterable = model.getSpots().iterable( true );
		}
		
		for ( final Spot spot : iterable )
		{
			final double x = spot.getDoublePosition( 0 ) / calibration[ 0 ];
			final double y = spot.getDoublePosition( 1 ) / calibration[ 1 ];
			if ( roi.containsPoint( x, y ) )
				spotsInRoi.add( spot );
		}
		selectionModel.addSpotToSelection( spotsInRoi );
	}

	public static void install( final Model model, final SelectionModel selectionModel, final ImagePlus imp )
	{
		new SelectSpotsWithRoiListener( model, selectionModel, imp ).run( null );
	}
}
