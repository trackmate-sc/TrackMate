package fiji.plugin.trackmate.visualization.trackscheme.behaviours;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import org.scijava.ui.behaviour.util.AbstractNamedAction;

import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeGraphComponent;

public class PanAction extends AbstractNamedAction
{

	private static final long serialVersionUID = 1L;

	private final int amountx;

	private final int amounty;

	private final TrackSchemeGraphComponent graphComponent;

	public PanAction( final String name, final int amountx, final int amounty, final TrackSchemeGraphComponent graphComponent )
	{
		super( name );
		this.graphComponent = graphComponent;
		this.amountx = amountx;
		this.amounty = amounty;
	}

	@Override
	public void actionPerformed( final ActionEvent e )
	{
		final Rectangle r = graphComponent.getViewport().getViewRect();
		final int right = r.x + ( ( amountx < 0 ) ? 0 : r.width ) + amountx;
		final int bottom = r.y + ( ( amounty < 0 ) ? 0 : r.height ) + amounty;
		graphComponent.getGraphControl().scrollRectToVisible( new Rectangle( right, bottom, 0, 0 ) );
	}
}
