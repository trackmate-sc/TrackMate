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

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.GuiModel;

public class SpotFeatureGrapher extends AbstractFeatureGrapher
{

	private final List< Spot > spots;

	private final boolean addLines;

	private final GuiModel guiModel;

	public SpotFeatureGrapher(
			final GuiModel guiModel,
			final List< Spot > spots,
			final String xFeature,
			final List< String > yFeatures,
			final boolean addLines )
	{
		super(
				xFeature,
				yFeatures,
				guiModel.getModel().getFeatureModel().getSpotFeatureDimensions().get( xFeature ),
				guiModel.getModel().getFeatureModel().getSpotFeatureDimensions(),
				guiModel.getModel().getFeatureModel().getSpotFeatureNames(),
				guiModel.getModel().getSpaceUnits(),
				guiModel.getModel().getTimeUnits() );
		this.guiModel = guiModel;
		this.spots = spots;
		this.addLines = addLines;
	}

	@Override
	protected ModelDataset buildMainDataSet( final List< String > targetYFeatures )
	{
		return new SpotCollectionDataset(
				guiModel.getModel(),
				guiModel.getSelectionModel(),
				guiModel.getDisplaySettings(),
				xFeature,
				targetYFeatures,
				spots,
				addLines );
	}
}
