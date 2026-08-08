/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
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
package fiji.plugin.trackmate.features;

import java.util.List;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.gui.GuiModel;

public class EdgeFeatureGrapher extends AbstractFeatureGrapher
{

	private final List< DefaultWeightedEdge > edges;

	private final boolean addLines;

	private final GuiModel guiModel;

	public EdgeFeatureGrapher(
			final GuiModel guiModel,
			final List< DefaultWeightedEdge > edges,
			final String xFeature,
			final List< String > yFeatures,
			final boolean addLines )
	{
		super(
				xFeature,
				yFeatures,
				guiModel.getModel().getFeatureModel().getEdgeFeatureDimensions().get( xFeature ),
				guiModel.getModel().getFeatureModel().getEdgeFeatureDimensions(),
				guiModel.getModel().getFeatureModel().getEdgeFeatureNames(),
				guiModel.getModel().getSpaceUnits(),
				guiModel.getModel().getTimeUnits() );
		this.guiModel = guiModel;
		this.edges = edges;
		this.addLines = addLines;
	}

	@Override
	protected ModelDataset buildMainDataSet( final List< String > targetYFeatures )
	{
		return new EdgeCollectionDataset(
				guiModel.getModel(),
				guiModel.getSelectionModel(),
				guiModel.getDisplaySettings(),
				xFeature,
				targetYFeatures,
				edges,
				addLines );
	}
}
