package fiji.plugin.trackmate.gui.wizard.descriptors;

import org.scijava.Cancelable;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.components.LogPanel;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor;

public class ExecuteDetectionDescriptor extends WizardPanelDescriptor
{

	public static final String KEY = "ExecuteDetection";

	private final TrackMate trackmate;

	public ExecuteDetectionDescriptor( final TrackMate trackmate, final LogPanel logPanel )
	{
		super( KEY );
		this.trackmate = trackmate;
		this.targetPanel = logPanel;
	}

	@Override
	public Runnable getForwardRunnable()
	{
		return () -> {
			final long start = System.currentTimeMillis();
			trackmate.execDetection();
			final long end = System.currentTimeMillis();
			trackmate.getModel().getLogger().log( String.format( "Detection done in %.1f s.\n", ( end - start ) / 1e3f ) );
		};
	}

	@Override
	public Cancelable getCancelable()
	{
		return trackmate;
	}
}
