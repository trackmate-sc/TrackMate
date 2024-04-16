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

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;

/**
 * Color an edge by the manual color of its target spot.
 */
public class ManualEdgePerSpotColorGenerator implements TrackColorGenerator
{
	private final Model model;

	private final ManualSpotColorGenerator manualSpotColorGenerator;

	public ManualEdgePerSpotColorGenerator( final Model model, final Color missingValueColor )
	{
		this.model = model;
		manualSpotColorGenerator = new ManualSpotColorGenerator( missingValueColor );
	}

	@Override
	public Color color( final DefaultWeightedEdge edge )
	{
		final Spot spot = model.getTrackModel().getEdgeTarget( edge );
		return manualSpotColorGenerator.color( spot );
	}
}
