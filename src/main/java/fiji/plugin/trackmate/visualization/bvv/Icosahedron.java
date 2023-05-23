package fiji.plugin.trackmate.visualization.bvv;

import fiji.plugin.trackmate.Spot;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Meshes;
import net.imagej.mesh.Triangle;
import net.imagej.mesh.naive.NaiveDoubleMesh;
import net.imagej.mesh.nio.BufferMesh;
import net.imglib2.RealLocalizable;

/**
 * Icosahedron spheres.
 * <p>
 * Based on https://github.com/caosdoar/spheres
 * 
 * @author Jean-Yves Tinevez
 */
public class Icosahedron
{

	public static final Mesh core()
	{
		final NaiveDoubleMesh mesh = new NaiveDoubleMesh();

		// Vertices
		final double t = ( 1. + Math.sqrt( 5. ) ) / 2.;
		final double[][] vs = new double[][] {
				{ -1.0, t, 0.0 },
				{ 1.0, t, 0.0 },
				{ -1.0, -t, 0.0 },
				{ 1.0, -t, 0.0 },
				{ 0.0, -1.0, t },
				{ 0.0, 1.0, t },
				{ 0.0, -1.0, -t },
				{ 0.0, 1.0, -t },
				{ t, 0.0, -1.0 },
				{ t, 0.0, 1.0 },
				{ -t, 0.0, -1.0 },
				{ -t, 0.0, 1.0 }
		};
		final double[] tmp = new double[ 3 ];
		for ( final double[] v : vs )
		{
			normalize( v, tmp );
			mesh.vertices().add( tmp[ 0 ], tmp[ 1 ], tmp[ 2 ] );
		}

		// Faces
		mesh.triangles().add( 0, 11, 5 );
		mesh.triangles().add( 0, 5, 1 );
		mesh.triangles().add( 0, 1, 7 );
		mesh.triangles().add( 0, 7, 10 );
		mesh.triangles().add( 0, 10, 11 );
		mesh.triangles().add( 1, 5, 9 );
		mesh.triangles().add( 5, 11, 4 );
		mesh.triangles().add( 11, 10, 2 );
		mesh.triangles().add( 10, 7, 6 );
		mesh.triangles().add( 7, 1, 8 );
		mesh.triangles().add( 3, 9, 4 );
		mesh.triangles().add( 3, 4, 2 );
		mesh.triangles().add( 3, 2, 6 );
		mesh.triangles().add( 3, 6, 8 );
		mesh.triangles().add( 3, 8, 9 );
		mesh.triangles().add( 4, 9, 5 );
		mesh.triangles().add( 2, 4, 11 );
		mesh.triangles().add( 6, 2, 10 );
		mesh.triangles().add( 8, 6, 7 );
		mesh.triangles().add( 9, 8, 1 );
		return mesh;
	}

	public static final BufferMesh refine( final Mesh core )
	{
		final int nVerticesOut = ( int ) ( 6 * core.triangles().size() );
		final int nTrianglesOut = ( int ) ( 4 * core.triangles().size() );
		final BufferMesh out = new BufferMesh( nVerticesOut, nTrianglesOut );

		final double[] tmpIn = new double[ 3 ];
		final double[] tmpOut = new double[ 3 ];
		for ( final Triangle t : core.triangles() )
		{
			final long v0 = out.vertices().add( t.v0x(), t.v0y(), t.v0z() );
			final long v1 = out.vertices().add( t.v1x(), t.v1y(), t.v1z() );
			final long v2 = out.vertices().add( t.v2x(), t.v2y(), t.v2z() );

			tmpIn[ 0 ] = 0.5 * ( t.v0xf() + t.v1xf() );
			tmpIn[ 1 ] = 0.5 * ( t.v0yf() + t.v1yf() );
			tmpIn[ 2 ] = 0.5 * ( t.v0zf() + t.v1zf() );
			normalize( tmpIn, tmpOut );
			final long v3 = out.vertices().add( tmpOut[ 0 ], tmpOut[ 1 ], tmpOut[ 2 ] );

			tmpIn[ 0 ] = 0.5 * ( t.v2xf() + t.v1xf() );
			tmpIn[ 1 ] = 0.5 * ( t.v2yf() + t.v1yf() );
			tmpIn[ 2 ] = 0.5 * ( t.v2zf() + t.v1zf() );
			normalize( tmpIn, tmpOut );
			final long v4 = out.vertices().add( tmpOut[ 0 ], tmpOut[ 1 ], tmpOut[ 2 ] );

			tmpIn[ 0 ] = 0.5 * ( t.v0xf() + t.v2xf() );
			tmpIn[ 1 ] = 0.5 * ( t.v0yf() + t.v2yf() );
			tmpIn[ 2 ] = 0.5 * ( t.v0zf() + t.v2zf() );
			normalize( tmpIn, tmpOut );
			final long v5 = out.vertices().add( tmpOut[ 0 ], tmpOut[ 1 ], tmpOut[ 2 ] );

			out.triangles().add( v0, v3, v5 );
			out.triangles().add( v3, v1, v4 );
			out.triangles().add( v5, v4, v2 );
			out.triangles().add( v3, v4, v5 );
		}

		return out;
	}

	public static BufferMesh sphere( final Spot spot )
	{
		return sphere( spot, spot.getFeature( Spot.RADIUS ) );
	}

	public static BufferMesh sphere( final RealLocalizable center, final double radius )
	{
		return sphere( center, radius, 3 );
	}

	public static BufferMesh sphere( final RealLocalizable center, final double radius, final int nSubdivisions )
	{
		Mesh mesh = core();
		for ( int i = 0; i < nSubdivisions; i++ )
			mesh = refine( mesh );

		scale( mesh, center, radius );
		final BufferMesh out = new BufferMesh( ( int ) mesh.vertices().size(), ( int ) mesh.triangles().size() );
		Meshes.calculateNormals( mesh, out );
		return out;
	}

	private static void scale( final Mesh mesh, final RealLocalizable center, final double radius )
	{
		final long nV = mesh.vertices().size();
		for ( int i = 0; i < nV; i++ )
		{
			final double x = mesh.vertices().x( i ) * radius + center.getDoublePosition( 0 );
			final double y = mesh.vertices().y( i ) * radius + center.getDoublePosition( 1 );
			final double z = mesh.vertices().z( i ) * radius + center.getDoublePosition( 2 );
			mesh.vertices().set( i, x, y, z );
		}
	}

	private static void normalize( final double[] v, final double[] tmp )
	{
		final double l = Math.sqrt( v[ 0 ] * v[ 0 ] + v[ 1 ] * v[ 1 ] + v[ 2 ] * v[ 2 ] );
		tmp[ 0 ] = v[ 0 ] / l;
		tmp[ 1 ] = v[ 1 ] / l;
		tmp[ 2 ] = v[ 2 ] / l;
	}
}
