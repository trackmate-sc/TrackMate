package fiji.plugin.trackmate.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.SimpleGraph;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;
import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;

/**
 * A class that can decompose the tracks of a {@link Model} in convex branches.
 * <p>
 * A convex branch is a portion of a track for which all spots - but the first
 * and last one - have exactly one predecessor and one successor (in time). The
 * first and last spots of a branch may have 0 or 1 or more predecessors or
 * successors respectively, depending on they are the start or the end of a
 * track, or a fusion or merging point, or a gap (see below).
 * <p>
 * Schematically, if a track is arranged as follow:
 * 
 * <pre>
 * A
 * |
 * B--+
 * |  |
 * C  I
 * |  |
 * D  J
 * |  |
 * E  K
 * |  |
 * F  L
 * |  |
 * G--+
 * |
 * H
 * </pre>
 * 
 * then
 * 
 * <pre>
 * A - B,
 * C - D - E - F,
 * I - J - K - L,
 * G - H
 * </pre>
 * 
 * are convex branches. This class generates the decomposition of a track in
 * these branches.
 * <p>
 * In the example above, note that another acceptable decomposition could be:
 * 
 * <pre>
 * A,
 * B - C - D - E - F - G,
 * I - J - K - L,
 * H
 * </pre>
 * 
 * depending on to what branch you choose to attach splitting or merging points.
 * This class attaches split points to the end of the early branch, and merge
 * points to the beginning of the late branch. So that for our example above,
 * the output is indeed:
 * 
 * <pre>
 * A - B,
 * C - D - E - F,
 * I - J - K - L,
 * G - H
 * </pre>
 * <p>
 * The behavior of this algorithm can be tuned using two boolean flags. The
 * first one specifies whether we can violate the convex branch contract and
 * have branches that contain a spot with more than one predecessor and one
 * successor. For instance, if a track is as follow:
 * 
 * <pre>
 * A
 * |
 * B
 * |
 * C--+
 * |  |
 * D  F
 * |  |
 * E  G
 * </pre>
 * 
 * the default output would be:
 * 
 * <pre>
 * A - B - C,
 * D - E,
 * F - G
 * </pre>
 * 
 * Setting the <code>forbidMiddleLinks</code> flag to <code>false</code> would
 * give instead:
 * 
 * <pre>
 * A - B - C - D - E,
 * F - G
 * </pre>
 * 
 * which yields fewer and longer branches.
 * <p>
 * Some branches may have gaps in them, that is two spots separated by more than
 * one frame. By default this does not lead to cutting the branch in two. If you
 * want to force branches to contain spots that are separated by exactly only
 * one frame, set the <code>forbidGaps</code> flag to <code>true</code>. In that
 * case, a track arranged as following (<code>ø</code> is a missing detection in
 * a frame, or a gap):
 * 
 * <pre>
 * A - B - C - ø - D - E - F
 * </pre>
 * 
 * will be split in two branches.
 * 
 * <pre>
 * A - B - C,
 * D - E - F
 * </pre>
 * <p>
 * It is ensured that each spot in the model is present in exactly one branch of
 * the decomposition. Only spots belonging to visible tracks are taken into
 * account. This class also outputs the links that were cut in the source model
 * to generate these branches.
 * 
 * @author Jean-Yves Tinevez - 2014
 */
public class ConvexBranchesDecomposition implements Algorithm, Benchmark
{
	private static final String BASE_ERROR_MSG = "[ConvexBranchesDecomposition] ";

	private String errorMessage;

	private Collection< List< Spot >> branches;

	private Collection< List< Spot > > links;

	private Map< Integer, Collection< List< Spot >>> branchesPerTrack;

	private Map< Integer, Collection< List< Spot > >> linksPerTrack;

	private long processingTime;

	private final TrackModel tm;

	private final TimeDirectedNeighborIndex neighborIndex;

	private final boolean forbidMiddleLinks;

	private final boolean forbidGaps;

	/**
	 * Creates a new track splitter.
	 *
	 * @param model
	 *            the {@link Model} from which tracks are to be split. Only
	 *            tracks marked visible will be processed.
	 * @param forbidMiddleLinks
	 *            specifies whether we enforce links between branches to be
	 *            between an end point of a branch and a start point of another
	 *            branch. If <code>true</code>, links will only reach for these
	 *            spots. If <code>false</code>, a link can target a spot within
	 *            a branch, which can lead to fewer and longer branches.
	 * @param forbidGaps
	 *            specifies whether we forbid gaps in tracks. If
	 *            <code>true</code>, a track containing a gap (detections
	 *            missing in at least 1 consecutive frames) will be split in 2
	 *            branches. If <code>false</code>, branches may contain gaps.
	 */
	public ConvexBranchesDecomposition( final Model model, final boolean forbidMiddleLinks, final boolean forbidGaps )
	{
		this.forbidMiddleLinks = forbidMiddleLinks;
		this.forbidGaps = forbidGaps;
		this.tm = model.getTrackModel();
		this.neighborIndex = tm.getDirectedNeighborIndex();
	}

	/**
	 * Creates a new track splitter. Links between spots from within branches
	 * and gaps within convex branches are forbidden.
	 *
	 * @param model
	 *            the {@link Model} from which tracks are to be split. Only
	 *            tracks marked visible will be processed.
	 */
	public ConvexBranchesDecomposition( final Model model )
	{
		this( model, true, true );
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public boolean checkInput()
	{
		final long start = System.currentTimeMillis();
		for ( final DefaultWeightedEdge edge : tm.edgeSet() )
		{
			final Spot source = tm.getEdgeSource( edge );
			final Spot target = tm.getEdgeTarget( edge );
			if ( source.diffTo( target, Spot.FRAME ) == 0d )
			{
				errorMessage = BASE_ERROR_MSG + "Cannot deal with links between two spots in the same frame (" + source + " & " + target + ").\n";
				return false;
			}
		}
		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	@Override
	public boolean process()
	{
		final long startT = System.currentTimeMillis();

		final Set< Integer > trackIDs = tm.trackIDs( true );

		branches = new ArrayList<>();
		branchesPerTrack = new HashMap<>();
		links = new ArrayList< >();
		linksPerTrack = new HashMap<>();
		for ( final Integer trackID : trackIDs )
		{
			final TrackBranchDecomposition branchDecomposition = processTrack( trackID, tm, neighborIndex, forbidMiddleLinks, forbidGaps );

			branchesPerTrack.put( trackID, branchDecomposition.branches );
			linksPerTrack.put( trackID, branchDecomposition.links );

			branches.addAll( branchDecomposition.branches );
			links.addAll( branchDecomposition.links );
		}

		final long endT = System.currentTimeMillis();
		processingTime = endT - startT;

		return true;

	}

	/**
	 * A static utility that generates the convex branch decomposition of a
	 * specific track in a model.
	 *
	 * @param trackID
	 *            the ID of the track to decompose.
	 * @param tm
	 *            the {@link TrackModel} in which the track is stored.
	 * @param neighborIndex
	 *            a {@link TimeDirectedNeighborIndex} needed to quickly retrieve
	 *            neighbors in the mother graph.
	 * @param forbidMiddleLinks
	 *            if <code>true</code>, the decomposition will include branches
	 *            where only the first and last spots may have more than one
	 *            predecessor and one successor respectively. If
	 *            <code>false</code>, some spots inside a branch may be a fusion
	 *            or splitting point. This leads to fewer and longer branches.
	 * @param forbidGaps
	 *            if <code>true</code>, two neighbor spots in a branch will be
	 *            separated by exactly one frame. If <code>false</code>,
	 *            branches will include gaps.
	 * @return a new {@link TrackBranchDecomposition}.
	 * @see ConvexBranchesDecomposition
	 */
	public static final TrackBranchDecomposition processTrack( final Integer trackID, final TrackModel tm, final TimeDirectedNeighborIndex neighborIndex, final boolean forbidMiddleLinks, final boolean forbidGaps )
	{
		final Set< Spot > allSpots = tm.trackSpots( trackID );
		final Set< DefaultWeightedEdge > allEdges = tm.trackEdges( trackID );
		final SimpleGraph< Spot, DefaultWeightedEdge > graph = new SimpleGraph< >( DefaultWeightedEdge.class );

		for ( final Spot spot : allSpots )
		{
			graph.addVertex( spot );
		}
		for ( final DefaultWeightedEdge edge : allEdges )
		{
			graph.addEdge( tm.getEdgeSource( edge ), tm.getEdgeTarget( edge ) );
		}

		final Collection< List< Spot >> links = new HashSet< >();
		for ( final Spot spot : allSpots )
		{
			final Set< Spot > successors = neighborIndex.successorsOf( spot );
			final Set< Spot > predecessors = neighborIndex.predecessorsOf( spot );
			if ( predecessors.size() <= 1 && successors.size() <= 1 )
			{
				continue;
			}

			if ( predecessors.size() == 0 )
			{
				boolean found = false;
				for ( final Spot successor : successors )
				{
					if ( !forbidMiddleLinks && !found && successor.diffTo( spot, Spot.FRAME ) < 2 )
					{
						found = true;
					}
					else
					{
						graph.removeEdge( spot, successor );
						links.add( makeLink( spot, successor ) );
					}
				}
			}
			else if ( successors.size() == 0 )
			{
				boolean found = false;
				for ( final Spot predecessor : predecessors )
				{
					if ( !forbidMiddleLinks && !found && spot.diffTo( predecessor, Spot.FRAME ) < 2 )
					{
						found = true;
					}
					else
					{
						graph.removeEdge( predecessor, spot );
						links.add( makeLink( predecessor, spot ) );
					}
				}
			}
			else if ( predecessors.size() == 1 )
			{
				final Spot previous = predecessors.iterator().next();
				if ( previous.diffTo( spot, Spot.FRAME ) < 2 )
				{
					for ( final Spot successor : successors )
					{
						graph.removeEdge( spot, successor );
						links.add( makeLink( spot, successor ) );
					}
				}
				else
				{
					graph.removeEdge( previous, spot );
					links.add( makeLink( previous, spot ) );
					boolean found = false;
					for ( final Spot successor : successors )
					{
						if ( !forbidMiddleLinks && !found && successor.diffTo( spot, Spot.FRAME ) < 2 )
						{
							found = true;
						}
						else
						{
							graph.removeEdge( spot, successor );
							links.add( makeLink( spot, successor ) );
						}
					}
				}
			}
			else if ( successors.size() == 1 )
			{
				final Spot next = successors.iterator().next();
				if ( spot.diffTo( next, Spot.FRAME ) < 2 )
				{
					for ( final Spot predecessor : predecessors )
					{
						graph.removeEdge( predecessor, spot );
						links.add( makeLink( predecessor, spot ) );
					}
				}
				else
				{
					graph.removeEdge( spot, next );
					links.add( makeLink( spot, next ) );
					boolean found = false;
					for ( final Spot predecessor : predecessors )
					{
						if ( !forbidMiddleLinks && !found && spot.diffTo( predecessor, Spot.FRAME ) < 2 )
						{
							found = true;
						}
						else
						{
							graph.removeEdge( predecessor, spot );
							links.add( makeLink( predecessor, spot ) );
						}
					}
				}
			}
			else
			{
				/*
				 * Complex point: we have more than 2 successor and more than 2
				 * predecessors.
				 */
				boolean found = false;
				for ( final Spot predecessor : predecessors )
				{
					if ( !forbidMiddleLinks && !found && spot.diffTo( predecessor, Spot.FRAME ) < 2 )
					{
						found = true;
					}
					else
					{
						graph.removeEdge( predecessor, spot );
						links.add( makeLink( predecessor, spot ) );
					}
				}
				if ( !forbidMiddleLinks )
				{
					// Possibly extend the branch requires resetting this to
					// false, so that we do not destroy on outgoing link.
					found = false;
				}
				for ( final Spot successor : successors )
				{
					if ( !forbidMiddleLinks && !found && successor.diffTo( spot, Spot.FRAME ) < 2 )
					{
						found = true;
					}
					else
					{
						graph.removeEdge( spot, successor );
						links.add( makeLink( spot, successor ) );
					}
				}
			}
		}

		/*
		 * 2nd pass: remove gaps.
		 */

		if ( forbidGaps )
		{
			final Set< DefaultWeightedEdge > newEdges = graph.edgeSet();
			final Set< DefaultWeightedEdge > toRemove = new HashSet< >();
			for ( final DefaultWeightedEdge edge : newEdges )
			{
				final Spot source = graph.getEdgeSource( edge );
				final Spot target = graph.getEdgeTarget( edge );
				if ( Math.abs( source.diffTo( target, Spot.FRAME ) ) > 1 )
				{
					toRemove.add( edge );
					links.add( makeLink( source, target ) );
				}
			}

			for ( final DefaultWeightedEdge edge : toRemove )
			{
				graph.removeEdge( edge );
			}
		}

		/*
		 * Output
		 */

		final ConnectivityInspector< Spot, DefaultWeightedEdge > connectivity = new ConnectivityInspector< >( graph );
		final List< Set< Spot >> connectedSets = connectivity.connectedSets();
		final Collection< List< Spot >> branches = new HashSet< >( connectedSets.size() );
		final Comparator< Spot > comparator = Spot.frameComparator;
		for ( final Set< Spot > set : connectedSets )
		{
			final List< Spot > branch = new ArrayList< >( set.size() );
			branch.addAll( set );
			Collections.sort( branch, comparator );
			branches.add( branch );
		}

		final TrackBranchDecomposition output = new TrackBranchDecomposition();
		output.branches = branches;
		output.links = links;
		return output;

	}

	/**
	 * Builds a directed graph made of a convex branch decomposition.
	 * <p>
	 * In the graph, the vertices are made of the branches of the decomposition,
	 * and the edges are the links between each branch.
	 * 
	 * @param branchDecomposition
	 *            the convex branch decomposition to transform.
	 * @return a new simple directed graph. The direction of the edges in the
	 *         graph are taken as the end of a branch is the source, and the
	 *         beginning of a branch as the target, following time.
	 */
	public static final SimpleDirectedGraph< List< Spot >, DefaultEdge > buildBranchGraph( final TrackBranchDecomposition branchDecomposition )
	{
		final SimpleDirectedGraph< List< Spot >, DefaultEdge > branchGraph = new SimpleDirectedGraph< >( DefaultEdge.class );

		final Collection< List< Spot >> branches = branchDecomposition.branches;
		final Collection< List< Spot >> links = branchDecomposition.links;

		// Map of the first spot of each branch.
		final Map< Spot, List< Spot > > firstSpots = new HashMap< >( branches.size() );
		// Map of the last spot of each branch.
		final Map< Spot, List< Spot > > lastSpots = new HashMap< >( branches.size() );
		for ( final List< Spot > branch : branches )
		{
			firstSpots.put( branch.get( 0 ), branch );
			lastSpots.put( branch.get( branch.size() - 1 ), branch );
			branchGraph.addVertex( branch );
		}

		for ( final List< Spot > link : links )
		{
			final Spot source = link.get( 0 );
			final Spot target = link.get( 1 );

			List< Spot > targetBranch = firstSpots.get( target );
			if ( targetBranch == null )
			{
				/*
				 * We could not find this link's target in the map of first
				 * spots. Most likely this means that the link targets a middle
				 * spot, because the branch decomposition authorized it. So we
				 * have to find it...
				 */
				for ( final List< Spot > branch : branches )
				{
					if ( branch.contains( target ) )
					{
						targetBranch = branch;
						break;
					}
				}
			}

			List< Spot > sourceBranch = lastSpots.get( source );
			if ( sourceBranch == null )
			{
				for ( final List< Spot > branch : branches )
				{
					if ( branch.contains( source ) )
					{
						sourceBranch = branch;
						break;
					}
				}
			}

			branchGraph.addEdge( sourceBranch, targetBranch );
		}

		return branchGraph;
	}

	private static final List< Spot > makeLink( final Spot spotA, final Spot spotB )
	{
		final List< Spot > link = new ArrayList< >( 2 );
		link.add( spotA );
		link.add( spotB );
		return link;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	/**
	 * Returns the collection of branches built by this algorithm.
	 * <p>
	 * Branches are returned as list of spot. It is ensured that the spots are
	 * ordered in the list by increasing frame number, and that two consecutive
	 * spot are separated by exactly one frame.
	 *
	 * @return the collection of branches.
	 */
	public Collection< List< Spot >> getBranches()
	{
		return branches;
	}

	/**
	 * Returns the mapping of each source track ID to the branches it was split
	 * in.
	 * <p>
	 * Branches are returned as list of spot. It is ensured that the spots are
	 * ordered in the list by increasing frame number, and that two consecutive
	 * spot are separated by exactly one frame.
	 *
	 * @return a mapping of collections of branches.
	 */
	public Map< Integer, Collection< List< Spot >>> getBranchesPerTrack()
	{
		return branchesPerTrack;
	}

	/**
	 * Returns the links cut by this algorithm when splitting the model in
	 * linear, convex branches.
	 * <p>
	 * These links are returned as a collection of 2-elements list. If the
	 * instance was created with <code>forbidMiddleLinks</code> sets to
	 * <code>true</code>, it is ensured that the first element of all links is
	 * the last spot of a branch, and the second element of this link is the
	 * first spot of another branch. Otherwise, a link can target a spot within
	 * a branch.
	 *
	 * @return a collection of links as a 2-elements list.
	 */
	public Collection< List< Spot >> getLinks()
	{
		return links;
	}

	/**
	 * Returns the mapping of each source track ID to the links that were cut in
	 * it to split it in branches.
	 * <p>
	 * These links are returned as a collection of 2-elements list. If the
	 * instance was created with <code>forbidMiddleLinks</code> sets to
	 * <code>true</code>, it is ensured that the first element of all links is
	 * the last spot of a branch, and the second element of this link is the
	 * first spot of another branch. Otherwise, a link can target a spot within
	 * a branch.
	 *
	 * @return the mapping of track IDs to the links.
	 */
	public Map< Integer, Collection< List< Spot >>> getLinksPerTrack()
	{
		return linksPerTrack;
	}

	/*
	 * STATIC CLASSES
	 */

	/**
	 * A two public fields class used to return the convex branch decomposition
	 * of a track.
	 */
	public static final class TrackBranchDecomposition
	{
		/**
		 * Branches are returned as list of spot. It is ensured that the spots
		 * are ordered in the list by increasing frame number, and that two
		 * consecutive spot are separated by exactly one frame.
		 */
		public Collection< List< Spot >> branches;

		/**
		 * Links, as a collection of 2-elements list.
		 */
		public Collection< List< Spot >> links;

		@Override
		public String toString()
		{
			final StringBuilder str = new StringBuilder();
			str.append( super.toString() + ";\n" );
			str.append( "  Branches:\n" );
			int index = 0;
			for ( final List< Spot > branch : branches )
			{
				str.append( String.format( "    % 4d:\t" + branch + '\n', index++ ) );
			}
			str.append( "  Links:\n" );
			index = 0;
			for ( final List< Spot > link : links )
			{
				str.append( String.format( "    % 4d:\t" + link.get( 0 ) + "\t→\t" + link.get( 1 ) + '\n', index++ ) );
			}
			return str.toString();
		}
	}
}
