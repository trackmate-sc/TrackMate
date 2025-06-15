package fiji.plugin.trackmate.gui.editor;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import org.scijava.ui.behaviour.util.AbstractNamedAction;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.editor.labkit.TMLabKitFrame;
import fiji.plugin.trackmate.gui.editor.labkit.TMLabKitModel;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.util.EverythingDisablerAndReenabler;
import fiji.plugin.trackmate.util.TMUtils;
import ij.ImagePlus;
import ij.gui.Roi;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import sc.fiji.labkit.ui.labeling.Labeling;

public class LabkitLauncher< T extends IntegerType< T > & NativeType< T > >
{

	private static final boolean ENABLE_SPOT_EDITOR = true;

	private final double[] calibration;

	private final TrackMate trackmate;

	private final EverythingDisablerAndReenabler disabler;

	private final DisplaySettings ds;

	private final boolean is3D;

	private final boolean isSingleTimePoint;

	private static boolean simplify = true;

	public LabkitLauncher( final TrackMate trackmate, final DisplaySettings ds, final EverythingDisablerAndReenabler disabler )
	{
		this.trackmate = trackmate;
		this.ds = ds;
		this.disabler = disabler;
		final ImagePlus imp = trackmate.getSettings().imp;
		this.calibration = TMUtils.getSpatialCalibration( imp );
		this.is3D = !DetectionUtils.is2D( imp );
		this.isSingleTimePoint = imp.getNFrames() <= 1;
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
		int timepoint;
		if ( !singleTimePoint )
			timepoint = -1;
		else
			timepoint = imp.getT() - 1;
		final Model trackmateModel = trackmate.getModel();
		final Interval interval = createROIInterval( imp );
		final TMLabKitModel model = TMLabKitModel.createModel(
				imp,
				trackmateModel,
				ds,
				interval,
				timepoint );

		// Store a copy of the labeling index image.
		final Labeling labeling = model.imageLabelingModel().labeling().get();
		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< T > previousIndexImg = copy( ( RandomAccessibleInterval< T > ) labeling.getIndexImg() );

		// Show LabKit.
		String title = "Editing TrackMate data for " + imp.getShortTitle();
		if ( singleTimePoint )
			title += "at frame " + ( timepoint + 1 );

		final TMLabKitFrame labkit = new TMLabKitFrame( model, title );
		labkit.setLocationRelativeTo( imp.getWindow() );
		labkit.setVisible( true );

		// Prepare re-importer.
		final Map< Integer, Spot > spotLabels = model.getLabelMap();
		final double dt = imp.getCalibration().frameInterval;
		labkit.onCloseListeners().addListener( () -> {
			@SuppressWarnings( "unchecked" )
			final RandomAccessibleInterval< T > indexImg = ( RandomAccessibleInterval< T > ) model.imageLabelingModel().labeling().get().getIndexImg();
			reimport( indexImg, previousIndexImg, spotLabels, timepoint, dt );
		} );
	}

	private static Interval createROIInterval( final ImagePlus imp )
	{
		final Roi roi = imp.getRoi();
		if ( roi == null )
			return null;

		final boolean is3D = !DetectionUtils.is2D( imp );
		final long[] min = new long[ is3D ? 3 : 2 ];
		final long[] max = new long[ min.length ];

		min[ 0 ] = roi.getBounds().x;
		max[ 0 ] = roi.getBounds().x + roi.getBounds().width - 1;
		min[ 1 ] = roi.getBounds().y;
		max[ 1 ] = roi.getBounds().y + roi.getBounds().height - 1;
		if ( is3D )
		{
			min[ 2 ] = 0;
			max[ 2 ] = imp.getNSlices();
		}
		return new FinalInterval( min, max );
	}

	private void reimport(
			final RandomAccessibleInterval< T > indexImg,
			final RandomAccessibleInterval< T > previousIndexImg,
			final Map< Integer, Spot > spotLabels,
			final int timepoint,
			final double dt )
	{
		new Thread( "TrackMate-LabKit-Importer-thread" )
		{
			@Override
			public void run()
			{
				try
				{
					// Do we have something to reimport?
					final AtomicBoolean modified = new AtomicBoolean( false );
					LoopBuilder.setImages( previousIndexImg, indexImg )
							.multiThreaded()
							.forEachChunk( chunk -> {
								if ( modified.get() )
									return null;
								chunk.forEachPixel( ( p1, p2 ) -> {
									if ( p1.getInteger() != p2.getInteger() )
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
					final String msg = ( isSingleTimePoint )
							? "Commit the changes made to the\n"
									+ "segmentation in the image?"
							: ( timepoint < 0 )
									? "Commit the changes made to the\n"
											+ "segmentation in whole movie?"
									: "Commit the changes made to the\n"
											+ "segmentation in frame " + ( timepoint + 1 ) + "?";
					final String title = "Commit edits to TrackMate";
					final JCheckBox chkbox = new JCheckBox( "Simplify the contours of modified spots" );
					chkbox.setSelected( simplify );
					final Object[] objs = new Object[] { msg, new JSeparator(), chkbox };
					final int returnedValue = JOptionPane.showConfirmDialog(
							null,
							objs,
							title,
							JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE,
							Icons.TRACKMATE_ICON );
					if ( returnedValue != JOptionPane.YES_OPTION )
						return;

					simplify = chkbox.isSelected();
					final LabkitImporter< T > reimporter = new LabkitImporter<>( trackmate.getModel(), calibration, dt, simplify );

					// Possibly determine the number of time-points to parse.
					final int timeDim = ( isSingleTimePoint )
							? -1
							: ( is3D ) ? 3 : 2;
					final long nTimepoints = ( timeDim < 0 )
							? 0
							: indexImg.numDimensions() > timeDim ? indexImg.dimension( timeDim ) : 0;

					if ( timepoint < 0 && nTimepoints > 1 )
					{
						// All time-points.
						final Logger log = Logger.IJ_LOGGER;
						log.setStatus( "Re-importing from Labkit..." );
						for ( int t = 0; t < nTimepoints; t++ )
						{
							// The spots of this time-point:
							final Map< Integer, Spot > spotLabelsThisFrame = new HashMap<>();
							for ( final Integer label : spotLabels.keySet() )
							{
								final Spot spot = spotLabels.get( label );
								if ( spot.getFeature( Spot.FRAME ).intValue() == t )
									spotLabelsThisFrame.put( label, spot );
							}

							final RandomAccessibleInterval< T > novelIndexImgThisFrame = Views.hyperSlice( indexImg, timeDim, t );
							final RandomAccessibleInterval< T > previousIndexImgThisFrame = Views.hyperSlice( previousIndexImg, timeDim, t );
							reimporter.reimport( novelIndexImgThisFrame, previousIndexImgThisFrame, t, spotLabelsThisFrame );
							log.setProgress( t / ( double ) nTimepoints );
						}
						log.setStatus( "" );
						log.setProgress( 0. );
					}
					else
					{
						// Only one.
						final int localT = Math.max( 0, timepoint );
						reimporter.reimport( indexImg, previousIndexImg, localT, spotLabels );
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
		}.start();
	}

	private Img< T > copy( final RandomAccessibleInterval< T > in )
	{
		final ImgFactory< T > factory = Util.getArrayOrCellImgFactory( in, in.getType() );
		final Img< T > out = factory.create( in );
		LoopBuilder.setImages( in, out )
				.multiThreaded()
				.forEachPixel( ( i, o ) -> o.setInteger( i.getInteger() ) );
		return out;
	}

	public static final AbstractNamedAction getLaunchAction( final TrackMate trackmate, final DisplaySettings ds )
	{
		final AbstractNamedAction action = new AbstractNamedAction( "launch labkit editor" )
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
							@SuppressWarnings( "rawtypes" )
							final LabkitLauncher launcher = new LabkitLauncher( trackmate, ds, disabler );
							launcher.launch( singleTimepoint );
						}
						catch ( final Exception e )
						{
							disabler.reenable();
							e.printStackTrace();
						}
					};
				}.start();
			}
		};
		// Disable if the image is not 2D.
		if ( !DetectionUtils.is2D( trackmate.getSettings().imp ) )
			action.setEnabled( false );
		else
			action.setEnabled( ENABLE_SPOT_EDITOR );

		return action;
	}

	public static void main( final String[] args )
	{
//		final String filename = "FakeTracks-rescaled.xml";
		final String filename = "/Users/tinevez/Desktop/2-Generate a tracking ground truth/results/gmanina_movie_frames_8_20-GT.xml";

		final TmXmlReader reader = new TmXmlReader( new File( filename ) );
		if ( !reader.isReadingOk() )
		{
			System.out.println( reader.getErrorMessage() );
			return;
		}
		final Model model = reader.getModel();
		final ImagePlus image = reader.readImage();
		final Settings settings = reader.readSettings( image );
		final DisplaySettings ds = reader.getDisplaySettings();
		final TrackMate trackmate = new TrackMate( model, settings );

		final EverythingDisablerAndReenabler disabler = null;
		final LabkitLauncher< ? > launcher = new LabkitLauncher<>( trackmate, ds, disabler );
		launcher.launch( true );
	}
}
