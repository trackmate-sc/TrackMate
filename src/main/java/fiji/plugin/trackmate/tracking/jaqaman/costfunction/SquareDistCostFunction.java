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
package fiji.plugin.trackmate.tracking.jaqaman.costfunction;

import fiji.plugin.trackmate.Spot;

/**
 * A cost function that returns cost equal to the square distance. Suited to
 * Brownian motion.
 *
 * @author Jean-Yves Tinevez - 2014
 *
 */
public class SquareDistCostFunction implements CostFunction< Spot, Spot >
{

	@Override
	public double linkingCost( final Spot source, final Spot target )
	{
		final double d2 = source.squareDistanceTo( target );
		return ( d2 == 0 ) ? Double.MIN_NORMAL : d2;
	}

}
