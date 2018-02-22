package fiji.plugin.trackmate.features.spot;

import static fiji.plugin.trackmate.features.spot.SpotRadiusEstimatorFactory.ESTIMATED_DIAMETER;

import java.util.Iterator;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.SpotNeighborhood;
import fiji.plugin.trackmate.util.SpotNeighborhoodCursor;
import net.imagej.ImgPlus;
import net.imglib2.type.numeric.RealType;

public class SpotRadiusEstimator< T extends RealType< T >> extends IndependentSpotFeatureAnalyzer< T >
{

	/*
	 * FIELDS
	 */

	public static final double MIN_DIAMETER_RATIO = 0.1f;

	public static final double MAX_DIAMETER_RATIO = 2;

	/** The number of different diameters to try. */
	protected int nDiameters = 20;

	/**
	 * Create a feature analyzer that will return the best estimated diameter
	 * for a spot. Estimated diameter is obtained by finding the diameter that
	 * gives the maximum contrast, as calculated by difference in mean intensity
	 * in successive rings. Searched diameters are linearly spread between
	 * <code>diameter</code> {@value #MIN_DIAMETER_RATIO} and
	 * <code>diameter</code> {@value #MAX_DIAMETER_RATIO}. The optimum is them
	 * calculated by doing an interpolation over calculated values.
	 */
	public SpotRadiusEstimator( final ImgPlus< T > img, final Iterator< Spot > spots )
	{
		super( img, spots );
	}

	@Override
	public final void process( final Spot spot )
	{

		// Get diameter array and radius squared
		final double radius = spot.getFeature( Spot.RADIUS );
		final double[] diameters = prepareDiameters( radius * 2, nDiameters );
		final double[] r2 = new double[ nDiameters ];
		for ( int i = 0; i < r2.length; i++ )
		{
			r2[ i ] = diameters[ i ] * diameters[ i ] / 4;
		}

		// Calculate total intensity in balls
		final double[] ring_intensities = new double[ nDiameters ];
		final int[] ring_volumes = new int[ nDiameters ];

		// A tmp spot we will use to iterate around the real spot
		final Spot tmpSpot = new Spot( spot );
		tmpSpot.putFeature( Spot.RADIUS, diameters[ nDiameters - 1 ] / 2 );

		final SpotNeighborhood< T > neighborhood = new SpotNeighborhood<>( tmpSpot, img );
		if ( neighborhood.size() <= 1 )
		{
			spot.putFeature( ESTIMATED_DIAMETER, Double.valueOf( radius ) );
			return;
		}

		final SpotNeighborhoodCursor< T > cursor = neighborhood.cursor();
		double d2, val;
		int i;
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			d2 = cursor.getDistanceSquared();
			val = cursor.get().getRealDouble();
			for ( i = 0; i < nDiameters && d2 > r2[ i ]; i++ )
			{
				ring_intensities[ i ] += val;
				ring_volumes[ i ]++;
			}
		}

		// Calculate mean intensities from ring volumes
		final double[] mean_intensities = new double[ diameters.length ];
		for ( int j = 0; j < mean_intensities.length; j++ )
			mean_intensities[ j ] = ring_intensities[ j ] / ring_volumes[ j ];

		// Calculate contrasts as minus difference between outer and inner rings
		// mean intensity
		final double[] contrasts = new double[ diameters.length - 1 ];
		for ( int j = 0; j < contrasts.length - 1; j++ )
		{
			contrasts[ j + 1 ] = -( mean_intensities[ j + 1 ] - mean_intensities[ j ] );
		}

		// Find max contrast
		double maxConstrast = Float.NEGATIVE_INFINITY;
		int maxIndex = 0;
		for ( int j = 0; j < contrasts.length; j++ )
		{
			if ( contrasts[ j ] > maxConstrast )
			{
				maxConstrast = contrasts[ j ];
				maxIndex = j;
			}
		}

		double bestDiameter;
		if ( 1 >= maxIndex || contrasts.length - 1 == maxIndex )
		{
			bestDiameter = diameters[ maxIndex ];
		}
		else
		{
			bestDiameter = quadratic1DInterpolation( diameters[ maxIndex - 1 ], contrasts[ maxIndex - 1 ], diameters[ maxIndex ], contrasts[ maxIndex ], diameters[ maxIndex + 1 ], contrasts[ maxIndex + 1 ] );
		}
		spot.putFeature( ESTIMATED_DIAMETER, bestDiameter );
	}

	private static final double quadratic1DInterpolation( final double x1, final double y1, final double x2, final double y2, final double x3, final double y3 )
	{
		final double d2 = 2 * ( ( y3 - y2 ) / ( x3 - x2 ) - ( y2 - y1 ) / ( x2 - x1 ) ) / ( x3 - x1 );
		if ( d2 == 0 )
			return x2;

		final double d1 = ( y3 - y2 ) / ( x3 - x2 ) - d2 / 2 * ( x3 - x2 );
		return x2 - d1 / d2;
	}

	private static final double[] prepareDiameters( final double centralDiameter, final int nDiameters )
	{
		final double[] diameters = new double[ nDiameters ];
		for ( int i = 0; i < diameters.length; i++ )
		{
			diameters[ i ] = centralDiameter * ( MIN_DIAMETER_RATIO + i * ( MAX_DIAMETER_RATIO - MIN_DIAMETER_RATIO ) / ( nDiameters - 1 ) );
		}
		return diameters;
	}
}
