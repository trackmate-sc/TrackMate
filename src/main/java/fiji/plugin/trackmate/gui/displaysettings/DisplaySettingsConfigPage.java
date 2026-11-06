package fiji.plugin.trackmate.gui.displaysettings;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.WindowConstants;

import bdv.ui.settings.SelectAndEditProfileSettingsPage;
import bdv.ui.settings.SettingsPanel;
import bdv.ui.settings.style.StyleProfile;
import bdv.ui.settings.style.StyleProfileManager;

public class DisplaySettingsConfigPage extends SelectAndEditProfileSettingsPage< StyleProfile< DisplaySettings > >
{

	public DisplaySettingsConfigPage( final String treePath, final DisplaySettingsManager displaySettingsManager )
	{
		super(
				treePath,
				new StyleProfileManager<>( displaySettingsManager, new DisplaySettingsManager( null, false ) ),
				new DisplaySettingsPanel( displaySettingsManager.getSelectedStyle() ) );
	}

	public static void main( final String[] args )
	{
		final DisplaySettingsManager styleManager = new DisplaySettingsManager();

		final SettingsPanel settings = new SettingsPanel();
		settings.addPage( new DisplaySettingsConfigPage( "Display settings", styleManager ) );

		final JDialog dialog = new JDialog( ( Frame ) null, "Settings" );
		dialog.getContentPane().add( settings, BorderLayout.CENTER );
		dialog.pack();
		dialog.setLocationRelativeTo( null );

		settings.onOk( () -> dialog.setVisible( false ) );
		settings.onCancel( () -> dialog.setVisible( false ) );

		dialog.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		dialog.addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				settings.cancel();
			}
		} );
		dialog.setVisible( true );
	}
}
