package fiji.plugin.trackmate.gui.editor.labkit.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.MaskUtils;
import ij.IJ;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.roi.IterableRegion;
import net.imglib2.roi.labeling.LabelingMapping;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.view.Views;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.labeling.Labeling;
import sc.fiji.labkit.ui.labeling.Labelings;

/**
 * Re-import the edited segmentation made in Labkit into the TrackMate model it
 * started from.
 */
public class LabkitImporter2
{

	private static final boolean DEBUG = false;

	private final Model model;

	private final double[] calibration;

	private final double dt;

	private final Labeling labeling;

	private final RandomAccessibleInterval< UnsignedIntType > previousIndexImg;

	private final Map< Label, Spot > initialMapping;

	private final int targetTimePoint;

	private final boolean simplifyContours;

	public static Builder create()
	{
		return new Builder();
	}

	/**
	 * Creates a new re-importer.
	 *
	 * @param model
	 *            the model to add, remove or edit spots in.
	 * @param targetTimePoint
	 *            the time-point to import. If negative, all time-points will be
	 *            imported.
	 * @param initialMapping
	 *            mapping of labels to spots in the initial label image (prior
	 *            to modification).
	 * @param initialIndexImg
	 *            the initial label image (prior to modification).
	 * @param labeling
	 *            the current labeling (after modification).
	 * @param simplifyContours
	 *            if <code>true</code> the contours of the spots imported and
	 *            modified will be simplified. If <code>false</code> their
	 *            contour will follow pixel edges.
	 * @param calibration
	 *            the spatial calibration array: <code>[dx, dy dz]</code>.
	 * @param dt
	 *            the frame interval.
	 */
	private LabkitImporter2(
			final Model model,
			final Labeling labeling,
			final RandomAccessibleInterval< UnsignedIntType > initialIndexImg,
			final Map< Label, Spot > initialMapping,
			final int targetTimePoint,
			final boolean simplifyContours,
			final double[] calibration,
			final double dt )
	{
		this.model = model;
		this.labeling = labeling;
		this.previousIndexImg = initialIndexImg;
		this.initialMapping = initialMapping;
		this.targetTimePoint = targetTimePoint;
		this.simplifyContours = simplifyContours;
		this.calibration = calibration;
		this.dt = dt;
	}

	/**
	 * Returns <code>true</code> if changes have been made in the labeling
	 * compared to the initial index image and initial spot labels.
	 *
	 * @return <code>true</code> if changes are detected.
	 */
	public boolean hasChanges()
	{
		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< UnsignedIntType > indexImg = ( RandomAccessibleInterval< UnsignedIntType > ) labeling.getIndexImg();
		return TMLabKitUtils.isDifferent( previousIndexImg, indexImg );
	}

	/**
	 * Performs the import of modifications into the TrackMate model.
	 */
	public void run()
	{
		// Check dimensionality.
		boolean isSingleTimePoint = true;
		boolean is3D = false;
		for ( final CalibratedAxis axis : labeling.axes() )
		{
			if ( axis.type().equals( Axes.TIME ) )
				isSingleTimePoint = false;
			if ( axis.type().equals( Axes.Z ) )
				is3D = true;
		}

		// Possibly determine the number of time-points to parse.
		final int timeDim = ( isSingleTimePoint )
				? -1
				: ( is3D ) ? 3 : 2;
		final long nTimepoints = ( timeDim < 0 )
				? 0
				: labeling.numDimensions() > timeDim ? labeling.dimension( timeDim ) : 0;

		if ( targetTimePoint < 0 && nTimepoints > 1 )
		{
			// All time-points.
			final List< Labeling > slices = Labelings.slices( labeling );
			final Logger log = Logger.IJ_LOGGER;
			log.setStatus( "Re-importing from Labkit..." );
			for ( int t = 0; t < nTimepoints; t++ )
			{
				// The spots of this time-point:
				final Map< Label, Spot > spotLabelsThisFrame = new HashMap<>();
				for ( final Label label : initialMapping.keySet() )
				{
					final Spot spot = initialMapping.get( label );
					if ( spot.getFeature( Spot.FRAME ).intValue() == t )
						spotLabelsThisFrame.put( label, spot );
				}

				final Labeling labelingThisFrame = slices.get( t );
				final RandomAccessibleInterval< UnsignedIntType > previousIndexImgThisFrame = Views.hyperSlice( previousIndexImg, timeDim, t );
				reimport( labelingThisFrame, previousIndexImgThisFrame, t, spotLabelsThisFrame, simplifyContours );
				log.setProgress( t / ( double ) nTimepoints );
			}
			log.setStatus( "" );
			log.setProgress( 0. );
		}
		else
		{
			// Only one.
			final int localT = Math.max( 0, targetTimePoint );
			reimport( labeling, previousIndexImg, localT, initialMapping, simplifyContours );
		}
	}

	/**
	 * Re-import the specified label image (specified by its index image) into
	 * the TrackMate model. The label images must corresponds to one time-point.
	 * <p>
	 * The index image after modification is compared with the original one, and
	 * modifications are detected. Spots corresponding to modifications are
	 * added, removed or edited in the TrackMate model.
	 *
	 * @param labeling
	 *            the labeling, that represents the TrackMate model in the
	 *            specified time-point after modification.
	 * @param previousIndexImg
	 *            the previous index image, that represents the TrackMate model
	 *            before modification.
	 * @param currentTimePoint
	 *            the time-point in the TrackMate model that corresponds to the
	 *            index image.
	 * @param spotLabels
	 *            the map of spots (vs the label value in the previous labeling)
	 *            that were written in the previous index image.
	 * @param simplifyContours
	 */
	private void reimport(
			final Labeling labeling,
			final RandomAccessibleInterval< UnsignedIntType > previousIndexImg,
			final int currentTimePoint,
			final Map< Label, Spot > spotLabels, final boolean simplifyContours )
	{
		// Collect labels corresponding to spots that have been modified.
		final Set< Integer > modifiedIndices = getModifiedIndices( labeling, previousIndexImg );
		final int nModified = modifiedIndices.size();
		if ( nModified == 0 )
			return;

		final LabelingMapping< Label > mapping = labeling.getType().getMapping();
		final List< Label > modifiedLabels = new ArrayList<>();
		for ( final Integer id : modifiedIndices )
			modifiedLabels.addAll( mapping.labelsAtIndex( id ) );

		System.out.println( "Modified indices: " + modifiedIndices ); // DEBUG
		System.out.println( "Corresponding modified labels:" ); // DEBUG
		modifiedLabels.forEach( l -> System.out.println( " - " + l.name() ) );
		System.out.println( "Corresponding modified spots:" ); // DEBUG
		modifiedLabels.forEach( l -> System.out.println( " - " + spotLabels.get( l ) ) );

		model.beginUpdate();
		try
		{
			/*
			 * Get all the spots present in the new image, as a map against the
			 * label in the novel index image.
			 */
			final Map< Label, List< Spot > > novelSpots = getSpots( labeling, simplifyContours );

			System.out.println( "New spots for timepoint " + +currentTimePoint + ":" ); // DEBUG
			for ( final Label label : novelSpots.keySet() )
			{
				System.out.println( " - Label: " + label.name() ); // DEBUG
				final List< Spot > spots = novelSpots.get( label );
				for ( final Spot spot : spots )
				{
					spot.putFeature( Spot.FRAME, Double.valueOf( currentTimePoint ) );
					System.out.println( "    - " + str( spot ) );
				}
			}

			// Update model for those spots.
			for ( final Label label : modifiedLabels )
			{
				final Spot previousSpot = spotLabels.get( label );
				final List< Spot > novelSpotList = novelSpots.get( label );
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
					IJ.log( " - Removed spot " + str( previousSpot ) );
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
			final RandomAccessibleInterval< UnsignedIntType > previousIndexImg )
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

	private Map< Label, List< Spot > > getSpots( final Labeling labeling, final boolean simplifyContours )
	{
		final List< Label > labels = labeling.getLabels();
		final Map< Label, List< Spot > > spots = new HashMap<>();
		final Map< Label, IterableRegion< BitType > > regions = labeling.iterableRegions();
		final double threshold = 0.5;
		final int numThreads = 1;
		for ( final Label label : labels )
		{
			final IterableRegion< BitType > region = regions.get( label );
			final List< Spot > spotsForLabel = MaskUtils.fromThresholdWithROI( region, region, calibration, threshold, simplifyContours, numThreads, null );
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
				"@ t=" + spot.getFeature( Spot.FRAME );
	}

	private static double roundToN( final double num, final int n )
	{
		final double scale = Math.pow( 10, n );
		return Math.round( num * scale ) / scale;
	}

	public static class Builder
	{

		private Model model;

		private double[] calibration;

		private double dt;

		private Labeling labeling;

		private RandomAccessibleInterval< UnsignedIntType > initialIndexImg;

		private int targetTimePoint;

		private Map< Label, Spot > initialMapping;

		private boolean simplifyContours;

		public LabkitImporter2 get()
		{
			return new LabkitImporter2(
					model,
					labeling,
					initialIndexImg,
					initialMapping,
					targetTimePoint,
					simplifyContours,
					calibration,
					dt );
		}

		public Builder initialMapping( final Map< Label, Spot > initialMapping )
		{
			this.initialMapping = initialMapping;
			return this;
		}

		public Builder initialIndexImg( final RandomAccessibleInterval< UnsignedIntType > previousIndexImg )
		{
			this.initialIndexImg = previousIndexImg;
			return this;
		}

		public Builder targetTimePoint( final int targetTimePoint )
		{
			this.targetTimePoint = targetTimePoint;
			return this;
		}

		public Builder labeling( final Labeling labeling )
		{
			this.labeling = labeling;
			return this;
		}

		public Builder trackmateModel( final Model model )
		{
			this.model = model;
			return this;
		}

		public Builder calibration( final double[] calibration )
		{
			this.calibration = calibration;
			return this;
		}

		public Builder frameInterval( final double dt )
		{
			this.dt = dt;
			return this;
		}

		public Builder simplifyContours( final boolean simplifyContours )
		{
			this.simplifyContours = simplifyContours;
			return this;
		}
	}
}
