package fiji.plugin.trackmate.visualization;


import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.features.manual.ManualEdgeColorAnalyzer;
import fiji.plugin.trackmate.gui.panels.components.ColorByFeatureGUIPanel;

import java.awt.Color;

import org.jgrapht.graph.DefaultWeightedEdge;

public class ManualEdgeColorGenerator implements TrackColorGenerator
{
	private final Model model;

	public ManualEdgeColorGenerator( final Model model )
	{
		this.model = model;
	}

	@Override
	public Color color( final DefaultWeightedEdge  edge)
	{
		final Double val = model.getFeatureModel().getEdgeFeature( edge, ManualEdgeColorAnalyzer.FEATURE );
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
	public void setCurrentTrackID( final Integer trackID )
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
