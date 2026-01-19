/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
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
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.util.SpotUtil;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.GlasbeyLut;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class LabelImgExporter extends AbstractTMAction
{

	public static final String INFO_TEXT = "<html>"
			+ "This action creates a label image from the tracking results. "
			+ "<p> "
			+ "A new 32-bit image is generated, of same dimension and size that "
			+ "of the input image. The label image has one channel, with black background (0 value) "
			+ "everywhere, except where there are spots. Each spot is painted with "
			+ "a uniform integer value, configurable to be the spot ID, the ID of the "
			+ "track it belongs to, or a simple index, unique to the frame or to the movie. "
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
		final LabelIdPainting labelIdPainting;
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
			labelIdPainting = panel.labelIdPainting();
		}
		else
		{
			exportSpotsAsDots = false;
			exportTracksOnly = false;
			labelIdPainting = LabelIdPainting.LABEL_IS_INDEX_MOVIE_UNIQUE;
		}

		/*
		 * Generate label image.
		 */

		createLabelImagePlus( trackmate, exportSpotsAsDots, exportTracksOnly, labelIdPainting, logger ).show();
	}

	/**
	 * Creates a new label {@link ImagePlus} where the spots of the specified
	 * model are painted with their shape, with their track ID as pixel value.
	 *
	 * @param trackmate
	 *            the trackmate instance from which we takes the spots to paint.
	 *            The label image will have the same calibration, name and
	 *            dimension from the input image stored in the trackmate
	 *            settings. The output label image will have the same size that
	 *            of this input image, except for the number of channels, which
	 *            will be 1.
	 * @param exportSpotsAsDots
	 *            if <code>true</code>, spots will be painted as single dots. If
	 *            <code>false</code> they will be painted with their shape.
	 * @param exportTracksOnly
	 *            if <code>true</code>, only the spots belonging to visible
	 *            tracks will be painted. If <code>false</code>, spots not
	 *            belonging to a track will be painted with a unique ID,
	 *            different from the track IDs and different for each spot.
	 * @param labeIdPainting
	 *            specifies how to paint the label ID of spots.
	 *
	 * @return a new {@link ImagePlus}.
	 */
	public static final ImagePlus createLabelImagePlus(
			final TrackMate trackmate,
			final boolean exportSpotsAsDots,
			final boolean exportTracksOnly,
			final LabelIdPainting labeIdPainting
			)
	{
		return createLabelImagePlus( trackmate, exportSpotsAsDots, exportTracksOnly, labeIdPainting, Logger.VOID_LOGGER );
	}

	/**
	 * Creates a new label {@link ImagePlus} where the spots of the specified
	 * model are painted with their shape, with their track ID as pixel value.
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
	 *            instead their shape.
	 * @param exportTracksOnly
	 *            if <code>true</code>, only the spots belonging to visible
	 *            tracks will be painted. If <code>false</code>, spots not
	 *            belonging to a track will be painted with a unique ID,
	 *            different from the track IDs and different for each spot.
	 * @param labelIdPainting
	 *            specifies how to paint the label ID of spots.
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
			final LabelIdPainting labelIdPainting,
			final Logger logger )
	{
		return createLabelImagePlus( trackmate.getModel(), trackmate.getSettings().imp, exportSpotsAsDots, exportTracksOnly, labelIdPainting, logger );
	}

	/**
	 * Creates a new label {@link ImagePlus} where the spots of the specified
	 * model are painted with their shape, with their track ID as pixel value.
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
	 *            instead their shape.
	 * @param exportTracksOnly
	 *            if <code>true</code>, only the spots belonging to visible
	 *            tracks will be painted. If <code>false</code>, spots not
	 *            belonging to a track will be painted with a unique ID,
	 *            different from the track IDs and different for each spot.
	 * @param labelIdPainting
	 *            specifies how to paint the label ID of spots.
	 *
	 * @return a new {@link ImagePlus}.
	 */
	public static final ImagePlus createLabelImagePlus(
			final Model model,
			final ImagePlus imp,
			final boolean exportSpotsAsDots,
			final boolean exportTracksOnly,
			final LabelIdPainting labelIdPainting )
	{
		return createLabelImagePlus( model, imp, exportSpotsAsDots, exportTracksOnly, labelIdPainting, Logger.VOID_LOGGER );
	}

	/**
	 * Creates a new label {@link ImagePlus} where the spots of the specified
	 * model are painted with their shape, with their track ID as pixel value.
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
	 *            instead their shape.
	 * @param exportTracksOnly
	 *            if <code>true</code>, only the spots belonging to visible
	 *            tracks will be painted. If <code>false</code>, spots not
	 *            belonging to a track will be painted with a unique ID,
	 *            different from the track IDs and different for each spot.
	 * @param labelIdPainting
	 *            specifies how to paint the label ID of spots.
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
			final LabelIdPainting labelIdPainting,
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
		final ImagePlus lblImp = createLabelImagePlus( model, dims, calibration, exportSpotsAsDots, exportTracksOnly, labelIdPainting, logger );
		lblImp.setCalibration( imp.getCalibration().copy() );
		lblImp.setTitle( "LblImg_" + imp.getTitle() );
		return lblImp;

	}

	/**
	 * Creates a new label {@link ImagePlus} where the spots of the specified
	 * model are painted with their shape, with their track ID as pixel value.
	 *
	 * @param model
	 *            the model from which we takes the spots to paint.
	 * @param dimensions
	 *            the desired dimensions of the output image (width, height,
	 *            nZSlices, nFrames) as a 4 element long array. Spots outside
	 *            these dimensions are ignored.
	 * @param calibration
	 *            the desired calibration of the output image (pixel width,
	 *            pixel height, pixel depth, frame interval) as a 4 element
	 *            double array.
	 * @param exportSpotsAsDots
	 *            if <code>true</code>, spots will be painted as single dots
	 *            instead their shape.
	 * @param exportTracksOnly
	 *            if <code>true</code>, only the spots belonging to visible
	 *            tracks will be painted. If <code>false</code>, spots not
	 *            belonging to a track will be painted with a unique ID,
	 *            different from the track IDs and different for each spot.
	 * @param labelIdPainting
	 *            specifies how to paint the label ID of spots.
	 *
	 * @return a new {@link ImagePlus}.
	 */
	public static final ImagePlus createLabelImagePlus(
			final Model model,
			final int[] dimensions,
			final double[] calibration,
			final boolean exportSpotsAsDots,
			final boolean exportTracksOnly,
			final LabelIdPainting labelIdPainting )
	{
		return createLabelImagePlus( model, dimensions, calibration, exportSpotsAsDots, exportTracksOnly, labelIdPainting, Logger.VOID_LOGGER );
	}

	/**
	 * Creates a new label {@link ImagePlus} where the spots of the specified
	 * model are painted with their shape, with their track ID as pixel value.
	 *
	 * @param model
	 *            the model from which we takes the spots to paint.
	 * @param dimensions
	 *            the desired dimensions of the output image (width, height,
	 *            nZSlices, nFrames) as a 4 element int array. Spots outside
	 *            these dimensions are ignored.
	 * @param calibration
	 *            the desired calibration of the output image (pixel width,
	 *            pixel height, pixel depth, frame interval) as a 4 element
	 *            double array.
	 * @param exportSpotsAsDots
	 *            if <code>true</code>, spots will be painted as single dots
	 *            instead their shape.
	 * @param exportTracksOnly
	 *            if <code>true</code>, only the spots belonging to visible
	 *            tracks will be painted. If <code>false</code>, spots not
	 *            belonging to a track will be painted with a unique ID,
	 *            different from the track IDs and different for each spot.
	 * @param labelIdPainting
	 *            specifies how to paint the label ID of spots.
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
			final LabelIdPainting labelIdPainting,
			final Logger logger )
	{
		final long[] dims = new long[ 4 ];
		for ( int d = 0; d < dims.length; d++ )
			dims[ d ] = dimensions[ d ];

		final ImagePlus lblImp = ImageJFunctions.wrap( createLabelImg( model, dims, calibration, exportSpotsAsDots, exportTracksOnly, labelIdPainting, logger ), "LblImage" );
		lblImp.setDimensions( 1, dimensions[ 2 ], dimensions[ 3 ] );
		lblImp.setLut( GlasbeyLut.toLUT() );
		lblImp.setDisplayRange( 0, 255 );
		lblImp.setOpenAsHyperStack( true );
		return lblImp;
	}

	/**
	 * Creates a new label {@link Img} of {@link FloatType} where the spots of
	 * the specified model are painted with their shape, with their track ID as
	 * pixel value.
	 *
	 * @param model
	 *            the model from which we takes the spots to paint.
	 * @param dimensions
	 *            the desired dimensions of the output image (width, height,
	 *            nZSlices, nFrames) as a 4 element long array. Spots outside
	 *            these dimensions are ignored.
	 * @param calibration
	 *            the desired calibration of the output image (pixel width,
	 *            pixel height, pixel depth, frame interval) as a 4 element
	 *            double array.
	 * @param exportSpotsAsDots
	 *            if <code>true</code>, spots will be painted as single dots
	 *            instead their shape.
	 * @param exportTracksOnly
	 *            if <code>true</code>, only the spots belonging to visible
	 *            tracks will be painted. If <code>false</code>, spots not
	 *            belonging to a track will be painted with a unique ID,
	 *            different from the track IDs and different for each spot.
	 * @param labelIdPainting
	 *            specifies how to paint the label ID of spots.
	 *
	 * @return a new {@link Img}.
	 */
	public static final Img< FloatType > createLabelImg(
			final Model model,
			final long[] dimensions,
			final double[] calibration,
			final boolean exportSpotsAsDots,
			final boolean exportTracksOnly,
			final LabelIdPainting labelIdPainting )
	{
		return createLabelImg( model, dimensions, calibration, exportSpotsAsDots, exportTracksOnly, labelIdPainting, Logger.VOID_LOGGER );
	}

	/**
	 * Creates a new label {@link Img} of {@link FloatType} where the spots of
	 * the specified model are painted with their shape, with an ID pixel value.
	 *
	 * @param model
	 *            the model from which we takes the spots to paint.
	 * @param dimensions
	 *            the desired dimensions of the output image (width, height,
	 *            nZSlices, nFrames) as a 4 element long array. Spots outside
	 *            these dimensions are ignored.
	 * @param calibration
	 *            the desired calibration of the output image (pixel width,
	 *            pixel height, pixel depth, frame interval) as a 4 element
	 *            double.
	 * @param exportSpotsAsDots
	 *            if <code>true</code>, spots will be painted as single dots
	 *            instead their shape.
	 * @param exportTracksOnly
	 *            if <code>true</code>, only the spots belonging to visible
	 *            tracks will be painted. If <code>false</code>, spots not
	 *            belonging to a track will be painted with a unique ID,
	 *            different from the track IDs and different for each spot.
	 * @param labelIdPainting
	 *            specifies how to paint the label ID of spots.
	 * @param logger
	 *            a {@link Logger} instance, to report progress of the export
	 *            process.
	 *
	 * @return a new {@link Img}.
	 */
	public static final Img< FloatType > createLabelImg(
			final Model model,
			final long[] dimensions,
			final double[] calibration,
			final boolean exportSpotsAsDots,
			final boolean exportTracksOnly,
			final LabelIdPainting labelIdPainting,
			final Logger logger )
	{
		/*
		 * Create target image.
		 */
		final Dimensions targetSize = FinalDimensions.wrap( dimensions );
		final Img< FloatType > lblImg = Util.getArrayOrCellImgFactory( targetSize, new FloatType() ).create( targetSize );
		final AxisType[] axes = new AxisType[] {
				Axes.X,
				Axes.Y,
				Axes.Z,
				Axes.TIME };
		final ImgPlus< FloatType > imgPlus = new ImgPlus<>( lblImg, "LblImg", axes, calibration );

		/*
		 * How to assign an ID to spots.
		 */

		final IdGenerator idGenerator = labelIdPainting.idGenerator( model.getTrackModel(), exportTracksOnly );

		/*
		 * Frame by frame iteration.
		 */

		logger.log( "Writing label image.\n" );
		for ( int frame = 0; frame < dimensions[ 3 ]; frame++ )
		{
			final ImgPlus< FloatType > imgCT = TMUtils.hyperSlice( imgPlus, 0, frame );
			final SpotWriter spotWriter = exportSpotsAsDots
					? new SpotAsDotWriter<>( imgCT )
					: new SpotRoiWriter<>( imgCT );
			idGenerator.nextFrame();

			for ( final Spot spot : model.getSpots().iterable( frame, true ) )
			{
				final int id = idGenerator.id( spot );
				spotWriter.write( spot, id );
			}
			logger.setProgress( ( double ) ( 1 + frame ) / dimensions[ 3 ] );
		}
		logger.log( "Done.\n" );

		return lblImg;
	}


	/**
	 * Creates a new label {@link ImgPlus} of specified pixel type where the
	 * spots are painted with an ID. All visible spots are painted, whether they
	 * are in a track or not.
	 *
	 * @param spots
	 *            the spots to paint.
	 * @param dimensions
	 *            the desired dimensions of the output image (width, height,
	 *            nZSlices, nFrames) as a 4 element long array. Spots outside
	 *            these dimensions are ignored.
	 * @param calibration
	 *            the desired calibration of the output image (pixel width,
	 *            pixel height, pixel depth, frame interval) as a 4 element
	 *            double array.
	 * @param exportSpotsAsDots
	 *            if <code>true</code>, spots will be painted as single dots
	 *            instead of their shape.
	 * @param labelIdPainting
	 *            specifies how to paint the label ID of spots. The
	 *            {@link LabelIdPainting#LABEL_IS_TRACK_ID} is not supported and
	 *            defaults to {@link LabelIdPainting#LABEL_IS_SPOT_ID}.
	 * @param outputType
	 *            the output pixel type.
	 * @param logger
	 *            a {@link Logger} instance, to report progress of the export
	 *            process.
	 * @param <T>
	 *            the pixel type of the output image.
	 * @return a new {@link ImgPlus}.
	 */
	public static < T extends RealType< T > & NativeType< T > > ImgPlus< T > createLabelImg(
			final SpotCollection spots,
			final long[] dimensions,
			final double[] calibration,
			final boolean exportSpotsAsDots,
			final LabelIdPainting labelIdPainting,
			final T outputType,
			final Logger logger )
	{
		/*
		 * Create target image.
		 */
		final Dimensions targetSize = FinalDimensions.wrap( dimensions );
		final Img< T > lblImg = Util.getArrayOrCellImgFactory( targetSize, outputType ).create( targetSize );
		final AxisType[] axes = new AxisType[] {
				Axes.X,
				Axes.Y,
				Axes.Z,
				Axes.TIME };
		final ImgPlus< T > imgPlus = new ImgPlus<>( lblImg, "LblImg", axes, calibration );

		/*
		 * How to assign an ID to spots.
		 */

		final IdGenerator idGenerator;
		switch ( labelIdPainting )
		{
		case LABEL_IS_INDEX:
			idGenerator = new SpotIndexGeneratorUniqueInFrame( null, false );
			break;
		case LABEL_IS_INDEX_MOVIE_UNIQUE:
			idGenerator = new SpotIndexGeneratorUniqueInMovie( null, false );
			break;
		case LABEL_IS_SPOT_ID:
		case LABEL_IS_TRACK_ID:
			idGenerator = new SpotIdGenerator( null, false );
			break;
		default:
			throw new IllegalArgumentException( "Unknown painting method: " + labelIdPainting );
		}

		/*
		 * Frame by frame iteration.
		 */

		logger.log( "Writing label image.\n" );
		for ( int frame = 0; frame < dimensions[ 3 ]; frame++ )
		{
			final ImgPlus< T > imgCT = TMUtils.hyperSlice( imgPlus, 0, frame );
			final SpotWriter spotWriter = exportSpotsAsDots
					? new SpotAsDotWriter<>( imgCT )
					: new SpotRoiWriter<>( imgCT );
			idGenerator.nextFrame();

			for ( final Spot spot : spots.iterable( frame, true ) )
			{
				final int id = idGenerator.id( spot );
				spotWriter.write( spot, id );
			}
			logger.setProgress( ( double ) ( 1 + frame ) / dimensions[ 3 ] );
		}
		logger.log( "Done.\n" );

		return imgPlus;
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
	public static interface SpotWriter
	{
		public void write( Spot spot, int id );
	}

	public static final class SpotRoiWriter< T extends RealType< T > > implements SpotWriter
	{

		private final ImgPlus< T > img;

		public SpotRoiWriter( final ImgPlus< T > img )
		{
			this.img = img;
		}

		@Override
		public void write( final Spot spot, final int id )
		{
			for ( final T pixel : SpotUtil.iterable( spot, img ) )
				pixel.setReal( id );
		}
	}

	public static final class SpotAsDotWriter< T extends RealType< T > > implements SpotWriter
	{

		private final double[] calibration;

		private final long[] center;

		private final RandomAccess< T > ra;

		public SpotAsDotWriter( final ImgPlus< T > img )
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
			ra.get().setReal( id );
		}
	}

	/**
	 * Specifies how spots will be labeled in the exported label image.
	 */
	public static enum LabelIdPainting
	{

		LABEL_IS_TRACK_ID( "Track ID",
				"The spot label is the ID of the track it belongs to, plus one (+1). "
				+ "Spots that do not belong to tracks are painted with a unique integer "
				+ "larger than the last trackID in the dataset." ),
		LABEL_IS_SPOT_ID( "Spot ID",
				"The spot label is the spot ID, plus one (+1)." ),
		LABEL_IS_INDEX( "Index unique in frame",
				"The spot label is an index starting from 1. "
				+ "It is unique within a frame, "
				+ "but can be repeated across frames." ),
		LABEL_IS_INDEX_MOVIE_UNIQUE( "Unique index",
				"The spot label is an index starting from 1. "
				+ "It is unique within the whole movie." );

		private final String info;

		private final String methodName;

		private LabelIdPainting( final String methodName, final String info )
		{
			this.methodName = methodName;
			this.info = info;
		}

		@Override
		public String toString()
		{
			return methodName;
		}

		public String getInfo()
		{
			return info;
		}

		public IdGenerator idGenerator( final TrackModel tm, final boolean visibleTracksOnly)
		{
			switch ( this )
			{
			case LABEL_IS_INDEX:
				return new SpotIndexGeneratorUniqueInFrame( tm, visibleTracksOnly );
			case LABEL_IS_INDEX_MOVIE_UNIQUE:
				return new SpotIndexGeneratorUniqueInMovie( tm, visibleTracksOnly );
			case LABEL_IS_SPOT_ID:
				return new SpotIdGenerator( tm, visibleTracksOnly );
			case LABEL_IS_TRACK_ID:
				return new TrackIdGenerator( tm, visibleTracksOnly );
			default:
				throw new IllegalArgumentException( "Unknown painting id mode: " + this );
			}
		}
	}

	private static interface IdGenerator
	{
		public int id( Spot spot );

		public default void nextFrame()
		{};
	}

	private static abstract class AbstractIdGenerator implements IdGenerator
	{
		protected final TrackModel tm;

		protected final boolean visibleTracksOnly;

		public AbstractIdGenerator( final TrackModel tm, final boolean visibleTracksOnly )
		{
			this.tm = tm;
			this.visibleTracksOnly = visibleTracksOnly;
		}
	}

	private static class TrackIdGenerator extends AbstractIdGenerator
	{

		private final AtomicInteger lonelySpotID;

		public TrackIdGenerator( final TrackModel tm, final boolean visibleTracksOnly )
		{
			super( tm, visibleTracksOnly );
			int maxTrackID = -1;
			final Set< Integer > trackIDs = tm.trackIDs( false );
			if ( null != trackIDs )
				for ( final Integer trackID : trackIDs )
					if ( trackID > maxTrackID )
						maxTrackID = trackID.intValue();
			this.lonelySpotID = new AtomicInteger( maxTrackID + 2 );
		}

		@Override
		public int id( final Spot spot )
		{
			final Integer trackID = tm.trackIDOf( spot );
			if ( null == trackID || !tm.isVisible( trackID ) )
			{
				if ( visibleTracksOnly )
					return -1;
				return lonelySpotID.getAndIncrement();
			}
			return trackID + 1;
		}
	}

	private static class SpotIdGenerator extends AbstractIdGenerator
	{

		public SpotIdGenerator( final TrackModel tm, final boolean visibleTracksOnly )
		{
			super( tm, visibleTracksOnly );
		}

		@Override
		public int id( final Spot spot )
		{
			if ( tm != null )
			{
				final Integer trackID = tm.trackIDOf( spot );
				if ( null == trackID || !tm.isVisible( trackID ) )
				{
					if ( visibleTracksOnly )
						return -1;
				}
			}
			return spot.ID() + 1;
		}
	}

	private static class SpotIndexGeneratorUniqueInMovie extends AbstractIdGenerator
	{

		protected final AtomicInteger index;

		public SpotIndexGeneratorUniqueInMovie( final TrackModel tm, final boolean visibleTracksOnly )
		{
			super( tm, visibleTracksOnly );
			this.index = new AtomicInteger( 0 );
		}

		@Override
		public int id( final Spot spot )
		{
			if ( tm != null )
			{
				final Integer trackID = tm.trackIDOf( spot );
				if ( null == trackID || !tm.isVisible( trackID ) )
				{
					if ( visibleTracksOnly )
						return -1;
				}
			}
			return index.incrementAndGet();
		}
	}


	private static class SpotIndexGeneratorUniqueInFrame extends SpotIndexGeneratorUniqueInMovie
	{

		public SpotIndexGeneratorUniqueInFrame( final TrackModel tm, final boolean visibleTracksOnly )
		{
			super( tm, visibleTracksOnly );
		}

		@Override
		public void nextFrame()
		{
			index.set( 0 );
		}
	}
}
