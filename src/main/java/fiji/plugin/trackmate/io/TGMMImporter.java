package fiji.plugin.trackmate.io;

import ij.ImageJ;

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

import net.imglib2.algorithm.OutputAlgorithm;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class TGMMImporter implements OutputAlgorithm< Model >
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

	private static final String XML_SCALE = "scale";

	private static final String XML_ID = "id";

	private static final String XML_SCORE = "splitScore";

	private static final String XML_PRECISION_MATRIX = "W";

	private static final String XML_LINEAGE = "lineage";

	private static final String XML_PARENT = "parent";

	private final File file;

	private String errorMessage;

	private final Pattern framePattern;

	private Model model;

	/*
	 * CONSTRUCTORS
	 */

	public TGMMImporter( final File file, final Pattern framePattern )
	{
		this.file = file;
		this.framePattern = framePattern;
	}

	public TGMMImporter( final File file )
	{
		this( file, Pattern.compile( ".+_frame(\\d+)\\.xml" ) );
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

		model = new Model();

		/*
		 * Grab all the XML files
		 */

		final File[] xmlFiles = file.listFiles( xmlFilter );

		/*
		 * Extract frame information from filename. It is not stored elsewhere
		 * so we have to rely on a specific pattern to get it. Note that it is
		 * not robust at all.
		 */
		
		final int[] frames = new int[ xmlFiles.length ];
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
				errorMessage = BASE_ERROR_MSG + "Could not retrieve frame bumber from file " + file + ".\n" + nfe.getMessage() + "\n";
				return false;
			}
		}

		/*
		 * Read XML
		 */

		final SAXBuilder saxBuilder = new SAXBuilder();

		final SpotCollection sc = new SpotCollection();
		final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph = new SimpleWeightedGraph< Spot, DefaultWeightedEdge >( DefaultWeightedEdge.class );

		Map< Integer, Spot > previousSpotID = null;
		Map< Integer, Spot > currentSpotID;

		for ( int i = 0; i < frames.length; i++ )
		{
			final File xmlFile = xmlFiles[ i ];
			Element root;
			try
			{
				final Document doc = saxBuilder.build( xmlFile );
				root = doc.getRootElement();
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

			final List< Element > detectionEls = root.getChildren( XML_DETECTION_NAME );
			final Collection< Spot > spots = new ArrayList< Spot >( detectionEls.size() );

			/*
			 * Parse all detections
			 */

			currentSpotID = new HashMap< Integer, Spot >( detectionEls.size() );

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

				final String scaleStr = detectionEl.getAttributeValue( XML_SCALE );
				if ( null == scaleStr )
				{
					errorMessage = BASE_ERROR_MSG + "Element " + detectionEl + " in file " + xmlFile + " misses the scale attribute (" + XML_SCALE + ").\n";
					return false;
				}
				final String[] scaleStrs = scaleStr.split( " " );
				
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

					final double ix = Double.parseDouble( pixelPosStrs[ 0 ] );
					final double iy = Double.parseDouble( pixelPosStrs[ 1 ] );
					final double iz = Double.parseDouble( pixelPosStrs[ 2 ] );

					final double sx = Double.parseDouble( scaleStrs[ 0 ] );
					final double sy = Double.parseDouble( scaleStrs[ 1 ] );
					final double sz = Double.parseDouble( scaleStrs[ 2 ] );
					final double[] scales = new double[] { sx, sy, sz };

					final double x = ix * sx;
					final double y = iy * sy;
					final double z = iz * sz;

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

					// final double[] vals = new double[ 9 ];
					// for ( int j = 0; j < vals.length; j++ )
					// {
					// vals[ j ] = Double.parseDouble( precMatStrs[ j ] );
					// }
					// final Matrix precMat = new Matrix( vals, 3 );
					// final Matrix covMat = precMat.inverse();
					// final EigenvalueDecomposition eig = covMat.eig();
					// final double[] eigVals = eig.getRealEigenvalues();
					// final Matrix eigVectors = eig.getV();
					//
					// /*
					// * Scale shape properly
					// */
					//
					// final double[] scaledEigVals = new double[3];
					// final double[][] scaledEigVecs = new double[3][3];
					// for ( int k = 0; k < scaledEigVals.length; k++ )
					// {
					// scaledEigVals[k] = scales[k] * eigVals[k];
					// for ( int l = 0; l < 3; l++ )
					// {
					// scaledEigVecs[k][l] = scales[l] * eigVectors.get( l, k );
					// }
					// }
					
					/*
					 * Build a mean radius
					 */
					
					// FIXME
					final double radius = 5; // Util.computeAverage(
												// scaledEigVals );
					
					/*
					 * Make a spot and add it to this frame collection.
					 */
					
					final Spot spot = new Spot( x, y, z, radius, score, lineage + " (" + id + ")" );
					spots.add( spot );
					currentSpotID.put( Integer.valueOf( id ), spot );

					graph.addVertex( spot );
					if ( parent >= 0 && previousSpotID != null )
					{
						final Spot source = previousSpotID.get( Integer.valueOf( parent ) );
						if ( null == source )
						{
							System.out.println( BASE_ERROR_MSG + "The parent of the current spot (frame " + frames[ i ] + ", id = " + id + " could not be found (was expected in frame " + ( frames[ i ] - 1 ) + " with id = " + parent + ".\n" );
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

			sc.put( frames[ i ], spots );
			previousSpotID = currentSpotID;

		}

		sc.setVisible( true );

		model.setSpots( sc, false );
		model.setTracks( graph, false );
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

	/*
	 * MAIN METHOD
	 */

	public static void main( final String[] args )
	{
		final File file = new File( "/Users/tinevez/Development/Fernando/extract" );
		final TGMMImporter importer = new TGMMImporter( file );
		if ( !importer.checkInput() || !importer.process() )
		{
			System.out.println( importer.getErrorMessage() );
			return;
		}

		final Model model = importer.getResult();

		System.out.println( model.getSpots() );

		ImageJ.main( args );
		final HyperStackDisplayer view = new HyperStackDisplayer( model, new SelectionModel( model ) );
		view.render();
	}

}

