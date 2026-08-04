package fiji.plugin.trackmate.visualization.hyperstack.behaviours.semiautotracking;

import java.util.ArrayList;

import javax.swing.JPanel;

import org.scijava.listeners.Listeners;
import org.scijava.ui.config.visitors.gui.GuiBuilder;
import org.scijava.ui.config.visitors.gui.GuiBuilder.ConfigPanel;

import bdv.ui.settings.ModificationListener;
import bdv.ui.settings.SettingsPage;

public class SpotEditToolSettingsPage implements SettingsPage
{

	private final String treePath;

	private final Listeners.List< ModificationListener > modificationListeners;

	private final SemiAutoTrackingParams tmpParams;

	private final ConfigPanel panel;

	public SpotEditToolSettingsPage( final String treePath, final SemiAutoTrackingParams params )
	{
		this.treePath = treePath;
		this.modificationListeners = new Listeners.SynchronizedList<>();
		this.tmpParams = new SemiAutoTrackingParams();
		this.panel = GuiBuilder.build( tmpParams );
		onApply( () -> params.set( tmpParams ) );
		onCancel( () -> tmpParams.set( params ) );
	}

	@Override
	public String getTreePath()
	{
		return treePath;
	}

	@Override
	public JPanel getJPanel()
	{
		return panel;
	}

	@Override
	public Listeners< ModificationListener > modificationListeners()
	{
		return modificationListeners;
	}

	protected final ArrayList< Runnable > runOnApply = new ArrayList<>();

	public synchronized void onApply( final Runnable runnable )
	{
		runOnApply.add( runnable );
	}

	protected final ArrayList< Runnable > runOnCancel = new ArrayList<>();

	public synchronized void onCancel( final Runnable runnable )
	{
		runOnCancel.add( runnable );
	}

	@Override
	public void cancel()
	{
		runOnCancel.forEach( Runnable::run );
	}

	@Override
	public void apply()
	{
		runOnApply.forEach( Runnable::run );
	}
}
