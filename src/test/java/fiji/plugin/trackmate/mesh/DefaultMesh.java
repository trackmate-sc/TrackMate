package fiji.plugin.trackmate.mesh;

import java.util.List;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.detection.ThresholdDetector;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.measure.Calibration;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.mesh.obj.Mesh;
import net.imglib2.type.logic.BitType;

public class DefaultMesh
{

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		final ImgPlus< BitType > img = Demo3DMesh.loadTestMask2();
		final ImagePlus imp = ImageJFunctions.show( img, "box" );
		imp.setDimensions(
				img.dimensionIndex( Axes.CHANNEL ),
				img.dimensionIndex( Axes.Z ),
				img.dimensionIndex( Axes.TIME ) );
		final double[] calibration = new double[] { 1., 1., 1. };

		final ThresholdDetector< BitType > detector = new ThresholdDetector< BitType >( img, img, calibration, 0, false );
		detector.process();
		final List< Spot > spots = detector.getResult();

		final Model model = new Model();
		for ( final Spot spot : spots )
		{
			model.getSpots().add( spot, 0 );
			System.out.println( spot );
		}

		final SelectionModel selectionModel = new SelectionModel( model );
		final DisplaySettings ds = DisplaySettingsIO.readUserDefault();
		final HyperStackDisplayer view = new HyperStackDisplayer( model, selectionModel, imp, ds );
		view.render();
	}

	public static void main2( final String[] args )
	{
		ImageJ.main( args );
		final ImagePlus imp = NewImage.createByteImage( "dummy",
				64, 64, 64, NewImage.FILL_RAMP );
		final Calibration cal = imp.getCalibration();
		cal.pixelWidth = 0.5;
		cal.pixelHeight = 0.5;
		cal.pixelDepth = 0.5;
		imp.show();

		final long[] min = new long[] { 2, 2, 2 };
		final long[] max = new long[] { 20, 20, 20 };
		final Mesh mesh = Demo3DMesh.debugMesh( min, max );
		final Spot spot = new SpotMesh( mesh, 1. );

		final Model model = new Model();
		model.beginUpdate();
		try
		{
			model.addSpotTo( spot, 0 );
		}
		finally
		{
			model.endUpdate();
		}

		final SelectionModel selectionModel = new SelectionModel( model );
		final DisplaySettings ds = DisplaySettingsIO.readUserDefault();
		final HyperStackDisplayer view = new HyperStackDisplayer( model, selectionModel, imp, ds );
		view.render();
	}
}
