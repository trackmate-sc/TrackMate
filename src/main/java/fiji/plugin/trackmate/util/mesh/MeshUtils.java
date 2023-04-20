package fiji.plugin.trackmate.util.mesh;

import java.io.IOException;

import net.imagej.mesh.Mesh;
import net.imagej.mesh.Meshes;
import net.imagej.mesh.Triangles;
import net.imagej.mesh.Vertices;
import net.imagej.mesh.io.stl.STLMeshIO;
import net.imagej.mesh.nio.BufferMesh;

/**
 * A collection of small utilities to facilitate debugging issues related to
 * meshes in TrackMate.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class MeshUtils
{

	/**
	 * Saves a sub-mesh containing the specified triangles to a STL file.
	 *
	 * @param tl
	 *            the list of triangles (ids in the original mesh) to save.
	 * @param mesh
	 *            the original mesh.
	 * @param saveFilePath
	 *            a file path for a STL file.
	 */
	public static void exportMeshSubset( final long[] tl, final Mesh mesh, final String saveFilePath )
	{
		final Triangles triangles = mesh.triangles();
		final Vertices vertices = mesh.vertices();
		final BufferMesh out = new BufferMesh( tl.length * 3, tl.length );
		for ( int i = 0; i < tl.length; i++ )
		{
			final long id = tl[ i ];

			final long v0 = triangles.vertex0( id );
			final double x0 = vertices.x( v0 );
			final double y0 = vertices.y( v0 );
			final double z0 = vertices.z( v0 );
			final double v0nx = vertices.nx( v0 );
			final double v0ny = vertices.ny( v0 );
			final double v0nz = vertices.nz( v0 );
			final long nv0 = out.vertices().add( x0, y0, z0, v0nx, v0ny, v0nz, 0., 0. );

			final long v1 = triangles.vertex1( id );
			final double x1 = vertices.x( v1 );
			final double y1 = vertices.y( v1 );
			final double z1 = vertices.z( v1 );
			final double v1nx = vertices.nx( v1 );
			final double v1ny = vertices.ny( v1 );
			final double v1nz = vertices.nz( v1 );
			final long nv1 = out.vertices().add( x1, y1, z1, v1nx, v1ny, v1nz, 0., 0. );

			final long v2 = triangles.vertex2( id );
			final double x2 = vertices.x( v2 );
			final double y2 = vertices.y( v2 );
			final double z2 = vertices.z( v2 );
			final double v2nx = vertices.nx( v2 );
			final double v2ny = vertices.ny( v2 );
			final double v2nz = vertices.nz( v2 );
			final long nv2 = out.vertices().add( x2, y2, z2, v2nx, v2ny, v2nz, 0., 0. );

			final double nx = triangles.nx( id );
			final double ny = triangles.ny( id );
			final double nz = triangles.nz( id );

			out.triangles().add( nv0, nv1, nv2, nx, ny, nz );
		}
		Meshes.removeDuplicateVertices( out, 0 );

		final STLMeshIO io = new STLMeshIO();
		try
		{
			io.save( out, saveFilePath );
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
	}

	public static String triangleToString( final Mesh mesh, final long id )
	{
		final StringBuilder str = new StringBuilder( id + ": " );

		final Triangles triangles = mesh.triangles();
		final Vertices vertices = mesh.vertices();
		final long v0 = triangles.vertex0( id );
		final double x0 = vertices.x( v0 );
		final double y0 = vertices.y( v0 );
		final double z0 = vertices.z( v0 );
		str.append( String.format( "(%5.1f, %5.1f, %5.1f) - ", x0, y0, z0 ) );

		final long v1 = triangles.vertex1( id );
		final double x1 = vertices.x( v1 );
		final double y1 = vertices.y( v1 );
		final double z1 = vertices.z( v1 );
		str.append( String.format( "(%5.1f, %5.1f, %5.1f) - ", x1, y1, z1 ) );

		final long v2 = triangles.vertex2( id );
		final double x2 = vertices.x( v2 );
		final double y2 = vertices.y( v2 );
		final double z2 = vertices.z( v2 );
		str.append( String.format( "(%5.1f, %5.1f, %5.1f) - ", x2, y2, z2 ) );

		str.append( String.format( "N = (%4.2f, %4.2f, %4.2f) ",
				triangles.nx( id ), triangles.nz( id ), triangles.nz( id ) ) );

		return str.toString();
	}

}

