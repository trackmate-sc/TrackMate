package fiji.plugin.trackmate.action.meshtools;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotMesh;
import net.imglib2.mesh.Mesh;
import net.imglib2.mesh.Meshes;
import net.imglib2.mesh.alg.TaubinSmoothing;
import net.imglib2.mesh.alg.TaubinSmoothing.TaubinWeightType;
import net.imglib2.mesh.impl.nio.BufferMesh;

public class MeshSmoother
{

	private final Map< SpotMesh, BufferMesh > undoMap;

	private final Logger logger;

	public MeshSmoother( final Iterable< Spot > spots, final Logger logger )
	{
		this.logger = logger;
		// Store undo.
		this.undoMap = new HashMap<>();
		final double[] center = new double[ 3 ];
		for ( final Spot spot : spots )
		{
			if ( SpotMesh.class.isInstance( spot ) )
			{
				final SpotMesh sm = ( SpotMesh ) spot;
				final Mesh mesh = sm.getMesh();
				final BufferMesh meshCopy = new BufferMesh( mesh.vertices().size(), mesh.triangles().size() );
				Meshes.copy( mesh, meshCopy );
				sm.localize( center );
				Meshes.translate( meshCopy, center );
				undoMap.put( sm, meshCopy );
			}
		}
	}

	public void undo()
	{
		logger.setStatus( "Undoing mesh smoothing" );
		final Set< SpotMesh > keys = undoMap.keySet();
		final int nSpots = keys.size();
		int i = 0;
		for ( final SpotMesh sm : keys )
		{
			final BufferMesh old = undoMap.get( sm );
			sm.setMesh( old );
			logger.setProgress( ( double ) ( ++i ) / nSpots );
		}
		logger.setStatus( "" );
	}

	public void smooth( final int nIters, final double mu, final double lambda, final TaubinWeightType weightType )
	{
		logger.setStatus( "Taubin smoothing" );
		final Set< SpotMesh > keys = undoMap.keySet();
		final int nSpots = keys.size();
		int i = 0;
		final double[] center = new double[ 3 ];
		for ( final SpotMesh sm : keys )
		{
			final Mesh mesh = sm.getMesh();
			sm.localize( center );
			Meshes.translate( mesh, center );
			final BufferMesh smoothedMesh = TaubinSmoothing.smooth( mesh, nIters, lambda, mu, weightType );
			sm.setMesh( smoothedMesh );
			logger.setProgress( ( double ) ( ++i ) / nSpots );
		}
		logger.setStatus( "" );
	}
}
