package fiji.plugin.trackmate.util;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.detection.DetectionUtils;
import net.imagej.ImgPlus;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class SpotUtil
{

	public static final < T extends RealType< T > > IterableInterval< T > iterable( final Spot spot, final ImgPlus< T > img )
	{
		// Prepare neighborhood
		final IterableInterval< T > neighborhood;
		final SpotRoi roi = spot.getRoi();
		if ( null != roi && DetectionUtils.is2D( img ) )
		{
			// Operate on ROI only if we have one and the image is 2D.
			neighborhood = roi.sample( spot, img );
		}
		else
		{
			// Otherwise default to circle / sphere.
			neighborhood = new SpotNeighborhood<>( spot, img );
		}

		final int npixels = ( int ) neighborhood.size();
		if ( npixels <= 1 )
		{
			final double[] calibration = TMUtils.getSpatialCalibration( img );
			final long[] min = new long[ img.numDimensions() ];
			final long[] max = new long[ img.numDimensions() ];
			for ( int d = 0; d < min.length; d++ )
			{
				final long center = Math.round( spot.getFeature( Spot.POSITION_FEATURES[ d ] ).doubleValue() / calibration[ d ] );
				min[ d ] = center;
				max[ d ] = center + 1;
			}

			final Interval interval = new FinalInterval( min, max );
			return Views.interval( img, interval );
		}
		else
		{
			return neighborhood;
		}
	}
}
