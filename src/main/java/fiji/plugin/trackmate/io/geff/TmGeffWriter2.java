package fiji.plugin.trackmate.io.geff;

import static fiji.plugin.trackmate.Spot.FRAME;
import static fiji.plugin.trackmate.Spot.POSITION_X;
import static fiji.plugin.trackmate.Spot.POSITION_Y;
import static fiji.plugin.trackmate.Spot.POSITION_Z;
import static fiji.plugin.trackmate.Spot.RADIUS;
import static fiji.plugin.trackmate.features.track.TrackIndexAnalyzer.TRACK_ID;
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
import java.util.function.ToLongFunction;

import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.mastodon.geff.GeffAxis;
import org.mastodon.geff.GeffMetadata;
import org.mastodon.geff.GeffMetadata.GeffDisplayHints;
import org.mastodon.geff.GeffMetadata.RelatedObjects;
import org.mastodon.geff.PropMetadata;
import org.mastodon.geff.imglib2.ElementType;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeDoubleFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeIntFunction;
import org.mastodon.geff.imglib2.GeffPropertyWriter;
import org.mastodon.geff.imglib2.Maybe;

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
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.io.json.SettingsIO;
import fiji.plugin.trackmate.util.TMUtils;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * Let's try to write a GEFF writer with Tobias' new writer and reader APIs.
 */
public class TmGeffWriter2
{

	// Because we specify meshes. To check later.
	private static final String GEFF_VERSION = "1.3.1";

	static final String TRACKMATE_SPOT_ID_PROP = "trackmate_spot_id";

	static final String NAME_PROP = "name";

	static final String POLYGON_PROP = "polygon";

	static final String MESH_V_PROP = "mesh_vertices";

	static final String MESH_VN_PROP = "mesh_vertex_normals";

	static final String MESH_F_PROP = "mesh_triangles";

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

	/** The chunk size to use when writing the GEFF file. */
	private int chunkSize = 8 * 1024; // 8 kiB;

	/**
	 * Creates a new writer that will write a GEFF file to the specified path (a
	 * Zarr container).
	 *
	 * @param zarrPath
	 *            the path to the GEFF file (a Zarr container).
	 */
	public TmGeffWriter2( final String zarrPath )
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
	 * TODO: Also write the tracks!
	 *
	 * @param model
	 *            the model whose {@link SpotCollection} to serialize.
	 */
	public void appendModel( final Model model )
	{

		final SpotCollection spots = model.getSpots();
		final FeatureModel fm = model.getFeatureModel();
		final boolean is2D = DetectionUtils.is2D( model );
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
		final Set< String > featureKeys = new LinkedHashSet<>();
		for ( final Spot spot : spotList )
		{
			hasRoi |= spot instanceof SpotRoi;
			hasMesh |= spot instanceof SpotMesh;
			featureKeys.addAll( spot.getFeatures().keySet() );
		}

		// Initialize the property writer for the spots.
		final GeffPropertyWriter< Spot > writer = new GeffPropertyWriter<>( spotList, ElementType.NODE );

		// Now we create an accessor for each property we want to write.
		// Ids: sequential, in iteration order.
		final ToLongFunction< Spot > idAccessor = spot -> spotToId.get( spot );
		writer.id( idAccessor, "<u8" );

		// Time and position. Space and time properties use the GEFF
		// conventional names 't', 'x', 'y', 'z'
		writer.add( "t", TmGeffWriter2::t );
		writer.add( "x", TmGeffWriter2::x );
		writer.add( "y", TmGeffWriter2::y );
		if ( !is2D )
			writer.add( "z", TmGeffWriter2::z );

		// Radius, possibly missing.
		writer.add( "radius", TmGeffWriter2::radius );

		// TrackMate ID and name.
		writer.add( TRACKMATE_SPOT_ID_PROP, Spot::ID );
		writer.add( NAME_PROP, TmGeffWriter2::name );

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
		for ( final String feature : featureKeys )
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
			writer.add( POLYGON_PROP, TmGeffWriter2::polygon );

		// SpotMesh: vertices positions and normals, triangle indices.
		if ( hasMesh )
		{
			writer.add( MESH_V_PROP, TmGeffWriter2::meshVertices );
			writer.add( MESH_VN_PROP, TmGeffWriter2::meshNormals );
			writer.add( MESH_F_PROP, TmGeffWriter2::meshTriangles );
		}

		// Store the writer for later use in write().
		this.spotWriter = writer;

		/*
		 * Metadata: only property datasets are written by the
		 * GeffPropertyWriter; we have to build the GEFF metadata ourselves.
		 */
		buildMetadata( model, is2D, featureKeys, hasTracks, hasRoi, hasMesh );
	}

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
		if ( spotWriter != null )
		{
			// TODO: log?
			try (final N5Writer n5 = new N5ZarrWriter( geffPath ))
			{
				spotWriter.write( n5, new RawCompression(), chunkSize );
			}
		}

		// Metadata.
		GeffMetadata.writeToZarr( metadata, geffPath );
	}

	public int getChunkSize()
	{
		return chunkSize;
	}

	public void setChunkSize( final int chunkSize )
	{
		this.chunkSize = chunkSize;
	}

	private void buildMetadata(
			final Model model,
			final boolean is2D,
			final Set< String > featureKeys,
			final boolean hasTracks,
			final boolean hasRoi,
			final boolean hasMesh )
	{
		final FeatureModel fm = model.getFeatureModel();
		final String spaceUnits = model.getSpaceUnits();
		final String timeUnits = model.getTimeUnits();

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
		nodePropsMetadata.put( "radius", new PropMetadata( "radius", "float64", false, spaceUnits, Spot.FEATURE_NAMES.get( RADIUS ), "The radius of the spot" ) );
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

		for ( final String feature : featureKeys )
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

		// Shape declarations: declare which node property holds the shapes,
		// as mandated by the GEFF specification.
		metadata.setSphere( "radius" );
		if ( hasMesh )
			metadata.setMesh( new GeffMetadata.MeshMetadata( MESH_V_PROP, MESH_F_PROP, MESH_VN_PROP, null ) );
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
	 * Returns the track ID of the specified spot, or an empty {@link Maybe}
	 * if the spot is not part of a track.
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
