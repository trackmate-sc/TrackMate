package fiji.plugin.trackmate.gui.wizard.descriptors;


import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor2;

public class SpotDetectorDescriptor extends WizardPanelDescriptor2
{

	private static final String KEY = "ConfigureDetector";

	public SpotDetectorDescriptor( final ConfigurationPanel configurationPanel )
	{
		super( KEY );
		this.targetPanel = configurationPanel;
	}
}
