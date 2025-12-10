/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2025 TrackMate developers.
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
package fiji.plugin.trackmate.util.cli.condapath;

import static fiji.plugin.trackmate.gui.Icons.TRACKMATE_ICON;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

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
import fiji.plugin.trackmate.util.cli.condapath.CondaDetector.CondaInfo;
import fiji.plugin.trackmate.util.cli.condapath.CondaDetector.CondaNotFoundException;
import ij.IJ;
import ij.ImageJ;

@Plugin( type = Command.class,
		label = "Configure the path to the Conda executable used in TrackMate...",
		iconPath = "/icons/commands/information.png",
		menuPath = "Edit > Options > Configure TrackMate Conda path..." )
public class CondaPathConfigCommand implements Command
{

	@Override
	public void run()
	{
		SwingUtilities.invokeLater( () -> createAndShowDialog() );
	}

	private void createAndShowDialog()
	{
		final PrefService prefs = TMUtils.getContext().getService( PrefService.class );

		// Get or detect default paths
		String findPath;
		try
		{
			findPath = CLIUtils.findDefaultCondaPath();
		}
		catch ( final IllegalArgumentException e )
		{
			findPath = "/usr/local/opt/micromamba/bin/micromamba";
		}

		final String condaPath = prefs.get( CLIUtils.class, CLIUtils.CONDA_PATH_PREF_KEY, findPath );
		final Path path = Paths.get( condaPath );
		final Path parent = path.getParent();
		final Path parentOfParent = ( parent != null ) ? parent.getParent() : null;
		final String defaultValue = "/usr/local/opt/micromamba/";

		String condaRootPrefix = ( parentOfParent != null )
				? parentOfParent.toString()
				: defaultValue;
		condaRootPrefix = prefs.get( CLIUtils.class, CLIUtils.CONDA_ROOT_PREFIX_KEY, condaRootPrefix );

		// Create non-modal dialog
		final JDialog dialog = new JDialog( IJ.getInstance(), "TrackMate Conda Configuration", false );
		dialog.setIconImage( TRACKMATE_ICON.getImage() );
		dialog.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );

		// Main panel with padding
		final JPanel mainPanel = new JPanel( new BorderLayout( 10, 10 ) );
		mainPanel.setBorder( new EmptyBorder( 15, 15, 15, 15 ) );
		mainPanel.setBackground( Color.WHITE );

		// ========== Header Panel ==========
		final JPanel headerPanel = createHeaderPanel();
		mainPanel.add( headerPanel, BorderLayout.NORTH );

		// ========== Center Panel (Form) ==========
		final JPanel centerPanel = new JPanel();
		centerPanel.setLayout( new BoxLayout( centerPanel, BoxLayout.Y_AXIS ) );
		centerPanel.setBackground( Color.WHITE );

		// Status label for feedback
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
		statusArea.setText( "Configure conda paths or use auto-detection" );

		final JScrollPane statusScrollPane = new JScrollPane( statusArea );
		statusScrollPane.setBorder( BorderFactory.createEmptyBorder() );
		statusScrollPane.setMaximumSize( new Dimension( Integer.MAX_VALUE, 60 ) );
		centerPanel.add( statusScrollPane );
		centerPanel.add( Box.createVerticalStrut( 15 ) );

		// Conda executable path
		final JPanel execPanel = createPathPanel(
				"Conda Executable Path",
				"Path to the conda, mamba, or micromamba executable",
				condaPath );
		final JTextField execField = ( JTextField ) execPanel.getClientProperty( "textfield" );
		final JButton execBrowseButton = ( JButton ) execPanel.getClientProperty( "browse" );
		centerPanel.add( execPanel );
		centerPanel.add( Box.createVerticalStrut( 10 ) );

		// Conda root prefix
		final JPanel rootPanel = createPathPanel(
				"Conda Root Prefix",
				"Root directory of conda installation (CONDA_ROOT_PREFIX)",
				condaRootPrefix );
		final JTextField rootField = ( JTextField ) rootPanel.getClientProperty( "textfield" );
		final JButton rootBrowseButton = ( JButton ) rootPanel.getClientProperty( "browse" );
		centerPanel.add( rootPanel );
		centerPanel.add( Box.createVerticalStrut( 15 ) );

		// Browse button actions
		execBrowseButton.addActionListener( e -> browseForFile( execField, dialog ) );
		rootBrowseButton.addActionListener( e -> browseForDirectory( rootField, dialog ) );

		mainPanel.add( centerPanel, BorderLayout.CENTER );

		// ========== Button Panel ==========
		final JPanel buttonPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT, 10, 0 ) );
		buttonPanel.setBackground( Color.WHITE );

		final JButton autoDetectButton = new JButton( "Auto-detect" );
		autoDetectButton.setIcon( Icons.PREVIEW_ICON );
		autoDetectButton.addActionListener( e -> autoDetect( execField, rootField, statusArea ) );

		final JButton diagnoseButton = new JButton( "Diagnose" );
		diagnoseButton.setIcon( Icons.COG_ICON );
		diagnoseButton.addActionListener( e -> diagnose() );

		final JButton testButton = new JButton( "Test" );
		testButton.setIcon( Icons.EXECUTE_ICON );
		testButton.addActionListener( e -> test( execField.getText(), rootField.getText(), statusArea ) );

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

		// ========== Finalize Dialog ==========
		dialog.add( mainPanel );
		dialog.pack();
		dialog.setLocationRelativeTo( IJ.getInstance() );
		dialog.setVisible( true );
	}

	// ========== UI Component Factories ==========

	private JPanel createHeaderPanel()
	{
		final JPanel headerPanel = new JPanel( new BorderLayout( 10, 5 ) );
		headerPanel.setBackground( Color.WHITE );

		final JLabel iconLabel = new JLabel( GuiUtils.scaleImage( Icons.TRACKMATE_ICON, 48, 48 ) );
		headerPanel.add( iconLabel, BorderLayout.WEST );

		final JPanel textPanel = new JPanel();
		textPanel.setLayout( new BoxLayout( textPanel, BoxLayout.Y_AXIS ) );
		textPanel.setBackground( Color.WHITE );

		final JLabel titleLabel = new JLabel( "Conda Configuration" );
		titleLabel.setFont( Fonts.BIG_FONT );
		titleLabel.setAlignmentX( Component.LEFT_ALIGNMENT );

		final JLabel subtitleLabel = new JLabel(
				"Configure conda executable for TrackMate modules" );
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

	private JPanel createPathPanel( final String title, final String description, final String defaultPath )
	{
		final JPanel panel = new JPanel( new BorderLayout( 5, 5 ) );
		panel.setBackground( Color.WHITE );
		panel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder( title ),
				new EmptyBorder( 5, 5, 5, 5 ) ) );

		// Description
		final JLabel descLabel = new JLabel( description );
		descLabel.setFont( Fonts.SMALL_FONT );
		descLabel.setForeground( Color.GRAY );
		panel.add( descLabel, BorderLayout.NORTH );

		// Path input panel
		final JPanel inputPanel = new JPanel( new BorderLayout( 5, 0 ) );
		inputPanel.setBackground( Color.WHITE );

		final JTextField textField = new JTextField( defaultPath, 40 );
		textField.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );

		final JButton browseButton = new JButton( "Browse..." );
		browseButton.setFocusable( false );

		inputPanel.add( textField, BorderLayout.CENTER );
		inputPanel.add( browseButton, BorderLayout.EAST );

		panel.add( inputPanel, BorderLayout.CENTER );

		// Store components for later access
		panel.putClientProperty( "textfield", textField );
		panel.putClientProperty( "browse", browseButton );

		return panel;
	}

	// ========== Action Handlers ==========

	private void browseForFile( final JTextField textField, final JDialog parent )
	{
		final JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle( "Select Conda Executable" );
		chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );

		final String currentPath = textField.getText();
		if ( !currentPath.isEmpty() )
		{
			final File currentFile = new File( currentPath );
			if ( currentFile.getParentFile() != null && currentFile.getParentFile().exists() )
				chooser.setCurrentDirectory( currentFile.getParentFile() );
		}

		if ( chooser.showOpenDialog( parent ) == JFileChooser.APPROVE_OPTION )
		{
			final File selected = chooser.getSelectedFile();
			textField.setText( selected.getAbsolutePath() );
		}
	}

	private void browseForDirectory( final JTextField textField, final JDialog parent )
	{
		final JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle( "Select Conda Root Directory" );
		chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );

		final String currentPath = textField.getText();
		if ( !currentPath.isEmpty() )
		{
			final File currentDir = new File( currentPath );
			if ( currentDir.exists() )
				chooser.setCurrentDirectory( currentDir );
		}

		if ( chooser.showOpenDialog( parent ) == JFileChooser.APPROVE_OPTION )
		{
			final File selected = chooser.getSelectedFile();
			textField.setText( selected.getAbsolutePath() );
		}
	}

	private void autoDetect( final JTextField execField, final JTextField rootField, final JTextArea statusArea )
	{
		statusArea.setForeground( new Color( 60, 120, 180 ) );
		statusArea.setText( "Auto-detecting conda installation..." );

		new Thread( () -> {
			try
			{
				final CondaInfo condaInfo = CondaDetector.detect();
				SwingUtilities.invokeLater( () -> {
					execField.setText( condaInfo.getCondaExecutable() );
					rootField.setText( condaInfo.getRootPrefix() );
					statusArea.setForeground( new Color( 0, 128, 0 ) );
					statusArea.setText( String.format(
							"✓ Auto-detection successful!\nFound conda %s at: %s",
							condaInfo.getVersion(),
							condaInfo.getCondaExecutable() ) );
				} );
			}
			catch ( final CondaNotFoundException e )
			{
				SwingUtilities.invokeLater( () -> {
					statusArea.setForeground( new Color( 180, 0, 0 ) );
					statusArea.setText( "✗ Auto-detection failed:\n" + e.getMessage() );
				} );
			}
		}, "Conda-AutoDetect" ).start();
	}

	private void diagnose()
	{
		new Thread( () -> {
			IJ.log( "\n========== Conda Diagnostics ==========\n" );
			CondaDetector.diagnose();
		}, "Conda-Diagnose" ).start();
	}

	private void test( final String execPath, final String rootPath, final JTextArea statusArea )
	{
		statusArea.setForeground( new Color( 60, 120, 180 ) );
		statusArea.setText( "Testing conda configuration..." );

		new Thread( () -> {
			try
			{
				// Temporarily set the paths for testing
				final PrefService prefs = TMUtils.getContext().getService( PrefService.class );
				final String oldExec = prefs.get( CLIUtils.class, CLIUtils.CONDA_PATH_PREF_KEY, "" );
				final String oldRoot = prefs.get( CLIUtils.class, CLIUtils.CONDA_ROOT_PREFIX_KEY, "" );

				prefs.put( CLIUtils.class, CLIUtils.CONDA_PATH_PREF_KEY, execPath );
				prefs.put( CLIUtils.class, CLIUtils.CONDA_ROOT_PREFIX_KEY, rootPath );
				CLIUtils.clearEnvMap();

				final Map< String, String > map = CLIUtils.getEnvMap();
				final StringBuilder str = new StringBuilder();
				str.append( "✓ Test successful! Found " + map.size() + " environment(s):\n" );
				map.forEach( ( k, v ) -> str.append( String.format( "  • %s\n", k ) ) );

				SwingUtilities.invokeLater( () -> {
					statusArea.setForeground( new Color( 0, 128, 0 ) );
					statusArea.setText( str.toString() );
				} );

				IJ.log( "\n========== Conda Test Results ==========\n" + str.toString() );

				// Restore old values
				prefs.put( CLIUtils.class, CLIUtils.CONDA_PATH_PREF_KEY, oldExec );
				prefs.put( CLIUtils.class, CLIUtils.CONDA_ROOT_PREFIX_KEY, oldRoot );
				CLIUtils.clearEnvMap();
			}
			catch ( final IOException e )
			{
				SwingUtilities.invokeLater( () -> {
					statusArea.setForeground( new Color( 180, 0, 0 ) );
					statusArea.setText( "✗ Test failed:\nConda executable path seems incorrect.\n" + e.getMessage() );
				} );
				IJ.error( "Conda Test Failed",
						"Conda executable path seems to be incorrect.\nError: " + e.getMessage() );
			}
			catch ( final Exception e )
			{
				SwingUtilities.invokeLater( () -> {
					statusArea.setForeground( new Color( 180, 0, 0 ) );
					statusArea.setText( "✗ Test failed:\n" + e.getMessage() );
				} );
				e.printStackTrace();
				IJ.error( "Conda Test Failed",
						"Error when running conda.\nError: " + e.getMessage() );
			}
		}, "Conda-Test" ).start();
	}

	private void saveAndClose( final String execPath, final String rootPath, final PrefService prefs, final JDialog dialog )
	{
		prefs.put( CLIUtils.class, CLIUtils.CONDA_PATH_PREF_KEY, execPath );
		prefs.put( CLIUtils.class, CLIUtils.CONDA_ROOT_PREFIX_KEY, rootPath );
		CLIUtils.clearEnvMap();

		IJ.log( "Conda configuration saved:" );
		IJ.log( "  Executable: " + execPath );
		IJ.log( "  Root Prefix: " + rootPath );

		dialog.dispose();
	}

	// ========== Main for Testing ==========

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		TMUtils.getContext().getService( CommandService.class ).run( CondaPathConfigCommand.class, false );
	}
}
