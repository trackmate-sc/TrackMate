/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.util.cli.pixipath;

import static fiji.plugin.trackmate.gui.Icons.TRACKMATE_ICON;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;

import fiji.plugin.trackmate.gui.Fonts;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.util.cli.CLIUtils;
import fiji.plugin.trackmate.util.cli.pixipath.PixiDetector.PixiInfo;
import fiji.plugin.trackmate.util.cli.pixipath.PixiDetector.PixiNotFoundException;
import ij.IJ;
import ij.ImageJ;

@Plugin( type = Command.class,
		label = "Configure the path to the Pixi executable used in TrackMate...",
		iconPath = "/icons/commands/information.png",
		menuPath = "Edit > Options > Configure TrackMate Pixi path..." )
public class PixiPathConfigCommand implements Command
{

	@Override
	public void run()
	{
		SwingUtilities.invokeLater( () -> createAndShowDialog() );
	}

	private void createAndShowDialog()
	{
		final PrefService prefs = TMUtils.getContext().getService( PrefService.class );

		String findPath;
		try
		{
			findPath = CLIUtils.findDefaultPixiPath();
		}
		catch ( final IllegalArgumentException e )
		{
			findPath = System.getProperty( "user.home" ) + "/.pixi/bin/pixi";
		}
		final String pixiPath = prefs.get( CLIUtils.class, CLIUtils.PIXI_PATH_PREF_KEY, findPath );
		final String projectsRoot = prefs.get( CLIUtils.class, CLIUtils.PIXI_PROJECTS_ROOT_KEY, "" );

		final JDialog dialog = new JDialog( IJ.getInstance(), "TrackMate Pixi Configuration", false );
		dialog.setIconImage( TRACKMATE_ICON.getImage() );
		dialog.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );

		final JPanel mainPanel = new JPanel( new BorderLayout( 10, 10 ) );
		mainPanel.setBorder( new EmptyBorder( 15, 15, 15, 15 ) );
		mainPanel.setBackground( Color.WHITE );

		mainPanel.add( createHeaderPanel(), BorderLayout.NORTH );

		final JPanel centerPanel = new JPanel();
		centerPanel.setLayout( new BoxLayout( centerPanel, BoxLayout.Y_AXIS ) );
		centerPanel.setBackground( Color.WHITE );

		final JTextArea statusArea = new JTextArea( 2, 50 );
		statusArea.setEditable( false );
		statusArea.setLineWrap( true );
		statusArea.setWrapStyleWord( true );
		statusArea.setFont( new Font( "SansSerif", Font.PLAIN, 11 ) );
		statusArea.setForeground( new Color( 60, 120, 180 ) );
		statusArea.setBackground( new Color( 240, 248, 255 ) );
		statusArea.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder( new Color( 180, 200, 220 ) ),
				new EmptyBorder( 5, 8, 5, 8 ) ) );
		statusArea.setText( "Configure pixi paths below" );

		final JScrollPane statusScrollPane = new JScrollPane( statusArea );
		statusScrollPane.setBorder( BorderFactory.createEmptyBorder() );
		centerPanel.add( statusScrollPane );
		centerPanel.add( Box.createVerticalStrut( 15 ) );

		final JPanel execPanel = createPathPanel(
				"Pixi Executable Path",
				"Path to the pixi executable (e.g. ~/.pixi/bin/pixi)",
				pixiPath,
				false );
		final JTextField execField = ( JTextField ) execPanel.getClientProperty( "textfield" );
		final JButton execBrowseButton = ( JButton ) execPanel.getClientProperty( "browse" );
		centerPanel.add( execPanel );
		centerPanel.add( Box.createVerticalStrut( 10 ) );

		final JPanel rootPanel = createPathPanel(
				"Pixi Projects Root",
				"Folder whose subdirectories are pixi projects (each contains a pixi.toml). Used by the \"find\" button in detector panels.",
				projectsRoot,
				true );
		final JTextField rootField = ( JTextField ) rootPanel.getClientProperty( "textfield" );
		final JButton rootBrowseButton = ( JButton ) rootPanel.getClientProperty( "browse" );
		centerPanel.add( rootPanel );
		centerPanel.add( Box.createVerticalStrut( 15 ) );

		execBrowseButton.addActionListener( e -> browseFor( execField, dialog, false ) );
		rootBrowseButton.addActionListener( e -> browseFor( rootField, dialog, true ) );

		mainPanel.add( centerPanel, BorderLayout.CENTER );

		final JPanel buttonPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT, 10, 0 ) );
		buttonPanel.setBackground( Color.WHITE );

		final JButton autoDetectButton = new JButton( "Auto-detect" );
		autoDetectButton.setIcon( Icons.PREVIEW_ICON );
		autoDetectButton.addActionListener( e -> autoDetect( execField, statusArea ) );

		final JButton diagnoseButton = new JButton( "Diagnose" );
		diagnoseButton.setIcon( Icons.COG_ICON );
		diagnoseButton.addActionListener( e -> diagnose() );

		final JButton testButton = new JButton( "Test" );
		testButton.setIcon( Icons.EXECUTE_ICON );
		testButton.addActionListener( e -> test( execField.getText(), statusArea ) );

		final JButton okButton = new JButton( "OK" );
		okButton.addActionListener( e -> saveAndClose( execField.getText(), rootField.getText(), prefs, dialog ) );

		final JButton cancelButton = new JButton( "Cancel" );
		cancelButton.addActionListener( e -> dialog.dispose() );

		buttonPanel.add( autoDetectButton );
		buttonPanel.add( diagnoseButton );
		buttonPanel.add( testButton );
		buttonPanel.add( Box.createHorizontalStrut( 20 ) );
		buttonPanel.add( okButton );
		buttonPanel.add( cancelButton );

		mainPanel.add( buttonPanel, BorderLayout.SOUTH );

		dialog.add( mainPanel );
		dialog.pack();
		dialog.setLocationRelativeTo( IJ.getInstance() );
		dialog.setVisible( true );
	}

	private JPanel createHeaderPanel()
	{
		final JPanel headerPanel = new JPanel( new BorderLayout( 10, 5 ) );
		headerPanel.setBackground( Color.WHITE );

		final JLabel iconLabel = new JLabel( GuiUtils.scaleImage( Icons.TRACKMATE_ICON, 48, 48 ) );
		headerPanel.add( iconLabel, BorderLayout.WEST );

		final JPanel textPanel = new JPanel();
		textPanel.setLayout( new BoxLayout( textPanel, BoxLayout.Y_AXIS ) );
		textPanel.setBackground( Color.WHITE );

		final JLabel titleLabel = new JLabel( "Pixi Configuration" );
		titleLabel.setFont( Fonts.BIG_FONT );
		titleLabel.setAlignmentX( Component.LEFT_ALIGNMENT );

		final JLabel subtitleLabel = new JLabel( "Configure pixi executable for TrackMate" );
		subtitleLabel.setFont( Fonts.SMALL_FONT );
		subtitleLabel.setForeground( Color.GRAY );
		subtitleLabel.setAlignmentX( Component.LEFT_ALIGNMENT );

		textPanel.add( titleLabel );
		textPanel.add( Box.createVerticalStrut( 3 ) );
		textPanel.add( subtitleLabel );

		headerPanel.add( textPanel, BorderLayout.CENTER );
		headerPanel.add( Box.createVerticalStrut( 10 ), BorderLayout.SOUTH );

		return headerPanel;
	}

	private JPanel createPathPanel( final String title, final String description, final String defaultPath, final boolean directory )
	{
		final JPanel panel = new JPanel( new BorderLayout( 5, 5 ) );
		panel.setBackground( Color.WHITE );
		panel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder( title ),
				new EmptyBorder( 5, 5, 5, 5 ) ) );

		final JLabel descLabel = new JLabel( description );
		descLabel.setFont( Fonts.SMALL_FONT );
		descLabel.setForeground( Color.GRAY );
		panel.add( descLabel, BorderLayout.NORTH );

		final JPanel inputPanel = new JPanel( new BorderLayout( 5, 0 ) );
		inputPanel.setBackground( Color.WHITE );

		final JTextField textField = new JTextField( defaultPath, 40 );
		textField.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );

		final JButton browseButton = new JButton( "Browse..." );
		browseButton.setFocusable( false );

		inputPanel.add( textField, BorderLayout.CENTER );
		inputPanel.add( browseButton, BorderLayout.EAST );

		panel.add( inputPanel, BorderLayout.CENTER );
		panel.putClientProperty( "textfield", textField );
		panel.putClientProperty( "browse", browseButton );

		return panel;
	}

	private void browseFor( final JTextField textField, final JDialog parent, final boolean directory )
	{
		final JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode( directory ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY );
		chooser.setDialogTitle( directory ? "Select pixi projects root folder" : "Select pixi executable" );

		final String currentPath = textField.getText();
		if ( !currentPath.isEmpty() )
		{
			final File current = new File( currentPath );
			final File startDir = directory ? current : ( current.getParentFile() != null ? current.getParentFile() : current );
			if ( startDir.exists() )
				chooser.setCurrentDirectory( startDir );
		}

		if ( chooser.showOpenDialog( parent ) == JFileChooser.APPROVE_OPTION )
			textField.setText( chooser.getSelectedFile().getAbsolutePath() );
	}

	private void autoDetect( final JTextField execField, final JTextArea statusArea )
	{
		statusArea.setForeground( new Color( 60, 120, 180 ) );
		statusArea.setText( "Auto-detecting pixi installation..." );

		new Thread( () -> {
			try
			{
				final PixiInfo info = PixiDetector.detect();
				SwingUtilities.invokeLater( () -> {
					execField.setText( info.getPixiExecutable() );
					statusArea.setForeground( new Color( 0, 128, 0 ) );
					statusArea.setText( String.format(
							"Auto-detection successful! Found pixi %s at: %s",
							info.getVersion(),
							info.getPixiExecutable() ) );
				} );
			}
			catch ( final PixiNotFoundException e )
			{
				SwingUtilities.invokeLater( () -> {
					statusArea.setForeground( new Color( 180, 0, 0 ) );
					statusArea.setText( "Auto-detection failed: " + e.getMessage() );
				} );
			}
		}, "Pixi-AutoDetect" ).start();
	}

	private void diagnose()
	{
		new Thread( () -> {
			IJ.log( "\n========== Pixi Diagnostics ==========\n" );
			PixiDetector.diagnose();
		}, "Pixi-Diagnose" ).start();
	}

	private void test( final String execPath, final JTextArea statusArea )
	{
		statusArea.setForeground( new Color( 60, 120, 180 ) );
		statusArea.setText( "Testing pixi executable..." );

		new Thread( () -> {
			try
			{
				final ProcessBuilder pb = new ProcessBuilder( execPath, "--version" );
				pb.redirectErrorStream( true );
				final Process process = pb.start();
				final StringBuilder sb = new StringBuilder();
				try ( final java.io.BufferedReader reader = new java.io.BufferedReader(
						new java.io.InputStreamReader( process.getInputStream() ) ) )
				{
					String line;
					while ( ( line = reader.readLine() ) != null )
						sb.append( line );
				}
				final int exit = process.waitFor();
				SwingUtilities.invokeLater( () -> {
					if ( exit == 0 )
					{
						statusArea.setForeground( new Color( 0, 128, 0 ) );
						statusArea.setText( "Test successful: " + sb.toString().trim() );
					}
					else
					{
						statusArea.setForeground( new Color( 180, 0, 0 ) );
						statusArea.setText( "Test failed (exit " + exit + "): " + sb.toString().trim() );
					}
				} );
			}
			catch ( final Exception e )
			{
				SwingUtilities.invokeLater( () -> {
					statusArea.setForeground( new Color( 180, 0, 0 ) );
					statusArea.setText( "Test failed: " + e.getMessage() );
				} );
			}
		}, "Pixi-Test" ).start();
	}

	private void saveAndClose( final String execPath, final String projectsRoot, final PrefService prefs, final JDialog dialog )
	{
		prefs.put( CLIUtils.class, CLIUtils.PIXI_PATH_PREF_KEY, execPath );
		prefs.put( CLIUtils.class, CLIUtils.PIXI_PROJECTS_ROOT_KEY, projectsRoot );
		PixiDetector.clearCache();

		IJ.log( "Pixi configuration saved: executable = " + execPath );
		IJ.log( "Pixi projects root = " + ( projectsRoot.isEmpty() ? "(not set)" : projectsRoot ) );

		dialog.dispose();
	}

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		TMUtils.getContext().getService( CommandService.class ).run( PixiPathConfigCommand.class, false );
	}
}
