package fiji.plugin.trackmate.visualization;


import java.awt.Color;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.features.manual.ManualEdgeColorAnalyzer;
import fiji.plugin.trackmate.gui.panels.components.ColorByFeatureGUIPanel;

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
		if ( null == val ) { return ManualEdgeColorAnalyzer.DEFAULT_COLOR; }
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
}
