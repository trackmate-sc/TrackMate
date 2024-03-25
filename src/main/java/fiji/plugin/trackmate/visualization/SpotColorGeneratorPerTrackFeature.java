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

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.displaysettings.Colormap;

public class SpotColorGeneratorPerTrackFeature implements FeatureColorGenerator< Spot >
{

	private final Model model;

	private final Color missingValueColor;

	private final PerTrackFeatureColorGenerator colorGenerator;

	public SpotColorGeneratorPerTrackFeature(
			final Model model,
			final String trackFeature,
			final Color missingValueColor,
			final Color undefinedValueColor,
			final Colormap colormap,
			final double min,
			final double max )
	{
		this.model = model;
		this.missingValueColor = missingValueColor;
		this.colorGenerator = new PerTrackFeatureColorGenerator( model, trackFeature, missingValueColor, undefinedValueColor, colormap, min, max );
	}

	@Override
	public Color color( final Spot spot )
	{
		final Integer trackID = model.getTrackModel().trackIDOf( spot );
		if ( null == trackID || !model.getTrackModel().isVisible( trackID ) )
			return missingValueColor;

		return colorGenerator.colorOf( trackID );
	}
}
