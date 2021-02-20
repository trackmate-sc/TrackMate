package fiji.plugin.trackmate.gui.displaysettings;

import static fiji.plugin.trackmate.gui.Icons.APPLY_ICON;
import static fiji.plugin.trackmate.gui.Icons.RESET_ICON;
import static fiji.plugin.trackmate.gui.Icons.REVERT_ICON;
import static fiji.plugin.trackmate.gui.Icons.TRACKMATE_ICON;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import ij.ImageJ;

@Plugin( type = Command.class,
		label = "Configure TrackMate display settings...",
		iconPath = "/icons/commands/information.png",
		menuPath = "Edit >  Options > Configure TrackMate display settings..." )
public class ConfigTrackMateDisplaySettings implements Command
{

	private static final String APPLY_TOOLTIP = "<html>Save the current settings to the user default settings. "
			+ "They will be used in all the following TrackMate sessions.</html>";
	private static final String REVERT_TOOLTIP = "<html>Revert the current settings to the ones saved in the "
			+ "user default settings file.</html>";
	private static final String RESET_TOOLTIP = "<html>Reset the current settings to the built-in defaults.</html>";

	@Override
	public void run()
	{
		editor( DisplaySettingsIO.readUserDefault(),
				"Configure the default settings to be used by TrackMate.",
				"TrackMate user default settings" ).setVisible( true );
	}

	public static JFrame editor( final DisplaySettings ds, final String titleStr, final String frameName )
	{
		final JPanel configPanel = new JPanel();
		configPanel.setLayout( new BorderLayout() );

		/*
		 * Title.
		 */

		final JLabel title = new JLabel( "<html><i>"
				+ titleStr
				+ "</i></html>" );
		title.setBorder( BorderFactory.createEmptyBorder( 10, 5, 10, 5 ) );
		configPanel.add( title, BorderLayout.NORTH );

		/*
		 * Buttons.
		 */

		final JPanel panelButton = new JPanel();
		final BoxLayout panelButtonLayout = new BoxLayout( panelButton, BoxLayout.LINE_AXIS );
		panelButton.setLayout( panelButtonLayout );
		final JButton btnReset = new JButton( "Reset", RESET_ICON );
		btnReset.setToolTipText( RESET_TOOLTIP );
		final JButton btnRevert = new JButton( "Revert", REVERT_ICON );
		btnRevert.setToolTipText( REVERT_TOOLTIP );
		final JButton btnApply = new JButton( "Save to user defaults", APPLY_ICON );
		btnApply.setToolTipText( APPLY_TOOLTIP );
		panelButton.add( btnReset );
		panelButton.add( Box.createHorizontalStrut( 5 ) );
		panelButton.add( btnRevert );
		panelButton.add( Box.createHorizontalGlue() );
		panelButton.add( btnApply );
		panelButton.setBorder( BorderFactory.createEmptyBorder( 10, 5, 10, 5 ) );
		configPanel.add( panelButton, BorderLayout.SOUTH );

		/*
		 * Display settings editor.
		 */
		
		final DisplaySettingsPanel editor = new DisplaySettingsPanel( ds );
		final JScrollPane scrollPane = new JScrollPane( editor, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		scrollPane.setPreferredSize( new Dimension( 350, 500 ) );
		scrollPane.getVerticalScrollBar().setUnitIncrement( 16 );
		configPanel.add( scrollPane, BorderLayout.CENTER );
		
		/*
		 * Listeners.
		 */

		btnReset.addActionListener( e -> {
			ds.set( DisplaySettings.defaultStyle().copy( "User-default" ) );
			title.setText( "Reset the current settings to the built-in defaults." );
		} );
		btnRevert.addActionListener( e -> {
			ds.set( DisplaySettingsIO.readUserDefault() );
			title.setText( "Reverted the current settings to the user defaults." );
		} );
		btnApply.addActionListener( e -> {
			DisplaySettingsIO.saveToUserDefault( ds );
			title.setText( "Saved the current settings to the user defaults file." );
		} );

		/*
		 * Create and show frame.
		 */

		final JFrame frame = new JFrame( frameName );
		frame.setIconImage( TRACKMATE_ICON.getImage() );
		frame.getContentPane().add( configPanel );
		frame.pack();
		frame.setLocationRelativeTo( null );
		return frame;
	}

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		ImageJ.main( args );
		new ConfigTrackMateDisplaySettings().run();
	}
}
