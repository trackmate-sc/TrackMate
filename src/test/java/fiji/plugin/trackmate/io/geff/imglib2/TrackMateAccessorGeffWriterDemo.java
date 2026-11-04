package fiji.plugin.trackmate.io.geff.imglib2;

import static fiji.plugin.trackmate.Spot.FRAME;
import static fiji.plugin.trackmate.Spot.RADIUS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToLongFunction;

import org.apache.commons.io.FileUtils;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.mastodon.geff.GeffMetadata;
import org.mastodon.geff.imglib2.ElementType;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeDoubleFunction;
import org.mastodon.geff.imglib2.FunctionTypes.ToMaybeIntFunction;
import org.mastodon.geff.imglib2.GeffException;
import org.mastodon.geff.imglib2.GeffPropertyWriter;
import org.mastodon.geff.imglib2.Maybe;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.gui.GuiModel;
import fiji.plugin.trackmate.io.geff.GeffTestBase;
import fiji.plugin.trackmate.io.geff.TmGeffIODemo;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * Serializes the {@link SpotCollection} of a TrackMate model into a GEFF file,
 * using the imglib2-based writing API from Tobias in the geff-java library
 * ({@link GeffPropertyWriter})
 */
public class TrackMateAccessorGeffWriterDemo extends GeffTestBase
{

	private static final int CHUNK_SIZE = 8 * 1024; // 8 kiB;

	public static void main( final String[] args ) throws IOException, GeffException
	{
//		final String path = "samples/FakeTracks.xml";
		final String path = "samples/CElegans3D-smoothed-mask-orig.xml";

		System.out.println( "Reading from " + path );
		final GuiModel gm = TmGeffIODemo.loadFromXML( path );
		if ( null == gm )
			return;
		System.out.println( "Done. " + gm.getModel().getSpots().getNSpots( false ) + " spots." );

		final String savePath = path.replace( ".xml", "-accessor.geff" );
		System.out.println( "Deleting previous GEFF " + savePath + " if it exists." );
		FileUtils.deleteDirectory( new File( savePath ) );

		System.out.println( "Writing spots to " + savePath );
		writeToGeff( gm.getModel(), savePath );
		System.out.println( "Done." );

		System.out.println( "Reading back " + savePath );
		GeffWriterDemos.printSummary( savePath, 5 );
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

		/*
		 * Write the spots with the GeffPropertyWriter, following the pattern
		 * of its Javadoc: one accessor per property, registered as method
		 * references on the static accessors of this class.
		 */
		try (final N5Writer n5 = new N5ZarrWriter( path ))
		{
			final GeffPropertyWriter< Spot > writer = new GeffPropertyWriter<>( spotList, ElementType.NODE );

			// Ids: sequential, in iteration order. There is no sequential id
			// on the Spot interface, so we use a lambda over a map for this
			// one accessor.
			final ToLongFunction< Spot > idAccessor = spot -> spotToId.get( spot );
			writer.id( idAccessor, "<u8" );

			// Time and position. Space and time properties use the GEFF
			// conventional names 't', 'x', 'y', 'z' (not the TrackMate
			// feature keys).
			writer.add( "t", TrackMateAccessorGeffWriterDemo::t );
			writer.add( "x", TrackMateAccessorGeffWriterDemo::x );
			writer.add( "y", TrackMateAccessorGeffWriterDemo::y );
			if ( !is2D )
				writer.add( "z", TrackMateAccessorGeffWriterDemo::z );

			// Radius, possibly missing.
			writer.add( "radius", TrackMateAccessorGeffWriterDemo::radius );

			// TrackMate ID and name.
			writer.add( GeffWriterDemos.TRACKMATE_SPOT_ID_PROP, Spot::ID );
			writer.add( GeffWriterDemos.NAME_PROP, TrackMateAccessorGeffWriterDemo::name );

			// Other spot features, possibly missing per spot. These accessors
			// are lambdas capturing the feature key (method references are not
			// possible for a dynamic key), and must be explicitly typed, as
			// raw lambdas are ambiguous against the many add() overloads.
			for ( final String feature : featureKeys )
			{
				if ( GeffWriterDemos.SKIP_PROPS.contains( feature ) )
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
				writer.add( GeffWriterDemos.POLYGON_PROP, TrackMateAccessorGeffWriterDemo::polygon );

			// SpotMesh: vertices positions and normals, triangle indices.
			if ( hasMesh )
			{
				writer.add( GeffWriterDemos.MESH_V_PROP, TrackMateAccessorGeffWriterDemo::meshVertices );
				writer.add( GeffWriterDemos.MESH_VN_PROP, TrackMateAccessorGeffWriterDemo::meshNormals );
				writer.add( GeffWriterDemos.MESH_F_PROP, TrackMateAccessorGeffWriterDemo::meshTriangles );
			}

			writer.write( n5, new RawCompression(), CHUNK_SIZE );
		}

		/*
		 * Metadata: only property datasets are written by the
		 * GeffPropertyWriter; we have to write the GEFF metadata ourselves.
		 */
		final GeffMetadata metadata = GeffWriterDemos.buildMetadata( model, is2D, featureKeys, hasRoi, hasMesh );
		GeffMetadata.writeToZarr( metadata, path );
	}

	/*
	 * Static accessors, used as method references by the writer. The ones that
	 * depend on the concrete spot class dispatch with instanceof.
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
			return new Maybe.MaybeFloatArray( GeffWriterDemos.toArray( ( ( SpotMesh ) spot ).getMesh().vertices().verts() ) );
		return new Maybe.MaybeFloatArray();
	}

	/**
	 * Returns the interleaved XYZ coordinates of the mesh vertex normals of
	 * the specified spot. Empty if the spot is not a {@link SpotMesh}.
	 */
	private static Maybe.MaybeFloatArray meshNormals( final Spot spot )
	{
		if ( spot instanceof SpotMesh )
			return new Maybe.MaybeFloatArray( GeffWriterDemos.toArray( ( ( SpotMesh ) spot ).getMesh().vertices().normals() ) );
		return new Maybe.MaybeFloatArray();
	}

	/**
	 * Returns the flattened indices of the mesh triangles of the specified
	 * spot. Empty if the spot is not a {@link SpotMesh}.
	 */
	private static Maybe.MaybeLongArray meshTriangles( final Spot spot )
	{
		if ( spot instanceof SpotMesh )
			return new Maybe.MaybeLongArray( GeffWriterDemos.toLongArray( ( ( SpotMesh ) spot ).getMesh().triangles().indices() ) );
		return new Maybe.MaybeLongArray();
	}
}
