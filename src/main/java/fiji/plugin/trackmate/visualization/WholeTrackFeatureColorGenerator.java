package fiji.plugin.trackmate.visualization;

import java.awt.Color;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.displaysettings.Colormap;

public class WholeTrackFeatureColorGenerator implements FeatureColorGenerator< Integer >
{

	private final Model model;

	private final String trackFeature;

	private final Color missingValueColor;

	private final Color undefinedValueColor;

	private final Colormap colormap;

	private final double min;

	private final double max;

	public WholeTrackFeatureColorGenerator(
			final Model model,
			final String trackFeature,
			final Color missingValueColor,
			final Color undefinedValueColor,
			final Colormap colormap,
			final double min,
			final double max )
	{
		this.model = model;
		this.trackFeature = trackFeature;
		this.missingValueColor = missingValueColor;
		this.undefinedValueColor = undefinedValueColor;
		this.colormap = colormap;
		this.min = min;
		this.max = max;
	}

	@Override
	public Color color( final Integer trackID )
	{
		final Double feat = model.getFeatureModel().getTrackFeature( trackID, trackFeature );
		if ( null == feat )
			return missingValueColor;

		if ( feat.isNaN() )
			return undefinedValueColor;

		final double val = feat.doubleValue();
		return colormap.getPaint( ( val - min ) / ( max - min ) );
	}

}
