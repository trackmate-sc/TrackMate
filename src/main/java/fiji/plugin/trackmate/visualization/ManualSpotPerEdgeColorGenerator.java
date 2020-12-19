package fiji.plugin.trackmate.visualization;

import java.awt.Color;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;

/**
 * Color a spot by the manual color of its first incoming edges.
 */
public class ManualSpotPerEdgeColorGenerator implements FeatureColorGenerator< Spot >
{

	private final Color missingValueColor;

	private final Model model;

	private final ManualEdgeColorGenerator manualEdgeColorGenerator;

	public ManualSpotPerEdgeColorGenerator( final Model model, final Color missingValueColor )
	{
		this.model = model;
		this.missingValueColor = missingValueColor;
		this.manualEdgeColorGenerator = new ManualEdgeColorGenerator( model, missingValueColor );
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
		
		return manualEdgeColorGenerator.color( edge );
	}
}
