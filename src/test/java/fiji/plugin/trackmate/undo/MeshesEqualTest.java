/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
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
package fiji.plugin.trackmate.undo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import net.imglib2.mesh.impl.nio.BufferMesh;

/**
 * Tests for the meshesEqual helper method in UndoRedoStack.
 */
public class MeshesEqualTest
{

	@Test
	public void testMeshesEqualIdenticalMeshes()
	{
		final BufferMesh mesh1 = createTetrahedronMesh();
		final BufferMesh mesh2 = createTetrahedronMesh();
		assertThat( meshesEqual( mesh1, mesh2 ) ).isTrue();
	}

	@Test
	public void testMeshesEqualSameReference()
	{
		final BufferMesh mesh = createTetrahedronMesh();
		assertThat( meshesEqual( mesh, mesh ) ).isTrue();
	}

	@Test
	public void testMeshesEqualBothNull()
	{
		assertThat( meshesEqual( null, null ) ).isTrue();
	}

	@Test
	public void testMeshesEqualOneNull()
	{
		final BufferMesh mesh = createTetrahedronMesh();
		assertThat( meshesEqual( mesh, null ) ).isFalse();
		assertThat( meshesEqual( null, mesh ) ).isFalse();
	}

	@Test
	public void testMeshesEqualDifferentVertexPosition()
	{
		final BufferMesh mesh1 = createTetrahedronMesh();
		final BufferMesh mesh2 = createTetrahedronMesh();
		mesh2.vertices().setPositionf( 0, 1.0f, 0.0f, 1.0f );
		assertThat( meshesEqual( mesh1, mesh2 ) ).isFalse();
	}

	@Test
	public void testMeshesEqualDifferentTriangle()
	{
		final BufferMesh mesh1 = createTetrahedronMesh();
		// Create mesh with different triangle connectivity
		final BufferMesh mesh2 = new BufferMesh( 4, 4 );
		mesh2.vertices().add( 0.0f, 0.0f, 1.0f );
		mesh2.vertices().add( 0.0f, 0.942809f, -0.333333f );
		mesh2.vertices().add( -0.816497f, -0.471405f, -0.333333f );
		mesh2.vertices().add( 0.816497f, -0.471405f, -0.333333f );
		// Different triangle connectivity (swapped vertex order)
		mesh2.triangles().add( 0, 2, 1 ); // Was: 0, 1, 2
		mesh2.triangles().add( 0, 3, 2 ); // Was: 0, 2, 3
		mesh2.triangles().add( 0, 1, 3 ); // Was: 0, 3, 1
		mesh2.triangles().add( 1, 2, 3 ); // Was: 1, 3, 2
		assertThat( meshesEqual( mesh1, mesh2 ) ).isFalse();
	}

	@Test
	public void testMeshesEqualDifferentVertexCount()
	{
		final BufferMesh mesh1 = createTetrahedronMesh();
		final BufferMesh mesh2 = new BufferMesh( 5, 4 );
		mesh2.vertices().add( 0.0f, 0.0f, 1.0f );
		mesh2.vertices().add( 0.0f, 0.942809f, -0.333333f );
		mesh2.vertices().add( -0.816497f, -0.471405f, -0.333333f );
		mesh2.vertices().add( 0.816497f, -0.471405f, -0.333333f );
		mesh2.vertices().add( 1.0f, 1.0f, 1.0f ); // Extra vertex
		mesh2.triangles().add( 0, 1, 2 );
		mesh2.triangles().add( 0, 2, 3 );
		mesh2.triangles().add( 0, 3, 1 );
		mesh2.triangles().add( 1, 3, 2 );
		assertThat( meshesEqual( mesh1, mesh2 ) ).isFalse();
	}

	@Test
	public void testMeshesEqualDifferentTriangleCount()
	{
		final BufferMesh mesh1 = createTetrahedronMesh();
		final BufferMesh mesh2 = new BufferMesh( 4, 3 );
		mesh2.vertices().add( 0.0f, 0.0f, 1.0f );
		mesh2.vertices().add( 0.0f, 0.942809f, -0.333333f );
		mesh2.vertices().add( -0.816497f, -0.471405f, -0.333333f );
		mesh2.vertices().add( 0.816497f, -0.471405f, -0.333333f );
		mesh2.triangles().add( 0, 1, 2 );
		mesh2.triangles().add( 0, 2, 3 );
		mesh2.triangles().add( 0, 3, 1 );
		// One less triangle
		assertThat( meshesEqual( mesh1, mesh2 ) ).isFalse();
	}

	/**
	 * Creates a simple tetrahedron mesh for testing.
	 */
	private static BufferMesh createTetrahedronMesh()
	{
		final BufferMesh mesh = new BufferMesh( 4, 4 );
		// 4 vertices of a tetrahedron
		mesh.vertices().add( 0.0f, 0.0f, 1.0f );
		mesh.vertices().add( 0.0f, 0.942809f, -0.333333f );
		mesh.vertices().add( -0.816497f, -0.471405f, -0.333333f );
		mesh.vertices().add( 0.816497f, -0.471405f, -0.333333f );
		// 4 triangles
		mesh.triangles().add( 0, 1, 2 );
		mesh.triangles().add( 0, 2, 3 );
		mesh.triangles().add( 0, 3, 1 );
		mesh.triangles().add( 1, 3, 2 );
		return mesh;
	}

	/**
	 * Copies the meshesEqual logic from UndoRedoStack for testing.
	 */
	private static boolean meshesEqual( final BufferMesh a, final BufferMesh b )
	{
		if ( a == null && b == null )
			return true;
		if ( a == null || b == null )
			return false;

		final net.imglib2.mesh.Vertices verticesA = a.vertices();
		final net.imglib2.mesh.Vertices verticesB = b.vertices();
		final long nVerticesA = verticesA.size();
		final long nVerticesB = verticesB.size();

		if ( nVerticesA != nVerticesB )
			return false;

		// Compare vertex positions
		for ( long i = 0; i < nVerticesA; i++ )
		{
			if ( Float.compare( verticesA.xf( i ), verticesB.xf( i ) ) != 0 ||
				Float.compare( verticesA.yf( i ), verticesB.yf( i ) ) != 0 ||
				Float.compare( verticesA.zf( i ), verticesB.zf( i ) ) != 0 )
			{
				return false;
			}
		}

		// Compare triangles
		final net.imglib2.mesh.Triangles trianglesA = a.triangles();
		final net.imglib2.mesh.Triangles trianglesB = b.triangles();
		final long nTrianglesA = trianglesA.size();
		final long nTrianglesB = trianglesB.size();

		if ( nTrianglesA != nTrianglesB )
			return false;

		for ( long i = 0; i < nTrianglesA; i++ )
		{
			if ( trianglesA.vertex0( i ) != trianglesB.vertex0( i ) ||
				trianglesA.vertex1( i ) != trianglesB.vertex1( i ) ||
				trianglesA.vertex2( i ) != trianglesB.vertex2( i ) )
			{
				return false;
			}
		}

		return true;
	}
}
