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

	private final FeatureColorGenerator< Integer > trackColorGenerator;

	public TrackCollectionDataset(
			final List< Integer > trackIDs,
			final String xFeature,
			final List< String > yFeatures,
			final Model model,
			final SelectionModel selectionModel,
			final DisplaySettings ds )
	{
		super( model, selectionModel, ds, xFeature, yFeatures );
		this.trackIDs = trackIDs;
		this.trackColorGenerator = FeatureUtils.createWholeTrackColorGenerator( model, ds );
	}

	@Override
	public int getItemCount( final int series )
	{
		return trackIDs.size();
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

		@Override
		public Paint getItemPaint( final int series, final int item )
		{
			final Integer trackID = trackIDs.get( item );
			if ( selectionModel != null && selectionModel.getSpotSelection().containsAll( model.getTrackModel().trackSpots( trackID ) ) )
				return ds.getHighlightColor();
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
