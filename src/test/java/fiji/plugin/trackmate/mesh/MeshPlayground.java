/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.mesh;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.joml.Matrix4f;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;

import bvv.core.VolumeViewerPanel;
import bvv.core.util.MatrixMath;
import bvv.vistools.Bvv;
import bvv.vistools.BvvFunctions;
import bvv.vistools.BvvSource;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.bvv.StupidMesh;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.mesh.Mesh;
import net.imglib2.mesh.Meshes;
import net.imglib2.mesh.impl.naive.NaiveDoubleMesh;
import net.imglib2.mesh.impl.nio.BufferMesh;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;

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
		for ( int j = 1; j <= 3; ++j )
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
				final Matrix4f view = MatrixMath.affine( data.getRenderTransformWorldToScreen(), new Matrix4f() );
				final Matrix4f vm = MatrixMath.screen( data.getDCam(), data.getScreenWidth(), data.getScreenHeight(), new Matrix4f() ).mul( view );
				meshes.forEach( mesh -> mesh.draw( gl, pvm, vm, false ) );
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
			net.imglib2.mesh.io.stl.STLMeshIO.read( nmesh, new File( fn ) );
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
		final int nvertices = mesh.vertices().size();
		final int ntriangles = mesh.triangles().size();
		final BufferMesh bufferMesh = new BufferMesh( nvertices, ntriangles, true );
		Meshes.calculateNormals( mesh, bufferMesh );
		return bufferMesh;
	}
}
