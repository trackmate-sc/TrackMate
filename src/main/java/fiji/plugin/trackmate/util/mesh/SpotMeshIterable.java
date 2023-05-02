package fiji.plugin.trackmate.util.mesh;

import java.util.Iterator;

import fiji.plugin.trackmate.SpotMesh;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessible;
import net.imglib2.RealLocalizable;

public class SpotMeshIterable< T > implements IterableInterval< T >, Localizable
{

	private final double[] calibration;

	private final RandomAccessible< T > img;

	private final SpotMesh sm;

	private final RealLocalizable center;

	public SpotMeshIterable(
			final RandomAccessible< T > img,
			final SpotMesh sm,
			final RealLocalizable center,
			final double[] calibration )
	{
		this.img = img;
		this.sm = sm;
		this.center = center;
		this.calibration = calibration;
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
		return Math.round( ( sm.boundingBox[ d ] + center.getFloatPosition( d ) ) / calibration[ d ] );
	}

	@Override
	public long max( final int d )
	{
		return Math.round( ( sm.boundingBox[ 3 + d ] + center.getFloatPosition( d ) ) / calibration[ d ] );
	}

	@Override
	public Cursor< T > cursor()
	{
		return new SpotMeshCursor<>( img.randomAccess(), sm, center, calibration );
	}

	@Override
	public Cursor< T > localizingCursor()
	{
		return cursor();
	}
}
