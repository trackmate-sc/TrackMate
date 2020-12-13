package fiji.plugin.trackmate.features.spot;

import java.util.Iterator;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.util.SpotNeighborhood;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.meta.view.HyperSliceImgPlus;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

@SuppressWarnings( "deprecation" )
public class SpotMeanIntensityMultiCAnalyzer< T extends RealType< T > > extends IndependentSpotFeatureAnalyzer< T >
{

	private final int nChannels;

	public SpotMeanIntensityMultiCAnalyzer( final ImgPlus< T > imgT, final Iterator< Spot > spots, final int nChannels )
	{
		super( imgT, spots );
		this.nChannels = nChannels;
	}

	@Override
	public void process( final Spot spot )
	{
		for ( int c = 0; c < nChannels; c++ )
		{
			final ImgPlus< T > imgC = HyperSliceImgPlus.fixChannelAxis( img, c );

			// Prepare neighborhood
			final IterableInterval< T > neighborhood;
			final SpotRoi roi = spot.getRoi();
			if ( null != roi && imgC.numDimensions() == 2 )
			{
				// Operate on ROI only if we have one and the image is 2D.
				neighborhood = roi.sample( spot, imgC );
			}
			else
			{
				// Otherwise default to circle / sphere.
				neighborhood = new SpotNeighborhood<>( spot, imgC );
			}


			final int npixels = ( int ) neighborhood.size();
			if ( npixels <= 1 )
			{
				/*
				 * Hack around a bug in spot iterator causing it to never end if
				 * the size of the spot is lower than one pixel.
				 */
				final double[] calibration = TMUtils.getSpatialCalibration( img );
				// Center
				final long[] center = new long[ img.numDimensions() ];
				for ( int d = 0; d < center.length; d++ )
					center[ d ] = Math.round( spot.getFeature( Spot.POSITION_FEATURES[ d ] ).doubleValue() / calibration[ d ] );

				final RandomAccess< T > ra = Views.extendZero( img ).randomAccess();
				ra.setPosition( center );
				final double val = ra.get().getRealDouble();
				spot.putFeature( SpotMeanIntensityMultiCAnalyzerFactory.makeFeatureKey( c ), Double.valueOf( val ) );
			}
			else
			{
				int n = 0;
				double sum = 0;
				for ( final T pixel : neighborhood )
				{
					final double val = pixel.getRealDouble();
					sum += val;
					n++;
				}
				final double mean = sum / n;
				spot.putFeature( SpotMeanIntensityMultiCAnalyzerFactory.makeFeatureKey( c ), mean );
			}
		}
	}
}
