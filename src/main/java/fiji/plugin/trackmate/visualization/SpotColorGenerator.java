package fiji.plugin.trackmate.visualization;

import java.awt.Color;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.displaysettings.Colormap;

public class SpotColorGenerator implements FeatureColorGenerator< Spot >
{

	private final String feature;

	private final Color missingValueColor;

	private final Color undefinedValueColor;

	private final Colormap colormap;

	private final double min;

	private final double max;

	public SpotColorGenerator(
			final String feature,
			final Color missingValueColor,
			final Color undefinedValueColor,
			final Colormap colormap,
			final double min,
			final double max )
	{
		this.feature = feature;
		this.missingValueColor = missingValueColor;
		this.undefinedValueColor = undefinedValueColor;
		this.colormap = colormap;
		this.min = min;
		this.max = max;
	}

	@Override
	public Color color( final Spot spot )
	{
		final Double feat = spot.getFeature( feature );
		if ( null == feat )
			return missingValueColor;
		if ( feat.isNaN() )
			return undefinedValueColor;

		return colormap.getPaint( ( feat.doubleValue() - min ) / ( max - min ) );
	}
}
