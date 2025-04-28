package fiji.plugin.trackmate.detection.onestep;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;

/**
 * Base class for the output of a {@link SpotDetectorTracker}.
 * <p>
 * Because we need to return a {@link SpotCollection} and a
 * {@link SimpleWeightedGraph}, this class encapsulate both of them with
 * convenience methods.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class SpotDetectorTrackerOutput
{

	private final SpotCollection spots;

	private final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph;

	public SpotDetectorTrackerOutput( final SpotCollection spots, final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph )
	{
		this.spots = spots;
		this.graph = graph;
	}

	public SpotCollection getSpots()
	{
		return spots;
	}

	public SimpleWeightedGraph< Spot, DefaultWeightedEdge > getGraph()
	{
		return graph;
	}

}
