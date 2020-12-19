package fiji.plugin.trackmate.visualization;

import java.awt.Color;

import org.jfree.chart.renderer.InterpolatePaintScale;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;

public class PerEdgeFeatureColorGenerator implements TrackColorGenerator
{

	private final Model model;

	private final String edgeFeature;

	private final Color missingValueColor;

	private final Color undefinedValueColor;

	private final InterpolatePaintScale colormap;

	private final double min;

	private final double max;

	public PerEdgeFeatureColorGenerator(
			final Model model,
			final String edgeFeature,
			final Color missingValueColor,
			final Color undefinedValueColor,
			final InterpolatePaintScale colormap,
			final double min,
			final double max )
	{
		this.model = model;
		this.edgeFeature = edgeFeature;
		this.missingValueColor = missingValueColor;
		this.undefinedValueColor = undefinedValueColor;
		this.colormap = colormap;
		this.min = min;
		this.max = max;
	}

	@Override
	public Color color( final DefaultWeightedEdge edge )
	{
		final Double feat = model.getFeatureModel().getEdgeFeature( edge, edgeFeature );
		if ( null == feat )
			return missingValueColor;

		if ( feat.isNaN() )
			return undefinedValueColor;

		final double val = feat.doubleValue();
		return colormap.getPaint( ( val - min ) / ( max - min ) );
	}
}
