package fiji.plugin.trackmate.util;

import fiji.plugin.trackmate.Spot;
import net.imagej.ImgPlus;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Positionable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPositionable;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.region.localneighborhood.AbstractNeighborhood;
import net.imglib2.algorithm.region.localneighborhood.EllipseNeighborhood;
import net.imglib2.algorithm.region.localneighborhood.EllipsoidNeighborhood;
import net.imglib2.algorithm.region.localneighborhood.RectangleNeighborhoodGPL;
import net.imglib2.outofbounds.OutOfBoundsMirrorExpWindowingFactory;
import net.imglib2.type.numeric.RealType;

public class SpotNeighborhood< T extends RealType< T >> implements Neighborhood< T >
{

	/*
	 * FIELDS
	 */

	protected final double[] calibration;

	protected final AbstractNeighborhood< T > neighborhood;

	protected final long[] center;

	/*
	 * CONSTRUCTOR
	 */

	public SpotNeighborhood( final Spot spot, final ImgPlus< T > img )
	{
		this.calibration = TMUtils.getSpatialCalibration( img );
		// Center
		this.center = new long[ img.numDimensions() ];
		for ( int d = 0; d < center.length; d++ )
		{
			center[ d ] = Math.round( spot.getFeature( Spot.POSITION_FEATURES[ d ] ).doubleValue() / calibration[ d ] );
		}
		// Span
		final long[] span = new long[ img.numDimensions() ];
		for ( int d = 0; d < span.length; d++ )
		{
			span[ d ] = Math.round( spot.getFeature( Spot.RADIUS ) / calibration[ d ] );
		}

		// Neighborhood

		/*
		 * We have to detect here whether we were given a 1D image. Trouble is,
		 * since it is an ImgPlus, it will always be of fim at least 2. So we
		 * have to test pedantically.
		 */

		final OutOfBoundsMirrorExpWindowingFactory< T, RandomAccessibleInterval< T >> oob = new OutOfBoundsMirrorExpWindowingFactory<>();
		if ( img.numDimensions() == 2 && img.dimension( 0 ) < 2 || img.dimension( 1 ) < 2 )
		{
			if ( img.dimension( 0 ) < 2 )
			{
				span[ 0 ] = 0;
			}
			else
			{
				span[ 1 ] = 0;
			}
			this.neighborhood = new RectangleNeighborhoodGPL< >( img, oob );
			neighborhood.setPosition( center );
			neighborhood.setSpan( span );
		}
		else if ( img.numDimensions() == 2 )
		{
			this.neighborhood = new EllipseNeighborhood< >( img, center, span, oob );
		}
		else if ( img.numDimensions() == 3 )
		{
			this.neighborhood = new EllipsoidNeighborhood< >( img, center, span, oob );
		}
		else
		{
			throw new IllegalArgumentException( "Source input must be 1D, 2D or 3D, got nDims = " + img.numDimensions() );
		}

	}

	/*
	 * METHODS We delegate everything to the wrapped neighborhood
	 */

	@Override
	public final SpotNeighborhoodCursor< T > cursor()
	{
		return new SpotNeighborhoodCursor< >( this );
	}

	@Override
	public SpotNeighborhoodCursor< T > localizingCursor()
	{
		return cursor();
	}

	@Override
	public long size()
	{
		return neighborhood.size();
	}

	@Override
	public T firstElement()
	{
		return neighborhood.firstElement();
	}

	@Override
	public Object iterationOrder()
	{
		return neighborhood.iterationOrder();
	}

	@Override
	public double realMin( final int d )
	{
		return neighborhood.realMin( d );
	}

	@Override
	public void realMin( final double[] min )
	{
		neighborhood.realMin( min );

	}

	@Override
	public void realMin( final RealPositionable min )
	{
		neighborhood.realMin( min );

	}

	@Override
	public double realMax( final int d )
	{
		return neighborhood.realMax( d );
	}

	@Override
	public void realMax( final double[] max )
	{
		neighborhood.realMax( max );
	}

	@Override
	public void realMax( final RealPositionable max )
	{
		neighborhood.realMax( max );
	}

	@Override
	public int numDimensions()
	{
		return neighborhood.numDimensions();
	}

	@Override
	public SpotNeighborhoodCursor< T > iterator()
	{
		return cursor();
	}

	@Override
	public long min( final int d )
	{
		return neighborhood.min( d );
	}

	@Override
	public void min( final long[] min )
	{
		neighborhood.min( min );
	}

	@Override
	public void min( final Positionable min )
	{
		neighborhood.min( min );
	}

	@Override
	public long max( final int d )
	{
		return neighborhood.max( d );
	}

	@Override
	public void max( final long[] max )
	{
		neighborhood.max( max );
	}

	@Override
	public void max( final Positionable max )
	{
		neighborhood.max( max );
	}

	@Override
	public void dimensions( final long[] dimensions )
	{
		neighborhood.dimensions( dimensions );
	}

	@Override
	public long dimension( final int d )
	{
		return neighborhood.dimension( d );
	}

	@Override
	public void localize( final int[] position )
	{
		for ( int d = 0; d < position.length; d++ )
		{
			position[ d ] = ( int ) center[ d ];
		}
	}

	@Override
	public void localize( final long[] position )
	{
		for ( int d = 0; d < position.length; d++ )
		{
			position[ d ] = center[ d ];
		}
	}

	@Override
	public int getIntPosition( final int d )
	{
		return ( int ) center[ d ];
	}

	@Override
	public long getLongPosition( final int d )
	{
		return center[ d ];
	}

	@Override
	public void localize( final float[] position )
	{
		for ( int d = 0; d < position.length; d++ )
		{
			position[ d ] = center[ d ];
		}
	}

	@Override
	public void localize( final double[] position )
	{
		for ( int d = 0; d < position.length; d++ )
		{
			position[ d ] = center[ d ];
		}
	}

	@Override
	public float getFloatPosition( final int d )
	{
		return center[ d ];
	}

	@Override
	public double getDoublePosition( final int d )
	{
		return center[ d ];
	}

	@Override
	public Interval getStructuringElementBoundingBox()
	{
		final long[] min = new long[ numDimensions() ];
		final long[] max = new long[ numDimensions() ];
		min( min );
		max( max );
		final FinalInterval interval = new FinalInterval( min, max );
		return interval;
	}

}
