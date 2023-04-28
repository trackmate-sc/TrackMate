package fiji.plugin.trackmate.mesh;

import java.io.File;
import java.util.List;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.ImageJ;
import ij.ImagePlus;
import net.imagej.mesh.zslicer.ZSlicer;
import net.imagej.mesh.zslicer.ZSlicer.Contour;

public class DebugZSlicer
{
	public static void main( final String[] args )
	{
		try
		{
			ImageJ.main( args );

			final String filePath = "samples/CElegans3D-smoothed-mask-orig-t7.xml";
			final TmXmlReader reader = new TmXmlReader( new File( filePath ) );
			if ( !reader.isReadingOk() )
			{
				System.err.println( reader.getErrorMessage() );
				return;
			}

			final ImagePlus imp = reader.readImage();
			imp.show();
			final double[] calibration = TMUtils.getSpatialCalibration( imp );

			final Model model = reader.getModel();
			final SelectionModel selection = new SelectionModel( model );
			final DisplaySettings ds = reader.getDisplaySettings();

			final HyperStackDisplayer view = new HyperStackDisplayer( model, selection, imp, ds );
			view.render();

			final Spot spot = model.getSpots().iterable( true ).iterator().next();
			final double z = 14.;

			imp.setZ( ( int ) Math.round( z / calibration[ 2 ] ) + 1 );

			final double tolerance = 1e-3 * calibration[ 0 ];
			final List< Contour > contours = ZSlicer.slice( spot.getMesh().mesh, z, tolerance );
			System.out.println( "Found " + contours.size() + " contours." );
			int i = 0;
			for ( final Contour contour : contours )
			{
				System.out.println( "Contour " + ( ++i ) );
				System.out.println( contour ); // DEBUG
			}

		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}

}

