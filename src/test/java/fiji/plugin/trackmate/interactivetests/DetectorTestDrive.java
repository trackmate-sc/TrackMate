package fiji.plugin.trackmate.interactivetests;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.LogDetector;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.SpotColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.ImageJ;
import ij.ImagePlus;

import java.io.File;

import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class DetectorTestDrive
{
	public static <T extends RealType< T > & NativeType< T >>void main( final String[] args )
	{
		ImageJ.main( args );

		// @SuppressWarnings( "unchecked" )
		// final Img< T > img = ( Img< T > ) ArrayImgs.unsignedBytes( 64, 64, 64
		// );
		// final HyperSphere< T > sphere = new HyperSphere< T >( img, new Point(
		// 32, 32, 32 ), 5 );
		// for ( final T t : sphere )
		// {
		// t.setReal( 255d );
		// }
		//
		// final ImagePlus imp = ImageJFunctions.wrap( img, "" );

		final File file = new File( "/Users/tinevez/Desktop/Data/Celegans-5pc_17timepoints-1.tif" );
		final ImagePlus imp = new ImagePlus( file.getAbsolutePath() );
		final Img< T > img = ImageJFunctions.wrap( imp );
		final double[] calibration = TMUtils.getSpatialCalibration( imp );

//		final double radius = 2.5d;
//		final double[] calibration = new double[] { 0.2, 0.2 };
//		@SuppressWarnings( "unchecked" )
//		final Img< T > img = ( Img< T > ) ArrayImgs.unsignedBytes( 64, 64 );
//		final Spot cspot = new Spot( new double[] { 64 / 2 * calibration[ 0 ], 64 / 2 * calibration[ 1 ], 0 } );
//		cspot.putFeature( Spot.RADIUS, radius );
//		final ImgPlus< T > imgp = new ImgPlus< T >( img, "Ellipsoid", new AxisType[] { Axes.X, Axes.Y }, calibration );
//		final SpotNeighborhood< T > ellipsoid = new SpotNeighborhood< T >( cspot, imgp );
//		for ( final T t : ellipsoid )
//		{
//			t.setReal( 255d );
//		}
//		final ImagePlus imp = ImageJFunctions.wrap( imgp, "" );
//		imp.setDimensions( 1, imp.getStackSize(), 1 );

		final Model model = new Model();

		final HyperStackDisplayer viewer = new HyperStackDisplayer( model, new SelectionModel( model ), imp );
		viewer.render();


		System.out.println( "Radius\tQuality\tTime(ms)" );
		for ( int i = 2; i < 12; i++ )
		{
			final double tRadius = i / 2d;
			final LogDetector< T > detector = new LogDetector< >( img, img, calibration, tRadius, 100d, true, false );
			if ( !detector.checkInput() || !detector.process() )
			{
				System.err.println( detector.getErrorMessage() );
				return;
			}

			// ImageJFunctions.show( DetectionUtils.createLoGKernel( radius,
			// img.numDimensions(), calibration ), "" + i );

			model.beginUpdate();
			try
			{
				for ( final Spot spot : detector.getResult() )
				{
					model.getSpots().add( spot, 0 );
				}
			}
			finally
			{
				model.endUpdate();
			}

			// double quality = 0;
			// Spot bestSpot = null;
			// for ( final Spot spot : detector.getResult() )
			// {
			// if ( spot.getFeature( Spot.QUALITY ) > quality )
			// {
			// quality = spot.getFeature( Spot.QUALITY );
			// bestSpot = spot;
			// }
			// }
			//
			// model.beginUpdate();
			// try
			// {
			// if ( null != bestSpot )
			// {
			// model.getSpots().add( bestSpot, 0 );
			// System.out.println( bestSpot.getFeature( Spot.RADIUS ) + "\t" +
			// String.format( "%.1f", bestSpot.getFeature( Spot.QUALITY ) ) +
			// "\t" + detector.getProcessingTime() ); // +
			// }
			// }
			// finally
			// {
			// model.endUpdate();
			// }
		}

		final SpotColorGenerator colorer = new SpotColorGenerator( model );
		colorer.setFeature( Spot.QUALITY );
		viewer.setDisplaySettings( TrackMateModelView.KEY_SPOT_COLORING, colorer );
		viewer.refresh();


	}
}
