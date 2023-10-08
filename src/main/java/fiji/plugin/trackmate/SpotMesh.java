package fiji.plugin.trackmate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fiji.plugin.trackmate.util.mesh.SpotMeshIterable;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.mesh.Mesh;
import net.imglib2.mesh.MeshStats;
import net.imglib2.mesh.Meshes;
import net.imglib2.mesh.Vertices;
import net.imglib2.mesh.alg.zslicer.RamerDouglasPeucker;
import net.imglib2.mesh.alg.zslicer.Slice;
import net.imglib2.mesh.alg.zslicer.ZSlicer;
import net.imglib2.mesh.impl.nio.BufferMesh;
import net.imglib2.type.numeric.RealType;

public class SpotMesh extends SpotBase
{

	/**
	 * The mesh representing the 3D contour of the spot. The mesh is centered on
	 * (0, 0, 0) and the true position of its vertices is obtained by adding the
	 * spot center.
	 */
	private final Mesh mesh;

	private Map< Integer, Slice > sliceMap;

	/** The bounding-box, <b>centered on (0,0,0)</b> of this object. */
	public RealInterval boundingBox;

	public SpotMesh(
			final Mesh mesh,
			final double quality )
	{
		this( mesh, quality, null );
	}

	/**
	 * Creates a new spot from the specified mesh. Its position and radius are
	 * calculated from the mesh.
	 *
	 * @param quality
	 *            the spot quality.
	 * @param name
	 *            the spot name.
	 * @param m
	 *            the mesh to create the spot from.
	 */
	public SpotMesh(
			final Mesh m,
			final double quality,
			final String name )
	{
		// Dummy coordinates and radius.
		super( 0., 0., 0., 0., quality, name );

		// Compute triangles and vertices normals.
		final BufferMesh mesh = new BufferMesh( m.vertices().size(), m.triangles().size() );
		Meshes.calculateNormals( m, mesh );

		this.mesh = mesh;
		final RealPoint center = Meshes.center( mesh );

		// Reposition the spot.
		setPosition( center );

		// Shift mesh to (0, 0, 0).
		final Vertices vertices = mesh.vertices();
		final long nVertices = vertices.size();
		for ( long i = 0; i < nVertices; i++ )
			vertices.setPositionf( i,
					vertices.xf( i ) - center.getFloatPosition( 0 ),
					vertices.yf( i ) - center.getFloatPosition( 1 ),
					vertices.zf( i ) - center.getFloatPosition( 2 ) );

		// Compute radius.
		final double r = radius( mesh );
		putFeature( Spot.RADIUS, r );

		// Bounding box, also centered on (0,0,0)
		this.boundingBox = Meshes.boundingBox( mesh );
	}

	/**
	 * This constructor is only used for deserializing a model from a TrackMate
	 * file. It messes with the ID of the spots and should be not used
	 * otherwise.
	 *
	 * @param ID
	 *            the ID to create the spot with.
	 * @param mesh
	 *            the mesh used to create the spot.
	 */
	public SpotMesh( final int ID, final BufferMesh mesh )
	{
		super( ID );
		this.mesh = mesh;
		final RealPoint center = Meshes.center( mesh );

		// Reposition the spot.
		setPosition( center );

		// Shift mesh to (0, 0, 0).
		final Vertices vertices = mesh.vertices();
		final long nVertices = vertices.size();
		for ( long i = 0; i < nVertices; i++ )
			vertices.setPositionf( i,
					vertices.xf( i ) - center.getFloatPosition( 0 ),
					vertices.yf( i ) - center.getFloatPosition( 1 ),
					vertices.zf( i ) - center.getFloatPosition( 2 ) );

		// Compute radius.
		final double r = radius( mesh );
		putFeature( Spot.RADIUS, r );

		// Bounding box, also centered on (0,0,0)
		this.boundingBox = Meshes.boundingBox( mesh );
	}

	/**
	 * Exposes the mesh object stores in this spot. The coordinates of the
	 * vertices are relative to the spot center. That is: the coordinates are
	 * centered on (0,0,0).
	 *
	 * @return the mesh.
	 */
	public Mesh getMesh()
	{
		return mesh;
	}

	@Override
	public double realMax( final int d )
	{
		return getDoublePosition( d ) + boundingBox.realMax( d );
	}

	@Override
	public double realMin( final int d )
	{
		return getDoublePosition( d ) + boundingBox.realMin( d );
	}

	@Override
	public < T extends RealType< T > > IterableInterval< T > iterable( final RandomAccessible< T > ra, final double[] calibration )
	{
		return new SpotMeshIterable<>( ra, this, calibration );
	}

	/**
	 * Gets the slice resulting from the intersection of the mesh with the XY
	 * plane with the specified z position, in pixel coordinates, 0-based.
	 * <p>
	 * Relies on a sort of Z-slice cache. To regenerate it if needed, we need
	 * the specification of a scale in XY and Z specified here.
	 *
	 * @param zSlice
	 *            the Z position of the slice, in pixel coordinates, 0-based.
	 * @param xyScale
	 *            a measure of the mesh scale along XY, for instance the pixel
	 *            size in XY that it was generated from. Used to correct and
	 *            simplify the slice contours.
	 * @param zScale
	 *            the pixel size in Z, used to generate the Z planes spacing.
	 * @return the slice, or <code>null</code> if the mesh does not intersect
	 *         with the specified XY plane. The slice XY coordinates are
	 *         centered so (0,0) corresponds to the mesh center.
	 */
	public Slice getZSlice( final int zSlice, final double xyScale, final double zScale )
	{
		if ( sliceMap == null )
			sliceMap = buildSliceMap( mesh, boundingBox, this, xyScale, zScale );

		return sliceMap.get( Integer.valueOf( zSlice ) );
	}

	/**
	 * Invalidates the Z-slices cache. This will force its recomputation. To be
	 * called after the spot has changed size or Z position.
	 */
	public void resetZSliceCache()
	{
		sliceMap = null;
	}

	/**
	 * Returns the radius of the equivalent sphere with the same volume that of
	 * the specified mesh.
	 * 
	 * @param mesh
	 *            the mesh.
	 * @return the radius in physical units.
	 */
	public static final double radius( final Mesh mesh )
	{
		return Math.pow( 3. * MeshStats.volume( mesh ) / ( 4 * Math.PI ), 1. / 3. );
	}

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
		return MeshStats.volume( mesh );
	}

	@Override
	public void scale( final double alpha )
	{
		final net.imglib2.mesh.Vertices vertices = mesh.vertices();
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
		this.boundingBox = Meshes.boundingBox( mesh );
	}

	@Override
	public SpotMesh copy()
	{
		final BufferMesh meshCopy = new BufferMesh( mesh.vertices().size(), mesh.triangles().size() );
		Meshes.copy( this.mesh, meshCopy );
		return new SpotMesh( meshCopy, getFeature( Spot.QUALITY ), getName() );
	}

	@Override
	public String toString()
	{
		final StringBuilder str = new StringBuilder( super.toString() );

		str.append( "\nBounding-box" );
		str.append( String.format( "\n%5s: %7.2f -> %7.2f", "X", boundingBox.realMin( 0 ), boundingBox.realMax( 0 ) ) );
		str.append( String.format( "\n%5s: %7.2f -> %7.2f", "Y", boundingBox.realMin( 1 ), boundingBox.realMax( 1 ) ) );
		str.append( String.format( "\n%5s: %7.2f -> %7.2f", "Z", boundingBox.realMin( 2 ), boundingBox.realMax( 2 ) ) );

		final net.imglib2.mesh.Vertices vertices = mesh.vertices();
		final long nVertices = vertices.size();
		str.append( "\nV (" + nVertices + "):" );
		for ( long i = 0; i < nVertices; i++ )
			str.append( String.format( "\n%5d: %7.2f %7.2f %7.2f",
					i, vertices.x( i ), vertices.y( i ), vertices.z( i ) ) );

		final net.imglib2.mesh.Triangles triangles = mesh.triangles();
		final long nTriangles = triangles.size();
		str.append( "\nF (" + nTriangles + "):" );
		for ( long i = 0; i < nTriangles; i++ )
			str.append( String.format( "\n%5d: %5d %5d %5d",
					i, triangles.vertex0( i ), triangles.vertex1( i ), triangles.vertex2( i ) ) );

		return str.toString();
	}

	/**
	 * Computes the intersections of the specified mesh with the multiple
	 * Z-slice <b>at integer coordinates</b> corresponding to 1-pixel spacing in
	 * Z. This is why we need to have the <code>calibration</code> array. The
	 * slices are centered on (0,0) the mesh center.
	 *
	 * @param mesh
	 *            the mesh to reslice, centered on (0,0,0).
	 * @param boundingBox
	 *            its bounding box, also centered on (0,0,0).
	 * @param center
	 *            the mesh center true position. Needed to reposition it in Z.
	 * @param calibration
	 *            the pixel size array, needed to compute the 1-pixel spacing.
	 * @return a map from slice position (integer, pixel coordinates) to slices.
	 */
	private static final Map< Integer, Slice > buildSliceMap(
			final Mesh mesh,
			final RealInterval boundingBox,
			final RealLocalizable center,
			final double xyScale,
			final double zScale )
	{
		/*
		 * Let's try to have everything relative to (0,0,0), so that we do not
		 * have to recompute the Z slices when the mesh is moved in X and Y.
		 */

		/*
		 * Compute the Z integers, in pixel coordinates, of the mesh
		 * intersection. These coordinates are absolute value (relative to mesh
		 * center).
		 */
		final double zc = center.getDoublePosition( 2 );
		final int minZ = ( int ) Math.ceil( ( boundingBox.realMin( 2 ) + zc ) / zScale );
		final int maxZ = ( int ) Math.floor( ( boundingBox.realMax( 2 ) + zc ) / zScale );
		final int[] zSlices = new int[ maxZ - minZ + 1 ];
		for ( int i = 0; i < zSlices.length; i++ )
			zSlices[ i ] = ( minZ + i );// pixel coords, absolute value

		/*
		 * Compute equivalent Z positions in physical units, relative to
		 * (0,0,0), of these intersections.
		 */
		final double[] zPos = new double[ zSlices.length ];
		for ( int i = 0; i < zPos.length; i++ )
			zPos[ i ] = zSlices[ i ] * zScale - zc;

		// Compute the slices. They will be centered on (0,0) in XY.
		final List< Slice > slices = ZSlicer.slices(
				mesh,
				zPos,
				zScale );

		// Simplify below 1/4th of a pixel.
		final double epsilon = xyScale * 0.25;
		final List< Slice > simplifiedSlices = slices.stream()
				.map( s -> RamerDouglasPeucker.simplify( s, epsilon ) )
				.collect( Collectors.toList() );

		// Store in a map of Z slice -> slice.
		final Map< Integer, Slice > sliceMap = new HashMap<>();
		for ( int i = 0; i < zSlices.length; i++ )
			sliceMap.put( Integer.valueOf( zSlices[ i ] ), simplifiedSlices.get( i ) );

		return sliceMap;
	}
}
