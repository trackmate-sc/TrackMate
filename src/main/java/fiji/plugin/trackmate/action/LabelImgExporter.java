package fiji.plugin.trackmate.action;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.util.SpotNeighborhood;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.meta.view.HyperSliceImgPlus;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Util;

@SuppressWarnings( "deprecation" )
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

	public static final ImageIcon ICON = new ImageIcon( TrackMateWizard.class.getResource( "images/picture_key.png" ) );

	public static final String NAME = "Export label image";

	@Override
	public void execute( final TrackMate trackmate )
	{
		createLabelImagePlus( trackmate, logger ).show();
	}

	public static final ImagePlus createLabelImagePlus( final TrackMate trackmate )
	{
		return createLabelImagePlus( trackmate, Logger.VOID_LOGGER );
	}

	public static final ImagePlus createLabelImagePlus( final TrackMate trackmate, final Logger logger )
	{
		return createLabelImagePlus( trackmate.getModel(), trackmate.getSettings().imp, logger );
	}

	public static final ImagePlus createLabelImagePlus( final Model model, final ImagePlus imp )
	{
		return createLabelImagePlus( model, imp, Logger.VOID_LOGGER );
	}

	public static final ImagePlus createLabelImagePlus( final Model model, final ImagePlus imp, final Logger logger )
	{
		final int[] dimensions = imp.getDimensions();
		final int[] dims = new int[] { dimensions[ 0 ], dimensions[ 1 ], dimensions[ 3 ], dimensions[ 4 ] };

		final ImagePlus lblImp = createLabelImagePlus( model, dims, logger );
		lblImp.setCalibration( imp.getCalibration().copy() );
		lblImp.setTitle( "LblImg_" + imp.getTitle() );
		return lblImp;
	}

	public static final ImagePlus createLabelImagePlus( final Model model, final int[] dimensions )
	{
		return createLabelImagePlus( model, dimensions, Logger.VOID_LOGGER );
	}

	/**
	 * @param model
	 * @param dimensions
	 *            the dimensions of the output image (width, height, nZSlices,
	 *            nFrames) as a 4 element int array.
	 * @return a new {@link ImagePlus}
	 */

	public static final ImagePlus createLabelImagePlus( final Model model, final int[] dimensions, final Logger logger )
	{
		final long[] dims = new long[ 4 ];
		for ( int d = 0; d < dims.length; d++ )
			dims[ d ] = dimensions[ d ];

		final ImagePlus lblImp = ImageJFunctions.wrap( createLabelImg( model, dims, logger ), "LblImage" );
		lblImp.setDimensions( 1, dimensions[ 2 ], dimensions[ 3 ] );
		lblImp.setOpenAsHyperStack( true );
		lblImp.resetDisplayRange();
		return lblImp;
	}

	public static final Img< UnsignedShortType > createLabelImg( final Model model, final long[] dimensions )
	{
		return createLabelImg( model, dimensions, Logger.VOID_LOGGER );
	}

	/**
	 * @param model
	 * @param dimensions
	 *            the dimensions of the output image (width, height, nZSlices,
	 *            nFrames) as a 4 element int array.
	 *
	 * @return a new {@link ImagePlus}
	 */
	public static final Img< UnsignedShortType > createLabelImg( final Model model, final long[] dimensions, final Logger logger )
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
		final ImgPlus< UnsignedShortType > imgPlus = new ImgPlus<>( lblImg, "LblImg", axes );

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

		logger.log( "Writing label image." );
		for ( int frame = 0; frame < dimensions[ 3 ]; frame++ )
		{
			final ImgPlus< UnsignedShortType > imgC = HyperSliceImgPlus.fixChannelAxis( imgPlus, 0 );
			final ImgPlus< UnsignedShortType > imgCT = HyperSliceImgPlus.fixTimeAxis( imgC, frame );
			for ( final Spot spot : model.getSpots().iterable( frame, true ) )
			{
				final int id;
				final Integer trackID = model.getTrackModel().trackIDOf( spot );
				if ( null == trackID || !model.getTrackModel().isVisible( trackID ) )
					id = lonelySpotID.getAndIncrement();
				else
					id = 1 + trackID.intValue();

				final SpotNeighborhood< UnsignedShortType > neighborhood = new SpotNeighborhood< UnsignedShortType >( spot, imgCT );
				for ( final UnsignedShortType pixel : neighborhood )
					pixel.set( id );
			}
			logger.setProgress( ( double ) ( 1 + frame ) / dimensions[ 3 ] );
		}
		logger.log( "Done." );

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
		public TrackMateAction create( final TrackMateGUIController controller )
		{
			return new LabelImgExporter();
		}

		@Override
		public ImageIcon getIcon()
		{
			return ICON;
		}

		@Override
		public String getName()
		{
			return NAME;
		}
	}
}
