/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.undo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.interactivetests.GraphTest;

/**
 * Tests for undo/redo of track name changes as they would occur from GUI actions.
 * This verifies that track name changes wrapped in transactions are properly undoable.
 */
public class TrackNameUndoRedoGuiTest
{

	private Model model;

	@Before
	public void setUp()
	{
		this.model = GraphTest.getExampleModel();
	}

	@Test
	public void testUndoTrackRenameWithTransaction()
	{
		// Simulate GUI-style track rename (wrapped in transaction)
		final Integer trackId = model.getTrackModel().trackIDs( false ).iterator().next();
		final String originalName = model.getTrackModel().name( trackId );

		// GUI components wrap the rename in a transaction
		model.beginUpdate();
		try
		{
			model.setTrackName( trackId, "RenamedFromGUI" );
		}
		finally
		{
			model.endUpdate();
		}

		// Verify name changed
		assertThat( model.getTrackModel().name( trackId ) )
				.as( "Track name after rename" )
				.isEqualTo( "RenamedFromGUI" );

		// Undo
		model.undo();

		// Verify name is restored
		assertThat( model.getTrackModel().name( trackId ) )
				.as( "Track name after undo" )
				.isEqualTo( originalName );

		// Redo
		model.redo();

		// Verify name is changed again
		assertThat( model.getTrackModel().name( trackId ) )
				.as( "Track name after redo" )
				.isEqualTo( "RenamedFromGUI" );
	}
}
