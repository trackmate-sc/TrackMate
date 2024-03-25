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
package fiji.plugin.trackmate.visualization;

import java.awt.Color;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.manual.ManualSpotColorAnalyzerFactory;

public class ManualSpotColorGenerator implements FeatureColorGenerator< Spot >
{

	private final Color missingValueColor;

	public ManualSpotColorGenerator( final Color missingValueColor )
	{
		this.missingValueColor = missingValueColor;
	}

	@Override
	public Color color( final Spot spot )
	{
		final Double val = spot.getFeature( ManualSpotColorAnalyzerFactory.FEATURE );
		if ( null == val )
			return missingValueColor;

		return new Color( val.intValue() );
	}
}
