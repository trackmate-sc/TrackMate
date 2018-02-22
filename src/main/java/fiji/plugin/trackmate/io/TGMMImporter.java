package fiji.plugin.trackmate.io;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.OutputAlgorithm;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.util.Util;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;

public class TGMMImporter implements OutputAlgorithm< Model >, Benchmark
{

	private static final FilenameFilter xmlFilter = new FilenameFilter()
	{
		@Override
		public boolean accept( final File folder, final String name )
		{
			return ( name.toLowerCase().endsWith( ".xml" ) );
		}
	};

	private static final String BASE_ERROR_MSG = "[TGMMImporter] ";

	private static final String XML_DETECTION_NAME = "GaussianMixtureModel";

	private static final String XML_CENTROID = "m";

	private static final String XML_ID = "id";

	private static final String XML_SCORE = "splitScore";

	private static final String XML_NU = "nu";

	private static final String XML_PRECISION_MATRIX = "W";

	private static final String XML_LINEAGE = "lineage";

	private static final String XML_PARENT = "parent";

	private static final Pattern DEFAULT_PATTERN = Pattern.compile( ".+_frame(\\d+)\\.xml" );

	private final File file;

	private String errorMessage;

	private final Pattern framePattern;

	private Model model;

	private final List< AffineTransform3D > transforms;

	private final Logger logger;

	private long processingTime;

	/*
	 * CONSTRUCTORS
	 */

	public TGMMImporter( final File file, final List< AffineTransform3D > transforms, final Pattern framePattern, final Logger logger )
	{
		this.file = file;
		this.framePattern = framePattern;
		this.transforms = transforms;
		this.logger = logger;
	}

	public TGMMImporter( final File file, final List< AffineTransform3D > transforms, final Pattern framePattern )
	{
		this( file, transforms, framePattern, Logger.VOID_LOGGER );
	}

	public TGMMImporter( final File file, final List< AffineTransform3D > transforms )
	{
		this( file, transforms, DEFAULT_PATTERN );
	}

	public TGMMImporter( final File file, final List< AffineTransform3D > transforms, final Logger logger )
	{
		this( file, transforms, DEFAULT_PATTERN, logger );
	}

	/*
	 * METHODS
	 */

	@Override
	public boolean checkInput()
	{
		if ( !file.exists() )
		{
			errorMessage = BASE_ERROR_MSG + "Folder " + file + " does not exist.\n";
			return false;
		}
		if ( !file.canRead() )
		{
			errorMessage = BASE_ERROR_MSG + "Folder " + file + " cannot be read.\n";
			return false;
		}
		if ( !file.isDirectory() )
		{
			errorMessage = BASE_ERROR_MSG + file + " is not a folder.\n";
			return false;
		}

		if ( file.listFiles( xmlFilter ).length == 0 )
		{
			errorMessage = BASE_ERROR_MSG + "Folder " + file + " does not contain XML files.\n";
			return false;
		}
		return true;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		model = new Model();
		final SpotCollection sc = new SpotCollection();
		final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph = new SimpleWeightedGraph< >( DefaultWeightedEdge.class );

		final double[] targetCoordsHolder = new double[ 3 ];
		final double[] sourceCoordsHolder = new double[ 3 ];

		/*
		 * Grab all the XML files
		 */

		final File[] xmlFiles = file.listFiles( xmlFilter );

		/*
		 * Extract frame information from filename. It is not stored elsewhere
		 * so we have to rely on a specific pattern to get it. Note that it is
		 * not robust at all.
		 */
		logger.setProgress( 0d );
		logger.setStatus( "Importing TGMM files." );
		final int[] frames = new int[ xmlFiles.length ];
		logger.log( "Importing " + frames.length + " TGMM files.\n" );

		for ( int i = 0; i < frames.length; i++ )
		{
			final String name = xmlFiles[ i ].getName();
			final Matcher matcher = framePattern.matcher( name );
			if ( !matcher.matches() )
			{
				errorMessage = BASE_ERROR_MSG + "File " + name + " does not match the name pattern.\n";
				return false;
			}
			final String strFrame = matcher.group( 1 );
			try
			{
				final int frame = Integer.parseInt( strFrame );
				frames[ i ] = frame;
			}
			catch ( final NumberFormatException nfe )
			{
				errorMessage = BASE_ERROR_MSG + "Could not retrieve frame number from file " + file + ".\n" + nfe.getMessage() + "\n";
				return false;
			}
		}

		/*
		 * Read XML
		 */

		File xmlFile = null;
		try
		{
			final SAXBuilder saxBuilder = new SAXBuilder();

			Map< Integer, Spot > previousSpotID = null;
			Map< Integer, Spot > currentSpotID;

			for ( int t = 0; t < frames.length; t++ )
			{
				logger.log( "Processing frame " + frames[ t ] + ". " );
				final AffineTransform3D transform = transforms.get( frames[ t ] );

				xmlFile = xmlFiles[ t ];
				final Document doc = saxBuilder.build( xmlFile );
				final Element root = doc.getRootElement();
				final List< Element > detectionEls = root.getChildren( XML_DETECTION_NAME );

				final Collection< Spot > spots = new ArrayList< >( detectionEls.size() );

				/*
				 * Parse all detections
				 */

				currentSpotID = new HashMap< >( detectionEls.size() );

				for ( final Element detectionEl : detectionEls )
				{

					/*
					 * Fetch and check attribute strings.
					 */

					final String pixelPosStr = detectionEl.getAttributeValue( XML_CENTROID );
					if ( null == pixelPosStr )
					{
						errorMessage = BASE_ERROR_MSG + "Element " + detectionEl + " in file " + xmlFile + " misses the centroid attribute (" + XML_CENTROID + ").\n";
						return false;
					}
					final String[] pixelPosStrs = pixelPosStr.split( " " );

					final String idStr = detectionEl.getAttributeValue( XML_ID );
					if ( null == idStr )
					{
						errorMessage = BASE_ERROR_MSG + "Element " + detectionEl + " in file " + xmlFile + " misses the ID attribute (" + XML_ID + ").\n";
						return false;
					}

					final String lineageStr = detectionEl.getAttributeValue( XML_LINEAGE );
					if ( null == lineageStr )
					{
						errorMessage = BASE_ERROR_MSG + "Element " + detectionEl + " in file " + xmlFile + " misses the lineage attribute (" + XML_LINEAGE + ").\n";
						return false;
					}

					final String parentStr = detectionEl.getAttributeValue( XML_PARENT );
					if ( null == parentStr )
					{
						errorMessage = BASE_ERROR_MSG + "Element " + detectionEl + " in file " + xmlFile + " misses the parent attribute (" + XML_LINEAGE + ").\n";
						return false;
					}

					final String scoreStr = detectionEl.getAttributeValue( XML_SCORE );
					if ( null == scoreStr )
					{
						errorMessage = BASE_ERROR_MSG + "Element " + detectionEl + " in file " + xmlFile + " misses the score attribute (" + XML_SCORE + ").\n";
						return false;
					}

					final String nuStr = detectionEl.getAttributeValue( XML_NU );
					if ( null == nuStr )
					{
						errorMessage = BASE_ERROR_MSG + "Element " + detectionEl + " in file " + xmlFile + " misses the nu attribute (" + XML_NU + ").\n";
						return false;
					}

					final String precMatStr = detectionEl.getAttributeValue( XML_PRECISION_MATRIX );
					if ( null == precMatStr )
					{
						errorMessage = BASE_ERROR_MSG + "Element " + detectionEl + " in file " + xmlFile + " misses the prevision matrix attribute (" + XML_PRECISION_MATRIX + ").\n";
						return false;
					}
					final String[] precMatStrs = precMatStr.split( " " );

					/*
					 * Parse attribute strings.
					 */

					try
					{
						/*
						 * Build position
						 */

						{
							final double x = Double.parseDouble( pixelPosStrs[ 0 ] );
							final double y = Double.parseDouble( pixelPosStrs[ 1 ] );
							final double z = Double.parseDouble( pixelPosStrs[ 2 ] );

							/*
							 * Map it back to global coordinate system.
							 */

							sourceCoordsHolder[ 0 ] = x;
							sourceCoordsHolder[ 1 ] = y;
							sourceCoordsHolder[ 2 ] = z;
						}

						transform.apply( sourceCoordsHolder, targetCoordsHolder );

						final double mx = targetCoordsHolder[ 0 ];
						final double my = targetCoordsHolder[ 1 ];
						final double mz = targetCoordsHolder[ 2 ];

						/*
						 * ID and parent and lineage and score.
						 */

						final int id = Integer.parseInt( idStr );
						final double score = Double.parseDouble( scoreStr );
						final int lineage = Integer.parseInt( lineageStr );
						final int parent = Integer.parseInt( parentStr );

						/*
						 * Shape and radius
						 */

						final double nu = Double.parseDouble( nuStr );
						final double[] vals = new double[ 9 ];
						for ( int j = 0; j < vals.length; j++ )
						{
							vals[ j ] = nu * Double.parseDouble( precMatStrs[ j ] );
						}
						final Matrix precMat = new Matrix( vals, 3 );
						final Matrix covMat = precMat.inverse();

						/*
						 * Scale shape properly
						 */

						final double[][] S = covMat.getArray();
						final double[][] T = new double[ 3 ][ 3 ];
						for ( int r = 0; r < 3; ++r )
							for ( int c = 0; c < 3; ++c )
								T[ r ][ c ] = transform.get( r, c );
						final double[][] TS = new double[ 3 ][ 3 ];
						LinAlgHelpers.mult( T, S, TS );
						LinAlgHelpers.multABT( TS, T, S );
						// note that by writing to S we write the internal array
						// of covMat.

						/*
						 * Build a mean radius
						 */

						final double nSigmas = 2; // ellipsoid is at nSigmas std
													// devs of the Gaussian
						final EigenvalueDecomposition eig = covMat.eig();
						final double[] radii = eig.getRealEigenvalues();
						for ( int i = 0; i < radii.length; ++i )
							radii[ i ] = Math.sqrt( radii[ i ] );
						final double radius = nSigmas * Util.average( radii );

						/*
						 * Make a spot and add it to this frame collection.
						 */

						final Spot spot = new Spot( mx, my, mz, radius, score, lineage + " (" + id + ")" );
						spots.add( spot );
						currentSpotID.put( Integer.valueOf( id ), spot );

						graph.addVertex( spot );
						if ( parent >= 0 && previousSpotID != null )
						{
							final Spot source = previousSpotID.get( Integer.valueOf( parent ) );
							if ( null == source )
							{
								System.out.println( BASE_ERROR_MSG + "The parent of the current spot (frame " + frames[ t ] + ", id = " + id + " could not be found (was expected in frame " + ( frames[ t ] - 1 ) + " with id = " + parent + ".\n" );
								continue;
							}
							final DefaultWeightedEdge edge = graph.addEdge( source, spot );
							if ( null == edge )
							{
								System.out.println( BASE_ERROR_MSG + "Trouble adding edge between " + source + " and " + spot + ". Edge already exists?" );
								continue;
							}
						}
					}
					catch ( final NumberFormatException nfe )
					{
						errorMessage = BASE_ERROR_MSG + "Could not parse attributes of element " + detectionEl + " in xmlFile " + xmlFile + ".\n" + nfe.getMessage() + "\n";
//					return false;
						System.out.println( errorMessage );
						continue;
					}
				}

				/*
				 * Finished inspecting a frame. Store it in the spot collection.
				 */

				sc.put( frames[ t ], spots );
				previousSpotID = currentSpotID;
				logger.log( "Found " + spots.size() + " spots.\n" );
				logger.setProgress( ( double ) t / frames.length );
			}
		}
		catch ( final JDOMException e )
		{
			errorMessage = BASE_ERROR_MSG + "File " + xmlFile + " is not a poperly formed XML file.\n" + e.getMessage() + "\n";
			return false;
		}
		catch ( final IOException e )
		{
			errorMessage = BASE_ERROR_MSG + "Could not open file " + xmlFile + " for reading.\n" + e.getMessage() + "\n";
			return false;
		}
		finally
		{
			sc.setVisible( true );
			model.setSpots( sc, false );
			model.setTracks( graph, false );

			final long end = System.currentTimeMillis();
			processingTime = end - start;
			logger.setProgress( 0d );
			logger.log( String.format( "Import completed in %.1f s.\n", ( processingTime / 1000d ) ) );
			logger.setStatus( "" );
		}

		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public Model getResult()
	{
		return model;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

}
