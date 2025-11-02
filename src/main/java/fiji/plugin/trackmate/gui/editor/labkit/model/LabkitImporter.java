package fiji.plugin.trackmate.gui.editor.labkit.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.MaskUtils;
import ij.IJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.IterableRegion;
import net.imglib2.roi.labeling.LabelingMapping;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.labeling.Labeling;

/**
 * Re-import the edited segmentation made in Labkit into the TrackMate model it
 * started from.
 */
public class LabkitImporter
{

	private static final boolean DEBUG = true;

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
	private LabkitImporter(
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
		final Logger log = Logger.IJ_LOGGER;
		log.setStatus( "Re-importing from Labkit..." );

		// Collect modified indices.
		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< UnsignedIntType > indexImg = ( RandomAccessibleInterval< UnsignedIntType > ) labeling.getIndexImg();
		final Set< Integer > modifiedIndices = TMLabKitUtils.getModifiedIndices( indexImg, previousIndexImg );
		final int nModified = modifiedIndices.size();
		if ( nModified == 0 )
			return;

		// Collect corresponding LabKit labels.
		final LabelingMapping< Label > mapping = labeling.getType().getMapping();
		final Set< Label > modifiedLabels = new HashSet<>();
		for ( final Integer id : modifiedIndices )
			modifiedLabels.addAll( mapping.labelsAtIndex( id ) );

		if ( DEBUG )
		{
			IJ.log( "\nRe-importing from Labkit" );
			IJ.log( "Modified indices: " + modifiedIndices );
			IJ.log( "Corresponding modified labels & initial spot (if any):" );
			modifiedLabels.forEach( l -> IJ.log( " - " + l.name() + " -> " + initialMapping.get( l ) ) );
			IJ.log( "Re-insertion in the model:" );
		}

		/*
		 * Create and collect all new spots corresponding to modified labels.
		 *
		 * A LabKit label can possibly extend over several time-points. We want
		 * to split them frame by frame, to have the corresponding 2D or 3D
		 * masks.
		 */
		final double threshold = .5;
		final int numThreads = 1;
		final Map< Label, IterableRegion< BitType > > regions = labeling.iterableRegions();
		final int timeAxis = TMLabKitUtils.timeAxis( labeling );
		// Map of timepoint -> (Map of label -> list of spots (at this timepoint, for this label)).
		final Map< Integer, Map< Label, List< Spot > > > allModifiedSpots = new TreeMap<>();

		if ( timeAxis < 0 )
		{
			// Only one time-point.
			final int lt = Math.max( 0, targetTimePoint );
			final Map< Label, List< Spot > > map = new HashMap<>();
			for ( final Label label : modifiedLabels )
			{
				final IterableRegion< BitType > region = regions.get( label );
				final double smoothingScale = -1.; // no smoothing
				final List< Spot > spots = MaskUtils.fromThresholdWithROI( region, region, calibration, threshold, simplifyContours, smoothingScale, numThreads, null );
				map.put( label, spots );
			}
			allModifiedSpots.put( lt, map );
		}
		else
		{
			for ( final Label label : modifiedLabels )
			{
				final IterableRegion< BitType > region = regions.get( label );
				final long minT = region.min( timeAxis );
				final long maxT = region.max( timeAxis );
				for ( long t = minT; t <= maxT; t++ )
				{
					final IntervalView< BitType > slice = Views.hyperSlice( region, timeAxis, t );
					final double smoothingScale = -1.; // no smoothing
					final List< Spot > spots = MaskUtils.fromThresholdWithROI( slice, slice, calibration, threshold, simplifyContours, smoothingScale, numThreads, null );

					Map< Label, List< Spot > > map = allModifiedSpots.get( Integer.valueOf( ( int ) t ) );
					if ( map == null )
					{
						map = new HashMap<>();
						allModifiedSpots.put( Integer.valueOf( ( int ) t ), map );
					}
					map.put( label, spots );
				}
			}
		}

		/*
		 * Modify the TrackMate model to reflect the changes in the spots. We
		 * operate time-point by time-point.
		 */
		final int nTimepoints = allModifiedSpots.size();
		final Set< Integer > timepoints = allModifiedSpots.keySet();
		int progress = 0;
		for ( final Integer timepoint : timepoints )
		{
			// The new modified of this time-point.
			final Map< Label, List< Spot > > modifiedSpots = allModifiedSpots.get( timepoint );

			// The initial spots of this time-point.
			final Map< Label, Spot > previousSpots = new HashMap<>();
			for ( final Label label : initialMapping.keySet() )
			{
				final Spot spot = initialMapping.get( label );
				if ( spot.getFeature( Spot.FRAME ).intValue() == timepoint )
					previousSpots.put( label, spot );
			}

			reimport( previousSpots, modifiedSpots, timepoint, simplifyContours );
			log.setProgress( ++progress / ( double ) nTimepoints );
		}
	}


	/**
	 * Re-import the spots into the TrackMate model at one specific time-point.
	 * <p>
	 * The two spot collections are compared. Spots corresponding to
	 * modifications are added, removed or edited in the TrackMate model.
	 *
	 * @param initialSpots
	 *            the initial spots (corresponding to a specific label)
	 * @param modifiedSpots
	 *            the list of new spots, as a map from the corresponding label.
	 * @param currentTimePoint
	 *            the time-point to import to.
	 * @param simplifyContours
	 *            whether to simplify the contour of spots created from the
	 *            masks.
	 */
	private void reimport(
			final Map< Label, Spot > initialSpots,
			final Map< Label, List< Spot > > modifiedSpots,
			final int currentTimePoint,
			final boolean simplifyContours )
	{
		model.beginUpdate();
		try
		{
			// Update model for those spots.
			for ( final Label label : modifiedSpots.keySet() )
			{
				final Spot previousSpot = initialSpots.get( label );
				final List< Spot > novelSpotList = modifiedSpots.get( label );
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
					if ( DEBUG )
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

		public LabkitImporter get()
		{
			return new LabkitImporter(
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
