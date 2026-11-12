package fiji.plugin.trackmate.io.geff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;
import org.junit.Test;
import org.litt.geff.GeffAxis;
import org.litt.geff.GeffDisplayHints;
import org.litt.geff.GeffMetadata;
import org.litt.geff.PropMetadata;
import org.litt.geff.io.GeffPropertyWriter;
import org.litt.geff.io.IoUtils;
import org.litt.geff.props.ElementType;
import org.litt.geff.props.FunctionTypes;
import org.litt.geff.props.GeffProperties;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;

/**
 * Tests that the {@link TmGeffReader} can open foreign GEFF files (files not
 * written by TrackMate) by mapping their property names to the
 * TrackMate-canonical ones using the display hints and heuristics, and by
 * synthesizing the TrackMate-specific properties that are missing.
 */
public class GeffTestForeignFilesTest extends GeffTestBase
{

	/**
	 * A record mimicking a foreign dataset: non-canonical property names, no
	 * TrackMate-specific properties (no radius, no spot ID, no name).
	 */
	record ForeignSpot( double positionX, double positionY, double positionZ, int time )
	{}

	/**
	 * A record mimicking an edge of a foreign dataset, referencing the node
	 * ids of the file.
	 */
	record ForeignEdge( int source, int target )
	{}

	private static final List< ForeignSpot > SPOTS = List.of(
			new ForeignSpot( 1., 2., 0.5, 0 ),
			new ForeignSpot( 3., 4., 1.5, 0 ),
			new ForeignSpot( 5., 6., 2.5, 1 ),
			new ForeignSpot( 7., 8., 3.5, 1 ),
			new ForeignSpot( 9., 10., 4.5, 2 ) );

	/**
	 * Writes a foreign GEFF file with properties named {@code POSITION_X/Y/Z}
	 * and {@code time}, optionally with display hints and without edges.
	 */
	private static Path writeForeignFile( final Path directory, final GeffDisplayHints hints ) throws IOException
	{
		return writeForeignFile( directory, hints, List.of() );
	}

	/**
	 * Writes a foreign GEFF file with properties named {@code POSITION_X/Y/Z}
	 * and {@code time}, optionally with display hints and with the given
	 * edges, referencing the node ids of the file.
	 */
	private static Path writeForeignFile( final Path directory, final GeffDisplayHints hints, final List< ForeignEdge > edges ) throws IOException
	{
		final String zarrPath = directory.resolve( "foreign.geff" ).toString();

		final long[] counter = { 0 };
		final GeffProperties props = new GeffPropertyWriter<>( SPOTS, ElementType.NODE )
				.id( ( ToLongFunction< ForeignSpot > ) o -> counter[ 0 ]++, "<u8" )
				.add( "POSITION_X", ( ToDoubleFunction< ForeignSpot > ) ForeignSpot::positionX )
				.add( "POSITION_Y", ( ToDoubleFunction< ForeignSpot > ) ForeignSpot::positionY )
				.add( "POSITION_Z", ( ToDoubleFunction< ForeignSpot > ) ForeignSpot::positionZ )
				.add( "time", ( ToIntFunction< ForeignSpot > ) ForeignSpot::time )
				.createGeffProperties();

		final GeffProperties edgeProps = ( edges.isEmpty() )
				? null
				: new GeffPropertyWriter<>( edges, ElementType.EDGE )
						.id( ( FunctionTypes.ToLongArrayFunction< ForeignEdge > ) e -> new long[] { e.source(), e.target() }, 2, "<u8" )
						.createGeffProperties();

		try (final N5ZarrWriter n5 = new N5ZarrWriter( zarrPath ))
		{
			IoUtils.writeProperties( n5, props, null, 0, null );
			if ( edgeProps != null )
				IoUtils.writeProperties( n5, edgeProps, null, 0, null );

			final GeffMetadata metadata = new GeffMetadata( "1.3", true );
			// IoUtils.writeProperties() writes only the datasets. The
			// enumeration of properties for the reader lives in the
			// node_props_metadata attribute of the metadata, that we must
			// populate explicitly.
			final Map< String, PropMetadata > nodePropsMetadata = new LinkedHashMap<>();
			nodePropsMetadata.put( "POSITION_X", new PropMetadata( "POSITION_X", "float64", false, "micrometer", null, null ) );
			nodePropsMetadata.put( "POSITION_Y", new PropMetadata( "POSITION_Y", "float64", false, "micrometer", null, null ) );
			nodePropsMetadata.put( "POSITION_Z", new PropMetadata( "POSITION_Z", "float64", false, "micrometer", null, null ) );
			nodePropsMetadata.put( "time", new PropMetadata( "time", "int32", false, "second", null, null ) );
			metadata.setNodePropsMetadata( nodePropsMetadata );
			metadata.setGeffAxes( List.of(
					GeffAxis.createTimeAxis( "time", "second", Double.valueOf( 0 ), Double.valueOf( 2 ) ),
					GeffAxis.createSpaceAxis( "POSITION_X", "micrometer", Double.valueOf( 0 ), Double.valueOf( 9 ) ),
					GeffAxis.createSpaceAxis( "POSITION_Y", "micrometer", Double.valueOf( 1 ), Double.valueOf( 10 ) ),
					GeffAxis.createSpaceAxis( "POSITION_Z", "micrometer", Double.valueOf( 0 ), Double.valueOf( 5 ) ) ) );
			if ( hints != null )
				metadata.setDisplayHints( hints );
			GeffMetadata.writeToZarr( metadata, zarrPath );
		}
		return Path.of( zarrPath );
	}

	/**
	 * The spots of the model match the original records, have the default
	 * radius and the default names, and carry fresh unique IDs.
	 */
	private void checkModel( final Model model )
	{
		final SpotCollection spots = model.getSpots();
		assertEquals( SPOTS.size(), spots.getNSpots( false ) );

		final List< Boolean > matched = new ArrayList<>( SPOTS.size() );
		for ( int i = 0; i < SPOTS.size(); i++ )
			matched.add( Boolean.FALSE );

		final List< Long > ids = new ArrayList<>();
		for ( final Spot spot : spots.iterable( false ) )
		{
			final double x = spot.getDoublePosition( 0 );
			final double y = spot.getDoublePosition( 1 );
			final double z = spot.getDoublePosition( 2 );
			final int t = spot.getFeature( Spot.FRAME ).intValue();

			int found = -1;
			for ( int i = 0; i < SPOTS.size(); i++ )
			{
				if ( matched.get( i ).booleanValue() )
					continue;
				final ForeignSpot fs = SPOTS.get( i );
				if ( fs.positionX() == x && fs.positionY() == y && fs.positionZ() == z && fs.time() == t )
				{
					found = i;
					break;
				}
			}
			if ( found < 0 )
				throw new AssertionError( "No expected spot at (" + x + "," + y + "," + z + ") frame " + t );
			matched.set( found, Boolean.TRUE );

			// Radius defaults to 5.0 for foreign files without any
			// radius-like property.
			assertEquals( Double.valueOf( 5. ), spot.getFeature( Spot.RADIUS ) );

			// Names default to "ID<id>" for foreign files without a name
			// property.
			assertEquals( "ID" + spot.ID(), spot.getName() );

			ids.add( Long.valueOf( spot.ID() ) );
		}

		// Fresh unique IDs.
		assertEquals( SPOTS.size(), ids.stream().distinct().count() );
	}

	@Test
	public void testForeignFileWithDisplayHints() throws IOException, org.litt.geff.GeffException
	{
		final Path dir = Files.createTempDirectory( "GeffTestForeignFilesTest" );
		final GeffDisplayHints hints = new GeffDisplayHints();
		hints.setDisplayHorizontal( "POSITION_X" );
		hints.setDisplayVertical( "POSITION_Y" );
		hints.setDisplayDepth( "POSITION_Z" );
		hints.setDisplayTime( "time" );
		final Path file = writeForeignFile( dir, hints );

		final TmGeffReader reader = new TmGeffReader( file.toString() );
		final Model model = reader.getModel();
		checkModel( model );
	}

	@Test
	public void testForeignFileWithHeuristics() throws IOException, org.litt.geff.GeffException
	{
		// No display hints: the axis names must be identified by the
		// heuristics.
		final Path dir = Files.createTempDirectory( "GeffTestForeignFilesTest" );
		final Path file = writeForeignFile( dir, null );

		final TmGeffReader reader = new TmGeffReader( file.toString() );
		final Model model = reader.getModel();
		checkModel( model );
	}

	/**
	 * A foreign file with edges but without the tracks sub-file: the tracks
	 * must be reconstructed from the graph connectivity, as connected
	 * components of the edge graph, with fresh track ids, default visibility
	 * and default names.
	 */
	@Test
	public void testForeignFileWithEdges() throws IOException, org.litt.geff.GeffException
	{
		// Two connected components: {0, 1, 2} and {3, 4}.
		final List< ForeignEdge > edges = List.of(
				new ForeignEdge( 0, 1 ),
				new ForeignEdge( 1, 2 ),
				new ForeignEdge( 3, 4 ) );
		final Path dir = Files.createTempDirectory( "GeffTestForeignFilesTest" );
		final Path file = writeForeignFile( dir, null, edges );

		final TmGeffReader reader = new TmGeffReader( file.toString() );
		final Model model = reader.getModel();
		checkModel( model );

		final TrackModel trackModel = model.getTrackModel();
		assertEquals( 2, trackModel.nTracks( false ) );

		final Set< Set< ForeignSpot > > groups = new HashSet<>();
		for ( final Integer trackID : trackModel.trackIDs( false ) )
		{
			// Default visibility and default names for foreign files without
			// the tracks sub-file.
			assertTrue( trackModel.isVisible( trackID ) );
			assertEquals( "Track_" + trackID, trackModel.name( trackID ) );

			// The track id itself is restored as the TRACK_ID feature.
			final Double trackIdFeature = model.getFeatureModel().getTrackFeature( trackID, TrackIndexAnalyzer.TRACK_ID );
			assertEquals( Integer.valueOf( trackID ), Integer.valueOf( trackIdFeature.intValue() ) );

			final Set< ForeignSpot > group = new HashSet<>();
			for ( final Spot spot : trackModel.trackSpots( trackID ) )
				group.add( new ForeignSpot(
						spot.getDoublePosition( 0 ),
						spot.getDoublePosition( 1 ),
						spot.getDoublePosition( 2 ),
						( int ) spot.getFeature( Spot.FRAME ).doubleValue() ) );
			groups.add( group );
		}

		// The components must match the edges: spots 0, 1, 2 together; spots
		// 3, 4 together.
		final Set< ForeignSpot > expected0 = Set.of( SPOTS.get( 0 ), SPOTS.get( 1 ), SPOTS.get( 2 ) );
		final Set< ForeignSpot > expected1 = Set.of( SPOTS.get( 3 ), SPOTS.get( 4 ) );
		assertEquals( Set.of( expected0, expected1 ), groups );
	}
}
