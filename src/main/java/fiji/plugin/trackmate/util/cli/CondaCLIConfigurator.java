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
package fiji.plugin.trackmate.util.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CondaCLIConfigurator extends CLIConfigurator
{

	public static final String KEY_CONDA_ENV = "CONDA_ENV";

	protected final CondaEnvironmentCommand condaEnv;

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
				System.err.println( "The list of conda environments is empty." );
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
			{
				System.err.println( "The list of conda environments is empty." );
				return;
			}

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

	protected CondaCLIConfigurator()
	{
		super();

		// Make a UI-only arg configuring the conda env.
		// Default is last one (base is not interesting as a default).
		final List< String > envList = new ArrayList<>();
		try
		{
			final List< String > l = CLIUtils.getEnvList();
			envList.addAll( l );
		}
		catch ( final Exception e )
		{
			// Do nothing. The list of envs will be empty.
			System.err.println( "There was an error retrieving the list of conda environments.\n"
					+ "Did you configure Conda for TrackMate? (Edit >  Options > Configure TrackMate Conda path...)" );
			e.printStackTrace();
		}
		this.condaEnv = new CondaEnvironmentCommand();
		envList.forEach( condaEnv::addEnvironment );
		condaEnv.key( KEY_CONDA_ENV );
		condaEnv.set( 0 );

		// Add the translator to make a proper cmd line calling conda first.
		setCommandTranslator( condaEnv, s -> {
			final List< String > cmd = new ArrayList<>();
			final String condaPath = CLIUtils.getCondaPath();
			final String os = System.getProperty( "os.name" ).toLowerCase();
			if ( os.contains( "win" ) )
			{
				// In Windows: Launch a cmd.exe shell.
				cmd.addAll( Arrays.asList( "cmd.exe", "/c" ) );
			}
			else
			{
				// On Mac or Linux.
			}
			// Call conda run.
			cmd.add( condaPath );
			cmd.add( "run" );
			cmd.add( "-n" );
			// The executable stuff.
			final String envname = ( String ) s;
			cmd.add( envname );
			// Important: don't buffer output
			cmd.add( "--no-capture-output" );
			// The rest of the command, split by spaces.
			final String executableCommand = getCommand();
			final String[] split = executableCommand.split( " " );
			cmd.addAll( Arrays.asList( split ) );
			return cmd;
		} );
	}

	@Override
	public CondaEnvironmentCommand getCommandArg()
	{
		return condaEnv;
	}

	/**
	 * Returns the command that must be run in the configured conda environment.
	 * In case the command is made of several tokens, they can be returned
	 * separated by space (as in a normal command line).
	 *
	 * @return the command for this tool.
	 */
	protected abstract String getCommand();

	/**
	 * Returns the version of the Python tool that this configurator is
	 * configured for. This method assumes that the module has the same name
	 * that the CLI command used to run it.
	 *
	 * @return the version string or <code>null</code> if the version could not
	 *         be determined or if the command does not run on Python.
	 */
	public String getVersion()
	{
		return getVersion( getCommandArg().getValue() );
	}

	/**
	 * Returns the version of the Python tool that this configurator is
	 * configured for.
	 *
	 * @param moduleName
	 *            the name of the module to get the version for.
	 * @return the version string or <code>null</code> if the version could not
	 *         be determined or if the command does not run on Python.
	 */
	public String getVersion( final String moduleName )
	{
		return CLIUtils.getModuleVersion( condaEnv.getValue(), moduleName );
	}
}
