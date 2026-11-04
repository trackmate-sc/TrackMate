package fiji.plugin.trackmate.io.geff;

import static fiji.plugin.trackmate.Spot.FRAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_STATE_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.LOG_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.geff.TmGeffWriter2.MESH_F_PROP;
import static fiji.plugin.trackmate.io.geff.TmGeffWriter2.MESH_VN_PROP;
import static fiji.plugin.trackmate.io.geff.TmGeffWriter2.MESH_V_PROP;
import static fiji.plugin.trackmate.io.geff.TmGeffWriter2.NAME_PROP;
import static fiji.plugin.trackmate.io.geff.TmGeffWriter2.POLYGON_PROP;
import static fiji.plugin.trackmate.io.geff.TmGeffWriter2.RADIUS_PROP_NAME;
import static fiji.plugin.trackmate.io.geff.TmGeffWriter2.TRACKMATE_SPOT_ID_PROP;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.mastodon.geff.GeffMetadata;
import org.mastodon.geff.GeffMetadata.RelatedObjects;
import org.mastodon.geff.imglib2.Construction.FromId;
import org.mastodon.geff.imglib2.Construction.FromProperty;
import org.mastodon.geff.imglib2.Construction.NodeConstructor;
import org.mastodon.geff.imglib2.ElementCreator;
import org.mastodon.geff.imglib2.ElementType;
import org.mastodon.geff.imglib2.GeffException;
import org.mastodon.geff.imglib2.GeffProperties;
import org.mastodon.geff.imglib2.GeffProperty;
import org.mastodon.geff.imglib2.IoUtils;
import org.mastodon.geff.imglib2.Maybe;
import org.mastodon.geff.imglib2.Suppliers;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
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
public class TmGeffReader2
{

	private final String geffPath;

	private GeffMetadata metadata;

	public TmGeffReader2( final String geffPath )
	{
		this.geffPath = geffPath;
	}

	public String getLog()
	{
		if ( null == metadata )
			metadata = GeffMetadata.readFromZarr( geffPath );

		@SuppressWarnings( "unchecked" )
		final Map< String, Object > trackmateMD = ( Map< String, Object > ) metadata.getExtra().get( "trackmate" );
		final String log = ( String ) trackmateMD.get( LOG_ELEMENT_KEY );
		return log;
	}

	public String getGUIState()
	{
		if ( null == metadata )
			metadata = GeffMetadata.readFromZarr( geffPath );

		@SuppressWarnings( "unchecked" )
		final Map< String, Object > trackmateMD = ( Map< String, Object > ) metadata.getExtra().get( "trackmate" );
		final String guiState = ( String ) trackmateMD.get( GUI_STATE_ELEMENT_KEY );
		return guiState;
	}

	@SuppressWarnings( "unchecked" )
	public DisplaySettings getDisplaySettings()
	{
		if ( null == metadata )
			metadata = GeffMetadata.readFromZarr( geffPath );

		final Map< String, Object > trackmateMD = ( Map< String, Object > ) metadata.getExtra().get( "trackmate" );
		final Map< String, Object > displaySettingsJson = ( Map< String, Object > ) trackmateMD.get( "displaySettings" );
		return DisplaySettingsIO.fromJsonTree( displaySettingsJson );
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

		final Map< String, Object > trackmateMD = ( Map< String, Object > ) metadata.getExtra().get( "trackmate" );
		final Map< String, Object > settingsJson = ( Map< String, Object > ) trackmateMD.get( "settings" );

		final Settings settings = new Settings( imp );
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

		// TODO: Read edges.
		// TODO: Read tracks.

		// TODO: Read feature declarations.

		return model;
	}

	private boolean readUnits( final Model model )
	{
		String spaceUnits = null;
		String timeUnits = null;
		int spaceDim = 0;
		for ( final var axis : metadata.getGeffAxesList() )
		{
			if ( spaceUnits == null )
				spaceUnits = axis.getUnit();

			if ( axis.getType().equals( org.mastodon.geff.GeffAxis.TYPE_SPACE ) )
				spaceDim++;

			if ( timeUnits == null && axis.getType().equals( org.mastodon.geff.GeffAxis.TYPE_TIME ) )
				timeUnits = axis.getUnit();
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
			 * TODO! Right now we are strict and EXPECTS the core properties to
			 * have certain names. This must be relaxed, by looking at display
			 * hints to guess what is x, y, ...
			 */
			for ( final String required : new String[] { "x", "y", "t", RADIUS_PROP_NAME, TRACKMATE_SPOT_ID_PROP, NAME_PROP } )
				if ( !props.properties().containsKey( required ) )
					throw new GeffException( "Missing required node property: '" + required + "'." );
			if ( !is2D && !props.properties().containsKey( "z" ) )
				throw new GeffException( "Missing required node property: 'z' (3D model)." );

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
					final java.util.function.DoubleSupplier value = Suppliers.asDoubleSupplier( prop.convert( DoubleType::new ) );
					spot.putFeature( key, Double.valueOf( value.getAsDouble() ) );
				}
			}
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
				@FromProperty( TRACKMATE_SPOT_ID_PROP ) final int trackmateId,
				@FromProperty( NAME_PROP ) final Optional< String > name )
		{
			final SpotBase spot = new SpotBase( trackmateId );
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
				@FromProperty( TRACKMATE_SPOT_ID_PROP ) final int trackmateId,
				@FromProperty( NAME_PROP ) final Optional< String > name )
		{
			final SpotBase spot = new SpotBase( trackmateId );
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
				@FromProperty( TRACKMATE_SPOT_ID_PROP ) final int trackmateId,
				@FromProperty( POLYGON_PROP ) final Optional< double[] > polygon,
				@FromProperty( NAME_PROP ) final Optional< String > name )
		{
			final Spot spot;
			if ( !polygon.isPresent() )
			{
				spot = new SpotBase( trackmateId );
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
				spot = new SpotRoi( trackmateId, xp, yp );
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
				@FromProperty( TRACKMATE_SPOT_ID_PROP ) final int trackmateId,
				@FromProperty( MESH_V_PROP ) final Optional< float[] > meshVertices,
				@FromProperty( MESH_VN_PROP ) final Optional< float[] > meshNormals,
				@FromProperty( MESH_F_PROP ) final Optional< long[] > meshTriangles,
				@FromProperty( NAME_PROP ) final Optional< String > name )
		{
			final Spot spot;
			if ( !meshVertices.isPresent() || !meshNormals.isPresent() || !meshTriangles.isPresent() )
			{
				spot = new SpotBase( trackmateId );
			}
			else
			{
				spot = new SpotMesh( trackmateId, buildMesh( meshVertices.get(), meshNormals.get(), meshTriangles.get() ) );
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
}
