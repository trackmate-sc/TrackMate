package fiji.plugin.trackmate.features.spot;

import static fiji.plugin.trackmate.features.spot.SpotContrastAndSNRAnalyzerFactory.CONTRAST;
import static fiji.plugin.trackmate.features.spot.SpotContrastAndSNRAnalyzerFactory.SNR;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.MEAN_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.STD_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.makeFeatureKey;

import java.util.Iterator;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.SpotNeighborhood;
import fiji.plugin.trackmate.util.SpotNeighborhoodCursor;
import net.imagej.ImgPlus;
import net.imglib2.meta.view.HyperSliceImgPlus;
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
@SuppressWarnings( "deprecation" )
public class SpotContrastAndSNRAnalyzer< T extends RealType< T > > extends IndependentSpotFeatureAnalyzer< T >
{

	protected static final double RAD_PERCENTAGE = 1f;

	private final int nChannels;

	/*
	 * CONSTRUCTOR
	 */

	public SpotContrastAndSNRAnalyzer( final ImgPlus< T > img, final Iterator< Spot > spots, final int nChannels )
	{
		super( img, spots );
		this.nChannels = nChannels;
	}

	/*
	 * METHODS
	 */

	@Override
	public final void process( final Spot spot )
	{
		for ( int c = 0; c < nChannels; c++ )
		{
			final double[] vals = getContrastAndSNR( spot, c );
			final double contrast = vals[ 0 ];
			final double snr = vals[ 1 ];

			spot.putFeature( makeFeatureKey( CONTRAST, c ), contrast );
			spot.putFeature( makeFeatureKey( SNR, c ), snr );
		}
	}

	/**
	 * Compute the contrast for the given spot.
	 * 
	 * @param c
	 *            the channel to compute on.
	 */
	private final double[] getContrastAndSNR( final Spot spot, final int c )
	{
		final ImgPlus< T > imgC = HyperSliceImgPlus.fixChannelAxis( img, c );

		final String meanFeature = makeFeatureKey( MEAN_INTENSITY, c );
		final String stdFeature = makeFeatureKey( STD_INTENSITY, c );
		final double meanIn = spot.getFeature( meanFeature );
		final double stdIn = spot.getFeature( stdFeature );

		final double radius = spot.getFeature( Spot.RADIUS );
		final Spot largeSpot = new Spot( spot );
		largeSpot.putFeature( Spot.RADIUS, 2 * radius );

		final SpotNeighborhood< T > neighborhood = new SpotNeighborhood<>( largeSpot, imgC );
		if ( neighborhood.size() <= 1 )
			return new double[] { Double.NaN, Double.NaN };

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
		final double meanOut = sumOut / nOut;

		// Compute contrast
		final double contrast = ( meanIn - meanOut ) / ( meanIn + meanOut );

		// Compute snr
		final double snr = ( meanIn - meanOut ) / stdIn;

		return new double[] { contrast, snr };
	}
}
