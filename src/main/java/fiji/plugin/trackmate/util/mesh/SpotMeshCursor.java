package fiji.plugin.trackmate.util.mesh;

import fiji.plugin.trackmate.SpotMesh;
import gnu.trove.list.array.TDoubleArrayList;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Meshes;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.Sampler;

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

	private final long minX;

	private final long maxX;

	private final long minY;

	private final long maxY;

	private final long minZ;

	private final long maxZ;

	private final RandomAccess< T > ra;

	private boolean hasNext;

	private long iy;

	private long iz;

	private long ix;

	/** Ray casting algorithm. */
	private final RayCastingX rayCasting;

	/**
	 * List of resolved X positions where we enter / exit the mesh. Set by the
	 * ray casting algorithm.
	 */
	private final TDoubleArrayList meshXs = new TDoubleArrayList();

	/** List of normal X component where we enter / exit the mesh. */
	private final TDoubleArrayList meshNs = new TDoubleArrayList();

	/** X position of the next (forward in X) intersection with the mesh. */
	private double nextXIntersection;

	/** X component of the normal at the next intersection with the mesh. */
	private double nextNormal;

	/** Index of the next intersection in the {@link #meshXs} list. */
	private int indexNextXIntersection;

	private Mesh mesh;

	public SpotMeshCursor( final RandomAccess< T > ra, final SpotMesh sm, final double[] cal )
	{
		this( ra, sm.mesh, sm.boundingBox, cal );
	}

	public SpotMeshCursor( final RandomAccess< T > ra, final Mesh mesh, final double[] cal )
	{
		this( ra, mesh, Meshes.boundingBox( mesh ), cal );
	}

	public SpotMeshCursor( final RandomAccess< T > ra, final Mesh mesh, final float[] boundingBox, final double[] cal )
	{
		this.ra = ra;
		this.mesh = mesh;
		this.cal = cal;
		this.bb = boundingBox;
		this.minX = Math.round( bb[ 0 ] / cal[ 0 ] );
		this.maxX = Math.round( bb[ 3 ] / cal[ 0 ] );
		this.minY = Math.round( bb[ 1 ] / cal[ 1 ] );
		this.maxY = Math.round( bb[ 4 ] / cal[ 1 ] );
		this.minZ = Math.round( bb[ 2 ] / cal[ 2 ] );
		this.maxZ = Math.round( bb[ 5 ] / cal[ 2 ] );
		this.rayCasting = new RayCastingX( mesh );
		reset();
	}


	@Override
	public void reset()
	{
		this.ix = maxX; // To force a new ray cast when we call fwd()
		this.iy = minY - 1; // Then we will move to minY.
		this.iz = minZ;
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
					}

					// New ray cast.
					final double z = iz * cal[ 2 ];
					final double y = iy * cal[ 1 ];
					rayCasting.cast( y, z, meshXs, meshNs );

					// No intersection?
					if ( !meshXs.isEmpty() )
					{
						this.indexNextXIntersection = 0;
						this.nextXIntersection = meshXs.getQuick( 0 );
						this.nextNormal = meshNs.getQuick( 0 );
						break;
					}
					// No intersection on this line, move to the next.
				}
			}
			// We have found the next position.

			// Is it inside?
			final double x = ix * cal[ 0 ];

			// Special case: only one intersection.
			if ( meshXs.size() == 1 )
			{
				if ( x == nextXIntersection )
				{
					hasNext = true;
					return;
				}
				else
				{
					continue;
				}
			}

			if ( x >= nextXIntersection )
			{
				indexNextXIntersection++;
				if ( indexNextXIntersection >= meshXs.size() )
				{
					final boolean inside = ( x == meshXs.get( meshXs.size() - 1 ) );
					if ( inside )
					{
						hasNext = true;
						return;
					}
				}
				else
				{
					final boolean isEntry = ( nextNormal < 0. ) || ( ix == nextXIntersection );
					nextXIntersection = meshXs.getQuick( indexNextXIntersection );
					nextNormal = meshNs.getQuick( indexNextXIntersection );
					if ( isEntry )
					{
						hasNext = true;
						return;
					}
				}
			}
			else
			{
				if ( nextNormal > 0. )
				{
					hasNext = true;
					return;
				}
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
	public Sampler< T > copy()
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

}
