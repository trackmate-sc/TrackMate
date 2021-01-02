package fiji.plugin.trackmate.features.edges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;

@Plugin( type = EdgeAnalyzer.class )
public class DirectionalChangeAnalyzer extends AbstractEdgeAnalyzer
{

	public static final String KEY = "Directional change";

	public static final String DIRECTIONAL_CHANGE_RATE = "DIRECTIONAL_CHANGE_RATE";
	public static final List< String > FEATURES = new ArrayList<>( 1 );
	public static final Map< String, String > FEATURE_NAMES = new HashMap<>( FEATURES.size() );
	public static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap<>( FEATURES.size() );
	public static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap<>( FEATURES.size() );
	public static final Map< String, Boolean > IS_INT = new HashMap<>( FEATURES.size() );

	static
	{
		FEATURES.add( DIRECTIONAL_CHANGE_RATE );
		FEATURE_NAMES.put( DIRECTIONAL_CHANGE_RATE, "Directional change rate" );
		FEATURE_SHORT_NAMES.put( DIRECTIONAL_CHANGE_RATE, "𝛾 rate" );
		FEATURE_DIMENSIONS.put( DIRECTIONAL_CHANGE_RATE, Dimension.ANGLE_RATE );
		IS_INT.put( DIRECTIONAL_CHANGE_RATE, Boolean.FALSE );
	}

	public DirectionalChangeAnalyzer()
	{
		super( KEY, KEY, FEATURES, FEATURE_NAMES, FEATURE_SHORT_NAMES, FEATURE_DIMENSIONS, IS_INT );
	}

	@Override
	protected void analyze( final DefaultWeightedEdge edge, final Model model )
	{
		final FeatureModel featureModel = model.getFeatureModel();
		
		// Storage array for 3D angle calculation.
		final double[] out = new double[ 3 ];

		Spot source = model.getTrackModel().getEdgeSource( edge );
		Spot target = model.getTrackModel().getEdgeTarget( edge );

		// Some edges maybe improperly oriented.
		if ( source.diffTo( target, Spot.FRAME ) > 0. )
		{
			final Spot tmp = target;
			target = source;
			source = tmp;
		}

		/*
		 * Edge absolute angle.
		 */

		final double dx2 = target.diffTo( source, Spot.POSITION_X );
		final double dy2 = target.diffTo( source, Spot.POSITION_Y );
		final double dz2 = target.diffTo( source, Spot.POSITION_Z );

		/*
		 * Rate of directional change. We need to fetch the predecessor edge,
		 * via the source.
		 */

		final Set< DefaultWeightedEdge > sourceEdges = model.getTrackModel().edgesOf( source );
		int nPredecessors = 0;
		Spot predecessor = null;
		for ( final DefaultWeightedEdge sourceEdge : sourceEdges )
		{
			predecessor = model.getTrackModel().getEdgeSource( sourceEdge );
			if ( predecessor.equals( source ) )
				predecessor = model.getTrackModel().getEdgeTarget( sourceEdge );
			
			if ( predecessor.diffTo( source, Spot.FRAME ) < 0. )
				nPredecessors++;
				// This is a predecessor edge.
		}

		/*
		 * We work only if there is only one predecessor. The directional change
		 * is anyway not defined in case of branching.
		 */
		if ( nPredecessors != 1 )
		{
			featureModel.putEdgeFeature( edge, DIRECTIONAL_CHANGE_RATE, Double.NaN );
			return;
		}

		// Vectors.
		final double dx1 = source.diffTo( predecessor, Spot.POSITION_X );
		final double dy1 = source.diffTo( predecessor, Spot.POSITION_Y );
		final double dz1 = source.diffTo( predecessor, Spot.POSITION_Z );

		crossProduct( dx1, dy1, dz1, dx2, dy2, dz2, out );
		final double deltaAlpha = Math.atan2( norm( out ), dotProduct( dx1, dy1, dz1, dx2, dy2, dz2 ) );
		final double angleSpeed = deltaAlpha / target.diffTo( source, Spot.POSITION_T );

		featureModel.putEdgeFeature( edge, DIRECTIONAL_CHANGE_RATE, Double.valueOf( angleSpeed ) );
	}

	private static final double dotProduct( final double dx1, final double dy1, final double dz1, final double dx2, final double dy2, final double dz2 )
	{
		return dx1 * dx2 + dy1 * dy2 + dz1 * dz2;
	}

	private static final void crossProduct( final double dx1, final double dy1, final double dz1, final double dx2, final double dy2, final double dz2, final double[] out )
	{
		out[ 0 ] = dy1 * dz2 - dz1 * dy2;
		out[ 1 ] = dz1 * dx2 - dx1 * dz2;
		out[ 2 ] = dx1 * dy2 - dy1 * dx2;
	}

	private static final double norm( final double[] v )
	{
		double sumSq = 0.;
		for ( final double d : v )
			sumSq += d * d;
		return Math.sqrt( sumSq );
	}
}
