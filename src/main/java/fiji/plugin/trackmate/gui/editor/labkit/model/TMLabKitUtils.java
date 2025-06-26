package fiji.plugin.trackmate.gui.editor.labkit.model;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.util.Util;
import sc.fiji.labkit.ui.labeling.Labeling;

public class TMLabKitUtils
{

	static final int timeAxis( final Labeling labeling )
	{
		final List< CalibratedAxis > axes = labeling.axes();
		for ( int d = 0; d < axes.size(); d++ )
		{
			final CalibratedAxis axis = axes.get( d );
			if (axis.type().equals( Axes.TIME ))
				return d;
		}
		return -1;
	}

	static final void boundingBox( final Spot spot, final ImgPlus< UnsignedIntType > img, final long[] min, final long[] max )
	{
		final double[] calibration = TMUtils.getSpatialCalibration( img );
		final SpotRoi roi = spot.getRoi();
		if ( roi == null )
		{
			final double cx = spot.getDoublePosition( 0 );
			final double cy = spot.getDoublePosition( 1 );
			final double r = spot.getFeature( Spot.RADIUS ).doubleValue();
			min[ 0 ] = ( long ) Math.floor( ( cx - r ) / calibration[ 0 ] );
			min[ 1 ] = ( long ) Math.floor( ( cy - r ) / calibration[ 1 ] );
			max[ 0 ] = ( long ) Math.ceil( ( cx + r ) / calibration[ 0 ] );
			max[ 1 ] = ( long ) Math.ceil( ( cy + r ) / calibration[ 1 ] );
		}
		else
		{
			final double[] x = roi.toPolygonX( calibration[ 0 ], 0, spot.getDoublePosition( 0 ), 1. );
			final double[] y = roi.toPolygonY( calibration[ 1 ], 0, spot.getDoublePosition( 1 ), 1. );
			min[ 0 ] = ( long ) Math.floor( Util.min( x ) );
			min[ 1 ] = ( long ) Math.floor( Util.min( y ) );
			max[ 0 ] = ( long ) Math.ceil( Util.max( x ) );
			max[ 1 ] = ( long ) Math.ceil( Util.max( y ) );
		}

		min[ 0 ] = Math.max( 0, min[ 0 ] );
		min[ 1 ] = Math.max( 0, min[ 1 ] );
		final long width = img.min( img.dimensionIndex( Axes.X ) ) + img.dimension( img.dimensionIndex( Axes.X ) );
		final long height = img.min( img.dimensionIndex( Axes.Y ) ) + img.dimension( img.dimensionIndex( Axes.Y ) );
		max[ 0 ] = Math.min( width, max[ 0 ] );
		max[ 1 ] = Math.min( height, max[ 1 ] );
	}

	static final Img< UnsignedIntType > copy( final RandomAccessibleInterval< UnsignedIntType > in )
	{
		final ImgFactory< UnsignedIntType > factory = Util.getArrayOrCellImgFactory( in, in.getType() );
		final Img< UnsignedIntType > out = factory.create( in );
		LoopBuilder.setImages( in, out )
				.multiThreaded()
				.forEachPixel( ( i, o ) -> o.setInteger( i.getInteger() ) );
		return out;
	}

	public static final boolean isDifferent(
			final RandomAccessibleInterval< UnsignedIntType > previousIndexImg,
			final RandomAccessibleInterval< UnsignedIntType > indexImg )
	{
		final AtomicBoolean modified = new AtomicBoolean( false );
		LoopBuilder.setImages( previousIndexImg, indexImg )
				.multiThreaded()
				.forEachChunk( chunk -> {
					if ( modified.get() )
						return null;
					chunk.forEachPixel( ( p1, p2 ) -> {
						if ( p1.getInteger() != p2.getInteger() )
						{
							modified.set( true );
							return;
						}
					} );
					return null;
				} );
		return modified.get();
	}

	static final Set< Integer > getModifiedIndices(
			final RandomAccessibleInterval< UnsignedIntType > img1,
			final RandomAccessibleInterval< UnsignedIntType > img2 )
	{
		final ConcurrentSkipListSet< Integer > modifiedIDs = new ConcurrentSkipListSet<>();
		LoopBuilder.setImages( img1, img2 )
				.multiThreaded( false )
				.forEachPixel( ( c, p ) -> {
					final int ci = c.getInteger();
					final int pi = p.getInteger();
					if ( ci == 0 && pi == 0 )
						return;
					if ( ci != pi )
					{
						modifiedIDs.add( Integer.valueOf( pi ) );
						modifiedIDs.add( Integer.valueOf( ci ) );
					}
				} );
		modifiedIDs.remove( Integer.valueOf( 0 ) );
		return modifiedIDs;
	}
}
