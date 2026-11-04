package fiji.plugin.trackmate.io.geff.imglib2;

import static fiji.plugin.trackmate.Spot.FRAME;
import static fiji.plugin.trackmate.Spot.POSITION_X;
import static fiji.plugin.trackmate.Spot.POSITION_Y;
import static fiji.plugin.trackmate.Spot.POSITION_Z;
import static fiji.plugin.trackmate.Spot.RADIUS;
import static fiji.plugin.trackmate.io.TmXmlKeys.PLUGIN_VERSION_ATTRIBUTE_NAME;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import org.apache.commons.io.FileUtils;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.mastodon.geff.GeffAxis;
import org.mastodon.geff.GeffMetadata;
import org.mastodon.geff.GeffMetadata.GeffDisplayHints;
import org.mastodon.geff.GeffMetadata.MeshMetadata;
import org.mastodon.geff.PropMetadata;
import org.mastodon.geff.imglib2.ElementType;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeDoubleArrayFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeDoubleFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeFloatArrayFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeIntFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeLongArrayFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeStringFunction;
import org.mastodon.geff.imglib2.GeffException;
import org.mastodon.geff.imglib2.GeffProperties;
import org.mastodon.geff.imglib2.GeffProperty;
import org.mastodon.geff.imglib2.GeffPropertyWriter;
import org.mastodon.geff.imglib2.IoUtils;
import org.mastodon.geff.imglib2.Maybe;
import org.mastodon.geff.imglib2.Suppliers;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.GuiModel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.io.geff.GeffTestBase;
import fiji.plugin.trackmate.io.geff.TmGeffIODemo;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;

/**
 * A demo that serializes the {@link SpotCollection} of a TrackMate model into
 * a GEFF file, using the imglib2-based writing API of the geff-java library
 * ({@link GeffPropertyWriter}).
 * <p>
 * Spots are serialized with a dispatch over the 3 implementations of
 * {@link Spot}:
 * <ul>
 * <li>{@link fiji.plugin.trackmate.SpotBase}: 2D or 3D spot defined by only a
 * radius;</li>
 * <li>{@link SpotRoi}: 2D spot defined by a closed polygon, written as one
 * var-length property {@code polygon}, with the XY coordinates of the polygon
 * vertices interleaved, relative to the spot center, as mandated by the GEFF
 * specification;</li>
 * <li>{@link SpotMesh}: 3D spot defined by a triangular mesh, written as three
 * var-length properties {@code mesh_vertices} (vertices positions),
 * {@code mesh_vertex_normals} (vertices normals) and {@code mesh_triangles}
 * (triangle indices).</li>
 * </ul>
 * Note that only the property datasets are written by the
 * {@link GeffPropertyWriter}; the GEFF metadata is written separately with
 * {@link GeffMetadata#writeToZarr(GeffMetadata, String)}.
 */
public class TrackMateImglibGeffWriterDemo extends GeffTestBase
{

	/**
	 * The chunk size to use when writing the GEFF file, as in
	 * {@link fiji.plugin.trackmate.io.geff.TmGeffWriter}.
	 */
	private static final int CHUNK_SIZE = 8 * 1024; // 8 kiB;

	/** Features not in the general props, because they are the core node. */
	private static final Set< String > SKIP_PROPS = Set.of( FRAME, POSITION_X, POSITION_Y, POSITION_Z, RADIUS );

	private static final String TRACKMATE_SPOT_ID_PROP = "trackmate_spot_id";
	private static final String NAME_PROP = "name";
	private static final String POLYGON_PROP = "polygon";
	private static final String MESH_V_PROP = "mesh_vertices";
	private static final String MESH_VN_PROP = "mesh_vertex_normals";
	private static final String MESH_F_PROP = "mesh_triangles";

	public static void main( final String[] args ) throws IOException, GeffException
	{
//		final String path = "samples/FakeTracks.xml";
//		final String path = GeffTestDeserializationTest.SPOT_MIXED_PATH;
		final String path = "samples/CElegans3D-smoothed-mask-orig.xml";

		System.out.println( "Reading from " + path );
		final GuiModel gm = TmGeffIODemo.loadFromXML( path );
		if ( null == gm )
			return;
		System.out.println( "Done. " + gm.getModel().getSpots().getNSpots( false ) + " spots." );

		final String savePath = path.replace( ".xml", ".geff" );
		System.out.println( "Deleting previous GEFF " + savePath + " if it exists." );
		FileUtils.deleteDirectory( new File( savePath ) );

		System.out.println( "Writing spots to " + savePath );
		writeToGeff( gm.getModel(), savePath );
		System.out.println( "Done." );

		System.out.println( "Reading back " + savePath );
		printSummary( savePath, 5 );
		System.out.println( "Done." );

		/*
		 * The GUI classes we touch (through the model readers) spawn non-daemon
		 * AWT threads that would prevent the JVM from exiting. Kill it
		 * explicitly.
		 */
		System.exit( 0 );
	}

	/**
	 * Serializes the spots of the specified model to the GEFF file at the
	 * specified path.
	 *
	 * @param model
	 *            the model whose {@link SpotCollection} to serialize.
	 * @param path
	 *            the path to the GEFF file (a Zarr container).
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	public static void writeToGeff( final Model model, final String path ) throws IOException
	{
		final SpotCollection spots = model.getSpots();
		final FeatureModel fm = model.getFeatureModel();
		final boolean is2D = DetectionUtils.is2D( model );

		/*
		 * Collect spots in a stable order, and build the map Spot -> GEFF id.
		 */
		final List< Spot > spotList = new ArrayList<>();
		spots.iterable( false ).forEach( spotList::add );
		final TObjectIntMap< Spot > spotToId = new TObjectIntHashMap<>();
		for ( int i = 0; i < spotList.size(); i++ )
			spotToId.put( spotList.get( i ), i );

		/*
		 * Determine the spot features that have an actual value in at least one
		 * spot.
		 */
		final Set< String > featureKeys = new LinkedHashSet<>();
		for ( final Spot spot : spotList )
			featureKeys.addAll( spot.getFeatures().keySet() );

		/*
		 * Detect the spot shapes present in the collection.
		 */
		boolean hasRoi = false;
		boolean hasMesh = false;
		for ( final Spot spot : spotList )
		{
			hasRoi |= spot instanceof SpotRoi;
			hasMesh |= spot instanceof SpotMesh;
		}

		try (final N5Writer n5 = new N5ZarrWriter( path ))
		{
			final GeffPropertyWriter< Spot > writer = new GeffPropertyWriter<>( spotList, ElementType.NODE );

			// Ids: sequential, in iteration order.
			final ToLongFunction< Spot > idAccessor = spot -> ( long ) spotToId.get( spot );
			writer.id( idAccessor, "<u8" );

			// Time. Note: int accessors are harvested as unsigned-int columns in
			// geff-java, so we must NOT force a "<i4" typestr; the dataset will
			// be uint32, and the PropMetadata must say "uint32" as well.
			final ToIntFunction< Spot > tAccessor = spot -> ( int ) spot.getFeature( FRAME ).doubleValue();
			writer.add( "t", tAccessor );

			// Position.
			final ToDoubleFunction< Spot > xAccessor = spot -> spot.getDoublePosition( 0 );
			writer.add( POSITION_X, xAccessor );
			final ToDoubleFunction< Spot > yAccessor = spot -> spot.getDoublePosition( 1 );
			writer.add( POSITION_Y, yAccessor );
			if ( !is2D )
			{
				final ToDoubleFunction< Spot > zAccessor = spot -> spot.getDoublePosition( 2 );
				writer.add( POSITION_Z, zAccessor );
			}

			// Radius, possibly missing.
			final ToMaybeDoubleFunction< Spot > radiusAccessor = spot ->
			{
				final Double r = spot.getFeature( RADIUS );
				return ( r == null ) ? new Maybe.MaybeDouble() : new Maybe.MaybeDouble( r.doubleValue() );
			};
			writer.add( "radius", radiusAccessor );

			// TrackMate ID and name.
			final ToIntFunction< Spot > tmIdAccessor = Spot::ID;
			writer.add( TRACKMATE_SPOT_ID_PROP, tmIdAccessor );
			final ToMaybeStringFunction< Spot > nameAccessor = spot -> spot.hasName()
					? new Maybe.MaybeString( spot.getName() )
					: new Maybe.MaybeString();
			writer.add( NAME_PROP, nameAccessor );

			// Other spot features, possibly missing per spot.
			for ( final String feature : featureKeys )
			{
				if ( SKIP_PROPS.contains( feature ) )
					continue;
				final boolean isInt = fm.getSpotFeatureIsInt().getOrDefault( feature, Boolean.FALSE ).booleanValue();
				if ( isInt )
				{
					final ToMaybeIntFunction< Spot > accessor = spot ->
					{
						final Double v = spot.getFeature( feature );
						return ( v == null ) ? new Maybe.MaybeInt() : new Maybe.MaybeInt( v.intValue() );
					};
					writer.add( feature, accessor );
				}
				else
				{
					final ToMaybeDoubleFunction< Spot > accessor = spot ->
					{
						final Double v = spot.getFeature( feature );
						return ( v == null ) ? new Maybe.MaybeDouble() : new Maybe.MaybeDouble( v.doubleValue() );
					};
					writer.add( feature, accessor );
				}
			}

			/*
			 * SpotRoi: polygon vertices, relative to the spot center. The GEFF
			 * specification stores the polygon as a single property with the
			 * coordinates of the vertices interleaved, one XY pair per point.
			 */
			if ( hasRoi )
			{
				final ToMaybeDoubleArrayFunction< Spot > polygonAccessor = spot ->
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
				};
				writer.add( POLYGON_PROP, polygonAccessor );
			}

			/*
			 * SpotMesh: vertices positions and normals, triangle indices, as
			 * mandated by the GEFF specification.
			 */
			if ( hasMesh )
			{
				final ToMaybeFloatArrayFunction< Spot > vAccessor = spot ->
				{
					if ( spot instanceof SpotMesh )
						return new Maybe.MaybeFloatArray( toArray( ( ( SpotMesh ) spot ).getMesh().vertices().verts() ) );
					return new Maybe.MaybeFloatArray();
				};
				writer.add( MESH_V_PROP, vAccessor );
				final ToMaybeFloatArrayFunction< Spot > vnAccessor = spot ->
				{
					if ( spot instanceof SpotMesh )
						return new Maybe.MaybeFloatArray( toArray( ( ( SpotMesh ) spot ).getMesh().vertices().normals() ) );
					return new Maybe.MaybeFloatArray();
				};
				writer.add( MESH_VN_PROP, vnAccessor );
				// Triangle indices. The GEFF spec example shows them as int64,
				// but the writer API can only emit uint64 for a long accessor
				// (the N5 writer is strict about signedness), so we use the
				// library default. Indices are non-negative, so this is
				// lossless.
				final ToMaybeLongArrayFunction< Spot > fAccessor = spot ->
				{
					if ( spot instanceof SpotMesh )
						return new Maybe.MaybeLongArray( toLongArray( ( ( SpotMesh ) spot ).getMesh().triangles().indices() ) );
					return new Maybe.MaybeLongArray();
				};
				writer.add( MESH_F_PROP, fAccessor );
			}

			writer.write( n5, new RawCompression(), CHUNK_SIZE );
		}

		/*
		 * Metadata: only property datasets are written by the
		 * GeffPropertyWriter; we have to write the GEFF metadata ourselves.
		 */
		final GeffMetadata metadata = buildMetadata( model, is2D, featureKeys, hasRoi, hasMesh );
		GeffMetadata.writeToZarr( metadata, path );
	}

	private static GeffMetadata buildMetadata(
			final Model model,
			final boolean is2D,
			final Set< String > featureKeys,
			final boolean hasRoi,
			final boolean hasMesh )
	{
		final FeatureModel fm = model.getFeatureModel();
		final String spaceUnits = model.getSpaceUnits();
		final String timeUnits = model.getTimeUnits();

		final GeffMetadata metadata = new GeffMetadata( "1.3.1", true );

		// Axes.
		final List< GeffAxis > axes = new ArrayList<>();
		final double[] minMaxT = FeatureUtils.autoMinMax( model, TrackMateObject.SPOTS, FRAME, false );
		axes.add( GeffAxis.createTimeAxis( "t", timeUnits, minMaxT[ 0 ], minMaxT[ 1 ] ) );
		for ( final String spaceFeature : is2D
				? new String[] { POSITION_X, POSITION_Y }
				: new String[] { POSITION_X, POSITION_Y, POSITION_Z } )
		{
			final double[] minMax = FeatureUtils.autoMinMax( model, TrackMateObject.SPOTS, spaceFeature, false );
			axes.add( GeffAxis.createSpaceAxis( spaceFeature, spaceUnits, minMax[ 0 ], minMax[ 1 ] ) );
		}
		metadata.setGeffAxes( axes );

		// Display hints.
		final GeffDisplayHints displayHints = new GeffDisplayHints();
		displayHints.setDisplayHorizontal( POSITION_X );
		displayHints.setDisplayVertical( POSITION_Y );
		displayHints.setDisplayTime( "t" );
		if ( !is2D )
			displayHints.setDisplayDepth( POSITION_Z );
		metadata.setDisplayHints( displayHints );

		/*
		 * Node properties metadata.
		 */
		final Map< String, Boolean > isInt = fm.getSpotFeatureIsInt();
		final Map< String, PropMetadata > nodePropsMetadata = new LinkedHashMap<>();

		nodePropsMetadata.put( "t", new PropMetadata( "t", "uint32", false, timeUnits, Spot.FEATURE_NAMES.get( FRAME ), null ) );
		nodePropsMetadata.put( POSITION_X, new PropMetadata( POSITION_X, "float64", false, spaceUnits, Spot.FEATURE_NAMES.get( POSITION_X ), null ) );
		nodePropsMetadata.put( POSITION_Y, new PropMetadata( POSITION_Y, "float64", false, spaceUnits, Spot.FEATURE_NAMES.get( POSITION_Y ), null ) );
		if ( !is2D )
			nodePropsMetadata.put( POSITION_Z, new PropMetadata( POSITION_Z, "float64", false, spaceUnits, Spot.FEATURE_NAMES.get( POSITION_Z ), null ) );
		nodePropsMetadata.put( "radius", new PropMetadata( "radius", "float64", false, spaceUnits, Spot.FEATURE_NAMES.get( RADIUS ), "The radius of the spot" ) );
		nodePropsMetadata.put( TRACKMATE_SPOT_ID_PROP, new PropMetadata( TRACKMATE_SPOT_ID_PROP, "uint32", false, null, "TrackMate ID", "The TrackMate internal ID of spots" ) );
		nodePropsMetadata.put( NAME_PROP, new PropMetadata( NAME_PROP, "uint8", true, null, "Spot name", "The name of the spot" ) );

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
		metadata.setNodePropsMetadata( nodePropsMetadata );

		/*
		 * Shape metadata.
		 */

		// Spheres.
		metadata.setSphere( "radius" );
		// Polygons.
		if ( hasRoi )
		{
			nodePropsMetadata.put( POLYGON_PROP, new PropMetadata( POLYGON_PROP, "float64", true, spaceUnits, "Polygon", "Interleaved XY coordinates of the polygon vertices, relative to the spot center" ) );
		}
		// Meshes.
		if ( hasMesh )
		{
			nodePropsMetadata.put( MESH_V_PROP, new PropMetadata( MESH_V_PROP, "float32", true, spaceUnits, "Mesh vertices", "Interleaved XYZ coordinates of the mesh vertices, relative to the spot center" ) );
			nodePropsMetadata.put( MESH_VN_PROP, new PropMetadata( MESH_VN_PROP, "float32", true, null, "Mesh vertex normals", "Interleaved XYZ coordinates of the mesh vertex normals" ) );
			nodePropsMetadata.put( MESH_F_PROP, new PropMetadata( MESH_F_PROP, "uint64", true, null, "Mesh triangles", "Flattened indices of the mesh triangles" ) );
			// Shape declarations: declare which node property holds the shapes,
			// as mandated by the GEFF specification.
			final MeshMetadata mesh = new MeshMetadata( MESH_V_PROP, MESH_F_PROP, MESH_VN_PROP, null );
			metadata.setMesh( mesh );
		}

		// Extra.
		final Map< String, Object > trackmateInfo = new LinkedHashMap<>();
		trackmateInfo.put( PLUGIN_VERSION_ATTRIBUTE_NAME, TrackMate.PLUGIN_NAME_VERSION );
		metadata.setExtra( Map.of( "trackmate", trackmateInfo ) );

		return metadata;
	}

	/**
	 * Reads back the GEFF file at the specified path and prints a short
	 * summary: the list of properties, and the first spots.
	 *
	 * @param path
	 *            the path to the GEFF file.
	 * @param nExamples
	 *            the number of spots to print.
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	@SuppressWarnings( "unchecked" )
	public static void printSummary( final String path, final int nExamples ) throws IOException, GeffException
	{
		final GeffMetadata metadata = GeffMetadata.readFromZarr( path );
		System.out.println( "Metadata: geff_version = " + metadata.getGeffVersion()
				+ ", directed = " + metadata.isDirected()
				+ ", axes = " + Arrays.toString( metadata.getAxisNames() ) );

		try (final N5Reader n5 = new N5ZarrReader( path ))
		{
			final GeffProperties props = IoUtils.loadProperties( n5, ElementType.NODE );
			System.out.println( props );
			System.out.println( props.numElements() + " elements." );

			final LongSupplier id = Suppliers.asLongSupplier( ( GeffProperty< UnsignedLongType > ) props.id() );
			final DoubleSupplier x = Suppliers.asDoubleSupplier( props.properties().get( POSITION_X ) );
			final DoubleSupplier y = Suppliers.asDoubleSupplier( props.properties().get( POSITION_Y ) );
			final IntSupplier t = Suppliers.asIntSupplier( ( GeffProperty< UnsignedIntType > ) props.properties().get( "t" ) );
			final Supplier< Maybe.MaybeString > name = Suppliers.asMaybeStringSupplier( props.properties().get( NAME_PROP ) );

			final int n = ( int ) Math.min( nExamples, props.numElements() );
			for ( int i = 0; i < n; i++ )
			{
				props.elementIndex().set( i );
				final Maybe.MaybeString nameOfSpot = name.get();
				System.out.println( "id=" + id.getAsLong()
						+ ", x=" + x.getAsDouble()
						+ ", y=" + y.getAsDouble()
						+ ", t=" + t.getAsInt()
						+ ", name=" + ( nameOfSpot.isPresent() ? nameOfSpot.get() : "<none>" ) );
			}
		}
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
