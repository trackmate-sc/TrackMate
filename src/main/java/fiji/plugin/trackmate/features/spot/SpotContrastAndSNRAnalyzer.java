package fiji.plugin.trackmate.features.spot;

import static fiji.plugin.trackmate.features.spot.SpotContrastAndSNRAnalyzerFactory.CONTRAST;
import static fiji.plugin.trackmate.features.spot.SpotContrastAndSNRAnalyzerFactory.SNR;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.MEAN_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.STD_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.TOTAL_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.makeFeatureKey;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.util.SpotNeighborhood;
import fiji.plugin.trackmate.util.SpotNeighborhoodCursor;
import net.imagej.ImgPlus;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;

/**
 * This {@link fiji.plugin.trackmate.features.FeatureAnalyzer} computes both the
 * <a href=http://en.wikipedia.org/wiki/Michelson_contrast#Formula>Michelson
 * contrast</a> and the SNR for each spot:
 * <p>
 * The contrast is defined as <code>C = (I_in - I_out) / (I_in + I_out)</code>
 * where <code>I_in</code> is the mean intensity inside the spot volume
 * (computed from its {@link Spot#RADIUS} feature), and <code>I_out</code> is
 * the mean intensity in a ring ranging from its radius to twice its radius.
 * <p>
 * The spots's SNR is computed a <code>(I_in - I_out) / std_in</code> where
 * <code>std_in</code> is the standard deviation computed within the spot.
 * <p>
 * <u>Important</u>: this analyzer relies on some results provided by the
 * {@link SpotIntensityAnalyzer} analyzer. Thus, it <b>must</b> be run after it.
 * 
 * @author Jean-Yves Tinevez, 2011 - 2012. Revised December 2020.
 */
public class SpotContrastAndSNRAnalyzer< T extends RealType< T > > extends AbstractSpotFeatureAnalyzer< T >
{

	protected static final double RAD_PERCENTAGE = 1f;

	private final int channel;

	private final ImgPlus< T > img;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * Instantiates an analyzer for contrast and SNR.
	 * 
	 * @param img
	 *            the 2D or 3D image of the desired time-point and channel to
	 *            operate on,
	 * @param channel
	 *            the channel to operate on.
	 */
	public SpotContrastAndSNRAnalyzer( final ImgPlus< T > img, final int channel )
	{
		this.img = img;
		this.channel = channel;
	}

	/*
	 * METHODS
	 */

	@Override
	public final void process( final Spot spot )
	{
		final String meanFeature = makeFeatureKey( MEAN_INTENSITY, channel );
		final String stdFeature = makeFeatureKey( STD_INTENSITY, channel );
		final double meanIn = spot.getFeature( meanFeature );
		final double stdIn = spot.getFeature( stdFeature );
		final double radius = spot.getFeature( Spot.RADIUS );
		final double outterRadius = 2. * radius;

		// Operate on ROI only if we have one and the image is 2D.
		final double meanOut;
		final SpotRoi roi = spot.getRoi();
		if ( null != roi && DetectionUtils.is2D( img ) )
		{
			final double alpha = outterRadius / radius;
			final SpotRoi outterRoi = roi.copy();
			outterRoi.scale( alpha );
			final IterableInterval< T > neighborhood = outterRoi.sample( spot, img );
			double outterSum = 0.;
			for ( final T t : neighborhood )
				outterSum += t.getRealDouble();

			final String sumFeature = makeFeatureKey( TOTAL_INTENSITY, channel );
			final double innterSum = spot.getFeature( sumFeature );
			outterSum -= innterSum;
			meanOut = outterSum / ( outterRoi.area() - roi.area() );
		}
		else
		{
			// Otherwise default to circle / sphere.
			final Spot largeSpot = new Spot( spot );
			largeSpot.putFeature( Spot.RADIUS, outterRadius );
			final SpotNeighborhood< T > neighborhood = new SpotNeighborhood<>( largeSpot, img );
			if ( neighborhood.size() <= 1 )
			{
				spot.putFeature( makeFeatureKey( CONTRAST, channel ), Double.NaN );
				spot.putFeature( makeFeatureKey( SNR, channel ), Double.NaN );
				return;
			}

			final double radius2 = radius * radius;
			int nOut = 0; // inner number of pixels
			double sumOut = 0;

			// Compute mean in the outer ring
			final SpotNeighborhoodCursor< T > cursor = neighborhood.cursor();
			while ( cursor.hasNext() )
			{
				cursor.fwd();
				final double dist2 = cursor.getDistanceSquared();
				if ( dist2 > radius2 )
				{
					nOut++;
					sumOut += cursor.get().getRealDouble();
				}
			}
			meanOut = sumOut / nOut;
		}

		// Compute contrast
		final double contrast = ( meanIn - meanOut ) / ( meanIn + meanOut );

		// Compute snr
		final double snr = ( meanIn - meanOut ) / stdIn;

		spot.putFeature( makeFeatureKey( CONTRAST, channel ), contrast );
		spot.putFeature( makeFeatureKey( SNR, channel ), snr );
	}
}
