package fiji.plugin.trackmate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.presentation.StandardRepresentation;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

/**
 * Utility class to provide AssertJ assertions for TrackMate objects.
 */
public class AssertJTrackMate
{

	public static void testDisplaySettingsEquality( final DisplaySettings actual, final DisplaySettings expected )
	{
		assertThat( actual )
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
				.isEqualTo( expected );
	}

	public static void testSettingsEquality( final Settings actual, final Settings expected )
	{
		assertThat( actual )
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
				.isEqualTo( expected );
	}

	public static void testModelEquality( final Model actual, final Model expected )
	{
		testModelEquality( actual, expected, true );
	}

	/**
	 * Compares two models for equality.
	 * <p>
	 * If {@code compareTrackIds} is true, tracks are matched by their track IDs.
	 * If false, tracks are matched by their spot content and names, ignoring
	 * track ID mismatches (useful after undo/redo operations that may change track IDs).
	 *
	 * @param actual the actual Model
	 * @param expected the expected Model
	 * @param compareTrackIds if true, compare tracks by ID; if false, compare by spot content and name
	 */
	public static void testModelEquality( final Model actual, final Model expected, final boolean compareTrackIds )
	{
		assertThat( actual )
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
				.isEqualTo( expected );

		/*
		 * SpotCollection
		 */

		compareSpotCollections( actual.getSpots(), expected.getSpots() );

		/*
		 * Feature model
		 */

		assertThat( actual.getFeatureModel() )
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
				.isEqualTo( expected.getFeatureModel() );

		// Compare edge feature values separately, because the keys are
		// DefaultWeightedEdge objects
		compareEdgeFeatureValues( actual, expected );

		/*
		 * Track model
		 */

		compareTrackModels( actual.getTrackModel(), expected.getTrackModel(), compareTrackIds );
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

	private static void compareTrackModels( final TrackModel actual, final TrackModel expected, final boolean compareById )
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
		compareTracks( actual, expected, compareById );
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

	/**
	 * Compares tracks between two TrackModels.
	 * <p>
	 * If {@code compareById} is true, tracks are matched by their track IDs
	 * (the default behavior). If false, tracks are matched by their spot content
	 * and names, ignoring track ID mismatches (useful after undo/redo operations
	 * that may change track IDs).
	 *
	 * @param actual the actual TrackModel
	 * @param expected the expected TrackModel
	 * @param compareById if true, compare by track ID; if false, compare by spot content and name
	 */
	private static void compareTracks( final TrackModel actual, final TrackModel expected, final boolean compareById )
	{
		final Set< Integer > actualTrackIDs = actual.trackIDs( false );
		final Set< Integer > expectedTrackIDs = expected.trackIDs( false );

		if ( compareById )
		{
			// Strict comparison: track IDs must match exactly
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
		else
		{
			// Flexible comparison: match tracks by spot content and name
			assertThat( actualTrackIDs.size() )
					.as( "TrackModel: number of tracks" )
					.isEqualTo( expectedTrackIDs.size() );

			// Build a map of track spots for matching
			final Map< Set< Integer >, Integer > expectedTrackSpotsToId = new HashMap<>();
			for ( final Integer expectedTID : expectedTrackIDs )
			{
				final Set< Integer > spotIds = expected.trackSpots( expectedTID ).stream()
						.map( Spot::ID )
						.collect( Collectors.toSet() );
				expectedTrackSpotsToId.put( spotIds, expectedTID );
			}

			// For each actual track, find matching expected track by spot content
			actualTrackIDs.forEach( actualTID -> {
				final Set< Integer > actualSpotIds = actual.trackSpots( actualTID ).stream()
						.map( Spot::ID )
						.collect( Collectors.toSet() );

				final Integer expectedTID = expectedTrackSpotsToId.get( actualSpotIds );
				assertThat( expectedTID )
						.as( "Track with spot IDs %s not found in expected model", actualSpotIds )
						.isNotNull();

				// Visibility
				assertThat( actual.isVisible( actualTID ) )
						.as( "visibility of track with spots %s", actualSpotIds )
						.isEqualTo( expected.isVisible( expectedTID ) );

				// Name
				assertThat( actual.name( actualTID ) )
						.as( "name of track with spots %s", actualSpotIds )
						.isEqualTo( expected.name( expectedTID ) );
			} );
		}
	}

	private static void compareEdgeFeatureValues( final Model m1, final Model m2 )
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
}
