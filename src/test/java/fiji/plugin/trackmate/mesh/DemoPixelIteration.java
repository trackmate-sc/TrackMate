package fiji.plugin.trackmate.mesh;

import java.awt.Color;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.ThresholdDetectorFactory;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.util.mesh.SpotMeshCursor;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.process.LUT;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.type.numeric.RealType;

public class DemoPixelIteration
{

	@SuppressWarnings( "unchecked" )
	public static < T extends RealType< T > > void main( final String[] args )
	{
		try
		{
			ImageJ.main( args );

//			final Mesh mesh = Demo3DMesh.debugMesh( new long[] { 4, 4, 4 }, new long[] { 10, 10, 10 } );
//			final Spot s0 = SpotMesh.createSpot( mesh, 1. );
//			final Model model = new Model();
//			model.beginUpdate();
//			try
//			{
//				model.addSpotTo( s0, 0 );
//			}
//			finally
//			{
//				model.endUpdate();
//			}
//			final ImagePlus imp = NewImage.createByteImage( "cube", 16, 16, 16, NewImage.FILL_BLACK );

			final String imPath = "samples/mesh/CElegansMask3DNoScale-mask-t1.tif";
			final ImagePlus imp = IJ.openImage( imPath );

			final Settings settings = new Settings( imp );
			settings.detectorFactory = new ThresholdDetectorFactory<>();
			settings.detectorSettings = settings.detectorFactory.getDefaultSettings();
			settings.detectorSettings.put(
					ThresholdDetectorFactory.KEY_SIMPLIFY_CONTOURS, false );
			settings.detectorSettings.put(
					ThresholdDetectorFactory.KEY_INTENSITY_THRESHOLD, 100. );

			final TrackMate trackmate = new TrackMate( settings );
			trackmate.setNumThreads( 4 );
			trackmate.execDetection();

			final Model model = trackmate.getModel();
			final SpotCollection spots = model.getSpots();
			spots.setVisible( true );

			final ImagePlus out = NewImage.createShortImage( "OUT", imp.getWidth(), imp.getHeight(), imp.getNSlices(), NewImage.FILL_BLACK );
			out.show();
			out.resetDisplayRange();

			imp.show();
			imp.resetDisplayRange();

			final double[] cal = TMUtils.getSpatialCalibration( imp );
			int i = 0;
			for ( final Spot spot : model.getSpots().iterable( true ) )
			{
				System.out.println( spot );
				final Cursor< T > cursor = new SpotMeshCursor< T >( TMUtils.rawWraps( out ).randomAccess(), spot.getMesh(), cal );
				final RandomAccess< T > ra = TMUtils.rawWraps( imp ).randomAccess();
				while ( cursor.hasNext() )
				{
					cursor.fwd();
					cursor.get().setReal( 1 + i++ );

					ra.setPosition( cursor );
					ra.get().setReal( 100 );
				}
			}

			final SelectionModel sm = new SelectionModel( model );
			final DisplaySettings ds = DisplaySettingsIO.readUserDefault();
			final HyperStackDisplayer view = new HyperStackDisplayer( model, sm, imp, ds );
			view.render();

			imp.setSlice( 19 );
			imp.resetDisplayRange();
			imp.setLut( LUT.createLutFromColor( Color.BLUE ) );
			out.setSlice( 19 );
			out.resetDisplayRange();
			System.out.println( "Done." );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}
}
