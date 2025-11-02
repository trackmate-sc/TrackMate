/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.features.spot;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotRoi;
import net.imglib2.type.numeric.RealType;

public class Spot2DShapeAnalyzer< T extends RealType< T > > extends AbstractSpotFeatureAnalyzer< T >
{

	private final boolean is2D;

	public Spot2DShapeAnalyzer( final boolean is2D )
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
			if ( spot instanceof SpotRoi )
			{
				final SpotRoi roi = ( SpotRoi ) spot;
				area = roi.area();
				perimeter = getLength( roi );
				final SpotRoi convexHull = ConvexHull2D.convexHull( roi );
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
		final double shapeIndex = ( area <= 0. ) ? Double.NaN : perimeter / Math.sqrt( area );

		spot.putFeature( Spot2DShapeAnalyzerFactory.AREA, area );
		spot.putFeature( Spot2DShapeAnalyzerFactory.PERIMETER, perimeter );
		spot.putFeature( Spot2DShapeAnalyzerFactory.CIRCULARITY, circularity );
		spot.putFeature( Spot2DShapeAnalyzerFactory.SOLIDITY, solidity );
		spot.putFeature( Spot2DShapeAnalyzerFactory.SHAPE_INDEX, shapeIndex );
	}

	private static final double getLength( final SpotRoi roi )
	{
		final int nPoints = roi.nPoints();
		if ( nPoints < 2 )
			return 0;

		double length = 0;
		int i;
		int j;
		for ( i = 0, j = nPoints - 1; i < nPoints; j = i++ )
		{
			final double dx = roi.x( i ) - roi.x( j );
			final double dy = roi.y( i ) - roi.y( j );
			length += Math.sqrt( dx * dx + dy * dy );
		}
		return length;
	}
}
