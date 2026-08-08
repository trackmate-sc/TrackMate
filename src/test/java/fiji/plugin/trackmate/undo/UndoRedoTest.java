package fiji.plugin.trackmate.undo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.Before;
import org.junit.Test;

import fiji.plugin.trackmate.AssertJTrackMate;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.interactivetests.GraphTest;

public class UndoRedoTest
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
	public void testUndoAddSpot()
	{
		testCore( () -> addSpot() );
	}

	@Test
	public void testUndoRemoveSpot()
	{
		testCore( () -> removeSpot() );
	}

	@Test
	public void testUndoAddEdge()
	{
		testCore( () -> addEdge() );
	}

	@Test
	public void testUndoRemoveEdge()
	{
		testCore( () -> removeEdge() );
	}

	@Test
	public void testSeveralModifs()
	{
		final Runnable doModifs = () -> {
			addSpot();
			removeSpot();
			addEdge();
			removeEdge();
		};
		testCore( doModifs );
	}

	@Test
	public void testUndoChangePosition()
	{
		final Spot spot = model.getSpots().iterator( 0, true ).next();
		final double originalX = spot.getDoublePosition( 0 );
		
		model.beginUpdate();
		try
		{
			model.beforeEdit( spot );
			spot.setPosition( originalX + 1,0 );
		}
		finally
		{
			model.endUpdate();
		}
		assertThat( originalX ).isNotEqualTo( spot.getDoublePosition( 0 ) );

		// Undo command.
		model.undo();

		// Must succeed: model is back to original state.
		assertThat( originalX ).isEqualTo( spot.getDoublePosition( 0 ) );
	}

	@Test
	public void testUndoChangeName()
	{
		final Spot spot = model.getSpots().iterator( 0, true ).next();
		final String originalName = spot.getName();

		model.beginUpdate();
		try
		{
			model.beforeEdit( spot );
			spot.setName( originalName + "_New name" );
		}
		finally
		{
			model.endUpdate();
		}
		assertThat( originalName ).isNotEqualTo( spot.getName() );

		// Undo command.
		model.undo();

		// Must succeed: model is back to original state.
		assertThat( originalName ).isEqualTo( spot.getName() );
	}

	private void testCore( final Runnable doModifs )
	{
		// Must succeed: model is not modified yet.
		AssertJTrackMate.testModelEquality( model, original, false );

		// Do modifications.
		model.beginUpdate();
		try
		{
			doModifs.run();
		}
		finally
		{
			model.endUpdate();
		}

		// Must fail: model is modified.
		assertThatThrownBy( () -> AssertJTrackMate.testModelEquality( model, original, false ) )
				.isInstanceOf( AssertionError.class );

		// Undo commands.
		model.undo();

		// Must succeed: model is back to original state.
		// Use flexible comparison (ignore track IDs) since track topology changes
		// may result in different track IDs even when content is restored.
		AssertJTrackMate.testModelEquality( model, original, false );
	}

	private void addSpot()
	{
		final Spot spot = new SpotBase( 0d, 0d, 0d, 1d, -1d );
		model.addSpotTo( spot, 0 );
	}

	private void removeSpot()
	{
		final Spot spot = model.getSpots().iterator( 0, true ).next();
		model.removeSpot( spot );
	}

	private void addEdge()
	{
		final Spot source = model.getSpots().iterator( 0, true ).next();
		final Spot target = model.getSpots().iterator( 3, true ).next();
		model.addEdge( source, target, 0 );
	}

	private void removeEdge()
	{
		final Set< DefaultWeightedEdge > edges = model.getTrackModel().edgeSet();
		model.removeEdge( edges.iterator().next() );
	}
}
