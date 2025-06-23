package fiji.plugin.trackmate.gui.editor.labkit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.MaskUtils;
import ij.IJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.roi.IterableRegion;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.labeling.Labeling;

/**
 * Re-import the edited segmentation made in Labkit into the TrackMate model it
 * started from.
 */
public class LabkitImporter2< T extends IntegerType< T > & NativeType< T > >
{

	private static final boolean DEBUG = false;

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
	public LabkitImporter2(
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
	 * @param labelingThisFrame
	 *            the new index image of the labeling model, that represents the
	 *            TrackMate model in the specified time-point after
	 *            modification.
	 * @param previousIndexImg
	 *            the previous index image, that represents the TrackMate model
	 *            before modification.
	 * @param currentTimePoint
	 *            the time-point in the TrackMate model that corresponds to the
	 *            index image.
	 * @param spotLabels
	 *            the map of spots (vs the label value in the previous labeling)
	 *            that were written in the previous index image.
	 */
	public void reimport(
			final Labeling labelingThisFrame,
			final RandomAccessibleInterval< T > previousIndexImg,
			final int currentTimePoint,
			final Map< Label, Spot > spotLabels )
	{
		// Collect labels corresponding to spots that have been modified.
		final Set< Integer > modifiedLabels = getModifiedIndices( labelingThisFrame, previousIndexImg );
		final int nModified = modifiedLabels.size();
		if ( nModified == 0 )
			return;

		model.beginUpdate();
		try
		{
			/*
			 * Get all the spots present in the new image, as a map against the
			 * label in the novel index image.
			 */
			final Map< Label, List< Spot > > novelSpots = getSpots( labelingThisFrame );

			System.out.println( "New spots for timepoint " + +currentTimePoint + ":" ); // DEBUG
			for ( final Label label : novelSpots.keySet() )
			{
				System.out.println( " - Label: " + label ); // DEBUG
				novelSpots.get( label ).forEach( s -> System.out.println( "    - " + str( s ) ) ); // DEBUG
			}

//			// Update model for those spots.
//			for ( final int labelValue : modifiedLabels )
//			{
//				final Spot previousSpot = spotLabels.get( labelValue );
//				final List< Spot > novelSpotList = novelSpots.get( labelValue );
//				if ( previousSpot == null )
//				{
//					/*
//					 * A new one (possible several) I cannot find in the
//					 * previous list -> add as a new spot.
//					 */
//					if ( novelSpotList != null )
//						addNewSpot( novelSpotList, currentTimePoint );
//				}
//				else if ( novelSpotList == null || novelSpotList.isEmpty() )
//				{
//					/*
//					 * One I had in the previous spot list, but that has
//					 * disappeared. Remove it.
//					 */
//					IJ.log( " - Removed spot " + str( previousSpot ) );
//					model.removeSpot( previousSpot );
//				}
//				else
//				{
//					/*
//					 * I know of them both. Treat the case as if the previous
//					 * spot was modified.
//					 */
//					modifySpot( novelSpotList, previousSpot, currentTimePoint );
//				}
//			}
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
		if ( DEBUG )
			IJ.log( " - Modified spot " + str( previousSpot ) + " -> " + str( mainNovelSpot ) );

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

			if ( DEBUG )
				IJ.log( " - Added spot " + str( s ) );
		}
	}

	private void addNewSpot( final Iterable< Spot > novelSpotList, final int currentTimePoint )
	{
		for ( final Spot spot : novelSpotList )
		{
			spot.putFeature( Spot.POSITION_T, currentTimePoint * dt );
			spot.putFeature( Spot.QUALITY, -1. );
			model.addSpotTo( spot, Integer.valueOf( currentTimePoint ) );

			if ( DEBUG )
				IJ.log( " - Added spot " + str( spot ) );
		}
	}

	private final Set< Integer > getModifiedIndices(
			final Labeling labelingThisFrame,
			final RandomAccessibleInterval< T > previousIndexImg )
	{
		final ConcurrentSkipListSet< Integer > modifiedIDs = new ConcurrentSkipListSet<>();
		LoopBuilder.setImages( labelingThisFrame, previousIndexImg )
				.multiThreaded( false )
				.forEachPixel( ( c, p ) -> {
					final int ci = c.getIndex().getInteger();
					final int pi = p.getInteger();
					if ( ci == 0 && pi == 0 )
						return;
					if ( ci != pi )
					{
						modifiedIDs.add( Integer.valueOf( pi ) );
						modifiedIDs.add( Integer.valueOf( ci ) );
					}
				} );
		modifiedIDs.remove( Integer.valueOf( -1 ) );
		return modifiedIDs;
	}

	private Map< Label, List< Spot > > getSpots( final Labeling labeling )
	{
		final List< Label > labels = labeling.getLabels();
		final Map< Label, List< Spot > > spots = new HashMap<>();
		final Map< Label, IterableRegion< BitType > > regions = labeling.iterableRegions();
		final double threshold = 0.5;
		final int numThreads = 1;
		for ( final Label label : labels )
		{
			final IterableRegion< BitType > region = regions.get( label );
			final List< Spot > spotsForLabel = MaskUtils.fromThresholdWithROI( region, region, calibration, threshold, simplify, numThreads, null );
			spots.put( label, spotsForLabel );
		}
		return spots;
	}

	private static final String str( final Spot spot )
	{
		return spot.ID() + " (" +
				roundToN( spot.getDoublePosition( 0 ), 1 ) + ", " +
				roundToN( spot.getDoublePosition( 1 ), 1 ) + ", " +
				roundToN( spot.getDoublePosition( 2 ), 1 ) + ") " +
				"@ t=" + spot.getFeature( Spot.FRAME ).intValue();
	}

	private static double roundToN( final double num, final int n )
	{
		final double scale = Math.pow( 10, n );
		return Math.round( num * scale ) / scale;
	}
}
