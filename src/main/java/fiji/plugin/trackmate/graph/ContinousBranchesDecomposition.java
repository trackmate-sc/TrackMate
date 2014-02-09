package fiji.plugin.trackmate.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.traverse.GraphIterator;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;

public class ContinousBranchesDecomposition implements Algorithm, Benchmark
{

	private static final String BASE_ERROR_MSG = "[ContinousBranchesDecomposition]";

	private String errorMessage;

	private Collection< List< Spot >> branches;

	private Collection< Spot[] > links;

	private Map< Integer, Collection< List< Spot >>> branchesPerTrack;

	private Map< Integer, Collection< Spot[] >> linksPerTrack;

	private long processingTime;

	private final TrackModel tm;

	public ContinousBranchesDecomposition( final Model model )
	{
		this.tm = model.getTrackModel();
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
		branchesPerTrack = new HashMap< Integer, Collection<List< Spot >>>();
		links = new ArrayList<Spot[]>();
		linksPerTrack = new HashMap< Integer, Collection<Spot[]>>();
		for ( final Integer trackID : trackIDs )
		{
			final TrackBranchDecomposition branchDecomposition = processTrack( trackID, tm );

			branchesPerTrack.put( trackID, branchDecomposition.branches );
			linksPerTrack.put( trackID, branchDecomposition.links );

			branches.addAll( branchDecomposition.branches );
			links.addAll( branchDecomposition.links );
		}

		final long endT = System.currentTimeMillis();
		processingTime = endT - startT;

		return true;

	}

	public static final TrackBranchDecomposition processTrack( final Integer trackID, final TrackModel tm )
	{

		/*
		 * Result holders.
		 */

		final Collection< List< Spot >> branches = new ArrayList< List< Spot > >();
		final Collection< Spot[] > links = new ArrayList< Spot[] >();

		/*
		 * Sort by frame
		 */

		final Comparator< Spot > comparator = Spot.frameComparator;

		/*
		 * Identify leaves
		 */

		final List< Spot > leaves = new ArrayList< Spot >();
		final Set< Spot > trackSpots = tm.trackSpots( trackID );

		// Add at least the first spot of the track
		final TreeSet< Spot > sorted = new TreeSet< Spot >( comparator );
		sorted.addAll( trackSpots );
		leaves.add( sorted.first() );

		// Add the leaf points of the graph.
		for ( final Spot spot : trackSpots )
		{
			if ( tm.edgesOf( spot ).size() == 1 )
			{
				if ( !leaves.contains( spot ) )
				{
					leaves.add( spot );
				}
			}
		}
		Collections.sort( leaves, comparator );

		/*
		 * Special case: we may have to attach spot to a what we think is a
		 * leaf in the case of very exotic branches. Namely: if we have a
		 * gap just before a split, we want to attach the lonely split point
		 * to one of the successor branch. Or if we have a gap just after a
		 * fusion, we want to attach the merging point to one of the
		 * ancestor. We store this weird mapping here.
		 */
		final Map< Spot, Spot > attachTo = new HashMap< Spot, Spot >();

		/*
		 * Nucleate a branch for each leaf. First pass: we do not care for gaps
		 * yet. Or just a bit: if there is a gap near fusion or fission, we try
		 * to be sensible as where to put the lonely spot, so as to avoid having
		 * too many branches made of one spot.
		 */

		final Set< Spot > visited = new HashSet< Spot >();
		do
		{
			final Spot start = leaves.iterator().next();
			leaves.remove( start );

			/*
			 * Initiate the branch.
			 */

			final List< Spot > branch = new ArrayList< Spot >();
			// Ensures we always move forward in time.
			final GraphIterator< Spot, DefaultWeightedEdge > it = tm.getDepthFirstIterator( start, true );

			/*
			 * Check if we have a spot to attach to this branch nucleus.
			 */

			final Spot attached = attachTo.get( start );
			if ( null != attached )
			{
				if ( start.diffTo( attached, Spot.FRAME ) > 0 )
				{
					// attached comes BEFORE
					branch.add( attached );
					branch.add( it.next() ); // is start;
				}
				else
				{
					// attached comes AFTER
					branch.add( it.next() ); // is start;
					branch.add( attached );
				}
			}
			else
			{
				branch.add( it.next() ); // is start.
			}

			Spot previous;

			/*
			 * Special case: Is out starting point a branching point? We
			 * must detect this now.
			 */

			final Set< Spot > startSuccessors = successorsOf( start, tm );
			if ( startSuccessors.size() > 1 )
			{
				final Spot next = it.next();
				startSuccessors.remove( next );
				branch.add( next ); // will belong to the main branch
				startSuccessors.remove( next );
				for ( final Spot successor : startSuccessors )
				{
					// Will nucleate their own branch
					if ( !leaves.contains( successor ) )
					{
						leaves.add( successor );
					}
					// Store links with successors.
					links.add( new Spot[] { start, successor } );
				}
				Collections.sort( leaves, comparator );
				previous = next;
			}
			else
			{
				previous = start;
			}

			while ( it.hasNext() )
			{
				final Spot spot = it.next();

				/*
				 * Splits or merges.
				 */
				if ( tm.edgesOf( spot ).size() > 2 )
				{
					// We have a fusion or a split
					leaves.remove( previous );
					if ( visited.contains( spot ) )
					{
						break;
					}
					visited.add( spot );

					// Inspect neighbors
					final Set< Spot > successors = successorsOf( spot, tm );
					final Set< Spot > predecessors = predecessorsOf( spot, tm );

					// Determine whether we have a fusion or something else.
					if ( predecessors.size() > 1 && successors.size() <= 1 )
					{
						/*
						 * FUSION
						 */

						if ( ( successors.size() == 1 ) && ( Math.abs( spot.diffTo( successors.iterator().next(), Spot.FRAME ) ) < 2 ) )
						{
							// No gap. Everything is fine, and we will
							// process this later.
							if ( !leaves.contains( spot ) )
							{
								leaves.add( spot );
							}
							for ( final Spot predecessor : predecessors )
							{
								links.add( new Spot[] { predecessor, spot } );
							}
						}
						else
						{
							branch.add( spot );
							if ( successors.size() == 1 )
							{
								final Spot next = successors.iterator().next();
								if ( !leaves.contains( next ) )
								{
									leaves.add( next );
								}
								links.add( new Spot[] { spot, next } );
							}

							predecessors.remove( previous );
							for ( final Spot predecessor : predecessors )
							{
								links.add( new Spot[] { predecessor, spot } );
							}
						}

					}
					else if ( predecessors.size() <= 1 && successors.size() > 1 )
					{

						/*
						 * SPLIT
						 */

						for ( final Spot successor : successors )
						{
							if ( !leaves.contains( successor ) )
							{
								leaves.add( successor );
							}
						}

						// Split point get to the mother branch, if they do
						// not make a gap.
						if ( Math.abs( spot.diffTo( previous, Spot.FRAME ) ) < 2 )
						{
							// No gap. Everything is fine and we attach the
							// split point to the current branch.
							branch.add( spot );
						}
						else
						{
							/*
							 * A gap: we attach it on one of the successors,
							 * but we will do it later. And we will do it
							 * only if the elected successor does not have a
							 * gap.
							 */
							boolean found = false;
							for ( final Spot target : successors )
							{
								if ( target.diffTo( spot, Spot.FRAME ) < 2 )
								{
									attachTo.put( target, spot );
									links.add( new Spot[] { previous, spot } );
									successors.remove( target );
									found = true;
									break;
								}
							}
							if ( !found )
							{
								/*
								 * I could not find a successor with no
								 * gaps. We put this fusion point on a
								 * lonely branch.
								 */
								final List< Spot > smallBranch = new ArrayList< Spot >( 1 );
								smallBranch.add( spot );
								branches.add( smallBranch );
							}

						}
						for ( final Spot successor : successors )
						{
							links.add( new Spot[] { spot, successor } );
						}
					}
					else
					{
						for ( final Spot successor : successors )
						{
							if ( !leaves.contains( successor ) )
							{
								leaves.add( successor );
							}
						}
						predecessors.remove( previous );
						for ( final Spot predecessor : predecessors )
						{
							if ( !leaves.contains( predecessor ) )
							{
								leaves.add( predecessor );
							}
						}
						// Check if we have a gap at this point.
						if ( Math.abs( spot.diffTo( previous, Spot.FRAME ) ) < 2 )
						{
							// No. Life is easy.
							branch.add( spot );
						}
						else
						{
							// Yes. Shoot. Finish this branch and memorize
							// the link.
							links.add( new Spot[] { previous, spot } );
							// Attach the current spot to a future branch.
							final Spot target = successors.iterator().next();
							attachTo.put( target, spot );
							successors.remove( target );
						}
						for ( final Spot successor : successors )
						{
							links.add( new Spot[] { spot, successor } );
						}
						for ( final Spot predecessor : predecessors )
						{
							links.add( new Spot[] { predecessor, spot } );
						}
					}
					Collections.sort( leaves, comparator );
					break;
				}


				/*
				 * Leaf
				 */
				if ( tm.edgesOf( spot ).size() == 1 )
				{
					// We have reached a leaf.
					branch.add( spot );
					leaves.remove( spot );
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
			 * Add the last visited spot to the visited list, so that we are
			 * sure not to create any faulty edge in case a track finishes
			 * with a merge point.
			 */
			visited.add( previous );

			if ( branch.size() > 0 )
			{
				// Ignore singletons.
				branches.add( branch );
			}
		}
		while ( !leaves.isEmpty() );

		System.out.println( "At this stage, we have:" );// DEBUG
		System.out.println( "Branches:" );// DEBUG
		for ( final List< Spot > branch : branches )
		{
			System.out.println( "  " + branch );// DEBUG
		}
		System.out.println( "Links:" );// DEBUG
		for ( final Spot[] link : links )
		{
			System.out.println( "  " + link[ 0 ] + "-" + link[ 1 ] );// DEBUG
		}

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
					links.add( new Spot[] { previous, spot } );
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
	public Collection< Spot[] > getLinks()
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
	public Map< Integer, Collection< Spot[] >> getLinksPerTrack()
	{
		return linksPerTrack;
	}

	/*
	 * STATIC METHODS
	 */

	private static final Set< Spot > successorsOf( final Spot spot, final TrackModel tm )
	{
		final Set< DefaultWeightedEdge > edges = tm.edgesOf( spot );
		final Set< Spot > successors = new HashSet< Spot >( edges.size() );
		for ( final DefaultWeightedEdge edge : edges )
		{
			Spot other = tm.getEdgeSource( edge );
			if ( other.equals( spot ) )
			{
				other = tm.getEdgeTarget( edge );
			}
			if ( other.diffTo( spot, Spot.FRAME ) > 0 )
			{
				successors.add( other );
			}
		}
		return successors;
	}

	private static final Set< Spot > predecessorsOf( final Spot spot, final TrackModel tm )
	{
		final Set< DefaultWeightedEdge > edges = tm.edgesOf( spot );
		final Set< Spot > successors = new HashSet< Spot >( edges.size() );
		for ( final DefaultWeightedEdge edge : edges )
		{
			Spot other = tm.getEdgeSource( edge );
			if ( other.equals( spot ) )
			{
				other = tm.getEdgeTarget( edge );
			}
			if ( other.diffTo( spot, Spot.FRAME ) < 0 )
			{
				successors.add( other );
			}
		}
		return successors;
	}

	public static final class TrackBranchDecomposition
	{
		public Collection< List< Spot >> branches;

		public Collection< Spot[] > links;

	}

}
