package fiji.plugin.trackmate.visualization;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.manual.ManualSpotColorAnalyzerFactory;
import fiji.plugin.trackmate.gui.panels.components.ColorByFeatureGUIPanel;

import java.awt.Color;

public class ManualSpotColorGenerator implements FeatureColorGenerator< Spot >
{
	@Override
	public Color color( final Spot spot )
	{
		final Double val = spot.getFeature( ManualSpotColorAnalyzerFactory.FEATURE );
		if ( null == val ) { return TrackMateModelView.DEFAULT_UNASSIGNED_FEATURE_COLOR; }
		return new Color( val.intValue() );
	}

	@Override
	public void setFeature( final String feature )
	{}

	@Override
	public String getFeature()
	{
		return ColorByFeatureGUIPanel.MANUAL_KEY;
	}

	@Override
	public void terminate()
	{}

	@Override
	public void activate()
	{}

	@Override
	public double getMin()
	{
		return Double.NaN;
	}

	@Override
	public double getMax()
	{
		return Double.NaN;
	}

	@Override
	public void setMinMax( final double min, final double max )
	{}

	@Override
	public void autoMinMax()
	{}

	@Override
	public void setAutoMinMaxMode( final boolean autoMode )
	{}

	@Override
	public boolean isAutoMinMaxMode()
	{
		return false;
	}

	@Override
	public void setFrom( final MinMaxAdjustable minMaxAdjustable )
	{}
}

