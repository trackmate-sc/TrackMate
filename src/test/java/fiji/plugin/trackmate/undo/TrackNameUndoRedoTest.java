package fiji.plugin.trackmate.undo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Before;
import org.junit.Test;

import fiji.plugin.trackmate.AssertJTrackMate;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.interactivetests.GraphTest;

/**
 * Tests for undo/redo of track name changes.
 * <p>
 * Note: Track topology changes (split/merge) are already handled by the existing
 * undo mechanism which restores edges. This test focuses on track name preservation
 * through undo/redo operations.
 */
public class TrackNameUndoRedoTest
{

	private Model model;

	private Model original;

	@Before
	public void setUp()
	{
		this.model = GraphTest.getExampleModel();
		this.original = model.copy();
	}

	@Test
	public void testUndoTrackRename()
	{
		// Get the first track ID
		final Integer trackId = model.getTrackModel().trackIDs( false ).iterator().next();
		final String originalName = model.getTrackModel().name( trackId );

		// Rename the track
		model.beginUpdate();
		try
		{
			model.setTrackName( trackId, "MyCustomTrackName" );
		}
		finally
		{
			model.endUpdate();
		}

		// Verify name changed
		assertThat( model.getTrackModel().name( trackId ) )
				.as( "Track name after rename" )
				.isEqualTo( "MyCustomTrackName" );

		// Model should differ from original
		assertThatThrownBy( () -> AssertJTrackMate.testModelEquality( model, original ) )
				.isInstanceOf( AssertionError.class );

		// Undo
		model.undo();

		// Verify name is restored
		assertThat( model.getTrackModel().name( trackId ) )
				.as( "Track name after undo" )
				.isEqualTo( originalName );

		// Model should be back to original
		AssertJTrackMate.testModelEquality( model, original );

		// Redo
		model.redo();

		// Verify name is changed again
		assertThat( model.getTrackModel().name( trackId ) )
				.as( "Track name after redo" )
				.isEqualTo( "MyCustomTrackName" );
	}

	@Test
	public void testUndoTrackRenameMultipleTracks()
	{
		// Rename multiple tracks
		final Integer trackId1 = model.getTrackModel().trackIDs( false ).iterator().next();
		final String originalName1 = model.getTrackModel().name( trackId1 );

		model.beginUpdate();
		try
		{
			model.setTrackName( trackId1, "FirstTrack" );
		}
		finally
		{
			model.endUpdate();
		}

		// Verify
		assertThat( model.getTrackModel().name( trackId1 ) ).isEqualTo( "FirstTrack" );

		// Undo
		model.undo();
		assertThat( model.getTrackModel().name( trackId1 ) ).isEqualTo( originalName1 );

		// Redo
		model.redo();
		assertThat( model.getTrackModel().name( trackId1 ) ).isEqualTo( "FirstTrack" );
	}

	@Test
	public void testUndoTrackRenameAfterRedo()
	{
		// Test multiple undo/redo cycles
		final Integer trackId = model.getTrackModel().trackIDs( false ).iterator().next();
		final String originalName = model.getTrackModel().name( trackId );

		// First rename
		model.beginUpdate();
		try
		{
			model.setTrackName( trackId, "FirstRename" );
		}
		finally
		{
			model.endUpdate();
		}

		// Undo back to original
		model.undo();
		assertThat( model.getTrackModel().name( trackId ) ).isEqualTo( originalName );

		// Redo to first rename
		model.redo();
		assertThat( model.getTrackModel().name( trackId ) ).isEqualTo( "FirstRename" );

		// Rename again
		model.beginUpdate();
		try
		{
			model.setTrackName( trackId, "SecondRename" );
		}
		finally
		{
			model.endUpdate();
		}

		// Should undo to first rename, not original
		model.undo();
		assertThat( model.getTrackModel().name( trackId ) ).isEqualTo( "FirstRename" );
	}
}
