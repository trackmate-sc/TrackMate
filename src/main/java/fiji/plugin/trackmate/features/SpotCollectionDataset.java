package fiji.plugin.trackmate.features;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Set;

import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.util.LineUtils;
import org.jfree.data.DomainOrder;
import org.jfree.data.general.AbstractDataset;
import org.jfree.data.xy.XYDataset;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;

public class SpotCollectionDataset extends AbstractDataset implements XYDataset
{

	private static final long serialVersionUID = 1L;

	private final List< Spot > spots;

	private final String datasetName;

	private final Model model;

	private final List< String > yFeatures;

	private final String xFeature;

	private final DisplaySettings ds;

	private final FeatureColorGenerator< Spot > spotColorGenerator;

	private final SelectionModel selectionModel;

	private final BasicStroke stroke;

	private final BasicStroke selectionStroke;

	public SpotCollectionDataset(
			final String datasetName,
			final List< Spot > spots,
			final String xFeature,
			final List< String > yFeatures,
			final Model model,
			final SelectionModel selectionModel,
			final DisplaySettings ds )
	{
		this.spots = spots;
		this.xFeature = xFeature;
		this.yFeatures = yFeatures;
		this.model = model;
		this.datasetName = datasetName;
		this.selectionModel = selectionModel;
		this.ds = ds;
		this.spotColorGenerator = FeatureUtils.createSpotColorGenerator( model, ds );
		this.stroke = new BasicStroke( ( float ) ds.getLineThickness(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND );
		this.selectionStroke = new BasicStroke( ( float ) ds.getSelectionLineThickness(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND );
	}

	@Override
	public int getSeriesCount()
	{
		return yFeatures.size();
	}

	@Override
	public String getSeriesKey( final int series )
	{
		if ( ( series < 0 ) || ( series >= getSeriesCount() ) )
			throw new IllegalArgumentException( "Series index out of bounds" );
		return model.getFeatureModel().getSpotFeatureShortNames().get( yFeatures.get( series ) );
	}

	@SuppressWarnings( "rawtypes" )
	@Override
	public int indexOf( final Comparable seriesKey )
	{
		for ( int i = 0; i < getSeriesCount(); i++ )
		{
			if ( getSeriesKey( i ).equals( seriesKey ) )
				return i;
		}
		return -1;
	}

	@Override
	public DomainOrder getDomainOrder()
	{
		return DomainOrder.NONE;
	}

	@Override
	public int getItemCount( final int series )
	{
		return spots.size();
	}

	@Override
	public Number getX( final int series, final int item )
	{
		return spots.get( item ).getFeatures().get( xFeature );
	}

	@Override
	public double getXValue( final int series, final int item )
	{
		return getX( series, item ).doubleValue();
	}

	@Override
	public Number getY( final int series, final int item )
	{
		return spots.get( item ).getFeatures().get( yFeatures.get( series ) );
	}

	@Override
	public double getYValue( final int series, final int item )
	{
		return getY( series, item ).doubleValue();
	}

	public XYItemRenderer getRenderer()
	{
		return new MyXYItemRenderer();
	}

	private final class MyXYItemRenderer extends XYLineAndShapeRenderer
	{

		private static final long serialVersionUID = 1L;

		@Override
		protected void drawPrimaryLine(
				final XYItemRendererState state,
				final Graphics2D g2,
				final XYPlot plot,
				final XYDataset dataset,
				final int pass,
				final int series,
				final int item,
				final ValueAxis domainAxis,
				final ValueAxis rangeAxis,
				final Rectangle2D dataArea )
		{

			final String yFeature = yFeatures.get( series );
			final Spot sourceSpot = spots.get( item );
			final Set< DefaultWeightedEdge > edges = model.getTrackModel().edgesOf( sourceSpot );
			for ( final DefaultWeightedEdge edge : edges )
			{
				final Spot targetSpot = model.getTrackModel().getEdgeTarget( edge );

				final Double x1 = targetSpot.getFeature( xFeature );
				final Double y1 = targetSpot.getFeature( yFeature );
				if ( x1 == null || y1 == null || x1.isNaN() || y1.isNaN() )
					continue;

				final Double x0 = sourceSpot.getFeature( xFeature );
				final Double y0 = sourceSpot.getFeature( yFeature );
				if ( x0 == null || y0 == null || x0.isNaN() || y0.isNaN() )
					continue;

				final RectangleEdge xAxisLocation = plot.getDomainAxisEdge();
				final RectangleEdge yAxisLocation = plot.getRangeAxisEdge();

				final double transX0 = domainAxis.valueToJava2D( x0, dataArea, xAxisLocation );
				final double transY0 = rangeAxis.valueToJava2D( y0, dataArea, yAxisLocation );

				final double transX1 = domainAxis.valueToJava2D( x1, dataArea, xAxisLocation );
				final double transY1 = rangeAxis.valueToJava2D( y1, dataArea, yAxisLocation );

				if ( Double.isNaN( transX0 ) || Double.isNaN( transY0 ) || Double.isNaN( transX1 ) || Double.isNaN( transY1 ) )
					continue;

				final PlotOrientation orientation = plot.getOrientation();
				boolean visible;
				if ( orientation == PlotOrientation.HORIZONTAL )
					state.workingLine.setLine( transY0, transX0, transY1, transX1 );
				else if ( orientation == PlotOrientation.VERTICAL )
					state.workingLine.setLine( transX0, transY0, transX1, transY1 );
				visible = LineUtils.clipLine( state.workingLine, dataArea );
				if ( visible )
					drawFirstPassShape( g2, pass, series, item, state.workingLine );
			}
		}

		@Override
		public Paint getItemPaint( final int series, final int item )
		{
			final Spot spot = spots.get( item );
			if ( selectionModel != null && selectionModel.getSpotSelection().contains( spot ) )
				return ds.getHighlightColor();
			return spotColorGenerator.color( spots.get( item ) );
		}

		@Override
		public Stroke getItemStroke( final int series, final int item )
		{
			final Spot spot = spots.get( item );
			if ( selectionModel != null && selectionModel.getSpotSelection().contains( spot ) )
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
