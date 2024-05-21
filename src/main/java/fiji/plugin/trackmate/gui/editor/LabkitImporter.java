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
package fiji.plugin.trackmate.gui.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.detection.SpotMeshUtils;
import fiji.plugin.trackmate.detection.SpotRoiUtils;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.view.Views;

/**
 * Re-import the edited segmentation made in Labkit into the TrackMate model it
 * started from.
 */
public class LabkitImporter< T extends IntegerType< T > & NativeType< T > >
{

	private final Model model;

	private final double[] calibration;

	private final double dt;

	/**
	 * Creates a new re-importer.
	 * 
	 * @param model
	 *            the model to add, remove or edit spots in.
	 * @param calibration
	 *            the spatial calibration array: <code>[dx, dy dz]</code>.
	 * @param dt
	 *            the frame interval.
	 */
	public LabkitImporter(
			final Model model,
			final double[] calibration,
			final double dt )
	{
		this.model = model;
		this.calibration = calibration;
		this.dt = dt;
	}

	/**
	 * Re-import the specified label image (specified by its index image) into
	 * the TrackMate model. The label images must corresponds to one time-point.
	 * <p>
	 * The index image after modification is compared with the original one, and
	 * modifications are detected. Spots corresponding to modifications are
	 * added, removed or edited in the TrackMate model.
	 * <p>
	 * To properly detect modifications, the indices in the label images must
	 * correspond to the spot ID + 1 (<code>index = id + 1</code>).
	 * 
	 * @param novelIndexImg
	 *            the new index image of the labeling model, that represents the
	 *            TrackMate model in the specified time-point after
	 *            modification.
	 * @param previousIndexImg
	 *            the previous index image, that represents the TrackMate model
	 *            before modification.
	 * @param currentTimePoint
	 *            the time-point in the TrackMate model that corresponds to the
	 *            index image.
	 * @param smoothingScale
	 *            the smoothing scale, in physical unit, to use for smoothing
	 *            the masks when re-importing them into TrackMate.
	 */
	public void reimport(
			final RandomAccessibleInterval< T > novelIndexImg,
			final RandomAccessibleInterval< T > previousIndexImg,
			final int currentTimePoint,
			final double smoothingScale )
	{

		// Collect ids of spots that have been modified. id = index - 1
		final Set< Integer > modifiedIDs = getModifiedIDs( novelIndexImg, previousIndexImg );
		final int nModified = modifiedIDs.size();

		if ( nModified == 0 )
			return;

		model.beginUpdate();
		try
		{
			// Map of previous spots against their ID:
			final SpotCollection spots = model.getSpots();
			final Map< Integer, Spot > previousSpotIDs = new HashMap<>();
			spots.iterable( currentTimePoint, true ).forEach( s -> previousSpotIDs.put( Integer.valueOf( s.ID() ), s ) );

			/*
			 * Get all the spots present in the new image. Because we specified
			 * the novel label image as 'quality' image, they have a quality
			 * value equal to the index in the label image (id+1).
			 */
			final List< Spot > novelSpots = getSpots( novelIndexImg, smoothingScale );

			/*
			 * Map of novel spots against the ID taken from the index of the
			 * novel label image. Normally, this index, and hence the novel id,
			 * corresponds to the id of previous spots. If one of the novel spot
			 * has an id we cannot find in the previous spot list, it means that
			 * it is a new one.
			 *
			 * Careful! The user might have created several connected components
			 * with the same label in LabKit, which will result in having
			 * several spots with the same quality value. We don't want to loose
			 * them, so the map is that of a id to a list of spots.
			 */
			final Map< Integer, List< Spot > > novelSpotIDs = new HashMap<>();
			novelSpots.forEach( s -> {
				final int id = Integer.valueOf( s.getFeature( Spot.QUALITY ).intValue() - 1 );
				final List< Spot > list = novelSpotIDs.computeIfAbsent( Integer.valueOf( id ), ( i ) -> new ArrayList<>() );
				list.add( s );
			} );

			// Update model for those spots.
			for ( final int id : modifiedIDs )
			{
				final Spot previousSpot = previousSpotIDs.get( Integer.valueOf( id ) );
				final List< Spot > novelSpotList = novelSpotIDs.get( Integer.valueOf( id ) );
				if ( previousSpot == null )
				{
					/*
					 * A new one (possible several) I cannot find in the
					 * previous list -> add as a new spot.
					 */
					addNewSpot( novelSpotList, currentTimePoint );
				}
				else if ( novelSpotList == null || novelSpotList.isEmpty() )
				{
					/*
					 * One I add in the previous spot list, but that has
					 * disappeared. Remove it.
					 */
					model.removeSpot( previousSpot );
				}
				else
				{
					/*
					 * I know of them both. Treat the case as if the previous
					 * spot was modified.
					 */
					modifySpot( novelSpotList, previousSpot, currentTimePoint );
				}
			}
		}
		finally
		{
			model.endUpdate();
		}
	}

	private void modifySpot( final List< Spot > novelSpotList, final Spot previousSpot, final int currentTimePoint )
	{
		/*
		 * Hopefully there is only one spot in the novel spot list. If not we
		 * privilege the one closest to the previous spots.
		 */
		final Spot mainNovelSpot;
		if ( novelSpotList.size() == 1 )
		{
			mainNovelSpot = novelSpotList.get( 0 );
		}
		else
		{
			Spot closest = null;
			double minD2 = Double.POSITIVE_INFINITY;
			for ( final Spot s : novelSpotList )
			{
				final double d2 = s.squareDistanceTo( previousSpot );
				if ( d2 < minD2 )
				{
					minD2 = d2;
					closest = s;
				}
			}
			mainNovelSpot = closest;
		}

		// Add it properly.
		mainNovelSpot.setName( previousSpot.getName() );
		mainNovelSpot.putFeature( Spot.POSITION_T, currentTimePoint * dt );
		mainNovelSpot.putFeature( Spot.QUALITY, -1. );
		model.addSpotTo( mainNovelSpot, Integer.valueOf( currentTimePoint ) );
		// Recreate links.
		final Set< DefaultWeightedEdge > edges = model.getTrackModel().edgesOf( previousSpot );
		for ( final DefaultWeightedEdge e : edges )
		{
			final double weight = model.getTrackModel().getEdgeWeight( e );
			final Spot source = model.getTrackModel().getEdgeSource( e );
			final Spot target = model.getTrackModel().getEdgeTarget( e );
			if ( source == previousSpot )
				model.addEdge( mainNovelSpot, target, weight );
			else if ( target == previousSpot )
				model.addEdge( source, mainNovelSpot, weight );
			else
				throw new IllegalArgumentException( "The edge of a spot does not have the spot as source or target?!?" );
		}
		model.removeSpot( previousSpot );

		// Deal with the other ones.
		final HashSet< Spot > extraSpots = new HashSet<>( novelSpotList );
		extraSpots.remove( mainNovelSpot );
		int i = 1;
		for ( final Spot s : extraSpots )
		{
			s.setName( previousSpot.getName() + "_" + i++ );
			s.putFeature( Spot.POSITION_T, currentTimePoint * dt );
			s.putFeature( Spot.QUALITY, -1. );
			model.addSpotTo( s, Integer.valueOf( currentTimePoint ) );
		}
	}

	private void addNewSpot( final List< Spot > novelSpotList, final int currentTimePoint )
	{
		for ( final Spot spot : novelSpotList )
		{
			spot.putFeature( Spot.POSITION_T, currentTimePoint * dt );
			spot.putFeature( Spot.QUALITY, -1. );
			model.addSpotTo( spot, Integer.valueOf( currentTimePoint ) );
		}
	}

	private final Set< Integer > getModifiedIDs(
			final RandomAccessibleInterval< T > novelIndexImg,
			final RandomAccessibleInterval< T > previousIndexImg )
	{
		final ConcurrentSkipListSet< Integer > modifiedIDs = new ConcurrentSkipListSet<>();
		LoopBuilder.setImages( novelIndexImg, previousIndexImg )
				.multiThreaded( false )
				.forEachPixel( ( c, p ) -> {
					final int ci = c.getInteger();
					final int pi = p.getInteger();
					if ( ci == 0 && pi == 0 )
						return;
					if ( ci != pi )
					{
						modifiedIDs.add( Integer.valueOf( pi - 1 ) );
						modifiedIDs.add( Integer.valueOf( ci - 1 ) );
					}
				} );
		modifiedIDs.remove( Integer.valueOf( -1 ) );
		return modifiedIDs;
	}

	private List< Spot > getSpots( final RandomAccessibleInterval< T > rai, final double smoothingScale )
	{
		// Get all labels.
		final AtomicInteger max = new AtomicInteger( 0 );
		Views.iterable( rai ).forEach( p -> {
			final int val = p.getInteger();
			if ( val != 0 && val > max.get() )
				max.set( val );
		} );
		final List< Integer > indices = new ArrayList<>( max.get() );
		for ( int i = 0; i < max.get(); i++ )
			indices.add( Integer.valueOf( i + 1 ) );

		final ImgLabeling< Integer, ? > labeling = ImgLabeling.fromImageAndLabels( rai, indices );
		final boolean simplify = true;
		if ( rai.numDimensions() == 2 )
		{
			return SpotRoiUtils.from2DLabelingWithROI(
					labeling,
					labeling.minAsDoubleArray(),
					calibration,
					simplify,
					smoothingScale,
					rai );
		}
		else
		{
			return SpotMeshUtils.from3DLabelingWithROI(
					labeling,
					labeling.minAsDoubleArray(),
					calibration,
					simplify,
					smoothingScale,
					rai );
		}
	}
}
