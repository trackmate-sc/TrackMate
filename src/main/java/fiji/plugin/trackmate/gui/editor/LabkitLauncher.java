package fiji.plugin.trackmate.gui.editor;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.Context;
import org.scijava.ui.behaviour.util.AbstractNamedAction;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.detection.MaskUtils;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.util.EverythingDisablerAndReenabler;
import fiji.plugin.trackmate.util.SpotUtil;
import fiji.plugin.trackmate.util.TMUtils;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import sc.fiji.labkit.ui.LabkitFrame;
import sc.fiji.labkit.ui.inputimage.DatasetInputImage;
import sc.fiji.labkit.ui.labeling.Labeling;
import sc.fiji.labkit.ui.models.DefaultSegmentationModel;
import sc.fiji.labkit.ui.models.ImageLabelingModel;

public class LabkitLauncher
{

	private final double[] calibration;

	private final TrackMate trackmate;

	private final EverythingDisablerAndReenabler disabler;

	private ImgPlus< UnsignedShortType > previousIndexImg;

	private int currentTimePoint;

	private final boolean is2D;

	private final double dt;

	public LabkitLauncher( final TrackMate trackmate, final EverythingDisablerAndReenabler disabler )
	{
		this.trackmate = trackmate;
		this.disabler = disabler;
		final ImagePlus imp = trackmate.getSettings().imp;
		this.calibration = TMUtils.getSpatialCalibration( imp );
		this.is2D = DetectionUtils.is2D( imp );
		this.dt = imp.getCalibration().frameInterval;
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	protected void launch()
	{
		final ImagePlus imp = trackmate.getSettings().imp;
		final ImgPlus src = TMUtils.rawWraps( imp );
		final int timeAxis = src.dimensionIndex( Axes.TIME );

		// Reslice for current time-point.
		this.currentTimePoint = imp.getFrame() - 1;
		final ImgPlus frame = ImgPlusViews.hyperSlice( src, timeAxis, currentTimePoint );
		final ImpBdvShowable showable = ImpBdvShowable.fromImp( frame, imp );
		final DatasetInputImage input = new DatasetInputImage( frame, showable );

		// Prepare label image.
		final AxisType[] axes = new AxisType[] { Axes.X, Axes.Y, Axes.Z, };
		final long[] dims = new long[ is2D ? 2 : 3 ];
		for ( int d = 0; d < dims.length; d++ )
			dims[ d ] = src.dimension( src.dimensionIndex( axes[ d ] ) );
		final Img< UnsignedShortType > lblImg = ArrayImgs.unsignedShorts( dims );
		final double[] calibration = TMUtils.getSpatialCalibration( imp );
		final ImgPlus< UnsignedShortType > lblImgPlus = new ImgPlus<>( lblImg, "LblImg", axes, calibration );

		// Write spots in it with index = id + 1
		final Iterable< Spot > spotsThisFrame = trackmate.getModel().getSpots().iterable( currentTimePoint, true );
		for ( final Spot spot : spotsThisFrame )
			SpotUtil.iterable( spot, lblImgPlus ).forEach( p -> p.set( spot.ID() + 1 ) );

		// Make a labeling model from it.
		final Context context = TMUtils.getContext();
		final DefaultSegmentationModel model = new DefaultSegmentationModel( context, input );
		model.imageLabelingModel().labeling().set( Labeling.fromImg( lblImgPlus ) );

		// Store a copy.
		this.previousIndexImg = lblImgPlus.copy();

		// Show LabKit.
		final LabkitFrame labkit = LabkitFrame.show( model, "Edit TrackMate data frame " + ( currentTimePoint + 1 ) );
		labkit.onCloseListeners().addListener( () -> reimportData( model.imageLabelingModel(), currentTimePoint ) );
	}

	@SuppressWarnings( "unchecked" )
	private void reimportData( final ImageLabelingModel lm, final int currentTimePoint )
	{
		try
		{
			/*
			 * We will update the spots using a comparison based on only the
			 * index images.
			 */
			final Labeling labeling = lm.labeling().get();
			final RandomAccessibleInterval< UnsignedShortType > novelIndexImg = ( RandomAccessibleInterval< UnsignedShortType > ) labeling.getIndexImg();

			// Collect ids of spots that have been modified. id = index - 1
			final Set< Integer > modifiedIDs = getModifiedIDs( novelIndexImg );
			final int nModified = modifiedIDs.size();

			if ( nModified == 0 )
				return;

			// Message the user.
			final String msg =
					"Commit the changes made to the\n"
							+ "segmentation in frame " + ( currentTimePoint + 1 ) + "?";
			final String title = "Commit edits to TrackMate";
			final int returnedValue = JOptionPane.showConfirmDialog(
					null,
					msg,
					title,
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					Icons.TRACKMATE_ICON );
			if ( returnedValue != JOptionPane.YES_OPTION )
				return;

			final Model model = trackmate.getModel();
			model.beginUpdate();
			try
			{
				// Map of previous spots against their ID:
				final SpotCollection spots = model.getSpots();
				final Map< Integer, Spot > previousSpotIDs = new HashMap<>();
				spots.iterable( currentTimePoint, true ).forEach( s -> previousSpotIDs.put( Integer.valueOf( s.ID() ), s ) );

				/*
				 * Get all the spots present in the new image. Because we
				 * specified the novel label image as 'quality' image, they have
				 * a quality value equal to the index in the label image (id+1).
				 */
				final List< Spot > novelSpots = getSpots( novelIndexImg );

				/*
				 * Map of novel spots against the ID taken from the index of the
				 * novel label image. Normally, this index, and hence the novel
				 * id, corresponds to the id of previous spots. If one of the
				 * novel spot has an id we cannot find in the previous spot
				 * list, it means that it is a new one.
				 *
				 * Careful! The user might have created several connected
				 * components with the same label in LabKit, which will result
				 * in having several spots with the same quality value. We don't
				 * want to loose them, so the map is that of a id to a list of
				 * spots.
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
						addNewSpot( novelSpotList );
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
						 * I know of them both. Treat the case as if the
						 * previous spot was modified.
						 */
						modifySpot( novelSpotList, previousSpot );
					}
				}
			}
			finally
			{
				model.endUpdate();
			}
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
		finally
		{
			disabler.reenable();
		}
	}

	private void modifySpot( final List< Spot > novelSpotList, final Spot previousSpot )
	{
		final Model model = trackmate.getModel();

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
			model.addSpotTo( s, Integer.valueOf( currentTimePoint ) );
		}
	}

	private void addNewSpot( final List< Spot > novelSpotList )
	{
		for ( final Spot spot : novelSpotList )
		{
			spot.putFeature( Spot.POSITION_T, currentTimePoint * dt );
			trackmate.getModel().addSpotTo( spot, Integer.valueOf( currentTimePoint ) );
		}
	}

	private final Set< Integer > getModifiedIDs( final RandomAccessibleInterval< UnsignedShortType > novelIndexImg )
	{
		final ConcurrentSkipListSet< Integer > modifiedIDs = new ConcurrentSkipListSet<>();
		LoopBuilder.setImages( novelIndexImg, previousIndexImg )
				.multiThreaded( false )
				.forEachPixel( ( c, p ) -> {
					final int ci = c.get();
					final int pi = p.get();
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

	private List< Spot > getSpots( final RandomAccessibleInterval< UnsignedShortType > rai )
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
		return MaskUtils.fromLabelingWithROI(
				labeling,
				labeling,
				calibration,
				simplify,
				rai );
	}

	public static final AbstractNamedAction getLaunchAction( final TrackMate trackmate )
	{
		return new AbstractNamedAction( "launch labkit editor" )
		{

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent ae )
			{
				new Thread( "TrackMate editor thread" )
				{
					@Override
					public void run()
					{
						final JRootPane parent = SwingUtilities.getRootPane( ( Component ) ae.getSource() );
						final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( parent, new Class[] { JLabel.class } );
						disabler.disable();
						try
						{
							final LabkitLauncher launcher = new LabkitLauncher( trackmate, disabler );
							launcher.launch();
						}
						catch ( final Exception e )
						{
							e.printStackTrace();
						}
					};
				}.start();
			}
		};
	}
}
