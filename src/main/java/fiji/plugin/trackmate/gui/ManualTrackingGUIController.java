package fiji.plugin.trackmate.gui;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.descriptors.WizardPanelDescriptor;

public class ManualTrackingGUIController extends TrackMateGUIController
{

	public ManualTrackingGUIController( final TrackMate trackmate, final DisplaySettings displaySettings, final SelectionModel selectionModel )
	{
		super( trackmate, displaySettings, selectionModel );
	}

	@Override
	protected WizardPanelDescriptor getFirstDescriptor()
	{
		return configureViewsDescriptor;
	}

	@Override
	protected WizardPanelDescriptor previousDescriptor( final WizardPanelDescriptor currentDescriptor )
	{
		if ( currentDescriptor == configureViewsDescriptor )
			return null;

		return super.previousDescriptor( currentDescriptor );
	}
}
