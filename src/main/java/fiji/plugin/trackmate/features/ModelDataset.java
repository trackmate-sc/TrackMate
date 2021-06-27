package fiji.plugin.trackmate.features;

import java.awt.BasicStroke;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.DomainOrder;
import org.jfree.data.general.AbstractDataset;
import org.jfree.data.xy.XYDataset;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.features.ModelDataset.DataItem;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

public abstract class ModelDataset extends AbstractDataset implements XYDataset, Iterable< DataItem >
{

	private static final long serialVersionUID = 1L;

	protected final Model model;

	protected final List< String > yFeatures;

	protected final String xFeature;

	protected final DisplaySettings ds;

	protected final SelectionModel selectionModel;

	protected final BasicStroke stroke;

	protected final BasicStroke selectionStroke;

	private final Map< String, Integer > featureNameMap;

	public ModelDataset(
			final Model model,
			final SelectionModel selectionModel,
			final DisplaySettings ds,
			final String xFeature,
			final List< String > yFeatures )
	{
		this.model = model;
		this.selectionModel = selectionModel;
		this.xFeature = xFeature;
		this.yFeatures = yFeatures;
		this.ds = ds;
		this.stroke = new BasicStroke( ( float ) ds.getLineThickness(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND );
		this.selectionStroke = new BasicStroke( ( float ) ds.getSelectionLineThickness(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND );
		this.featureNameMap = new HashMap<>();
		for ( int i = 0; i < yFeatures.size(); i++ )
			featureNameMap.put( getSeriesKey( i ).toString(), Integer.valueOf( i ) );
	}

	@Override
	public int getSeriesCount()
	{
		return yFeatures.size();
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
	public double getXValue( final int series, final int item )
	{
		final Number val = getX( series, item );
		if ( val == null )
			return Double.NaN;
		return val.doubleValue();
	}

	@Override
	public double getYValue( final int series, final int item )
	{
		final Number val = getY( series, item );
		if ( val == null )
			return Double.NaN;
		return val.doubleValue();
	}

	public abstract String getItemLabel( int item );

	public abstract void setItemLabel( int item, String label );

	public abstract XYItemRenderer getRenderer();

	
	@Override
	public Iterator< DataItem > iterator()
	{
		return new Iterator< DataItem >()
		{

			int item = 0;

			@Override
			public boolean hasNext()
			{
				return item < getItemCount( 0 );
			}

			@Override
			public DataItem next()
			{
				return new DataItem( item++ );
			}
		};
	}
	
	public final class DataItem
	{

		public final int item;

		private DataItem( final int item )
		{
			this.item = item;
		}

		public Double get( final String feature )
		{
			if ( xFeature.equals( feature ) )
				return ( Double ) getX( 0, item );
			final Integer series = featureNameMap.get( feature );
			if ( series == null )
				return null;
			return ( Double ) getY( series.intValue(), item );
		}
	}
}
