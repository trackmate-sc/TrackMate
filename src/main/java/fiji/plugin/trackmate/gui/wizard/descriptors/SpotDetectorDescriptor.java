package fiji.plugin.trackmate.gui.wizard.descriptors;


import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor2;

public class SpotDetectorDescriptor extends WizardPanelDescriptor2
{

	public static final String KEY = "ConfigureDetector";

	private final Settings settings;

	public SpotDetectorDescriptor( final Settings settings, final ConfigurationPanel configurationPanel )
	{
		super( KEY );
		this.settings = settings;
		this.targetPanel = configurationPanel;
	}

	@Override
	public void aboutToHidePanel()
	{
		final ConfigurationPanel configurationPanel = ( ConfigurationPanel ) targetPanel;
		settings.detectorSettings = configurationPanel.getSettings();
	}
}
