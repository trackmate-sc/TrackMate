package fiji.plugin.trackmate.features.spot;

import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.MAX_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.MEAN_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.MEDIAN_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.MIN_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.STD_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.TOTAL_INTENSITY;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.util.SpotNeighborhood;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class SpotIntensityMultiCAnalyzer< T extends RealType< T > > extends AbstractSpotFeatureAnalyzer< T >
{

	private final int channel;

	private final ImgPlus< T > imgCT;

	public SpotIntensityMultiCAnalyzer( final ImgPlus< T > imgCT, final int channel )
	{
		this.imgCT = imgCT;
		this.channel = channel;
	}

	@Override
	public void process( final Spot spot )
	{
		// Prepare neighborhood
		final IterableInterval< T > neighborhood;
		final SpotRoi roi = spot.getRoi();
		if ( null != roi && DetectionUtils.is2D( imgCT ) )
		{
			// Operate on ROI only if we have one and the image is 2D.
			neighborhood = roi.sample( spot, imgCT );
		}
		else
		{
			// Otherwise default to circle / sphere.
			neighborhood = new SpotNeighborhood<>( spot, imgCT );
		}

		final int npixels = ( int ) neighborhood.size();
		if ( npixels <= 1 )
		{
			/*
			 * Hack around a bug in spot iterator causing it to never end if the
			 * size of the spot is lower than one pixel.
			 */
			final double[] calibration = TMUtils.getSpatialCalibration( imgCT );
			// Center
			final long[] center = new long[ imgCT.numDimensions() ];
			for ( int d = 0; d < center.length; d++ )
				center[ d ] = Math.round( spot.getFeature( Spot.POSITION_FEATURES[ d ] ).doubleValue() / calibration[ d ] );

			final RandomAccess< T > ra = Views.extendZero( imgCT ).randomAccess();
			ra.setPosition( center );
			final double val = ra.get().getRealDouble();

			spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( MEAN_INTENSITY, channel ), Double.valueOf( val ) );
			spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( MEDIAN_INTENSITY, channel ), Double.valueOf( val ) );
			spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( MIN_INTENSITY, channel ), Double.valueOf( val ) );
			spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( MAX_INTENSITY, channel ), Double.valueOf( val ) );
			spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( TOTAL_INTENSITY, channel ), Double.valueOf( val ) );
			spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( STD_INTENSITY, channel ), Double.NaN );
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
			spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( MEAN_INTENSITY, channel ), Double.valueOf( Util.average( intensities ) ) );
			spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( MEDIAN_INTENSITY, channel ), Double.valueOf( intensities[ intensities.length / 2 ] ) );
			spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( MIN_INTENSITY, channel ), Double.valueOf( intensities[ 0 ] ) );
			spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( MAX_INTENSITY, channel ), Double.valueOf( intensities[ intensities.length - 1 ] ) );
			spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( TOTAL_INTENSITY, channel ), Double.valueOf( TMUtils.sum( intensities ) ) );
			spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( STD_INTENSITY, channel ), Double.valueOf( TMUtils.standardDeviation( intensities ) ) );
		}
	}
}
