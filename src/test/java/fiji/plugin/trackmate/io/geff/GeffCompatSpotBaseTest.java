package fiji.plugin.trackmate.io.geff;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.presentation.StandardRepresentation;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.io.TmXmlReader;

/**
 * Tests that a TrackMate XML file can be read and written to a GEFF file, and
 * that the resulting objects are identical.
 */
public class GeffCompatSpotBaseTest extends GeffTestBase
{

	/**
	 * A TrackMate XML file generated with TrackMate v8, containing only
	 * 'SpotBase' spots.
	 */
	private static final String PATH = GeffCompatSpotBaseTest.class
			.getResource( "FakeTracks.xml" )
			.getFile();

	/**
	 * JUnit 4 rule that creates a temporary folder that is guaranteed to exist
	 * and be writable, and is automatically deleted after each test.
	 */
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void testModelSerialization() throws Exception
	{
		final TmXmlReader reader = new TmXmlReader( new File( PATH ) );
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
				.ignoringFields(
						// Treated separately
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
						"eventCache" )
				.isEqualTo( readBackModel );

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
