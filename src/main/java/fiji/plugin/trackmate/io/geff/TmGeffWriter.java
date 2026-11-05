package fiji.plugin.trackmate.io.geff;

import static fiji.plugin.trackmate.Spot.FRAME;
import static fiji.plugin.trackmate.Spot.POSITION_X;
import static fiji.plugin.trackmate.Spot.POSITION_Y;
import static fiji.plugin.trackmate.Spot.POSITION_Z;
import static fiji.plugin.trackmate.Spot.RADIUS;
import static fiji.plugin.trackmate.SpotCollection.VISIBILITY;
import static fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer.SPOT_SOURCE_ID;
import static fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer.SPOT_TARGET_ID;
import static fiji.plugin.trackmate.features.edges.EdgeTimeLocationAnalyzer.Z_LOCATION;
import static fiji.plugin.trackmate.features.track.TrackDurationAnalyzer.TRACK_START;
import static fiji.plugin.trackmate.features.track.TrackIndexAnalyzer.TRACK_ID;
import static fiji.plugin.trackmate.features.track.TrackMotilityAnalyzer.TRACK_MAX_DISTANCE_TRAVELED;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_STATE_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.LOG_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.PLUGIN_VERSION_ATTRIBUTE_NAME;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.blosc.BloscCompression;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.litt.geff.GeffAxis;
import org.litt.geff.GeffDisplayHints;
import org.litt.geff.GeffMetadata;
import org.litt.geff.MeshMetadata;
import org.litt.geff.PropMetadata;
import org.litt.geff.RelatedObjects;
import org.litt.geff.io.GeffPropertyWriter;
import org.litt.geff.props.ElementType;
import org.litt.geff.props.FunctionTypes.ToLongArrayFunction;
import org.litt.geff.props.FunctionTypes.ToMaybeDoubleFunction;
import org.litt.geff.props.FunctionTypes.ToMaybeIntFunction;
import org.litt.geff.props.FunctionTypes.ToMaybeStringFunction;
import org.litt.geff.props.Maybe;

import com.google.gson.JsonElement;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.features.track.TrackLocationAnalyzer;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.io.json.FeatureModelIO;
import fiji.plugin.trackmate.io.json.SettingsIO;
import fiji.plugin.trackmate.util.TMUtils;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * Let's try to write a GEFF writer with Tobias' new writer and reader APIs.
 */
public class TmGeffWriter
{

	// Because we specify meshes. To check later.
	private static final String GEFF_VERSION = "1.3.1";

	static final String TRACKMATE_SPOT_ID_PROP = "trackmate_spot_id";

	/** The GEFF conventional name for the radius property. */
	static final String RADIUS_PROP_NAME = "radius";

	static final String NAME_PROP = "name";

	static final String POLYGON_PROP = "polygon";

	static final String MESH_V_PROP = "mesh_vertices";

	static final String MESH_VN_PROP = "mesh_vertex_normals";

	static final String MESH_F_PROP = "mesh_triangles";

	/** Name of the sub-GEFF file that holds the track nodes (geffception). */
	static final String TRACK_GEFF_NAME = "tracks.geff";

	/**
	 * Features not in the general props, because they are the core node
	 * properties (written as {@code t}, {@code x}, {@code y}, {@code z} and the
	 * {@code sphere} shape), or because they are written as a dedicated node
	 * property mapped to the GEFF {@code lineage} track node prop
	 * ({@link TrackIndexAnalyzer#TRACK_ID}).
	 */
	private static final Set< String > SKIP_PROPS = Set.of( FRAME, POSITION_X, POSITION_Y, POSITION_Z, RADIUS, TRACK_ID );

	private final String geffPath;

	private final GeffMetadata metadata;

	private final Map< String, Object > trackmateInfo;

	/** The spot writer configured by {@link #appendModel(Model)}. */
	private GeffPropertyWriter< Spot > spotWriter;

	/**
	 * The edge writer configured by {@link #appendModel(Model)}, or
	 * {@code null} if the model has no edges.
	 */
	private GeffPropertyWriter< DefaultWeightedEdge > edgeWriter;

	/**
	 * The writer of the tracks sub-GEFF file configured by {@link
	 * #appendModel(Model)}, or {@code null} if the model has no tracks.
	 */
	private GeffPropertyWriter< Integer > trackWriter;

	/** The metadata of the tracks sub-GEFF file, or {@code null}. */
	private GeffMetadata trackMetadata;

	/** The chunk size to use when writing the GEFF file. */
	private int chunkSize = 8 * 1024; // 8 kiB;

	/** The compression to use for writing the Zarr. */
	private Compression compression = new BloscCompression();

	/**
	 * Creates a new writer that will write a GEFF file to the specified path (a
	 * Zarr container).
	 *
	 * @param zarrPath
	 *            the path to the GEFF file (a Zarr container).
	 */
	public TmGeffWriter( final String zarrPath )
	{
		this.geffPath = zarrPath;
		this.metadata = new GeffMetadata( GEFF_VERSION, true );
		this.trackmateInfo = new HashMap<>();
		metadata.setExtra( Map.of( "trackmate", trackmateInfo ) );
		trackmateInfo.put( PLUGIN_VERSION_ATTRIBUTE_NAME, TrackMate.PLUGIN_NAME_VERSION );
	}

	/**
	 * The actual writing to disk is done by {@link #write()}.
	 *
	 * @param model
	 *            the model whose {@link SpotCollection} to serialize.
	 */
	public void appendModel( final Model model )
	{
		final boolean is2D = DetectionUtils.is2D( model );

		final SpotsReport spotsReport = appendSpots( model, is2D );
		final EdgesReport edgesReport = appendEdges( model, is2D, spotsReport.spotToId() );
		final TracksReport tracksReport = appendTracks( model, is2D );

		/*
		 * Metadata: only property datasets are written by the
		 * GeffPropertyWriter; we have to build the GEFF metadata ourselves.
		 */
		buildMetadata( model, is2D, spotsReport, edgesReport, tracksReport );

		// Feature declarations, as in TmGeffWriter.
		trackmateInfo.put( "featureDeclarations", FeatureModelIO.toJsonTree( model.getFeatureModel() ) );
	}

	/**
	 * Registers the node properties of the spots on a new
	 * {@link GeffPropertyWriter}, and stores it in {@link #spotWriter}.
	 *
	 * @param model
	 *            the model whose {@link SpotCollection} to serialize.
	 * @param is2D
	 *            whether the model is 2D (no {@code z} property).
	 * @return a report with the things needed to build the main GEFF metadata:
	 *         the spot {@code id -> GEFF id} map (needed to write the edges),
	 *         the features with an actual value in at least one spot, and
	 *         whether shapes are present in the collection.
	 */
	private SpotsReport appendSpots( final Model model, final boolean is2D )
	{
		final SpotCollection spots = model.getSpots();
		final FeatureModel fm = model.getFeatureModel();
		final TrackModel trackModel = model.getTrackModel();
		final boolean hasTracks = !trackModel.trackIDs( false ).isEmpty();

		/*
		 * Collect spots in a stable order, and build the map Spot -> GEFF id.
		 * We will probably need it to write the tracks.
		 */
		final List< Spot > spotList = new ArrayList<>();
		spots.iterable( false ).forEach( spotList::add );
		final TObjectIntMap< Spot > spotToId = new TObjectIntHashMap<>();
		for ( int i = 0; i < spotList.size(); i++ )
			spotToId.put( spotList.get( i ), i );

		/*
		 * Detect the spot shapes present in the collection, and the features
		 * that have an actual value in at least one spot.
		 */
		boolean hasRoi = false;
		boolean hasMesh = false;
		final Set< String > spotFeatureKeys = new LinkedHashSet<>();
		for ( final Spot spot : spotList )
		{
			hasRoi |= spot instanceof SpotRoi;
			hasMesh |= spot instanceof SpotMesh;
			spotFeatureKeys.addAll( spot.getFeatures().keySet() );
		}

		// Initialize the property writer for the spots.
		final GeffPropertyWriter< Spot > writer = new GeffPropertyWriter<>( spotList, ElementType.NODE );

		// Now we create an accessor for each property we want to write.
		// Ids: sequential, in iteration order.
		final ToLongFunction< Spot > idAccessor = spot -> spotToId.get( spot );
		writer.id( idAccessor, "<u8" );

		// Time and position. Space and time properties use the GEFF
		// conventional names 't', 'x', 'y', 'z'
		writer.add( "t", TmGeffWriter::t );
		writer.add( "x", TmGeffWriter::x );
		writer.add( "y", TmGeffWriter::y );
		if ( !is2D )
			writer.add( "z", TmGeffWriter::z );

		// Radius, possibly missing.
		writer.add( RADIUS_PROP_NAME, TmGeffWriter::radius );

		// TrackMate ID and name.
		writer.add( TRACKMATE_SPOT_ID_PROP, Spot::ID );
		writer.add( NAME_PROP, TmGeffWriter::name );

		// Track ID, mapped to the GEFF 'lineage' track node prop in the
		// metadata. Possibly missing, for spots that are not part of a track.
		// Only registered when the model has tracks; otherwise the property
		// would consist of only missing values.
		if ( hasTracks )
		{
			final ToMaybeIntFunction< Spot > trackIdAccessor = spot -> trackId( spot, trackModel );
			writer.add( TRACK_ID, trackIdAccessor );
		}

		// Other spot features, possibly missing per spot. These accessors are
		// lambdas capturing the feature key (method references are not
		// possible for a dynamic key), and must be explicitly typed, as raw
		// lambdas are ambiguous against the many add() overloads.
		for ( final String feature : spotFeatureKeys )
		{
			if ( SKIP_PROPS.contains( feature ) )
				continue;
			final boolean isInt = fm.getSpotFeatureIsInt().getOrDefault( feature, Boolean.FALSE ).booleanValue();
			if ( isInt )
			{
				final ToMaybeIntFunction< Spot > accessor = spot -> featureAsInt( spot, feature );
				writer.add( feature, accessor );
			}
			else
			{
				final ToMaybeDoubleFunction< Spot > accessor = spot -> featureAsDouble( spot, feature );
				writer.add( feature, accessor );
			}
		}

		// SpotRoi: polygon vertices, relative to the spot center,
		// interleaved XY as mandated by the GEFF spec.
		if ( hasRoi )
			writer.add( POLYGON_PROP, TmGeffWriter::polygon );

		// SpotMesh: vertices positions and normals, triangle indices.
		if ( hasMesh )
		{
			writer.add( MESH_V_PROP, TmGeffWriter::meshVertices );
			writer.add( MESH_VN_PROP, TmGeffWriter::meshNormals );
			writer.add( MESH_F_PROP, TmGeffWriter::meshTriangles );
		}

		// Store the writer for later use in write().
		this.spotWriter = writer;

		return new SpotsReport( spotToId, spotFeatureKeys, hasRoi, hasMesh );
	}

	/**
	 * Registers the edge properties of the tracks on a new
	 * {@link GeffPropertyWriter}, and stores it in {@link #edgeWriter}. We
	 * write the edges of all tracks as one flat set, regardless of track
	 * membership: track membership is conveyed by the TRACK_ID lineage node
	 * property written by {@link #appendSpots(Model, boolean)}. If the model
	 * has no edges, we do not write the edges part at all.
	 *
	 * @param model
	 *            the model whose edges to serialize.
	 * @param is2D
	 *            whether the model is 2D (no edge {@code Z_LOCATION} feature).
	 * @param spotToId
	 *            the map from spots to their GEFF id, as written by
	 *            {@link #appendSpots(Model, boolean)}.
	 * @return a report with the things needed to build the main GEFF metadata:
	 *         the edge features with an actual value in at least one edge.
	 */
	private EdgesReport appendEdges( final Model model, final boolean is2D, final TObjectIntMap< Spot > spotToId )
	{
		final FeatureModel fm = model.getFeatureModel();
		final TrackModel trackModel = model.getTrackModel();

		if ( trackModel.edgeSet().isEmpty() )
		{
			this.edgeWriter = null;
			return new EdgesReport( Set.of() );
		}

		final List< DefaultWeightedEdge > edgeList = new ArrayList<>( trackModel.edgeSet() );

		/*
		 * Determine the edge features that have an actual value in at least
		 * one edge. We skip the spot source and target IDs, which are
		 * structural (they are the edge ids themselves), and the edge Z
		 * location in 2D.
		 */
		final Set< String > edgeFeatureKeys = new LinkedHashSet<>();
		for ( final DefaultWeightedEdge edge : edgeList )
			for ( final String feature : fm.getEdgeFeatures() )
				if ( fm.getEdgeFeature( edge, feature ) != null )
					edgeFeatureKeys.add( feature );
		edgeFeatureKeys.remove( SPOT_SOURCE_ID );
		edgeFeatureKeys.remove( SPOT_TARGET_ID );
		if ( is2D )
			edgeFeatureKeys.remove( Z_LOCATION );

		final GeffPropertyWriter< DefaultWeightedEdge > edgeWriter = new GeffPropertyWriter<>( edgeList, ElementType.EDGE );

		/*
		 * Edge ids: the pair of node ids, source first. The source is
		 * always the spot that is earlier in time, as in TmXmlWriter.
		 */
		final ToLongArrayFunction< DefaultWeightedEdge > edgeIdAccessor = edge -> {
			final Spot source = trackModel.getEdgeSource( edge );
			final Spot target = trackModel.getEdgeTarget( edge );
			final boolean swap = source.getFeature( FRAME ).doubleValue() > target.getFeature( FRAME ).doubleValue();
			return new long[] {
					spotToId.get( swap ? target : source ),
					spotToId.get( swap ? source : target ) };
		};
		edgeWriter.id( edgeIdAccessor, 2, "<u8" );

		// Edge features, possibly missing per edge. Same lambda pattern
		// as for the spot features.
		for ( final String feature : edgeFeatureKeys )
		{
			final boolean isInt = fm.getEdgeFeatureIsInt().getOrDefault( feature, Boolean.FALSE ).booleanValue();
			if ( isInt )
			{
				final ToMaybeIntFunction< DefaultWeightedEdge > accessor = edge -> edgeFeatureAsInt( edge, feature, fm );
				edgeWriter.add( feature, accessor );
			}
			else
			{
				final ToMaybeDoubleFunction< DefaultWeightedEdge > accessor = edge -> edgeFeatureAsDouble( edge, feature, fm );
				edgeWriter.add( feature, accessor );
			}
		}

		// Store the writer for later use in write().
		this.edgeWriter = edgeWriter;
		return new EdgesReport( edgeFeatureKeys );
	}

	/**
	 * Registers the properties of the tracks on a new
	 * {@link GeffPropertyWriter}, and stores it in {@link #trackWriter}, with
	 * its metadata in {@link #trackMetadata}. We write the track features to a
	 * sub-GEFF file (geffception), with one node per track: its start time,
	 * location, excursion radius, visibility and name, plus all the other
	 * track features. The node ids are the TrackMate track ids. If the model
	 * has no tracks, we do not write the sub-file at all.
	 *
	 * @param model
	 *            the model whose tracks to serialize.
	 * @param is2D
	 *            whether the model is 2D (no {@code z} property).
	 * @return a report with the things needed to build the main GEFF metadata:
	 *         whether the model has tracks, and the track features with an
	 *         actual value in at least one track.
	 */
	private TracksReport appendTracks( final Model model, final boolean is2D )
	{
		final FeatureModel fm = model.getFeatureModel();
		final TrackModel trackModel = model.getTrackModel();
		final List< Integer > trackIDList = new ArrayList<>( trackModel.trackIDs( false ) );

		if ( trackIDList.isEmpty() )
		{
			this.trackWriter = null;
			this.trackMetadata = null;
			return new TracksReport( false, Set.of(), false );
		}

		/*
		 * Determine the track features that have an actual value in at
		 * least one track. We skip the ones that are written as core
		 * properties (t, x, y, z), and the one that is repurposed as the
		 * excursion radius of the track node.
		 */
		final Set< String > trackFeatureKeys = new LinkedHashSet<>();
		for ( final Integer trackID : trackIDList )
			for ( final String feature : fm.getTrackFeatures() )
				if ( fm.getTrackFeature( trackID, feature ) != null )
					trackFeatureKeys.add( feature );
		trackFeatureKeys.remove( TRACK_ID );
		trackFeatureKeys.remove( TRACK_START );
		trackFeatureKeys.remove( TrackLocationAnalyzer.X_LOCATION );
		trackFeatureKeys.remove( TrackLocationAnalyzer.Y_LOCATION );
		trackFeatureKeys.remove( TrackLocationAnalyzer.Z_LOCATION );
		// Repurposed as the radius of the track node, so that we can
		// declare the sphere shape.
		trackFeatureKeys.remove( TRACK_MAX_DISTANCE_TRAVELED );
		final boolean hasTrackRadius = fm.getTrackFeatures().contains( TRACK_MAX_DISTANCE_TRAVELED );

		final GeffPropertyWriter< Integer > trackWriter = new GeffPropertyWriter<>( trackIDList, ElementType.NODE );

		// Ids: the track ids themselves.
		final ToLongFunction< Integer > trackIdAccessor = Integer::longValue;
		trackWriter.id( trackIdAccessor, "<u8" );

		// Core properties: time (track start) and position (track
		// location), with the GEFF conventional names.
		final ToIntFunction< Integer > tAccessor = id -> ( int ) fm.getTrackFeature( id, TRACK_START ).doubleValue();
		trackWriter.add( "t", tAccessor );
		final ToDoubleFunction< Integer > xAccessor = id -> fm.getTrackFeature( id, TrackLocationAnalyzer.X_LOCATION ).doubleValue();
		trackWriter.add( "x", xAccessor );
		final ToDoubleFunction< Integer > yAccessor = id -> fm.getTrackFeature( id, TrackLocationAnalyzer.Y_LOCATION ).doubleValue();
		trackWriter.add( "y", yAccessor );
		if ( !is2D )
		{
			final ToDoubleFunction< Integer > zAccessor = id -> fm.getTrackFeature( id, TrackLocationAnalyzer.Z_LOCATION ).doubleValue();
			trackWriter.add( "z", zAccessor );
		}

		// Excursion radius: half of the max distance traveled, as in
		// TmGeffWriter. Possibly missing.
		if ( hasTrackRadius )
		{
			final ToMaybeDoubleFunction< Integer > radiusAccessor = id -> {
				final Double r = fm.getTrackFeature( id, TRACK_MAX_DISTANCE_TRAVELED );
				return ( r == null ) ? new Maybe.MaybeDouble() : new Maybe.MaybeDouble( r.doubleValue() / 2 );
			};
			trackWriter.add( RADIUS_PROP_NAME, radiusAccessor );
		}

		// Visibility and name.
		final ToIntFunction< Integer > visibilityAccessor = id -> trackModel.isVisible( id ) ? 1 : 0;
		trackWriter.add( VISIBILITY, visibilityAccessor );
		final ToMaybeStringFunction< Integer > trackNameAccessor = id -> {
			final String n = trackModel.name( id );
			return ( n == null ) ? new Maybe.MaybeString() : new Maybe.MaybeString( n );
		};
		trackWriter.add( NAME_PROP, trackNameAccessor );

		// Other track features, possibly missing per track. Same lambda
		// pattern as for the spot and edge features.
		for ( final String feature : trackFeatureKeys )
		{
			final boolean isInt = fm.getTrackFeatureIsInt().getOrDefault( feature, Boolean.FALSE ).booleanValue();
			if ( isInt )
			{
				final ToMaybeIntFunction< Integer > accessor = id -> trackFeatureAsInt( id, feature, fm );
				trackWriter.add( feature, accessor );
			}
			else
			{
				final ToMaybeDoubleFunction< Integer > accessor = id -> trackFeatureAsDouble( id, feature, fm );
				trackWriter.add( feature, accessor );
			}
		}

		// Store the writer for later use in write().
		this.trackWriter = trackWriter;
		this.trackMetadata = buildTrackMetadata( model, is2D, trackFeatureKeys, hasTrackRadius );
		return new TracksReport( true, trackFeatureKeys, hasTrackRadius );
	}

	/** The things returned by {@link #appendSpots(Model, boolean)}. */
	private record SpotsReport(
			TObjectIntMap< Spot > spotToId,
			Set< String > featureKeys,
			boolean hasRoi,
			boolean hasMesh )
	{}

	/** The things returned by {@link #appendEdges(Model, boolean, TObjectIntMap)}. */
	private record EdgesReport( Set< String > featureKeys )
	{}

	/** The things returned by {@link #appendTracks(Model, boolean)}. */
	private record TracksReport(
			boolean hasTracks,
			Set< String > featureKeys,
			boolean hasTrackRadius )
	{}

	public void appendSettings( final Settings settings )
	{
		// All settings
		final JsonElement settingsJson = SettingsIO.toJsonTree( settings );
		trackmateInfo.put( "settings", settingsJson );

		// Path to the image
		String imagePathStr = TMUtils.getImagePathWithoutExtension( settings );
		if ( settings.imageFileName != null && !settings.imageFileName.isEmpty() )
			imagePathStr = imagePathStr + "." + settings.imageFileName.substring( settings.imageFileName.lastIndexOf( '.' ) + 1 );

		// Make it relative to the save path
		final Path imagePath = Paths.get( imagePathStr ).toAbsolutePath().normalize();
		final Path savePath = Paths.get( geffPath ).toAbsolutePath().normalize();
		metadata.setRelatedObjects( new RelatedObjects().image( savePath.getParent().relativize( imagePath ).toString() ) );
	}

	public void appendLog( final String log )
	{
		trackmateInfo.put( LOG_ELEMENT_KEY, log );
	}

	public void appendDisplaySettings( final DisplaySettings ds )
	{
		final JsonElement json = DisplaySettingsIO.toJsonTree( ds );
		trackmateInfo.put( "displaySettings", json );
	}

	public void appendGUIState( final String currentPanelIdentifier )
	{
		trackmateInfo.put( GUI_STATE_ELEMENT_KEY, currentPanelIdentifier );
	}

	/**
	 * Writes the model to the GEFF file at the path specified in the
	 * constructor.
	 *
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	public void write() throws IOException
	{
		// TODO: log?
		try (final N5Writer n5 = new N5ZarrWriter( geffPath ))
		{
			if ( spotWriter != null )
				spotWriter.write( n5, compression, chunkSize );
			if ( edgeWriter != null )
				edgeWriter.write( n5, compression, chunkSize );
		}

		// Metadata.
		GeffMetadata.writeToZarr( metadata, geffPath );

		// Track sub-GEFF file (geffception), one node per track.
		if ( trackWriter != null )
		{
			final String trackGeffPath = Paths.get( geffPath, TRACK_GEFF_NAME ).toString();
			try (final N5Writer trackN5 = new N5ZarrWriter( trackGeffPath ))
			{
				trackWriter.write( trackN5, compression, chunkSize );
			}
			GeffMetadata.writeToZarr( trackMetadata, trackGeffPath );
		}
	}

	public int getChunkSize()
	{
		return chunkSize;
	}

	public void setChunkSize( final int chunkSize )
	{
		this.chunkSize = chunkSize;
	}

	/**
	 * Sets the compression to use to write the GEFF file.
	 *
	 * @param compression
	 *            the compression to use.
	 */
	public void setCompression( final Compression compression )
	{
		this.compression = compression;
	}

	public Compression getCompression()
	{
		return compression;
	}

	private void buildMetadata(
			final Model model,
			final boolean is2D,
			final SpotsReport spotsReport,
			final EdgesReport edgesReport,
			final TracksReport tracksReport )
	{
		final FeatureModel fm = model.getFeatureModel();
		final String spaceUnits = model.getSpaceUnits();
		final String timeUnits = model.getTimeUnits();
		final Set< String > spotFeatureKeys = spotsReport.featureKeys();
		final boolean hasRoi = spotsReport.hasRoi();
		final boolean hasMesh = spotsReport.hasMesh();
		final boolean hasTracks = tracksReport.hasTracks();

		// Axes. Space and time axes use the GEFF conventional names
		// 'x', 'y', 'z' and 't' (not the TrackMate feature keys).
		final List< GeffAxis > axes = new ArrayList<>();
		final double[] minMaxT = FeatureUtils.autoMinMax( model, TrackMateObject.SPOTS, FRAME, false );
		axes.add( GeffAxis.createTimeAxis( "t", timeUnits, minMaxT[ 0 ], minMaxT[ 1 ] ) );
		final String[] spaceFeatures = is2D
				? new String[] { POSITION_X, POSITION_Y }
				: new String[] { POSITION_X, POSITION_Y, POSITION_Z };
		for ( int i = 0; i < spaceFeatures.length; i++ )
		{
			final double[] minMax = FeatureUtils.autoMinMax( model, TrackMateObject.SPOTS, spaceFeatures[ i ], false );
			axes.add( GeffAxis.createSpaceAxis( "xyz".substring( i, i + 1 ), spaceUnits, minMax[ 0 ], minMax[ 1 ] ) );
		}
		metadata.setGeffAxes( axes );

		// Display hints.
		final GeffDisplayHints displayHints = new GeffDisplayHints();
		displayHints.setDisplayHorizontal( "x" );
		displayHints.setDisplayVertical( "y" );
		displayHints.setDisplayTime( "t" );
		if ( !is2D )
			displayHints.setDisplayDepth( "z" );
		metadata.setDisplayHints( displayHints );

		// Node properties metadata.
		final Map< String, Boolean > isInt = fm.getSpotFeatureIsInt();
		final Map< String, PropMetadata > nodePropsMetadata = new LinkedHashMap<>();

		nodePropsMetadata.put( "t", new PropMetadata( "t", "uint32", false, timeUnits, Spot.FEATURE_NAMES.get( FRAME ), null ) );
		nodePropsMetadata.put( "x", new PropMetadata( "x", "float64", false, spaceUnits, Spot.FEATURE_NAMES.get( POSITION_X ), null ) );
		nodePropsMetadata.put( "y", new PropMetadata( "y", "float64", false, spaceUnits, Spot.FEATURE_NAMES.get( POSITION_Y ), null ) );
		if ( !is2D )
			nodePropsMetadata.put( "z", new PropMetadata( "z", "float64", false, spaceUnits, Spot.FEATURE_NAMES.get( POSITION_Z ), null ) );
		nodePropsMetadata.put( RADIUS_PROP_NAME, new PropMetadata( RADIUS_PROP_NAME, "float64", false, spaceUnits, Spot.FEATURE_NAMES.get( RADIUS ), "The radius of the spot" ) );
		nodePropsMetadata.put( TRACKMATE_SPOT_ID_PROP, new PropMetadata( TRACKMATE_SPOT_ID_PROP, "uint32", false, null, "TrackMate ID", "The TrackMate internal ID of spots" ) );
		nodePropsMetadata.put( NAME_PROP, new PropMetadata( NAME_PROP, "uint8", true, null, "Spot name", "The name of the spot" ) );

		// Track ID, mapped to the GEFF 'lineage' track node prop. Only when
		// the model has tracks (the property would consist of only missing
		// values otherwise).
		if ( hasTracks )
		{
			nodePropsMetadata.put( TRACK_ID, new PropMetadata( TRACK_ID, "uint32", false, null, "Track ID", "The TrackMate track ID of the spot" ) );
			metadata.setTrackNodeProps( Map.of( "lineage", TRACK_ID ) );
		}

		for ( final String feature : spotFeatureKeys )
		{
			if ( SKIP_PROPS.contains( feature ) )
				continue;
			final String dType = isInt.getOrDefault( feature, Boolean.FALSE ).booleanValue() ? "uint32" : "float64";
			final Dimension dimension = fm.getSpotFeatureDimensions().get( feature );
			final String unit = ( dimension == null ) ? null : dimension.units( spaceUnits, timeUnits );
			final String name = fm.getSpotFeatureNames().get( feature );
			nodePropsMetadata.put( feature, new PropMetadata( feature, dType, false, unit, name, null ) );
		}

		if ( hasRoi )
			nodePropsMetadata.put( POLYGON_PROP, new PropMetadata( POLYGON_PROP, "float64", true, spaceUnits, "Polygon", "Interleaved XY coordinates of the polygon vertices, relative to the spot center" ) );

		if ( hasMesh )
		{
			nodePropsMetadata.put( MESH_V_PROP, new PropMetadata( MESH_V_PROP, "float32", true, spaceUnits, "Mesh vertices", "Interleaved XYZ coordinates of the mesh vertices, relative to the spot center" ) );
			nodePropsMetadata.put( MESH_VN_PROP, new PropMetadata( MESH_VN_PROP, "float32", true, null, "Mesh vertex normals", "Interleaved XYZ coordinates of the mesh vertex normals" ) );
			nodePropsMetadata.put( MESH_F_PROP, new PropMetadata( MESH_F_PROP, "uint64", true, null, "Mesh triangles", "Flattened indices of the mesh triangles" ) );
		}

		metadata.setNodePropsMetadata( nodePropsMetadata );

		// Edge properties metadata.
		final Map< String, PropMetadata > edgePropsMetadata = new LinkedHashMap<>();
		for ( final String feature : edgesReport.featureKeys() )
		{
			final String dType = fm.getEdgeFeatureIsInt().getOrDefault( feature, Boolean.FALSE ).booleanValue() ? "uint32" : "float64";
			final Dimension dimension = fm.getEdgeFeatureDimensions().get( feature );
			final String unit = ( dimension == null ) ? null : dimension.units( spaceUnits, timeUnits );
			final String name = fm.getEdgeFeatureNames().get( feature );
			edgePropsMetadata.put( feature, new PropMetadata( feature, dType, false, unit, name, null ) );
		}
		metadata.setEdgePropsMetadata( edgePropsMetadata );

		// Shape declarations: declare which node property holds the shapes,
		// as mandated by the GEFF specification.
		metadata.setSphere( RADIUS_PROP_NAME );
		if ( hasMesh )
			metadata.setMesh( new MeshMetadata( MESH_V_PROP, MESH_F_PROP, MESH_VN_PROP, null ) );
	}

	/**
	 * Builds the metadata of the tracks sub-GEFF file. It has one node per
	 * track, with the core properties {@code t} (the track start), {@code x},
	 * {@code y}, and possibly {@code z} (the track location), the {@code
	 * radius} property derived from the excursion of the track, the
	 * {@code name} property, the {@code VISIBILITY} property, and the other
	 * track features. There are no edges in this file.
	 *
	 * @param model
	 *            the model the properties come from.
	 * @param is2D
	 *            whether the model is 2D (no {@code z} axis).
	 * @param trackFeatureKeys
	 *            the track features present somewhere in the model, that are
	 *            not written as core properties.
	 * @param hasTrackRadius
	 *            whether the {@code TRACK_MAX_DISTANCE_TRAVELED} feature is
	 *            present (the {@code radius} property is written).
	 * @return the {@link GeffMetadata} to write to the tracks sub-GEFF file.
	 */
	private GeffMetadata buildTrackMetadata(
			final Model model,
			final boolean is2D,
			final Set< String > trackFeatureKeys,
			final boolean hasTrackRadius )
	{
		final FeatureModel fm = model.getFeatureModel();
		final String spaceUnits = model.getSpaceUnits();
		final String timeUnits = model.getTimeUnits();

		final GeffMetadata trackMetadata = new GeffMetadata( GEFF_VERSION, true );

		// Axes. Same as for the spots, but over the track features.
		final List< GeffAxis > axes = new ArrayList<>();
		final double[] minMaxT = FeatureUtils.autoMinMax( model, TrackMateObject.TRACKS, TRACK_START, false );
		axes.add( GeffAxis.createTimeAxis( "t", timeUnits, minMaxT[ 0 ], minMaxT[ 1 ] ) );
		final String[] spaceFeatures = is2D
				? new String[] { TrackLocationAnalyzer.X_LOCATION, TrackLocationAnalyzer.Y_LOCATION }
				: new String[] { TrackLocationAnalyzer.X_LOCATION, TrackLocationAnalyzer.Y_LOCATION, TrackLocationAnalyzer.Z_LOCATION };
		for ( int i = 0; i < spaceFeatures.length; i++ )
		{
			final double[] minMax = FeatureUtils.autoMinMax( model, TrackMateObject.TRACKS, spaceFeatures[ i ], false );
			axes.add( GeffAxis.createSpaceAxis( "xyz".substring( i, i + 1 ), spaceUnits, minMax[ 0 ], minMax[ 1 ] ) );
		}
		trackMetadata.setGeffAxes( axes );

		// Display hints.
		final GeffDisplayHints displayHints = new GeffDisplayHints();
		displayHints.setDisplayHorizontal( "x" );
		displayHints.setDisplayVertical( "y" );
		displayHints.setDisplayTime( "t" );
		if ( !is2D )
			displayHints.setDisplayDepth( "z" );
		trackMetadata.setDisplayHints( displayHints );

		// Node properties metadata.
		final Map< String, Boolean > isInt = fm.getTrackFeatureIsInt();
		final Map< String, PropMetadata > nodePropsMetadata = new LinkedHashMap<>();
		nodePropsMetadata.put( "t", new PropMetadata( "t", "uint32", false, timeUnits, fm.getTrackFeatureNames().get( TRACK_START ), null ) );
		nodePropsMetadata.put( "x", new PropMetadata( "x", "float64", false, spaceUnits, fm.getTrackFeatureNames().get( TrackLocationAnalyzer.X_LOCATION ), null ) );
		nodePropsMetadata.put( "y", new PropMetadata( "y", "float64", false, spaceUnits, fm.getTrackFeatureNames().get( TrackLocationAnalyzer.Y_LOCATION ), null ) );
		if ( !is2D )
			nodePropsMetadata.put( "z", new PropMetadata( "z", "float64", false, spaceUnits, fm.getTrackFeatureNames().get( TrackLocationAnalyzer.Z_LOCATION ), null ) );
		if ( hasTrackRadius )
			nodePropsMetadata.put( RADIUS_PROP_NAME, new PropMetadata( RADIUS_PROP_NAME, "float64", false, spaceUnits, "Radius", "The radius of excursion of the track" ) );
		nodePropsMetadata.put( VISIBILITY, new PropMetadata( VISIBILITY, "uint32", false, null, Spot.FEATURE_NAMES.get( VISIBILITY ), "Whether the track is visible in the display" ) );
		nodePropsMetadata.put( NAME_PROP, new PropMetadata( NAME_PROP, "uint8", true, null, "Track name", "The name of the track" ) );

		for ( final String feature : trackFeatureKeys )
		{
			final String dType = isInt.getOrDefault( feature, Boolean.FALSE ).booleanValue() ? "uint32" : "float64";
			final Dimension dimension = fm.getTrackFeatureDimensions().get( feature );
			final String unit = ( dimension == null ) ? null : dimension.units( spaceUnits, timeUnits );
			final String name = fm.getTrackFeatureNames().get( feature );
			nodePropsMetadata.put( feature, new PropMetadata( feature, dType, false, unit, name, null ) );
		}

		trackMetadata.setNodePropsMetadata( nodePropsMetadata );

		return trackMetadata;
	}

	/*
	 * Static accessors, used as method references by the writer. The ones that
	 * depend on the concrete spot class dispatch with instanceof. TODO: Move to
	 * a utility class?
	 */

	private static int t( final Spot spot )
	{
		return ( int ) spot.getFeature( FRAME ).doubleValue();
	}

	private static double x( final Spot spot )
	{
		return spot.getDoublePosition( 0 );
	}

	private static double y( final Spot spot )
	{
		return spot.getDoublePosition( 1 );
	}

	private static double z( final Spot spot )
	{
		return spot.getDoublePosition( 2 );
	}

	private static Maybe.MaybeDouble radius( final Spot spot )
	{
		final Double r = spot.getFeature( RADIUS );
		return ( r == null ) ? new Maybe.MaybeDouble() : new Maybe.MaybeDouble( r.doubleValue() );
	}

	private static Maybe.MaybeString name( final Spot spot )
	{
		return spot.hasName() ? new Maybe.MaybeString( spot.getName() ) : new Maybe.MaybeString();
	}

	/**
	 * Returns the track ID of the specified spot, or an empty {@link Maybe} if
	 * the spot is not part of a track.
	 */
	private static Maybe.MaybeInt trackId( final Spot spot, final TrackModel trackModel )
	{
		final Integer trackID = trackModel.trackIDOf( spot );
		return ( trackID == null ) ? new Maybe.MaybeInt() : new Maybe.MaybeInt( trackID.intValue() );
	}

	private static Maybe.MaybeInt featureAsInt( final Spot spot, final String feature )
	{
		final Double v = spot.getFeature( feature );
		return ( v == null ) ? new Maybe.MaybeInt() : new Maybe.MaybeInt( v.intValue() );
	}

	private static Maybe.MaybeDouble featureAsDouble( final Spot spot, final String feature )
	{
		final Double v = spot.getFeature( feature );
		return ( v == null ) ? new Maybe.MaybeDouble() : new Maybe.MaybeDouble( v.doubleValue() );
	}

	private static Maybe.MaybeInt edgeFeatureAsInt( final DefaultWeightedEdge edge, final String feature, final FeatureModel fm )
	{
		final Double v = fm.getEdgeFeature( edge, feature );
		return ( v == null ) ? new Maybe.MaybeInt() : new Maybe.MaybeInt( v.intValue() );
	}

	private static Maybe.MaybeDouble edgeFeatureAsDouble( final DefaultWeightedEdge edge, final String feature, final FeatureModel fm )
	{
		final Double v = fm.getEdgeFeature( edge, feature );
		return ( v == null ) ? new Maybe.MaybeDouble() : new Maybe.MaybeDouble( v.doubleValue() );
	}

	private static Maybe.MaybeInt trackFeatureAsInt( final Integer trackID, final String feature, final FeatureModel fm )
	{
		final Double v = fm.getTrackFeature( trackID, feature );
		return ( v == null ) ? new Maybe.MaybeInt() : new Maybe.MaybeInt( v.intValue() );
	}

	private static Maybe.MaybeDouble trackFeatureAsDouble( final Integer trackID, final String feature, final FeatureModel fm )
	{
		final Double v = fm.getTrackFeature( trackID, feature );
		return ( v == null ) ? new Maybe.MaybeDouble() : new Maybe.MaybeDouble( v.doubleValue() );
	}

	/**
	 * Returns the polygon vertices of the specified spot, relative to the spot
	 * center and interleaved as XY pairs, as mandated by the GEFF spec. Empty
	 * if the spot is not a {@link SpotRoi}.
	 */
	private static Maybe.MaybeDoubleArray polygon( final Spot spot )
	{
		if ( spot instanceof SpotRoi )
		{
			final SpotRoi roi = ( SpotRoi ) spot;
			final double[] p = new double[ 2 * roi.nPoints() ];
			for ( int i = 0; i < roi.nPoints(); i++ )
			{
				p[ 2 * i ] = roi.xr( i );
				p[ 2 * i + 1 ] = roi.yr( i );
			}
			return new Maybe.MaybeDoubleArray( p );
		}
		return new Maybe.MaybeDoubleArray();
	}

	/**
	 * Returns the interleaved XYZ coordinates of the mesh vertices of the
	 * specified spot, relative to the spot center. Empty if the spot is not a
	 * {@link SpotMesh}.
	 */
	private static Maybe.MaybeFloatArray meshVertices( final Spot spot )
	{
		if ( spot instanceof SpotMesh )
			return new Maybe.MaybeFloatArray( toArray( ( ( SpotMesh ) spot ).getMesh().vertices().verts() ) );
		return new Maybe.MaybeFloatArray();
	}

	/**
	 * Returns the interleaved XYZ coordinates of the mesh vertex normals of the
	 * specified spot. Empty if the spot is not a {@link SpotMesh}.
	 */
	private static Maybe.MaybeFloatArray meshNormals( final Spot spot )
	{
		if ( spot instanceof SpotMesh )
			return new Maybe.MaybeFloatArray( toArray( ( ( SpotMesh ) spot ).getMesh().vertices().normals() ) );
		return new Maybe.MaybeFloatArray();
	}

	/**
	 * Returns the flattened indices of the mesh triangles of the specified
	 * spot. Empty if the spot is not a {@link SpotMesh}.
	 */
	private static Maybe.MaybeLongArray meshTriangles( final Spot spot )
	{
		if ( spot instanceof SpotMesh )
			return new Maybe.MaybeLongArray( toLongArray( ( ( SpotMesh ) spot ).getMesh().triangles().indices() ) );
		return new Maybe.MaybeLongArray();
	}

	private static long[] toLongArray( final IntBuffer b )
	{
		final int n = b.limit();
		final long[] out = new long[ n ];
		for ( int i = 0; i < n; i++ )
			out[ i ] = b.get( i );
		return out;
	}

	private static float[] toArray( final FloatBuffer b )
	{
		try
		{
			return b.array();
		}
		catch ( final UnsupportedOperationException e )
		{}
		final int n = b.limit();
		final float[] out = new float[ n ];
		b.rewind();
		b.get( out );
		return out;
	}
}
