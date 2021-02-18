package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.gui.Icons.MAGNIFIER_ICON;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ImageIcon;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.trackscheme.SpotIconGrabber;
import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import net.imagej.ImgPlus;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.view.Views;

public class ExtractTrackStackAction extends AbstractTMAction
{

	public static final String NAME = "Extract track stack";

	public static final String KEY = "EXTRACT_TRACK_STACK";

	public static final String INFO_TEXT = "<html> "
			+ "Generate a stack of images taken from the track "
			+ "that joins two selected spots. "
			+ "<p> "
			+ "There must be exactly 1 or 2 spots selected for this action "
			+ "to work. If only one spot is selected, then the stack is extracted from "
			+ "the track it belongs to, from the first spot in time to the last in time. "
			+ "If there are two spots selected, they must belong to a track that connects "
			+ "them. A path is then found that joins them and the stack is extracted "
			+ "from this path."
			+ "<p> "
			+ "A stack of images will be generated from the spots that join "
			+ "them. A GUI allows specifying the size of the extract, in units of the largest "
			+ "spot in the track, and whether to capture a 2D or 3D stack over time. "
			+ "All channels are captured. " +
			"</html>";

	private static double diameterFactor = 1.5d;

	private static int dimChoice = 0;

	/**
	 * By how much we resize the capture window to get a nice border around the
	 * spot.
	 */
	private static final float RESIZE_FACTOR = 1.5f;

	@Override
	public void execute(
			final TrackMate trackmate,
			final SelectionModel selectionModel,
			final DisplaySettings displaySettings,
			final Frame parent )
	{
		// Show dialog.
		final GenericDialog dialog = new GenericDialog( "Extract track stack", parent );

		// Radius factor
		dialog.addSlider( "Image size (spot\ndiameter units):", 0.1, 5.1, diameterFactor );

		// Central slice vs 3D
		final String[] dimChoices = new String[] { "Central slice ", "3D" };
		dialog.addRadioButtonGroup( "Dimensionality:", dimChoices, 2, 1, dimChoices[ dimChoice ] );

		// Show & Read user input
		dialog.showDialog();
		if ( dialog.wasCanceled() )
			return;

		diameterFactor = dialog.getNextNumber();
		dimChoice = Arrays.asList( dimChoices ).indexOf( dialog.getNextRadioButton() );
		final boolean do3d = dimChoice == 1;

		logger.log( "Capturing " + ( do3d ? "3D" : "2D" ) + " track stack.\n" );

		final Model model = trackmate.getModel();
		final Set< Spot > selection = selectionModel.getSpotSelection();
		final int nspots = selection.size();
		if ( nspots != 2 )
		{
			if ( nspots == 1 )
			{
				final Spot spot = selection.iterator().next();

				// Put the path in the selection.
				final Integer trackID = model.getTrackModel().trackIDOf( spot );
				final List< Spot > spots = new ArrayList<>( model.getTrackModel().trackSpots( trackID ) );
				Collections.sort( spots, Spot.frameComparator );
				final Spot start = spots.get( 0 );
				final Spot end = spots.get( spots.size() - 1 );
				selectionModel.clearSelection();
				selectionModel.addSpotToSelection( start );
				selectionModel.addSpotToSelection( end );
				final List< DefaultWeightedEdge > edges = model.getTrackModel().dijkstraShortestPath( start, end );
				if ( null == edges )
				{
					logger.error( "The 2 spots are not connected.\nAborting\n" );
					return;
				}
				selectionModel.addEdgeToSelection( edges );

				// Get stack.
				final ImagePlus imp = trackStack( trackmate, spot, do3d, logger );
				imp.show();
				imp.setZ( imp.getNSlices() / 2 + 1 );
				imp.resetDisplayRange();
			}
			else
			{
				logger.error( "Expected 1 or 2 spots in the selection, got " + nspots + ".\nAborting.\n" );
				return;
			}
		}
		else
		{
			final Iterator< Spot > it = selection.iterator();
			final Spot start = it.next();
			final Spot end = it.next();

			// Put the path in the selection.
			selectionModel.clearSelection();
			selectionModel.addSpotToSelection( start );
			selectionModel.addSpotToSelection( end );
			final Spot start1;
			final Spot end1;
			if ( start.getFeature( Spot.POSITION_T ) > end.getFeature( Spot.POSITION_T ) )
			{
				end1 = start;
				start1 = end;
			}
			else
			{
				end1 = end;
				start1 = start;
			}
			final List< DefaultWeightedEdge > edges = model.getTrackModel().dijkstraShortestPath( start1, end1 );
			if ( null == edges )
			{
				logger.error( "The 2 spots are not connected.\nAborting\n" );
				return;
			}
			selectionModel.addEdgeToSelection( edges );

			// Get stack.
			final ImagePlus imp = trackStack( trackmate, start1, end1, do3d, logger );
			imp.show();
			imp.setZ( imp.getNSlices() / 2 + 1 );
			imp.resetDisplayRange();
		}
	}

	public static final ImagePlus trackStack(
			final TrackMate trackmate,
			final Spot spot,
			final boolean do3d,
			final Logger logger )
	{
		final Model model = trackmate.getModel();
		final Integer trackID = model.getTrackModel().trackIDOf( spot );
		final List< Spot > spots = new ArrayList<>( model.getTrackModel().trackSpots( trackID ) );
		Collections.sort( spots, Spot.frameComparator );
		final Spot start = spots.get( 0 );
		final Spot end = spots.get( spots.size() - 1 );
		return trackStack( trackmate, start, end, do3d, logger );
	}

	public static final ImagePlus trackStack(
			final TrackMate trackmate,
			final Spot start,
			final Spot end,
			final boolean do3d,
			final Logger logger )
	{
		final Model model = trackmate.getModel();
		final Spot start1;
		final Spot end1;
		if ( start.getFeature( Spot.POSITION_T ) > end.getFeature( Spot.POSITION_T ) )
		{
			end1 = start;
			start1 = end;
		}
		else
		{
			end1 = end;
			start1 = start;
		}
		final List< DefaultWeightedEdge > edges = model.getTrackModel().dijkstraShortestPath( start1, end1 );
		if ( null == edges )
		{
			logger.error( "The 2 spots are not connected.\nAborting\n" );
			return null;
		}

		/*
		 * Build spot list & Get largest diameter.
		 */
		final List< Spot > path = new ArrayList<>( edges.size() );
		path.add( start1 );
		Spot previous = start1;
		Spot current;
		double radius = Math.abs( start1.getFeature( Spot.RADIUS ) ) * diameterFactor;
		for ( final DefaultWeightedEdge edge : edges )
		{
			current = model.getTrackModel().getEdgeSource( edge );
			if ( current == previous )
			{
				current = model.getTrackModel().getEdgeTarget( edge );
			}
			path.add( current );
			final double ct = Math.abs( current.getFeature( Spot.RADIUS ) );
			if ( ct > radius )
			{
				radius = ct;
			}
			previous = current;
		}
		path.add( end1 );

		// Sort spot by ascending frame number
		final TreeSet< Spot > sortedSpots = new TreeSet<>( Spot.timeComparator );
		sortedSpots.addAll( path );
		return trackStack( trackmate.getSettings(), path, radius, do3d, logger );
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static final ImagePlus trackStack(
			final Settings settings,
			final List< Spot > path,
			final double radius,
			final boolean do3d,
			final Logger logger )
	{
		final int nspots = path.size();

		// Common coordinates
		final double[] calibration = TMUtils.getSpatialCalibration( settings.imp );
		final int width = ( int ) Math.ceil( 2 * radius * RESIZE_FACTOR / calibration[ 0 ] );
		final int height = ( int ) Math.ceil( 2 * radius * RESIZE_FACTOR / calibration[ 1 ] );
		final int depth;
		if ( do3d )
			depth = ( int ) Math.ceil( 2 * radius * RESIZE_FACTOR / calibration[ 2 ] );
		else
			depth = 1;

		// Extract target channel
		final ImgPlus img = TMUtils.rawWraps( settings.imp );

		// Prepare new image holder:
		final ImageStack stack = new ImageStack( width, height );

		// Iterate over set to grab imglib image
		int progress = 0;
		final int nChannels = settings.imp.getNChannels();

		for ( final Spot spot : path )
		{

			// Extract image for current frame
			final int frame = spot.getFeature( Spot.FRAME ).intValue();

			for ( int c = 0; c < nChannels; c++ )
			{
				final ImgPlus imgCT = TMUtils.hyperSlice( img, c, frame );

				// Compute target coordinates for current spot
				final int x = ( int ) ( Math.round( ( spot.getFeature( Spot.POSITION_X ) ) / calibration[ 0 ] ) - width / 2 );
				final int y = ( int ) ( Math.round( ( spot.getFeature( Spot.POSITION_Y ) ) / calibration[ 1 ] ) - height / 2 );
				long slice = 0;
				if ( imgCT.numDimensions() > 2 )
				{
					slice = Math.round( spot.getFeature( Spot.POSITION_Z ) / calibration[ 2 ] );
					if ( slice < 0 )
						slice = 0;

					if ( slice >= imgCT.dimension( 2 ) )
						slice = imgCT.dimension( 2 ) - 1;
				}

				final SpotIconGrabber< ? > grabber = new SpotIconGrabber( imgCT );
				if ( do3d )
				{
					final Img crop = grabber.grabImage( x, y, slice, width, height, depth );
					// Copy it so stack
					for ( int i = 0; i < crop.dimension( 2 ); i++ )
					{
						final ImageProcessor processor = ImageJFunctions.wrap( Views.hyperSlice( crop, 2, i ), crop.toString() ).getProcessor();
						stack.addSlice( spot.toString(), processor );
					}
				}
				else
				{
					final Img crop = grabber.grabImage( x, y, slice, width, height );
					stack.addSlice( spot.toString(), ImageJFunctions.wrap( crop, crop.toString() ).getProcessor() );
				}
			}
			logger.setProgress( ( float ) ( progress + 1 ) / nspots );
			progress++;
		}

		// Convert to plain ImageJ
		final ImagePlus stackTrack = new ImagePlus( "", stack );
		stackTrack.setTitle( "Path from " + path.get( 0 ) + " to " + path.get( path.size() - 1 ) );
		final Calibration impCal = stackTrack.getCalibration();
		impCal.setTimeUnit( settings.imp.getCalibration().getTimeUnit() );
		impCal.setUnit( settings.imp.getCalibration().getUnit() );
		impCal.pixelWidth = calibration[ 0 ];
		impCal.pixelHeight = calibration[ 1 ];
		impCal.pixelDepth = calibration[ 2 ];
		impCal.frameInterval = settings.dt;
		stackTrack.setDimensions( nChannels, depth, nspots );
		stackTrack.setOpenAsHyperStack( true );
		logger.log( "Done." );

		// Display it
		if ( nChannels > 1 )
		{
			final CompositeImage cmp = new CompositeImage( stackTrack, CompositeImage.COMPOSITE );
			if ( settings.imp instanceof CompositeImage )
			{
				final CompositeImage scmp = ( CompositeImage ) settings.imp;
				for ( int c = 0; c < nChannels; c++ )
					cmp.setChannelLut( scmp.getChannelLut( c + 1 ), c + 1 );
			}
			return cmp;
		}
		return stackTrack;
	}

	@Plugin( type = TrackMateActionFactory.class )
	public static class ExtractTrackStackActionFactory implements TrackMateActionFactory
	{

		@Override
		public String getInfoText()
		{
			return ExtractTrackStackAction.INFO_TEXT;
		}

		@Override
		public String getName()
		{
			return ExtractTrackStackAction.NAME;
		}

		@Override
		public String getKey()
		{
			return ExtractTrackStackAction.KEY;
		}

		@Override
		public ImageIcon getIcon()
		{
			return MAGNIFIER_ICON;
		}

		@Override
		public TrackMateAction create()
		{
			return new ExtractTrackStackAction();
		}
	}
}
