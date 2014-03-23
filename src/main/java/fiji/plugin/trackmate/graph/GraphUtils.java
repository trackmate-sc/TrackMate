package fiji.plugin.trackmate.graph;


import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jgrapht.VertexFactory;
import org.jgrapht.alg.DirectedNeighborIndex;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.tracking.TrackableObject;
import fiji.plugin.trackmate.tracking.TrackableObjectUtils;

public class GraphUtils {


	/**
	 * @return a pretty-print string representation of a {@link TrackModel}, as long it is 
	 * a tree (each spot must not have more than one predecessor).
	 * @throws IllegalArgumentException if the given graph is not a tree.
	 */
	public static final <T extends TrackableObject> String toString(final TrackModel<T> model) {
		/*
		 * Get directed cache
		 */
		final TimeDirectedNeighborIndex<T> cache = model.getDirectedNeighborIndex();
		
		/*
		 * Check input
		 */
		if (!isTree(model, cache)) {
			throw new IllegalArgumentException("toString cannot be applied to graphs that are not trees (each vertex must have at most one predecessor).");
		}
		
		/*
		 * Get column widths
		 */
		final Map<T, Integer> widths = cumulativeBranchWidth(model);
		
		/*
		 * By the way we compute the largest spot name
		 */
		int largestName = 0;
		for (final T spot : model.vertexSet()) {
			if (spot.getName().length() > largestName) {
				largestName = spot.getName().length();
			}
		}
		largestName += 2;

		/*
		 * Find how many different frames we have
		 */
		final TreeSet<Integer> frames = new TreeSet<Integer>();
		for (final TrackableObject spot : model.vertexSet()) {
			frames.add(spot.frame());
		}
		final int nframes = frames.size();


		/*
		 * Build string, one StringBuilder per frame
		 */
		final HashMap<Integer, StringBuilder> strings = new HashMap<Integer, StringBuilder>(nframes);
		for (final Integer frame : frames) {
			strings.put(frame, new StringBuilder());
		}

		final HashMap<Integer, StringBuilder> below = new HashMap<Integer, StringBuilder>(nframes);
		for (final Integer frame : frames) {
			below.put(frame, new StringBuilder());
		}

		/*
		 * Keep track of where the carret is for each spot
		 */
		final Map<T, Integer> carretPos = new HashMap<T, Integer>(model.vertexSet().size()); 

		/*
		 * Comparator to have spots order by name
		 */
		final Comparator<T> comparator = TrackableObjectUtils.nameComparator();
		
		/*
		 * Let's go!
		 */

		for (final Integer trackID : model.trackIDs(true)) {
			
			/*
			 *  Get the 'first' spot for an iterator that starts there
			 */
			final Set<T> track = model.trackSpots(trackID);
			final Iterator<T> it = track.iterator();
			T first = it.next();
			for (final T spot : track) {
				if (TrackableObjectUtils.frameDiff(first, spot) > 0) {
					first = spot;
				}
			}

			/*
			 * First, fill the linesBelow with spaces
			 */
			for (final Integer frame : frames) {
				final int columnWidth = widths.get(first);
				below.get(frame).append(makeSpaces(columnWidth*largestName));
			}
			
			/*
			 * Iterate down the tree
			 */
			final SortedDepthFirstIterator<T,DefaultWeightedEdge> iterator = model.getSortedDepthFirstIterator(first, comparator, true);
			while (iterator.hasNext()) {

				final T spot = iterator.next();
				final int frame = spot.frame();
				final boolean isLeaf = cache.successorsOf(spot).size() == 0;

				final int columnWidth = widths.get(spot);
				final String str = spot.getName();
				final int nprespaces = largestName/2 - str.length()/2;
				strings.get(frame).append(makeSpaces(columnWidth / 2 * largestName));
				strings.get(frame).append(makeSpaces(nprespaces));
				strings.get(frame).append(str);
				// Store bar position - deal with bars below
				final int currentBranchingPosition = strings.get(frame).length() - str.length()/2;
				carretPos.put(spot, currentBranchingPosition);
				// Resume filling the branch
				strings.get(frame).append(makeSpaces(largestName - nprespaces - str.length()));
				strings.get(frame).append(makeSpaces( (columnWidth*largestName) - (columnWidth/2*largestName) - largestName));

				// is leaf? then we fill all the columns below
				if (isLeaf) {
					final SortedSet<Integer> framesToFill = frames.tailSet(frame, false);
					for (final Integer subsequentFrame : framesToFill) {
						strings.get(subsequentFrame).append(makeSpaces(columnWidth * largestName));
					}
				} else {
					// Is there an empty slot below? Like when a link jumps above several frames?
					final Set<T> successors = cache.successorsOf(spot);
					for (final T successor : successors) {
						if (TrackableObjectUtils.frameDiff(successor, spot) > 1) {
							for (int subFrame = successor.frame(); subFrame <= successor.frame(); subFrame++) {
								strings.get(subFrame-1).append(makeSpaces(columnWidth * largestName));
							}
						}
					}
				}
				
				

			} // Finished iterating over spot of the track
			
			// Fill remainder with spaces
			
			for (final Integer frame : frames) {
				final int columnWidth = widths.get(first);
				final StringBuilder sb = strings.get(frame);
				final int pos = sb.length();
				final int nspaces = columnWidth * largestName - pos;
				if (nspaces > 0) {
					sb.append(makeSpaces(nspaces));
				}
			}

		} // Finished iterating over the track
		
		
		/*
		 * Second iteration over edges
		 */
		
		final Set<DefaultWeightedEdge> edges = model.edgeSet();
		for (final DefaultWeightedEdge edge : edges) {
			
			final T source = model.getEdgeSource(edge);
			final T target = model.getEdgeTarget(edge);
			
			final int sourceCarret = carretPos.get(source) - 1;
			final int targetCarret = carretPos.get(target) - 1;
			
			final int sourceFrame = source.frame();
			final int targetFrame = target.frame();
			
			for (int frame = sourceFrame; frame < targetFrame; frame++) {
				below.get(frame).setCharAt(sourceCarret, '|');
			}
			for (int frame = sourceFrame+1; frame < targetFrame; frame++) {
				strings.get(frame).setCharAt(sourceCarret, '|');
			}
			
			if (cache.successorsOf(source).size() > 1) {
				// We have branching
				final int minC = Math.min(sourceCarret, targetCarret);
				final int maxC = Math.max(sourceCarret, targetCarret);
				final StringBuilder sb = below.get(sourceFrame);
				for (int i = minC+1; i < maxC; i++) {
					if (sb.charAt(i) == ' ') {
						sb.setCharAt(i, '-');
					}
				}
				sb.setCharAt(minC, '+');
				sb.setCharAt(maxC, '+');
			}
		}
		

		/*
		 * Concatenate strings
		 */

		final StringBuilder finalString = new StringBuilder();
		for (final Integer frame : frames) {

			finalString.append(strings.get(frame).toString());
			finalString.append('\n');
			finalString.append(below.get(frame).toString());
			finalString.append('\n');
		}


		return finalString.toString();

	}
	
	
	
	
	public static final  <T extends TrackableObject> boolean  isTree(final TrackModel<T> model, final TimeDirectedNeighborIndex<T> cache) {
		return isTree(model.vertexSet(), cache);
	}
	

	
	public static final <T extends TrackableObject>  boolean isTree(final Iterable<T> spots, final TimeDirectedNeighborIndex<T> cache) {
		for (final T spot : spots) {
			if (cache.predecessorsOf(spot).size() > 1) {
				return false;
			}
		}
		return true;
	}
	
	
	
	
	public static final <T extends TrackableObject> Map<T, Integer> cumulativeBranchWidth(final TrackModel<T> model) {

		/*
		 * Elements stored:
		 * 	0. cumsum of leaf
		 */
		final VertexFactory<int[]> factory = new VertexFactory<int[]>() {
			@Override
			public int[] createVertex() {
				return new int[1];
			}
		};

		/*
		 * Build isleaf tree
		 */

		final TimeDirectedNeighborIndex<T> cache = model.getDirectedNeighborIndex();

		final Function1<T, int[]> isLeafFun = new Function1<T, int[]>() {
			@Override
			public void compute(final T input, final int[] output) {
				if (cache.successorsOf(input).size() == 0) {
					output[0] = 1;
				} else {
					output[0] = 0;
				}
			}
		};


		final Map<T, int[]> mappings = new HashMap<T, int[]>();
		final SimpleDirectedWeightedGraph<int[], DefaultWeightedEdge> leafTree = model.copy(factory, isLeafFun, mappings);

		/*
		 * Find root spots & first spots
		 * Roots are spots without any ancestors. There might be more than one per track.
		 * First spots are the first root found in a track. There is only one per track.
		 * 
		 * By the way we compute the largest spot name
		 */

		final Set<T> roots = new HashSet<T>(model.nTracks(false)); // approx
		final Set<T> firsts = new HashSet<T>(model.nTracks(false)); // exact
		final Set<Integer> ids = model.trackIDs(false);
		for (final Integer id : ids) {
			final Set<T> track = model.trackSpots(id);
			boolean firstFound = false;
			for (final T spot : track) {

				if (cache.predecessorsOf(spot).size() == 0) {
					if (!firstFound) {
						firsts.add(spot);
					}
					roots.add(spot);
					firstFound = true;
				}
			}
		}

		/*
		 * Build cumsum value
		 */

		final Function2<int[], int[]> cumsumFun = new Function2<int[], int[]>() {
			@Override
			public void compute(final int[] input1, final int[] input2, final int[] output) {
				output[0] = input1[0] + input2[0];
			}
		};

		final RecursiveCumSum<int[], DefaultWeightedEdge> cumsum = new RecursiveCumSum<int[], DefaultWeightedEdge>(leafTree, cumsumFun);
		for(final TrackableObject root : firsts) {
			final int[] current = mappings.get(root);
			cumsum.apply(current);
		}
		
		/*
		 * Convert to map of spot vs integer 
		 */
		final Map<T, Integer> widths = new HashMap<T, Integer>();
		for (final T spot : model.vertexSet()) {
			widths.put(spot, mappings.get(spot)[0]);
		}
		
		return widths;
	}
	
	
	

	private static char[] makeSpaces(final int width) {
		return makeChars(width, ' ');
	}


	private static char[] makeChars(final int width, final char c) {
		final char[] chars = new char[width];
		Arrays.fill(chars, c);
		return chars;
	}


	/**
	 * @return true only if the given model is a tree; that is: every spot has one or less
	 * predecessors.
	 */
	public static final <T extends TrackableObject>  Set<T> getSibblings(final DirectedNeighborIndex<T, DefaultWeightedEdge> cache, final T spot) {
		final HashSet<T> sibblings = new HashSet<T>();
		final Set<T> predecessors = cache.predecessorsOf(spot);
		for (final T predecessor : predecessors) {
			sibblings.addAll(cache.successorsOf(predecessor));
		}
		return sibblings;
	}


}
