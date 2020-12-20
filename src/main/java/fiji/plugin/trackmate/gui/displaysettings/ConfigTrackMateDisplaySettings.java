package fiji.plugin.trackmate.gui.displaysettings;

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

import fiji.plugin.trackmate.gui.TrackMateWizard;
import ij.ImageJ;

@Plugin( type = Command.class,
		label = "Configure TrackMate display settings...",
		iconPath = "/icons/commands/information.png",
		menuPath = "Edit >  Options > Configure TrackMate display settings..." )
public class ConfigTrackMateDisplaySettings implements Command
{

	@Override
	public void run()
	{
		final JPanel configPanel = new JPanel();
		configPanel.setLayout( new BorderLayout() );

		/*
		 * Title.
		 */

		final JLabel title = new JLabel( "<html><i>"
				+ "Configure the default settings to be used by TrackMate. "
				+ "</i></html>" );
		title.setBorder( BorderFactory.createEmptyBorder( 10, 5, 10, 5 ) );
		configPanel.add( title, BorderLayout.NORTH );

		/*
		 * Buttons.
		 */

		final JPanel panelButton = new JPanel();
		final BoxLayout panelButtonLayout = new BoxLayout( panelButton, BoxLayout.LINE_AXIS );
		panelButton.setLayout( panelButtonLayout );
		final JButton btnReset = new JButton( "Reset" );
		final JButton btnRevert = new JButton( "Revert" );
		final JButton btnApply = new JButton( "Apply" );
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
		
		final DisplaySettings ds = DisplaySettingsIO.readUserDefault();
		final DisplaySettingsPanel editor = new DisplaySettingsPanel( ds );
		final JScrollPane scrollPane = new JScrollPane( editor, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		scrollPane.setPreferredSize( new Dimension( 350, 500 ) );
		scrollPane.getVerticalScrollBar().setUnitIncrement( 16 );
		configPanel.add( scrollPane, BorderLayout.CENTER );
		
		/*
		 * Listeners.
		 */

		btnReset.addActionListener( e -> ds.set( DisplaySettings.defaultStyle().copy( "User-default" ) ) );
		btnRevert.addActionListener( e -> ds.set( DisplaySettingsIO.readUserDefault() ) );
		btnApply.addActionListener( e -> DisplaySettingsIO.saveToUserDefault( ds ) );

		/*
		 * Create and show frame.
		 */

		final JFrame frame = new JFrame( "TrackMate default settings" );
		frame.setIconImage( TrackMateWizard.TRACKMATE_ICON.getImage() );
		frame.getContentPane().add( configPanel );
		frame.pack();
		frame.setLocationRelativeTo( null );
		frame.setVisible( true );
	}

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		ImageJ.main( args );
		new ConfigTrackMateDisplaySettings().run();
	}
}
