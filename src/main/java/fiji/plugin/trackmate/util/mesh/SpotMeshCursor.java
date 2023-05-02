package fiji.plugin.trackmate.util.mesh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fiji.plugin.trackmate.SpotMesh;
import gnu.trove.list.array.TDoubleArrayList;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.alg.zslicer.RamerDouglasPeucker;
import net.imagej.mesh.alg.zslicer.Slice;
import net.imagej.mesh.alg.zslicer.ZSlicer;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;

/**
 * A {@link Cursor} that iterates over the pixels inside a mesh.
 * <p>
 * It is based on an implementation of the ray casting algorithm, with some
 * optimization to avoid querying the mesh for every single pixel. It does its
 * best to ensure that the pixels iterated inside a mesh created from a mask are
 * exactly the pixels of the original mask, but does not succeed fully (yet).
 *
 * @author Jean-Yves Tinevez
 *
 * @param <T>
 *            the types of the pixels iterated.
 */
public class SpotMeshCursor< T > implements Cursor< T >
{

	private final double[] cal;

	private final float[] bb;

	private final int minX;

	private final int maxX;

	private final int minY;

	private final int maxY;

	private final int minZ;

	private final int maxZ;

	private final RandomAccess< T > ra;

	private boolean hasNext;

	private int iy;

	private int iz;

	private int ix;

	/**
	 * List of resolved X positions where we enter / exit the mesh. Set by the
	 * ray casting algorithm.
	 */
	private final TDoubleArrayList intersectionXs = new TDoubleArrayList();

	private final Map< Integer, Slice > sliceMap;

	private Slice slice;

	private final Mesh mesh;

	public SpotMeshCursor( final RandomAccess< T > ra, final SpotMesh sm, final double[] cal )
	{
		this( ra, sm.mesh, sm.boundingBox, cal );
	}

	public SpotMeshCursor( final RandomAccess< T > ra, final Mesh mesh, final float[] boundingBox, final double[] cal )
	{
		this.ra = ra;
		this.mesh = mesh;
		this.cal = cal;
		this.bb = boundingBox;
		this.minX = ( int ) Math.floor( bb[ 0 ] / cal[ 0 ] );
		this.maxX = ( int ) Math.ceil( bb[ 3 ] / cal[ 0 ] );
		this.minY = ( int ) Math.floor( bb[ 1 ] / cal[ 1 ] );
		this.maxY = ( int ) Math.ceil( bb[ 4 ] / cal[ 1 ] );
		this.minZ = ( int ) Math.floor( bb[ 2 ] / cal[ 2 ] );
		this.maxZ = ( int ) Math.ceil( bb[ 5 ] / cal[ 2 ] );

		this.sliceMap = buildSliceMap( mesh, boundingBox, cal );
		reset();
	}

	@Override
	public void reset()
	{
		this.ix = maxX; // To force a new ray cast when we call fwd()
		this.iy = minY - 1; // Then we will move to minY.
		this.iz = minZ;
		this.slice = sliceMap.get( iz );
		this.hasNext = true;
		preFetch();
	}

	@Override
	public void fwd()
	{
		ra.setPosition( ix, 0 );
		ra.setPosition( iy, 1 );
		ra.setPosition( iz, 2 );
		preFetch();
	}

	private void preFetch()
	{
		hasNext = false;
		while ( true )
		{
			// Find next position.
			ix++;
			if ( ix > maxX )
			{
				ix = minX;
				while ( true )
				{
					// Next Y line, we will need to ray cast again.
					ix = minX;
					iy++;
					if ( iy > maxY )
					{
						iy = minY;
						iz++;
						if ( iz > maxZ )
							return; // Finished!
						slice = sliceMap.get( iz );
					}
					if ( slice == null )
						continue;

					// New ray cast.
					final double y = iy * cal[ 1 ];
					slice.xRayCast( y, intersectionXs, cal[ 1 ] );

					// No intersection?
					if ( !intersectionXs.isEmpty() )
						break;

					// No intersection on this line, move to the next.
				}
			}
			// We have found the next position.

			// Is it inside?
			final double x = ix * cal[ 0 ];

			// Special case: only one intersection.
			if ( intersectionXs.size() == 1 )
			{
				if ( x == intersectionXs.getQuick( 0 ) )
				{
					hasNext = true;
					return;
				}
				else
				{
					continue;
				}
			}

			final int i = intersectionXs.binarySearch( x );
			if ( i >= 0 )
			{
				// Fall on an intersection exactly.
				hasNext = true;
				return;
			}
			final int ip = -( i + 1 );
			// Odd or even?
			if ( ip % 2 != 0 )
			{
				// Odd. We are inside.
				hasNext = true;
				return;
			}

			// Not inside, move to the next point.
		}
	}

	@Override
	public boolean hasNext()
	{
		return hasNext;
	}

	@Override
	public void jumpFwd( final long steps )
	{
		for ( int i = 0; i < steps; i++ )
			fwd();
	}

	@Override
	public T next()
	{
		fwd();
		return get();
	}

	@Override
	public long getLongPosition( final int d )
	{
		return ra.getLongPosition( d );
	}

	@Override
	public Cursor< T > copyCursor()
	{
		return new SpotMeshCursor<>( ra.copyRandomAccess(), mesh, bb, cal );
	}

	@Override
	public Cursor< T > copy()
	{
		return copyCursor();
	}

	@Override
	public int numDimensions()
	{
		return 3;
	}

	@Override
	public T get()
	{
		return ra.get();
	}

	private static final Map< Integer, Slice > buildSliceMap( final Mesh mesh, final float[] boundingBox, final double[] calibration )
	{
		// Pre-compute slices.
		final int minZ = ( int ) Math.ceil( boundingBox[ 2 ] / calibration[ 2 ] );
		final int maxZ = ( int ) Math.floor( boundingBox[ 5 ] / calibration[ 2 ] );
		final double[] zs = new double[ maxZ - minZ + 1 ];
		final List< Integer > sliceIndices = new ArrayList<>( zs.length );
		for ( int i = 0; i < zs.length; i++ )
		{
			zs[ i ] = ( minZ + i ) * calibration[ 2 ]; // physical coords.
			sliceIndices.add( minZ + i ); // pixel coordinates.
		}
		final List< Slice > slices = ZSlicer.slices( mesh, zs, calibration[ 2 ] );

		// Simplify below /14th of a pixel.
		final double epsilon = calibration[ 0 ] * 0.25;
		final List< Slice > simplifiedSlices = slices.stream()
				.map( s -> RamerDouglasPeucker.simplify( s, epsilon ) )
				.collect( Collectors.toList() );

		// Store in a map Z (integer) pos -> slice.
		final Map< Integer, Slice > sliceMap = new HashMap<>();
		for ( int i = 0; i < sliceIndices.size(); i++ )
			sliceMap.put( sliceIndices.get( i ), simplifiedSlices.get( i ) );

		return sliceMap;
	}

}
