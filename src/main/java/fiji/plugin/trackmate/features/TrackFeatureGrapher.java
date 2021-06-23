/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2021 Fiji developers.
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

import java.util.Set;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.util.XYEdgeSeriesCollection;

public class TrackFeatureGrapher extends AbstractFeatureGrapher
{

	public TrackFeatureGrapher( final String xFeature, final Set< String > yFeatures, final Model model, final DisplaySettings displaySettings )
	{
		super(
				model,
				displaySettings,
				xFeature,
				yFeatures,
				model.getFeatureModel().getTrackFeatureDimensions().get( xFeature ),
				model.getFeatureModel().getTrackFeatureDimensions(),
				model.getFeatureModel().getTrackFeatureNames() );
	}


	/**
	 * @return a new dataset that contains the values, specified from the given
	 *         feature, and extracted from all the visible tracks in the model.
	 */
	@Override
	protected XYSeriesCollection buildMainDataSet( final Iterable< String > targetYFeatures )
	{
		final XYSeriesCollection dataset = new XYSeriesCollection();
		final FeatureModel fm = model.getFeatureModel();
		for ( final String feature : targetYFeatures )
		{
			final XYSeries series = new XYSeries( featureNames.get( feature ) );
			for ( final Integer trackID : model.getTrackModel().trackIDs( true ) )
			{
				final Double x = fm.getTrackFeature( trackID, xFeature );
				final Double y = fm.getTrackFeature( trackID, feature );
				if ( null == x || null == y )
					continue;

				series.add( x.doubleValue(), y.doubleValue() );
			}
			dataset.addSeries( series );
		}
		return dataset;
	}

	@Override
	protected XYEdgeSeriesCollection buildConnectionDataSet( final Iterable< String > targetYFeatures )
	{
		return null;
	}
}
