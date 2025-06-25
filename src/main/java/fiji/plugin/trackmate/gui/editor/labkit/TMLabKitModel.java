package fiji.plugin.trackmate.gui.editor.labkit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.scijava.Context;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.gui.editor.ImpBdvShowable;
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
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.util.Util;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;
import sc.fiji.labkit.ui.inputimage.DatasetInputImage;
import sc.fiji.labkit.ui.inputimage.InputImage;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.labeling.Labeling;
import sc.fiji.labkit.ui.models.SegmentationModel;
import sc.fiji.labkit.ui.models.SegmenterListModel;

/**
 * A custom LabKit model created from TrackMate data.
 */
public class TMLabKitModel implements SegmentationModel
{

	private final Context context;

	private final TMImageLabelingModel imageLabelingModel;


	protected TMLabKitModel( final Context context, final InputImage inputImage )
	{
		this.context = context;
		this.imageLabelingModel = new TMImageLabelingModel( inputImage );
	}

	@Override
	public Context context()
	{
		return context;
	}

	@Override
	public TMImageLabelingModel imageLabelingModel()
	{
		return imageLabelingModel;
	}

	@Override
	public SegmenterListModel segmenterList()
	{
		throw new UnsupportedOperationException( "TrackMate editor does not have segmenting capabilities" );
	}

	@Deprecated
	public < T extends IntegerType< T > & NativeType< T > >
			List< RandomAccessibleInterval< T > > getSegmentations( final T type )
	{
		throw new UnsupportedOperationException( "TrackMate editor does not have segmenting capabilities" );
	}

	@Deprecated
	public List< RandomAccessibleInterval< FloatType > > getPredictions()
	{
		throw new UnsupportedOperationException( "TrackMate editor does not have segmenting capabilities" );
	}

	public boolean isTrained()
	{
		throw new UnsupportedOperationException( "TrackMate editor does not have segmenting capabilities" );
	}

	/**
	 * Creates a LabKit {@link TMLabKitModel} from the specified TrackMate
	 * model.
	 * <p>
	 * Only 2D is supported for now. If this method is called with a 3D input,
	 * an exception is thrown.
	 *
	 * @param model
	 *            the model to read the spots from.
	 * @param imp
	 *            the image the model was created on. This is used to get the
	 *            dimensionality for the labeling, the calibration, etc.
	 * @param interval
	 *            the interval in the source image to restrict the import. Only
	 *            the first 2 dimensions of the interval are used (X&Y). The
	 *            returned output will have the size of this interval. Only the
	 *            spots that are fully within this interval are imported. If
	 *            <code>null</code>, all the spots are imported and the output
	 *            has the size of the input image.
	 * @param displaySettings
	 *            a {@link DisplaySettings} to create the spot colors from. If
	 *            <code>null</code>, spots are colored randomly.
	 * @param timepoint
	 *            the time-point to import. If negative, all time-points will be
	 *            imported.
	 * @param context
	 *            the {@link Context} to use to create the model.
	 * @return a new {@link TMLabKitModel}. Will be 2D (X&Y), and possibly 3D
	 *         (with time) if all time-points are imported. Its XY size will be
	 *         the size of the specified interval (or the size of the input
	 *         image if the interval is null).
	 * @throws UnsupportedOperationException
	 *             if this method is called with a 3D input.
	 */
	public final static TMLabKitModel create(
			final Model model,
			final ImagePlus imp,
			final Interval interval,
			final DisplaySettings displaySettings,
			final int timepoint,
			final Context context )
	{
		// Create the labeling.
		final Pair< Labeling, Map< Label, Spot > > out = createLabeling( model, imp, interval, displaySettings, timepoint );
		final Labeling labeling = out.getA();
		final Map< Label, Spot > mapping = out.getB();
		// Create the image dataset.
		final DatasetInputImage input = makeInput( imp, interval, timepoint );
		// Make the model.
		final TMLabKitModel lbModel = new TMLabKitModel( context, input );
		lbModel.imageLabelingModel().labeling().set( labeling );
		lbModel.imageLabelingModel().mapping().set( mapping );
		return lbModel;
	}

	/**
	 * Creates a new {@link DatasetInputImage} from the specified
	 * {@link ImagePlus}. The embedded label image is not set.
	 *
	 * @param imp
	 *            the input {@link ImagePlus}.
	 * @param interval
	 *            the interval in the input to include in the dataset image. If
	 *            <code>null</code>, the whole input is used. The interval must
	 *            only include spatial axes (not the channel axis, not the time
	 *            axis).
	 * @param timepoint
	 *            the time-point to extract. If strictly negative, all the
	 *            frames of the source image will be included in the image. If
	 *            positive or 0, the image will only contain the specified
	 *            time-point.
	 * @return a new {@link DatasetInputImage}.
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public final static DatasetInputImage makeInput(
			final ImagePlus imp,
			final Interval interval,
			final int timepoint )
	{
		final ImgPlus all = TMUtils.rawWraps( imp );
		final int timeAxis = all.dimensionIndex( Axes.TIME );
		final boolean singleTimePoint = timepoint >= 0;

		ImgPlus view;
		if ( singleTimePoint && timeAxis >= 0 )
		{
			view = ImgPlusViews.hyperSlice( all, timeAxis, timepoint );
		}
		else
		{
			view = all;
		}

		// Crop if we have a ROI.
		final ImgPlus fov;
		if ( interval != null )
		{
			final long[] min = view.minAsLongArray();
			final long[] max = view.maxAsLongArray();
			final int xAxis = view.dimensionIndex( Axes.X );
			final int yAxis = view.dimensionIndex( Axes.Y );
			min[ xAxis ] = interval.min( xAxis );
			min[ yAxis ] = interval.min( yAxis );
			max[ xAxis ] = interval.max( xAxis );
			max[ yAxis ] = interval.max( yAxis );
			final RandomAccessibleInterval crop = Views.interval( view, min, max );
			fov = ImgPlus.wrapRAI( crop );
			// Copy time axis if we have it.
			if ( !singleTimePoint && timeAxis >= 0 )
				fov.setAxis( view.axis( timeAxis ), timeAxis );
			// Copy channel axis if we have it.
			final int cAxis = view.dimensionIndex( Axes.CHANNEL );
			if ( cAxis > 0 )
				fov.setAxis( view.axis( cAxis ), cAxis );
		}
		else
		{
			fov = view;
		}
		final ImpBdvShowable showable = ImpBdvShowable.fromImp( fov, imp );
		return new DatasetInputImage( fov, showable );
	}

	/**
	 * Creates a LabKit {@link Labeling} from the specified TrackMate model.
	 * <p>
	 * Only 2D is supported for now. If this method is called with a 3D input,
	 * an exception is thrown.
	 *
	 * @param model
	 *            the model to read the spots from.
	 * @param imp
	 *            the image the model was created on. This is used to get the
	 *            dimensionality for the labeling, the calibration, etc.
	 * @param interval
	 *            the interval in the source image to restrict the import. Only
	 *            the first 2 dimensions of the interval are used (X&Y). The
	 *            returned labeling will have the size of this interval. Only
	 *            the spots that are fully within this interval are imported. If
	 *            <code>null</code>, all the spots are imported and the labeling
	 *            has the size of the input image.
	 * @param displaySettings
	 *            a {@link DisplaySettings} to create the spot colors from. If
	 *            <code>null</code>, spots are colored randomly.
	 * @param timepoint
	 *            the time-point to import. If negative, all time-points will be
	 *            imported.
	 * @return a new {@link Labeling} and the a mapping of labels to spots. Will
	 *         be 2D (X&Y), and possibly 3D (with time) if all time-points are
	 *         imported. Its XY size will be the size of the specified interval
	 *         (or the size of the input image if the interval is null).
	 * @throws UnsupportedOperationException
	 *             if this method is called with a 3D input.
	 */
	public static final Pair< Labeling, Map< Label, Spot > > createLabeling(
			final Model model,
			final ImagePlus imp,
			final Interval interval,
			final DisplaySettings displaySettings,
			final int timepoint )
	{
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

		// For now: 2D only.
		if ( is3D )
			throw new UnsupportedOperationException( "Using LabKit with TrackMate is only supported for 2D for now." );

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
				processFrame( labeling, lblImgPlusThisFrame, spots, t, origin, colorGen, spotLabels );
			}
		}
		return new ValuePair< Labeling, Map< Label, Spot > >( labeling, spotLabels );
	}

	/**
	 * Adds the spots of a {@link SpotCollection} to the provided
	 * {@link Labeling}.
	 * <p>
	 *
	 * @param labeling
	 *            the labeling to write to. One {@link Label} is created per
	 *            spot, with the name taken from the spot name, and the color
	 *            taken from the specified color generator.
	 * @param lblImgPlus
	 *            used as a dummy calibrated image to iterate properly over spot
	 *            coordinates.
	 * @param spots
	 *            the {@link SpotCollection} to read the spot from.
	 * @param t
	 *            the time-point to import the spot from. If negative, all
	 *            time-points will be imported.
	 * @param origin
	 *            the coordinates of the origin of the interval to import.
	 * @param colorGen
	 *            the spot color generator.
	 * @param spotLabels
	 *            a map that links the created labels to the spots imported.
	 *            This map is being written by this method.
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

		// If we have a single time-point, don't use -1 to retrieve spots.
		final int lt = t < 0 ? 0 : t;
		final Iterable< Spot > spotsThisFrame = spots.iterable( lt, true );
		if ( null == origin )
		{
			final RandomAccess< LabelingType< Label > > ra = labeling.randomAccess();
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
			final RandomAccess< LabelingType< Label > > ra = labeling.randomAccess();
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

}
