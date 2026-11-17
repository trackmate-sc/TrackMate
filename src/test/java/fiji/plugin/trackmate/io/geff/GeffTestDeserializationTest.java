package fiji.plugin.trackmate.io.geff;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.presentation.StandardRepresentation;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.features.track.TrackLocationAnalyzer;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.io.xml.TmXmlReader;

/**
 * Tests that check that a TrackMate model, settings etc, can be written and
 * read to / from a GEFF file, and that the resulting objects are identical.
 */
public class GeffTestDeserializationTest extends GeffTestBase
{

	/**
	 * A TrackMate XML file generated with TrackMate v8, containing only
	 * 'SpotBase' spots.
	 */
	static final String SPOT_BASE_PATH = GeffTestDeserializationTest.class
			.getResource( "FakeTracks.xml" )
			.getFile();

	/**
	 * A TrackMate XML file generated with TrackMate v8, containing only
	 * 'SpotRoi' spots.
	 */
	static final String SPOT_ROI_PATH = GeffTestDeserializationTest.class
			.getResource( "MAX_Merged.xml" )
			.getFile();

	/**
	 * A TrackMate XML file generated with TrackMate v8, containing 'SpotBase
	 * and 'SpotRoi' spots.
	 */
	public static final String SPOT_MIXED_PATH = GeffTestDeserializationTest.class
			.getResource( "MAX_Merged-mixed.xml" )
			.getFile();

	@Test
	public void testSpotBaseSerialization() throws Exception
	{
		testModelSerialization( SPOT_BASE_PATH );
	}

	@Test
	public void testSpotRoiSerialization() throws Exception
	{
		testModelSerialization( SPOT_ROI_PATH );
	}

	@Test
	public void testSpotMixedSerialization() throws Exception
	{
		testModelSerialization( SPOT_MIXED_PATH );
	}

	/**
	 * Tests that an empty model (no spots at all) can be saved to and read
	 * back from a GEFF file. The GEFF file then contains zero-length node
	 * datasets, which the reference python implementation also writes for
	 * empty graphs.
	 */
	@Test
	public void testEmptyModelSerialization() throws Exception
	{
		final Model emptyModel = new Model();
		emptyModel.setPhysicalUnits( "µm", "min" );

		final String geffFile = new File( temporaryFolder.getRoot(), "empty.geff" ).getAbsolutePath();
		final TmGeffWriter geffWriter = new TmGeffWriter( geffFile );
		geffWriter.appendModel( emptyModel );
		geffWriter.write();

		final TmGeffReader geffReader = new TmGeffReader( geffFile );
		final Model readBackModel = geffReader.getModel();
		assertThat( readBackModel )
				.as( "empty model is read back" )
				.isNotNull();
		assertThat( readBackModel.getSpots().iterable( false ) )
				.as( "no spots in the read-back model" )
				.isEmpty();
		assertThat( readBackModel.getTrackModel().nTracks( false ) )
				.as( "no tracks in the read-back model" )
				.isEqualTo( 0 );
	}

	/**
	 * Tests that re-saving over an existing GEFF file does not keep stale
	 * content: A GEFF file is a directory-based Zarr store, so a re-save
	 * merges with what is already there unless the store is deleted first.
	 * Here we save a model with a track, then save an empty model to the same
	 * path; the reloaded model must be empty, and in particular must not
	 * resurrect the edges of the first save.
	 */
	@Test
	public void testOverwriteErasesStaleContent() throws Exception
	{
		// A model with 2 spots joined by an edge, forming 1 track.
		final Model model = new Model();
		model.setPhysicalUnits( "µm", "min" );
		final Spot spot0 = new SpotBase( 10, 10, 0, 2., 1., "Spot_0" );
		final Spot spot1 = new SpotBase( 20, 10, 0, 2., 1., "Spot_1" );
		spot0.putFeature( Spot.FRAME, 0. );
		spot1.putFeature( Spot.FRAME, 1. );
		model.getSpots().add( spot0, 0 );
		model.getSpots().add( spot1, 1 );
		final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph = new SimpleWeightedGraph<>( DefaultWeightedEdge.class );
		graph.addVertex( spot0 );
		graph.addVertex( spot1 );
		final DefaultWeightedEdge edge = graph.addEdge( spot0, spot1 );
		model.getTrackModel().from(
				graph,
				Map.of( 0, Set.of( spot0, spot1 ) ),
				Map.of( 0, Set.of( edge ) ),
				Map.of( 0, true ),
				Map.of( 0, "Track_0" ) );
		// The track features the writer uses for the core track properties.
		final FeatureModel fm = model.getFeatureModel();
		fm.putTrackFeature( 0, TrackIndexAnalyzer.TRACK_ID, 0. );
		fm.putTrackFeature( 0, TrackDurationAnalyzer.TRACK_START, 0. );
		fm.putTrackFeature( 0, TrackLocationAnalyzer.X_LOCATION, 15. );
		fm.putTrackFeature( 0, TrackLocationAnalyzer.Y_LOCATION, 10. );

		final String geffFile = new File( temporaryFolder.getRoot(), "overwrite.geff" ).getAbsolutePath();
		final TmGeffWriter writer = new TmGeffWriter( geffFile );
		writer.appendModel( model );
		writer.write();

		final TmGeffReader firstReader = new TmGeffReader( geffFile );
		assertThat( firstReader.getModel().getTrackModel().nTracks( false ) )
				.as( "first save has 1 track" )
				.isEqualTo( 1 );

		// Save an empty model over the same path.
		final Model emptyModel = new Model();
		emptyModel.setPhysicalUnits( "µm", "min" );
		final TmGeffWriter overwriter = new TmGeffWriter( geffFile );
		overwriter.appendModel( emptyModel );
		overwriter.write();

		final TmGeffReader secondReader = new TmGeffReader( geffFile );
		final Model readBackModel = secondReader.getModel();
		assertThat( readBackModel ).isNotNull();
		assertThat( readBackModel.getSpots().iterable( false ) )
				.as( "re-save erases the spots of the first save" )
				.isEmpty();
		assertThat( readBackModel.getTrackModel().nTracks( false ) )
				.as( "re-save erases the tracks of the first save" )
				.isEqualTo( 0 );
		assertThat( readBackModel.getTrackModel().edgeSet() )
				.as( "the stale edges group of the first save is gone" )
				.isEmpty();
	}

	/**
	 * JUnit 4 rule that creates a temporary folder that is guaranteed to exist
	 * and be writable, and is automatically deleted after each test.
	 */
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	protected void testModelSerialization( final String path ) throws Exception
	{
		final TmXmlReader reader = new TmXmlReader( path );
		if ( !reader.isReadingOk() )
			throw new Exception( reader.getErrorMessage() );

		final Model initialModel = reader.getModel();
		final Settings initialSettings = reader.readSettings( null );
		final DisplaySettings initialDisplaySettings = reader.getDisplaySettings();
		final String initialLog = reader.getLog();
		final String initialGUIState = reader.getGUIState();

		final String geffFile = new File( temporaryFolder.getRoot(), "test.geff" ).getAbsolutePath();
		final TmGeffWriter geffWriter = new TmGeffWriter( geffFile );
		geffWriter.appendModel( initialModel );
		geffWriter.appendSettings( initialSettings );
		geffWriter.appendDisplaySettings( initialDisplaySettings );
		geffWriter.appendLog( initialLog );
		geffWriter.appendGUIState( initialGUIState );
		geffWriter.write();

		final TmGeffReader geffReader = new TmGeffReader( geffFile );

		final Model readBackModel = geffReader.getModel();
		testModelEquality( initialModel, readBackModel );

		final Settings readBackSettings = geffReader.readSettings( null );
		testSettingsEquality( initialSettings, readBackSettings );

		final DisplaySettings readBackDisplaySettings = geffReader.getDisplaySettings();
		testDisplaySettingsEquality( initialDisplaySettings, readBackDisplaySettings );

		final String readBackLog = geffReader.getLog();
		assertThat( readBackLog )
				.as( "TrackMate log" )
				.isEqualTo( initialLog );

		final String readBackGUIState = geffReader.getGUIState();
		assertThat( readBackGUIState )
				.as( "TrackMate GUI state" )
				.isEqualTo( initialGUIState );
	}

	private void testDisplaySettingsEquality( final DisplaySettings initialDisplaySettings, final DisplaySettings readBackDisplaySettings )
	{
		assertThat( initialDisplaySettings )
				.withRepresentation( new StandardRepresentation()
				{
					@Override
					public String toStringOf( final Object obj )
					{
						if ( obj instanceof final DisplaySettings ds )
							return String.format( "DisplaySettings[%d]", ds.hashCode() );
						return super.toStringOf( obj );
					}
				} )
				.usingRecursiveComparison()
				.isEqualTo( readBackDisplaySettings );
	}

	private void testSettingsEquality( final Settings initialSettings, final Settings readBackSettings )
	{

		assertThat( initialSettings )
				.withRepresentation( new StandardRepresentation()
				{
					@Override
					public String toStringOf( final Object obj )
					{
						if ( obj instanceof final Settings s )
							return String.format( "Settings[%d]", s.hashCode() );
						return super.toStringOf( obj );
					}
				} )
				.usingRecursiveComparison()
				.isEqualTo( readBackSettings );
	}

	private void testModelEquality( final Model initialModel, final Model readBackModel )
	{
		assertThat( initialModel )
				.withRepresentation( new StandardRepresentation()
				{
					@Override
					public String toStringOf( final Object obj )
					{
						if ( obj instanceof final Model m )
							return String.format( "Model[%d]", m.hashCode() );
						return super.toStringOf( obj );
					}
				} )
				.usingRecursiveComparison()
				.withEqualsForType(
						// Used to pair spots in sets.
						( a, b ) -> a.ID() == b.ID(), Spot.class )
				.ignoringFields(
						// Treated separately
						"spots",
						"featureModel",
						"trackModel",
						// Listeners
						"modelChangeListeners",
						// Utils
						"logger",
						// Transaction state fields
						"updateLevel",
						"spotsAdded",
						"spotsRemoved",
						"spotsMoved",
						"spotsUpdated",
						"eventCache",
						// Undo / redo
						"undoRedoStack" )
				.isEqualTo( readBackModel );

		/*
		 * SpotCollection
		 */

		compareSpotCollections( initialModel.getSpots(), readBackModel.getSpots() );

		/*
		 * Feature model
		 */

		assertThat( initialModel.getFeatureModel() )
				.withRepresentation( new StandardRepresentation()
				{
					@Override
					public String toStringOf( final Object obj )
					{
						if ( obj instanceof final FeatureModel fm )
							return String.format( "FeatureModel[%d]", fm.hashCode() );
						return super.toStringOf( obj );
					}
				} )
				.usingRecursiveComparison()
				.ignoringFields(
						"model", // Transient
						"edgeFeatureValues" ) // Treated separately )
				.isEqualTo( readBackModel.getFeatureModel() );

		// Compare edge feature values separately, because the keys are
		// DefaultWeightedEdge objects
		compareEdgeFeatureValues( initialModel, readBackModel );

		/*
		 * Track model
		 */

		compareTrackModels( initialModel.getTrackModel(), readBackModel.getTrackModel() );
	}

	private static void compareSpotCollections( final SpotCollection actual, final SpotCollection expected )
	{
		// Same frames
		assertThat( actual.keySet() )
				.as( "SpotCollection frames" )
				.containsExactlyInAnyOrderElementsOf( expected.keySet() );

		actual.keySet().forEach( frame -> {

			final Iterable< Spot > actualSpots = actual.iterable( frame, false );
			final Iterable< Spot > expectedSpots = expected.iterable( frame, false );

			// Same number of spots per frame
			assertThat( actualSpots )
					.as( "number of spots in frame %d", frame )
					.hasSameSizeAs( expectedSpots );

			// Build ID -> Spot lookup for expected
			final Map< Integer, Spot > expectedById = new HashMap<>();
			expectedSpots.forEach( s -> expectedById.put( s.ID(), s ) );

			actualSpots.forEach( actualSpot -> {

				// Spot exists in expected
				final Spot expectedSpot = expectedById.get( actualSpot.ID() );
				assertThat( expectedSpot )
						.as( "Spot ID=%d missing in frame %d", actualSpot.ID(), frame )
						.isNotNull();

				// Delegate to single-spot comparison
				assertSpotEquals( actualSpot, expectedSpot );
			} );
		} );
	}

	private static void assertSpotEquals( final Spot actual, final Spot expected )
	{
		// 1. Check same implementation type
		assertThat( actual.getClass() )
				.as( "Spot ID=%d implementation type", actual.ID() )
				.isEqualTo( expected.getClass() );

		// 2. Basic identity
		assertThat( actual.ID() )
				.as( "Spot ID" )
				.isEqualTo( expected.ID() );

		assertThat( actual.getName() )
				.as( "Spot ID=%d name", actual.ID() )
				.isEqualTo( expected.getName() );

		// 3. Feature keys
		assertThat( actual.getFeatures().keySet() )
				.as( "Spot ID=%d feature keys", actual.ID() )
				.containsExactlyInAnyOrderElementsOf(
						expected.getFeatures().keySet() );

		// 4. Feature values
		assertThat( actual.getFeatures() )
				.as( "Spot ID=%d feature values", actual.ID() )
				.allSatisfy( ( key, value ) -> assertThat( value )
						.as( "Spot ID=%d feature '%s'", actual.ID(), key )
						.isEqualTo(
								expected.getFeatures().getOrDefault( key, Double.NaN ) ) );

		// 5. SpotRoi-specific: polygon coordinates
		if ( actual instanceof SpotRoi )
			assertSpotRoiEquals( ( SpotRoi ) actual, ( SpotRoi ) expected );
	}

	private static void assertSpotRoiEquals( final SpotRoi actual, final SpotRoi expected )
	{
		// Number of polygon vertices
		assertThat( actual.nPoints() )
				.as( "SpotRoi ID=%d number of polygon points", actual.ID() )
				.isEqualTo( expected.nPoints() );

		// X coordinates of polygon vertices (relative to center)
		for ( int i = 0; i < actual.nPoints(); i++ )
		{
			final int idx = i; // for lambda capture
			assertThat( actual.xr( i ) )
					.as( "SpotRoi ID=%d polygon x[%d]", actual.ID(), idx )
					.isEqualTo( expected.xr( i ) );

			assertThat( actual.yr( i ) )
					.as( "SpotRoi ID=%d polygon y[%d]", actual.ID(), idx )
					.isEqualTo( expected.yr( i ) );
		}
	}

	private void compareTrackModels( final TrackModel actual, final TrackModel expected )
	{
		// 1. All spots in the graph (including isolated ones)
		assertThat( actual.vertexSet().stream()
				.map( s -> s.ID() )
				.collect( Collectors.toSet() ) )
						.as( "TrackModel: spot IDs" )
						.containsExactlyInAnyOrderElementsOf(
								expected.vertexSet().stream()
										.map( s -> s.ID() )
										.collect( Collectors.toSet() ) );

		// 2. Edge topology and weights
		final Map< String, Double > actualEdges = buildEdgeWeightMap( actual );
		final Map< String, Double > expectedEdges = buildEdgeWeightMap( expected );

		assertThat( actualEdges.keySet() )
				.as( "TrackModel: edges" )
				.containsExactlyInAnyOrderElementsOf( expectedEdges.keySet() );

		assertThat( actualEdges )
				.as( "TrackModel: edge weights" )
				.allSatisfy( ( edgeKey, weight ) -> assertThat( weight )
						.as( "weight of edge [%s]", edgeKey )
						.isEqualTo( expectedEdges.get( edgeKey ) ) );

		// 3. Track structure, visibility and names
		compareTracks( actual, expected );
	}

	/**
	 * Build "srcID->tgtID" -> weight map, independent of edge object identity.
	 */
	private static Map< String, Double > buildEdgeWeightMap( final TrackModel model )
	{
		final Map< String, Double > map = new LinkedHashMap<>();
		model.edgeSet().forEach( e -> {
			final String key = model.getEdgeSource( e ).ID() + "->" + model.getEdgeTarget( e ).ID();
			map.put( key, model.getEdgeWeight( e ) );
		} );
		return map;
	}

	/** Compare tracks matched by spot content, not by track ID. */
	private static void compareTracks( final TrackModel actual, final TrackModel expected )
	{
		final Set< Integer > actualTrackIDs = actual.trackIDs( false );
		final Set< Integer > expectedTrackIDs = expected.trackIDs( false );

		assertThat( actualTrackIDs )
				.as( "TrackModel: identical tracks IDs" )
				.isEqualTo( expectedTrackIDs );

		actualTrackIDs.forEach( actualTID -> {

			// Visibility
			assertThat( actual.isVisible( actualTID ) )
					.as( "visibility of track ID: [%d]", actualTID )
					.isEqualTo( expected.isVisible( actualTID ) );

			// Name
			assertThat( actual.name( actualTID ) )
					.as( "name of track ID: [%d]", actualTID )
					.isEqualTo( expected.name( actualTID ) );
		} );
	}

	private void compareEdgeFeatureValues( final Model m1, final Model m2 )
	{
		final FeatureModel fm1 = m1.getFeatureModel();
		final FeatureModel fm2 = m2.getFeatureModel();

		// If so, iterate edges from the track model instead:
		m1.getTrackModel().edgeSet().forEach( edge -> {
			final int sId = m1.getTrackModel().getEdgeSource( edge ).ID();
			final int tId = m1.getTrackModel().getEdgeTarget( edge ).ID();

			// Find matching edge in readback model
			final DefaultWeightedEdge matchedEdge = m2.getTrackModel().edgeSet().stream()
					.filter( e -> m2.getTrackModel().getEdgeSource( e ).ID() == sId
							&& m2.getTrackModel().getEdgeTarget( e ).ID() == tId )
					.findFirst()
					.orElseThrow( () -> new AssertionError( "No matching edge for ID" + sId + " -> ID" + tId ) );

			// Compare feature by feature
			fm1.getEdgeFeatures()
					.forEach( featureKey -> {
						final Double actual = fm1.getEdgeFeature( edge, featureKey );
						final Double expected = fm2.getEdgeFeature( matchedEdge, featureKey );

						// Both null is ok.
						if ( actual == null && expected == null )
							return;

						assertThat( actual )
								.as( "feature '%s' on edge (ID%d -> ID%d)", featureKey, sId, tId )
								.isEqualTo( expected );
					} );
		} );
	}

	@BeforeClass
	public static void checkBlosc()
	{
		System.out.println( "[GeffCompatSpotBaseTest] Checking that blosc is loadable..." );
		System.out.println( "jna.library.path = " + System.getProperty( "jna.library.path" ) );
		System.out.println( "java.library.path = " + System.getProperty( "java.library.path" ) );
		try
		{
			new org.janelia.saalfeldlab.n5.blosc.BloscCompression();
			System.out.println( "blosc loaded successfully" );
		}
		catch ( final Throwable t )
		{
			System.err.println( "blosc load failed: " + t.getMessage() );
		}
	}
}
