package fiji.plugin.trackmate.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.traverse.GraphIterator;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;

public class ContinousBranchesDecomposition implements Algorithm, Benchmark
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

	public ContinousBranchesDecomposition( final Model model )
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

		/*
		 * Result holders.
		 */

		final Collection< List< Spot >> branches = new ArrayList< List< Spot > >();
		final Collection< List< Spot > > links = new HashSet< List< Spot > >();

		final Set< Spot > starts = new HashSet< Spot >();
		final Set< Spot > trackSpots = tm.trackSpots( trackID );

		/*
		 * Identify leaves Add the branch nuclei of the graph, or 'leaves'. This
		 * is not exactly leaves: we want to find the spots that have no
		 * predecessors, regardless of how any successors they have.
		 */
		for ( final Spot spot : trackSpots )
		{
			if ( neighborIndex.predecessorsOf( spot ).size() == 0 )
			{
				starts.add( spot );
			}
		}

		/*
		 * Nucleate a branch for each leaf. First pass: we do not care for gaps
		 * yet. Or just a bit: if there is a gap near fusion or fission, we try
		 * to be sensible as where to put the lonely spot, so as to avoid having
		 * too many branches made of one spot.
		 */

		final Set< Spot > visited = new HashSet< Spot >();
		do
		{
			final Spot start = starts.iterator().next();
			starts.remove( start );
			visited.add( start );

			/*
			 * Initiate the branch.
			 */

			final List< Spot > branch = new ArrayList< Spot >();
			// Ensures we always move forward in time.
			final GraphIterator< Spot, DefaultWeightedEdge > it = tm.getDepthFirstIterator( start, true );
			branch.add( it.next() ); // is start.

			/*
			 * If our start point have several successors, we must check it
			 * here, because we want to create start points for its other
			 * successors as well.
			 */
			final Set< Spot > startSuccessors = neighborIndex.successorsOf( start );
			if ( startSuccessors.size() > 1 )
			{
				starts.addAll( startSuccessors );
				for ( final Spot successor : startSuccessors )
				{
					final List< Spot > link = new ArrayList< Spot >( 2 );
					link.add( start );
					link.add( successor );
					links.add( link );
				}
				branches.add( branch );
				// Early stop.
				continue;
			}

			Spot previous = start;

			while ( it.hasNext() )
			{
				final Spot spot = it.next();

				starts.remove( spot );
				if ( visited.contains( spot ) )
				{
					final List< Spot > link = new ArrayList< Spot >( 2 );
					link.add( previous );
					link.add( spot );
					links.add( link );
					break;
				}

				final Set< Spot > successors = new HashSet< Spot >( neighborIndex.successorsOf( spot ) );
				final Set< Spot > predecessors = new HashSet< Spot >( neighborIndex.predecessorsOf( spot ) );

				/*
				 * SPLIT
				 */

				if ( successors.size() > 1 )
				{
					visited.add( spot );
					branch.add( spot );
					for ( final Spot successor : successors )
					{
						final List< Spot > link = new ArrayList< Spot >( 2 );
						link.add( spot );
						link.add( successor );
						links.add( link );

						starts.add( successor );
					}
					break;
				}

				/*
				 * MERGE
				 */

				if ( predecessors.size() > 1 )
				{
					visited.add( spot );
					starts.add( spot );
					for ( final Spot predecessor : predecessors )
					{
						if ( neighborIndex.successorsOf( predecessor ).size() > 1 )
						{
							continue;
						}
						final List< Spot > link = new ArrayList< Spot >( 2 );
						link.add( predecessor );
						link.add( spot );
						links.add( link );
					}
					break;
				}

				/*
				 * SPECIAL POINT
				 */

				if ( successors.size() > 1 && predecessors.size() > 1 )
				{
					visited.add( spot );
					final List< Spot > smallBranch = new ArrayList< Spot >( 1 );
					smallBranch.add( spot );

					for ( final Spot successor : successors )
					{
						starts.add( successor );
					}

					for ( final Spot successor : successors )
					{
						final List< Spot > link = new ArrayList< Spot >( 2 );
						link.add( spot );
						link.add( successor );
						links.add( link );
					}
					for ( final Spot predecessor : predecessors )
					{
						final List< Spot > link = new ArrayList< Spot >( 2 );
						link.add( predecessor );
						link.add( spot );
						links.add( link );
					}
					break;
				}

				/*
				 * LEAF
				 */

				if ( successors.size() == 0 )
				{
					// We have reached a leaf.
					branch.add( spot );
					starts.remove( spot );
					break;
				}

				/*
				 * Contiguous spot; we can add it to the current branch.
				 */
				if ( !visited.contains( spot ) )
				{
					branch.add( spot );
				}
				previous = spot;
			}

			/*
			 * Remove the visited leaf, now that it is done.
			 */
			starts.remove( start );

			/*
			 * Add the last visited spot to the visited list, so that we are
			 * sure not to create any faulty edge in case a track finishes with
			 * a merge point.
			 */
			visited.add( previous );

			/*
			 * Add finished branch.
			 */
			branches.add( branch );
		}
		while ( !starts.isEmpty() );

		/*
		 * 2nd pass: Cut along gaps.
		 */

		final Collection< List< Spot >> refinedBranches = new ArrayList< List< Spot > >( branches.size() );
		for ( final List< Spot > branch : branches )
		{
			final Iterator< Spot > it = branch.iterator();
			Spot previous = it.next();
			List< Spot > newBranch = new ArrayList< Spot >();
			newBranch.add( previous );

			while ( it.hasNext() )
			{
				final Spot spot = it.next();
				if ( spot.diffTo( previous, Spot.FRAME ) > 1 )
				{
					refinedBranches.add( newBranch );

					final List< Spot > link = new ArrayList< Spot >( 2 );
					link.add( previous );
					link.add( spot );
					links.add( link );

					newBranch = new ArrayList< Spot >();
					newBranch.add( spot );
					if ( it.hasNext() )
					{
						previous = spot;
						continue;
					}
					else
					{
						break;
					}
				}
				newBranch.add( spot );
				previous = spot;
			}
			refinedBranches.add( newBranch );
		}

		final TrackBranchDecomposition output = new TrackBranchDecomposition();
		output.branches = refinedBranches;
		output.links = links;
		return output;

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
