package fiji.plugin.trackmate.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ImageIcon;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.trackscheme.SpotIconGrabber;
import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import net.imagej.ImgPlus;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.meta.view.HyperSliceImgPlus;
import net.imglib2.view.Views;

@SuppressWarnings( "deprecation" )
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

	public static final ImageIcon ICON = new ImageIcon( TrackMateWizard.class.getResource( "images/magnifier.png" ) );

	/**
	 * By how much we resize the capture window to get a nice border around the
	 * spot.
	 */
	private static final float RESIZE_FACTOR = 1.5f;

	private final SelectionModel selectionModel;

	private final double radiusRatio;

	private final boolean do3d;

	/*
	 * CONSTRUCTOR
	 */

	public ExtractTrackStackAction( final SelectionModel selectionModel, final double radiusRatio, final boolean do3d )
	{
		this.selectionModel = selectionModel;
		this.radiusRatio = radiusRatio;
		this.do3d = do3d;
	}

	/*
	 * METHODS
	 */

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	@Override
	public void execute( final TrackMate trackmate )
	{
		logger.log( "Capturing " + ( do3d ? "3D" : "2D" ) + " track stack.\n" );

		final Model model = trackmate.getModel();
		final Set< Spot > selection = selectionModel.getSpotSelection();
		int nspots = selection.size();
		if ( nspots != 2 )
		{
			if ( nspots == 1 )
			{
				final Integer trackID = model.getTrackModel().trackIDOf( selectionModel.getSpotSelection().iterator().next() );
				final List< Spot > spots = new ArrayList<>( model.getTrackModel().trackSpots( trackID ) );
				Collections.sort( spots, Spot.frameComparator );
				selectionModel.clearSelection();
				selectionModel.addSpotToSelection( spots.get( 0 ) );
				selectionModel.addSpotToSelection( spots.get( spots.size() - 1 ) );
			}
			else
			{
				logger.error( "Expected 1 or 2 spots in the selection, got " + nspots + ".\nAborting.\n" );
				return;
			}
		}

		// Get start & end
		Spot tmp1, tmp2, start, end;
		final Iterator< Spot > it = selection.iterator();
		tmp1 = it.next();
		tmp2 = it.next();
		if ( tmp1.getFeature( Spot.POSITION_T ) > tmp2.getFeature( Spot.POSITION_T ) )
		{
			end = tmp1;
			start = tmp2;
		}
		else
		{
			end = tmp2;
			start = tmp1;
		}

		// Find path
		final List< DefaultWeightedEdge > edges = model.getTrackModel().dijkstraShortestPath( start, end );
		if ( null == edges )
		{
			logger.error( "The 2 spots are not connected.\nAborting\n" );
			return;
		}
		selectionModel.clearEdgeSelection();
		selectionModel.addEdgeToSelection( edges );

		// Build spot list
		// & Get largest diameter
		final List< Spot > path = new ArrayList<>( edges.size() );
		path.add( start );
		Spot previous = start;
		Spot current;
		double radius = Math.abs( start.getFeature( Spot.RADIUS ) ) * radiusRatio;
		for ( final DefaultWeightedEdge edge : edges )
		{
			current = model.getTrackModel().getEdgeSource( edge );
			if ( current == previous )
			{
				current = model.getTrackModel().getEdgeTarget( edge ); // We have to check both in case of bad oriented edges
			}
			path.add( current );
			final double ct = Math.abs( current.getFeature( Spot.RADIUS ) );
			if ( ct > radius )
			{
				radius = ct;
			}
			previous = current;
		}
		path.add( end );

		// Sort spot by ascending frame number
		final TreeSet< Spot > sortedSpots = new TreeSet<>( Spot.timeComparator );
		sortedSpots.addAll( path );
		nspots = sortedSpots.size();

		// Common coordinates
		final Settings settings = trackmate.getSettings();
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


		for ( final Spot spot : sortedSpots )
		{

			// Extract image for current frame
			final int frame = spot.getFeature( Spot.FRAME ).intValue();

			for ( int c = 0; c < nChannels; c++ )
			{
				final ImgPlus imgC = HyperSliceImgPlus.fixChannelAxis( img, c );
				final ImgPlus imgCT = HyperSliceImgPlus.fixTimeAxis( imgC, frame );

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
		stackTrack.setTitle( "Path from " + start + " to " + end );
		final Calibration impCal = stackTrack.getCalibration();
		impCal.setTimeUnit( settings.imp.getCalibration().getTimeUnit() );
		impCal.setUnit( settings.imp.getCalibration().getUnit() );
		impCal.pixelWidth = calibration[ 0 ];
		impCal.pixelHeight = calibration[ 1 ];
		impCal.pixelDepth = calibration[ 2 ];
		impCal.frameInterval = settings.dt;
		stackTrack.setDimensions( nChannels, depth, nspots );
		stackTrack.setOpenAsHyperStack( true );

		//Display it
		if ( nChannels > 1 )
		{
			final CompositeImage cmp = new CompositeImage( stackTrack, CompositeImage.COMPOSITE );
			if ( settings.imp instanceof CompositeImage )
			{
				final CompositeImage scmp = ( CompositeImage ) settings.imp;
				for ( int c = 0; c < nChannels; c++ )
					cmp.setChannelLut( scmp.getChannelLut( c+1 ), c+1 );
			}

			cmp.show();
			cmp.setZ( depth / 2 + 1 );
			cmp.resetDisplayRange();
		}
		else
		{
			stackTrack.show();
			stackTrack.setZ( depth / 2 + 1 );
			stackTrack.resetDisplayRange();
		}

		logger.log( "Done." );
	}
}
