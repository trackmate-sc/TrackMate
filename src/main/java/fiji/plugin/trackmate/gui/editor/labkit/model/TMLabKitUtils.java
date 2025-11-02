package fiji.plugin.trackmate.gui.editor.labkit.model;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
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
		final double[] rmin = spot.minAsDoubleArray();
		final double[] rmax = spot.maxAsDoubleArray();
		final double[] calibration = TMUtils.getSpatialCalibration( img );

		final AxisType[] axes = new AxisType[] { Axes.X, Axes.Y, Axes.Z };
		for ( int d = 0; d < max.length; d++ )
		{
			min[ d ] = Math.round( rmin[ d ] / calibration[ d ] );
			min[ d ] = Math.max( 0, min[ d ] );

			max[ d ] = Math.round( rmax[ d ] / calibration[ d ] );
			final AxisType axis = axes[ d ];
			final int axisDim = img.dimensionIndex( axis );
			final long imgMax = img.min( axisDim ) + img.dimension( axisDim );
			max[ d ] = Math.min( imgMax, max[ d ] );
		}
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
