/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
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
import java.util.function.Supplier;

import org.jgrapht.alg.util.NeighborCache;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;

public class GraphUtils
{

	/**
	 * Pretty-prints a model.
	 * 
	 * @param model
	 *            the model.
	 * @return a pretty-print string representation of a {@link TrackModel}, as
	 *         long it is a tree (each spot must not have more than one
	 *         predecessor).
	 * @throws IllegalArgumentException
	 *             if the given graph is not a tree.
	 */
	public static final String toString( final TrackModel model )
	{
		/*
		 * Get directed cache
		 */
		final TimeDirectedNeighborIndex cache = model.getDirectedNeighborIndex();

		/*
		 * Check input
		 */
		if ( !isTree( model, cache ) )
			throw new IllegalArgumentException( "toString cannot be applied to graphs that are not trees (each vertex must have at most one predecessor)." );

		/*
		 * Get column widths
		 */
		final Map< Spot, Integer > widths = cumulativeBranchWidth( model );

		/*
		 * By the way we compute the largest spot name
		 */
		int largestName = 0;
		for ( final Spot spot : model.vertexSet() )
		{
			if ( spot.getName().length() > largestName )
				largestName = spot.getName().length();
		}
		largestName += 2;

		/*
		 * Find how many different frames we have
		 */
		final TreeSet< Integer > frames = new TreeSet<>();
		for ( final Spot spot : model.vertexSet() )
			frames.add( spot.getFeature( Spot.FRAME ).intValue() );

		final int nframes = frames.size();

		/*
		 * Build string, one StringBuilder per frame
		 */
		final HashMap< Integer, StringBuilder > strings = new HashMap<>( nframes );
		for ( final Integer frame : frames )
			strings.put( frame, new StringBuilder() );

		final HashMap< Integer, StringBuilder > below = new HashMap<>( nframes );
		for ( final Integer frame : frames )
			below.put( frame, new StringBuilder() );

		/*
		 * Keep track of where the carret is for each spot
		 */
		final Map< Spot, Integer > carretPos = new HashMap<>( model.vertexSet().size() );

		/*
		 * Comparator to have spots order by name
		 */
		final Comparator< Spot > comparator = Spot.nameComparator;

		/*
		 * Let's go!
		 */

		for ( final Integer trackID : model.trackIDs( true ) )
		{

			/*
			 * Get the 'first' spot for an iterator that starts there
			 */
			final Set< Spot > track = model.trackSpots( trackID );
			final Iterator< Spot > it = track.iterator();
			Spot first = it.next();
			for ( final Spot spot : track )
			{
				if ( first.diffTo( spot, Spot.FRAME ) > 0 )
					first = spot;
			}

			/*
			 * First, fill the linesBelow with spaces
			 */
			for ( final Integer frame : frames )
			{
				final int columnWidth = widths.get( first );
				below.get( frame ).append( makeSpaces( columnWidth * largestName ) );
			}

			/*
			 * Iterate down the tree
			 */
			final SortedDepthFirstIterator< Spot, DefaultWeightedEdge > iterator = model.getSortedDepthFirstIterator( first, comparator, true );
			while ( iterator.hasNext() )
			{

				final Spot spot = iterator.next();
				final int frame = spot.getFeature( Spot.FRAME ).intValue();
				final boolean isLeaf = cache.successorsOf( spot ).size() == 0;

				final int columnWidth = widths.get( spot );
				final String str = spot.getName();
				final int nprespaces = largestName / 2 - str.length() / 2;
				strings.get( frame ).append( makeSpaces( columnWidth / 2 * largestName ) );
				strings.get( frame ).append( makeSpaces( nprespaces ) );
				strings.get( frame ).append( str );
				// Store bar position - deal with bars below
				final int currentBranchingPosition = strings.get( frame ).length() - str.length() / 2;
				carretPos.put( spot, currentBranchingPosition );
				// Resume filling the branch
				strings.get( frame ).append( makeSpaces( largestName - nprespaces - str.length() ) );
				strings.get( frame ).append( makeSpaces( ( columnWidth * largestName ) - ( columnWidth / 2 * largestName ) - largestName ) );

				// is leaf? then we fill all the columns below
				if ( isLeaf )
				{
					final SortedSet< Integer > framesToFill = frames.tailSet( frame, false );
					for ( final Integer subsequentFrame : framesToFill )
						strings.get( subsequentFrame ).append( makeSpaces( columnWidth * largestName ) );
				}
				else
				{
					// Is there an empty slot below? Like when a link jumps
					// above several frames?
					final Set< Spot > successors = cache.successorsOf( spot );
					for ( final Spot successor : successors )
					{
						if ( successor.diffTo( spot, Spot.FRAME ) > 1 )
						{
							for ( int subFrame = successor.getFeature( Spot.FRAME ).intValue(); subFrame <= successor.getFeature( Spot.FRAME ).intValue(); subFrame++ )
								strings.get( subFrame - 1 ).append( makeSpaces( columnWidth * largestName ) );
						}
					}
				}

			} // Finished iterating over spot of the track

			// Fill remainder with spaces

			for ( final Integer frame : frames )
			{
				final int columnWidth = widths.get( first );
				final StringBuilder sb = strings.get( frame );
				final int pos = sb.length();
				final int nspaces = columnWidth * largestName - pos;
				if ( nspaces > 0 )
					sb.append( makeSpaces( nspaces ) );
			}

		} // Finished iterating over the track

		/*
		 * Second iteration over edges
		 */

		final Set< DefaultWeightedEdge > edges = model.edgeSet();
		for ( final DefaultWeightedEdge edge : edges )
		{

			final Spot source = model.getEdgeSource( edge );
			final Spot target = model.getEdgeTarget( edge );

			final int sourceCarret = carretPos.get( source ) - 1;
			final int targetCarret = carretPos.get( target ) - 1;

			final int sourceFrame = source.getFeature( Spot.FRAME ).intValue();
			final int targetFrame = target.getFeature( Spot.FRAME ).intValue();

			for ( int frame = sourceFrame; frame < targetFrame; frame++ )
			{
				below.get( frame ).setCharAt( sourceCarret, '|' );
			}
			for ( int frame = sourceFrame + 1; frame < targetFrame; frame++ )
			{
				strings.get( frame ).setCharAt( sourceCarret, '|' );
			}

			if ( cache.successorsOf( source ).size() > 1 )
			{
				// We have branching
				final int minC = Math.min( sourceCarret, targetCarret );
				final int maxC = Math.max( sourceCarret, targetCarret );
				final StringBuilder sb = below.get( sourceFrame );
				for ( int i = minC + 1; i < maxC; i++ )
				{
					if ( sb.charAt( i ) == ' ' )
						sb.setCharAt( i, '-' );
				}
				sb.setCharAt( minC, '+' );
				sb.setCharAt( maxC, '+' );
			}
		}

		/*
		 * Concatenate strings
		 */

		final StringBuilder finalString = new StringBuilder();
		for ( final Integer frame : frames )
		{
			finalString.append( strings.get( frame ).toString() );
			finalString.append( '\n' );
			finalString.append( below.get( frame ).toString() );
			finalString.append( '\n' );
		}

		return finalString.toString();

	}

	public static final boolean isTree( final TrackModel model, final TimeDirectedNeighborIndex cache )
	{
		return isTree( model.vertexSet(), cache );
	}

	public static final boolean isTree( final Iterable< Spot > spots, final TimeDirectedNeighborIndex cache )
	{
		for ( final Spot spot : spots )
		{
			if ( cache.predecessorsOf( spot ).size() > 1 )
				return false;
		}
		return true;
	}

	public static final Map< Spot, Integer > cumulativeBranchWidth( final TrackModel model )
	{

		/*
		 * Elements stored: 0. cumsum of leaf
		 */
		final Supplier< int[] > factory = new Supplier< int[] >()
		{
			@Override
			public int[] get()
			{
				return new int[ 1 ];
			}
		};

		/*
		 * Build isleaf tree
		 */

		final TimeDirectedNeighborIndex cache = model.getDirectedNeighborIndex();

		final Function1< Spot, int[] > isLeafFun = new Function1< Spot, int[] >()
		{
			@Override
			public void compute( final Spot input, final int[] output )
			{
				if ( cache.successorsOf( input ).size() == 0 )
					output[ 0 ] = 1;
				else
					output[ 0 ] = 0;
			}
		};

		final Map< Spot, int[] > mappings = new HashMap<>();
		final SimpleDirectedWeightedGraph< int[], DefaultWeightedEdge > leafTree = model.copy( factory, isLeafFun, mappings );

		/*
		 * Find root spots & first spots Roots are spots without any ancestors.
		 * There might be more than one per track. First spots are the first
		 * root found in a track. There is only one per track.
		 * 
		 * By the way we compute the largest spot name
		 */

		final Set< Spot > roots = new HashSet<>( model.nTracks( false ) ); // approx
		final Set< Spot > firsts = new HashSet<>( model.nTracks( false ) ); // exact
		final Set< Integer > ids = model.trackIDs( false );
		for ( final Integer id : ids )
		{
			final Set< Spot > track = model.trackSpots( id );
			boolean firstFound = false;
			for ( final Spot spot : track )
			{
				if ( cache.predecessorsOf( spot ).size() == 0 )
				{
					if ( !firstFound )
						firsts.add( spot );

					roots.add( spot );
					firstFound = true;
				}
			}
		}

		/*
		 * Build cumsum value
		 */

		final Function2< int[], int[] > cumsumFun = new Function2< int[], int[] >()
		{
			@Override
			public void compute( final int[] input1, final int[] input2, final int[] output )
			{
				output[ 0 ] = input1[ 0 ] + input2[ 0 ];
			}
		};

		final RecursiveCumSum< int[], DefaultWeightedEdge > cumsum = new RecursiveCumSum<>( leafTree, cumsumFun );
		for ( final Spot root : firsts )
		{
			final int[] current = mappings.get( root );
			cumsum.apply( current );
		}

		/*
		 * Convert to map of spot vs integer
		 */
		final Map< Spot, Integer > widths = new HashMap<>();
		for ( final Spot spot : model.vertexSet() )
			widths.put( spot, mappings.get( spot )[ 0 ] );

		return widths;
	}

	private static char[] makeSpaces( final int width )
	{
		return makeChars( width, ' ' );
	}

	private static char[] makeChars( final int width, final char c )
	{
		final char[] chars = new char[ width ];
		Arrays.fill( chars, c );
		return chars;
	}

	/**
	 * Returns the siblings of a spot. That is: all the spots that have the same
	 * predecessor.
	 * 
	 * @param cache
	 *            a neighbor cache.
	 * @param spot
	 *            the spot to inspect.
	 * @return a new set made of the spot siblings. Includes the spot.
	 */
	public static final Set< Spot > getSibblings( final NeighborCache< Spot, DefaultWeightedEdge > cache, final Spot spot )
	{
		final HashSet< Spot > sibblings = new HashSet<>();
		final Set< Spot > predecessors = cache.predecessorsOf( spot );
		for ( final Spot predecessor : predecessors )
			sibblings.addAll( cache.successorsOf( predecessor ) );

		return sibblings;
	}

}
