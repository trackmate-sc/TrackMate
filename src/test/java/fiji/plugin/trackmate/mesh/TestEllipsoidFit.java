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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import net.imglib2.mesh.Mesh;
import net.imglib2.mesh.alg.EllipsoidFitter;
import net.imglib2.mesh.alg.EllipsoidFitter.Ellipsoid;
import net.imglib2.mesh.impl.naive.NaiveDoubleMesh;

public class TestEllipsoidFit
{

	@Test
	public void testSimpleEllipsoids()
	{
		final double TOLERANCE = 1e-6;
		
		final double ra = 1.;
		final double rb = 2.;
		for ( double rc = 3.; rc < 10.; rc++ )
		{
			final Mesh mesh = generateEllipsoidMesh( ra, rb, rc, -1, -1 );
			final Ellipsoid fit = EllipsoidFitter.fit( mesh );

			final double[] arr = new double[ 3 ];

			// Center on 0.
			fit.center.localize( arr );
			assertArrayEquals( "Ellipsoid center should be close to 0.", arr, new double[] { 0., 0., 0. }, TOLERANCE );

			// Proper radius, ordered by increasing absolute value.
			assertEquals( "Smallest radius has unexpected value.", ra, fit.r1, TOLERANCE );
			assertEquals( "Mid radius has unexpected value.", rb, fit.r2, TOLERANCE );
			assertEquals( "Largest radius has unexpected value.", rc, fit.r3, TOLERANCE );

			// Vectors, aligned with axes.
			fit.ev1.localize( arr );
			for ( int d = 0; d < arr.length; d++ )
				arr[ d ] = Math.abs( arr[ d ] );
			assertArrayEquals( "Smallest eigenvector should be aligned with X axis.", arr, new double[] { 1., 0., 0. }, TOLERANCE );

			fit.ev2.localize( arr );
			for ( int d = 0; d < arr.length; d++ )
				arr[ d ] = Math.abs( arr[ d ] );
			assertArrayEquals( "Mid eigenvector should be aligned with Y axis.", arr, new double[] { 0., 1., 0. }, TOLERANCE );

			fit.ev3.localize( arr );
			for ( int d = 0; d < arr.length; d++ )
				arr[ d ] = Math.abs( arr[ d ] );
			assertArrayEquals( "Largest eigenvector should be aligned with Z axis.", arr, new double[] { 0., 0., 1. }, TOLERANCE );
		}
	}

	private static Mesh generateEllipsoidMesh( final double ra, final double rb, final double rc, int numLongitudes, int numLatitudes )
	{
		if ( numLongitudes < 4 )
			numLongitudes = 36; // Number of longitudinal divisions
		if ( numLatitudes < 4 )
			numLatitudes = 18; // Number of latitudinal divisions

		final NaiveDoubleMesh mesh = new NaiveDoubleMesh();
		for ( int lat = 0; lat < numLatitudes; lat++ )
		{
			final double theta1 = ( double ) lat / numLatitudes * Math.PI;
			final double theta2 = ( double ) ( lat + 1 ) / numLatitudes * Math.PI;

			for ( int lon = 0; lon < numLongitudes; lon++ )
			{
				final double phi1 = ( double ) lon / numLongitudes * 2 * Math.PI;
				final double phi2 = ( double ) ( lon + 1 ) / numLongitudes * 2 * Math.PI;

				// Calculate the vertices of each triangle
				final long p1 = addVertex( mesh, ra, rb, rc, theta1, phi1 );
				final long p2 = addVertex( mesh, ra, rb, rc, theta1, phi2 );
				final long p3 = addVertex( mesh, ra, rb, rc, theta2, phi1 );
				final long p4 = addVertex( mesh, ra, rb, rc, theta2, phi2 );

				// Draw the triangles
				addTriangle( mesh, p1, p3, p2 );
				addTriangle( mesh, p2, p3, p4 );
			}
		}
		return mesh;
	}

	private static long addVertex( final Mesh mesh, final double ra, final double rb, final double rc, final double theta, final double phi )
	{
		final double x = ra * Math.sin( theta ) * Math.cos( phi );
		final double y = rb * Math.sin( theta ) * Math.sin( phi );
		final double z = rc * Math.cos( theta );
		return mesh.vertices().add( x, y, z );
	}

	private static long addTriangle( final Mesh mesh, final long p1, final long p2, final long p3 )
	{
		return mesh.triangles().add( p1, p2, p3 );
	}
}
