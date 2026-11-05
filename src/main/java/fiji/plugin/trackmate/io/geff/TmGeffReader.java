package fiji.plugin.trackmate.io.geff;

import static fiji.plugin.trackmate.Spot.FRAME;
import static fiji.plugin.trackmate.SpotCollection.VISIBILITY;
import static fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer.EDGE_COST;
import static fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer.SPOT_SOURCE_ID;
import static fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer.SPOT_TARGET_ID;
import static fiji.plugin.trackmate.features.edges.EdgeTimeLocationAnalyzer.Z_LOCATION;
import static fiji.plugin.trackmate.features.track.TrackDurationAnalyzer.TRACK_START;
import static fiji.plugin.trackmate.features.track.TrackIndexAnalyzer.TRACK_ID;
import static fiji.plugin.trackmate.features.track.TrackLocationAnalyzer.X_LOCATION;
import static fiji.plugin.trackmate.features.track.TrackLocationAnalyzer.Y_LOCATION;
import static fiji.plugin.trackmate.features.track.TrackMotilityAnalyzer.TRACK_MAX_DISTANCE_TRAVELED;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_STATE_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.LOG_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.geff.TmGeffWriter.MESH_F_PROP;
import static fiji.plugin.trackmate.io.geff.TmGeffWriter.MESH_VN_PROP;
import static fiji.plugin.trackmate.io.geff.TmGeffWriter.MESH_V_PROP;
import static fiji.plugin.trackmate.io.geff.TmGeffWriter.NAME_PROP;
import static fiji.plugin.trackmate.io.geff.TmGeffWriter.POLYGON_PROP;
import static fiji.plugin.trackmate.io.geff.TmGeffWriter.RADIUS_PROP_NAME;
import static fiji.plugin.trackmate.io.geff.TmGeffWriter.TRACKMATE_SPOT_ID_PROP;
import static fiji.plugin.trackmate.io.geff.TmGeffWriter.TRACK_GEFF_NAME;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.litt.geff.GeffAxis;
import org.litt.geff.GeffDisplayHints;
import org.litt.geff.GeffException;
import org.litt.geff.GeffMetadata;
import org.litt.geff.RelatedObjects;
import org.litt.geff.bind.Construction.EdgeConstructor;
import org.litt.geff.bind.Construction.FromId;
import org.litt.geff.bind.Construction.FromProperty;
import org.litt.geff.bind.Construction.NodeConstructor;
import org.litt.geff.bind.ElementCreator;
import org.litt.geff.io.GeffPropertyWriter;
import org.litt.geff.io.IoUtils;
import org.litt.geff.props.ElementType;
import org.litt.geff.props.FunctionTypes;
import org.litt.geff.props.GeffProperties;
import org.litt.geff.props.GeffProperty;
import org.litt.geff.props.Maybe;
import org.litt.geff.props.Suppliers;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.io.geff.heuristics.AxisIdentifierHeuristic;
import fiji.plugin.trackmate.io.geff.heuristics.RadiusIdentifierHeuristic;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.features.track.TrackLocationAnalyzer;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.io.json.FeatureModelIO;
import fiji.plugin.trackmate.io.json.SettingsIO;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.mesh.impl.nio.BufferMesh;
import net.imglib2.mesh.impl.nio.BufferMesh.Triangles;
import net.imglib2.mesh.impl.nio.BufferMesh.Vertices;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * Reads a GEFF file into a TrackMate with Tobias' new GEFF property binding
 * framework.
 */
public class TmGeffReader
{

	private final String geffPath;

	private GeffMetadata metadata;

	public TmGeffReader( final String geffPath )
	{
		this.geffPath = geffPath;
	}

	public String getLog()
	{
		if ( null == metadata )
			metadata = GeffMetadata.readFromZarr( geffPath );

		final Map< String, Object > trackmateMD = trackmateMetadata();
		if ( trackmateMD == null )
			return null;
		final String log = ( String ) trackmateMD.get( LOG_ELEMENT_KEY );
		return log;
	}

	public String getGUIState()
	{
		if ( null == metadata )
			metadata = GeffMetadata.readFromZarr( geffPath );

		final Map< String, Object > trackmateMD = trackmateMetadata();
		if ( trackmateMD == null )
			return null;
		final String guiState = ( String ) trackmateMD.get( GUI_STATE_ELEMENT_KEY );
		return guiState;
	}

	public DisplaySettings getDisplaySettings()
	{
		if ( null == metadata )
			metadata = GeffMetadata.readFromZarr( geffPath );

		final Map< String, Object > trackmateMD = trackmateMetadata();
		if ( trackmateMD == null )
			return DisplaySettingsIO.readUserDefault();
		final Map< String, Object > displaySettingsJson = ( Map< String, Object > ) trackmateMD.get( "displaySettings" );
		return DisplaySettingsIO.fromJsonTree( displaySettingsJson );
	}

	/**
	 * Returns the TrackMate-specific extra metadata map of this file, or
	 * {@code null} if the file does not have one — the case for foreign GEFF
	 * files not written by TrackMate.
	 */
	@SuppressWarnings( "unchecked" )
	private Map< String, Object > trackmateMetadata()
	{
		final Map< String, Object > extra = metadata.getExtra();
		if ( null == extra )
			return null;
		return ( Map< String, Object > ) extra.get( "trackmate" );
	}

	public synchronized ImagePlus readImage()
	{
		if ( null == metadata )
			metadata = GeffMetadata.readFromZarr( geffPath );

		final RelatedObjects relatedObjects = metadata.getRelatedObjects();
		if ( null == relatedObjects )
			return null;

		final List< String > imagePaths = relatedObjects.getImagePaths();
		for ( final String imagePath : imagePaths )
		{
			final File imageFile = new File( new File( geffPath ).getParent(), imagePath );
			if ( !imageFile.exists() || !imageFile.canRead() )
				continue;

			final ImagePlus imp = IJ.openImage( imageFile.getAbsolutePath() );
			if ( null == imp )
				continue;
			return imp;
		}
		return null;
	}

	@SuppressWarnings( "unchecked" )
	public synchronized Settings readSettings( final ImagePlus imp )
	{
		if ( null == metadata )
			metadata = GeffMetadata.readFromZarr( geffPath );

		final Map< String, Object > trackmateMD = trackmateMetadata();
		final Settings settings = new Settings( imp );
		if ( trackmateMD == null )
			return settings;
		final Map< String, Object > settingsJson = ( Map< String, Object > ) trackmateMD.get( "settings" );

		SettingsIO.updateFromJsonTree( settingsJson, settings );
		return settings;
	}

	/**
	 * Reads the model from the GEFF file.
	 *
	 * @return a new {@link Model} with the spots (and later: edges, tracks,
	 *         features) read from the file.
	 * @throws IOException
	 *             if an I/O error occurs.
	 * @throws GeffException
	 *             if the file is not a valid GEFF file, or if a required
	 *             property is missing from it.
	 */
	public synchronized Model getModel() throws IOException, GeffException
	{
		final Model model = new Model();

		if ( null == metadata )
			metadata = GeffMetadata.readFromZarr( geffPath );

		// Units
		final boolean is2D = readUnits( model );

		// Spots
		final TIntObjectMap< Spot > spotIdMap = new TIntObjectHashMap<>();
		final TObjectIntMap< Spot > spotTrackIDMap = new TObjectIntHashMap<>();
		readSpots( model, is2D, spotIdMap, spotTrackIDMap );

		// Edges: build the graph and the track membership maps, which are
		// derived from the spot lineage. The graph is not wired into the model
		// yet: TrackModel.from() also needs the track visibility and names,
		// which are read from the tracks sub-file afterwards.
		final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph = new SimpleWeightedGraph<>( DefaultWeightedEdge.class );
		final Map< Integer, Set< Spot > > connectedVertexSet = new HashMap<>();
		final Map< Integer, Set< DefaultWeightedEdge > > connectedEdgeSet = new HashMap<>();
		readEdges( model, is2D, spotIdMap, spotTrackIDMap, graph, connectedVertexSet, connectedEdgeSet );

		// Tracks sub-file: track visibility, names and feature values. It is
		// absent when the model has no tracks.
		final Map< Integer, Boolean > visibility = new HashMap<>();
		final Map< Integer, String > savedTrackNames = new HashMap<>();
		final boolean hasTracks = readTracks( model, visibility, savedTrackNames );

		// Single-spot tracks: their spots are not referenced by any edge, so
		// they are not in the connected vertex sets yet. Add them from the
		// spot lineage (a set, so already-present spots are not added twice).
		if ( hasTracks )
		{
			for ( final Spot spot : spotTrackIDMap.keySet() )
			{
				final int trackID = spotTrackIDMap.get( spot );
				connectedVertexSet
						.computeIfAbsent( Integer.valueOf( trackID ), k -> new HashSet<>() )
						.add( spot );
			}
		}

		// Wire the graph into the model.
		model.getTrackModel().from( graph, connectedVertexSet, connectedEdgeSet, visibility, savedTrackNames );

		// Feature declarations, from the TrackMate extra metadata.
		readFeatureDeclarations( model );

		return model;
	}

	/**
	 * Restores the feature declarations of the model from the TrackMate extra
	 * metadata, using {@link FeatureModelIO#fromJsonTree}. If the extra
	 * metadata is absent or does not contain the declarations, the feature
	 * model is left without declarations (the feature values are restored
	 * regardless).
	 *
	 * @param model
	 *            the model being built.
	 */
	@SuppressWarnings( "unchecked" )
	private void readFeatureDeclarations( final Model model )
	{
		final Map< String, Object > trackmateMD = trackmateMetadata();
		if ( trackmateMD == null )
			return;

		final Object featureDeclarations = trackmateMD.get( "featureDeclarations" );
		if ( featureDeclarations instanceof Map )
			FeatureModelIO.fromJsonTree( ( Map< String, Object > ) featureDeclarations, model.getFeatureModel() );
	}

	private boolean readUnits( final Model model )
	{
		String spaceUnits = null;
		String timeUnits = null;
		int spaceDim = 0;
		for ( final GeffAxis axis : metadata.getGeffAxes() )
		{
			if ( axis.getType().equals( GeffAxis.TYPE_SPACE ) )
			{
				spaceDim++;
				if ( spaceUnits == null )
					spaceUnits = axis.getUnit();
			}
			else if ( axis.getType().equals( GeffAxis.TYPE_TIME ) )
			{
				if ( timeUnits == null )
					timeUnits = axis.getUnit();
			}
		}
		model.setPhysicalUnits( spaceUnits, timeUnits );
		final boolean is2D = spaceDim < 3;
		return is2D;
	}

	/**
	 * Node properties consumed by the spot builders, or handled specially in
	 * the post-processing pass. All other node properties are put back as
	 * spot features as-is.
	 */
	private static final Set< String > CONSUMED_PROPS = Set.of(
			"t", "x", "y", "z",
			RADIUS_PROP_NAME,
			TRACKMATE_SPOT_ID_PROP,
			NAME_PROP,
			POLYGON_PROP,
			MESH_V_PROP, MESH_VN_PROP, MESH_F_PROP );

	/**
	 * The radius given to spots of foreign GEFF files that do not have any
	 * radius-like property we can identify.
	 */
	private static final double DEFAULT_SPOT_RADIUS = 5.0;

	/**
	 * Map the node properties of the loaded file to the TrackMate-canonical
	 * names the spot builders expect ( {@code "x"}, {@code "y"}, {@code "z"},
	 * {@code "t"}, {@link #RADIUS_PROP_NAME}, {@link #TRACKMATE_SPOT_ID_PROP}
	 * and {@link #NAME_PROP} ), so that the builders can remain written
	 * against the canonical names.
	 * <p>
	 * For the spatial and time axes, we try, in this order:
	 * <ol>
	 * <li>the display hints of the metadata, when they are set;</li>
	 * <li>a heuristic on the axis names declared in the metadata
	 * ( {@link AxisIdentifierHeuristic} for the space axes; the axis of type
	 * {@code time} for the time axis );</li>
	 * <li>the GEFF-conventional literal names, accepted as-is.</li>
	 * </ol>
	 * For the radius, we use the {@code sphere} metadata key first, then
	 * {@link RadiusIdentifierHeuristic}.
	 * <p>
	 * TrackMate-specific properties that are missing from foreign files are
	 * synthesized as follows:
	 * <ul>
	 * <li>no time axis: all spots are in frame {@code 0};</li>
	 * <li>no radius: the TrackMate default spot radius
	 * ({@value #DEFAULT_SPOT_RADIUS});</li>
	 * <li>no spot ID: the ID property is synthesized as absent, and the spots
	 * get fresh IDs from {@link SpotBase#IDcounter};</li>
	 * <li>no name: the name property is synthesized as absent, and the spots
	 * keep the TrackMate default name ( {@code ID<id>} ).</li>
	 * </ul>
	 * On the other hand, {@code x} and {@code y} (and {@code z} for 3D models)
	 * cannot be defaulted: if they cannot be identified, this is a hard error.
	 *
	 * @param props
	 *            the properties loaded from the file, modified in place.
	 * @param is2D
	 *            whether the model is 2D, as determined from the metadata
	 *            axes.
	 * @throws GeffException
	 *             if a spatial axis cannot be identified.
	 */
	private void mapCoreSpotProps( final GeffProperties props, final boolean is2D ) throws GeffException
	{
		final GeffDisplayHints hints = metadata.getDisplayHints();

		// --- Time axis. ---
		if ( !props.properties().containsKey( "t" ) )
		{
			String t = ( hints == null ) ? null : hints.getDisplayTime();
			if ( t == null || !props.properties().containsKey( t ) )
			{
				// Fall back on the axis declared with the 'time' type.
				t = null;
				if ( metadata.getGeffAxes() != null )
					for ( final GeffAxis axis : metadata.getGeffAxes() )
						if ( "time".equals( axis.getType() ) && props.properties().containsKey( axis.getName() ) )
						{
							t = axis.getName();
							break;
						}
			}
			if ( t != null )
				props.rename( t, "t" );
			else
				props.put( constantIntProperty( "t", 0, props.numElements() ) );
		}

		// --- Spatial axes. ---
		// Candidate properties: those matching axes declared with the 'space'
		// type in the metadata.
		final List< String > spaceAxes = new ArrayList<>();
		if ( metadata.getGeffAxes() != null )
			for ( final GeffAxis axis : metadata.getGeffAxes() )
				if ( "space".equals( axis.getType() ) && props.properties().containsKey( axis.getName() ) )
					spaceAxes.add( axis.getName() );
		if ( spaceAxes.size() > 3 )
			spaceAxes.subList( 3, spaceAxes.size() ).clear(); // The heuristic cannot handle more than 3 axes.
		final Map< Character, String > identified = ( spaceAxes.isEmpty() )
				? Collections.emptyMap()
				: AxisIdentifierHeuristic.identify( spaceAxes );

		if ( hints != null )
		{
			mapSpatialAxis( props, 'x', hints.getDisplayHorizontal(), identified );
			mapSpatialAxis( props, 'y', hints.getDisplayVertical(), identified );
			if ( !is2D )
				mapSpatialAxis( props, 'z', hints.getDisplayDepth(), identified );
		}
		else
		{
			mapSpatialAxis( props, 'x', null, identified );
			mapSpatialAxis( props, 'y', null, identified );
			if ( !is2D )
				mapSpatialAxis( props, 'z', null, identified );
		}

		for ( final String required : new String[] { "x", "y" } )
			if ( !props.properties().containsKey( required ) )
				throw new GeffException( "Cannot determine which node property represents the '" + required
						+ "' axis of the GEFF file." );
		if ( !is2D && !props.properties().containsKey( "z" ) )
			throw new GeffException( "Cannot determine which node property represents the 'z' axis of the 3D GEFF file." );

		// --- Radius. ---
		if ( !props.properties().containsKey( RADIUS_PROP_NAME ) )
		{
			final String sphere = metadata.getSphere();
			if ( sphere != null && props.properties().containsKey( sphere ) )
				props.rename( sphere, RADIUS_PROP_NAME );
			else
			{
				final String identifiedRadius = RadiusIdentifierHeuristic.identify( props.properties().keySet() );
				if ( identifiedRadius != null )
					props.rename( identifiedRadius, RADIUS_PROP_NAME );
				else
					props.put( constantRadiusProperty( props.numElements() ) );
			}
		}

		// --- TrackMate spot ID. ---
		// Synthesized as absent: the spots will get fresh IDs from the
		// SpotBase.IDcounter.
		if ( !props.properties().containsKey( TRACKMATE_SPOT_ID_PROP ) )
			props.put( missingIntProperty( TRACKMATE_SPOT_ID_PROP, props.numElements() ) );

		// --- Name. ---
		// Synthesized as absent: the spots will keep their default name.
		if ( !props.properties().containsKey( NAME_PROP ) )
			props.put( missingStringProperty( NAME_PROP, props.numElements() ) );
	}

	/**
	 * Map one spatial axis to its canonical name ( {@code x}, {@code y} or
	 * {@code z} ). The literal canonical name is accepted if present; then
	 * the display hint; then the heuristic result. The property is renamed
	 * in place, so nothing is done if the canonical name is already taken.
	 */
	private static void mapSpatialAxis(
			final GeffProperties props,
			final char axis,
			final String hint,
			final Map< Character, String > identified )
	{
		final String canonical = String.valueOf( axis );
		if ( props.properties().containsKey( canonical ) )
			return;
		if ( hint != null && props.properties().containsKey( hint ) )
		{
			props.rename( hint, canonical );
			return;
		}
		final String viaHeuristic = identified.get( Character.valueOf( axis ) );
		if ( viaHeuristic != null && props.properties().containsKey( viaHeuristic ) )
			props.rename( viaHeuristic, canonical );
	}

	/**
	 * Synthesize an int property with a constant value for all elements.
	 */
	private static GeffProperty< ? > constantIntProperty( final String identifier, final int value, final long numElements )
	{
		final List< Object > dummies = Collections.nCopies( ( int ) numElements, null );
		final java.util.function.ToIntFunction< Object > accessor = o -> value;
		return new GeffPropertyWriter<>( dummies, ElementType.NODE )
				.id( ( java.util.function.ToLongFunction< Object > ) o -> 0L, "<u8" )
				.add( identifier, accessor )
				.createGeffProperties()
				.property( identifier );
	}

	/**
	 * Synthesize an optional double property with a constant present value
	 * ( the TrackMate default spot radius ) for all elements.
	 */
	private static GeffProperty< ? > constantRadiusProperty( final long numElements )
	{
		final List< Object > dummies = Collections.nCopies( ( int ) numElements, null );
		final FunctionTypes.ToMaybeDoubleFunction< Object > accessor = o -> new Maybe.MaybeDouble( DEFAULT_SPOT_RADIUS );
		return new GeffPropertyWriter<>( dummies, ElementType.NODE )
				.id( ( java.util.function.ToLongFunction< Object > ) o -> 0L, "<u8" )
				.add( RADIUS_PROP_NAME, accessor )
				.createGeffProperties()
				.property( RADIUS_PROP_NAME );
	}

	/**
	 * Synthesize an optional int property with all values missing.
	 */
	private static GeffProperty< ? > missingIntProperty( final String identifier, final long numElements )
	{
		final List< Object > dummies = Collections.nCopies( ( int ) numElements, null );
		final FunctionTypes.ToMaybeIntFunction< Object > accessor = o -> new Maybe.MaybeInt();
		return new GeffPropertyWriter<>( dummies, ElementType.NODE )
				.id( ( java.util.function.ToLongFunction< Object > ) o -> 0L, "<u8" )
				.add( identifier, accessor )
				.createGeffProperties()
				.property( identifier );
	}

	/**
	 * Synthesize an optional string property with all values missing.
	 */
	private static GeffProperty< ? > missingStringProperty( final String identifier, final long numElements )
	{
		final List< Object > dummies = Collections.nCopies( ( int ) numElements, null );
		final FunctionTypes.ToMaybeStringFunction< Object > accessor = o -> new Maybe.MaybeString();
		return new GeffPropertyWriter<>( dummies, ElementType.NODE )
				.id( ( java.util.function.ToLongFunction< Object > ) o -> 0L, "<u8" )
				.add( identifier, accessor )
				.createGeffProperties()
				.property( identifier );
	}


	private void readSpots(
			final Model model,
			final boolean is2D,
			final TIntObjectMap< Spot > spotIdMap,
			final TObjectIntMap< Spot > spotTrackIDMap ) throws IOException, GeffException
	{
		final SpotCollection spots = model.getSpots();
		try (final N5Reader n5 = new N5ZarrReader( geffPath ))
		{
			final GeffProperties props = IoUtils.loadProperties( n5, ElementType.NODE );

			/*
			 * Map the properties of the file to the TrackMate-canonical names
			 * the builders expect. We try the display hints first, then
			 * heuristics on the axis names, then accept the GEFF-conventional
			 * literal names. TrackMate-specific properties that are missing
			 * are synthesized with sensible defaults. See
			 * {@link #mapCoreSpotProps(GeffProperties, boolean)}.
			 */
			mapCoreSpotProps( props, is2D );

			final boolean hasPolygons = props.properties().containsKey( POLYGON_PROP );
			final boolean hasMeshes = props.properties().containsKey( MESH_V_PROP )
					&& props.properties().containsKey( MESH_VN_PROP )
					&& props.properties().containsKey( MESH_F_PROP );
			if ( hasMeshes && hasPolygons )
				throw new GeffException( "Cannot handle a model with both polygon and mesh shapes." );

			// Select the builder adapted to the content of the file.
			final AbstractSpotBuilder builder;
			if ( is2D )
			{
				if ( hasPolygons )
					builder = new SpotRoiBuilder2D( spots );
				else
					builder = new SpotBaseBuilder2D( spots );
			}
			else
			{
				if ( hasMeshes )
					builder = new SpotMeshBuilder3D( spots );
				else
					builder = new SpotBaseBuilder3D( spots );
			}

			// Build the spots.
			try
			{
				final ElementCreator creator = ElementCreator.of( builder, props );
				for ( int i = 0; i < props.numElements(); i++ )
					creator.createElement( i );
			}
			catch ( final IllegalAccessException | NoSuchMethodException | GeffException e )
			{
				throw ( e instanceof GeffException )
						? ( GeffException ) e
						: new GeffException( e );
			}

			/*
			 * Post-processing pass: rebuild the maps we need for the graph
			 * (GEFF id -> spot; spot -> TrackMate track id), and put back all
			 * the features that are not bound in the builders.
			 */
			final List< Spot > spotList = builder.spots();
			final LongSupplier idSupplier = Suppliers.asLongSupplier( props.id() );
			final String trackIdProp = metadata.getTrackNodeProps() == null
					? null
					: metadata.getTrackNodeProps().get( "lineage" );
			final Supplier< OptionalInt > trackIdSupplier = ( trackIdProp == null )
					? null
					: Suppliers.asOptionalIntSupplier( props.properties().get( trackIdProp ) );

			for ( int i = 0; i < props.numElements(); i++ )
			{
				props.elementIndex().set( i );
				final Spot spot = spotList.get( i );

				// GEFF id -> spot.
				spotIdMap.put( ( int ) idSupplier.getAsLong(), spot );

				// spot -> TrackMate track id.
				if ( trackIdSupplier != null )
				{
					final OptionalInt trackID = trackIdSupplier.get();
					if ( trackID.isPresent() )
						spotTrackIDMap.put( spot, trackID.getAsInt() );
				}

				// All the other properties become features.
				for ( final Map.Entry< String, GeffProperty< ? > > entry : props.properties().entrySet() )
				{
					final String key = entry.getKey();
					if ( CONSUMED_PROPS.contains( key ) )
						continue;
					if ( trackIdProp != null && trackIdProp.equals( key ) )
						continue;
					final GeffProperty< ? > prop = entry.getValue();
					if ( prop.isVarlength() )
						continue; // We do not know how to interpret it as a feature.
					final DoubleSupplier value = Suppliers.asDoubleSupplier( prop.convert( DoubleType::new ) );
					spot.putFeature( key, Double.valueOf( value.getAsDouble() ) );
				}
			}
		}
	}

	/**
	 * Reads the edges of the GEFF file and builds the graph. The graph is not
	 * wired into the model yet: {@link fiji.plugin.trackmate.TrackModel#from}
	 * needs the track visibility and names, which are read from the tracks
	 * sub-file in the next step.
	 * <p>
	 * Edge features are put back as-is, except the structural ones: the source
	 * and target spot IDs are restored from the spots themselves, and the
	 * {@code Z_LOCATION} feature is given a dummy value of 0 in 2D (it was not
	 * serialized). The edge weight is taken from the {@code EDGE_COST} feature
	 * if present.
	 *
	 * @param model
	 *            the model being built (to access its feature model).
	 * @param is2D
	 *            whether the model is 2D.
	 * @param spotIdMap
	 *            the map from GEFF node id to spot, as built by
	 *            {@link #readSpots}.
	 * @param spotTrackIDMap
	 *            the map from spot to TrackMate track id, as built by
	 *            {@link #readSpots}.
	 * @param graph
	 *            the graph to populate.
	 * @param connectedVertexSet
	 *            the map from track id to the spots of the track, populated
	 *            here from the spot lineage.
	 * @param connectedEdgeSet
	 *            the map from track id to the edges of the track, populated
	 *            here.
	 * @throws IOException
	 *             if an I/O error occurs.
	 * @throws GeffException
	 *             if the file is not a valid GEFF file.
	 */
	private void readEdges(
			final Model model,
			final boolean is2D,
			final TIntObjectMap< Spot > spotIdMap,
			final TObjectIntMap< Spot > spotTrackIDMap,
			final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph,
			final Map< Integer, Set< Spot > > connectedVertexSet,
			final Map< Integer, Set< DefaultWeightedEdge > > connectedEdgeSet ) throws IOException, GeffException
	{
		try (final N5Reader n5 = new N5ZarrReader( geffPath ))
		{
			// The file might not have any edge at all.
			if ( !n5.datasetExists( "edges/ids" ) )
				return;

			final GeffProperties edgeProps = IoUtils.loadProperties( n5, ElementType.EDGE );

			// Build the edges with the annotation framework.
			final EdgeBuilder builder = new EdgeBuilder( spotIdMap, graph );
			try
			{
				final ElementCreator creator = ElementCreator.of( builder, edgeProps );
				for ( int i = 0; i < edgeProps.numElements(); i++ )
					creator.createElement( i );
			}
			catch ( final IllegalAccessException | NoSuchMethodException | GeffException e )
			{
				throw ( e instanceof GeffException )
						? ( GeffException ) e
						: new GeffException( e );
			}

			/*
			 * Post-processing pass: put back the edge features, the edge
			 * weight, and the track membership from the spot lineage.
			 */
			final FeatureModel fm = model.getFeatureModel();
			final List< DefaultWeightedEdge > edgeList = builder.edgeList();
			for ( int i = 0; i < edgeProps.numElements(); i++ )
			{
				edgeProps.elementIndex().set( i );
				final DefaultWeightedEdge edge = edgeList.get( i );
				final Spot source = graph.getEdgeSource( edge );
				final Spot target = graph.getEdgeTarget( edge );

				// Structural features, from the spots themselves.
				fm.putEdgeFeature( edge, SPOT_SOURCE_ID, Double.valueOf( source.ID() ) );
				fm.putEdgeFeature( edge, SPOT_TARGET_ID, Double.valueOf( target.ID() ) );

				// We did not serialize the Z location for the 2D case, but we
				// need to put something back, because the feature model
				// expects it to be there. This default value will be
				// overwritten if the edge has a Z feature value.
				if ( is2D )
					fm.putEdgeFeature( edge, Z_LOCATION, Double.valueOf( 0. ) );

				// All the other properties become features.
				for ( final Map.Entry< String, GeffProperty< ? > > entry : edgeProps.properties().entrySet() )
				{
					final String key = entry.getKey();
					if ( key.equals( SPOT_SOURCE_ID ) || key.equals( SPOT_TARGET_ID ) )
						continue;
					final GeffProperty< ? > prop = entry.getValue();
					if ( prop.isVarlength() )
						continue; // We do not know how to interpret it as a feature.
					final DoubleSupplier value = Suppliers.asDoubleSupplier( prop.convert( DoubleType::new ) );
					fm.putEdgeFeature( edge, key, Double.valueOf( value.getAsDouble() ) );
				}

				// Edge weight.
				final Double weight = fm.getEdgeFeature( edge, EDGE_COST );
				graph.setEdgeWeight( edge, ( weight == null ) ? 1.0 : weight.doubleValue() );

				// Track membership, from the spot lineage.
				final int trackID = spotTrackIDMap.get( source );
				connectedVertexSet.computeIfAbsent( trackID, k -> new HashSet<>() ).add( source );
				connectedVertexSet.get( trackID ).add( target );
				connectedEdgeSet.computeIfAbsent( trackID, k -> new HashSet<>() ).add( edge );
			}
		}
	}

	/**
	 * Reads the tracks sub-GEFF file, if there is one, and fills the track
	 * visibility and name maps, as well as the per-track feature values. The
	 * sub-file is absent when the model has no tracks, in which case this
	 * method returns immediately with empty maps.
	 * <p>
	 * The GEFF node ids of the sub-file are the TrackMate track ids. The
	 * {@code VISIBILITY} and {@code name} properties are restored to the maps,
	 * and the core {@code t}, {@code x}, {@code y}, {@code z} properties are
	 * mapped back to the {@code TRACK_START}, {@code X_LOCATION},
	 * {@code Y_LOCATION} and {@code Z_LOCATION} features. The {@code radius}
	 * property is restored as twice the {@code TRACK_MAX_DISTANCE_TRAVELED}
	 * feature, since it was repurposed as the sphere radius of the track node
	 * on writing.
	 * <p>
	 * FIXME: The tracks sub-GEFF file is a TrackMate extension and must not be
	 * required to read a GEFF file. When it is missing while the file has
	 * edges, the tracks must be reconstructed from the graph instead of
	 * leaving it in an incoherent state: group the edges into connected
	 * components (e.g. with JGraphT {@code ConnectivityInspector}), assign
	 * them fresh synthetic track ids, with default {@code true} visibility and
	 * default {@code Track_<n>} names, and no per-track feature values.
	 * Currently a foreign file with edges but without the sub-file is read
	 * with all its edges orphaned (no track membership).
	 *
	 * @param model
	 *            the model being built (to access its feature model).
	 * @param visibility
	 *            the map from track id to visibility, populated here. It must
	 *            contain an entry for every track id, as
	 *            {@link fiji.plugin.trackmate.TrackModel#from} expects it.
	 * @param savedTrackNames
	 *            the map from track id to track name, populated here.
	 * @return whether the tracks sub-file exists and was read.
	 * @throws IOException
	 *             if an I/O error occurs.
	 * @throws GeffException
	 *             if the file is not a valid GEFF file.
	 */
	private boolean readTracks(
			final Model model,
			final Map< Integer, Boolean > visibility,
			final Map< Integer, String > savedTrackNames ) throws IOException, GeffException
	{
		final Path trackGeffPath = Paths.get( geffPath, TRACK_GEFF_NAME );
		if ( !Files.exists( trackGeffPath ) )
			return false;

		final FeatureModel fm = model.getFeatureModel();
		try (final N5Reader n5 = new N5ZarrReader( trackGeffPath.toString() ))
		{
			final GeffProperties props = IoUtils.loadProperties( n5, ElementType.NODE );

			/*
			 * The tracks sub-file is normally written by TrackMate, but be
			 * tolerant with foreign or incomplete ones: the visibility
			 * defaults to true, and the names to the TrackMate default
			 * naming.
			 */
			if ( !props.properties().containsKey( VISIBILITY ) )
				props.put( missingIntProperty( VISIBILITY, props.numElements() ) );
			if ( !props.properties().containsKey( NAME_PROP ) )
				props.put( missingStringProperty( NAME_PROP, props.numElements() ) );

			// Build the track nodes with the annotation framework.
			final TrackBuilder builder = new TrackBuilder( visibility, savedTrackNames );
			try
			{
				final ElementCreator creator = ElementCreator.of( builder, props );
				for ( int i = 0; i < props.numElements(); i++ )
					creator.createElement( i );
			}
			catch ( final IllegalAccessException | NoSuchMethodException | GeffException e )
			{
				throw ( e instanceof GeffException )
						? ( GeffException ) e
						: new GeffException( e );
			}

			/*
			 * Post-processing pass: put back the per-track feature values. The
			 * core properties have GEFF-conventional names, and must be mapped
			 * back to the TrackMate feature keys.
			 */
			final LongSupplier idSupplier = Suppliers.asLongSupplier( props.id() );
			for ( int i = 0; i < props.numElements(); i++ )
			{
				props.elementIndex().set( i );
				final int trackID = ( int ) idSupplier.getAsLong();

				// TRACK_ID as a track feature.
				fm.putTrackFeature( trackID, TRACK_ID, Double.valueOf( trackID ) );

				// Put Z = 0 in 2D.
				if ( !props.properties().containsKey( "z" ) )
					fm.putTrackFeature( trackID, TrackLocationAnalyzer.Z_LOCATION, Double.valueOf( 0. ) );

				for ( final Map.Entry< String, GeffProperty< ? > > entry : props.properties().entrySet() )
				{
					final String key = entry.getKey();
					if ( key.equals( VISIBILITY ) || key.equals( NAME_PROP ) )
						continue;

					final GeffProperty< ? > prop = entry.getValue();
					if ( prop.isVarlength() )
						continue; // We do not know how to interpret it as a feature.

					if ( key.equals( RADIUS_PROP_NAME ) )
					{
						// The excursion radius was serialized as the sphere
						// radius of the track node. Restore the TrackMate
						// feature as twice it, and only when it is present.
						final Maybe.MaybeDouble r = Suppliers.asMaybeDoubleSupplier( prop ).get();
						if ( r.isPresent() )
							fm.putTrackFeature( trackID, TRACK_MAX_DISTANCE_TRAVELED, Double.valueOf( 2. * r.get() ) );
						continue;
					}

					// Map the GEFF-conventional core property names back to
					// the TrackMate feature keys.
					final String featureKey;
					if ( key.equals( "t" ) )
						featureKey = TRACK_START;
					else if ( key.equals( "x" ) )
						featureKey = X_LOCATION;
					else if ( key.equals( "y" ) )
						featureKey = Y_LOCATION;
					else if ( key.equals( "z" ) )
						featureKey = TrackLocationAnalyzer.Z_LOCATION;
					else
						featureKey = key;

					final double val = Suppliers.asDoubleSupplier( prop.convert( DoubleType::new ) ).getAsDouble();
					fm.putTrackFeature( trackID, featureKey, Double.valueOf( val ) );
				}
			}
		}
		return true;
	}

	/*
	 * TRACK BUILDER.
	 *
	 * Same annotation framework as for the spots. We only bind the properties
	 * needed to fill the visibility and name maps; the feature values are
	 * restored in the post-processing pass of {@link #readTracks}.
	 */
	public static class TrackBuilder
	{

		private final Map< Integer, Boolean > visibility;

		private final Map< Integer, String > savedTrackNames;

		TrackBuilder( final Map< Integer, Boolean > visibility, final Map< Integer, String > savedTrackNames )
		{
			this.visibility = visibility;
			this.savedTrackNames = savedTrackNames;
		}

		@NodeConstructor
		public void addNode(
				@FromId final long id,
				@FromProperty( VISIBILITY ) final OptionalInt visibilityFlag,
				@FromProperty( NAME_PROP ) final Optional< String > name )
		{
			final int trackID = ( int ) id;
			// Tracks of foreign files without a visibility property are
			// visible by default.
			final boolean visible = !visibilityFlag.isPresent() || visibilityFlag.getAsInt() > 0;
			visibility.put( Integer.valueOf( trackID ), Boolean.valueOf( visible ) );
			if ( name.isPresent() )
				savedTrackNames.put( Integer.valueOf( trackID ), name.get() );
		}
	}

	/*
	 * SPOT BUILDERS.
	 *
	 * Each builder is bound to the properties of the file by the annotation
	 * framework: one method annotated with {@link NodeConstructor}, one
	 * parameter per property. They only take care of instantiating the right
	 * spot class and adding it to the collection; everything else (GEFF id
	 * mapping, other features) is done in the post-processing pass of
	 * {@link #readSpots}.
	 */
	public abstract static class AbstractSpotBuilder
	{
		protected final SpotCollection spots;

		/** The spots created by this builder, in creation order. */
		final List< Spot > spotList = new ArrayList<>();

		AbstractSpotBuilder( final SpotCollection spots )
		{
			this.spots = spots;
		}

		List< Spot > spots()
		{
			return spotList;
		}

		/**
		 * Returns the TrackMate spot ID to use: the one from the file when it
		 * is present, or a fresh one from the {@link SpotBase#IDcounter}
		 * otherwise (for foreign GEFF files without a spot-ID property).
		 */
		protected static int spotId( final OptionalInt trackmateId )
		{
			return trackmateId.isPresent()
					? trackmateId.getAsInt()
					: SpotBase.IDcounter.incrementAndGet();
		}

		protected void setUp(
				final Spot spot,
				final double x,
				final double y,
				final double z,
				final int t,
				final Maybe.MaybeDouble r,
				final Optional< String > name )
		{
			spot.setPosition( x, 0 );
			spot.setPosition( y, 1 );
			spot.setPosition( z, 2 );
			spot.putFeature( Spot.POSITION_X, Double.valueOf( x ) );
			spot.putFeature( Spot.POSITION_Y, Double.valueOf( y ) );
			spot.putFeature( Spot.POSITION_Z, Double.valueOf( z ) );
			spot.putFeature( FRAME, Double.valueOf( t ) );
			if ( r.isPresent() )
				spot.putFeature( Spot.RADIUS, Double.valueOf( r.get() ) );
			if ( name.isPresent() )
				spot.setName( name.get() );
			spots.add( spot, t );
			spotList.add( spot );
		}
	}

	public static class SpotBaseBuilder2D extends AbstractSpotBuilder
	{
		SpotBaseBuilder2D( final SpotCollection spots )
		{
			super( spots );
		}

		@NodeConstructor
		public void addNode(
				@FromId final long id,
				@FromProperty( "x" ) final double x,
				@FromProperty( "y" ) final double y,
				@FromProperty( "t" ) final int t,
				@FromProperty( RADIUS_PROP_NAME ) final Maybe.MaybeDouble r,
				@FromProperty( TRACKMATE_SPOT_ID_PROP ) final OptionalInt trackmateId,
				@FromProperty( NAME_PROP ) final Optional< String > name )
		{
			final SpotBase spot = new SpotBase( spotId( trackmateId ) );
			setUp( spot, x, y, 0., t, r, name );
		}
	}

	public static class SpotBaseBuilder3D extends AbstractSpotBuilder
	{
		SpotBaseBuilder3D( final SpotCollection spots )
		{
			super( spots );
		}

		@NodeConstructor
		public void addNode(
				@FromId final long id,
				@FromProperty( "x" ) final double x,
				@FromProperty( "y" ) final double y,
				@FromProperty( "z" ) final double z,
				@FromProperty( "t" ) final int t,
				@FromProperty( RADIUS_PROP_NAME ) final Maybe.MaybeDouble r,
				@FromProperty( TRACKMATE_SPOT_ID_PROP ) final OptionalInt trackmateId,
				@FromProperty( NAME_PROP ) final Optional< String > name )
		{
			final SpotBase spot = new SpotBase( spotId( trackmateId ) );
			setUp( spot, x, y, z, t, r, name );
		}
	}

	public static class SpotRoiBuilder2D extends AbstractSpotBuilder
	{
		SpotRoiBuilder2D( final SpotCollection spots )
		{
			super( spots );
		}

		@NodeConstructor
		public void addNode(
				@FromId final long id,
				@FromProperty( "x" ) final double x,
				@FromProperty( "y" ) final double y,
				@FromProperty( "t" ) final int t,
				@FromProperty( RADIUS_PROP_NAME ) final Maybe.MaybeDouble r,
				@FromProperty( TRACKMATE_SPOT_ID_PROP ) final OptionalInt trackmateId,
				@FromProperty( POLYGON_PROP ) final Optional< double[] > polygon,
				@FromProperty( NAME_PROP ) final Optional< String > name )
		{
			final Spot spot;
			if ( !polygon.isPresent() )
			{
				spot = new SpotBase( spotId( trackmateId ) );
			}
			else
			{
				// Polygon coordinates are relative to the spot center, as
				// mandated by the GEFF spec — just like SpotRoi.
				final double[] p = polygon.get();
				final int nPoints = p.length / 2;
				final double[] xp = new double[ nPoints ];
				final double[] yp = new double[ nPoints ];
				for ( int i = 0; i < nPoints; i++ )
				{
					xp[ i ] = p[ 2 * i ];
					yp[ i ] = p[ 2 * i + 1 ];
				}
				spot = new SpotRoi( spotId( trackmateId ), xp, yp );
			}
			setUp( spot, x, y, 0., t, r, name );
		}
	}

	public static class SpotMeshBuilder3D extends AbstractSpotBuilder
	{
		SpotMeshBuilder3D( final SpotCollection spots )
		{
			super( spots );
		}

		@NodeConstructor
		public void addNode(
				@FromId final long id,
				@FromProperty( "x" ) final double x,
				@FromProperty( "y" ) final double y,
				@FromProperty( "z" ) final double z,
				@FromProperty( "t" ) final int t,
				@FromProperty( RADIUS_PROP_NAME ) final Maybe.MaybeDouble r,
				@FromProperty( TRACKMATE_SPOT_ID_PROP ) final OptionalInt trackmateId,
				@FromProperty( MESH_V_PROP ) final Optional< float[] > meshVertices,
				@FromProperty( MESH_VN_PROP ) final Optional< float[] > meshNormals,
				@FromProperty( MESH_F_PROP ) final Optional< long[] > meshTriangles,
				@FromProperty( NAME_PROP ) final Optional< String > name )
		{
			final Spot spot;
			if ( !meshVertices.isPresent() || !meshNormals.isPresent() || !meshTriangles.isPresent() )
			{
				spot = new SpotBase( spotId( trackmateId ) );
			}
			else
			{
				spot = new SpotMesh( spotId( trackmateId ), buildMesh( meshVertices.get(), meshNormals.get(), meshTriangles.get() ) );
				// NB: The SpotMesh constructor re-centers the mesh and sets
				// the spot position and radius from it. The mesh vertices are
				// already relative to the spot center, so its center is (0, 0,
				// 0) up to numerical error, and the call to setUp below
				// restores the position and radius from the file.
			}
			setUp( spot, x, y, z, t, r, name );
		}
	}

	/**
	 * Builds a {@link BufferMesh} from the interleaved vertex coordinates and
	 * normals, and flattened triangle indices, as stored in the GEFF file.
	 */
	private static BufferMesh buildMesh( final float[] v, final float[] vn, final long[] f )
	{
		final int nVertices = v.length / 3;
		final int nTriangles = f.length / 3;
		final BufferMesh mesh = new BufferMesh( nVertices, nTriangles );

		final Vertices vertices = mesh.vertices();
		for ( int i = 0; i < nVertices; i++ )
			vertices.addf(
					v[ 3 * i ], v[ 3 * i + 1 ], v[ 3 * i + 2 ],
					vn[ 3 * i ], vn[ 3 * i + 1 ], vn[ 3 * i + 2 ],
					0, 0 );

		final Triangles triangles = mesh.triangles();
		for ( int i = 0; i < nTriangles; i++ )
			triangles.add( ( int ) f[ 3 * i ], ( int ) f[ 3 * i + 1 ], ( int ) f[ 3 * i + 2 ], 0f, 0f, 0f );

		return mesh;
	}

	/*
	 * EDGE BUILDER.
	 *
	 * Same annotation framework as for the spots, but with the
	 * {@link EdgeConstructor} annotation, picked automatically by
	 * {@link ElementCreator} because the properties have the EDGE element
	 * type. The edge id is the fixed-length pair of GEFF node ids (source
	 * first), bound as a {@code long[]} array.
	 */

	public static class EdgeBuilder
	{

		private final TIntObjectMap< Spot > spotIdMap;

		private final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph;

		/** The edges created by this builder, in creation order. */
		final List< DefaultWeightedEdge > edgeList = new ArrayList<>();

		EdgeBuilder( final TIntObjectMap< Spot > spotIdMap, final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph )
		{
			this.spotIdMap = spotIdMap;
			this.graph = graph;
		}

		List< DefaultWeightedEdge > edgeList()
		{
			return edgeList;
		}

		@EdgeConstructor
		public void addEdge( @FromId final long[] ids )
		{
			final Spot source = spotIdMap.get( ( int ) ids[ 0 ] );
			final Spot target = spotIdMap.get( ( int ) ids[ 1 ] );
			if ( source == null || target == null )
				throw new IllegalStateException( "Edge refers to unknown GEFF node ids: [" + ids[ 0 ] + ", " + ids[ 1 ] + "]." );
			graph.addVertex( source );
			graph.addVertex( target );
			edgeList.add( graph.addEdge( source, target ) );
		}
	}
}
