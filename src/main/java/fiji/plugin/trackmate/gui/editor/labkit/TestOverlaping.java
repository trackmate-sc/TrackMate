package fiji.plugin.trackmate.gui.editor.labkit;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;

import org.scijava.Context;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.util.SpotUtil;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import sc.fiji.labkit.ui.inputimage.DatasetInputImage;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.labeling.Labeling;
import sc.fiji.labkit.ui.labeling.Labelings;
import sc.fiji.labkit.ui.models.DefaultSegmentationModel;
import sc.fiji.labkit.ui.utils.Notifier;

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

		final boolean is3D = !DetectionUtils.is2D( imp );
		final boolean singleTimePoint = timepoint >= 0;

		// Axes.
		final AxisType[] axes = ( is3D )
				? ( singleTimePoint )
						? new AxisType[] { Axes.X, Axes.Y, Axes.Z }
						: new AxisType[] { Axes.X, Axes.Y, Axes.Z, Axes.TIME }
				: ( singleTimePoint )
						? new AxisType[] { Axes.X, Axes.Y }
						: new AxisType[] { Axes.X, Axes.Y, Axes.TIME };

		// N dimensions.
		final int nDims = is3D
				? singleTimePoint ? 3 : 4
				: singleTimePoint ? 2 : 3;

		// Dimensions.
		final long[] dims = new long[ nDims ];
		int dim = 0;
		dims[ dim++ ] = imp.getWidth();
		dims[ dim++ ] = imp.getHeight();
		if ( is3D )
			dims[ dim++ ] = imp.getNSlices();
		if ( !singleTimePoint )
			dims[ dim++ ] = imp.getNFrames();

		// Possibly crop.
		final long[] origin;
		if ( interval != null )
		{
			dims[ 0 ] = interval.dimension( 0 );
			dims[ 1 ] = interval.dimension( 1 );
			origin = new long[ dims.length ];
			origin[ 0 ] = interval.min( 0 );
			origin[ 1 ] = interval.min( 1 );
		}
		else
		{
			origin = null;
		}

		// Raw image.
		final Img< UnsignedIntType > lblImg = ArrayImgs.unsignedInts( dims );
		// Calibration.
		final double[] c = TMUtils.getSpatialCalibration( imp );
		final double[] calibration = new double[ nDims ];
		dim = 0;
		calibration[ dim++ ] = c[ 0 ];
		calibration[ dim++ ] = c[ 1 ];
		if ( is3D )
			calibration[ dim++ ] = c[ 2 ];
		if ( !singleTimePoint )
			calibration[ dim++ ] = 1.;

		// Label image holder.
		final ImgPlus< UnsignedIntType > lblImgPlus = new ImgPlus<>( lblImg, "LblImg", axes, calibration );
		final Labeling labeling = Labeling.fromImg( lblImgPlus );

		// Labels color to match TrackMate display.
		final DisplaySettings defaultStyle = DisplaySettings.defaultStyle().copy();
		defaultStyle.setSpotColorBy( TrackMateObject.SPOTS, FeatureUtils.USE_RANDOM_COLOR_KEY );
		final DisplaySettings ds = ( displaySettings == null )
				? defaultStyle
				: displaySettings;
		final FeatureColorGenerator< Spot > colorGen = FeatureUtils.createSpotColorGenerator( model, ds );

		// Write spots in it with index = id + 1 and build a map index -> spot.
		final SpotCollection spots = model.getSpots();
		final Map< Label, Spot > spotLabels = new HashMap<>();
		if ( singleTimePoint )
		{
			processFrame( labeling, lblImgPlus, spots, timepoint, origin, colorGen, spotLabels );
		}
		else
		{
			final int timeDim = lblImgPlus.dimensionIndex( Axes.TIME );
			for ( int t = 0; t < imp.getNFrames(); t++ )
			{
				final ImgPlus< UnsignedIntType > lblImgPlusThisFrame = ImgPlusViews.hyperSlice( lblImgPlus, timeDim, t );
				final IntervalView< LabelingType< Label > > labelingThisFrame = Views.hyperSlice( labeling, timeDim, t );
				processFrame( labeling, lblImgPlusThisFrame, spots, t, origin, colorGen, spotLabels );
			}
		}

		// Image component
		final DatasetInputImage input = TMLabKitModel.makeInput( imp, interval, timepoint );
		// Make the model.
		final Context context = TMUtils.getContext();
		final DefaultSegmentationModel lbModel = new DefaultSegmentationModel( context, input );
		lbModel.imageLabelingModel().labeling().set( labeling );

		// Create the UI for editing.
		final TMLabKitFrame labkit = new TMLabKitFrame( lbModel );
		labkit.setLocationRelativeTo( imp.getWindow() );
		labkit.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

		// Notify our listeners when the window is closed.
		final Notifier onCloseListeners = new Notifier();
		labkit.addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosed( final WindowEvent e )
			{
				onCloseListeners.notifyListeners();
			}
		} );

		// Make a copy of the initial index image
		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final RandomAccessibleInterval< UnsignedIntType > previousIndexImg = copy( ( RandomAccessibleInterval ) labeling.getIndexImg() );

		onCloseListeners.addListener( () -> {
			System.out.println( "REIMPORTING" ); // DEBUG
			reimport( imp, labeling, previousIndexImg, spotLabels, timepoint, model );
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
			final ImagePlus imp,
			final Labeling labeling,
			final RandomAccessibleInterval< UnsignedIntType > previousIndexImg,
			final Map< Label, Spot > spotLabels,
			final int timepoint,
			final Model model )
	{
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
					{
						System.out.println( "No change detected." ); // DEBUG
						return;
					}
					System.out.println( "Change detected." ); // DEBUG

					// Message the user.
					final String msg = ( isSingleTimePoint )
							? "Commit the changes made to the\n"
									+ "segmentation in the image?"
							: ( timepoint < 0 )
									? "Commit the changes made to the\n"
											+ "segmentation in whole movie?"
									: "Commit the changes made to the\n"
											+ "segmentation in frame " + ( timepoint + 1 ) + "?";
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
							for ( final Label label : spotLabels.keySet() )
							{
								final Spot spot = spotLabels.get( label );
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
						reimporter.reimport( labeling, previousIndexImg, localT, spotLabels );
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


	/**
	 *
	 * @param labeling
	 * @param lblImgPlus
	 *            used as a dummy calibrated image to iterate properly over spot
	 *            coordinates.
	 * @param spots
	 * @param t
	 * @param origin
	 * @param colorGen
	 * @param spotLabels
	 */
	private static void processFrame(
			final Labeling labeling,
			final ImgPlus< UnsignedIntType > lblImgPlus,
			final SpotCollection spots,
			final int t,
			final long[] origin,
			final FeatureColorGenerator< Spot > colorGen,
			final Map< Label, Spot > spotLabels )
	{
		final RandomAccess< LabelingType< Label > > ra = labeling.randomAccess();

		// If we have a single timepoint, don't use -1 to retrieve spots.
		final int lt = t < 0 ? 0 : t;
		final Iterable< Spot > spotsThisFrame = spots.iterable( lt, true );
		if ( null == origin )
		{
			for ( final Spot spot : spotsThisFrame )
			{
				final Label label = labeling.addLabel( spot.getName() );
				label.setColor( new ARGBType( colorGen.color( spot ).getRGB() ) );
				final Cursor< UnsignedIntType > c = SpotUtil.iterable( spot, lblImgPlus ).localizingCursor();
				while ( c.hasNext() )
				{
					c.fwd();
					ra.setPosition( c );
					ra.get().add( label );
				}
				spotLabels.put( label, spot );
			}
		}
		else
		{
			final long[] min = new long[ 2 ];
			final long[] max = new long[ 2 ];
			final FinalInterval spotBB = FinalInterval.wrap( min, max );
			final FinalInterval imgBB = Intervals.createMinSize( origin[ 0 ], origin[ 1 ], lblImgPlus.dimension( 0 ), lblImgPlus.dimension( 1 ) );
			for ( final Spot spot : spotsThisFrame )
			{
				boundingBox( spot, lblImgPlus, min, max );
				// Inside? We skip if we touch the border.
				final boolean isInside = Intervals.contains( imgBB, spotBB );
				if ( !isInside )
					continue;

				final Label label = new Label( spot.getName(), new ARGBType( colorGen.color( spot ).getRGB() ) );
				final Cursor< UnsignedIntType > c = SpotUtil.iterable( spot, lblImgPlus ).localizingCursor();
				while ( c.hasNext() )
				{
					c.fwd();
					ra.setPosition( c );
					ra.get().add( label );
				}
				spotLabels.put( label, spot );
			}
		}
	}

	private static final void boundingBox( final Spot spot, final ImgPlus< UnsignedIntType > img, final long[] min, final long[] max )
	{
		final double[] calibration = TMUtils.getSpatialCalibration( img );
		final SpotRoi roi = spot.getRoi();
		if ( roi == null )
		{
			final double cx = spot.getDoublePosition( 0 );
			final double cy = spot.getDoublePosition( 1 );
			final double r = spot.getFeature( Spot.RADIUS ).doubleValue();
			min[ 0 ] = ( long ) Math.floor( ( cx - r ) / calibration[ 0 ] );
			min[ 1 ] = ( long ) Math.floor( ( cy - r ) / calibration[ 1 ] );
			max[ 0 ] = ( long ) Math.ceil( ( cx + r ) / calibration[ 0 ] );
			max[ 1 ] = ( long ) Math.ceil( ( cy + r ) / calibration[ 1 ] );
		}
		else
		{
			final double[] x = roi.toPolygonX( calibration[ 0 ], 0, spot.getDoublePosition( 0 ), 1. );
			final double[] y = roi.toPolygonY( calibration[ 1 ], 0, spot.getDoublePosition( 1 ), 1. );
			min[ 0 ] = ( long ) Math.floor( Util.min( x ) );
			min[ 1 ] = ( long ) Math.floor( Util.min( y ) );
			max[ 0 ] = ( long ) Math.ceil( Util.max( x ) );
			max[ 1 ] = ( long ) Math.ceil( Util.max( y ) );
		}

		min[ 0 ] = Math.max( 0, min[ 0 ] );
		min[ 1 ] = Math.max( 0, min[ 1 ] );
		final long width = img.min( img.dimensionIndex( Axes.X ) ) + img.dimension( img.dimensionIndex( Axes.X ) );
		final long height = img.min( img.dimensionIndex( Axes.Y ) ) + img.dimension( img.dimensionIndex( Axes.Y ) );
		max[ 0 ] = Math.min( width, max[ 0 ] );
		max[ 1 ] = Math.min( height, max[ 1 ] );
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
