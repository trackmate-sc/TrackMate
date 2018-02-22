package fiji.plugin.trackmate.io;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.DataConversionException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Logger.StringBuilderLogger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;

/**
 * A reader for the XML files generated in the TrackManager of the Icy software
 * <a href=http://icy.bioimageanalysis.org/plugin/Track_Manager>Icy/Track
 * Manager </a>.
 * <p>
 * The reader requires a calibration s to operate, so as to generate a proper
 * model objects. Indeed, the Icy file format stores the detection coordinates
 * in pixel coordinates, while TrackMate uses image coordinates. We need a
 * proper calibration for conversion.
 *
 * @author Jean-Yves Tinevez &lt;jeanyves.tinevez@gmail.com&gt; Apr 2014
 */
public class IcyXmlReader
{
	/**
	 * The quality value ICY spots will get by default.
	 */
	private static final double DEFAULT_QUALITY = 1.0d;

	private static final double DEFAULT_RADIUS = 2d;

	private boolean ok = true;

	protected StringBuilderLogger logger = new StringBuilderLogger();

	private final Element root;

	private final double radius;

	private final double spotQuality;

	private final double edgeWeigth;

	private final double[] calibration;

	private final double dt;

	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Creates a new ICY importer from parsing the specified XML file, with
	 * default calibration, frame interval, spot radius, spot quality and edge
	 * weight values.
	 * <p>
	 * If an error triggered upon parsing the file, the flag returned by
	 * {@link #isReadingOk()} will return <code>false</code>. The content of the
	 * error message can then be obtained through {@link #getErrorMessage()}.
	 *
	 * @param file
	 *            the ICY tracks XML file. Will generate an error if the XML
	 *            file is not a proper ICY tracks XML file.
	 */
	public IcyXmlReader( final File file )
	{
		this( file, new double[] { 1d, 1d, 1d }, 1d );
	}

	/**
	 * Creates a new ICY importer from parsing the specified XML file, with
	 * default spot radius, spot quality and edge weight values.
	 * <p>
	 * If an error triggered upon parsing the file, the flag returned by
	 * {@link #isReadingOk()} will return <code>false</code>. The content of the
	 * error message can then be obtained through {@link #getErrorMessage()}.
	 *
	 * @param file
	 *            the ICY tracks XML file. Will generate an error if the XML
	 *            file is not a proper ICY tracks XML file.
	 * @param calibration
	 *            the spatial calibration as a <code>double[]</code> array.
	 *            Stores the pixel sizes <code>dx, dy, dz</code>.
	 * @param dt
	 *            the frame interval in time units.
	 */
	public IcyXmlReader( final File file, final double[] calibration, final double dt )
	{
		this( file, calibration, dt, DEFAULT_RADIUS, DEFAULT_QUALITY, DEFAULT_QUALITY );
	}

	/**
	 * Creates a new ICY importer from parsing the specified XML file.
	 * <p>
	 * If an error triggered upon parsing the file, the flag returned by
	 * {@link #isReadingOk()} will return <code>false</code>. The content of the
	 * error message can then be obtained through {@link #getErrorMessage()}.
	 *
	 * @param file
	 *            the ICY tracks XML file. Will generate an error if the XML
	 *            file is not a proper ICY tracks XML file.
	 * @param calibration
	 *            the spatial calibration as a <code>double[]</code> array. Must
	 *            store the pixel sizes <code>dx, dy, dz</code>.
	 * @param dt
	 *            the frame interval in time units.
	 * @param radius
	 *            the radius, in image coordinates, for the spot created upon
	 *            loading.
	 * @param spotQuality
	 *            the quality for spot created upon loading.
	 * @param edgeWeight
	 *            the weight for edges created upon loading.
	 */
	public IcyXmlReader( final File file, final double[] calibration, final double dt, final double radius, final double spotQuality, final double edgeWeight )
	{
		assert calibration.length == 3;
		this.calibration = calibration;
		this.dt = dt;
		this.radius = radius;
		this.spotQuality = spotQuality;
		this.edgeWeigth = edgeWeight;
		final SAXBuilder sb = new SAXBuilder();
		Element r = null;
		try
		{
			final Document document = sb.build( file );
			r = document.getRootElement();
		}
		catch ( final JDOMException e )
		{
			ok = false;
			logger.error( "Problem parsing " + file.getName() + ", it is not a valid ICY tracks XML file.\nError message is:\n" + e.getLocalizedMessage() + '\n' );
		}
		catch ( final IOException e )
		{
			logger.error( "Problem reading " + file.getName() + ".\nError message is:\n" + e.getLocalizedMessage() + '\n' );
			ok = false;
		}
		this.root = r;
		if ( !root.getName().equals( "root" ) || root.getChild( "trackfile" ) == null )
		{
			ok = false;
			logger.error( "Problem parsing " + file.getName() + ", it is not an ICY tracks XML file.\n" );
		}
	}

	/*
	 * METHODS
	 */

	/**
	 * Returns the version string stored in the file.
	 */
	public String getVersion()
	{
		return root.getChild( "trackfile" ).getAttribute( "version" ).getValue();
	}

	/**
	 * Returns an explanatory message about the last unsuccessful read attempt.
	 *
	 * @return an error message.
	 * @see #isReadingOk()
	 */
	public String getErrorMessage()
	{
		return logger.toString();
	}

	/**
	 * Returns <code>true</code> if the last reading method call happened
	 * without any warning or error, <code>false</code> otherwise.
	 *
	 * @return <code>true</code> if reading was ok.
	 * @see #getErrorMessage()
	 */
	public boolean isReadingOk()
	{
		return ok;
	}

	/**
	 * Builds and returns a TrackMate model from the ICY track file.
	 * <p>
	 * If an error triggered upon building the model, the flag returned by
	 * {@link #isReadingOk()} will return <code>false</code>. The content of the
	 * error message can then be obtained through {@link #getErrorMessage()}.
	 *
	 * @return a new model.
	 */
	public Model getModel()
	{
		final Model model = new Model();
		final SpotCollection spotCollection = model.getSpots();
		final double dx = calibration[ 0 ];
		final double dy = calibration[ 1 ];
		final double dz = calibration[ 2 ];

		// Holders for track model reconstruction
		final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph = new SimpleWeightedGraph< >( DefaultWeightedEdge.class );
		final Map< Integer, Spot > firsts = new HashMap< >();
		final Map< Integer, Spot > lasts = new HashMap< >();

		try
		{
			/*
			 * First deal with the track groups.
			 */

			final List< Element > trackGroups = root.getChildren( "trackgroup" );
			for ( final Element trackGroup : trackGroups )
			{
				final List< Element > tracks = trackGroup.getChildren( "track" );

				for ( final Element track : tracks )
				{
					final int id = track.getAttribute( "id" ).getIntValue();

					Spot previous = null;
					Spot first = null;

					final List< Element > detections = track.getChildren( "detection" );
					for ( final Element detection : detections )
					{
						final double x = detection.getAttribute( "x" ).getDoubleValue() * dx;
						final double y = detection.getAttribute( "y" ).getDoubleValue() * dy;
						final double z = detection.getAttribute( "z" ).getDoubleValue() * dz;
						final int t = detection.getAttribute( "t" ).getIntValue();
						final Spot spot = new Spot( x, y, z, radius, spotQuality );
						spot.putFeature( Spot.POSITION_T, Double.valueOf( t * dt ) );

						if ( first == null )
						{
							// Store the first of the track.
							first = spot;
						}

						spotCollection.add( spot, t );
						graph.addVertex( spot );
						if ( null != previous )
						{
							final DefaultWeightedEdge edge = graph.addEdge( previous, spot );
							graph.setEdgeWeight( edge, edgeWeigth );
						}

						previous = spot;
					}

					final Integer trackKey = Integer.valueOf( id );
					firsts.put( trackKey, first );
					lasts.put( trackKey, previous );
				}
			}

			/*
			 * Then deal with the link list.
			 */
			final Element linklist = root.getChild( "linklist" );
			final List< Element > links = linklist.getChildren( "link" );
			for ( final Element link : links )
			{
				final int from = link.getAttribute( "from" ).getIntValue();
				final int to = link.getAttribute( "to" ).getIntValue();

				final Spot fromSpot = lasts.get( Integer.valueOf( from ) );
				final Spot toSpot = firsts.get( Integer.valueOf( to ) );

				final DefaultWeightedEdge edge = graph.addEdge( fromSpot, toSpot );
				if ( null == edge )
				{
					// ok = false;
					// logger.error( "Problem reading link from track " + from +
					// " to " + to + ":\nCannot create edge between spots " +
					// fromSpot + " and " + toSpot + ".\n" );
					// Apparently links can exist in duplicate. Just skip them.
				}
				else
				{
					graph.setEdgeWeight( edge, edgeWeigth );
				}
			}

			/*
			 * Finally build model.
			 */

			model.setTracks( graph, false );

		}
		catch ( final DataConversionException e )
		{
			ok = false;
			logger.error( "Error reading tracks:\n" + e.getMessage() );
		}

		return model;
	}
}
