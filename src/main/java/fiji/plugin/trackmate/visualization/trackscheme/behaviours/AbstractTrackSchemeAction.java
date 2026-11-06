package fiji.plugin.trackmate.visualization.trackscheme.behaviours;

import java.util.ArrayList;
import java.util.List;

import org.scijava.ui.behaviour.util.AbstractNamedAction;

import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;

public abstract class AbstractTrackSchemeAction extends AbstractNamedAction
{

	private static final long serialVersionUID = 1L;

	protected AbstractTrackSchemeAction( final String name )
	{
		super( name );
	}

	protected List< mxCell > getSelectionVertices( final mxGraph graph )
	{
		// Build selection categories
		final Object[] selection = graph.getSelectionCells();
		final ArrayList< mxCell > vertices = new ArrayList<>();
		for ( final Object obj : selection )
		{
			final mxCell cell = ( mxCell ) obj;
			if ( cell.isVertex() )
				vertices.add( cell );
		}
		return vertices;
	}

	protected List< mxCell > getSelectionEdges( final mxGraph graph )
	{
		// Build selection categories
		final Object[] selection = graph.getSelectionCells();
		final ArrayList< mxCell > edges = new ArrayList<>();
		for ( final Object obj : selection )
		{
			final mxCell cell = ( mxCell ) obj;
			if ( cell.isEdge() )
				edges.add( cell );
		}
		return edges;
	}

}
