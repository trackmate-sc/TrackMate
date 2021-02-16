package fiji.plugin.trackmate.gui.wizard;

import java.awt.Component;

import org.scijava.Cancelable;

public class WizardPanelDescriptor2
{

	protected Component targetPanel;

	protected final String panelIdentifier;

	public WizardPanelDescriptor2( final String panelIdentifier )
	{
		this.panelIdentifier = panelIdentifier;
	}

	public final Component getPanelComponent()
	{
		return targetPanel;
	}

	public final String getPanelDescriptorIdentifier()
	{
		return panelIdentifier;
	}

	public void aboutToHidePanel()
	{}

	public void aboutToDisplayPanel()
	{}

	public void displayingPanel()
	{}

	public Runnable getForwardRunnable()
	{
		return null;
	}

	public Runnable getBackwardRunnable()
	{
		return null;
	}

	public Cancelable getCancelable()
	{
		return null;
	}
}
