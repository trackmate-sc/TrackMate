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
import fiji.plugin.trackmate.detection.MaskUtils;
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

	private final boolean simplify;

	/**
	 * Creates a new re-importer.
	 *
	 * @param model
	 *            the model to add, remove or edit spots in.
	 * @param calibration
	 *            the spatial calibration array: <code>[dx, dy dz]</code>.
	 * @param dt
	 *            the frame interval.
	 * @param simplifyContours
	 *            if <code>true</code> the contours of the spots imported and
	 *            modified will be simplified. If <code>false</code> their
	 *            contour will follow pixel edges.
	 */
	public LabkitImporter(
			final Model model,
			final double[] calibration,
			final double dt,
			final boolean simplifyContours )
	{
		this.model = model;
		this.calibration = calibration;
		this.dt = dt;
		this.simplify = simplifyContours;
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
	 */
	public void reimport(
			final RandomAccessibleInterval< T > novelIndexImg,
			final RandomAccessibleInterval< T > previousIndexImg,
			final int currentTimePoint )
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
			 * Get all the spots present in the new image, as a map against the
			 * label in the novel index image. This label value corresponds to
			 * the ids of the spots in the previous index image (label = id+1).
			 */
			final Map< Integer, List< Spot > > novelSpots = getSpots( novelIndexImg );

			// Update model for those spots.
			for ( final int id : modifiedIDs )
			{
				final Spot previousSpot = previousSpotIDs.get( Integer.valueOf( id ) );
				final List< Spot > novelSpotList = novelSpots.get( Integer.valueOf( id ) + 1 );
				if ( previousSpot == null )
				{
					/*
					 * A new one (possible several) I cannot find in the
					 * previous list -> add as a new spot.
					 */
					if ( novelSpotList != null )
						addNewSpot( novelSpotList, currentTimePoint );
				}
				else if ( novelSpotList == null || novelSpotList.isEmpty() )
				{
					/*
					 * One I had in the previous spot list, but that has
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

	private void addNewSpot( final Iterable< Spot > novelSpotList, final int currentTimePoint )
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

	private Map< Integer, List< Spot > > getSpots( final RandomAccessibleInterval< T > rai )
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
		final Map< Integer, List< Spot > > spots = MaskUtils.fromLabelingWithROIMap(
				labeling,
				labeling,
				calibration,
				simplify,
				rai );
		return spots;
	}
}
