package fiji.plugin.trackmate.mesh;

import java.util.Arrays;

import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TLongHashSet;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Triangles;
import net.imagej.mesh.Vertices;

public class MeshPlaneIntersection
{

	public static double[][] intersect( final Mesh mesh, final double z )
	{
		/*
		 * Build the edge and face maps. This could be more efficiently
		 * implemented in a read-only winged-edge mesh class.
		 */

		final Vertices vertices = mesh.vertices();
		final Triangles triangles = mesh.triangles();

		// Map of vertex id to list of faces they are in.
		final TLongObjectHashMap< TLongArrayList > vertexList = new TLongObjectHashMap<>();

		// Map of edge (va -> vb with always va < vb) to face pair (fa, fb).
		// They are stored as a paired integers using Szudzik pairing.
		// So it won't work if the index exceeds a few 100s of millions.
		final TLongObjectHashMap< long[] > edgeList = new TLongObjectHashMap< long[] >();

		// Iterate through all the faces.
		final long startTime = System.currentTimeMillis();
		final long[] vs = new long[ 3 ];
		final int[] pairHolder = new int[ 2 ];
		for ( long face = 0; face < triangles.size(); face++ )
		{
			vs[ 0 ] = triangles.vertex0( face );
			vs[ 1 ] = triangles.vertex1( face );
			vs[ 2 ] = triangles.vertex2( face );
			Arrays.sort( vs );

			// Insert face into vertex list.
			for ( final long v : vs )
				insertFaceIntoVertexList( v, face, vertexList );

			// Deal with the 3 edges.
			insertEdge( vs[ 0 ], vs[ 1 ], face, edgeList, pairHolder );
			insertEdge( vs[ 0 ], vs[ 2 ], face, edgeList, pairHolder );
			insertEdge( vs[ 1 ], vs[ 2 ], face, edgeList, pairHolder );
		}
		final long endTime = System.currentTimeMillis();
		System.out.println( "Built edge and face lists for " + triangles.size() + " faces in " + ( endTime - startTime ) + " ms." );

//		edgeList.forEachEntry( new TLongObjectProcedure< long[] >()
//		{
//			private final int[] phK = new int[ 2 ];
//			
//			@Override
//			public boolean execute( final long k, final long[] v )
//			{
//				unpair( k, phK );
//				System.out.println( String.format( "%d, %d -> %d, %d", phK[ 0 ], phK[ 1 ], v[ 0 ], v[ 1 ] ) );
//				return true;
//			}
//		} );

		/*
		 * Find one edge that crosses the Z plane.
		 */
		
		final TLongIterator edgeIt = edgeList.keySet().iterator();
		long start = -1;
		while ( edgeIt.hasNext() )
		{
			final long edge = edgeIt.next();
			unpair( edge, pairHolder );
			final long va = pairHolder[ 0 ];
			final long vb = pairHolder[ 1 ];
			if ( testLineIntersectPlane( vertices, va, vb, z ) )
			{
				start = edge;
				break;
			}
		}
		if ( start < 0 )
		{
			System.out.println( "Could not find an edge that intersects with Z = " + z );
			return null;
		}

		final TDoubleArrayList intersectionX = new TDoubleArrayList();
		final TDoubleArrayList intersectionY = new TDoubleArrayList();
		long current = start;
		long previousFace = -1;
		final long[][] edges = new long[ 3 ][ 2 ];
		final TLongHashSet visited = new TLongHashSet();
		while ( true )
		{
			addEdgeToContour( vertices, current, z, intersectionX, intersectionY, pairHolder );
			final long face = getNextFace( edgeList, current, previousFace );
			if ( visited.contains( face ) )
				break;

			final long next = getNextEdge( mesh, edgeList, face, current, z, previousFace, edges, vs );
			if ( next < 0 )
				break;

			visited.add( face );
			previousFace = face;
			current = next;
		}
		return new double[][] { intersectionX.toArray(), intersectionY.toArray() };
	}

	private static long getNextFace( final TLongObjectHashMap< long[] > edgeList, final long current, final long previousFace )
	{
		// Get the faces of this edge.
		final long[] faces = edgeList.get( current );
		// Retain the one we have not been visiting.
		long face;
		if ( faces[ 0 ] == previousFace )
			face = faces[ 1 ];
		else
			face = faces[ 0 ];
		return face;
	}

	private static long getNextEdge( final Mesh mesh, final TLongObjectHashMap< long[] > edgeList, final long face, final long current, final double z, final long previousFace, final long[][] edges, final long[] vs )
	{
		final Triangles triangles = mesh.triangles();
		final Vertices vertices = mesh.vertices();

		// Get the edges of this face.
		vs[ 0 ] = triangles.vertex0( face );
		vs[ 1 ] = triangles.vertex1( face );
		vs[ 2 ] = triangles.vertex2( face );
		Arrays.sort( vs );
		edges[ 0 ][ 0 ] = vs[ 0 ];
		edges[ 0 ][ 1 ] = vs[ 1 ];
		edges[ 1 ][ 0 ] = vs[ 0 ];
		edges[ 1 ][ 1 ] = vs[ 2 ];
		edges[ 2 ][ 0 ] = vs[ 1 ];
		edges[ 2 ][ 1 ] = vs[ 2 ];
		for ( final long[] edge : edges )
		{
			final long e = pair( edge[ 0 ], edge[ 1 ]);
			if (  e == current )
				continue;

			if ( testLineIntersectPlane( vertices, edge[ 0 ], edge[ 1 ], z ) )
				return e;
		}
		return -1;

	}

	private static void addEdgeToContour( final Vertices vertices, final long edge, final double z, final TDoubleArrayList cx, final TDoubleArrayList cy, final int[] pairHolder )
	{
		unpair( edge, pairHolder );
		final int sv = pairHolder[ 0 ];
		final int tv = pairHolder[ 1 ];
		final double xs = vertices.x( sv );
		final double ys = vertices.y( sv );
		final double zs = vertices.z( sv );
		final double xt = vertices.x( tv );
		final double yt = vertices.y( tv );
		final double zt = vertices.z( tv );
		if ( zs == zt )
		{
			cx.add( 0.5 * ( xs + xt ) );
			cy.add( 0.5 * ( ys + yt ) );
		}
		else
		{
			final double t = ( z - zs ) / ( zt - zs );
			cx.add( xs + t * ( xt - xs ) );
			cy.add( ys + t * ( yt - ys ) );
		}

	}

	private static void insertEdge( final long va, final long vb, final long face, final TLongObjectHashMap< long[] > edgeList, final int[] pairHolder )
	{
		assert va < vb;
		final long edge = pair( va, vb );
		final long[] faces = edgeList.get( edge );
		if ( faces == null )
		{
			edgeList.put( edge, new long[] { face, -1 } );
			return;
		}
		faces[ 1 ] = face;
	}

	private static void insertFaceIntoVertexList( final long vertex, final long face, final TLongObjectHashMap< TLongArrayList > vertexList )
	{
		TLongArrayList faceList = vertexList.get( vertex );
		if ( faceList == null )
		{
			faceList = new TLongArrayList();
			vertexList.put( vertex, faceList );
		}
		faceList.add( face );

	}

	private static boolean testLineIntersectPlane( final Vertices vertices, final long source, final long target, final double z )
	{
		final double z0 = vertices.z( source );
		final double z1 = vertices.z( target );
		if ( ( z0 > z && z1 > z ) || ( z0 < z && z1 < z ) )
			return false;
		return true;
	}

	/**
	 * Szudzik pairing.
	 * 
	 * @param x
	 *            the 1st int to pair.
	 * @param y
	 *            the 2nd int to pair.
	 * @return Szudzik pairing.
	 */
	public static long pair( final double x, final double y )
	{
		return ( long ) ( x >= y ? x * x + x + y : y * y + x );
	}

	/**
	 * Szudzik unpairing.
	 * 
	 * @param z
	 *            the factor to unpair.
	 * @param out
	 *            where to write the results in.
	 */
	public static void unpair( final long z, final int[] out )
	{
		final long b = ( long ) Math.sqrt( z );
		final int a = ( int ) ( z - b * b );
		if ( a < b )
		{
			out[ 0 ] = a;
			out[ 1 ] = ( int ) b;
		}
		else
		{
			out[ 0 ] = ( int ) b;
			out[ 1 ] = ( int ) ( a - b );
		}
	}

	public static void main( final String[] args )
	{
		final int[][] tests = new int[][] {
				{ 1, 2 },
				{ 100, 5 },
				{ 5, 100 },
				{ 0, 500 },
				{ 9, 0 },
				{ 120, 12345678 },
				{ -1, 50 },
				{ 20, -1 }
		};
		final int[] out = new int[ 2 ];
		for ( final int[] test : tests )
		{
			final long z = pair( test[ 0 ], test[ 1 ] );
			unpair( z, out );
			System.out.println( String.format( "%d, %d -> %d -> %d, %d", test[ 0 ], test[ 1 ], z, out[ 0 ], out[ 1 ] ) );
		}
	}

}
