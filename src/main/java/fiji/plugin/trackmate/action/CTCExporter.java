package fiji.plugin.trackmate.action;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.action.LabelImgExporter.SpotRoiWriter;
import fiji.plugin.trackmate.graph.ConvexBranchesDecomposition;
import fiji.plugin.trackmate.graph.ConvexBranchesDecomposition.TrackBranchDecomposition;
import fiji.plugin.trackmate.graph.GraphUtils;
import fiji.plugin.trackmate.graph.TimeDirectedNeighborIndex;
import fiji.plugin.trackmate.io.TmXmlWriter;
import fiji.plugin.trackmate.util.TMUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Util;

/**
 * An exporter that saves the current TrackMate session as files following the
 * Cell-Tracking-Challenge convention.
 * <p>
 * See http://celltrackingchallenge.net/
 * 
 * @author Jean-Yves Tinevez
 *
 */
public class CTCExporter
{

	private static final Function< Integer, String > nameGen = i -> String.format( "%02d", i );

	public enum ExportType
	{
		GOLD_TRUTH( "Gold truth", "_GT" ),
		SILVER_TRUTH( "Silver truth", "_ST" ),
		RESULTS( "Results", "_RES" );

		private final String label;

		private final String suffix;

		ExportType( final String label, final String suffix )
		{
			this.label = label;
			this.suffix = suffix;
		}

		@Override
		public String toString()
		{
			return label;
		}

		public String suffix()
		{
			return suffix;
		}

		public Path getTrackTextFilePath( final String exportRootFolder, final int saveId )
		{
			switch ( this )
			{
			case GOLD_TRUTH:
			case SILVER_TRUTH:
				return Paths.get( exportRootFolder, nameGen.apply( saveId ) + suffix, "TRA", "man_track.txt" );
			case RESULTS:
				return Paths.get( exportRootFolder, nameGen.apply( saveId ) + suffix, "res_track.txt" );
			default:
				throw new IllegalArgumentException( "Unknown export type: " + this );
			}
		}

		public Path getTrackTifFilePath( final String exportRootFolder, final int saveId, final long frame, final int nFrames )
		{
			switch ( this )
			{
			case GOLD_TRUTH:
			case SILVER_TRUTH:
			{
				final Function< Long, String > tifNameGen = nFrames > 999
						? i -> String.format( "man_track%04d.tif", i )
						: i -> String.format( "man_track%03d.tif", i );
				final String name = tifNameGen.apply( frame );
				return Paths.get( exportRootFolder, nameGen.apply( saveId ) + suffix, "TRA", name );
			}
			case RESULTS:
			{
				final Function< Long, String > tifNameGen = nFrames > 999
						? i -> String.format( "mask%04d.tif", i )
						: i -> String.format( "mask%03d.tif", i );
				final String name = tifNameGen.apply( frame );
				return Paths.get( exportRootFolder, nameGen.apply( saveId ) + suffix, name );
			}
			default:
				throw new IllegalArgumentException( "Unknown export type: " + this );
			}
		}
	}

	public static String exportAll( final String exportRootFolder, final TrackMate trackmate, final ExportType exportType, final Logger logger ) throws IOException
	{
		if ( !new File( exportRootFolder ).exists() )
			new File( exportRootFolder ).mkdirs();

		logger.log( "Exporting as CTC type: " + exportType.toString() + '\n' );
		final int id = getAvailableDatasetID( exportRootFolder );
		exportOriginalImageData( exportRootFolder, id, trackmate, logger );
		final String resultsFolder = exportTrackingData( exportRootFolder, id, exportType, trackmate, logger );
		if ( exportType != ExportType.RESULTS )
			exportSegmentationData( exportRootFolder, id, exportType, trackmate, logger );
		else
			exportSettingsFile( exportRootFolder, id, trackmate, logger );
		logger.log( "Export done.\n" );
		return resultsFolder;
	}

	/**
	 * Saves the settings part as XML for reference.
	 * 
	 * @param exportRootFolder
	 *            the root folder for exporting.
	 * @param saveId
	 *            the id of the save file (<code>exportRootFolder/01</code>,
	 *            <code>exportRootFolder/02</code>, etc)
	 * @param trackmate
	 *            the trackmate to export.
	 * @param logger
	 *            a logger to report progress.
	 */
	public static void exportSettingsFile( final String exportRootFolder, final int saveId, final TrackMate trackmate, final Logger logger ) throws IOException
	{
		final Path path = ExportType.RESULTS.getTrackTextFilePath( exportRootFolder, saveId );
		Files.createDirectories( path.getParent() );
		final File settingsPath = new File( path.getParent().toFile(), "TrackMateSettings.xml" );
		logger.log( "Exporting TrackMate settings file to " + settingsPath + '\n' );
		final TmXmlWriter writer = new TmXmlWriter( settingsPath, logger );
		writer.appendSettings( trackmate.getSettings() );
		writer.writeToFile();
		logger.log( "Done.\n" );
	}

	/**
	 * Returns a suitable dataset id to save in the specified folder.
	 * <p>
	 * For instance the first id return will be '1', which means that the
	 * original image data will be saved under the folder '01'. If '01' already
	 * exists, then this method will return 2, etc.
	 * 
	 * @param exportRootFolder
	 *            the root folder in which to export the data.
	 * @return an integer id that can be passed in the other method of this
	 *         class.
	 */
	public static int getAvailableDatasetID( final String exportRootFolder )
	{
		int i = 1;
		Path savePath1 = Paths.get( exportRootFolder, nameGen.apply( i ) );
		Path savePath2 = Paths.get( exportRootFolder, nameGen.apply( i ) + ExportType.GOLD_TRUTH.suffix );
		Path savePath3 = Paths.get( exportRootFolder, nameGen.apply( i ) + ExportType.SILVER_TRUTH.suffix );
		Path savePath4 = Paths.get( exportRootFolder, nameGen.apply( i ) + ExportType.RESULTS.suffix );
		while ( Files.exists( savePath1 ) || Files.exists( savePath2 ) || Files.exists( savePath3 ) || Files.exists( savePath4 ) )
		{
			i++;
			savePath1 = Paths.get( exportRootFolder, nameGen.apply( i ) );
			savePath2 = Paths.get( exportRootFolder, nameGen.apply( i ) + ExportType.GOLD_TRUTH.suffix );
			savePath3 = Paths.get( exportRootFolder, nameGen.apply( i ) + ExportType.SILVER_TRUTH.suffix );
			savePath4 = Paths.get( exportRootFolder, nameGen.apply( i ) + ExportType.RESULTS.suffix );
		}
		return i;
	}

	public static void exportOriginalImageData( final String exportRootFolder, final int saveId, final TrackMate trackmate, final Logger logger ) throws IOException
	{
		exportOriginalImageData( exportRootFolder, saveId, trackmate.getSettings().imp, logger );
	}

	public static void exportOriginalImageData( final String exportRootFolder, final int saveId, final ImagePlus imp, final Logger logger ) throws IOException
	{
		if ( imp == null )
			return;

		final Path savePath = Paths.get( exportRootFolder, String.format( "%02d", saveId ) );
		if ( Files.exists( savePath ) )
		{
			final String msg = "Cannot save to " + savePath + ". Folder already exists.";
			logger.error( msg );
			return;
		}

		Files.createDirectory( savePath );
		logger.log( "Exporting original image to " + savePath.toString() );


		final int nFrames = imp.getNFrames();
		final String format = ( nFrames > 999 ) ? "t%04d.tif" : "t%03d.tif";
		final Duplicator duplicator = new Duplicator();
		final int firstC = 1;
		final int lastC = imp.getNChannels();
		final int firstZ = 1;
		final int lastZ = imp.getNSlices();

		for ( int frame = 0; frame < nFrames; frame++ )
		{
			final ImagePlus tp = duplicator.run( imp, firstC, lastC, firstZ, lastZ, frame + 1, frame + 1 );
			IJ.saveAsTiff( tp, Paths.get( savePath.toString(), String.format( format, frame ) ).toString() );
		}
		logger.log( ". Done.\n" );
	}

	/**
	 * Writes the segmentation ground-truth files.
	 * <p>
	 * Only exports the spots that have a ROI, and write only the frames that
	 * have at least one spot with a ROI.
	 * 
	 * @param exportRootFolder
	 *            the root of the export folder.
	 * @param saveId
	 *            the id under which to save the data.
	 * @param exportType
	 *            the export type. Must be {@link ExportType#GOLD_TRUTH} or
	 *            {@link ExportType#SILVER_TRUTH}.
	 * @param trackmate
	 *            the TrackMate object to export.
	 * @param logger
	 *            a {@link Logger} instance to report progress.
	 * @throws IOException
	 *             if a problem happens during writing.
	 */
	public static void exportSegmentationData( final String exportRootFolder, final int saveId, final ExportType exportType, final TrackMate trackmate, final Logger logger ) throws IOException
	{
		// Create image holder to write labels in.
		final ImagePlus imp = trackmate.getSettings().imp;
		final long[] dims;
		if ( imp != null )
		{
			final int[] dimensions = imp.getDimensions();
			dims = new long[] { dimensions[ 0 ], dimensions[ 1 ], dimensions[ 3 ], dimensions[ 4 ] };
		}
		else
		{
			final Settings s = trackmate.getSettings();
			dims = new long[] { s.width, s.height, s.nslices, s.nframes };
		}
		final double[] calibration = new double[] {
				imp.getCalibration().pixelWidth,
				imp.getCalibration().pixelHeight,
				imp.getCalibration().pixelDepth,
				imp.getCalibration().frameInterval
		};
		final ImgPlus< UnsignedShortType > labelImg = createLabelImg( dims, calibration );

		// Write labels in.
		final Model model = trackmate.getModel();
		final AtomicInteger idGen = new AtomicInteger( 1 );
		final Set< Integer > framesToWrite = new HashSet<>();
		for ( int frame = 0; frame < dims[ 3 ]; frame++ )
		{
			final ImgPlus< UnsignedShortType > imgCT = TMUtils.hyperSlice( labelImg, 0, frame );
			final SpotRoiWriter spotWriter = new SpotRoiWriter( imgCT );

			for ( final Spot spot : model.getSpots().iterable( frame, true ) )
			{
				if ( spot.getRoi() == null )
					continue;
				final int id = idGen.getAndIncrement();
				spotWriter.write( spot, id );
				framesToWrite.add( Integer.valueOf( frame ) );
			}
		}

		/*
		 * Now export the label image.
		 */

		final Path path = Paths.get( exportRootFolder, nameGen.apply( saveId ) + exportType.suffix(), "SEG" );
		Files.createDirectories( path );
		logger.log( "Exporting segmentation mask files to " + path.toString() );

		final int nFrames = imp.getNFrames();
		final Function< Long, String > tifNameGen = nFrames > 999
				? i -> String.format( "man_seg%04d.tif", i )
				: i -> String.format( "man_seg%03d.tif", i );
				
		// Only save frames with spots in.
		for ( final int frame : framesToWrite )
		{
			final ImgPlus< UnsignedShortType > imgCT = TMUtils.hyperSlice( labelImg, 0, frame );
			final String name = tifNameGen.apply( ( long ) frame );
			final ImagePlus tp = ImageJFunctions.wrapUnsignedShort( imgCT, name );

			final Path pathTif = Paths.get( exportRootFolder, nameGen.apply( saveId ) + exportType.suffix(), "SEG", name );
			IJ.saveAsTiff( tp, pathTif.toString() );
		}
		logger.log( ". Done.\n" );
	}

	public static String exportTrackingData( final String exportRootFolder, final int saveId, final ExportType exportType, final TrackMate trackmate, final Logger logger ) throws FileNotFoundException, IOException
	{
		/*
		 * Check for future consistency errors. It is possible that 2 spots are
		 * so close than the mask of one completely erase the other. This will
		 * trigger an error in the CTC metrics calculation so we have to protect
		 * ourselves from it. This solution consists in check whether that is
		 * the case, and if yes, removing the erased spot from the smallest
		 * track.
		 */
		final Model model2 = sanitizeAndCopy( trackmate.getModel() );

		// What we need to decompose tracks in branches.
		final TrackModel trackModel = model2.getTrackModel();
		final TimeDirectedNeighborIndex neighborIndex = model2.getTrackModel().getDirectedNeighborIndex();

		// Sanity check.
		if ( !GraphUtils.isTree( trackModel, neighborIndex ) )
		{
			final String msg = "Cannot perform CTC export of tracks that have fusion events.";
			logger.error( msg );
			return null;
		}

		// Create image holder to write labels in.
		final ImagePlus imp = trackmate.getSettings().imp;
		final long[] dims;
		if ( imp != null )
		{
			final int[] dimensions = imp.getDimensions();
			dims = new long[] { dimensions[ 0 ], dimensions[ 1 ], dimensions[ 3 ], dimensions[ 4 ] };
		}
		else
		{
			final Settings s = trackmate.getSettings();
			dims = new long[] { s.width, s.height, s.nslices, s.nframes };
		}
		final double[] calibration = new double[] {
				imp.getCalibration().pixelWidth,
				imp.getCalibration().pixelHeight,
				imp.getCalibration().pixelDepth,
				imp.getCalibration().frameInterval
		};
		final ImgPlus< UnsignedShortType > labelImg = createLabelImg( dims, calibration );

		// Configure the convex branch decomposition.
		final boolean forbidMiddleLinks = true;
		final boolean forbidGaps = true;
		final AtomicInteger branchIDGen = new AtomicInteger( 1 );

		// Map of vertex to their ID in the file. Initially empty.
		final Map< List< Spot >, Integer > branchID = new HashMap<>();

		final Path path = exportType.getTrackTextFilePath( exportRootFolder, saveId );
		Files.createDirectories( path.getParent() );
		logger.log( "Exporting tracking text file to " + path.toString() );

		try (FileOutputStream fos = new FileOutputStream( path.toFile() );
				BufferedWriter bw = new BufferedWriter( new OutputStreamWriter( fos ) ))
		{

			for ( final Integer trackID : trackModel.trackIDs( true ) )
			{
				// Decompose the track.
				final TrackBranchDecomposition decomposition = ConvexBranchesDecomposition.processTrack( trackID, trackModel, neighborIndex, forbidMiddleLinks, forbidGaps );
				final SimpleDirectedGraph< List< Spot >, DefaultEdge > branchGraph = ConvexBranchesDecomposition.buildBranchGraph( decomposition );

				// Find the first branch (the one with no parent).
				List< Spot > start = null;
				for ( final List< Spot > vertex : branchGraph.vertexSet() )
				{
					if ( branchGraph.incomingEdgesOf( vertex ).isEmpty() )
					{
						start = vertex;
						break;
					}
				}

				// Iterate from the start.
				final BreadthFirstIterator< List< Spot >, DefaultEdge > bfi = new BreadthFirstIterator<>( branchGraph, start );
				while ( bfi.hasNext() )
				{
					final List< Spot > current = bfi.next();
					final int currentID = branchIDGen.getAndIncrement();
					branchID.put( current, Integer.valueOf( currentID ) );

					/*
					 * Write spot label into output image.
					 */

					for ( final Spot spot : current )
					{
						final long frame = spot.getFeature( Spot.FRAME ).longValue();
						final ImgPlus< UnsignedShortType > imgCT = TMUtils.hyperSlice( labelImg, 0, frame );
						final SpotRoiWriter spotRoiWriter = new SpotRoiWriter( imgCT );
						spotRoiWriter.write( spot, currentID );
					}

					/*
					 * Text file.
					 */

					// Get parent ID, if any.
					int parentID;
					if ( branchGraph.incomingEdgesOf( current ).isEmpty() )
					{
						parentID = 0;
					}
					else
					{
						final DefaultEdge edge = branchGraph.incomingEdgesOf( current ).iterator().next();
						final List< Spot > parent = Graphs.getOppositeVertex( branchGraph, edge, current );
						final Integer obj = branchID.get( parent );
						parentID = ( obj == null ) ? 0 : obj.intValue();
					}

					// Start and finish frame.
					final int startFrame = current.get( 0 ).getFeature( Spot.FRAME ).intValue();
					final int endFrame = current.get( current.size() - 1 ).getFeature( Spot.FRAME ).intValue();

					// Write it down.
					final int L = currentID; // track label
					final int B = startFrame; // track begins
					final int E = endFrame; // track ends
					final int P = parentID; // parent ID
					bw.write( String.format( "%d %d %d %d", L, B, E, P ) );
					bw.newLine();
				}
			}
		}
		logger.log( ". Done.\n" );

		/*
		 * Now export the label image.
		 */

		final int nFrames = imp.getNFrames();
		final Path pathTif0 = exportType.getTrackTifFilePath( exportRootFolder, saveId, 0, nFrames );
		logger.log( "Exporting tracking mask files to " + pathTif0.getParent().toString() );

		for ( long frame = 0; frame < dims[ 3 ]; frame++ )
		{
			final ImgPlus< UnsignedShortType > imgCT = TMUtils.hyperSlice( labelImg, 0, frame );
			final Path pathTif = exportType.getTrackTifFilePath( exportRootFolder, saveId, frame, nFrames );
			final String name = pathTif.getFileName().toString();
			final ImagePlus tp = ImageJFunctions.wrapUnsignedShort( imgCT, name );
			IJ.saveAsTiff( tp, pathTif.toString() );
		}
		logger.log( ". Done.\n" );

		// Return the results folder.
		return pathTif0.getParent().toString();
	}

	/**
	 * Returns the folder in which the tracking data will be exported.
	 * 
	 * @param exportRootFolder
	 *            the root folder to export in.
	 * @param saveId
	 *            the save id.
	 * @param exportType
	 *            the export type.
	 * @param trackmate
	 *            the TrackMate object to eport.
	 * @return a path as a string.
	 * @see #exportTrackingData(String, int, ExportType, TrackMate, Logger)
	 */
	public static final String getExportTrackingDataPath( final String exportRootFolder, final int saveId, final ExportType exportType, final TrackMate trackmate )
	{
		final Path pathTif0 = exportType.getTrackTifFilePath( exportRootFolder, saveId, 0, trackmate.getSettings().imp.getNFrames() );
		return pathTif0.getParent().toString();
	}

	/**
	 * Check for future consistency errors.
	 * <p>
	 * It is possible that 2 spots are so close than the mask of one completely
	 * erase the other. This will trigger an error in the CTC metrics
	 * calculation so we have to protect ourselves from it. This solution
	 * consists in check whether that is the case, and if yes, removing the
	 * erased spot from the smallest track.
	 * <p>
	 * This fix is approximate: the calculus for complete overlap assume the
	 * spots are spherical with a fudge factor.
	 */
	private static final Model sanitizeAndCopy( final Model model )
	{
		final double fudgeFactor = 1.2; // 20%
		final Model copy = model.copy();

		final SpotCollection allSpots = copy.getSpots();
		final TrackModel trackModel = copy.getTrackModel();
		for ( final Integer frame : allSpots.keySet() )
		{
			final List< Spot > spots = new ArrayList<>();
			allSpots.iterable( frame, true ).forEach( spots::add );
			final int nSpots = spots.size();
			for ( int i = 0; i < nSpots; i++ )
			{
				final Spot s1 = spots.get( i );
				final double r1 = s1.getFeature( Spot.RADIUS ).doubleValue();
				for ( int j = i + 1; j < nSpots; j++ )
				{
					final Spot s2 = spots.get( j );
					final double r2 = s2.getFeature( Spot.RADIUS ).doubleValue();
					final double d = Math.sqrt( s1.squareDistanceTo( s2 ) );
					
					if ( fudgeFactor * r1 > ( d + r2 ) || fudgeFactor * r2 > ( d + r1 ) )
					{
						// They overlap too much. We must fix this.
						final Integer id1 = trackModel.trackIDOf( s1 );
						final Set< Spot > track1 = trackModel.trackSpots( id1 );
						if ( track1 == null )
						{
							try
							{
								model.removeSpot( s1 );
							}
							finally
							{
								model.endUpdate();
							}
							continue;
						}
						final int n1 = track1.size();

						final Integer id2 = trackModel.trackIDOf( s2 );
						final Set< Spot > track2 = trackModel.trackSpots( id2 );
						if ( track2 == null )
						{
							try
							{
								model.removeSpot( s2 );
							}
							finally
							{
								model.endUpdate();
							}
							continue;
						}
						final int n2 = track2.size();

						final Spot toRemove = ( n2 > n1 ) ? s1 : s2;

						// To mend the edges later.
						final List< Spot > sources = new ArrayList<>();
						final List< Spot > targets = new ArrayList<>();
						final Set< DefaultWeightedEdge > edges = trackModel.edgesOf( toRemove );
						for ( final DefaultWeightedEdge edge : edges )
						{
							final Spot source = trackModel.getEdgeSource( edge );
							if ( source == toRemove )
								targets.add( trackModel.getEdgeTarget( edge ) );
							else
								sources.add( trackModel.getEdgeSource( edge ) );
						}
						
						model.beginUpdate();
						try
						{
							for ( final Spot source : sources )
								for ( final Spot target : targets )
									model.addEdge( source, target, -1. );

							model.removeSpot( toRemove );
						}
						finally
						{
							model.endUpdate();
						}
					}
				}
			}
		}
		return copy;
	}

	/**
	 * Creates a new label {@link ImgPlus} suitable to be used to write spot
	 * labels in. It is initially empty.
	 *
	 * @param dimensions
	 *            the desired dimensions of the output image (width, height,
	 *            nZSlices, nFrames) as a 4 element int array. Spots outside
	 *            these dimensions are ignored.
	 *
	 * @return a new {@link ImgPlus}.
	 */
	private static final ImgPlus< UnsignedShortType > createLabelImg(
			final long[] dimensions,
			final double[] calibration )
	{
		final Dimensions targetSize = FinalDimensions.wrap( dimensions );
		final Img< UnsignedShortType > lblImg = Util.getArrayOrCellImgFactory( targetSize, new UnsignedShortType() ).create( targetSize );
		final AxisType[] axes = new AxisType[] {
				Axes.X,
				Axes.Y,
				Axes.Z,
				Axes.TIME };
		final ImgPlus< UnsignedShortType > imgPlus = new ImgPlus<>( lblImg, "LblImg", axes, calibration );
		return imgPlus;
	}
}
