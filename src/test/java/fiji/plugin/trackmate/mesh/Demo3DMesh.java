package fiji.plugin.trackmate.mesh;

import java.io.IOException;
import java.util.Iterator;

import fiji.plugin.trackmate.detection.MaskUtils;
import fiji.plugin.trackmate.util.TMUtils;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.linked.TLongLinkedList;
import gnu.trove.procedure.TLongProcedure;
import gnu.trove.set.hash.TLongHashSet;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Meshes;
import net.imagej.mesh.Triangles;
import net.imagej.mesh.Vertices;
import net.imagej.mesh.io.stl.STLMeshIO;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.img.ImgView;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class Demo3DMesh
{

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		final ImgPlus< BitType > mask = loadTestMask2();
//		final ImgPlus< BitType > mask = loadTestMask();

		// Convert it to labeling.
		final ImgLabeling< Integer, IntType > labeling = MaskUtils.toLabeling( mask, mask, 0.5, 1 );
		final ImagePlus out = ImageJFunctions.show( labeling.getIndexImg(), "labeling" );

		// Iterate through all components.
		final LabelRegions< Integer > regions = new LabelRegions< Integer >( labeling );
		final double[] cal = TMUtils.getSpatialCalibration( mask );

		// Parse regions to create polygons on boundaries.
		final Iterator< LabelRegion< Integer > > iterator = regions.iterator();
		int j = 0;
		while ( iterator.hasNext() )
		{
			final LabelRegion< Integer > region = iterator.next();
			final IntervalView< BoolType > box = Views.zeroMin( region );

			// To mesh.
			final Mesh mesh = Meshes.marchingCubes( box );
			final Mesh cleaned = Meshes.removeDuplicateVertices( mesh, 0 );
			final Mesh simplified = Meshes.simplify( cleaned, 0.1f, 10 );

			// Scale and offset with physical coordinates.
			final double[] origin = region.minAsDoubleArray();
			scale( simplified.vertices(), cal, origin );

			// Simplify.
			System.out.println( "Before cleaning: " + mesh.vertices().size() + " vertices and " + mesh.triangles().size() + " faces." );
			System.out.println( "Before simplification: " + cleaned.vertices().size() + " vertices and " + cleaned.triangles().size() + " faces." );
			System.out.println( "After simplification: " + simplified.vertices().size() + " vertices and " + simplified.triangles().size() + " faces." );
			System.out.println();

			/*
			 * IO.
			 */
			testIO( simplified, ++j );

			/*
			 * Display.
			 */

			// Intersection with a XY plane at a fixed Z position.
			final int zslice = 20; // plan
			final double z = ( zslice ) * cal[ 2 ]; // um

			final double[][] xy = intersect2( simplified, z );
			final float[] xRoi = new float[ xy[ 0 ].length ];
			final float[] yRoi = new float[ xy[ 0 ].length ];
			for ( int i = 0; i < xy[ 0 ].length; i++ )
			{
				xRoi[ i ] = ( float ) ( xy[ 0 ][ i ] / cal[ 0 ] );
				yRoi[ i ] = ( float ) ( xy[ 1 ][ i ] / cal[ 1 ] );
			}
			final PolygonRoi roi = new PolygonRoi( xRoi, yRoi, PolygonRoi.POLYGON );
			Overlay overlay = out.getOverlay();
			if ( overlay == null )
			{
				overlay = new Overlay();
				out.setOverlay( overlay );
			}
			overlay.add( roi );
		}
		System.out.println( "Done." );
	}

	@SuppressWarnings( "unused" )
	private static < T extends RealType< T > & NumericType< T > > ImgPlus< BitType > loadTestMask2()
	{
		final String filePath = "samples/mesh/Cube.tif";
		final ImagePlus imp = IJ.openImage( filePath );
		@SuppressWarnings( "unchecked" )
		final ImgPlus< T > img = TMUtils.rawWraps( imp );
		final RandomAccessibleInterval< BitType > mask = RealTypeConverters.convert( img, new BitType() );
		return new ImgPlus<>( ImgView.wrap( mask ), img );
	}

	private static double[][] intersect2( final Mesh mesh, final double z )
	{
		final Triangles triangles = mesh.triangles();
		final Vertices vertices = mesh.vertices();

		// Find a line that intersects with z plane.
		long[] start = null;
		for ( long i = 0; i < triangles.size(); i++ )
		{
			final long v0 = triangles.vertex0( i );
			final long v1 = triangles.vertex1( i );
			final long v2 = triangles.vertex2( i );
			if ( testLineIntersectPlane( vertices, v0, v1, z ) )
			{
				start = new long[] { v0, v1 };
				break;
			}
			if ( testLineIntersectPlane( vertices, v0, v2, z ) )
			{
				start = new long[] { v0, v2 };
				break;
			}
			if ( testLineIntersectPlane( vertices, v2, v1, z ) )
			{
				start = new long[] { v2, v1 };
				break;
			}
		}
		if ( start == null )
		{
			System.out.println( "No intersection with Z = " + z + " found." );
			return null;
		}
		System.out.println( "Intersection with Z = " + z + ": " + Util.printCoordinates( start ) );

		final TDoubleArrayList intersectionX = new TDoubleArrayList();
		final TDoubleArrayList intersectionY = new TDoubleArrayList();
		final TLongLinkedList queue = new TLongLinkedList();
		final TLongHashSet visited = new TLongHashSet();
		final TLongHashSet neighborVertices = new TLongHashSet( 12 );
		final LineIntersectProcedure lineIntersectProcedure = new LineIntersectProcedure( vertices, z, queue, visited, intersectionX, intersectionY );
		queue.add( start[ 0 ] );
		while ( !queue.isEmpty() )
		{
			final long source = queue.removeAt( queue.size() - 1 );
			if (visited.contains( source ))
				continue;
			visited.add( source );

			// Search neighbors of the current one that intersect with the Z
			// plane.
			searchNeighbors( mesh, source, z, neighborVertices );

			// Check if line connecting neighbors intersect plane.
			lineIntersectProcedure.setSourceV( source );
			neighborVertices.forEach( lineIntersectProcedure );
		}
		
		return new double[][] { intersectionX.toArray(), intersectionY.toArray() };
	}

	/**
	 * Finds the indices of the vertices that are connected to the vertex with
	 * the specified index in the specified mesh, if they make a line that
	 * crosses the Z plane at the specified position.
	 * <p>
	 * TODO This search is inefficient, and would benefit from having a data
	 * structure that stores this info.
	 * 
	 * @param mesh
	 *            the mesh.
	 * @param v
	 *            the index of the vertex to find the neighbors of.
	 * @param neighbors
	 *            an array in which to write the indices of the neighbors. Is
	 *            reset by this method.
	 */
	private static void searchNeighbors( final Mesh mesh, final long v, final double z, final TLongHashSet neighbors )
	{
		neighbors.clear();
		for ( long face = 0; face < mesh.triangles().size(); face++ )
			testFace( mesh, face, v, z, neighbors );
	}

	/**
	 * 
	 * @param mesh
	 *            the mesh to test.
	 * @param face
	 *            the index of the face to test.
	 * @param v
	 *            the index of the vertex we are searching.
	 * @param z
	 *            the z position an edge needs to cross.
	 * @param neighbors
	 *            the list of neighbors to add candidate to.
	 */
	private static final void testFace( final Mesh mesh, final long face, final long v, final double z, final TLongHashSet neighbors )
	{
		final Triangles triangles = mesh.triangles();
		final Vertices vertices = mesh.vertices();
		final long v0 = triangles.vertex0( face );
		final long v1 = triangles.vertex1( face );
		final long v2 = triangles.vertex2( face );
		testFaceVertexIs( vertices, v, v0, v1, v2, z, neighbors );
		testFaceVertexIs( vertices, v, v1, v0, v2, z, neighbors );
		testFaceVertexIs( vertices, v, v2, v0, v1, z, neighbors );
	}

	private static void testFaceVertexIs( final Vertices vertices, final long searched, final long source, final long v1, final long v2, final double z, final TLongHashSet neighbors )
	{
		if ( source != searched )
			return;

		if ( testLineIntersectPlane( vertices, source, v1, z ) )
			neighbors.add( v1 );
		if ( testLineIntersectPlane( vertices, source, v2, z ) )
			neighbors.add( v2 );
	}

	private static boolean testLineIntersectPlane( final Vertices vertices, final long source, final long target, final double z )
	{
		final double z0 = vertices.z( source );
		final double z1 = vertices.z( target );
		if ( ( z0 > z && z1 > z ) || ( z0 < z && z1 < z ) )
			return false;
		return true;
	}

	private static void testIO( final Mesh simplified, final int j )
	{
		final STLMeshIO meshIO = new STLMeshIO();
		// Serialize to disk.
		try
		{
			meshIO.save( simplified, String.format( "samples/mesh/CElegansMask3D_%02d.stl", j ) );
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
	}

	private static void scale( final Vertices vertices, final double[] scale, final double[] origin )
	{
		final long nv = vertices.size();
		for ( long i = 0; i < nv; i++ )
		{
			final double x = ( origin[ 0 ] + vertices.x( i ) ) * scale[ 0 ];
			final double y = ( origin[ 1 ] + vertices.y( i ) ) * scale[ 1 ];
			final double z = ( origin[ 2 ] + vertices.z( i ) ) * scale[ 2 ];
			vertices.set( i, x, y, z );
		}
	}

	/**
	 * Procedure that adds the intersection of the line made by the source
	 * vertex and target vertices iterated.
	 * <p>
	 * The intersection is added only if the target vertex has not been visited.
	 * New targets are also added to the queue.
	 */
	private static final class LineIntersectProcedure implements TLongProcedure
	{

		private final Vertices vertices;

		private long sv = -1;

		private final double z;

		private final TLongLinkedList queue;

		private final TLongHashSet visited;

		private final TDoubleArrayList intersectionX;

		private final TDoubleArrayList intersectionY;

		private double zs;

		private double xs;

		private double ys;

		public LineIntersectProcedure(
				final Vertices vertices,
				final double z,
				final TLongLinkedList queue,
				final TLongHashSet visited,
				final TDoubleArrayList intersectionX,
				final TDoubleArrayList intersectionY )
		{
			this.vertices = vertices;
			this.z = z;
			this.queue = queue;
			this.visited = visited;
			this.intersectionX = intersectionX;
			this.intersectionY = intersectionY;
		}

		public void setSourceV( final long sourceV )
		{
			this.sv = sourceV;
			this.xs = vertices.x( sv );
			this.ys = vertices.y( sv );
			this.zs = vertices.z( sv );
		}

		@Override
		public boolean execute( final long tv )
		{
			if ( !visited.contains( tv ) )
			{
				final double xt = vertices.x( tv );
				final double yt = vertices.y( tv );
				final double zt = vertices.z( tv );
				if ( zs == zt )
				{
					intersectionX.add( 0.5 * ( xs + xt ) );
					intersectionY.add( 0.5 * ( ys + yt ) );
				}
				else
				{
					final double t = ( z - zs ) / ( zt - zs );
					intersectionX.add( xs + t * ( xt - xs ) );
					intersectionY.add( ys + t * ( yt - ys ) );
				}
				queue.add( tv );
			}
			return true;
		}
	}

	private static < T extends RealType< T > & NumericType< T > > ImgPlus< BitType > loadTestMask()
	{
		final String filePath = "samples/mesh/CElegansMask3D.tif";
		final ImagePlus imp = IJ.openImage( filePath );

		// First channel is the mask.
		@SuppressWarnings( "unchecked" )
		final ImgPlus< T > img = TMUtils.rawWraps( imp );
		final ImgPlus< T > c1 = ImgPlusViews.hyperSlice( img, img.dimensionIndex( Axes.CHANNEL ), 0 );

		// Take the first time-point
		final ImgPlus< T > t1 = ImgPlusViews.hyperSlice( c1, c1.dimensionIndex( Axes.TIME ), 0 );
		// Make it to boolean.
		final RandomAccessibleInterval< BitType > mask = RealTypeConverters.convert( t1, new BitType() );
		return new ImgPlus< BitType >( ImgView.wrap( mask ), t1 );
	}
}
