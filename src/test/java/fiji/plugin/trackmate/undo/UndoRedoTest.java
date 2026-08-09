package fiji.plugin.trackmate.undo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.Before;
import org.junit.Test;

import fiji.plugin.trackmate.AssertJTrackMate;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.interactivetests.GraphTest;
import net.imglib2.mesh.impl.nio.BufferMesh;

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

	@Test
	public void testUndoChangeSpotFeature()
	{
		final Spot spot = model.getSpots().iterator( 0, true ).next();
		final String featureKey = "QUALITY";
		final double originalQuality = spot.getFeature( featureKey );
		final double newQuality = originalQuality + 100.0;

		model.beginUpdate();
		try
		{
			model.beforeEdit( spot );
			spot.putFeature( featureKey, newQuality );
		}
		finally
		{
			model.endUpdate();
		}

		// Verify the feature was changed
		assertThat( originalQuality ).isNotEqualTo( spot.getFeature( featureKey ) );
		assertThat( spot.getFeature( featureKey ) ).isEqualTo( newQuality );

		// Undo command.
		model.undo();

		// Must succeed: model is back to original state.
		assertThat( spot.getFeature( featureKey ) )
				.as( "Spot feature %s should be restored after undo", featureKey )
				.isEqualTo( originalQuality );
	}

	@Test
	public void testUndoChangeEdgeFeature()
	{
		// Get an edge from the model
		final DefaultWeightedEdge edge = model.getTrackModel().edgeSet().iterator().next();
		final Spot source = model.getTrackModel().getEdgeSource( edge );
		final FeatureModel featureModel = model.getFeatureModel();

		// Get the first available edge feature key
		final String featureKey = featureModel.getEdgeFeatures().iterator().next();
		final Double originalValue = featureModel.getEdgeFeature( edge, featureKey );
		final Double newValue = ( originalValue == null ? 42.0 : originalValue + 100.0 );

		model.beginUpdate();
		try
		{
			// Flag the edge for undo by flagging the source spot
			// (edge features are captured when touching spots are flagged)
			model.beforeEdit( source );
			featureModel.putEdgeFeature( edge, featureKey, newValue );
		}
		finally
		{
			model.endUpdate();
		}

		// Verify the feature was changed
		assertThat( featureModel.getEdgeFeature( edge, featureKey ) ).isEqualTo( newValue );
		assertThat( featureModel.getEdgeFeature( edge, featureKey ) ).isNotEqualTo( originalValue );

		// Undo command.
		model.undo();

		// Must succeed: model is back to original state.
		assertThat( featureModel.getEdgeFeature( edge, featureKey ) )
				.as( "Edge feature %s should be restored after undo", featureKey )
				.isEqualTo( originalValue );
	}

	@Test
	public void testUndoChangeSpotPolygon()
	{
		// Create a spot with ROI (a simple triangle)
		final double[] xCoords = { -0.5, 0.5, 0.0 };
		final double[] yCoords = { -0.5, -0.5, 0.5 };
		final SpotRoi spotWithRoi = new SpotRoi( 0d, 0d, 0d, 1d, -1d, "TestSpot", xCoords, yCoords );

		// Capture original polygon coordinates
		final int nPoints = spotWithRoi.nPoints();
		final double[] originalXr = new double[ nPoints ];
		final double[] originalYr = new double[ nPoints ];
		for ( int i = 0; i < nPoints; i++ )
		{
			originalXr[ i ] = spotWithRoi.xr( i );
			originalYr[ i ] = spotWithRoi.yr( i );
		}

		model.beginUpdate();
		try
		{
			model.beforeEdit( spotWithRoi );
			// Modify the polygon (change first point)
			spotWithRoi.setXr( 0, originalXr[ 0 ] + 1.0 );
			spotWithRoi.setYr( 0, originalYr[ 0 ] + 1.0 );
		}
		finally
		{
			model.endUpdate();
		}

		// Verify the polygon was changed
		assertThat( spotWithRoi.xr( 0 ) )
				.as( "Polygon X[0] should be modified" )
				.isEqualTo( originalXr[ 0 ] + 1.0 );
		assertThat( spotWithRoi.yr( 0 ) )
				.as( "Polygon Y[0] should be modified" )
				.isEqualTo( originalYr[ 0 ] + 1.0 );

		// Undo command.
		model.undo();

		// Must succeed: model is back to original state.
		for ( int i = 0; i < nPoints; i++ )
		{
			assertThat( spotWithRoi.xr( i ) )
					.as( "Polygon X[%d] should be restored after undo", i )
					.isEqualTo( originalXr[ i ] );
			assertThat( spotWithRoi.yr( i ) )
					.as( "Polygon Y[%d] should be restored after undo", i )
					.isEqualTo( originalYr[ i ] );
		}
	}

	@Test
	public void testUndoRedoSpotMove()
	{
		final Spot spot = model.getSpots().iterator( 0, true ).next();
		final double originalX = spot.getDoublePosition( 0 );
		final double originalY = spot.getDoublePosition( 1 );
		final double originalZ = spot.getDoublePosition( 2 );
		final double newX = originalX + 10.0;
		final double newY = originalY + 10.0;
		final double newZ = originalZ + 10.0;

		model.beginUpdate();
		try
		{
			model.beforeEdit( spot );
			spot.setPosition( newX, 0 );
			spot.setPosition( newY, 1 );
			spot.setPosition( newZ, 2 );
		}
		finally
		{
			model.endUpdate();
		}

		// Verify the spot was moved
		assertThat( spot.getDoublePosition( 0 ) ).isEqualTo( newX );
		assertThat( spot.getDoublePosition( 1 ) ).isEqualTo( newY );
		assertThat( spot.getDoublePosition( 2 ) ).isEqualTo( newZ );

		// Undo command.
		model.undo();

		// Verify position is restored after undo
		assertThat( spot.getDoublePosition( 0 ) ).isEqualTo( originalX );
		assertThat( spot.getDoublePosition( 1 ) ).isEqualTo( originalY );
		assertThat( spot.getDoublePosition( 2 ) ).isEqualTo( originalZ );

		// Redo command.
		model.redo();

		// Verify position is restored after redo
		assertThat( spot.getDoublePosition( 0 ) ).isEqualTo( newX );
		assertThat( spot.getDoublePosition( 1 ) ).isEqualTo( newY );
		assertThat( spot.getDoublePosition( 2 ) ).isEqualTo( newZ );
	}

	@Test
	public void testUndoChangeSpotMesh()
	{
		// Create a simple tetrahedron mesh
		final BufferMesh originalMesh = createTetrahedronMesh();
		final SpotMesh spotWithMesh = new SpotMesh( originalMesh, -1., "TestSpotMesh" );

		// Add spot to model
		model.beginUpdate();
		try
		{
			model.addSpotTo( spotWithMesh, 0 );
		}
		finally
		{
			model.endUpdate();
		}

		// Capture original vertex positions
		final int nVertices = originalMesh.vertices().size();
		final float[] originalX = new float[ nVertices ];
		final float[] originalY = new float[ nVertices ];
		final float[] originalZ = new float[ nVertices ];
		for ( int i = 0; i < nVertices; i++ )
		{
			originalX[ i ] = originalMesh.vertices().xf( i );
			originalY[ i ] = originalMesh.vertices().yf( i );
			originalZ[ i ] = originalMesh.vertices().zf( i );
		}

		// Modify the mesh (move first vertex)
		model.beginUpdate();
		try
		{
			model.beforeEdit( spotWithMesh );
			spotWithMesh.getMesh().vertices().setPositionf( 0,
					originalX[ 0 ] + 1.0f,
					originalY[ 0 ] + 1.0f,
					originalZ[ 0 ] + 1.0f );
		}
		finally
		{
			model.endUpdate();
		}

		// Verify the mesh was changed
		assertThat( spotWithMesh.getMesh().vertices().xf( 0 ) )
				.as( "Mesh vertex X[0] should be modified" )
				.isEqualTo( originalX[ 0 ] + 1.0f );

		// Undo command.
		model.undo();

		// Verify mesh is restored (use offset comparison for floating point tolerance)
		for ( int i = 0; i < nVertices; i++ )
		{
			assertThat( spotWithMesh.getMesh().vertices().xf( i ) )
					.as( "Mesh vertex X[%d] should be restored after undo", i )
					.isEqualTo( originalX[ i ], within( 1e-5f ) );
			assertThat( spotWithMesh.getMesh().vertices().yf( i ) )
					.as( "Mesh vertex Y[%d] should be restored after undo", i )
					.isEqualTo( originalY[ i ], within( 1e-5f ) );
			assertThat( spotWithMesh.getMesh().vertices().zf( i ) )
					.as( "Mesh vertex Z[%d] should be restored after undo", i )
					.isEqualTo( originalZ[ i ], within( 1e-5f ) );
		}
	}

	@Test
	public void testUndoRedoSpotMeshScale()
	{
		// Create a simple tetrahedron mesh
		final BufferMesh originalMesh = createTetrahedronMesh();
		final SpotMesh spotWithMesh = new SpotMesh( originalMesh, -1., "TestSpotMesh" );

		// Add spot to model
		model.beginUpdate();
		try
		{
			model.addSpotTo( spotWithMesh, 0 );
		}
		finally
		{
			model.endUpdate();
		}

		// Capture original vertex positions and radius
		final int nVertices = originalMesh.vertices().size();
		final float[] originalX = new float[ nVertices ];
		final float[] originalY = new float[ nVertices ];
		final float[] originalZ = new float[ nVertices ];
		final double originalRadius = spotWithMesh.getFeature( Spot.RADIUS );
		for ( int i = 0; i < nVertices; i++ )
		{
			originalX[ i ] = originalMesh.vertices().xf( i );
			originalY[ i ] = originalMesh.vertices().yf( i );
			originalZ[ i ] = originalMesh.vertices().zf( i );
		}

		// Scale the mesh by factor of 2
		final double scaleFactor = 2.0;
		model.beginUpdate();
		try
		{
			model.beforeEdit( spotWithMesh );
			spotWithMesh.scale( scaleFactor );
		}
		finally
		{
			model.endUpdate();
		}

		// Verify the mesh was scaled
		assertThat( spotWithMesh.getFeature( Spot.RADIUS ) )
				.as( "Radius should be scaled" )
				.isCloseTo( originalRadius * scaleFactor, within( 1e-10 ) );

		// Undo command
		model.undo();

		// Verify radius is restored
		assertThat( spotWithMesh.getFeature( Spot.RADIUS ) )
				.as( "Radius should be restored after undo" )
				.isCloseTo( originalRadius, within( 1e-10 ) );

		// Verify mesh vertices are restored
		for ( int i = 0; i < nVertices; i++ )
		{
			assertThat( spotWithMesh.getMesh().vertices().xf( i ) )
					.as( "Mesh vertex X[%d] should be restored after undo", i )
					.isCloseTo( originalX[ i ], within( 1e-5f ) );
			assertThat( spotWithMesh.getMesh().vertices().yf( i ) )
					.as( "Mesh vertex Y[%d] should be restored after undo", i )
					.isCloseTo( originalY[ i ], within( 1e-5f ) );
			assertThat( spotWithMesh.getMesh().vertices().zf( i ) )
					.as( "Mesh vertex Z[%d] should be restored after undo", i )
					.isCloseTo( originalZ[ i ], within( 1e-5f ) );
		}

		// Redo command
		model.redo();

		// Verify radius is restored after redo
		assertThat( spotWithMesh.getFeature( Spot.RADIUS ) )
				.as( "Radius should be restored after redo" )
				.isCloseTo( originalRadius * scaleFactor, within( 1e-10 ) );
	}

	/**
	 * Creates a simple tetrahedron mesh for testing.
	 */
	private static BufferMesh createTetrahedronMesh()
	{
		final BufferMesh mesh = new BufferMesh( 4, 4 );
		// 4 vertices of a tetrahedron
		mesh.vertices().add( 0.0f, 0.0f, 1.0f );
		mesh.vertices().add( 0.0f, 0.942809f, -0.333333f );
		mesh.vertices().add( -0.816497f, -0.471405f, -0.333333f );
		mesh.vertices().add( 0.816497f, -0.471405f, -0.333333f );
		// 4 triangles
		mesh.triangles().add( 0, 1, 2 );
		mesh.triangles().add( 0, 2, 3 );
		mesh.triangles().add( 0, 3, 1 );
		mesh.triangles().add( 1, 3, 2 );
		return mesh;
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