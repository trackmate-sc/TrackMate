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

import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.util.List;

import org.jfree.chart.LegendItem;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;

public class TrackCollectionDataset extends ModelDataset
{

	private static final long serialVersionUID = 1L;

	private final List< Integer > trackIDs;

	public TrackCollectionDataset(
			final Model model,
			final SelectionModel selectionModel,
			final DisplaySettings ds,
			final String xFeature,
			final List< String > yFeatures,
			final List< Integer > trackIDs )
	{
		super( model, selectionModel, ds, xFeature, yFeatures );
		this.trackIDs = trackIDs;
	}

	@Override
	public int getItemCount( final int series )
	{
		return trackIDs.size();
	}

	@Override
	public String getItemLabel( final int item )
	{
		return model.getTrackModel().name( trackIDs.get( item ) );
	}

	@Override
	public void setItemLabel( final int item, final String label )
	{
		model.getTrackModel().setName( trackIDs.get( item ), label );
	}

	@Override
	public String getSeriesKey( final int series )
	{
		if ( ( series < 0 ) || ( series >= getSeriesCount() ) )
			throw new IllegalArgumentException( "Series index out of bounds" );
		return model.getFeatureModel().getTrackFeatureShortNames().get( yFeatures.get( series ) );
	}

	@Override
	public Number getX( final int series, final int item )
	{
		return model.getFeatureModel().getTrackFeature( trackIDs.get( item ), xFeature );
	}

	@Override
	public Number getY( final int series, final int item )
	{
		return model.getFeatureModel().getTrackFeature( trackIDs.get( item ), yFeatures.get( series ) );
	}

	@Override
	public XYItemRenderer getRenderer()
	{
		return new MyXYItemRenderer();
	}

	private final class MyXYItemRenderer extends XYLineAndShapeRenderer
	{

		private static final long serialVersionUID = 1L;

		public MyXYItemRenderer()
		{
			super( false, true );
		}

		@Override
		public Paint getItemPaint( final int series, final int item )
		{
			final Integer trackID = trackIDs.get( item );
			if ( selectionModel != null && selectionModel.getSpotSelection().containsAll( model.getTrackModel().trackSpots( trackID ) ) )
				return ds.getHighlightColor();

			final FeatureColorGenerator< Integer > trackColorGenerator = FeatureUtils.createWholeTrackColorGenerator( model, ds );
			return trackColorGenerator.color( trackIDs.get( item ) );
		}

		@Override
		public Stroke getItemStroke( final int series, final int item )
		{
			final Integer trackID = trackIDs.get( item );
			if ( selectionModel != null && selectionModel.getSpotSelection().containsAll( model.getTrackModel().trackSpots( trackID ) ) )
				return selectionStroke;
			return stroke;
		}

		@Override
		public LegendItem getLegendItem( final int datasetIndex, final int series )
		{
			final LegendItem legendItem = super.getLegendItem( datasetIndex, series );
			legendItem.setFillPaint( Color.BLACK );
			legendItem.setLinePaint( Color.BLACK );
			legendItem.setOutlinePaint( Color.BLACK );
			return legendItem;
		}
	}
}
