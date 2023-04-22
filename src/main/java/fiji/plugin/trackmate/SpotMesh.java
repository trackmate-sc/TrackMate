package fiji.plugin.trackmate;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.util.mesh.MeshUtils;
import fiji.plugin.trackmate.util.mesh.RayCastingX;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.list.linked.TDoubleLinkedList;
import gnu.trove.set.hash.TIntHashSet;
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

	public List< TDoubleLinkedList[] > slice( final double z )
	{
		return slice2( mesh, z );
	}

	public static List< TDoubleLinkedList[] > slice2( final Mesh mesh, final double z )
	{
		final double resolution = 1; // FIXME.

		final Triangles triangles = mesh.triangles();
		final Vertices vertices = mesh.vertices();
		final TLongArrayList intersecting = new TLongArrayList();
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

			intersecting.add( f );
		}

		// Holder for ray-casting results.
		final TDoubleArrayList xs = new TDoubleArrayList();
		final TDoubleArrayList normals = new TDoubleArrayList();
		final List< TDoubleLinkedList[] > contours = new ArrayList<>();

		// Adding mesh entries and exits to contours.
		final TDoubleLinkedList exits = new TDoubleLinkedList();
		final TDoubleLinkedList entries = new TDoubleLinkedList();

		// Set of contours that are still active (it is still ok to
		// add points to them).
		final TIntHashSet activeContours = new TIntHashSet(); // lol

		// What contours to remove from the active set.
		final TIntHashSet removeFromActive = new TIntHashSet();

		final float[] bb = Meshes.boundingBox( mesh );
		final RayCastingX ray = new RayCastingX( mesh );
		for ( double y = bb[ 1 ]; y <= bb[ 4 ]; y += resolution )
		{
			ray.cast( y, z, xs, normals );
			if ( xs.isEmpty() )
				continue;

			if ( contours.isEmpty() )
			{
				// Initializing.
				for ( int i = 0; i < xs.size(); i++ )
				{
					final double x = xs.getQuick( i );
					if ( normals.getQuick( i ) < 0. )
					{
						// Entry: new contour.
						final TDoubleLinkedList cx = new TDoubleLinkedList();
						final TDoubleLinkedList cy = new TDoubleLinkedList();
						cx.add( x );
						cy.add( y );
						contours.add( new TDoubleLinkedList[] { cx, cy } );
						activeContours.add( contours.size() - 1 );
					}
					else
					{
						// Exit: add it to the end of an existing one if we have
						// it.
						if ( contours.isEmpty() )
						{
							final TDoubleLinkedList cx = new TDoubleLinkedList();
							final TDoubleLinkedList cy = new TDoubleLinkedList();
							cx.add( x );
							cy.add( y );
							contours.add( new TDoubleLinkedList[] { cx, cy } );
							activeContours.add( contours.size() - 1 );
						}
						else
						{
							final TDoubleLinkedList[] contour = contours.get( contours.size() - 1 );
							contour[ 0 ].add( x );
							contour[ 1 ].add( y );
						}
					}
				}
			}
			else
			{
				// Find to what contour to add it. Criterion: nearest.

				/*
				 * It's a tracking problem. We want to link a set of X position
				 * (mesh borders) to contour tails and heads. X positions that
				 * are not matched indicate a new contour should be created.
				 *
				 * We assume that there is no global disappearance of mesh
				 * entries. That is: there is not situation where all contours
				 * suddenly stops and another entry and exits appear away.
				 */

				removeFromActive.clear();
				removeFromActive.addAll( activeContours );

				entries.clear();
				exits.clear();
				for ( int j = 0; j < xs.size(); j++ )
				{
					final double x = xs.get( j );
					if ( normals.get( j ) < 0 )
						entries.add( x );
					else
						exits.add( x );
				}

				// Find suitable entries for contours. We only iterate over
				// active contours.
				final TIntIterator it = activeContours.iterator();
				while ( it.hasNext() )
				{
					final int i = it.next();
					final TDoubleLinkedList cx = contours.get( i )[0];
					final TDoubleLinkedList cy = contours.get( i )[1];

					// Entries.
					double minDist = java.lang.Double.POSITIVE_INFINITY;
					int bestEntry = -1;
					for ( int j = 0; j < entries.size(); j++ )
					{
						final double x = entries.get( j );
						final double d = Math.abs( x - cx.get( 0 ) );
						if ( d < minDist )
						{
							minDist = d;
							bestEntry = j;
						}
					}
					if ( bestEntry >= 0 )
					{
						cx.insert( 0, entries.get( bestEntry ) );
						cy.insert( 0, y );
						entries.removeAt( bestEntry );
						removeFromActive.remove( i ); // mark contour as active.
					}

					// Exits.
					minDist = java.lang.Double.POSITIVE_INFINITY;
					int bestExit = -1;
					for ( int j = 0; j < exits.size(); j++ )
					{
						final double x = exits.get( j );
						final double d = Math.abs( x - cx.get( cx.size() - 1 ) );
						if ( d < minDist )
						{
							minDist = d;
							bestExit = j;
						}
					}
					if ( bestExit >= 0 )
					{
						cx.add( exits.get( bestExit ) );
						cy.add( y );
						exits.removeAt( bestExit );
						removeFromActive.remove( i ); // mark contour as active.
					}
				}

				// Do we still have entries and exits without a contour?
				if ( !entries.isEmpty() || !exits.isEmpty() )
				{
					// -> create one for them.
					for ( int i = 0; i < Math.max( entries.size(), exits.size() ); i++ )
					{
						final TDoubleLinkedList cx = new TDoubleLinkedList();
						final TDoubleLinkedList cy = new TDoubleLinkedList();
						if ( i < entries.size() )
						{
							cx.add( entries.get( i ) );
							cy.add( y );
						}
						if ( i < exits.size() )
						{
							cx.add( exits.get( i ) );
							cy.add( y );
						}
						contours.add( new TDoubleLinkedList[] { cx, cy } );
						activeContours.add( contours.size() - 1 );
					}
				}

				// Do we have contours that did not receive a entry or an exit?
				if ( !removeFromActive.isEmpty() )
					activeContours.removeAll( removeFromActive );

			}
		}
		return contours;
	}

	public static List< TDoubleLinkedList[] > slice( final Mesh mesh, final double z )
	{
		final Triangles triangles = mesh.triangles();
		final Vertices vertices = mesh.vertices();
		final TLongArrayList intersecting = new TLongArrayList();
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
			if ( minZ == maxZ )
				continue; // parallel.

			intersecting.add( f );
		}

		final ArrayDeque< Point2D.Double[] > segments = new ArrayDeque<>();
		for ( int i = 0; i < intersecting.size(); i++ )
		{
			final long id = intersecting.getQuick( i );
			final Point2D.Double[] endPoints = triangleIntersection( mesh, id, z );
			if ( endPoints != null && endPoints[ 0 ] != null && endPoints[ 1 ] != null )
			{
				final Double a = endPoints[ 0 ];
				final Double b = endPoints[ 1 ];
				if ( a.x == b.x && a.y == b.y )
					continue;

				segments.add( endPoints );
			}
		}

		final List< TDoubleLinkedList[] > contours = new ArrayList<>();
		SEGMENT: while ( !segments.isEmpty() )
		{
			final Double[] segment = segments.pop();
			final Double a = segment[ 0 ];
			final Double b = segment[ 1 ];

			// What contour does it belong to?
			for ( final TDoubleLinkedList[] contour : contours )
			{
				final TDoubleLinkedList x = contour[ 0 ];
				final TDoubleLinkedList y = contour[ 1 ];

				// Test if connects to first point of the contour.
				final double xstart = x.get( 0 );
				final double ystart = y.get( 0 );
				if ( a.x == xstart && a.y == ystart )
				{
					// Insert other extremity just before the first point.
					x.insert( 0, b.x );
					y.insert( 0, b.y );
					continue SEGMENT;
				}
				else if ( b.x == xstart && b.y == ystart )
				{
					x.insert( 0, a.x );
					y.insert( 0, a.y );
					continue SEGMENT;
				}

				// Test if connects to first point of the contour.
				final double xend = x.get( x.size() - 1 );
				final double yend = y.get( y.size() - 1 );
				if ( a.x == xend && a.y == yend )
				{
					// Add other extremity at the end.
					x.add( b.x );
					y.add( b.y );
					continue SEGMENT;
				}
				else if ( b.x == xend && b.y == yend )
				{
					// Add other extremity at the end.
					x.add( a.x );
					y.add( a.y );
					continue SEGMENT;
				}
			}

			/*
			 * It does not belong to a contour. Make a new one.
			 */

			final TDoubleLinkedList x = new TDoubleLinkedList();
			final TDoubleLinkedList y = new TDoubleLinkedList();
			x.add( a.x );
			x.add( b.x );
			y.add( a.y );
			y.add( b.y );
			contours.add( new TDoubleLinkedList[] { x, y } );
		}

		System.out.println( "Found " + contours.size() + " contours:" ); // DEBUG
		for ( int i = 0; i < contours.size(); i++ )
		{
			System.out.println( "- Contour " + ( i + 1 ) ); // DEBUG
			final TDoubleLinkedList[] contour = contours.get( i );
			final TDoubleLinkedList x = contour[ 0 ];
			for ( int j = 0; j < x.size(); j++ )
				System.out.print( String.format( "%3.0f, ", x.get( j ) ) );
			System.out.println();
			final TDoubleLinkedList y = contour[ 1 ];
			for ( int j = 0; j < y.size(); j++ )
				System.out.print( String.format( "%3.0f, ", y.get( j ) ) );
			System.out.println();
		}

		return contours;
	}

	private static Double[] triangleIntersection( final Mesh mesh, final long id, final double z )
	{
		final long v0 = mesh.triangles().vertex0( id );
		final long v1 = mesh.triangles().vertex1( id );
		final long v2 = mesh.triangles().vertex2( id );

		final double x0 = mesh.vertices().x( v0 );
		final double x1 = mesh.vertices().x( v1 );
		final double x2 = mesh.vertices().x( v2 );
		final double y0 = mesh.vertices().y( v0 );
		final double y1 = mesh.vertices().y( v1 );
		final double y2 = mesh.vertices().y( v2 );
		final double z0 = mesh.vertices().z( v0 );
		final double z1 = mesh.vertices().z( v1 );
		final double z2 = mesh.vertices().z( v2 );

		Double a = null;
		Double b = null;

		if ( z0 == z )
			a = new Double( x0, y0 );

		if ( z1 == z )
		{
			if ( a == null )
			{
				a = new Double( x1, y1 );
			}
			else
			{
				b = new Double( x1, y1 );
				return new Double[] { a, b };
			}
		}
		if ( z2 == z )
		{
			if ( a == null )
			{
				a = new Double( x2, y2 );
			}
			else
			{
				b = new Double( x2, y2 );
				return new Double[] { a, b };
			}
		}

		final Double p01 = edgeIntersection( x0, y0, z0, x1, y1, z1, z );
		if ( p01 != null )
		{
			if ( a == null )
			{
				a = p01;
			}
			else
			{
				b = p01;
				return new Double[] { a, b };
			}
		}

		final Double p02 = edgeIntersection( x0, y0, z0, x2, y2, z2, z );
		if ( p02 != null )
		{
			if ( a == null )
			{
				a = p02;
			}
			else
			{
				b = p02;
				return new Double[] { a, b };
			}
		}

		final Double p12 = edgeIntersection( x1, y1, z1, x2, y2, z2, z );
		if ( p12 != null )
		{
			if ( a == null )
			{
				a = p12;
			}
			else
			{
				b = p12;
				return new Double[] { a, b };
			}
		}

//		throw new IllegalStateException( "Could not find an intersection for triangle " + id );

		System.out.println(); // DEBUG
		System.out.println( "Weird triangle: " + MeshUtils.triangleToString( mesh, id ) ); // DEBUG
		final double minZ = minZ( mesh.vertices(), v0, v1, v2 );
		final double maxZ = maxZ( mesh.vertices(), v0, v1, v2 );
		System.out.println( "but minZ=" + minZ + " maxZ=" + maxZ + " and z=" + z + " - equal? " + ( minZ == maxZ ) ); // DEBUG

		return null;
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

	private static Double edgeIntersection( final double xs, final double ys, final double zs,
			final double xt, final double yt, final double zt, final double z )
	{
		if ( ( zs > z && zt > z ) || ( zs < z && zt < z ) )
			return null;

		assert ( zs != zt );
		final double t = ( z - zs ) / ( zt - zs );
		final double x = xs + t * ( xt - xs );
		final double y = ys + t * ( yt - ys );
		return new Double( x, y );
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

	private static final double minZ( final Vertices vertices, final long v0, final long v1, final long v2 )
	{
		return Math.min( vertices.z( v0 ), Math.min( vertices.z( v1 ), vertices.z( v2 ) ) );
	}

	private static final double maxZ( final Vertices vertices, final long v0, final long v1, final long v2 )
	{
		return Math.max( vertices.z( v0 ), Math.max( vertices.z( v1 ), vertices.z( v2 ) ) );
	}

	private static final double minY( final Vertices vertices, final Triangles triangles, final long id )
	{
		final long v0 = triangles.vertex0( id );
		final long v1 = triangles.vertex1( id );
		final long v2 = triangles.vertex2( id );
		return Math.min( vertices.y( v0 ), Math.min( vertices.y( v1 ), vertices.y( v2 ) ) );
	}

	private static final double maxY( final Vertices vertices, final Triangles triangles, final long id )
	{
		final long v0 = triangles.vertex0( id );
		final long v1 = triangles.vertex1( id );
		final long v2 = triangles.vertex2( id );
		return Math.max( vertices.y( v0 ), Math.max( vertices.y( v1 ), vertices.y( v2 ) ) );
	}

	private static final double minY( final Vertices vertices, final long v0, final long v1, final long v2 )
	{
		return Math.min( vertices.y( v0 ), Math.min( vertices.y( v1 ), vertices.y( v2 ) ) );
	}

	private static final double maxY( final Vertices vertices, final long v0, final long v1, final long v2 )
	{
		return Math.max( vertices.y( v0 ), Math.max( vertices.y( v1 ), vertices.y( v2 ) ) );
	}

}
