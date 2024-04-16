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

import java.util.Map;

import fiji.plugin.trackmate.Spot;

/**
 * A cost function that tempers a square distance cost by difference in feature
 * values.
 * <p>
 * This cost is calculated as follow:
 * <ul>
 * <li>The distance between the two spots <code>D</code> is calculated
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
 * <li>The cost is set to the square of the product: <code>C = ( D × P )²</code>
 * </ul>
 * For instance: if 2 spots differ by twice the value in a feature which is in
 * the penalty map with a factor of 1, they will <i>look</i> as if they were
 * twice as far.
 *
 * @author Jean-Yves Tinevez - 2014
 *
 */
public class FeaturePenaltyCostFunction implements CostFunction< Spot, Spot >
{

	private final Map< String, Double > featurePenalties;

	public FeaturePenaltyCostFunction( final Map< String, Double > featurePenalties )
	{
		this.featurePenalties = featurePenalties;
	}

	@Override
	public double linkingCost( final Spot source, final Spot target )
	{
		final double d1 = source.squareDistanceTo( target );
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
