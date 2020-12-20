package fiji.plugin.trackmate.visualization;

import java.awt.Color;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.displaysettings.Colormap;

public class SpotColorGeneratorPerTrackFeature implements FeatureColorGenerator< Spot >
{

	private final Model model;

	private final Color missingValueColor;

	private final PerTrackFeatureColorGenerator colorGenerator;

	public SpotColorGeneratorPerTrackFeature(
			final Model model,
			final String trackFeature,
			final Color missingValueColor,
			final Color undefinedValueColor,
			final Colormap colormap,
			final double min,
			final double max )
	{
		this.model = model;
		this.missingValueColor = missingValueColor;
		this.colorGenerator = new PerTrackFeatureColorGenerator( model, trackFeature, missingValueColor, undefinedValueColor, colormap, min, max );
	}

	@Override
	public Color color( final Spot spot )
	{
		final Integer trackID = model.getTrackModel().trackIDOf( spot );
		if ( null == trackID || !model.getTrackModel().isVisible( trackID ) )
			return missingValueColor;

		return colorGenerator.colorOf( trackID );
	}
}
