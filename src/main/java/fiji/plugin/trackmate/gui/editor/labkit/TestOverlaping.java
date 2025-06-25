package fiji.plugin.trackmate.gui.editor.labkit;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import org.scijava.Context;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.util.TMUtils;
import ij.ImagePlus;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.labeling.Labeling;
import sc.fiji.labkit.ui.labeling.Labelings;

public class TestOverlaping
{

	private static boolean simplify = true;

	@SuppressWarnings( "unused" )
	public static void main( final String[] args )
	{
		final String filename = "/Users/tinevez/Library/CloudStorage/GoogleDrive-jeanyves.tinevez@gmail.com/My Drive/TrackMate/v8/DevSamples/TestOverlapingSpots.xml";
		final int timepoint = 0;
		final Interval interval = null;

		final TmXmlReader reader = new TmXmlReader( new File( filename ) );
		if ( !reader.isReadingOk() )
		{
			System.out.println( reader.getErrorMessage() );
			return;
		}

		final ImagePlus imp = reader.readImage();
		final Settings settings = reader.readSettings( imp );
		final Model model = reader.getModel();
		final DisplaySettings displaySettings = reader.getDisplaySettings();

		// Create the LabKit model.
		final Context context = TMUtils.getContext();
		final TMLabKitModel lbModel = TMLabKitModel.create( model, imp, interval, displaySettings, timepoint, context );

		// Create the UI for editing.
		final TMLabKitFrame labkit = new TMLabKitFrame( lbModel );
		GuiUtils.positionWindow( labkit, imp.getWindow() );
		labkit.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

		// Make a copy of the initial index image
		final Labeling labeling = lbModel.imageLabelingModel().labeling().get();
		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final RandomAccessibleInterval< UnsignedIntType > previousIndexImg = copy( ( RandomAccessibleInterval ) labeling.getIndexImg() );

		labkit.onCloseListeners().addListener( () -> {
			System.out.println( "REIMPORTING" ); // DEBUG
			final Map< Label, Spot > spotLabels = lbModel.imageLabelingModel().mapping().get();
			reimport( lbModel, imp, previousIndexImg, timepoint, model );
		} );

		// Show
		labkit.setIconImage( Icons.TRACKMATE_ICON.getImage() );
		labkit.setSize( 1000, 800 );
		GuiUtils.positionWindow( labkit, imp.getWindow() );
		labkit.setTitle( "Test overlaping" );
		labkit.setVisible( true );

		// DEBUG
		// Programmatically modify the labeling.
		final Label label = labeling.getLabels().get( 0 );
		Views
				.interval( labeling, Intervals.createMinSize( 10, 90, 11, 11 ) )
				.forEach( p -> p.add( label ) );
		labkit.dispose();
	}

	private static void reimport(
			final TMLabKitModel lbModel,
			final ImagePlus imp,
			final RandomAccessibleInterval< UnsignedIntType > previousIndexImg,
			final int timepoint,
			final Model model )
	{
		final Labeling labeling = lbModel.imageLabelingModel().labeling().get();
		final Map< Label, Spot > mapping = lbModel.imageLabelingModel().mapping().get();

		final double dt = imp.getCalibration().frameInterval;
		final boolean isSingleTimePoint = imp.getNFrames() <= 1;
		final boolean is3D = !DetectionUtils.is2D( imp );

		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< UnsignedIntType > indexImg = ( RandomAccessibleInterval< UnsignedIntType > ) labeling.getIndexImg();
		new Thread( "TrackMate-LabKit-Importer-thread" )
		{
			@Override
			public void run()
			{
				try
				{
					// Do we have something to reimport?
					final boolean modified = TMLabKitUtils.isDifferent( previousIndexImg, indexImg );
					if ( !modified )
					{
						System.out.println( "No change detected." ); // DEBUG
						return;
					}
					System.out.println( "Change detected." ); // DEBUG

					// Message the user.
//					final String msg = ( isSingleTimePoint )
//							? "Commit the changes made to the\n"
//									+ "segmentation in the image?"
//							: ( timepoint < 0 )
//									? "Commit the changes made to the\n"
//											+ "segmentation in whole movie?"
//									: "Commit the changes made to the\n"
//											+ "segmentation in frame " + ( timepoint + 1 ) + "?";
//					final String title = "Commit edits to TrackMate";
//					final JCheckBox chkbox = new JCheckBox( "Simplify the contours of modified spots" );
//					chkbox.setSelected( simplify );
//					final Object[] objs = new Object[] { msg, new JSeparator(), chkbox };
//					final int returnedValue = JOptionPane.showConfirmDialog(
//							null,
//							objs,
//							title,
//							JOptionPane.YES_NO_OPTION,
//							JOptionPane.QUESTION_MESSAGE,
//							Icons.TRACKMATE_ICON );
//					if ( returnedValue != JOptionPane.YES_OPTION )
//						return;
//
//					simplify = chkbox.isSelected();
					simplify = true;
					final double[] calibration = TMUtils.getSpatialCalibration( imp );
					final LabkitImporter2< UnsignedIntType > reimporter = new LabkitImporter2<>( model, calibration, dt, simplify );

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
						final List< Labeling > slices = Labelings.slices( labeling );
						final Logger log = Logger.IJ_LOGGER;
						log.setStatus( "Re-importing from Labkit..." );
						for ( int t = 0; t < nTimepoints; t++ )
						{
							// The spots of this time-point:
							final Map< Label, Spot > spotLabelsThisFrame = new HashMap<>();
							for ( final Label label : mapping.keySet() )
							{
								final Spot spot = mapping.get( label );
								if ( spot.getFeature( Spot.FRAME ).intValue() == t )
									spotLabelsThisFrame.put( label, spot );
							}

							final Labeling labelingThisFrame = slices.get( t );
							final RandomAccessibleInterval< UnsignedIntType > previousIndexImgThisFrame = Views.hyperSlice( previousIndexImg, timeDim, t );
							reimporter.reimport( labelingThisFrame, previousIndexImgThisFrame, t, spotLabelsThisFrame );
							log.setProgress( t / ( double ) nTimepoints );
						}
						log.setStatus( "" );
						log.setProgress( 0. );
					}
					else
					{
						// Only one.
						final int localT = Math.max( 0, timepoint );
						reimporter.reimport( labeling, previousIndexImg, localT, mapping );
					}
				}
				catch ( final Exception e )
				{
					e.printStackTrace();
				}
				finally
				{
//					if ( disabler != null )
//						disabler.reenable();
				}
				System.out.println( "Resulting TrackMate model:" ); // DEBUG
				System.out.println( model.getSpots() ); // DEBUG

			}
		}.start();

	}


	private static final Img< UnsignedIntType > copy( final RandomAccessibleInterval< UnsignedIntType > in )
	{
		final ImgFactory< UnsignedIntType > factory = Util.getArrayOrCellImgFactory( in, in.getType() );
		final Img< UnsignedIntType > out = factory.create( in );
		LoopBuilder.setImages( in, out )
				.multiThreaded()
				.forEachPixel( ( i, o ) -> o.setInteger( i.getInteger() ) );
		return out;
	}

}
