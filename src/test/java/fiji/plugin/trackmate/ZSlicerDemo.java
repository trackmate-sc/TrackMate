package fiji.plugin.trackmate;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Random;

import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.plugin.Duplicator;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.MeshConnectedComponents;
import net.imagej.mesh.Meshes;
import net.imagej.mesh.nio.BufferMesh;
import net.imagej.mesh.zslicer.ZSlicer;
import net.imagej.mesh.zslicer.ZSlicer.Contour;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Cast;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class ZSlicerDemo
{
	public static < T extends RealType< T > > void main( final String[] args ) throws IOException, URISyntaxException
	{
		try
		{
			ImageJ.main( args );
			System.out.println( "Opening image." );
			final String filePath = "samples/CElegans3D-smoothed-mask-orig-t7.tif";
			final ImagePlus imp = IJ.openImage( filePath );
			imp.show();
			final ImgPlus< T > img = Cast.unchecked( TMUtils.rawWraps( imp ) );
			final double[] pixelSizes = new double[] {
					img.averageScale( img.dimensionIndex( Axes.X ) ),
					img.averageScale( img.dimensionIndex( Axes.Y ) ),
					img.averageScale( img.dimensionIndex( Axes.Z ) ) };
			System.out.println( Util.printCoordinates( pixelSizes ) );

			// First channel is the smoothed version.
			System.out.println( "Marching cube on grayscale." );
			final RandomAccessibleInterval< T > smoothed;
			if ( img.dimensionIndex( Axes.CHANNEL ) >= 0 )
				smoothed = Views.hyperSlice( img, img.dimensionIndex( Axes.CHANNEL ), 0 );
			else
				smoothed = img;

			final double isoLevel = 250;
			final Mesh mesh1 = Meshes.marchingCubes( smoothed, isoLevel );
			final double z = 11.0;
			runMesh( imp, mesh1, z, pixelSizes, filePath, "-grayscale" );

			System.out.println( "Finished!" );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}

	private static void runMesh( final ImagePlus imp, Mesh mesh, final double z, final double[] pixelSizes, final String filePath, final String suffix ) throws IOException
	{
		final ImagePlus out = new Duplicator().run( imp, 1, 1, ( int ) z + 1, ( int ) z + 1, 1, 1 );
		out.show();

		System.out.println( "Before removing duplicates: " + mesh );
		mesh = Meshes.removeDuplicateVertices( mesh, 2 );
		System.out.println( "After removing duplicates: " + mesh );
		System.out.println( "Scaling." );
		Meshes.scale( mesh, pixelSizes );

		System.out.println( "N connected components: " + Meshes.nConnectedComponents( mesh ) );
		System.out.println( "Splitting in connected components:" );
		int i = 0;
		final Overlay overlay = new Overlay();
		out.setOverlay( overlay );
		final Random ran = new Random( 2l );

		final Model model = new Model();
		model.beginUpdate();
		try
		{

			for ( final BufferMesh cc : MeshConnectedComponents.iterable( mesh ) )
			{
				i++;
				System.out.println( " # " + i + ": " + cc );
//			new PLYMeshIO().save( cc, filePath + suffix + "-" + i + ".ply" );

				final double tolerance = 1e-3 * pixelSizes[ 0 ];
				final List< Contour > contours = ZSlicer.slice( cc, z, tolerance );
				for ( final Contour contour : contours )
				{
//					final float[] xp = new float[ contour.x.size() ];
//					final float[] yp = new float[ xp.length ];
//					for ( int j = 0; j < xp.length; j++ )
//					{
//						xp[ j ] = ( float ) ( 0.5 + contour.x.getQuick( j ) / pixelSizes[ 0 ] );
//						yp[ j ] = ( float ) ( 0.5 + contour.y.getQuick( j ) / pixelSizes[ 1 ] );
//					}
//					final PolygonRoi roi = new PolygonRoi( xp, yp, PolygonRoi.POLYGON );
//					roi.setStrokeColor( new Color( 0.5f * ( 1 + ran.nextFloat() ),
//							0.5f * ( 1 + ran.nextFloat() ),
//							0.5f * ( 1 + ran.nextFloat() ) ) );
//					overlay.add( roi );
//					System.out.println( roi ); // DEBUG

					final Spot spot = SpotRoi.createSpot( contour.xScaled( 1. ), contour.yScaled( 1. ), 1. );
					model.addSpotTo( spot, 0 );

//					System.out.println( Util.printCoordinates( spot.getRoi().x ) ); // DEBUG
//					System.out.println( Util.printCoordinates( spot ) ); // DEBUG
				}
				model.getSpots().setVisible( true );



//			break;
			}
//		System.out.println( "Simplifying to 10%:" );
//		i = 0;
//		for ( final BufferMesh cc : MeshConnectedComponents.iterable( mesh ) )
//		{
//			i++;
//			final Mesh simplified = Meshes.simplify( cc, 0.1f, 10 );
//			System.out.println( " # " + i + ": " + simplified );
//			new PLYMeshIO().save( simplified, filePath + suffix + "-simplified-" + i + ".ply" );
//		}

		}
		finally
		{
			model.endUpdate();
		}
		final SelectionModel selectionModel = new SelectionModel( model );
		final DisplaySettings ds = DisplaySettingsIO.readUserDefault();
		ds.setSpotDisplayedAsRoi( true );
		final HyperStackDisplayer view = new HyperStackDisplayer( model, selectionModel, out, ds );
		view.render();
		view.refresh();
		System.out.println( model.getSpots() ); // DEBUG
		System.out.println();
	}
}
