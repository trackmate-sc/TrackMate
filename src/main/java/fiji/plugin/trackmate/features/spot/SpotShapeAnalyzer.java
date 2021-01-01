package fiji.plugin.trackmate.features.spot;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotRoi;
import net.imglib2.type.numeric.RealType;

public class SpotShapeAnalyzer< T extends RealType< T > > extends AbstractSpotFeatureAnalyzer< T >
{

	private final boolean is2D;

	public SpotShapeAnalyzer( final boolean is2D )
	{
		this.is2D = is2D;
	}

	@Override
	public void process( final Spot spot )
	{
		final double area;
		final double convexArea;
		final double perimeter;

		if ( is2D )
		{
			final SpotRoi roi = spot.getRoi();
			if ( roi != null )
			{
				area = roi.area();
				perimeter = getLength( roi );
				final SpotRoi convexHull = ConvexHull.convexHull( roi );
				convexArea = convexHull.area();
			}
			else
			{
				final double radius = spot.getFeature( Spot.RADIUS );
				area = Math.PI * radius * radius;
				convexArea = area;
				perimeter = 2. * Math.PI * radius;
			}
		}
		else
		{
			final double radius = spot.getFeature( Spot.RADIUS );
			area = 4. * Math.PI * radius * radius;
			convexArea = area;
			perimeter = Double.NaN;
		}
		final double circularity = 4. * Math.PI * ( area / ( perimeter * perimeter ) );
		final double solidity = area / convexArea;

		spot.putFeature( SpotShapeAnalyzerFactory.AREA, area );
		spot.putFeature( SpotShapeAnalyzerFactory.PERIMETER, perimeter );
		spot.putFeature( SpotShapeAnalyzerFactory.CIRCULARITY, circularity );
		spot.putFeature( SpotShapeAnalyzerFactory.SOLIDITY, solidity );
	}

	private static final double getLength( final SpotRoi roi )
	{
		final double[] x = roi.x;
		final double[] y = roi.y;
		final int npoints = x.length;
		if ( npoints < 2 )
			return 0;

		double length = 0;
		for ( int i = 0; i < npoints - 1; i++ )
		{
			final double dx = x[ i + 1 ] - x[ i ];
			final double dy = y[ i + 1 ] - y[ i ];
			length += Math.sqrt( dx * dx + dy * dy );
		}

		final double dx0 = x[ 0 ] - x[ npoints - 1 ];
		final double dy0 = y[ 0 ] - y[ npoints - 1 ];
		length += Math.sqrt( dx0 * dx0 + dy0 * dy0 );

		return length;
	}
}
