package fiji.plugin.trackmate.visualization;

import java.awt.Color;

import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * A dummy track color generator that always return the default color.
 *
 * @author Jean-Yves Tinevez - 2013. Revised December 2020.
 */
public class UniformTrackColorGenerator implements TrackColorGenerator
{

	private final Color color;

	public UniformTrackColorGenerator( final Color color )
	{
		this.color = color;
	}

	@Override
	public Color color( final DefaultWeightedEdge obj )
	{
		return color;
	}
}
