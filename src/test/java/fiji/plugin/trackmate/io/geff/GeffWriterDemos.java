package fiji.plugin.trackmate.io.geff;

import static fiji.plugin.trackmate.Spot.FRAME;
import static fiji.plugin.trackmate.Spot.POSITION_X;
import static fiji.plugin.trackmate.Spot.POSITION_Y;
import static fiji.plugin.trackmate.Spot.POSITION_Z;
import static fiji.plugin.trackmate.Spot.RADIUS;
import static fiji.plugin.trackmate.features.track.TrackIndexAnalyzer.TRACK_ID;
import static fiji.plugin.trackmate.io.TmXmlKeys.PLUGIN_VERSION_ATTRIBUTE_NAME;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.mastodon.geff.GeffAxis;
import org.mastodon.geff.GeffMetadata;
import org.mastodon.geff.GeffMetadata.GeffDisplayHints;
import org.mastodon.geff.GeffMetadata.MeshMetadata;
import org.mastodon.geff.PropMetadata;
import org.mastodon.geff.imglib2.ElementType;
import org.mastodon.geff.imglib2.GeffException;
import org.mastodon.geff.imglib2.GeffProperties;
import org.mastodon.geff.imglib2.GeffProperty;
import org.mastodon.geff.imglib2.IoUtils;
import org.mastodon.geff.imglib2.Maybe;
import org.mastodon.geff.imglib2.Suppliers;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;

/**
 * Shared code for the TrackMate GEFF writing demos: the GEFF metadata builder
 * and the skip-set for {@link TrackMateImglibGeffWriterDemo}, and the
 * read-back summary used by all the demos. ({@link TrackMateAccessorGeffWriterDemo}
 * now delegates to {@link fiji.plugin.trackmate.io.geff.TmGeffWriter},
 * which has its own copy of the registration logic in main sources.)
 */
final class GeffWriterDemos
{

	/**
	 * TrackMate spot features that are not re-emitted as generic node
	 * properties, because they are the core GEFF node properties — written
	 * under the GEFF conventional names {@code t}, {@code x}, {@code y},
	 * {@code z} — or are represented by the shape declaration
	 * ({@code sphere} = the {@code radius} property). The TRACK_ID feature is
	 * skipped as well: tracks belong to the {@code track_node_props}
	 * metadata, which the demos do not write.
	 */
	static final Set< String > SKIP_PROPS = Set.of( FRAME, POSITION_X, POSITION_Y, POSITION_Z, RADIUS, TRACK_ID );

	static final String TRACKMATE_SPOT_ID_PROP = "trackmate_spot_id";
	static final String NAME_PROP = "name";
	static final String POLYGON_PROP = "polygon";
	static final String MESH_V_PROP = "mesh_vertices";
	static final String MESH_VN_PROP = "mesh_vertex_normals";
	static final String MESH_F_PROP = "mesh_triangles";

	private GeffWriterDemos()
	{}

	/**
	 * Builds the GEFF metadata matching the node properties written by the
	 * demos: axes and display hints over the core {@code t}/{@code x}/{@code
	 * y}/{@code z} properties, {@code node_props_metadata} for every written
	 * property, the shape declarations ({@code sphere}, {@code mesh}), and an
	 * {@code extra.trackmate} map with the plugin version.
	 *
	 * @param model
	 *            the model the properties come from (for feature units,
	 *            names, and auto min/max of the axes).
	 * @param is2D
	 *            whether the model is 2D (no {@code z} axis).
	 * @param featureKeys
	 *            the spot features present somewhere in the collection, that
	 *            are not in {@link #SKIP_PROPS}.
	 * @param hasRoi
	 *            whether the collection contains {@link SpotRoi} spots (the
	 *            {@code polygon} property is written).
	 * @param hasMesh
	 *            whether the collection contains {@link SpotMesh} spots (the
	 *            mesh properties are written).
	 * @return the {@link GeffMetadata} to write to the GEFF file root.
	 */
	static GeffMetadata buildMetadata(
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
			metadata.setMesh( new MeshMetadata( MESH_V_PROP, MESH_F_PROP, MESH_VN_PROP, null ) );

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
	static void printSummary( final String path, final int nExamples ) throws IOException, GeffException
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
			final DoubleSupplier x = Suppliers.asDoubleSupplier( props.properties().get( "x" ) );
			final DoubleSupplier y = Suppliers.asDoubleSupplier( props.properties().get( "y" ) );
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

	static long[] toLongArray( final IntBuffer b )
	{
		final int n = b.limit();
		final long[] out = new long[ n ];
		for ( int i = 0; i < n; i++ )
			out[ i ] = b.get( i );
		return out;
	}

	static float[] toArray( final FloatBuffer b )
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
