package fiji.plugin.trackmate.gui.wizard.descriptors;

import fiji.plugin.trackmate.gui.components.LogPanel;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor2;

public class LogPanelDescriptor2 extends WizardPanelDescriptor2
{

	public static final String KEY = "LogPanel";

	public LogPanelDescriptor2( final LogPanel logPanel )
	{
		super( KEY );
		this.targetPanel = logPanel;
	}
}
