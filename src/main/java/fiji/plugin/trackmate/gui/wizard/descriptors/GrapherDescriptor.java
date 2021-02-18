package fiji.plugin.trackmate.gui.wizard.descriptors;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.GrapherPanel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor2;

public class GrapherDescriptor extends WizardPanelDescriptor2
{

	private static final String KEY = "GraphFeatures";

	public GrapherDescriptor( final TrackMate trackmate, final DisplaySettings displaySettings )
	{
		super( KEY );
		this.targetPanel = new GrapherPanel( trackmate, displaySettings );
	}

	@Override
	public void aboutToDisplayPanel()
	{
		final GrapherPanel panel = ( GrapherPanel ) targetPanel;
		panel.refresh();
	}
}
