package fiji.plugin.trackmate.mesh;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.joml.Matrix4f;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;

import bvv.util.Bvv;
import bvv.util.BvvFunctions;
import bvv.util.BvvSource;
import fiji.plugin.trackmate.util.TMUtils;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Meshes;
import net.imagej.mesh.io.stl.STLMeshIO;
import net.imagej.mesh.naive.NaiveDoubleMesh;
import net.imagej.mesh.nio.BufferMesh;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;
import tpietzsch.example2.VolumeViewerPanel;
import tpietzsch.scene.mesh.StupidMesh;

public class MeshPlayground
{
	public static < T extends Type< T > > void main( final String[] args )
	{
		final String filePath = "samples/mesh/CElegansMask3D.tif";
		final ImagePlus imp = IJ.openImage( filePath );

		final ImgPlus< T > img = TMUtils.rawWraps( imp );
		final ImgPlus< T > c1 = ImgPlusViews.hyperSlice( img, img.dimensionIndex( Axes.CHANNEL ), 1 );
		final ImgPlus< T > t1 = ImgPlusViews.hyperSlice( c1, c1.dimensionIndex( Axes.TIME ), 0 );
		final double[] cal = TMUtils.getSpatialCalibration( t1 );

		final BvvSource source = BvvFunctions.show( c1, "t1",
				Bvv.options()
						.maxAllowedStepInVoxels( 0 )
						.renderWidth( 1024 )
						.renderHeight( 1024 )
						.preferredSize( 512, 512 )
						.sourceTransform( cal ) );

		source.setDisplayRangeBounds( 0, 1024 );
		source.setColor( new ARGBType( 0xaaffaa ) );


		final List< StupidMesh > meshes = new ArrayList<>();
		for ( int j = 1; j <= 3; ++j)
		{
			final String fn = String.format( "samples/mesh/CElegansMask3D_%02d.stl", j );
			meshes.add( new StupidMesh( load( fn ) ) );
		}

		final VolumeViewerPanel viewer = source.getBvvHandle().getViewerPanel();

		final AtomicBoolean showMeshes = new AtomicBoolean( true );
		viewer.setRenderScene( ( gl, data ) -> {
			if ( showMeshes.get() )
			{
				final Matrix4f pvm = new Matrix4f( data.getPv() );
				final Matrix4f vm = new Matrix4f( data.getCamview() );
				meshes.forEach( mesh -> mesh.draw( gl, pvm, vm ) );
			}
		} );

		final Actions actions = new Actions( new InputTriggerConfig() );
		actions.install( source.getBvvHandle().getKeybindings(), "my-new-actions" );
		actions.runnableAction( () -> {
			showMeshes.set( !showMeshes.get() );
			viewer.requestRepaint();
		}, "toggle meshes", "G" );

		viewer.requestRepaint();
	}


	private static BufferMesh load( final String fn )
	{
		BufferMesh mesh = null;
		try
		{
			final NaiveDoubleMesh nmesh = new NaiveDoubleMesh();
			final STLMeshIO meshIO = new STLMeshIO();
			meshIO.read( nmesh, new File( fn ) );
			mesh = calculateNormals(
					nmesh
//					Meshes.removeDuplicateVertices( nmesh, 5 )
			);
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
		return mesh;
	}

	private static BufferMesh calculateNormals( final Mesh mesh )
	{
		final int nvertices = ( int ) mesh.vertices().size();
		final int ntriangles = ( int ) mesh.triangles().size();
		final BufferMesh bufferMesh = new BufferMesh( nvertices, ntriangles, true );
		Meshes.calculateNormals( mesh, bufferMesh );
		return bufferMesh;
	}
}