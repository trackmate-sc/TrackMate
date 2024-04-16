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
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;

/**
 * Color a spot by the manual color of its first incoming edges.
 */
public class ManualSpotPerEdgeColorGenerator implements FeatureColorGenerator< Spot >
{

	private final Color missingValueColor;

	private final Model model;

	private final ManualEdgeColorGenerator manualEdgeColorGenerator;

	public ManualSpotPerEdgeColorGenerator( final Model model, final Color missingValueColor )
	{
		this.model = model;
		this.missingValueColor = missingValueColor;
		this.manualEdgeColorGenerator = new ManualEdgeColorGenerator( model, missingValueColor );
	}

	@Override
	public Color color( final Spot spot )
	{
		final Set< DefaultWeightedEdge > edges = model.getTrackModel().edgesOf( spot );
		DefaultWeightedEdge edge = null;
		for ( final DefaultWeightedEdge e : edges )
		{
			if ( model.getTrackModel().getEdgeTarget( e ).equals( spot ) )
			{
				edge = e;
				break;
			}
		}
		if ( edge == null )
			return missingValueColor;

		return manualEdgeColorGenerator.color( edge );
	}
}
