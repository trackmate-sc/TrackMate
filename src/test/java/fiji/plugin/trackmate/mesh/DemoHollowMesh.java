package fiji.plugin.trackmate.mesh;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.ThresholdDetectorFactory;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.util.SpotUtil;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import net.imagej.ImgPlus;
import net.imglib2.RealPoint;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class DemoHollowMesh
{

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		final ImagePlus imp = makeImg();

		final Settings settings = new Settings( imp );
		settings.detectorFactory = new ThresholdDetectorFactory<>();
		settings.detectorSettings = settings.detectorFactory.getDefaultSettings();
		settings.detectorSettings.put( ThresholdDetectorFactory.KEY_INTENSITY_THRESHOLD, 120. );
		settings.detectorSettings.put( ThresholdDetectorFactory.KEY_SIMPLIFY_CONTOURS, false );

		final TrackMate trackmate = new TrackMate( settings );
		trackmate.execDetection();

		final Model model = trackmate.getModel();
		model.getSpots().setVisible( true );
		final SelectionModel selection = new SelectionModel( model );
		final DisplaySettings ds = DisplaySettingsIO.readUserDefault();

		final HyperStackDisplayer view = new HyperStackDisplayer( model, selection, imp, ds );
		view.render();
	}

	public static ImagePlus makeImg()
	{
		final ImagePlus imp = NewImage.createByteImage( "Hollow", 256, 256, 256, NewImage.FILL_BLACK );
		final ImgPlus< UnsignedByteType > img = TMUtils.rawWraps( imp );

		final RealPoint center = RealPoint.wrap( new double[] {
				imp.getWidth() / 2.,
				imp.getHeight() / 2.,
				imp.getNSlices() / 2.
		} );
		final double r1 = imp.getWidth() / 4.;
		final double r2 = imp.getWidth() / 8.;
		final double r3 = imp.getWidth() / 16.;
		final Spot s1 = new Spot( center, r1, 1. );
		final Spot s2 = new Spot( center, r2, 1. );
		final Spot s3 = new Spot( center, r3, 1. );
		SpotUtil.iterable( s1, img ).forEach( p -> p.setReal( 250. ) );
		SpotUtil.iterable( s2, img ).forEach( p -> p.setZero() );
		SpotUtil.iterable( s3, img ).forEach( p -> p.setReal( 250. ) );
		return imp;
	}
}
