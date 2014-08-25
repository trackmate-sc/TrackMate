package fiji.plugin.trackmate.action;

import ij.IJ;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.graph.ConvexBranchesDecomposition;
import fiji.plugin.trackmate.graph.ConvexBranchesDecomposition.TrackBranchDecomposition;
import fiji.plugin.trackmate.graph.TimeDirectedNeighborIndex;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.io.TmXmlReader;

public class TrackBranchAnalysis extends AbstractTMAction
{

	private static final String INFO_TEXT = "<html>This action splits each track in branches and run analysis on each branch.</html>";

	private static final String KEY = "TRACK_BRANCH_ANALYSIS";

	private static final String NAME = "Branches analysis";

	private static final ImageIcon ICON = null;


	@Override
	public void execute( final TrackMate trackmate )
	{
		logger.log( "Generating track branches analysis.\n" );
		final Model model = trackmate.getModel();
		final int ntracks = model.getTrackModel().nTracks( true );
		if ( ntracks == 0 )
		{
			logger.log( "No visible track found. Aborting.\n" );
			return;
		}

		final TimeDirectedNeighborIndex neighborIndex = model.getTrackModel().getDirectedNeighborIndex();

		final Set< Spot > visited = new HashSet< Spot >();

		for ( final Integer trackID : model.getTrackModel().unsortedTrackIDs( true ) )
		{
			final TrackBranchDecomposition branchDecomposition = ConvexBranchesDecomposition.processTrack( trackID, model.getTrackModel(), neighborIndex, true, false );
			final SimpleDirectedGraph< List< Spot >, DefaultEdge > branchGraph = ConvexBranchesDecomposition.buildBranchGraph( branchDecomposition );

			final Map< Branch, Set< List< Spot >> > successorMap = new HashMap< Branch, Set< List< Spot >> >();
			final Map< Branch, Set< List< Spot >> > predecessorMap = new HashMap< Branch, Set< List< Spot >> >();
			final Map<List<Spot>, Branch> branchMap = new HashMap< List<Spot>, Branch >();

			for ( final List< Spot > branch : branchGraph.vertexSet() )
			{
				final Branch br = new Branch();
				branchMap.put( branch, br );

				// First and last spot.
				br.first = branch.get( 0 );
				br.last = branch.get( branch.size() - 1 );

				// Predecessors
				final Set< DefaultEdge > incomingEdges = branchGraph.incomingEdgesOf( branch );
				final Set< List< Spot >> predecessors = new HashSet< List< Spot > >( incomingEdges.size() );
				for ( final DefaultEdge edge : incomingEdges )
				{
					final List< Spot > predecessorBranch = branchGraph.getEdgeSource( edge );
					predecessors.add( predecessorBranch );
				}

				// Successors
				final Set< DefaultEdge > outgoingEdges = branchGraph.outgoingEdgesOf( branch );
				final Set< List< Spot >> successors = new HashSet< List< Spot > >( outgoingEdges.size() );
				for ( final DefaultEdge edge : outgoingEdges )
				{
					final List< Spot > successorBranch = branchGraph.getEdgeTarget( edge );
					successors.add( successorBranch );
				}

				successorMap.put( br, successors );
				predecessorMap.put( br, predecessors );
			}
			
			for ( final Branch br : successorMap.keySet() )
			{
				final Set< List< Spot >> succs = successorMap.get( br );
				final Set<Branch> succBrs = new HashSet< Branch >(succs.size());
				for ( final List<Spot> branch : succs )
				{
					final Branch succBr = branchMap.get( branch );
					succBrs.add( succBr );
				}
				br.successors = succBrs;
				
				final Set< List< Spot >> preds = predecessorMap.get( br );
				final Set<Branch> predBrs = new HashSet< Branch >(preds.size());
				for ( final List<Spot> branch : preds )
				{
					final Branch predBr = branchMap.get( branch );
					predBrs.add( predBr );
				}
				br.predecessors = predBrs;
			}
				

				String branchName = "" + first.getName() + "-" + last.getName();

				if ( neighborIndex.predecessorsOf( first ).size() < 1 )
				{
					logger.log( "Branch " + branchName + " starting point is not defined. Skipping.\n" );
					continue;
				}

				final Set< Spot > nextOnes = neighborIndex.successorsOf( last );
				final int nSucc = nextOnes.size();
				if ( nSucc < 1 )
				{
					logger.log( "Branch " + branchName + " end point is not defined. Skipping.\n" );
					continue;
				}

				final Spot nextOne = nextOnes.iterator().next();
				if ( visited.contains( nextOne ) )
				{
					continue;
				}
				visited.add( nextOne );
				if ( nSucc == 1 )
				{
					// It is a fusion - change the branch name
					final Spot previous = neighborIndex.predecessorsOf( first ).iterator().next();
					branchName = "" + previous.getName() + "-" + nextOne.getName();
				}

				final int nPred = neighborIndex.predecessorsOf( nextOne ).size();
				final DivisionType type = DivisionType.getType( nSucc, nPred );
				final int dt = ( int ) last.diffTo( first, Spot.FRAME );
				logger.log( "Branch " + branchName + ", " + type.getName() + ", dt = " + dt + " frames.\n" );
			}

		}
		logger.log( "Done.\n" );

	}

	/*
	 * STATIC CLASSES AND ENUMS
	 */

	/**
	 * A class to describe a branch.
	 */
	class Branch
	{
		Spot first;

		Spot last;

		double dt;

		 Set< Branch > predecessors;

		 Set< Branch > successors;
	}

	public static enum DivisionType
	{

		BIPOLAR_DIVISION( "Bipolar division" ), TRIPOLAR_DIVISION( "Tripolar division" ), TETRAPOLAR_DIVISION( "Tetrapolar division" ), FUSION( "Fusion" ), OTHER( "Other cases" );

		private final String name;

		DivisionType(final String name)
		{
			this.name = name;
		}

		public String getName()
		{
			return name;
		}

		public static DivisionType getType( final int nSucc, final int nPred )
		{
			if ( nPred < 2 )
			{
				switch ( nSucc )
				{
				case 2:
					return BIPOLAR_DIVISION;
				case 3:
					return TRIPOLAR_DIVISION;
				case 4:
					return TETRAPOLAR_DIVISION;
				default:
					return OTHER;
				}
			}
			else
			{
				if ( nSucc == 1 )
				{
					return FUSION;
				}
				else
				{
					return OTHER;
				}
			}
		}


	}

	@Plugin( type = TrackMateActionFactory.class, enabled = true )
	public static class Factory implements TrackMateActionFactory
	{



		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getName()
		{
			return NAME;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public TrackMateAction create( final TrackMateGUIController controller )
		{
			return new TrackBranchAnalysis();
		}

		@Override
		public ImageIcon getIcon()
		{
			return ICON;
		}
	}

	/*
	 * MAIN METHOD
	 */

	public static void main( final String[] args )
	{
		final File file = new File( "/Users/tinevez/Desktop/Data/Milan/Video_C03-1-fullHD.xml" );
		final TmXmlReader reader = new TmXmlReader( file );
		if ( !reader.isReadingOk() )
		{
			IJ.error( TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION, reader.getErrorMessage() );
			return;
		}
		final Model model = reader.getModel();
		if ( !reader.isReadingOk() )
		{
			IJ.error( "Problem reading the model:\n" + reader.getErrorMessage() );
		}

		final TrackMate trackmate = new TrackMate( model, new Settings() );

		final TrackBranchAnalysis analyzer = new TrackBranchAnalysis();
		analyzer.setLogger( Logger.IJ_LOGGER );
		analyzer.execute( trackmate );
	}

}
