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

import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleGraph;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.tracking.TrackableObject;
import fiji.plugin.trackmate.tracking.TrackableObjectUtils;

/**
 * A class that can decompose the tracks of a {@link Model} in convex branches.
 * <p>
 * A convex branch is a portion of a track that contains spots that are
 * separated by exactly one frame. Here they are returned, sorted by increasing
 * frame number. It is ensured that the spots within a convex branch are not a
 * splitting or a merging point. If desired, gaps within a convex branch can be
 * removed.
 * <p>
 * This class also outputs the links that were cut in the source model to
 * generate these branches. A flag allows to specify whether these links must be
 * between end and starting point of a branch. Another flag relax the convex
 * constraint and allows gaps to be included in branches.
 * 
 * @author Jean-Yves Tinevez - 2014
 */
public class ConvexBranchesDecomposition<T extends TrackableObject> implements Algorithm, Benchmark
{
	private static final String BASE_ERROR_MSG = "[ConvexBranchesDecomposition] ";

	private String errorMessage;

	private Collection< List< T >> branches;

	private Collection< List< T > > links;

	private Map< Integer, Collection< List< T >>> branchesPerTrack;

	private Map< Integer, Collection< List< T > >> linksPerTrack;

	private long processingTime;

	private final TrackModel<T> tm;

	private final TimeDirectedNeighborIndex<T> neighborIndex;

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
	public ConvexBranchesDecomposition( final TrackModel<T> model, final boolean forbidMiddleLinks, final boolean forbidGaps )
	{
		this.forbidMiddleLinks = forbidMiddleLinks;
		this.forbidGaps = forbidGaps;
		this.tm = model;
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
	public ConvexBranchesDecomposition( final TrackModel<T> model )
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
			final T source = tm.getEdgeSource( edge );
			final T target = tm.getEdgeTarget( edge );
			if ( TrackableObjectUtils.frameDiff(source, target) == 0 )
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

		branches = new ArrayList< List< T >>();
		branchesPerTrack = new HashMap< Integer, Collection< List< T >>>();
		links = new ArrayList< List< T > >();
		linksPerTrack = new HashMap< Integer, Collection< List< T > >>();
		for ( final Integer trackID : trackIDs )
		{
			final TrackBranchDecomposition<T> branchDecomposition = processTrack( trackID, tm, neighborIndex, forbidMiddleLinks, forbidGaps );

			branchesPerTrack.put( trackID, branchDecomposition.branches );
			linksPerTrack.put( trackID, branchDecomposition.links );

			branches.addAll( branchDecomposition.branches );
			links.addAll( branchDecomposition.links );
		}

		final long endT = System.currentTimeMillis();
		processingTime = endT - startT;

		return true;

	}

	public static final <T extends TrackableObject> TrackBranchDecomposition<T> processTrack( final Integer trackID, final TrackModel<T> tm, final TimeDirectedNeighborIndex<T> neighborIndex, final boolean forbidMiddleLinks, final boolean forbidGaps )
	{
		final Set< T > allSpots = tm.trackSpots( trackID );
		final Set< DefaultWeightedEdge > allEdges = tm.trackEdges( trackID );
		final SimpleGraph< T, DefaultWeightedEdge > graph = new SimpleGraph< T, DefaultWeightedEdge >( DefaultWeightedEdge.class );

		for ( final T spot : allSpots )
		{
			graph.addVertex( spot );
		}
		for ( final DefaultWeightedEdge edge : allEdges )
		{
			graph.addEdge( tm.getEdgeSource( edge ), tm.getEdgeTarget( edge ) );
		}

		final Collection< List< T >> links = new HashSet< List< T > >();
		for ( final T spot : allSpots )
		{
			final Set< T > successors = neighborIndex.successorsOf( spot );
			final Set< T > predecessors = neighborIndex.predecessorsOf( spot );
			if ( predecessors.size() <= 1 && successors.size() <= 1 )
			{
				continue;
			}

			if ( predecessors.size() == 0 )
			{
				boolean found = false;
				for ( final T successor : successors )
				{
					if ( !forbidMiddleLinks && !found && TrackableObjectUtils.frameDiff(successor, spot) < 2 )
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
				for ( final T predecessor : predecessors )
				{
					if ( !forbidMiddleLinks && !found && TrackableObjectUtils.frameDiff(spot, predecessor) < 2 )
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
				final T previous = predecessors.iterator().next();
				if ( TrackableObjectUtils.frameDiff(previous, spot) < 2 )
				{
					for ( final T successor : successors )
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
					for ( final T successor : successors )
					{
						if ( !forbidMiddleLinks && !found && TrackableObjectUtils.frameDiff(successor, spot) < 2 )
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
				final T next = successors.iterator().next();
				if ( TrackableObjectUtils.frameDiff(spot, next) < 2 )
				{
					for ( final T predecessor : predecessors )
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
					for ( final T predecessor : predecessors )
					{
						if ( !forbidMiddleLinks && !found && TrackableObjectUtils.frameDiff(spot, predecessor) < 2 )
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
				boolean found = false;
				for ( final T predecessor : predecessors )
				{
					if ( !forbidMiddleLinks && !found && TrackableObjectUtils.frameDiff(spot, predecessor) < 2 )
					{
						found = true;
					}
					else
					{
						graph.removeEdge( predecessor, spot );
						links.add( makeLink( predecessor, spot ) );
					}
				}
				for ( final T successor : successors )
				{
					if ( !forbidMiddleLinks && !found && TrackableObjectUtils.frameDiff(successor, spot) < 2 )
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
			final Set< DefaultWeightedEdge > toRemove = new HashSet< DefaultWeightedEdge >();
			for ( final DefaultWeightedEdge edge : newEdges )
			{
				final T source = graph.getEdgeSource( edge );
				final T target = graph.getEdgeTarget( edge );
				if ( Math.abs( TrackableObjectUtils.frameDiff(source, target) ) > 1 )
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

		final ConnectivityInspector< T, DefaultWeightedEdge > connectivity = new ConnectivityInspector< T, DefaultWeightedEdge >( graph );
		final List< Set< T >> connectedSets = connectivity.connectedSets();
		final Collection< List< T >> branches = new HashSet< List< T > >( connectedSets.size() );
		final Comparator< T > comparator = TrackableObjectUtils.frameComparator();
		for ( final Set< T > set : connectedSets )
		{
			final List< T > branch = new ArrayList< T >( set.size() );
			branch.addAll( set );
			Collections.sort( branch, comparator );
			branches.add( branch );
		}

		final TrackBranchDecomposition<T> output = new TrackBranchDecomposition<T>();
		output.branches = branches;
		output.links = links;
		return output;

	}

	private static final <T extends TrackableObject> List< T > makeLink( final T spotA, final T spotB )
	{
		final List< T > link = new ArrayList< T >( 2 );
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
	public Collection< List< T >> getBranches()
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
	public Map< Integer, Collection< List< T >>> getBranchesPerTrack()
	{
		return branchesPerTrack;
	}

	/**
	 * Returns the links cut by this algorithm when splitting the model in
	 * linear, convex branches.
	 * <p>
	 * These links are returned as a collection of 2-elements list. If the
	 * instance was created with {@link #forbidMiddleLinks} sets to
	 * <code>true</code>, it is ensured that the first element of all links is
	 * the last spot of a branch, and the second element of this link is the
	 * first spot of another branch. Otherwise, a link cam target a spot within
	 * a branch.
	 *
	 * @return a collection of links as a 2-elements list.
	 */
	public Collection< List< T >> getLinks()
	{
		return links;
	}

	/**
	 * Returns the mapping of each source track ID to the links that were cut in
	 * it to split it in branches.
	 * <p>
	 * These links are returned as a collection of 2-elements list. If the
	 * instance was created with {@link #forbidMiddleLinks} sets to
	 * <code>true</code>, it is ensured that the first element of all links is
	 * the last spot of a branch, and the second element of this link is the
	 * first spot of another branch. Otherwise, a link can target a spot within
	 * a branch.
	 *
	 * @return the mapping of track IDs to the links.
	 */
	public Map< Integer, Collection< List< T >>> getLinksPerTrack()
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
	public static final class TrackBranchDecomposition<T>
	{
		/**
		 * Branches are returned as list of spot. It is ensured that the spots
		 * are ordered in the list by increasing frame number, and that two
		 * consecutive spot are separated by exactly one frame.
		 */
		public Collection< List< T >> branches;

		/**
		 * Links, as a collection of 2-elements list.
		 */
		public Collection< List< T >> links;
	}

}
