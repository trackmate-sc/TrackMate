package fiji.plugin.trackmate.visualization.trackscheme.behaviours;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.util.List;

import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.trackscheme.JGraphXAdapter;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeGraphComponent;

public class EditNameAction extends AbstractTrackSchemeAction
{

	private static final long serialVersionUID = 1L;

	private final TrackSchemeGraphComponent graphComponent;

	private final Model model;

	public EditNameAction( final Model model, final TrackSchemeGraphComponent graphComponent )
	{
		super( TrackSchemeActions.EDIT_NAME );
		this.model = model;
		this.graphComponent = graphComponent;
	}

	@Override
	public void actionPerformed( final ActionEvent e )
	{
		multiEditSpotName( graphComponent, e );
	}

	private void multiEditSpotName( final TrackSchemeGraphComponent lGraphComponent, final ActionEvent triggerEvent )
	{
		/*
		 * We want to display the editing window in the cell is the closer to
		 * where the user clicked. That is not perfect, because we can imagine
		 * the click is made for from the selected cells, and that the editing
		 * window will not even be displayed on the screen. No idea for that
		 * yet, because JGraphX is expecting to receive a cell as location for
		 * the editing window.
		 */
		final JGraphXAdapter graph = lGraphComponent.getGraph();
		final List< mxCell > vertices = getSelectionVertices( graph );
		if ( vertices.isEmpty() )
			return;

		final Point mousePosition = lGraphComponent.getMousePosition();
		final mxCell tc;
		if ( null != mousePosition )
			tc = getClosestCell( vertices, mousePosition );
		else
			tc = vertices.get( 0 );
		vertices.remove( tc );

		lGraphComponent.startEditingAtCell( tc, triggerEvent );
		lGraphComponent.addListener( mxEvent.LABEL_CHANGED, new mxIEventListener()
		{

			@Override
			public void invoke( final Object sender, final mxEventObject evt )
			{
				model.beginUpdate();
				try
				{
					for ( final mxCell cell : vertices )
					{
						cell.setValue( tc.getValue() );
						final Spot spot = graph.getSpotFor( cell );
						model.beforeEdit( spot ); // name change undoable
						spot.setName( tc.getValue().toString() );
					}
					lGraphComponent.refresh();
					lGraphComponent.removeListener( this );
				}
				finally
				{
					model.endUpdate();
				}
			}
		} );
	}

	/**
	 * Return, from the given list of cell, the one which is the closer to the
	 * point of this instance.
	 *
	 * @param point
	 */
	private mxCell getClosestCell( final Iterable< mxCell > vertices, final Point2D point )
	{
		double min_dist = Double.POSITIVE_INFINITY;
		mxCell target_cell = null;
		for ( final mxCell cell : vertices )
		{
			final Point location = cell.getGeometry().getPoint();
			final double dist = location.distanceSq( point );
			if ( dist < min_dist )
			{
				min_dist = dist;
				target_cell = cell;
			}
		}
		return target_cell;
	}
}
