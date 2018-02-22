package fiji.plugin.trackmate.features.spot;

import static fiji.plugin.trackmate.features.spot.SpotContrastAndSNRAnalyzerFactory.CONTRAST;
import static fiji.plugin.trackmate.features.spot.SpotContrastAndSNRAnalyzerFactory.SNR;

import java.util.Iterator;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.SpotNeighborhood;
import fiji.plugin.trackmate.util.SpotNeighborhoodCursor;
import net.imagej.ImgPlus;
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
 * @author Jean-Yves Tinevez &lt;jeanyves.tinevez@gmail.com&gt; 2011 - 2012
 */
public class SpotContrastAndSNRAnalyzer< T extends RealType< T > > extends IndependentSpotFeatureAnalyzer< T >
{

	protected static final double RAD_PERCENTAGE = 1f;

	/*
	 * CONSTRUCTOR
	 */

	public SpotContrastAndSNRAnalyzer( final ImgPlus< T > img, final Iterator< Spot > spots )
	{
		super( img, spots );
	}

	/*
	 * METHODS
	 */

	@Override
	public final void process( final Spot spot )
	{
		final double[] vals = getContrastAndSNR( spot );
		final double contrast = vals[ 0 ];
		final double snr = vals[ 1 ];
		spot.putFeature( CONTRAST, contrast );
		spot.putFeature( SNR, snr );
	}

	/**
	 * Compute the contrast for the given spot.
	 */
	private final double[] getContrastAndSNR( final Spot spot )
	{
		final double mean_in = spot.getFeature( SpotIntensityAnalyzerFactory.MEAN_INTENSITY );
		final double std_in = spot.getFeature( SpotIntensityAnalyzerFactory.STANDARD_DEVIATION );

		final double radius = spot.getFeature( Spot.RADIUS );
		Spot largeSpot = new Spot( spot );
		largeSpot.putFeature( Spot.RADIUS, 2 * radius );

		final SpotNeighborhood< T > neighborhood = new SpotNeighborhood<>( largeSpot, img );
		if ( neighborhood.size() <= 1 ) { return new double[] { Double.NaN, Double.NaN }; }

		final double radius2 = radius * radius;
		int n_out = 0; // inner number of pixels
		double dist2;
		double sum_out = 0;

		// Compute mean in the outer ring
		final SpotNeighborhoodCursor< T > cursor = neighborhood.cursor();
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			dist2 = cursor.getDistanceSquared();
			if ( dist2 > radius2 )
			{
				n_out++;
				sum_out += cursor.get().getRealFloat();
			}
		}
		final double mean_out = sum_out / n_out;

		// Compute contrast
		final double contrast = ( mean_in - mean_out ) / ( mean_in + mean_out );

		// Compute snr
		final double snr = ( mean_in - mean_out ) / std_in;

		final double[] ret = new double[ 2 ];
		ret[ 0 ] = contrast;
		ret[ 1 ] = snr;
		return ret;
	}
}
