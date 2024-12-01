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

import math.geom2d.polygon.Polygons2D;
import math.geom2d.polygon.SimplePolygon2D;
import math.geom2d.conic.Circle2D;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotRoi;

/**
 * A cost function that returns cost equal to 1 - (Intersection over Union). 
 **/
public class OverlapCostFunction implements CostFunction< Spot, Spot >
{
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
		
		return 1 - intersection / union;
		
	}

}
