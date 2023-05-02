package fiji.plugin.trackmate;

import net.imagej.mesh.Mesh;
import net.imagej.mesh.Meshes;
import net.imagej.mesh.Triangles;
import net.imagej.mesh.Vertices;
import net.imagej.mesh.nio.BufferMesh;
import net.imglib2.RealPoint;

public class SpotMesh implements SpotShape
{

	/**
	 * The mesh representing the 3D contour of the spot. The mesh is centered on
	 * (0, 0, 0) and the true position of its vertices is obtained by adding the
	 * spot center.
	 */
	public final Mesh mesh;

	/**
	 * The bounding-box, <b>centered on (0,0,0)</b> of this object.
	 */
	public float[] boundingBox;

	public SpotMesh( final Mesh mesh, final float[] boundingBox )
	{
		this.mesh = mesh;
		this.boundingBox = boundingBox;
	}

	/**
	 * Creates a spot representing a 3D object, with the mesh specifying its
	 * position and shape.
	 * <p>
	 * <b>Warning</b>: the specified mesh is modified and wrapped in the spot.
	 *
	 * @param mesh
	 *            the mesh.
	 * @param quality
	 *            the spot quality.
	 * @return a new {@link Spot}.
	 */
	public static Spot createSpot( final Mesh mesh, final double quality )
	{
		final RealPoint center = Meshes.center( mesh );

		// Shift mesh to (0, 0, 0).
		final Vertices vertices = mesh.vertices();
		final long nVertices = vertices.size();
		for ( long i = 0; i < nVertices; i++ )
			vertices.setPositionf( i,
					vertices.xf( i ) - center.getFloatPosition( 0 ),
					vertices.yf( i ) - center.getFloatPosition( 1 ),
					vertices.zf( i ) - center.getFloatPosition( 2 ) );

		// Bounding box with respect to 0.
		final float[] boundingBox = Meshes.boundingBox( mesh );

		// Spot mesh, all relative to 0.
		final SpotMesh spotMesh = new SpotMesh( mesh, boundingBox );

		// Create spot.
		final double r = spotMesh.radius();
		final Spot spot = new Spot(
				center.getDoublePosition( 0 ),
				center.getDoublePosition( 1 ),
				center.getDoublePosition( 2 ),
				r,
				quality );
		spot.setMesh( spotMesh );
		return spot;
	}

	/**
	 * Returns the radius of the equivalent sphere with the same volume that of
	 * the specified mesh.
	 *
	 * @return the radius in physical units.
	 */
	public static final double radius( final Mesh mesh )
	{
		return Math.pow( 3. * volume( mesh ) / ( 4 * Math.PI ), 1. / 3. );
	}

	/**
	 * Returns the volume of the specified mesh.
	 *
	 * @return the volume in physical units.
	 */
	public static double volume( final Mesh mesh )
	{

		final Vertices vertices = mesh.vertices();
		final Triangles triangles = mesh.triangles();
		final long nTriangles = triangles.size();
		double sum = 0.;
		for ( long t = 0; t < nTriangles; t++ )
		{
			final long v1 = triangles.vertex0( t );
			final long v2 = triangles.vertex1( t );
			final long v3 = triangles.vertex2( t );

			final double x1 = vertices.x( v1 );
			final double y1 = vertices.y( v1 );
			final double z1 = vertices.z( v1 );
			final double x2 = vertices.x( v2 );
			final double y2 = vertices.y( v2 );
			final double z2 = vertices.z( v2 );
			final double x3 = vertices.x( v3 );
			final double y3 = vertices.y( v3 );
			final double z3 = vertices.z( v3 );

			final double v321 = x3 * y2 * z1;
			final double v231 = x2 * y3 * z1;
			final double v312 = x3 * y1 * z2;
			final double v132 = x1 * y3 * z2;
			final double v213 = x2 * y1 * z3;
			final double v123 = x1 * y2 * z3;

			sum += ( 1. / 6. ) * ( -v321 + v231 + v312 - v132 - v213 + v123 );
		}
		return Math.abs( sum );
	}

	@Override
	public double radius()
	{
		return radius( mesh );
	}

	/**
	 * Returns the volume of this mesh.
	 *
	 * @return the volume in physical units.
	 */
	public double volume()
	{
		return volume( mesh );
	}

	@Override
	public double size()
	{
		return volume();
	}

	@Override
	public void scale( final double alpha )
	{
		final Vertices vertices = mesh.vertices();
		final long nVertices = vertices.size();
		for ( int v = 0; v < nVertices; v++ )
		{
			final float x = vertices.xf( v );
			final float y = vertices.yf( v );
			final float z = vertices.zf( v );

			// Spherical coords.
			if ( x == 0. && y == 0. )
			{
				if ( z == 0 )
					continue;

				vertices.setPositionf( v, 0f, 0f, ( float ) ( z * alpha ) );
				continue;
			}
			final double r = Math.sqrt( x * x + y * y + z * z );
			final double theta = Math.acos( z / r );
			final double phi = Math.signum( y ) * Math.acos( x / Math.sqrt( x * x + y * y ) );

			final double ra = r * alpha;
			final float xa = ( float ) ( ra * Math.sin( theta ) * Math.cos( phi ) );
			final float ya = ( float ) ( ra * Math.sin( theta ) * Math.sin( phi ) );
			final float za = ( float ) ( ra * Math.cos( theta ) );
			vertices.setPositionf( v, xa, ya, za );
		}
		boundingBox = Meshes.boundingBox( mesh );
	}

	@Override
	public SpotMesh copy()
	{
		final BufferMesh meshCopy = new BufferMesh( ( int ) mesh.vertices().size(), ( int ) mesh.triangles().size() );
		Meshes.copy( this.mesh, meshCopy );
		return new SpotMesh( meshCopy, boundingBox.clone() );
	}

	@Override
	public String toString()
	{
		final StringBuilder str = new StringBuilder( super.toString() );

		str.append( "\nBounding-box" );
		str.append( String.format( "\n%5s: %7.2f -> %7.2f", "X", boundingBox[ 0 ], boundingBox[ 3 ] ) );
		str.append( String.format( "\n%5s: %7.2f -> %7.2f", "Y", boundingBox[ 1 ], boundingBox[ 4 ] ) );
		str.append( String.format( "\n%5s: %7.2f -> %7.2f", "Z", boundingBox[ 2 ], boundingBox[ 5 ] ) );

		final Vertices vertices = mesh.vertices();
		final long nVertices = vertices.size();
		str.append( "\nV (" + nVertices + "):" );
		for ( long i = 0; i < nVertices; i++ )
			str.append( String.format( "\n%5d: %7.2f %7.2f %7.2f",
					i, vertices.x( i ), vertices.y( i ), vertices.z( i ) ) );

		final Triangles triangles = mesh.triangles();
		final long nTriangles = triangles.size();
		str.append( "\nF (" + nTriangles + "):" );
		for ( long i = 0; i < nTriangles; i++ )
			str.append( String.format( "\n%5d: %5d %5d %5d",
					i, triangles.vertex0( i ), triangles.vertex1( i ), triangles.vertex2( i ) ) );

		return str.toString();
	}
}
