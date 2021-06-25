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

import java.util.List;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.util.XYEdgeSeries;
import fiji.plugin.trackmate.util.XYEdgeSeriesCollection;

public class EdgeFeatureGrapher extends AbstractFeatureGrapher
{

	private final List< DefaultWeightedEdge > edges;

	public EdgeFeatureGrapher(
			final String xFeature,
			final List< String > yFeatures,
			final List< DefaultWeightedEdge > edges,
			final Model model,
			final DisplaySettings displaySettings )
	{
		super(
				model,
				displaySettings,
				xFeature,
				yFeatures,
				model.getFeatureModel().getEdgeFeatureDimensions().get( xFeature ),
				model.getFeatureModel().getEdgeFeatureDimensions(),
				model.getFeatureModel().getEdgeFeatureNames() );
		this.edges = edges;
	}

	/**
	 * Returns a new dataset that contains the values, specified from the given
	 * feature, and extracted from all the given edges.
	 * 
	 * @return a new dataset.
	 */
	@Override
	protected XYSeriesCollection buildMainDataSet( final Iterable< String > targetYFeatures )
	{
		final XYSeriesCollection dataset = new XYSeriesCollection();
		final FeatureModel fm = model.getFeatureModel();
		for ( final String feature : targetYFeatures )
		{
			final XYSeries series = new XYSeries( featureNames.get( feature ) );
			for ( final DefaultWeightedEdge edge : edges )
			{
				final Number x = fm.getEdgeFeature( edge, xFeature );
				final Number y = fm.getEdgeFeature( edge, feature );
				if ( null == x || null == y )
				{
					continue;
				}
				series.add( x.doubleValue(), y.doubleValue() );
			}
			dataset.addSeries( series );
		}
		return dataset;
	}

	@Override
	protected XYEdgeSeriesCollection buildConnectionDataSet( final Iterable< String > targetYFeatures )
	{
		final XYEdgeSeriesCollection edgeDataset = new XYEdgeSeriesCollection();
		// First create series per y features. At this stage, we assume that
		// they are all numeric
		for ( final String yFeature : targetYFeatures )
		{
			final XYEdgeSeries edgeSeries = new XYEdgeSeries( featureNames.get( yFeature ) );
			edgeDataset.addSeries( edgeSeries );
		}

		// Build dataset. We look for edges that have a spot in common, one for
		// the target one for the source
		final FeatureModel fm = model.getFeatureModel();
		for ( final DefaultWeightedEdge edge0 : edges )
		{
			for ( final DefaultWeightedEdge edge1 : edges )
			{

				if ( model.getTrackModel().getEdgeSource( edge0 ).equals( model.getTrackModel().getEdgeTarget( edge1 ) ) )
				{
					for ( final String yFeature : targetYFeatures )
					{
						final XYEdgeSeries edgeSeries = edgeDataset.getSeries( featureNames.get( yFeature ) );
						final Number x0 = fm.getEdgeFeature( edge0, xFeature );
						final Number y0 = fm.getEdgeFeature( edge0, yFeature );
						final Number x1 = fm.getEdgeFeature( edge1, xFeature );
						final Number y1 = fm.getEdgeFeature( edge1, yFeature );

						// Some feature values might be null.
						if ( null == x0 || null == y0 || null == x1 || null == y1 )
							continue;

						edgeSeries.addEdge( x0.doubleValue(), y0.doubleValue(), x1.doubleValue(), y1.doubleValue() );
					}
				}
			}
		}
		return edgeDataset;
	}
}
