package fiji.plugin.trackmate.util.mesh;

import java.util.Iterator;

import fiji.plugin.trackmate.SpotMesh;
import net.imagej.mesh.Meshes;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessible;
import net.imglib2.RealPoint;

public class SpotMeshIterable< T > implements IterableInterval< T >, Localizable
{

	private final double[] calibration;

	private final SpotMesh sm;

	private final RealPoint center;

	private final RandomAccessible< T > img;

	public SpotMeshIterable( final RandomAccessible< T > img, final SpotMesh sm, final double[] calibration )
	{
		this.img = img;
		this.sm = sm;
		this.calibration = calibration;
		this.center = Meshes.center( sm.mesh );
	}

	@Override
	public int numDimensions()
	{
		return 3;
	}

	@Override
	public long getLongPosition( final int d )
	{
		return Math.round( center.getDoublePosition( d ) / calibration[ d ] );
	}

	@Override
	public long size()
	{
		// Costly!
		long size = 0;
		for ( @SuppressWarnings( "unused" )
		final T t : this )
			size++;

		return size;
	}

	@Override
	public T firstElement()
	{
		return cursor().next();
	}

	@Override
	public Object iterationOrder()
	{
		return this;
	}

	@Override
	public Iterator< T > iterator()
	{
		return cursor();
	}

	@Override
	public long min( final int d )
	{
		return Math.round( sm.boundingBox[ d ] / calibration[ d ] );
	}

	@Override
	public long max( final int d )
	{
		return Math.round( sm.boundingBox[ 3 + d ] / calibration[ d ] );
	}

	@Override
	public Cursor< T > cursor()
	{
		return new SpotMeshCursor<>( img.randomAccess(), sm, calibration );
	}

	@Override
	public Cursor< T > localizingCursor()
	{
		return cursor();
	}
}
