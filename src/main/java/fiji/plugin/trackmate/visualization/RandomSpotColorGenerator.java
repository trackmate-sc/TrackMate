package fiji.plugin.trackmate.visualization;

import java.awt.Color;

import fiji.plugin.trackmate.Spot;

public class RandomSpotColorGenerator implements FeatureColorGenerator< Spot >
{

	@Override
	public Color color( final Spot spot )
	{
		final int i = spot.ID() % GlasbeyLut.colors.length;
		return GlasbeyLut.colors[ i ];
	}
}
