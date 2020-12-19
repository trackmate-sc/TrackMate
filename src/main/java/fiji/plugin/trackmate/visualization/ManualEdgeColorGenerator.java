package fiji.plugin.trackmate.visualization;


import java.awt.Color;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.features.manual.ManualEdgeColorAnalyzer;

public class ManualEdgeColorGenerator implements TrackColorGenerator
{
	private final Model model;

	private final Color missingValueColor;

	public ManualEdgeColorGenerator( final Model model, final Color missingValueColor )
	{
		this.model = model;
		this.missingValueColor = missingValueColor;
	}

	@Override
	public Color color( final DefaultWeightedEdge  edge)
	{
		final Double val = model.getFeatureModel().getEdgeFeature( edge, ManualEdgeColorAnalyzer.FEATURE );
		if ( null == val )
			return missingValueColor;
		return new Color( val.intValue() );
	}
}
