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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.util.XYEdgeSeries;
import fiji.plugin.trackmate.util.XYEdgeSeriesCollection;

public class SpotFeatureGrapher extends AbstractFeatureGrapher
{

	private final Collection< Spot > spots;

	public SpotFeatureGrapher( final String xFeature, final Set< String > yFeatures, final Collection< Spot > spots, final Model model, final DisplaySettings displaySettings )
	{
		super(
				model,
				displaySettings,
				xFeature,
				yFeatures,
				model.getFeatureModel().getSpotFeatureDimensions().get( xFeature ),
				model.getFeatureModel().getSpotFeatureDimensions(),
				model.getFeatureModel().getSpotFeatureNames() );
		this.spots = spots;
	}

	/**
	 * Returns a new dataset that contains the values, specified from the given
	 * feature, and extracted from the spots given in constructor.
	 * 
	 * @return a new dataset.
	 */
	@Override
	protected XYSeriesCollection buildMainDataSet( final Iterable< String > targetYFeatures )
	{
		final XYSeriesCollection dataset = new XYSeriesCollection();
		for ( final String feature : targetYFeatures )
		{
			final XYSeries series = new XYSeries( featureNames.get( feature ) );
			for ( final Spot spot : spots )
			{
				final Double x = spot.getFeature( xFeature );
				final Double y = spot.getFeature( feature );
				if ( null == x || null == y )
					continue;

				series.add( x.doubleValue(), y.doubleValue() );
			}
			dataset.addSeries( series );
		}
		return dataset;
	}

	/**
	 * Returns a new dataset that contains the values, specified from the given
	 * feature, and extracted from all the given spots. The dataset returned is
	 * a {@link XYEdgeSeriesCollection}, made to plot the lines between 2 points
	 * representing 2 spots.
	 * 
	 * @return a new dataset.
	 */
	@Override
	protected XYEdgeSeriesCollection buildConnectionDataSet( final Iterable< String > targetYFeatures )
	{
		// Collect edges
		final List< DefaultWeightedEdge > edges = getInsideEdges( spots );

		// Build dataset
		final XYEdgeSeriesCollection edgeDataset = new XYEdgeSeriesCollection();
		Double x0, x1, y0, y1;
		XYEdgeSeries edgeSeries;
		Spot source, target;
		for ( final String yFeature : targetYFeatures )
		{
			edgeSeries = new XYEdgeSeries( featureNames.get( yFeature ) );
			for ( final DefaultWeightedEdge edge : edges )
			{
				source = model.getTrackModel().getEdgeSource( edge );
				target = model.getTrackModel().getEdgeTarget( edge );
				x0 = source.getFeature( xFeature );
				y0 = source.getFeature( yFeature );
				x1 = target.getFeature( xFeature );
				y1 = target.getFeature( yFeature );
				if ( null == x0 || null == y0 || null == x1 || null == y1 )
				{
					continue;
				}
				edgeSeries.addEdge( x0.doubleValue(), y0.doubleValue(), x1.doubleValue(), y1.doubleValue() );
			}
			edgeDataset.addSeries( edgeSeries );
		}
		return edgeDataset;
	}

	/**
	 * Returns the list of links that have their source and target in the given
	 * spot list.
	 * 
	 * @return a new list.
	 */
	private final List< DefaultWeightedEdge > getInsideEdges( final Collection< Spot > spots )
	{
		final int nspots = spots.size();
		final ArrayList< DefaultWeightedEdge > edges = new ArrayList<>( nspots );
		final TrackModel trackModel = model.getTrackModel();
		for ( final DefaultWeightedEdge edge : trackModel.edgeSet() )
		{
			final Spot source = trackModel.getEdgeSource( edge );
			final Spot target = trackModel.getEdgeTarget( edge );
			if ( spots.contains( source ) && spots.contains( target ) )
				edges.add( edge );

		}
		return edges;
	}
}
