package fiji.plugin.trackmate.gui.editor.labkit;

import java.util.Collections;
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
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImgPlusViews;
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

	private final Map< Integer, Spot > spotLabels;

	/**
	 * Creates a new LabKit model where labels are read from a TrackMate
	 * {@link SpotCollection}.
	 *
	 * @param imp
	 *            the source image for the model.
	 * @param trackmateModel
	 *            the TrackMate model to read the spots from.
	 * @param ds
	 *            the {@link DisplaySettings} to use for spot coloring. Can be
	 *            <code>null</code>, in that case the spots are colored
	 *            randomly.
	 * @param interval
	 *            the interval in the input to include in the model.The interval
	 *            must only include spatial axes (not the channel axis, not the
	 *            time axis). The input image will be cropped from the interval,
	 *            and only the spots that are fully included in the interval
	 *            will be included. If <code>null</code>, the whole input is
	 *            used.
	 * @param timepoint
	 *            the time-point to extract the data from. If strictly negative,
	 *            all the frames of the source image and all the spots in the
	 *            collection will be included in the model. If positive or 0,
	 *            the model will only contain data from the specified
	 *            time-point.
	 * @return a new {@link TMLabKitModel}.
	 */
	public static final TMLabKitModel createModel(
			final ImagePlus imp,
			final Model trackmateModel,
			final DisplaySettings ds,
			final Interval interval,
			final int timepoint )
	{
		// Image component
		final DatasetInputImage input = makeInput( imp, interval, timepoint );
		// Labeling component
		final Pair< Labeling, Map< Integer, Spot > > pair = makeLabeling( imp, trackmateModel, ds, interval, timepoint );
		final Labeling labeling = pair.getA();
		final Map< Integer, Spot > spotLabels = pair.getB();
		// Make the model.
		final Context context = TMUtils.getContext();
		final TMLabKitModel model = new TMLabKitModel( context, input, spotLabels );
		model.imageLabelingModel().labeling().set( labeling );

		return model;
	}

	protected TMLabKitModel( final Context context, final InputImage inputImage, final Map< Integer, Spot > spotLabels )
	{
		this.context = context;
		this.spotLabels = spotLabels;
		this.imageLabelingModel = new TMImageLabelingModel( inputImage );
	}

	/**
	 * Returns a mapping from label indices in the labeling image to the
	 * corresponding spots that were used to create labels.
	 * <p>
	 * This map is static, unmutable, and stores the mapping at the time of the
	 * model creation. Edits made subsequently to the model are not reflected in
	 * this map.
	 *
	 * @return the mapping.
	 */
	public Map< Integer, Spot > getLabelMap()
	{
		return Collections.unmodifiableMap( spotLabels );
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
	 * Creates a new {@link DatasetInputImage} from the specified
	 * {@link ImagePlus}. The embedded label image is empty.
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
	private final static DatasetInputImage makeInput(
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
	 * Prepare the label image for annotation. The labeling is created and each
	 * of its labels receive the name and the color from the spot it is created
	 * from. Only the spots fully included in specified interval are written in
	 * the labeling.
	 *
	 * @param imp
	 *            the source image plus.
	 * @param trackmateModel
	 *            the TrackMate model to read the spots from.
	 * @param displaySettings
	 *            the {@link DisplaySettings} to use for spot coloring. Can be
	 *            <code>null</code>, in that case the spots are colored
	 *            randomly.
	 * @param timepoint
	 *            the time-point to extract to create the labeling. If strictly
	 *            negative, all time-points will be imported.
	 * @return the pair of: A. a new {@link Labeling}, B. the map of spots that
	 *         were written in the labeling. The keys are the label value in the
	 *         labeling.
	 */
	private static Pair< Labeling, Map< Integer, Spot > > makeLabeling(
			final ImagePlus imp,
			final Model trackmateModel,
			final DisplaySettings displaySettings,
			final Interval interval,
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

		// Write spots in it with index = id + 1 and build a map index -> spot.
		final SpotCollection spots = trackmateModel.getSpots();
		final Map< Integer, Spot > spotLabels = new HashMap<>();
		if ( singleTimePoint )
		{
			processFrame( lblImgPlus, spots, timepoint, spotLabels, origin );
		}
		else
		{
			final int timeDim = lblImgPlus.dimensionIndex( Axes.TIME );
			for ( int t = 0; t < imp.getNFrames(); t++ )
			{
				final ImgPlus< UnsignedIntType > lblImgPlusThisFrame = ImgPlusViews.hyperSlice( lblImgPlus, timeDim, t );
				processFrame( lblImgPlusThisFrame, spots, t, spotLabels, origin );
			}
		}
		final Labeling labeling = Labeling.fromImg( lblImgPlus );

		// Fine tune labels name and color.
		final DisplaySettings defaultStyle = DisplaySettings.defaultStyle().copy();
		defaultStyle.setSpotColorBy( TrackMateObject.SPOTS, FeatureUtils.USE_RANDOM_COLOR_KEY );
		final DisplaySettings ds = ( displaySettings == null )
				? defaultStyle
				: displaySettings;
		final FeatureColorGenerator< Spot > colorGen = FeatureUtils.createSpotColorGenerator( trackmateModel, ds );
		for ( final Label label : labeling.getLabels() )
		{
			final String name = label.name();
			final int labelVal = Integer.parseInt( name );
			final Spot spot = spotLabels.get( labelVal );
			if ( spot == null )
			{
				System.out.println( "Spot is null for label " + labelVal + "!!" ); // DEBUG
				continue;
			}
			label.setName( spot.getName() );
			label.setColor( new ARGBType( colorGen.color( spot ).getRGB() ) );
		}

		return new ValuePair<>( labeling, spotLabels );
	}

	private static final void processFrame(
			final ImgPlus< UnsignedIntType > lblImgPlus,
			final SpotCollection spots,
			final int t,
			final Map< Integer, Spot > spotLabels,
			final long[] origin )
	{
		// If we have a single timepoint, don't use -1 to retrieve spots.
		final int lt = t < 0 ? 0 : t;
		final Iterable< Spot > spotsThisFrame = spots.iterable( lt, true );
		if ( null == origin )
		{
			for ( final Spot spot : spotsThisFrame )
			{
				final int index = spot.ID() + 1;
				SpotUtil.iterable( spot, lblImgPlus ).forEach( p -> p.set( index ) );
				spotLabels.put( index, spot );
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

				final int index = spot.ID() + 1;
				SpotUtil.iterable( spot, lblImgPlus ).forEach( p -> p.set( index ) );
				spotLabels.put( index, spot );
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
