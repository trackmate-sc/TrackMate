package fiji.plugin.trackmate.gui.editor;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import org.scijava.Context;
import org.scijava.ui.behaviour.util.AbstractNamedAction;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.util.EverythingDisablerAndReenabler;
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
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import sc.fiji.labkit.ui.LabkitFrame;
import sc.fiji.labkit.ui.inputimage.DatasetInputImage;
import sc.fiji.labkit.ui.labeling.Labeling;
import sc.fiji.labkit.ui.models.DefaultSegmentationModel;

public class LabkitLauncher
{

	private final double[] calibration;

	private final TrackMate trackmate;

	private final EverythingDisablerAndReenabler disabler;

	private int currentTimePoint;

	public LabkitLauncher( final TrackMate trackmate, final EverythingDisablerAndReenabler disabler )
	{
		this.trackmate = trackmate;
		this.disabler = disabler;
		final ImagePlus imp = trackmate.getSettings().imp;
		this.calibration = TMUtils.getSpatialCalibration( imp );
	}

	/**
	 * Launches the Labkit editor.
	 * 
	 * @param singleTimePoint
	 *            if <code>true</code>, will launch the editor using only the
	 *            time-point currently displayed in the main view. Otherwise,
	 *            will edit all time-points.
	 */
	protected void launch( final boolean singleTimePoint )
	{
		final ImagePlus imp = trackmate.getSettings().imp;
		final DatasetInputImage input = makeInput( imp, singleTimePoint );
		final ImgPlus< UnsignedShortType > lblImgPlus = makeLblImage( imp, trackmate.getModel().getSpots(), singleTimePoint );

		// Make a labeling model from it.
		final Context context = TMUtils.getContext();
		final DefaultSegmentationModel model = new DefaultSegmentationModel( context, input );
		model.imageLabelingModel().labeling().set( Labeling.fromImg( lblImgPlus ) );

		// Store a copy.
		final ImgPlus< UnsignedShortType > previousIndexImg = lblImgPlus.copy();

		// Show LabKit.
		final LabkitFrame labkit = LabkitFrame.show( model, "Edit TrackMate data frame " + ( currentTimePoint + 1 ) );

		// Prepare re-importer.
		final double dt = imp.getCalibration().frameInterval;
		final LabkitImporter reimporter = new LabkitImporter( trackmate.getModel(), calibration, dt );
		labkit.onCloseListeners().addListener( () -> {
			try
			{
				@SuppressWarnings( "unchecked" )
				final RandomAccessibleInterval< UnsignedShortType > indexImg = ( RandomAccessibleInterval< UnsignedShortType > ) model.imageLabelingModel().labeling().get().getIndexImg();

				// Do we have something to reimport?
				final AtomicBoolean modified = new AtomicBoolean( false );
				LoopBuilder.setImages( previousIndexImg, indexImg )
						.multiThreaded()
						.forEachChunk( chunk -> {
							if ( modified.get() )
								return null;
							chunk.forEachPixel( ( p1, p2 ) -> {
								if ( p1.get() != p2.get() )
								{
									modified.set( true );
									return;
								}
							} );
							return null;
						} );
				if ( !modified.get() )
					return;

				// Message the user.
				final String msg = ( currentTimePoint < 0 )
						? "Commit the changes made to the\n"
								+ "segmentation in whole movie?"
						: "Commit the changes made to the\n"
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

				if ( currentTimePoint < 0 )
				{
					// All time-points.
					final Logger log = Logger.IJ_LOGGER;
					log.setStatus( "Re-importing from Labkit..." );
					for ( int t = 0; t < imp.getNFrames(); t++ )
					{
						final RandomAccessibleInterval< UnsignedShortType > novelIndexImgThisFrame = Views.hyperSlice( indexImg, 3, t );
						final RandomAccessibleInterval< UnsignedShortType > previousIndexImgThisFrame = Views.hyperSlice( previousIndexImg, 3, t );
						reimporter.reimport( novelIndexImgThisFrame, previousIndexImgThisFrame, t );
						log.setProgress( ++t / ( double ) imp.getNFrames() );
					}
					log.setStatus( "" );
					log.setProgress( 0. );
				}
				else
				{
					// Only one.
					reimporter.reimport( indexImg, previousIndexImg, currentTimePoint );
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
		} );
	}

	/**
	 * Creates a new {@link DatasetInputImage} from the specified
	 * {@link ImagePlus}. The embedded label image is empty.
	 * 
	 * @param imp
	 *            the input {@link ImagePlus}.
	 * @param singleTimePoint
	 *            if <code>true</code>, then the dataset will be created only
	 *            for the time-point currently displayed in the
	 *            {@link ImagePlus}. This time-point is then stored in the
	 *            {@link #currentTimePoint} field. If <code>false</code>, the
	 *            dataset is created for the whole movie and the
	 *            {@link #currentTimePoint} takes the value -1.
	 * @return a new {@link DatasetInputImage}.
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	private final DatasetInputImage makeInput( final ImagePlus imp, final boolean singleTimePoint )
	{
		final ImgPlus src = TMUtils.rawWraps( imp );
		// Possibly reslice for current time-point.
		final ImpBdvShowable showable;
		final ImgPlus inputImg;
		if ( singleTimePoint )
		{
			this.currentTimePoint = imp.getFrame() - 1;
			final int timeAxis = src.dimensionIndex( Axes.TIME );
			inputImg = ImgPlusViews.hyperSlice( src, timeAxis, currentTimePoint );
			showable = ImpBdvShowable.fromImp( inputImg, imp );
		}
		else
		{
			this.currentTimePoint = -1;
			showable = ImpBdvShowable.fromImp( imp );
			inputImg = src;
		}
		return new DatasetInputImage( inputImg, showable );
	}

	/**
	 * Prepare the label image for annotation.
	 * 
	 * @param imp
	 *            the source image plus.
	 * @param spots
	 *            the spot collection.
	 * @param singleTimePoint
	 *            if <code>true</code> we only annotate one time-point.
	 * @return a new {@link ImgPlus}.
	 */
	private ImgPlus< UnsignedShortType > makeLblImage( final ImagePlus imp, final SpotCollection spots, final boolean singleTimePoint )
	{
		// Axes.
		final AxisType[] axes = ( singleTimePoint )
				? new AxisType[] { Axes.X, Axes.Y, Axes.Z }
				: new AxisType[] { Axes.X, Axes.Y, Axes.Z, Axes.TIME };

		// N dimensions.
		final int nDims = singleTimePoint ? 3 : 4;

		// Dimensions.
		final long[] dims = new long[ nDims ];
		dims[ 0 ] = imp.getWidth();
		dims[ 1 ] = imp.getHeight();
		dims[ 2 ] = imp.getNSlices();
		if ( !singleTimePoint )
			dims[ 3 ] = imp.getNFrames();

		// Raw image.
		final Img< UnsignedShortType > lblImg = ArrayImgs.unsignedShorts( dims );

		// Calibration.
		final double[] c = TMUtils.getSpatialCalibration( imp );
		final double[] calibration = new double[ nDims ];
		for ( int i = 0; i < c.length; i++ )
			calibration[ i ] = c[ i ];
		if ( !singleTimePoint )
			calibration[ 3 ] = 1.;

		// Label image holder.
		final ImgPlus< UnsignedShortType > lblImgPlus = new ImgPlus<>( lblImg, "LblImg", axes, calibration );

		// Write spots in it with index = id + 1
		if ( singleTimePoint )
		{
			processFrame( lblImgPlus, spots, currentTimePoint );
		}
		else
		{
			for ( int t = 0; t < imp.getNFrames(); t++ )
			{
				final ImgPlus< UnsignedShortType > lblImgPlusThisFrame = ImgPlusViews.hyperSlice( lblImgPlus, 3, t );
				processFrame( lblImgPlusThisFrame, spots, t );
			}
		}
		return lblImgPlus;
	}

	private static final void processFrame( final ImgPlus< UnsignedShortType > lblImgPlus, final SpotCollection spots, final int t )
	{
		final Iterable< Spot > spotsThisFrame = spots.iterable( t, true );
		for ( final Spot spot : spotsThisFrame )
			spot.iterable( lblImgPlus ).forEach( p -> p.set( spot.ID() + 1 ) );
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
						// Is shift pressed?
						final int mod = ae.getModifiers();
						final boolean shiftPressed = ( mod & ActionEvent.SHIFT_MASK ) > 0;
						final boolean singleTimepoint = !shiftPressed;

						final JRootPane parent = SwingUtilities.getRootPane( ( Component ) ae.getSource() );
						final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( parent, new Class[] { JLabel.class } );
						disabler.disable();
						try
						{
							final LabkitLauncher launcher = new LabkitLauncher( trackmate, disabler );
							launcher.launch( singleTimepoint );
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
