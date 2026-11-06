package fiji.plugin.trackmate.visualization.trackscheme.behaviours;

import java.awt.event.ActionEvent;
import java.util.List;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxICell;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.trackscheme.JGraphXAdapter;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeGraphComponent;

public class HomingActions
{

	/**
	 * Centers the view to the first cell in selection, sorted by frame number.
	 */
	public static class HomeAction extends AbstractTrackSchemeAction
	{

		private static final long serialVersionUID = 1L;

		private final TrackSchemeGraphComponent graphComponent;

		public HomeAction( final TrackSchemeGraphComponent graphComponent )
		{
			super( TrackSchemeActions.CENTER_ON_FIRST_SELECTED );
			this.graphComponent = graphComponent;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{

			mxCell cell = null;
			final JGraphXAdapter graph = graphComponent.getGraph();
			final List< mxCell > vertices = getSelectionVertices( graph );
			if ( !vertices.isEmpty() )
			{
				int minFrame = Integer.MAX_VALUE;
				for ( final mxCell mxCell : vertices )
				{
					final int frame = graph.getSpotFor( mxCell ).getFeature( Spot.FRAME ).intValue();
					if ( frame < minFrame )
					{
						minFrame = frame;
						cell = mxCell;
					}
				}
			}
			else
			{
				final List< mxCell > edges = getSelectionEdges( graph );
				if ( !edges.isEmpty() )
				{
					int minFrame = Integer.MAX_VALUE;
					for ( final mxCell mxCell : edges )
					{
						final mxICell target = mxCell.getTarget();
						final int frame = graph.getSpotFor( target ).getFeature( Spot.FRAME ).intValue();
						if ( frame < minFrame )
						{
							minFrame = frame;
							cell = mxCell;
						}
					}
					cell = edges.get( edges.size() - 1 );
				}
				else
				{
					return;
				}
			}
			graphComponent.scrollCellToVisible( cell, true );
		}
	}

	/**
	 * Centers the view to the last cell in selection, sorted by frame number.
	 */
	public static class EndAction extends AbstractTrackSchemeAction
	{

		private static final long serialVersionUID = 1L;

		private final TrackSchemeGraphComponent graphComponent;

		public EndAction( final TrackSchemeGraphComponent graphComponent )
		{
			super( TrackSchemeActions.CENTER_ON_LAST_SELECTED );
			this.graphComponent = graphComponent;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			mxCell cell = null;
			final JGraphXAdapter graph = graphComponent.getGraph();
			final List< mxCell > vertices = getSelectionVertices( graph );

			if ( !vertices.isEmpty() )
			{
				int maxFrame = Integer.MIN_VALUE;
				for ( final mxCell mxCell : vertices )
				{
					final int frame = graph.getSpotFor( mxCell ).getFeature( Spot.FRAME ).intValue();
					if ( frame > maxFrame )
					{
						maxFrame = frame;
						cell = mxCell;
					}
				}
			}
			else
			{
				final List< mxCell > edges = getSelectionEdges( graph );
				if ( !edges.isEmpty() )
				{
					int maxFrame = Integer.MIN_VALUE;
					for ( final mxCell mxCell : edges )
					{
						final mxICell target = mxCell.getTarget();
						final int frame = graph.getSpotFor( target ).getFeature( Spot.FRAME ).intValue();
						if ( frame > maxFrame )
						{
							maxFrame = frame;
							cell = mxCell;
						}
					}
					cell = edges.get( edges.size() - 1 );
				}
				else
				{
					return;
				}
			}
			graphComponent.scrollCellToVisible( cell, true );
		}
	}
}
