/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2023 TrackMate developers.
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
package fiji.plugin.trackmate.tracking.jaqaman.costfunction;

import java.util.Map;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotRoi;
import math.geom2d.conic.Circle2D;
import math.geom2d.polygon.Polygons2D;
import math.geom2d.polygon.SimplePolygon2D;

/**
 * A cost function that tempers an overlap cost by difference in feature
 * values.
 * <p>
 * This cost is calculated as follow:
 * <ul>
 * <li>The overlap cost between the two spots <code>O</code> is calculated
 * <li>For each feature in the map, a penalty <code>p</code> is calculated as
 * <code>p = 3 × α × |f1-f2| / (f1+f2)</code>, where <code>α</code> is the
 * factor associated to the feature in the map. This expression is such that:
 * <ul>
 * <li>there is no penalty if the 2 feature values <code>f1</code> and
 * <code>f2</code> are the same;
 * <li>that, with a factor of 1, the penalty if 1 is one value is the double of
 * the other;
 * <li>the penalty is 2 if one is 5 times the other one.
 * </ul>
 * <li>All penalties are summed, to form <code>P = (1 + ∑ p )</code>
 * <li>The cost is set to the square of the product: <code>C = ( O × P )²</code>
 * </ul>
 *
 *
 */
public class OverlapFeaturePenaltyCostFunction implements CostFunction< Spot, Spot >
{

	private final Map< String, Double > featurePenalties;

	public OverlapFeaturePenaltyCostFunction( final Map< String, Double > featurePenalties )
	{
		this.featurePenalties = featurePenalties;
	}

	private static SimplePolygon2D toPolygon( final Spot spot)
	{
		final double xc = spot.getDoublePosition( 0 );
		final double yc = spot.getDoublePosition( 1 );
		final SpotRoi roi = spot.getRoi();
		final SimplePolygon2D poly;
		if ( roi == null )
		{
			final double radius = spot.getFeature( Spot.RADIUS ).doubleValue();
			poly = new SimplePolygon2D( new Circle2D( xc, yc, radius ).asPolyline( 32 ) );
		}
		else
		{
			final double[] xcoords = roi.toPolygonX( 1., 0., xc, 1. );
			final double[] ycoords = roi.toPolygonY( 1., 0., yc, 1. );
			poly = new SimplePolygon2D( xcoords, ycoords );
		}
		return poly;
	}
	
	@Override
	public double linkingCost( final Spot source, final Spot target )
	{
		final SimplePolygon2D targetPoly = toPolygon(target);
		final SimplePolygon2D sourcePoly = toPolygon(source);	
		final double intersection = Math.abs( Polygons2D.intersection( targetPoly, sourcePoly ).area() );
		final double union = Math.abs( sourcePoly.area() ) + Math.abs( targetPoly.area() ) - intersection;
		
		final double d1 = 1 - intersection / union;
		final double d2 = ( d1 == 0 ) ? Double.MIN_NORMAL : d1;

		double penalty = 1;
		for ( final String feature : featurePenalties.keySet() )
		{
			final double ndiff = source.normalizeDiffTo( target, feature );
			if ( Double.isNaN( ndiff ) )
			{
				continue;
			}
			final double factor = featurePenalties.get( feature );
			penalty += factor * 1.5 * ndiff;
		}

		return d2 * penalty * penalty;
	}
}
