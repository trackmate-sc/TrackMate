package fiji.plugin.trackmate.features.spot;

import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.MAX_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.MEAN_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.MEDIAN_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.MIN_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.STD_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.TOTAL_INTENSITY;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.SpotUtil;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

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
		final IterableInterval< T > neighborhood = SpotUtil.iterable( spot, imgCT );
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
