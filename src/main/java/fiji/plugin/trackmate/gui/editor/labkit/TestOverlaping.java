package fiji.plugin.trackmate.gui.editor.labkit;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;

import org.scijava.Context;

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
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImgPlusViews;
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
import sc.fiji.labkit.ui.models.DefaultSegmentationModel;

public class TestOverlaping
{

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
		Img< UnsignedIntType > lblImg = ArrayImgs.unsignedInts( dims );
		if ( origin != null )
		{
			final RandomAccessibleInterval< UnsignedIntType > translated = Views.translate( lblImg, origin );
			lblImg = ImgView.wrap( translated );
		}

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
			processFrame( lblImgPlus, spots, timepoint, spotLabels, origin, labeling, colorGen );
		}
		else
		{
			final int timeDim = lblImgPlus.dimensionIndex( Axes.TIME );
			for ( int t = 0; t < imp.getNFrames(); t++ )
			{
				final ImgPlus< UnsignedIntType > lblImgPlusThisFrame = ImgPlusViews.hyperSlice( lblImgPlus, timeDim, t );
				final IntervalView< LabelingType< Label > > labelingThisFrame = Views.hyperSlice( labeling, timeDim, t );
				processFrame( lblImgPlusThisFrame, spots, t, spotLabels, origin, labelingThisFrame, colorGen );
			}
		}

		// Image component
		final DatasetInputImage input = TMLabKitModel.makeInput( imp, interval, timepoint );
		// Make the model.
		final Context context = TMUtils.getContext();
		final DefaultSegmentationModel lbModel = new DefaultSegmentationModel( context, input );
		lbModel.imageLabelingModel().labeling().set( labeling );

		final TMLabKitFrame labkit = new TMLabKitFrame( lbModel );
		labkit.setLocationRelativeTo( imp.getWindow() );
		labkit.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

		// Show
		labkit.setIconImage( Icons.TRACKMATE_ICON.getImage() );
		labkit.setSize( 1000, 800 );
		GuiUtils.positionWindow( labkit, imp.getWindow() );
		labkit.setTitle( "Test overlaping" );
		labkit.setVisible( true );
	}

	private static void processFrame(
			final ImgPlus< UnsignedIntType > lblImgPlus,
			final SpotCollection spots,
			final int t,
			final Map< Label, Spot > spotLabels,
			final long[] origin,
			final RandomAccessibleInterval< LabelingType< Label > > labeling,
			final FeatureColorGenerator< Spot > colorGen)
	{
		final RandomAccess< LabelingType< Label > > ra = labeling.randomAccess();

		// If we have a single timepoint, don't use -1 to retrieve spots.
		final int lt = t < 0 ? 0 : t;
		final Iterable< Spot > spotsThisFrame = spots.iterable( lt, true );
		if ( null == origin )
		{
			for ( final Spot spot : spotsThisFrame )
			{
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

	private static void boundingBox( final Spot spot, final ImgPlus< UnsignedIntType > img, final long[] min, final long[] max )
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
}
