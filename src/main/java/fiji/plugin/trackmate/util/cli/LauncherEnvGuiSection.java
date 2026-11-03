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
package fiji.plugin.trackmate.util.cli;

import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.linkedComboBoxSelector;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.linkedTextField;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.listElement;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.stringElement;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import fiji.plugin.trackmate.gui.Fonts;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.ListElement;
import fiji.plugin.trackmate.util.cli.Configurator.PathArgument;
import fiji.plugin.trackmate.util.cli.EnvCLIConfigurator.CondaEnvironmentCommand;
import fiji.plugin.trackmate.util.cli.EnvCLIConfigurator.PixiEnvironmentCommand;

/**
 * Builds the conda and pixi launcher sections of a {@link ConfigGuiBuilder}
 * panel. Package-private; used only by {@link ConfigGuiBuilder}.
 */
class LauncherEnvGuiSection
{

	private final ConfigGuiBuilder b;

	LauncherEnvGuiSection( final ConfigGuiBuilder builder )
	{
		this.b = builder;
	}

	void visitConda( final CondaEnvironmentCommand arg )
	{
		if ( arg.getEnvironments().isEmpty() )
		{
			// Compact label in dual-mode (pixi available); full error in conda-only mode.
			final boolean dualMode = b.panel.rdbtn != null;
			final JLabel lbl;
			if ( dualMode )
			{
				lbl = new JLabel( "Conda: not configured" );
				lbl.setFont( Fonts.SMALL_FONT );
				lbl.setForeground( Color.GRAY );
				lbl.setToolTipText( "Configure conda via Edit > Options > Configure TrackMate Conda path..." );
			}
			else
			{
				lbl = new JLabel( "<html>There was an error retrieving the list of conda environments."
						+ "<p>Did you configure Conda for TrackMate?"
						+ "<p>(Edit > Options > Configure TrackMate Conda path...)</html>" );
				lbl.setFont( Fonts.SMALL_FONT );
				lbl.setForeground( Color.RED );
				lbl.setPreferredSize( new Dimension( 200, 40 ) );
			}
			b.addToLayout( arg.getHelp(), lbl );
			return;
		}

		if ( !arg.isSet() )
		{
			if ( !arg.hasDefaultValue() )
				throw new IllegalArgumentException( "The GUI builder requires all arguments and commands "
						+ "to have a value or a default value. The argument '" + arg.getName() + "' misses both." );
			arg.set( arg.getDefaultValue() );
		}

		final ListElement< String > element = listElement( arg.getName(), arg.getEnvironments(), arg::getValue, arg::set );
		b.panel.elements.put( arg.getKey(), element );
		final JComboBox< String > comboBox = linkedComboBoxSelector( element );
		comboBox.setSelectedItem( arg.getValue() );
		b.addToLayout(
				arg.getHelp(),
				new JLabel( element.getLabel() ),
				comboBox,
				arg );
	}

	void visitPixi( final PixiEnvironmentCommand arg )
	{
		final PathArgument projectArg = arg.getProjectPathArg();
		final GridBagConstraints c = b.c;
		final int topInset = b.topInset;
		final int bottomInset = b.bottomInset;

		// Register path arg in panel elements so panel.refresh() syncs the field.
		final var pathElement = stringElement(
				"Pixi project folder", projectArg::getValue, projectArg::set );
		b.panel.elements.put( projectArg.getKey(), pathElement );

		final JRadioButton pixiRdbtn = b.panel.rdbtn;

		// Scan pixi projects from the configured root.
		final String root = CLIUtils.getPixiProjectsRoot();
		final java.util.List< File > foundProjects = root.isEmpty()
				? new java.util.ArrayList<>()
				: CLIUtils.findPixiProjectsInRoot( root );

		if ( foundProjects.size() > 1 )
		{
			// Multiple projects available: show comboboxes for project and env.
			final String[] projectPaths = foundProjects.stream()
					.map( File::getAbsolutePath ).toArray( String[]::new );
			final String[] projectNames = foundProjects.stream()
					.map( File::getName ).toArray( String[]::new );

			final JComboBox< String > projectCombo = new JComboBox<>( projectNames );
			projectCombo.setFont( Fonts.SMALL_FONT );
			projectCombo.setToolTipText( "Pixi project folder (contains pixi.toml)" );

			// Pre-select the currently configured project, or the first one.
			final String currentPath = projectArg.getValue();
			int initialIdx = 0;
			if ( currentPath != null && !currentPath.isEmpty() )
			{
				for ( int i = 0; i < projectPaths.length; i++ )
				{
					if ( projectPaths[ i ].equals( currentPath ) )
					{
						initialIdx = i;
						break;
					}
				}
			}
			projectCombo.setSelectedIndex( initialIdx );
			projectArg.set( projectPaths[ initialIdx ] );

			// Build env combobox.
			arg.refreshEnvs();
			if ( !arg.isSet() && !arg.getEnvironments().isEmpty() )
				arg.set( arg.getEnvironments().get( 0 ) );

			final ListElement< String > envElement = listElement(
					arg.getName(), arg.getEnvironments(), arg::getValue, arg::set );
			b.panel.elements.put( arg.getKey(), envElement );
			final JComboBox< String > envCombo = linkedComboBoxSelector( envElement );
			if ( arg.isSet() )
				envCombo.setSelectedItem( arg.getValue() );
			envCombo.setFont( Fonts.SMALL_FONT );

			// Refresh envs when project selection changes.
			projectCombo.addItemListener( e -> {
				if ( e.getStateChange() == ItemEvent.SELECTED )
				{
					final int idx = projectCombo.getSelectedIndex();
					projectArg.set( projectPaths[ idx ] );
					refreshEnvCombo( arg, envCombo );
				}
			} );

			// Sync combobox when panel.refresh() is called (e.g. from setSettings):
			// fromTrackMateSettings sets projectArg directly; without this hook
			// the combobox stays on its initial selection while the model holds "".
			pathElement.onSet( path -> {
				int idx = 0;
				if ( path != null )
				{
					for ( int i = 0; i < projectPaths.length; i++ )
					{
						if ( projectPaths[ i ].equals( path ) )
						{
							idx = i;
							break;
						}
					}
				}
				if ( projectCombo.getSelectedIndex() != idx )
				{
					// Selection changes → ItemListener fires → projectArg.set + refreshEnvCombo.
					projectCombo.setSelectedIndex( idx );
				}
				else
				{
					// Combobox already at correct index but envs may be from
					// a different project (built at construction time); always
					// refresh so the env list and selection are consistent.
					projectArg.set( projectPaths[ idx ] );
					refreshEnvCombo( arg, envCombo );
				}
			} );

			// Wire radio button.
			if ( pixiRdbtn != null )
			{
				pixiRdbtn.addItemListener( e -> {
					final boolean sel = pixiRdbtn.isSelected();
					projectCombo.setEnabled( sel );
					envCombo.setEnabled( sel );
				} );
				projectCombo.setEnabled( pixiRdbtn.isSelected() );
				envCombo.setEnabled( pixiRdbtn.isSelected() );
			}

			// Layout: project row.
			final JPanel projectHeader = new JPanel();
			projectHeader.setLayout( new BoxLayout( projectHeader, BoxLayout.LINE_AXIS ) );
			if ( pixiRdbtn != null )
				projectHeader.add( pixiRdbtn );
			final JLabel projectLabel = new JLabel( "Pixi project " );
			projectLabel.setFont( Fonts.SMALL_FONT );
			projectLabel.setToolTipText( "Select pixi project from the configured pixi projects root" );
			projectHeader.add( projectLabel );
			projectHeader.add( Box.createHorizontalGlue() );

			c.gridx = 0;
			c.insets = new Insets( topInset, 0, 0, 0 );
			c.gridwidth = 3;
			b.panel.add( projectHeader, c );
			c.gridy++;
			c.anchor = GridBagConstraints.LINE_START;
			c.insets = new Insets( 0, 0, bottomInset, 0 );
			b.panel.add( projectCombo, c );
			c.gridy++;

			// Layout: env row.
			final JPanel envHeader = new JPanel();
			envHeader.setLayout( new BoxLayout( envHeader, BoxLayout.LINE_AXIS ) );
			final JLabel envLabel = new JLabel( arg.getName() + " " );
			envLabel.setFont( Fonts.SMALL_FONT );
			if ( arg.getHelp() != null )
			{
				envLabel.setToolTipText( arg.getHelp() );
				envCombo.setToolTipText( arg.getHelp() );
			}
			envHeader.add( envLabel );
			envHeader.add( Box.createHorizontalGlue() );

			c.gridx = 0;
			c.insets = new Insets( topInset, 0, 0, 0 );
			c.gridwidth = 3;
			b.panel.add( envHeader, c );
			c.gridy++;
			c.anchor = GridBagConstraints.LINE_START;
			c.insets = new Insets( 0, 0, bottomInset, 0 );
			b.panel.add( envCombo, c );
			c.gridy++;
		}
		else
		{
			// Zero or one project: use the text-field based UI.
			final JTextField pathField = linkedTextField( pathElement );
			pathField.setColumns( 10 );
			pathField.setFont( Fonts.SMALL_FONT );

			// Auto-populate when exactly one project exists and path is not yet set.
			if ( ( projectArg.getValue() == null || projectArg.getValue().isEmpty() )
					&& foundProjects.size() == 1 )
			{
				final String path = foundProjects.get( 0 ).getAbsolutePath();
				projectArg.set( path );
				pathField.setText( path );
			}

			// Refresh list of environments now that the project path is set.
			arg.refreshEnvs();
			if ( !arg.isSet() && !arg.getEnvironments().isEmpty() )
				arg.set( arg.getEnvironments().get( 0 ) );

			final ListElement< String > envElement = listElement(
					arg.getName(), arg.getEnvironments(), arg::getValue, arg::set );
			b.panel.elements.put( arg.getKey(), envElement );
			final JComboBox< String > comboBox = linkedComboBoxSelector( envElement );
			if ( arg.isSet() )
				comboBox.setSelectedItem( arg.getValue() );
			comboBox.setFont( Fonts.SMALL_FONT );

			final JButton browseButton = new JButton( "browse" );
			browseButton.setFont( Fonts.SMALL_FONT );
			browseButton.addActionListener( e -> {
				final JFileChooser chooser = new JFileChooser();
				chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
				chooser.setDialogTitle( "Select pixi project folder (containing pixi.toml)" );
				final String current = pathField.getText();
				if ( !current.isEmpty() )
					chooser.setCurrentDirectory( new File( current ) );
				if ( chooser.showOpenDialog( b.panel ) == JFileChooser.APPROVE_OPTION )
				{
					pathField.setText( chooser.getSelectedFile().getAbsolutePath() );
					pathField.postActionEvent();
				}
			} );

			final JButton findButton = new JButton( "find" );
			findButton.setFont( Fonts.SMALL_FONT );
			findButton.setToolTipText( "Search the configured Pixi projects root and select a project." );
			findButton.addActionListener( e -> {
				final String r = CLIUtils.getPixiProjectsRoot();
				if ( r.isEmpty() )
				{
					JOptionPane.showMessageDialog( b.panel,
							"No pixi projects root configured.\n"
									+ "Set it in Edit > Options > Configure TrackMate Pixi path...",
							"Pixi projects root not set", JOptionPane.WARNING_MESSAGE );
					return;
				}
				final java.util.List< File > found = CLIUtils.findPixiProjectsInRoot( r );
				if ( found.isEmpty() )
				{
					JOptionPane.showMessageDialog( b.panel,
							"No pixi projects (subdirectories with pixi.toml) found in:\n" + r,
							"No projects found", JOptionPane.INFORMATION_MESSAGE );
					return;
				}
				final String picked = pickPixiProject( found );
				if ( picked != null )
				{
					pathField.setText( picked );
					pathField.postActionEvent();
				}
			} );

			// Refresh envs when path changes (Enter key or postActionEvent from browse/find).
			pathField.addActionListener( e -> refreshEnvCombo( arg, comboBox ) );

			final JButton refreshButton = new JButton( "refresh" );
			refreshButton.setFont( Fonts.SMALL_FONT );
			refreshButton.addActionListener( e -> {
				projectArg.set( pathField.getText().trim() );
				refreshEnvCombo( arg, comboBox );
			} );

			// Wire radio button enable/disable for all pixi components (dual-mode only).
			if ( pixiRdbtn != null )
			{
				pixiRdbtn.addItemListener( e -> {
					final boolean sel = pixiRdbtn.isSelected();
					pathField.setEnabled( sel );
					comboBox.setEnabled( sel );
					findButton.setEnabled( sel );
					browseButton.setEnabled( sel );
					refreshButton.setEnabled( sel );
				} );
				final boolean sel = pixiRdbtn.isSelected();
				pathField.setEnabled( sel );
				comboBox.setEnabled( sel );
				findButton.setEnabled( sel );
				browseButton.setEnabled( sel );
				refreshButton.setEnabled( sel );
			}

			// Layout: project directory row.
			final JPanel pathHeader = new JPanel();
			pathHeader.setLayout( new BoxLayout( pathHeader, BoxLayout.LINE_AXIS ) );
			if ( pixiRdbtn != null )
				pathHeader.add( pixiRdbtn );
			final JLabel pathLabel = new JLabel( "Pixi project folder " );
			pathLabel.setFont( Fonts.SMALL_FONT );
			pathLabel.setToolTipText( "Folder containing the pixi.toml file" );
			pathHeader.add( pathLabel );
			pathHeader.add( Box.createHorizontalGlue() );
			pathHeader.add( findButton );
			pathHeader.add( Box.createHorizontalStrut( 4 ) );
			pathHeader.add( browseButton );

			c.gridx = 0;
			c.insets = new Insets( topInset, 0, 0, 0 );
			c.gridwidth = 3;
			b.panel.add( pathHeader, c );
			c.gridy++;
			c.anchor = GridBagConstraints.LINE_START;
			c.insets = new Insets( 0, 0, bottomInset, 0 );
			b.panel.add( pathField, c );
			c.gridy++;

			// Layout: environment row.
			final JPanel envHeader = new JPanel();
			envHeader.setLayout( new BoxLayout( envHeader, BoxLayout.LINE_AXIS ) );
			final JLabel envLabel = new JLabel( arg.getName() + " " );
			envLabel.setFont( Fonts.SMALL_FONT );
			if ( arg.getHelp() != null )
			{
				envLabel.setToolTipText( arg.getHelp() );
				comboBox.setToolTipText( arg.getHelp() );
			}
			envHeader.add( envLabel );
			envHeader.add( Box.createHorizontalGlue() );
			envHeader.add( refreshButton );

			c.gridx = 0;
			c.insets = new Insets( topInset, 0, 0, 0 );
			c.gridwidth = 3;
			b.panel.add( envHeader, c );
			c.gridy++;
			c.anchor = GridBagConstraints.LINE_START;
			c.insets = new Insets( 0, 0, bottomInset, 0 );
			b.panel.add( comboBox, c );
			c.gridy++;
		}
	}

	private String pickPixiProject( final List< File > projects )
	{
		final String[] paths = projects.stream().map( File::getAbsolutePath ).toArray( String[]::new );
		final JList< String > list = new JList<>( paths );
		list.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		list.setSelectedIndex( 0 );
		final JScrollPane scroll = new JScrollPane( list );
		scroll.setPreferredSize( new Dimension( 500, 200 ) );
		final int result = JOptionPane.showConfirmDialog(
				b.panel, scroll,
				"Select pixi project", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE );
		if ( result == JOptionPane.OK_OPTION )
			return list.getSelectedValue();
		return null;
	}

	private void refreshEnvCombo( final PixiEnvironmentCommand arg, final JComboBox< String > comboBox )
	{
		final String currentEnv = arg.isSet() ? arg.getValue() : null;
		arg.refreshEnvs();
		comboBox.removeAllItems();
		arg.getEnvironments().forEach( comboBox::addItem );
		if ( !arg.getEnvironments().isEmpty() )
		{
			if ( currentEnv != null && !currentEnv.isEmpty() && arg.getEnvironments().contains( currentEnv ) )
			{
				comboBox.setSelectedItem( currentEnv );
				arg.set( currentEnv );
			}
			else
			{
				comboBox.setSelectedIndex( 0 );
				arg.set( ( String ) comboBox.getSelectedItem() );
			}
		}
	}
}
