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
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;

public class ContinuousBranchesDecomposition implements Algorithm, Benchmark
{
	private static final String BASE_ERROR_MSG = "[ContinousBranchesDecomposition] ";

	private String errorMessage;

	private Collection< List< Spot >> branches;

	private Collection< List< Spot > > links;

	private Map< Integer, Collection< List< Spot >>> branchesPerTrack;

	private Map< Integer, Collection< List< Spot > >> linksPerTrack;

	private long processingTime;

	private final TrackModel tm;

	private final TimeDirectedNeighborIndex neighborIndex;

	public ContinuousBranchesDecomposition( final Model model )
	{
		this.tm = model.getTrackModel();
		this.neighborIndex = tm.getDirectedNeighborIndex();
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

		branches = new ArrayList< List< Spot >>();
		branchesPerTrack = new HashMap< Integer, Collection< List< Spot >>>();
		links = new ArrayList< List< Spot > >();
		linksPerTrack = new HashMap< Integer, Collection< List< Spot > >>();
		for ( final Integer trackID : trackIDs )
		{
			final TrackBranchDecomposition branchDecomposition = processTrack( trackID, tm, neighborIndex );

			branchesPerTrack.put( trackID, branchDecomposition.branches );
			linksPerTrack.put( trackID, branchDecomposition.links );

			branches.addAll( branchDecomposition.branches );
			links.addAll( branchDecomposition.links );
		}

		final long endT = System.currentTimeMillis();
		processingTime = endT - startT;

		return true;

	}

	public static final TrackBranchDecomposition processTrack( final Integer trackID, final TrackModel tm, final TimeDirectedNeighborIndex neighborIndex )
	{
		final Set< Spot > allSpots = tm.trackSpots( trackID );
		final Set< DefaultWeightedEdge > allEdges = tm.trackEdges( trackID );
		final SimpleGraph< Spot, DefaultWeightedEdge > graph = new SimpleGraph< Spot, DefaultWeightedEdge >( DefaultWeightedEdge.class );

		for ( final Spot spot : allSpots )
		{
			graph.addVertex( spot );
		}
		for ( final DefaultWeightedEdge edge : allEdges )
		{
			graph.addEdge( tm.getEdgeSource( edge ), tm.getEdgeTarget( edge ) );
		}

		final Collection< List< Spot >> links = new HashSet< List< Spot > >();
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
					if ( !found && successor.diffTo( spot, Spot.FRAME ) < 2 )
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
					if ( !found && spot.diffTo( predecessor, Spot.FRAME ) < 2 )
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
						if ( !found && successor.diffTo( spot, Spot.FRAME ) < 2 )
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
						if ( !found && spot.diffTo( predecessor, Spot.FRAME ) < 2 )
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
				for ( final Spot predecessor : predecessors )
				{
					if ( !found && spot.diffTo( predecessor, Spot.FRAME ) < 2 )
					{
						found = true;
					}
					else
					{
						graph.removeEdge( predecessor, spot );
						links.add( makeLink( predecessor, spot ) );
					}
				}
				for ( final Spot successor : successors )
				{
					if ( !found && successor.diffTo( spot, Spot.FRAME ) < 2 )
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

		final Set< DefaultWeightedEdge > newEdges = graph.edgeSet();
		final Set< DefaultWeightedEdge > toRemove = new HashSet< DefaultWeightedEdge >();
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

		/*
		 * Output
		 */

		final ConnectivityInspector< Spot, DefaultWeightedEdge > connectivity = new ConnectivityInspector< Spot, DefaultWeightedEdge >( graph );
		final List< Set< Spot >> connectedSets = connectivity.connectedSets();
		final Collection< List< Spot >> branches = new HashSet< List< Spot > >( connectedSets.size() );
		final Comparator< Spot > comparator = Spot.frameComparator;
		for ( final Set< Spot > set : connectedSets )
		{
			final List< Spot > branch = new ArrayList< Spot >( set.size() );
			branch.addAll( set );
			Collections.sort( branch, comparator );
			branches.add( branch );
		}

		final TrackBranchDecomposition output = new TrackBranchDecomposition();
		output.branches = branches;
		output.links = links;
		return output;

	}

	private static final List< Spot > makeLink( final Spot spotA, final Spot spotB )
	{
		final List< Spot > link = new ArrayList< Spot >( 2 );
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
	 * linear, consecutive branches.
	 * <p>
	 * These links are returned as a collection of 2-elements array.
	 *
	 * @return a map.
	 */
	public Collection< List< Spot >> getLinks()
	{
		return links;
	}

	/**
	 * Returns the mapping of each source track ID to the links that were cut in
	 * it to split it in branches.
	 * <p>
	 * These links are returned as 2-elements arrays..
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

	public static final class TrackBranchDecomposition
	{
		public Collection< List< Spot >> branches;

		public Collection< List< Spot >> links;

	}

}
