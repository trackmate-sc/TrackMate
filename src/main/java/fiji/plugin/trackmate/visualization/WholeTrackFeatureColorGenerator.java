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
import fiji.plugin.trackmate.gui.displaysettings.Colormap;

public class WholeTrackFeatureColorGenerator implements FeatureColorGenerator< Integer >
{

	private final Model model;

	private final String trackFeature;

	private final Color missingValueColor;

	private final Color undefinedValueColor;

	private final Colormap colormap;

	private final double min;

	private final double max;

	public WholeTrackFeatureColorGenerator(
			final Model model,
			final String trackFeature,
			final Color missingValueColor,
			final Color undefinedValueColor,
			final Colormap colormap,
			final double min,
			final double max )
	{
		this.model = model;
		this.trackFeature = trackFeature;
		this.missingValueColor = missingValueColor;
		this.undefinedValueColor = undefinedValueColor;
		this.colormap = colormap;
		this.min = min;
		this.max = max;
	}

	@Override
	public Color color( final Integer trackID )
	{
		final Double feat = model.getFeatureModel().getTrackFeature( trackID, trackFeature );
		if ( null == feat )
			return missingValueColor;

		if ( feat.isNaN() )
			return undefinedValueColor;

		final double val = feat.doubleValue();
		return colormap.getPaint( ( val - min ) / ( max - min ) );
	}

}
