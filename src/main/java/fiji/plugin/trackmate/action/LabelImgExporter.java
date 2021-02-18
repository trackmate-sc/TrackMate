package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.gui.Icons.LABEL_IMG_ICON;
import static fiji.plugin.trackmate.gui.Icons.TRACKMATE_ICON;

import java.awt.Frame;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.util.SpotUtil;
import fiji.plugin.trackmate.util.TMUtils;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class LabelImgExporter extends AbstractTMAction
{

	public static final String INFO_TEXT = "<html>"
			+ "This action creates a label image from the tracking results. "
			+ "<p> "
			+ "A new 16-bit image is generated, of same dimension and size that "
			+ "of the input image. The label image has one channel, with black baground (0 value) "
			+ "everywhere, except where there are spots. Each spot is painted with "
			+ "a uniform integer value equal to the trackID it belongs to. "
			+ "Spots that do not belong to tracks are painted with a unique integer "
			+ "larger than the last trackID in the dataset. "
			+ "<p> "
			+ "Only visible spots are painted. "
			+ "</html>";

	public static final String KEY = "EXPORT_LABEL_IMG";

	public static final String NAME = "Export label image";

	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame gui )
	{
		/*
		 * Ask use for option.
		 */

		final boolean exportSpotsAsDots;
		final boolean exportTracksOnly;
		if ( gui != null )
		{
			final LabelImgExporterPanel panel = new LabelImgExporterPanel();
			final int userInput = JOptionPane.showConfirmDialog(
					gui,
					panel,
					"Export to label image",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					TRACKMATE_ICON );

			if ( userInput != JOptionPane.OK_OPTION )
				return;

			exportSpotsAsDots = panel.isExportSpotsAsDots();
			exportTracksOnly = panel.isExportTracksOnly();
		}
		else
		{
			exportSpotsAsDots = false;
			exportTracksOnly = false;
		}

		/*
		 * Generate label image.
		 */

		createLabelImagePlus( trackmate, exportSpotsAsDots, exportTracksOnly, logger ).show();
	}

	/**
	 * Creates a new label {@link ImagePlus} where the spots of the specified
	 * model are painted as ellipsoids taken from their shape, with their track
	 * ID as pixel value.
	 *
	 * @param trackmate
	 *            the trackmate instance from which we takes the spots to paint.
	 *            The label image will have the same calibration, name and
	 *            dimension from the input image stored in the trackmate
	 *            settings. The output label image will have the same size that
	 *            of this input image, except for the number of channels, which
	 *            will be 1.
	 * @param exportSpotsAsDots
	 *            if <code>true</code>, spots will be painted as single dots
	 *            instead of ellipsoids.
	 * @param exportTracksOnly
	 *            if <code>true</code>, only the spots belonging to visible
	 *            tracks will be painted. If <code>false</code>, spots not
	 *            belonging to a track will be painted with a unique ID,
	 *            different from the track IDs and different for each spot.
	 *
	 * @return a new {@link ImagePlus}.
	 */
	public static final ImagePlus createLabelImagePlus(
			final TrackMate trackmate,
			final boolean exportSpotsAsDots,
			final boolean exportTracksOnly )
	{
		return createLabelImagePlus( trackmate, exportSpotsAsDots, exportTracksOnly, Logger.VOID_LOGGER );
	}

	/**
	 * Creates a new label {@link ImagePlus} where the spots of the specified
	 * model are painted as ellipsoids taken from their shape, with their track
	 * ID as pixel value.
	 *
	 * @param trackmate
	 *            the trackmate instance from which we takes the spots to paint.
	 *            The label image will have the same calibration, name and
	 *            dimension from the input image stored in the trackmate
	 *            settings. The output label image will have the same size that
	 *            of this input image, except for the number of channels, which
	 *            will be 1.
	 * @param exportSpotsAsDots
	 *            if <code>true</code>, spots will be painted as single dots
	 *            instead of ellipsoids.
	 * @param exportTracksOnly
	 *            if <code>true</code>, only the spots belonging to visible
	 *            tracks will be painted. If <code>false</code>, spots not
	 *            belonging to a track will be painted with a unique ID,
	 *            different from the track IDs and different for each spot.
	 * @param logger
	 *            a {@link Logger} instance, to report progress of the export
	 *            process.
	 *
	 * @return a new {@link ImagePlus}.
	 */
	public static final ImagePlus createLabelImagePlus(
			final TrackMate trackmate,
			final boolean exportSpotsAsDots,
			final boolean exportTracksOnly,
			final Logger logger )
	{
		return createLabelImagePlus( trackmate.getModel(), trackmate.getSettings().imp, exportSpotsAsDots, exportTracksOnly, logger );
	}

	/**
	 * Creates a new label {@link ImagePlus} where the spots of the specified
	 * model are painted as ellipsoids taken from their shape, with their track
	 * ID as pixel value.
	 *
	 * @param model
	 *            the model from which we takes the spots to paint.
	 * @param imp
	 *            a source image to read calibration, name and dimension from.
	 *            The output label image will have the same size that of this
	 *            source image, except for the number of channels, which will be
	 *            1.
	 * @param exportSpotsAsDots
	 *            if <code>true</code>, spots will be painted as single dots
	 *            instead of ellipsoids.
	 * @param exportTracksOnly
	 *            if <code>true</code>, only the spots belonging to visible
	 *            tracks will be painted. If <code>false</code>, spots not
	 *            belonging to a track will be painted with a unique ID,
	 *            different from the track IDs and different for each spot.
	 *
	 * @return a new {@link ImagePlus}.
	 */
	public static final ImagePlus createLabelImagePlus(
			final Model model,
			final ImagePlus imp,
			final boolean exportSpotsAsDots,
			final boolean exportTracksOnly )
	{
		return createLabelImagePlus( model, imp, exportSpotsAsDots, exportTracksOnly, Logger.VOID_LOGGER );
	}

	/**
	 * Creates a new label {@link ImagePlus} where the spots of the specified
	 * model are painted as ellipsoids taken from their shape, with their track
	 * ID as pixel value.
	 *
	 * @param model
	 *            the model from which we takes the spots to paint.
	 * @param imp
	 *            a source image to read calibration, name and dimension from.
	 *            The output label image will have the same size that of this
	 *            source image, except for the number of channels, which will be
	 *            1.
	 * @param exportSpotsAsDots
	 *            if <code>true</code>, spots will be painted as single dots
	 *            instead of ellipsoids.
	 * @param exportTracksOnly
	 *            if <code>true</code>, only the spots belonging to visible
	 *            tracks will be painted. If <code>false</code>, spots not
	 *            belonging to a track will be painted with a unique ID,
	 *            different from the track IDs and different for each spot.
	 * @param logger
	 *            a {@link Logger} instance, to report progress of the export
	 *            process.
	 *
	 * @return a new {@link ImagePlus}.
	 */
	public static final ImagePlus createLabelImagePlus(
			final Model model,
			final ImagePlus imp,
			final boolean exportSpotsAsDots,
			final boolean exportTracksOnly,
			final Logger logger )
	{
		final int[] dimensions = imp.getDimensions();
		final int[] dims = new int[] { dimensions[ 0 ], dimensions[ 1 ], dimensions[ 3 ], dimensions[ 4 ] };
		final double[] calibration = new double[] {
				imp.getCalibration().pixelWidth,
				imp.getCalibration().pixelHeight,
				imp.getCalibration().pixelDepth,
				imp.getCalibration().frameInterval
		};
		final ImagePlus lblImp = createLabelImagePlus( model, dims, calibration, exportSpotsAsDots, exportTracksOnly, logger );
		lblImp.setCalibration( imp.getCalibration().copy() );
		lblImp.setTitle( "LblImg_" + imp.getTitle() );
		return lblImp;

	}

	/**
	 * Creates a new label {@link ImagePlus} where the spots of the specified
	 * model are painted as ellipsoids taken from their shape, with their track
	 * ID as pixel value.
	 *
	 * @param model
	 *            the model from which we takes the spots to paint.
	 * @param dimensions
	 *            the desired dimensions of the output image (width, height,
	 *            nZSlices, nFrames) as a 4 element int array. Spots outside
	 *            these dimensions are ignored.
	 * @param exportSpotsAsDots
	 *            if <code>true</code>, spots will be painted as single dots
	 *            instead of ellipsoids.
	 * @param exportTracksOnly
	 *            if <code>true</code>, only the spots belonging to visible
	 *            tracks will be painted. If <code>false</code>, spots not
	 *            belonging to a track will be painted with a unique ID,
	 *            different from the track IDs and different for each spot.
	 *
	 * @return a new {@link ImagePlus}.
	 */
	public static final ImagePlus createLabelImagePlus(
			final Model model,
			final int[] dimensions,
			final double[] calibration,
			final boolean exportSpotsAsDots,
			final boolean exportTracksOnly )
	{
		return createLabelImagePlus( model, dimensions, calibration, exportSpotsAsDots, exportTracksOnly, Logger.VOID_LOGGER );
	}

	/**
	 * Creates a new label {@link ImagePlus} where the spots of the specified
	 * model are painted as ellipsoids taken from their shape, with their track
	 * ID as pixel value.
	 *
	 * @param model
	 *            the model from which we takes the spots to paint.
	 * @param dimensions
	 *            the desired dimensions of the output image (width, height,
	 *            nZSlices, nFrames) as a 4 element int array. Spots outside
	 *            these dimensions are ignored.
	 * @param exportSpotsAsDots
	 *            if <code>true</code>, spots will be painted as single dots
	 *            instead of ellipsoids.
	 * @param exportTracksOnly
	 *            if <code>true</code>, only the spots belonging to visible
	 *            tracks will be painted. If <code>false</code>, spots not
	 *            belonging to a track will be painted with a unique ID,
	 *            different from the track IDs and different for each spot.
	 * @param logger
	 *            a {@link Logger} instance, to report progress of the export
	 *            process.
	 *
	 * @return a new {@link ImagePlus}.
	 */
	public static final ImagePlus createLabelImagePlus(
			final Model model,
			final int[] dimensions,
			final double[] calibration,
			final boolean exportSpotsAsDots,
			final boolean exportTracksOnly,
			final Logger logger )
	{
		final long[] dims = new long[ 4 ];
		for ( int d = 0; d < dims.length; d++ )
			dims[ d ] = dimensions[ d ];

		final ImagePlus lblImp = ImageJFunctions.wrap( createLabelImg( model, dims, calibration, exportSpotsAsDots, exportTracksOnly, logger ), "LblImage" );
		lblImp.setDimensions( 1, dimensions[ 2 ], dimensions[ 3 ] );
		lblImp.setOpenAsHyperStack( true );
		lblImp.resetDisplayRange();
		return lblImp;
	}

	/**
	 * Creates a new label {@link Img} of {@link UnsignedShortType} where the
	 * spots of the specified model are painted as ellipsoids taken from their
	 * shape, with their track ID as pixel value.
	 *
	 * @param model
	 *            the model from which we takes the spots to paint.
	 * @param dimensions
	 *            the desired dimensions of the output image (width, height,
	 *            nZSlices, nFrames) as a 4 element int array. Spots outside
	 *            these dimensions are ignored.
	 * @param exportSpotsAsDots
	 *            if <code>true</code>, spots will be painted as single dots
	 *            instead of ellipsoids.
	 * @param exportTracksOnly
	 *            if <code>true</code>, only the spots belonging to visible
	 *            tracks will be painted. If <code>false</code>, spots not
	 *            belonging to a track will be painted with a unique ID,
	 *            different from the track IDs and different for each spot.
	 *
	 * @return a new {@link Img}.
	 */
	public static final Img< UnsignedShortType > createLabelImg(
			final Model model,
			final long[] dimensions,
			final double[] calibration,
			final boolean exportSpotsAsDots,
			final boolean exportTracksOnly )
	{
		return createLabelImg( model, dimensions, calibration, exportSpotsAsDots, exportTracksOnly, Logger.VOID_LOGGER );
	}

	/**
	 * Creates a new label {@link Img} of {@link UnsignedShortType} where the
	 * spots of the specified model are painted as ellipsoids taken from their
	 * shape, with their track ID as pixel value.
	 *
	 * @param model
	 *            the model from which we takes the spots to paint.
	 * @param dimensions
	 *            the desired dimensions of the output image (width, height,
	 *            nZSlices, nFrames) as a 4 element int array. Spots outside
	 *            these dimensions are ignored.
	 * @param exportSpotsAsDots
	 *            if <code>true</code>, spots will be painted as single dots
	 *            instead of ellipsoids.
	 * @param exportTracksOnly
	 *            if <code>true</code>, only the spots belonging to visible
	 *            tracks will be painted. If <code>false</code>, spots not
	 *            belonging to a track will be painted with a unique ID,
	 *            different from the track IDs and different for each spot.
	 * @param logger
	 *            a {@link Logger} instance, to report progress of the export
	 *            process.
	 *
	 * @return a new {@link Img}.
	 */
	public static final Img< UnsignedShortType > createLabelImg(
			final Model model,
			final long[] dimensions,
			final double[] calibration,
			final boolean exportSpotsAsDots,
			final boolean exportTracksOnly,
			final Logger logger )
	{
		/*
		 * Create target image.
		 */
		final Dimensions targetSize = FinalDimensions.wrap( dimensions );
		final Img< UnsignedShortType > lblImg = Util.getArrayOrCellImgFactory( targetSize, new UnsignedShortType() ).create( targetSize );
		final AxisType[] axes = new AxisType[] {
				Axes.X,
				Axes.Y,
				Axes.Z,
				Axes.TIME };
		final ImgPlus< UnsignedShortType > imgPlus = new ImgPlus<>( lblImg, "LblImg", axes, calibration );

		/*
		 * Determine the starting id for spots not in tracks.
		 */

		int maxTrackID = -1;
		final Set< Integer > trackIDs = model.getTrackModel().trackIDs( false );
		if ( null != trackIDs )
			for ( final Integer trackID : trackIDs )
				if ( trackID > maxTrackID )
					maxTrackID = trackID.intValue();
		final AtomicInteger lonelySpotID = new AtomicInteger( maxTrackID + 2 );

		/*
		 * Frame by frame iteration.
		 */

		logger.log( "Writing label image.\n" );
		for ( int frame = 0; frame < dimensions[ 3 ]; frame++ )
		{
			final ImgPlus< UnsignedShortType > imgCT = TMUtils.hyperSlice( imgPlus, 0, frame );
			final SpotWriter spotWriter = exportSpotsAsDots
					? new SpotAsDotWriter( imgCT )
					: new SpotRoiWriter( imgCT );

			for ( final Spot spot : model.getSpots().iterable( frame, true ) )
			{
				final int id;
				final Integer trackID = model.getTrackModel().trackIDOf( spot );
				if ( null == trackID || !model.getTrackModel().isVisible( trackID ) )
				{
					if ( exportTracksOnly )
						continue;

					id = lonelySpotID.getAndIncrement();
				}
				else
				{
					id = 1 + trackID.intValue();
				}

				spotWriter.write( spot, id );
			}
			logger.setProgress( ( double ) ( 1 + frame ) / dimensions[ 3 ] );
		}
		logger.log( "Done.\n" );

		return lblImg;
	}

	@Plugin( type = TrackMateActionFactory.class )
	public static class Factory implements TrackMateActionFactory
	{

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public TrackMateAction create()
		{
			return new LabelImgExporter();
		}

		@Override
		public ImageIcon getIcon()
		{
			return LABEL_IMG_ICON;
		}

		@Override
		public String getName()
		{
			return NAME;
		}
	}

	/**
	 * Interface for classes that can 'write' a spot into a label image.
	 */
	private static interface SpotWriter
	{
		public void write( Spot spot, int id );
	}

	private static final class SpotRoiWriter implements SpotWriter
	{

		private final ImgPlus< UnsignedShortType > img;

		public SpotRoiWriter( final ImgPlus< UnsignedShortType > img )
		{
			this.img = img;
		}

		@Override
		public void write( final Spot spot, final int id )
		{
			for ( final UnsignedShortType pixel : SpotUtil.iterable( spot, img ) )
				pixel.set( id );
		}
	}

	private static final class SpotAsDotWriter implements SpotWriter
	{

		private final double[] calibration;

		private final long[] center;

		private final RandomAccess< UnsignedShortType > ra;

		public SpotAsDotWriter( final ImgPlus< UnsignedShortType > img )
		{
			this.calibration = TMUtils.getSpatialCalibration( img );
			this.center = new long[ img.numDimensions() ];
			this.ra = Views.extendZero( img ).randomAccess();
		}

		@Override
		public void write( final Spot spot, final int id )
		{
			for ( int d = 0; d < center.length; d++ )
				center[ d ] = Math.round( spot.getFeature( Spot.POSITION_FEATURES[ d ] ).doubleValue() / calibration[ d ] );

			ra.setPosition( center );
			ra.get().set( id );
		}
	}
}
