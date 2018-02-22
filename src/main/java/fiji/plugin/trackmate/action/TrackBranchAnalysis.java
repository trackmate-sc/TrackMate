package fiji.plugin.trackmate.action;

import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.text.TextPanel;
import ij.text.TextWindow;

import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.graph.ConvexBranchesDecomposition;
import fiji.plugin.trackmate.graph.ConvexBranchesDecomposition.TrackBranchDecomposition;
import fiji.plugin.trackmate.graph.TimeDirectedNeighborIndex;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;

public class TrackBranchAnalysis extends AbstractTMAction
{

	private static final String INFO_TEXT = "<html>This action analyzes each branch of all tracks, and outputs in an ImageJ results table the number of its predecessors, of successors, and its duration."
			+ "<p>"
			+ "The results table is in sync with the selection. Clicking on a line will select the target branch.</html>";

	private static final String KEY = "TRACK_BRANCH_ANALYSIS";

	private static final String NAME = "Branch hierarchy analysis";

	private static final ImageIcon ICON;
	static
	{
		final Image image = new ImageIcon( TrackMateWizard.class.getResource( "images/Icons4_print_transparency.png" ) ).getImage();
		final Image newimg = image.getScaledInstance( 16, 16, java.awt.Image.SCALE_SMOOTH );
		ICON = new ImageIcon( newimg );
	}

	private static final String TABLE_NAME = "Branch analysis";

	private final SelectionModel selectionModel;

	public TrackBranchAnalysis( final SelectionModel selectionModel )
	{
		this.selectionModel = selectionModel;
	}

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

		final List< Branch > brs = new ArrayList<>();
		for ( final Integer trackID : model.getTrackModel().unsortedTrackIDs( true ) )
		{
			final TrackBranchDecomposition branchDecomposition = ConvexBranchesDecomposition.processTrack( trackID, model.getTrackModel(), neighborIndex, true, false );
			final SimpleDirectedGraph< List< Spot >, DefaultEdge > branchGraph = ConvexBranchesDecomposition.buildBranchGraph( branchDecomposition );

			final Map< Branch, Set< List< Spot >> > successorMap = new HashMap<>();
			final Map< Branch, Set< List< Spot >> > predecessorMap = new HashMap<>();
			final Map<List<Spot>, Branch> branchMap = new HashMap<>();

			for ( final List< Spot > branch : branchGraph.vertexSet() )
			{
				final Branch br = new Branch();
				branchMap.put( branch, br );

				// Track name from ID
				br.trackID = model.getTrackModel().name( trackID );

				// First and last spot.
				br.first = branch.get( 0 );
				br.last = branch.get( branch.size() - 1 );

				// Distance traveled.
				br.distanceTraveled = Math.sqrt( br.last.squareDistanceTo( br.first ) );

				// Compute mean velocity "by hand".
				final double meanV;
				if ( branch.size() < 2 )
				{
					meanV = Double.NaN;
				}
				else
				{
					final Iterator< Spot > it = branch.iterator();
					Spot previous = it.next();
					double sum = 0;
					while ( it.hasNext() )
					{
						final Spot next = it.next();
						final double dr = Math.sqrt( next.squareDistanceTo( previous ) );
						sum += dr;
						previous = next;
					}
					meanV = sum / ( branch.size() - 1 );
				}
				br.meanVelocity = meanV;

				// Predecessors
				final Set< DefaultEdge > incomingEdges = branchGraph.incomingEdgesOf( branch );
				final Set< List< Spot >> predecessors = new HashSet<>( incomingEdges.size() );
				for ( final DefaultEdge edge : incomingEdges )
				{
					final List< Spot > predecessorBranch = branchGraph.getEdgeSource( edge );
					predecessors.add( predecessorBranch );
				}

				// Successors
				final Set< DefaultEdge > outgoingEdges = branchGraph.outgoingEdgesOf( branch );
				final Set< List< Spot >> successors = new HashSet<>( outgoingEdges.size() );
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
				final Set<Branch> succBrs = new HashSet<>(succs.size());
				for ( final List<Spot> branch : succs )
				{
					final Branch succBr = branchMap.get( branch );
					succBrs.add( succBr );
				}
				br.successors = succBrs;
				
				final Set< List< Spot >> preds = predecessorMap.get( br );
				final Set<Branch> predBrs = new HashSet<>(preds.size());
				for ( final List<Spot> branch : preds )
				{
					final Branch predBr = branchMap.get( branch );
					predBrs.add( predBr );
				}
				br.predecessors = predBrs;
			}

			brs.addAll( successorMap.keySet() );
		}

		Collections.sort( brs );
		final ResultsTable table = new ResultsTable();
		for ( final Branch br : brs )
		{
			table.incrementCounter();
			table.addLabel( br.toString() );
			table.addValue( "Track ID", br.trackID );
			table.addValue( "N predecessors", br.predecessors.size() );
			table.addValue( "N successors", br.successors.size() );
			table.addValue( "dt (frames)", br.dt() );
			table.addValue( "Distance (" + model.getSpaceUnits() + ")", br.distanceTraveled );
			table.addValue( "Mean velocity (" + model.getSpaceUnits() + "/frames)", br.meanVelocity );
			table.addValue( "First", br.first.getName() );
			table.addValue( "Last", br.last.getName() );
		}
		table.setPrecision( 2 );
		table.show( TABLE_NAME );
		logger.log( "Done.\n" );

		// Hack to make the results table in sync with selection model.
		if ( null != selectionModel )
		{
			final TextWindow window = ( TextWindow ) WindowManager.getWindow( TABLE_NAME );
			final TextPanel textPanel = window.getTextPanel();
			textPanel.addMouseListener( new MouseAdapter()
			{
				@Override
				public void mouseClicked( final MouseEvent e )
				{
					final int line = textPanel.getSelectionStart();
					if ( line < 0 ) { return; }
					final Branch br = brs.get( line );
					final List< DefaultWeightedEdge > edges = model.getTrackModel().dijkstraShortestPath( br.first, br.last );
					final Set< Spot > spots = new HashSet<>();
					for ( final DefaultWeightedEdge edge : edges )
					{
						spots.add( model.getTrackModel().getEdgeSource( edge ) );
						spots.add( model.getTrackModel().getEdgeTarget( edge ) );
					}
					selectionModel.clearSelection();
					selectionModel.addEdgeToSelection( edges );
					selectionModel.addSpotToSelection( spots );
				}

			} );
		}


	}

	/*
	 * STATIC CLASSES AND ENUMS
	 */

	/**
	 * A class to describe a branch.
	 */
	class Branch implements Comparable< Branch >
	{
		double meanVelocity;

		double distanceTraveled;

		String trackID;

		Spot first;

		Spot last;

		Set< Branch > predecessors;

		Set< Branch > successors;

		@Override
		public String toString()
		{
			return first + " → " + last;
		}

		int dt()
		{
			return ( int ) last.diffTo( first, Spot.FRAME );
		}

		/**
		 * Sort by predecessors number, then successors number, then
		 * alphabetically by first spot name.
		 */
		@Override
		public int compareTo( final Branch o )
		{
			if ( predecessors.size() != o.predecessors.size() ) { return predecessors.size() - o.predecessors.size(); }
			if ( successors.size() != o.successors.size() ) { return successors.size() - o.successors.size(); }
			if ( first.getName().compareTo( o.first.getName() ) != 0 ) { return first.getName().compareTo( o.first.getName() ); }
			return last.getName().compareTo( o.last.getName() );
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
			return new TrackBranchAnalysis( controller.getSelectionModel() );
		}

		@Override
		public ImageIcon getIcon()
		{
			return ICON;
		}
	}
}
