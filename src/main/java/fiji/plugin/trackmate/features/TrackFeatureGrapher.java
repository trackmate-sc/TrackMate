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
package fiji.plugin.trackmate.features;

import java.util.List;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

public class TrackFeatureGrapher extends AbstractFeatureGrapher
{

	private final List< Integer > trackIDs;

	private final Model model;

	private final SelectionModel selectionModel;

	private final DisplaySettings ds;

	public TrackFeatureGrapher(
			final List< Integer > trackIDs,
			final String xFeature,
			final List< String > yFeatures,
			final Model model,
			final SelectionModel selectionModel,
			final DisplaySettings displaySettings )
	{
		super(
				xFeature,
				yFeatures,
				model.getFeatureModel().getTrackFeatureDimensions().get( xFeature ),
				model.getFeatureModel().getTrackFeatureDimensions(),
				model.getFeatureModel().getTrackFeatureNames(),
				model.getSpaceUnits(),
				model.getTimeUnits() );
		this.trackIDs = trackIDs;
		this.model = model;
		this.selectionModel = selectionModel;
		this.ds = displaySettings;
	}

	@Override
	protected ModelDataset buildMainDataSet( final List< String > targetYFeatures )
	{
		return new TrackCollectionDataset(
				model,
				selectionModel,
				ds,
				xFeature,
				targetYFeatures,
				trackIDs );
	}
}
