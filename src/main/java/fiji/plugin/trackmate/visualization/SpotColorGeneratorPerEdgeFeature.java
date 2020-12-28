package fiji.plugin.trackmate.visualization;

import java.awt.Color;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.displaysettings.Colormap;

public class SpotColorGeneratorPerEdgeFeature implements FeatureColorGenerator< Spot >
{

	private final Model model;

	private final Color missingValueColor;

	private final PerEdgeFeatureColorGenerator colorGenerator;

	public SpotColorGeneratorPerEdgeFeature(
			final Model model,
			final String edgeFeature,
			final Color missingValueColor,
			final Color undefinedValueColor,
			final Colormap colormap,
			final double min,
			final double max )
	{
		this.model = model;
		this.missingValueColor = missingValueColor;
		this.colorGenerator = new PerEdgeFeatureColorGenerator( model, edgeFeature, missingValueColor, undefinedValueColor, colormap, min, max );
	}

	@Override
	public Color color( final Spot spot )
	{
		final Set< DefaultWeightedEdge > edges = model.getTrackModel().edgesOf( spot );
		DefaultWeightedEdge edge = null;
		for ( final DefaultWeightedEdge e : edges )
		{
			if ( model.getTrackModel().getEdgeTarget( e ).equals( spot ) )
			{
				edge = e;
				break;
			}
		}
		if ( edge == null )
			return missingValueColor;

		return colorGenerator.color( edge );
	}
}
