package fiji.plugin.trackmate.gui.wizard.descriptors;


import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor2;

public class SpotTrackerDescriptor extends WizardPanelDescriptor2
{

	private static final String KEY = "ConfigureTracker";

	private final Settings settings;

	public SpotTrackerDescriptor( final Settings settings, final ConfigurationPanel configurationPanel )
	{
		super( KEY );
		this.settings = settings;
		this.targetPanel = configurationPanel;
	}

	@Override
	public void aboutToHidePanel()
	{
		final ConfigurationPanel configurationPanel = ( ConfigurationPanel ) targetPanel;
		settings.trackerSettings = configurationPanel.getSettings();
	}
}
