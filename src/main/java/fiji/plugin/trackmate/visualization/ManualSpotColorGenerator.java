package fiji.plugin.trackmate.visualization;

import java.awt.Color;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.manual.ManualSpotColorAnalyzerFactory;

public class ManualSpotColorGenerator implements FeatureColorGenerator< Spot >
{

	private final Color missingValueColor;

	public ManualSpotColorGenerator( final Color missingValueColor )
	{
		this.missingValueColor = missingValueColor;
	}

	@Override
	public Color color( final Spot spot )
	{
		final Double val = spot.getFeature( ManualSpotColorAnalyzerFactory.FEATURE );
		if ( null == val )
			return missingValueColor;

		return new Color( val.intValue() );
	}
}
