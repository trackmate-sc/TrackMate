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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Common base for CLI configurators that run a tool inside a managed Python
 * environment — either a <b>conda</b> environment or a <b>pixi</b> project
 * environment.
 * <p>
 * The no-arg constructor registers <em>both</em> env modes and exposes them via
 * a {@link Configurator.SelectableArguments} group keyed {@code "LAUNCHER"}.
 * The user picks conda or pixi in the config panel; settings serialization
 * preserves the choice.
 * <p>
 * The single-mode {@link #EnvCLIConfigurator(Launcher)} constructor keeps the
 * old behavior for backward-compatible subclasses such as
 * {@link CondaCLIConfigurator}.
 *
 * @author Jean-Yves Tinevez, Laurent Guerard
 */
public abstract class EnvCLIConfigurator extends CLIConfigurator
{

	public enum Launcher
	{
		CONDA, PIXI
	}

	public static final String KEY_CONDA_ENV = "CONDA_ENV";

	public static final String KEY_PIXI_ENV = "PIXI_ENV";

	public static final String KEY_PIXI_PROJECT = "PIXI_PROJECT";

	/** Key stored in the settings map to identify the active launcher. */
	public static final String KEY_LAUNCHER = "LAUNCHER";

	// =========================================================================
	// CondaEnvironmentCommand
	// =========================================================================

	public static class CondaEnvironmentCommand extends AbstractStringArgument< CondaEnvironmentCommand >
	{

		private final List< String > envs = new ArrayList<>();

		protected CondaEnvironmentCommand()
		{
			name( "Conda environment" );
			help( "The conda environment in which the tool is configured." );
			key( KEY_CONDA_ENV );
			defaultValue( "base" );
			required( true );
		}

		protected CondaEnvironmentCommand addEnvironment( final String env )
		{
			if ( !envs.contains( env ) )
				envs.add( env );
			return this;
		}

		@Override
		public void set( final String env )
		{
			if ( envs.isEmpty() )
			{
				super.set( env );
				return;
			}
			final int sel = envs.indexOf( env );
			if ( sel < 0 )
			{
				super.set( envs.get( 0 ) );
				return;
			}
			super.set( env );
		}

		public void set( final int selected )
		{
			if ( envs.isEmpty() )
				return;
			if ( selected < 0 || selected >= envs.size() )
				set( envs.get( 0 ) );
			else
				set( envs.get( selected ) );
		}

		public List< String > getEnvironments()
		{
			return envs;
		}

		@Override
		public void accept( final ArgumentVisitor visitor )
		{
			visitor.visit( this );
		}
	}

	// =========================================================================
	// PixiEnvironmentCommand
	// =========================================================================

	public static class PixiEnvironmentCommand extends AbstractStringArgument< PixiEnvironmentCommand >
	{

		private final PathArgument projectPathArg;

		private final List< String > envs = new ArrayList<>();

		protected PixiEnvironmentCommand( final PathArgument projectPathArg )
		{
			this.projectPathArg = projectPathArg;
			name( "Pixi environment" );
			help( "The environment within the pixi project to run the tool in." );
			key( KEY_PIXI_ENV );
			defaultValue( "" );
			required( true );
		}

		public PathArgument getProjectPathArg()
		{
			return projectPathArg;
		}

		public void refreshEnvs()
		{
			envs.clear();
			final String projectDir = projectPathArg.getValue();
			if ( projectDir == null || projectDir.isEmpty() )
				return;
			final Path envsPath = Paths.get( projectDir, ".pixi", "envs" );
			if ( Files.isDirectory( envsPath ) )
			{
				try
				{
					Files.list( envsPath ).forEach( p -> {
						if ( Files.isDirectory( p ) )
							envs.add( p.getFileName().toString() );
					} );
					envs.sort( null );
				}
				catch ( final IOException e )
				{
					System.err.println( "Could not list pixi environments in: " + envsPath );
				}
			}
		}

		public List< String > getEnvironments()
		{
			return envs;
		}

		@Override
		public void set( final String env )
		{
			// Always accept without validating against envs — envs may be empty
			// or from a different project at deserialization time.
			// refreshEnvCombo() corrects invalid values after envs reload.
			super.set( env );
		}

		public void set( final int selected )
		{
			if ( envs.isEmpty() )
				return;
			if ( selected < 0 || selected >= envs.size() )
				set( envs.get( 0 ) );
			else
				set( envs.get( selected ) );
		}

		@Override
		public void accept( final ArgumentVisitor visitor )
		{
			visitor.visit( this );
		}
	}

	// =========================================================================
	// EnvCLIConfigurator
	// =========================================================================

	private final CondaEnvironmentCommand condaCmd;

	private final PixiEnvironmentCommand pixiCmd;

	/** Non-null only in dual-mode (no-arg constructor). */
	private final SelectableArguments selectableLauncher;

	/**
	 * Dual-mode constructor. Registers both conda and pixi env commands in the
	 * arguments list and exposes them as a {@link SelectableArguments} group.
	 * The user can pick conda or pixi in the config panel.
	 */
	protected EnvCLIConfigurator()
	{
		super();
		this.condaCmd = setupConda();
		this.pixiCmd = setupPixi();

		// Mark env commands as not CLI args (prefix is built via getCommandArg).
		condaCmd.inCLI( false );
		pixiCmd.inCLI( false );

		// Insert condaCmd before pixiProject (already added by addPathArgument).
		arguments.add( 0, condaCmd );
		arguments.add( pixiCmd );

		this.selectableLauncher = addSelectableArguments()
				.add( condaCmd )
				.add( pixiCmd )
				.key( KEY_LAUNCHER );

		// Default to pixi when a projects root is already configured.
		try
		{
			if ( !CLIUtils.getPixiProjectsRoot().isEmpty() )
				selectableLauncher.select( 1 );
		}
		catch ( final Exception e )
		{
			// Context not yet available; keep default (conda).
		}
	}

	/**
	 * Single-mode constructor for backward-compatible subclasses (e.g.
	 * {@link CondaCLIConfigurator}). Only the specified launcher is registered;
	 * no {@link SelectableArguments} is created.
	 *
	 * @param launcher
	 *            the fixed launcher for this configurator.
	 */
	protected EnvCLIConfigurator( final Launcher launcher )
	{
		super();
		this.selectableLauncher = null;
		if ( launcher == Launcher.CONDA )
		{
			this.condaCmd = setupConda();
			this.pixiCmd = null;
		}
		else
		{
			this.condaCmd = null;
			this.pixiCmd = setupPixi();
		}
	}

	private CondaEnvironmentCommand setupConda()
	{
		final List< String > envList = new ArrayList<>();
		try
		{
			envList.addAll( CLIUtils.getEnvList() );
		}
		catch ( final Exception e )
		{
			System.err.println( "There was an error retrieving the list of conda environments.\n"
					+ "Did you configure Conda or Pixi for TrackMate? (Edit > Options > Configure TrackMate Conda path... or "
					+ "Configure TrackMate Pixi path...)" );
			e.printStackTrace();
		}
		final CondaEnvironmentCommand cmd = new CondaEnvironmentCommand();
		envList.forEach( cmd::addEnvironment );
		cmd.key( KEY_CONDA_ENV );
		if ( envList.isEmpty() )
			cmd.set( "base" ); // Ensure non-null value when conda is not configured
		else
			cmd.set( 0 );

		setCommandTranslator( cmd, s -> {
			final List< String > tokens = new ArrayList<>();
			final String condaPath = CLIUtils.getCondaPath();
			final String os = System.getProperty( "os.name" ).toLowerCase();
			if ( os.contains( "win" ) )
				tokens.addAll( Arrays.asList( "cmd.exe", "/c" ) );
			tokens.add( condaPath );
			tokens.add( "run" );
			tokens.add( "-n" );
			tokens.add( ( String ) s );
			final String[] split = getCommand().split( " " );
			tokens.addAll( Arrays.asList( split ) );
			return tokens;
		} );
		return cmd;
	}

	private PixiEnvironmentCommand setupPixi()
	{
		final PathArgument pixiProject = addPathArgument()
				.name( "Pixi project folder" )
				.help( "Folder containing the pixi.toml file for this tool." )
				.key( KEY_PIXI_PROJECT )
				.defaultValue( "" )
				.inCLI( false )
				.visible( false )
				.get();

		final PixiEnvironmentCommand cmd = new PixiEnvironmentCommand( pixiProject );
		cmd.key( KEY_PIXI_ENV );

		setCommandTranslator( cmd, s -> {
			final List< String > tokens = new ArrayList<>();
			final String envName = ( String ) s;
			final String projectDir = pixiProject.getValue() != null ? pixiProject.getValue() : "";
			final String pixiExe = CLIUtils.getPixiPath();
			tokens.add( pixiExe );
			tokens.add( "run" );
			if ( !projectDir.isEmpty() )
			{
				final String manifest = CLIUtils.getPixiManifestPath( projectDir );
				if ( manifest != null )
				{
					tokens.add( "--manifest-path" );
					tokens.add( manifest );
				}
			}
			if ( envName != null && !envName.isEmpty() )
			{
				tokens.add( "--environment" );
				tokens.add( envName );
			}
			tokens.add( "--" );
			for ( final String token : getCommand().split( " " ) )
				tokens.add( token );
			return tokens;
		} );
		return cmd;
	}

	public Launcher getLauncher()
	{
		if ( selectableLauncher != null )
			return selectableLauncher.getSelected() == 0 ? Launcher.CONDA : Launcher.PIXI;
		return condaCmd != null ? Launcher.CONDA : Launcher.PIXI;
	}

	@Override
	public Argument< ?, ? > getCommandArg()
	{
		if ( selectableLauncher != null )
			return selectableLauncher.getSelection();
		return condaCmd != null ? condaCmd : pixiCmd;
	}

	/**
	 * Returns the command that must be run in the configured environment. If
	 * the command consists of several tokens, separate them with spaces.
	 *
	 * @return the command for this tool.
	 */
	protected abstract String getCommand();

	/**
	 * Returns the version of the Python tool, assuming the module name matches
	 * the first token of {@link #getCommand()}.
	 *
	 * @return the version string, or {@code null} if not determinable.
	 */
	public String getVersion()
	{
		return getVersion( getCommand().split( " " )[ 0 ] );
	}

	/**
	 * Returns the version of the specified Python module inside the configured
	 * environment.
	 *
	 * @param moduleName
	 *            the Python module name.
	 * @return the version string, or {@code null} if not determinable.
	 */
	public String getVersion( final String moduleName )
	{
		if ( getLauncher() == Launcher.CONDA )
			return CLIUtils.getModuleVersion(
					condaCmd.getValue(), moduleName );
		return CLIUtils.getPixiModuleVersion(
				pixiCmd.getProjectPathArg().getValue(),
				pixiCmd.getValue(),
				moduleName );
	}
}
