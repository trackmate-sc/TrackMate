package fiji.plugin.trackmate;

import gnu.trove.list.array.TDoubleArrayList;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Meshes;
import net.imagej.mesh.Triangles;
import net.imagej.mesh.Vertices;
import net.imglib2.RealPoint;

public class SpotMesh
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
	public final float[] boundingBox;

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
//		final Vertices vertices = mesh.vertices();
//		final long nVertices = vertices.size();
//		for ( long i = 0; i < nVertices; i++ )
//			vertices.setPositionf( i,
//					vertices.xf( i ) - center.getFloatPosition( 0 ),
//					vertices.yf( i ) - center.getFloatPosition( 1 ),
//					vertices.zf( i ) - center.getFloatPosition( 2 ) );

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

	private double radius()
	{
		return Math.pow( 3. * volume() / ( 4 * Math.PI ), 1. / 3. );
	}

	private double volume()
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

	public void scale(final double alpha)
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
			final double r = Math.sqrt( x * x + y * y + z * z ) ;
			final double theta = Math.acos( z / r );
			final double phi = Math.signum( y ) * Math.acos( x / Math.sqrt( x * x + y * y ) );

			final double ra = r * alpha;
			final float xa = ( float ) ( ra * Math.sin( theta ) * Math.cos( phi ) );
			final float ya = ( float ) ( ra * Math.sin( theta ) * Math.sin( phi ) );
			final float za = ( float ) ( ra * Math.cos( theta ) );
			vertices.setPositionf( v, xa, ya, za );
		}
	}

	public void slice( final double z, final TDoubleArrayList cx, final TDoubleArrayList cy )
	{
		slice( mesh, z, cx, cy );
	}

	public static void slice( final Mesh mesh, final double z, final TDoubleArrayList cx, final TDoubleArrayList cy )
	{
		// Clear contour holders.
		cx.resetQuick();
		cy.resetQuick();

		final Triangles triangles = mesh.triangles();
		final Vertices vertices = mesh.vertices();
		for ( long f = 0; f < triangles.size(); f++ )
		{
			final long v0 = triangles.vertex0( f );
			final long v1 = triangles.vertex1( f );
			final long v2 = triangles.vertex2( f );

			final double minZ = minZ( vertices, v0, v1, v2 );
			if ( minZ > z )
				continue;
			final double maxZ = maxZ( vertices, v0, v1, v2 );
			if ( maxZ < z )
				continue;

			triangleIntersection( vertices, v0, v1, v2, z, cx, cy );
		}
	}

	/**
	 * Intersection of a triangle with a Z plane.
	 */
	private static void triangleIntersection( final Vertices vertices, final long v0, final long v1, final long v2, final double z, final TDoubleArrayList cx, final TDoubleArrayList cy )
	{
		final double z0 = vertices.z( v0 );
		final double z1 = vertices.z( v1 );
		final double z2 = vertices.z( v2 );

		// Skip this; I don't know how to deal with this border case.
		if ( z0 == z && z1 == z && z2 == z )
		{
			addSegmentToContour( vertices, v0, v1, cx, cy );
			addSegmentToContour( vertices, v0, v2, cx, cy );
			addSegmentToContour( vertices, v1, v2, cx, cy );
			return;
		}

		if ( z0 == z && z1 == z )
		{
			addSegmentToContour( vertices, v0, v1, cx, cy );
			return;
		}
		if ( z0 == z && z2 == z )
		{
			addSegmentToContour( vertices, v0, v2, cx, cy );
			return;
		}
		if ( z1 == z && z2 == z )
		{
			addSegmentToContour( vertices, v1, v2, cx, cy );
			return;
		}

		// Only one vertex is touching the plane -> no need to paint.
		if ( z0 == z || z1 == z || z2 == z )
			return;

		addEdgeIntersectionToContour( vertices, v0, v1, z, cx, cy );
		addEdgeIntersectionToContour( vertices, v0, v2, z, cx, cy );
		addEdgeIntersectionToContour( vertices, v1, v2, z, cx, cy );
	}

	private static void addSegmentToContour( final Vertices vertices, final long v0, final long v1, final TDoubleArrayList cx, final TDoubleArrayList cy )
	{
		final double x0 = vertices.x( v0 );
		final double x1 = vertices.x( v1 );
		cx.add( x0 );
		cx.add( x1 );
		final double y0 = vertices.y( v0 );
		final double y1 = vertices.y( v1 );
		cy.add( y0 );
		cy.add( y1 );
	}

	private static void addEdgeIntersectionToContour(
			final Vertices vertices,
			final long sv,
			final long tv,
			final double z,
			final TDoubleArrayList cx,
			final TDoubleArrayList cy )
	{
		final double zs = vertices.z( sv );
		final double zt = vertices.z( tv );
		if ( ( zs > z && zt > z ) || ( zs < z && zt < z ) )
			return;

		final double xs = vertices.x( sv );
		final double ys = vertices.y( sv );
		final double xt = vertices.x( tv );
		final double yt = vertices.y( tv );
		final double t = ( zs == zt )
				? 0.5 : ( z - zs ) / ( zt - zs );
		final double x = xs + t * ( xt - xs );
		final double y = ys + t * ( yt - ys );
		cx.add( x );
		cy.add( y );
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

	private static final double minZ( final Vertices vertices, final long v0, final long v1, final long v2 )
	{
		return Math.min( vertices.z( v0 ), Math.min( vertices.z( v1 ), vertices.z( v2 ) ) );
	}

	private static final double maxZ( final Vertices vertices, final long v0, final long v1, final long v2 )
	{
		return Math.max( vertices.z( v0 ), Math.max( vertices.z( v1 ), vertices.z( v2 ) ) );
	}

}
