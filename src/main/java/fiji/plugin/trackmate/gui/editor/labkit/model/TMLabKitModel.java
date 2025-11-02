package fiji.plugin.trackmate.gui.editor.labkit.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.scijava.Context;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
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
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
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

	private final Model model;

	private final double dt;

	private final double[] calibration;

	protected TMLabKitModel(
			final Context context,
			final Model model,
			final InputImage inputImage,
			final ImagePlus imp )
	{
		this.context = context;
		this.model = model;
		this.imageLabelingModel = new TMImageLabelingModel( inputImage );
		this.dt = imp.getCalibration().frameInterval;
		this.calibration = TMUtils.getSpatialCalibration( imp );
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
	 *            the first 2 dimensions of the interval are used (X and Y). The
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
	 * @return a new {@link TMLabKitModel}. Will be 2D (X and Y), and possibly
	 *         3D (with time) if all time-points are imported. Its XY size will
	 *         be the size of the specified interval (or the size of the input
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
		final Map< Label, Spot > initialMapping = out.getB();
		// Create the image dataset.
		final DatasetInputImage input = makeInput( imp, interval, timepoint );
		// Make the model.
		final TMLabKitModel lbModel = new TMLabKitModel( context, model, input, imp );
		lbModel.imageLabelingModel().labeling().set( labeling );
		// Store info about what the labeling is when we created it (before modif)
		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< UnsignedIntType > initialIndexImg = TMLabKitUtils.copy( ( RandomAccessibleInterval< UnsignedIntType > ) labeling.getIndexImg() );
		lbModel.imageLabelingModel().setInitialState( initialMapping, initialIndexImg );
		return lbModel;
	}

	/**
	 * Returns <code>true</code> if changes have been made in the labeling
	 * compared to its initial state.
	 *
	 * @return <code>true</code> if changes are detected.
	 */
	public boolean hasChanges()
	{
		final Labeling labeling = imageLabelingModel.labeling().get();
		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< UnsignedIntType > indexImg = ( RandomAccessibleInterval< UnsignedIntType > ) labeling.getIndexImg();
		final RandomAccessibleInterval< UnsignedIntType > initialIndexImg = imageLabelingModel.initialIndexImg();
		return TMLabKitUtils.isDifferent( initialIndexImg, indexImg );
	}

	public void updateTrackMateModel( final boolean simplifyContours, final int timepoint )
	{
		final Labeling labeling = imageLabelingModel().labeling().get();
		final Map< Label, Spot > initialMapping = imageLabelingModel().initialMapping();
		final RandomAccessibleInterval< UnsignedIntType > initialIndexImg = imageLabelingModel().initialIndexImg();

		LabkitImporter.create()
				.trackmateModel( model )
				.labeling( labeling )
				.initialIndexImg( initialIndexImg )
				.initialMapping( initialMapping )
				.targetTimePoint( timepoint )
				.calibration( calibration )
				.frameInterval( dt )
				.simplifyContours( simplifyContours )
				.get()
				.run();
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
	private static final Pair< Labeling, Map< Label, Spot > > createLabeling(
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
		final CalibratedAxis[] caxes = new CalibratedAxis[ axes.length ];
		lblImgPlus.axes( caxes );
		labeling.setAxes( Arrays.asList( caxes ) );

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
			for ( int t = 0; t < imp.getNFrames(); t++ )
				processFrame( labeling, lblImgPlus, spots, t, origin, colorGen, spotLabels );
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
			final int timepoint,
			final long[] origin,
			final FeatureColorGenerator< Spot > colorGen,
			final Map< Label, Spot > spotLabels )
	{

		// If we have time, reslice.
		final int timeDim = lblImgPlus.dimensionIndex( Axes.TIME );
		final RandomAccess< LabelingType< Label > > ra;
		final ImgPlus< UnsignedIntType > img;
		if ( timeDim < 0 )
		{
			ra = labeling.randomAccess();
			img = lblImgPlus;
		}
		else
		{
			ra = Views.hyperSlice( labeling, timeDim, timepoint ).randomAccess();
			img = ImgPlusViews.hyperSlice( lblImgPlus, timeDim, timepoint );
		}

		final Iterable< Spot > spotsThisFrame = spots.iterable( timepoint, true );
		if ( null == origin )
		{
			for ( final Spot spot : spotsThisFrame )
			{
				final Label label = labeling.addLabel( spot.getName() );
				label.setColor( new ARGBType( colorGen.color( spot ).getRGB() ) );
				final Cursor< UnsignedIntType > c = spot.iterable( img ).localizingCursor();
				while ( c.hasNext() )
				{
					c.fwd();
					if ( !Intervals.contains( img, c ) )
						continue;
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
			final FinalInterval imgBB = Intervals.createMinSize( origin[ 0 ], origin[ 1 ], img.dimension( 0 ), img.dimension( 1 ) );
			for ( final Spot spot : spotsThisFrame )
			{
				TMLabKitUtils.boundingBox( spot, img, min, max );
				// Inside? We skip if we touch the border.
				final boolean isInside = Intervals.contains( imgBB, spotBB );
				if ( !isInside )
					continue;

				final Label label = labeling.addLabel( spot.getName() );
				label.setColor( new ARGBType( colorGen.color( spot ).getRGB() ) );
				final Cursor< UnsignedIntType > c = spot.iterable( img ).localizingCursor();
				while ( c.hasNext() )
				{
					c.fwd();
					if ( !Intervals.contains( img, c ) )
						continue;
					ra.setPosition( c );
					ra.get().add( label );
				}
				spotLabels.put( label, spot );
			}
		}
	}
}
