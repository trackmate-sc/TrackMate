package fiji.plugin.trackmate.features.spot;

import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.MAX_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.MEAN_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.MEDIAN_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.MIN_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.STD_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.TOTAL_INTENSITY;

import java.util.Iterator;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.util.SpotNeighborhood;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.meta.view.HyperSliceImgPlus;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

@SuppressWarnings( "deprecation" )
public class SpotIntensityMultiCAnalyzer< T extends RealType< T > > extends IndependentSpotFeatureAnalyzer< T >
{

	private final int nChannels;

	public SpotIntensityMultiCAnalyzer( final ImgPlus< T > imgT, final Iterator< Spot > spots, final int nChannels )
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
			if ( null != roi && DetectionUtils.is2D( imgC ) )
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

				spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( MEAN_INTENSITY, c ), Double.valueOf( val ) );
				spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( MEDIAN_INTENSITY, c ), Double.valueOf( val ) );
				spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( MIN_INTENSITY, c ), Double.valueOf( val ) );
				spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( MAX_INTENSITY, c ), Double.valueOf( val ) );
				spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( TOTAL_INTENSITY, c ), Double.valueOf( val ) );
				spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( STD_INTENSITY, c ), Double.valueOf( val ) );
			}
			else
			{
				final double[] intensities = new double[ ( int ) neighborhood.size() ];
				int n = 0;
				for ( final T pixel : neighborhood )
				{
					final double val = pixel.getRealDouble();
					intensities[ n++ ] = val;
				}
				Util.quicksort( intensities );
				spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( MEAN_INTENSITY, c ), Double.valueOf( Util.average( intensities ) ) );
				spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( MEDIAN_INTENSITY, c ), Double.valueOf( intensities[ intensities.length / 2 ] ) );
				spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( MIN_INTENSITY, c ), Double.valueOf( intensities[ 0 ] ) );
				spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( MAX_INTENSITY, c ), Double.valueOf( intensities[ intensities.length - 1 ] ) );
				spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( TOTAL_INTENSITY, c ), Double.valueOf( TMUtils.sum( intensities ) ) );
				spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( STD_INTENSITY, c ), Double.valueOf( TMUtils.standardDeviation( intensities ) ) );
			}
		}
	}
}
