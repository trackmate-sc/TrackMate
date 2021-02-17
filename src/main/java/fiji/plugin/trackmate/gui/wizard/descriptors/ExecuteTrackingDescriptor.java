package fiji.plugin.trackmate.gui.wizard.descriptors;

import org.scijava.Cancelable;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.LogPanel;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor2;

public class ExecuteTrackingDescriptor extends WizardPanelDescriptor2
{

	public static final String KEY = "ExecuteTracking";

	private final TrackMate trackmate;

	public ExecuteTrackingDescriptor( final TrackMate trackmate, final LogPanel logPanel )
	{
		super( KEY );
		this.trackmate = trackmate;
		this.targetPanel = logPanel;
	}

	@Override
	public Runnable getForwardRunnable()
	{
		return () -> trackmate.execTracking();
	}

	@Override
	public Cancelable getCancelable()
	{
		return trackmate;
	}
}
