package fiji.plugin.trackmate.visualization;

import java.awt.Color;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.displaysettings.Colormap;

public class PerSpotFeatureColorGenerator implements TrackColorGenerator
{

	private final Model model;

	private final String spotFeature;

	private final Color missingValueColor;

	private final Color undefinedValueColor;

	private final Colormap colormap;

	private final double min;

	private final double max;

	public PerSpotFeatureColorGenerator(
			final Model model,
			final String spotFeature,
			final Color missingValueColor,
			final Color undefinedValueColor,
			final Colormap colormap,
			final double min,
			final double max )
	{
		this.model = model;
		this.spotFeature = spotFeature;
		this.missingValueColor = missingValueColor;
		this.undefinedValueColor = undefinedValueColor;
		this.colormap = colormap;
		this.min = min;
		this.max = max;
	}

	@Override
	public Color color( final DefaultWeightedEdge edge )
	{
		final Spot spot = model.getTrackModel().getEdgeTarget( edge );
		final Double feat = spot.getFeature( spotFeature );

		if ( null == feat )
			return missingValueColor;

		if ( feat.isNaN() )
			return undefinedValueColor;

		final double val = feat.doubleValue();
		return colormap.getPaint( ( val - min ) / ( max - min ) );
	}
}
