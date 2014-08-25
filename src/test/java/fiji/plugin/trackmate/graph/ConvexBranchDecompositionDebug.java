package fiji.plugin.trackmate.graph;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.graph.ConvexBranchesDecomposition.TrackBranchDecomposition;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class ConvexBranchDecompositionDebug
{
	public static void main( final String[] args )
	{

		final Spot sa0 = new Spot( 0, 0, 0, 1, -1, "SA_0" );
		final Spot sa1 = new Spot( 0, 0, 0, 1, -1, "SA_1" );
		final Spot sa3 = new Spot( 0, 0, 0, 1, -1, "SA_3" );
		final Spot sa4 = new Spot( 0, 0, 0, 1, -1, "SA_4" );

		final Spot sb0 = new Spot( 0, 0, 0, 1, -1, "SB_0" );
		final Spot sb1 = new Spot( 0, 0, 0, 1, -1, "SB_1" );
		final Spot sb3 = new Spot( 0, 0, 0, 1, -1, "SB_3" );
		final Spot sb4 = new Spot( 0, 0, 0, 1, -1, "SB_4" );

		final Spot nexus = new Spot( 0, 0, 0, 1, -1, "NEXUS" );

		final SpotCollection spots = new SpotCollection();
		spots.add( sa0, 0 );
		spots.add( sb0, 0 );
		spots.add( sa1, 1 );
		spots.add( sb1, 1 );
		spots.add( nexus, 2 );
		spots.add( sa3, 3 );
		spots.add( sb3, 3 );
		spots.add( sa4, 4 );
		spots.add( sb4, 4 );

		final Model model = new Model();
		model.setSpots( spots, false );

		model.addEdge( sa0, sa1, -2 );
		model.addEdge( sb0, sb1, -2 );
		model.addEdge( sa1, nexus, -2 );
		model.addEdge( sb1, nexus, -2 );
		model.addEdge( nexus, sa3, -2 );
		model.addEdge( nexus, sb3, -2 );
		model.addEdge( sa3, sa4, -2 );
		model.addEdge( sb3, sb4, -2 );

		final SelectionModel sm = new SelectionModel( model );
		final TrackScheme trackScheme = new TrackScheme( model, sm );
		trackScheme.render();

		final TrackModel tm = model.getTrackModel();
		final Integer trackID = tm.trackIDOf( sa0 );
		final TimeDirectedNeighborIndex neighborIndex = tm.getDirectedNeighborIndex();

		final TrackBranchDecomposition branchDecomposition = ConvexBranchesDecomposition.processTrack( trackID, tm, neighborIndex, false, false );
		System.out.println( branchDecomposition );

		ConvexBranchesDecomposition.buildBranchGraph( branchDecomposition );
	}
}
