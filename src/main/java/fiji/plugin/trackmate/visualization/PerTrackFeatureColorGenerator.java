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
/**
 *
 */
package fiji.plugin.trackmate.visualization;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.gui.displaysettings.Colormap;

/**
 * A {@link TrackColorGenerator} that generate colors based on the whole track
 * feature.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class PerTrackFeatureColorGenerator implements TrackColorGenerator
{

	private Map< Integer, Color > colorIndex;

	private final Model model;

	private final String feature;

	private final Colormap cmap;

	private final Color missingValueColor;

	public PerTrackFeatureColorGenerator(
			final Model model,
			final String trackFeature,
			final Color missingValueColor,
			final Color undefinedValueColor,
			final Colormap colormap,
			final double min,
			final double max )
	{
		this.model = model;
		this.feature = trackFeature;
		this.missingValueColor = missingValueColor;
		this.cmap = colormap;
		this.colorIndex = new HashMap<>();

		final TrackModel trackModel = model.getTrackModel();
		final Set< Integer > trackIDs = trackModel.trackIDs( true );

		if ( feature.equals( TrackIndexAnalyzer.TRACK_INDEX ) )
		{
			// Special case for track index.
			GlasbeyLut.reset();
			for ( final Integer trackID : trackIDs )
				colorIndex.put( trackID, GlasbeyLut.next() );
		}
		else
		{
			// Create value->color map
			final FeatureModel fm = model.getFeatureModel();
			colorIndex = new HashMap<>( trackIDs.size() );
			for ( final Integer trackID : trackIDs )
			{
				final Double val = fm.getTrackFeature( trackID, feature );
				final Color col;
				if ( null == val )
					col = missingValueColor;
				else if ( val.isNaN() )
					col = undefinedValueColor;
				else
					col = cmap.getPaint( ( val.doubleValue() - min ) / ( max - min ) );

				colorIndex.put( trackID, col );
			}
		}
	}

	public Color colorOf( final Integer trackID )
	{
		return colorIndex.get( trackID );
	}

	@Override
	public Color color( final DefaultWeightedEdge edge )
	{
		final Integer id = model.getTrackModel().trackIDOf( edge );
		if ( id == null )
			return missingValueColor;

		return colorIndex.get( id );
	}
}
