package fiji.plugin.trackmate.features.spot;

import static fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory.MAX_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory.MEAN_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory.MEDIAN_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory.MIN_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory.STANDARD_DEVIATION;
import static fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory.TOTAL_INTENSITY;

import java.util.Iterator;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.util.SpotNeighborhood;
import net.imagej.ImgPlus;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

public class SpotIntensityAnalyzer< T extends RealType< T > > extends IndependentSpotFeatureAnalyzer< T >
{

	public SpotIntensityAnalyzer( final ImgPlus< T > img, final Iterator< Spot > spots )
	{
		super( img, spots );
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Compute descriptive statistics items for this spot. Implementation
	 * follows <a
	 * href=http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
	 * >Wikipedia/Algorithms for calculating variance</a>.
	 */
	@Override
	public final void process( final Spot spot )
	{
		// Prepare neighborhood
		final IterableInterval< T > neighborhood;
		final SpotRoi roi = spot.getRoi();
		if ( null != roi && img.numDimensions() == 2 )
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
			/*
			 * Hack around a bug in spot iterator causing it to never end if the
			 * size of the spot is lower than one pixel.
			 */
			final Cursor< T > cursor = neighborhood.cursor();
			cursor.fwd();
			final double val = cursor.get().getRealDouble();

			spot.putFeature( MEDIAN_INTENSITY, Double.valueOf( val ) );
			spot.putFeature( MIN_INTENSITY, Double.valueOf( val ) );
			spot.putFeature( MAX_INTENSITY, Double.valueOf( val ) );
			spot.putFeature( MEAN_INTENSITY, Double.valueOf( val ) );
			spot.putFeature( STANDARD_DEVIATION, Double.NaN );
			spot.putFeature( TOTAL_INTENSITY, Double.valueOf( val ) );
			return;
		}

		// For variance, kurtosis and skewness
		double sum = 0;
		double mean = 0;
		double M2 = 0;
		double delta, delta_n;
		double term1;
		int n1;

		// Others
		final double[] pixel_values = new double[ npixels ];
		int n = 0;
		for ( final T pixel : neighborhood )
		{
			final double val = pixel.getRealDouble();

			// For median, min and max
			pixel_values[ n ] = val;
			// For variance and mean
			sum += val;

			// For kurtosis
			n1 = n;
			n++;
			delta = val - mean;
			delta_n = delta / n;
			term1 = delta * delta_n * n1;
			mean = mean + delta_n;
			M2 = M2 + term1;
		}

		Util.quicksort( pixel_values, 0, npixels - 1 );
		final double median = pixel_values[ npixels / 2 ];
		final double min = pixel_values[ 0 ];
		final double max = pixel_values[ npixels - 1 ];
		mean = sum / npixels;
		final double variance = M2 / ( npixels - 1 );

		spot.putFeature( MEDIAN_INTENSITY, median );
		spot.putFeature( MIN_INTENSITY, min );
		spot.putFeature( MAX_INTENSITY, max );
		spot.putFeature( MEAN_INTENSITY, mean );
		spot.putFeature( STANDARD_DEVIATION, Math.sqrt( variance ) );
		spot.putFeature( TOTAL_INTENSITY, sum );

	}
}
