package fiji.plugin.trackmate.mesh;

import java.util.Arrays;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TLongArrayList;
import net.imagej.mesh.Edges;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Meshes;
import net.imagej.mesh.Triangles;
import net.imagej.mesh.Vertices;

public class MeshPlaneIntersection
{

	/**
	 * Only works if the {@link Mesh} supports {@link Mesh#edges()}.
	 *
	 * @param mesh
	 * @param z
	 * @return
	 */
	public static void intersect(
			final Mesh mesh,
			final double z,
			final TDoubleArrayList cx,
			final TDoubleArrayList cy )
	{
		/*
		 * Clear contour holders.
		 */
		cx.resetQuick();
		cy.resetQuick();

		/*
		 * Check if bounding-box intersect. TODO: use a data structure where the
		 * bounding-box is a field, calculated once.
		 */
		final float[] bb = Meshes.boundingBox( mesh );
		if ( bb[ 2 ] > z )
			return;
		if ( bb[ 5 ] < z )
			return;

		/*
		 * Find one edge that crosses the Z plane.
		 */

		final Edges edges = mesh.edges();
		final Vertices vertices = mesh.vertices();
		final long nEdges = edges.size();

		long start = -1;
		for ( long e = 0; e < nEdges; e++ )
		{
			if ( edgeCrossPlane( vertices, edges, e, z ) )
			{
				// Edge is part of a face?
				final long f0 = edges.f0( e );
				if ( f0 >= 0 )
				{
					start = e;
					break;
				}
				// This edge has no face, we need another one.
			}
		}
		// Cannot build contour based on edge with no faces.
		if ( start < 0 )
			return;

		// Holder for the vertices of a triangle.
		final long[] vs = new long[ 3 ];
		// Holder for the 3 edges of a triangle.
		final long[] es = new long[ 3 ];

		long current = start;
		final long startTriangle = edges.f0( start );
		long previousTriangle = startTriangle;
		final TLongArrayList visited = new TLongArrayList();
//		final TLongHashSet visited = new TLongHashSet();
		visited.add( startTriangle );
		while ( true )
		{
			addEdgeToContour( vertices, edges, current, z, cx, cy );

			final long triangle = getNextTriangle( edges, current, previousTriangle );
			System.out.println( "At triangle: " + toString( mesh, triangle ) );

			if ( triangle < 0 || visited.contains( triangle ) )
				return;

			visited.add( triangle );
			final long next = getNextEdge( mesh, triangle, current, z, vs, es );

			if ( next < 0 || next == start )
				return;

			previousTriangle = triangle;
			current = next;
		}
	}

	private static String toString( final Mesh mesh, final long triangle )
	{
		// TODO Auto-generated method stub
		return null;
	}

	private static boolean edgeCrossPlane( final Vertices vertices, final Edges edges, final long e, final double z )
	{
		final double z0 = vertices.z( edges.v0( e ) );
		final double z1 = vertices.z( edges.v1( e ) );
		if ( z0 > z && z1 > z )
			return false;
		if ( z0 < z && z1 < z )
			return false;
		return true;
	}

	private static long getNextTriangle( final Edges edges, final long e, final long previousFace )
	{
		final long f0 = edges.f0( e );
		if ( f0 == previousFace )
			return edges.f1( e );
		return f0;
	}

	/**
	 * Returns the index of the edge in the specified triangle that crosses the
	 * plane with the specified z, and that is different from the specified
	 * current edge. Returns -1 is such an edge cannot be found for the
	 * specified triangle.
	 *
	 * @param mesh
	 *            the mesh structure.
	 * @param face
	 *            the triangle to inspect.
	 * @param current
	 *            the current edge, that should not be returned.
	 * @param z
	 *            the value of the Z plane.
	 * @param vs
	 *            holder for the vertices of the triangle (size at least 3).
	 * @param es
	 *            holder for the edges of the triangle (size at least 3).
	 * @return the index of the next edge.
	 */
	private static long getNextEdge(
			final Mesh mesh,
			final long face,
			final long current,
			final double z,
			final long[] vs,
			final long[] es )
	{
		final Triangles triangles = mesh.triangles();
		final Vertices vertices = mesh.vertices();
		final Edges edges = mesh.edges();

		// Get the edges of this face.
		vs[ 0 ] = triangles.vertex0( face );
		vs[ 1 ] = triangles.vertex1( face );
		vs[ 2 ] = triangles.vertex2( face );
		Arrays.sort( vs );
		es[ 0 ] = edges.indexOf( vs[ 0 ], vs[ 1 ] );
		es[ 1 ] = edges.indexOf( vs[ 0 ], vs[ 2 ] );
		es[ 2 ] = edges.indexOf( vs[ 1 ], vs[ 2 ] );
		for ( final long e : es )
		{
			if ( e == current )
				continue;

			if ( edgeCrossPlane( vertices, edges, e, z ) )
				return e;
		}
		return -1;

	}

	private static void addEdgeToContour(
			final Vertices vertices,
			final Edges edges,
			final long e,
			final double z,
			final TDoubleArrayList cx,
			final TDoubleArrayList cy )
	{
		final long sv = edges.v0( e );
		final long tv = edges.v1( e );
		final double xs = vertices.x( sv );
		final double ys = vertices.y( sv );
		final double zs = vertices.z( sv );
		final double xt = vertices.x( tv );
		final double yt = vertices.y( tv );
		final double zt = vertices.z( tv );
		final double t = ( zs == zt )
				? 0.5 : ( z - zs ) / ( zt - zs );
		final double x = xs + t * ( xt - xs );
		final double y = ys + t * ( yt - ys );
		final int np = cx.size();
		if ( np > 1 && cx.getQuick( np - 1 ) == x && cy.getQuick( np - 1 ) == y )
			return; // Don't add duplicate.

		cx.add( x );
		cy.add( y );
	}
}
