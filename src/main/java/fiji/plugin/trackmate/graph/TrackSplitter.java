package fiji.plugin.trackmate.graph;

import ij.ImageJ;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.imglib2.algorithm.Algorithm;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.traverse.GraphIterator;
import org.scijava.util.AppUtils;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class TrackSplitter implements Algorithm
{

	private final Model model;

	private String errorMessage;

	private Collection< List< Spot >> branches;

	public TrackSplitter( final Model model )
	{
		this.model = model;
	}

	@Override
	public boolean checkInput()
	{
		return true;
	}

	@Override
	public boolean process()
	{
		final TrackModel tm = model.getTrackModel();
		final Set< Integer > trackIDs = tm.trackIDs( true );
		branches = new ArrayList< List< Spot >>();
		for ( final Integer trackID : trackIDs )
		{
			System.out.println();// DEBUG
			System.out.println( "For track " + trackID );// DEBUG

			/*
			 * Identify leaves
			 */

			final Set< Spot > trackSpots = tm.trackSpots( trackID );
			final List< Spot > leaves = new ArrayList< Spot >();

			// Add at least the first spot of the track
			final TreeSet< Spot > sorted = new TreeSet< Spot >( Spot.frameComparator );
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
			Collections.sort( leaves, Spot.frameComparator );

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
			 * Nucleate a branch for each leaf.
			 */

			final Set< Spot > visited = new HashSet< Spot >();
			do
			{
				final Spot start = leaves.iterator().next();
				leaves.remove( start );

				/*
				 * Initiate the branch
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

				System.out.println( "\nStarting at spot " + start );// DEBUG
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
						leaves.add( successor );
						System.out.println( "Adding " + successor + " as a new leaf." );// DEBUG
					}
					Collections.sort( leaves, Spot.frameComparator );
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
						System.out.println( "  Found a fusion/split at " + spot + " in " + tm.edgesOf( spot ).size() + " parts." );// DEBUG.
						leaves.remove( previous );
						if ( visited.contains( spot ) )
						{
							System.out.println( "    Already visited, we stop before." );// DEBUG
							break;
						}
						visited.add( spot );

						// Inspect neighbors
						final Set< Spot > successors = successorsOf( spot, tm );
						final Set< Spot > predecessors = predecessorsOf( spot, tm );

						// Determine whether we have a fusion or something else.
						if ( predecessors.size() > 1 && successors.size() <= 1 )
						{
							System.out.println( "    We have a fusion point." );// DEBUG
							if ( ( successors.size() == 1 ) && ( Math.abs( spot.diffTo( successors.iterator().next(), Spot.FRAME ) ) < 2 ) )
							{
								// No gap. Everything is fine, and we will
								// process this later.
								leaves.add( spot );
							}
							else
							{
								branch.add( spot );
								if ( successors.size() == 1 )
								{
									leaves.add( successors.iterator().next() );
								}
								System.out.println( "    Attaching " + spot + " to current branch." );// DEBUG
							}

						}
						else if ( predecessors.size() <= 1 && successors.size() > 1 )
						{
							System.out.println( "    We have a split point." );// DEBUG
							leaves.addAll( successors );
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
								// A gap: we attach it on one of the successors,
								// but we will do it later.
								final Spot target = successors.iterator().next();
								attachTo.put( target, spot );
								System.out.println( "    Attaching " + spot + " to " + target + " for next branch." );// DEBUG
							}
						}
						else
						{
							System.out.println( "    We have a complex point." );// DEBUG
							branch.add( spot );
							leaves.addAll( successors );
							predecessors.remove( previous );
							leaves.addAll( predecessors );
						}
						Collections.sort( leaves, Spot.frameComparator );
						break;
					}


					/*
					 * Leaf
					 */

					if ( tm.edgesOf( spot ).size() == 1 )
					{
						// We have reached a leaf.
						System.out.println( "  Reached a leaf at " + spot + "." );// DEBUG
						branch.add( spot );
						leaves.remove( spot );
						break;
					}


					/*
					 * Detect gaps
					 */

					if ( Math.abs( spot.diffTo( previous, Spot.FRAME ) ) > 1 )
					{
						// We have a gap.
						System.out.println( "  Found a gap between " + spot + " and " + previous + "." );// DEBUG
						leaves.add( spot );
						Collections.sort( leaves, Spot.frameComparator );
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
					System.out.println( "Finished branch " + branch );// DEBUG
					System.out.println( "Current leaves: " + leaves );// DEBUG
					branches.add( branch );
				}
			}
			while ( !leaves.isEmpty() );

		}

		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

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

	public static void main( final String[] args )
	{
		// final File file = new File( AppUtils.getBaseDirectory(
		// TrackMate.class ), "samples/FakeTracks.xml" );
		// final File file = new File( AppUtils.getBaseDirectory(
		// TrackMate.class ), "samples/FakeTracks_MergeGap.xml" );
		final File file = new File( AppUtils.getBaseDirectory( TrackMate.class ), "samples/FakeTracks_GapSplit.xml" );
		// final File file = new File( AppUtils.getBaseDirectory(
		// TrackMate.class ), "samples/FakeTracks_Loops.xml" );
		// final File file = new File( AppUtils.getBaseDirectory(
		// TrackMate.class ), "samples/FakeTracks_SingleSplit.xml" );
		// final File file = new File( AppUtils.getBaseDirectory(
		// TrackMate.class ), "samples/FakeTracks_SplitsMerges.xml" );
		final TmXmlReader reader = new TmXmlReader( file );
		final Model model = reader.getModel();
		System.out.println( model.toString() );

		ImageJ.main( args );
		final HyperStackDisplayer displayer = new HyperStackDisplayer( model, new SelectionModel( model ) );
		displayer.render();

		final TrackSplitter splitter = new TrackSplitter( model );
		splitter.process();

		System.out.println();// DEBUG
		System.out.println( "Found " + splitter.branches.size() + " branches." );// DEBUG

		final Model model2 = new Model();
		model2.beginUpdate();
		try
		{
			for ( final List< Spot > branch : splitter.branches )
			{
				final Iterator< Spot > it = branch.iterator();
				Spot previous = it.next();
				model2.addSpotTo( previous, previous.getFeature( Spot.FRAME ).intValue() );
				while ( it.hasNext() )
				{
					final Spot spot = it.next();
					model2.addSpotTo( spot, spot.getFeature( Spot.FRAME ).intValue() );
					model2.addEdge( previous, spot, -1d );
					previous = spot;
				}
			}
		}
		finally
		{
			model2.endUpdate();
		}

		final TrackIndexAnalyzer analyzer = new TrackIndexAnalyzer();
		analyzer.process( model2.getTrackModel().trackIDs( true ), model2 );


		final SelectionModel sm2 = new SelectionModel( model2 );
		final HyperStackDisplayer displayer2 = new HyperStackDisplayer( model2, sm2 );
		final PerTrackFeatureColorGenerator tcg = new PerTrackFeatureColorGenerator( model2, TrackIndexAnalyzer.TRACK_ID );
		displayer2.setDisplaySettings( TrackMateModelView.KEY_TRACK_COLORING, tcg );
		displayer2.render();
		final TrackScheme trackScheme = new TrackScheme( model2, sm2 );
		trackScheme.setDisplaySettings( TrackMateModelView.KEY_TRACK_COLORING, tcg );
		trackScheme.render();



	}

}
