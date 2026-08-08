package fiji.plugin.trackmate.visualization.hyperstack.behaviours.semiautotracking;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;

import javax.swing.JButton;
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

	private final JPanel mainPanel;

	public SpotEditToolSettingsPage( final String treePath, final SemiAutoTrackingParams params )
	{
		this.treePath = treePath;
		this.modificationListeners = new Listeners.SynchronizedList<>();
		this.tmpParams = new SemiAutoTrackingParams();
		tmpParams.set( params );
		final ConfigPanel configPanel = GuiBuilder.build( tmpParams );
		tmpParams.updateListeners().add( () -> modificationListeners.list.forEach( ModificationListener::setModified ) );
		onApply( () -> {
			params.set( tmpParams );
			SemiAutoTrackingParamsIO.savePrefs( params );
		} );
		onCancel( () -> {
			tmpParams.set( params );
			configPanel.refresh();
		} );

		this.mainPanel = new JPanel();
		mainPanel.setLayout( new BorderLayout() );
		mainPanel.add( configPanel, BorderLayout.CENTER );
		final JPanel buttonPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT, 5, 5 ) );
		final JButton reset = new JButton( "Reset" );
		reset.addActionListener( e -> {
			tmpParams.set( new SemiAutoTrackingParams() );
			configPanel.refresh();
		} );
		buttonPanel.add( reset );
		mainPanel.add( buttonPanel, BorderLayout.SOUTH );
	}

	@Override
	public String getTreePath()
	{
		return treePath;
	}

	@Override
	public JPanel getJPanel()
	{
		return mainPanel;
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
