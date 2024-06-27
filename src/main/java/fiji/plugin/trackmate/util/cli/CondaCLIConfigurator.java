/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ij.IJ;

public abstract class CondaCLIConfigurator extends CLIConfigurator
{

	public static final String KEY_CONDA_ENV = "CONDA_ENV";

	private final CondaEnvironmentCommand condaEnv;

	public static class CondaEnvironmentCommand extends AbstractStringArgument< CondaEnvironmentCommand >
	{

		private final List< String > envs = new ArrayList<>();

		protected CondaEnvironmentCommand()
		{
			name( "Conda environment" );
			help( "The conda environment in which the tool is configured." );
			key( KEY_CONDA_ENV );
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
		final List< String > envList = CLIUtils.getEnvList();
		this.condaEnv = new CondaEnvironmentCommand();
		envList.forEach( condaEnv::addEnvironment );
		condaEnv.key( KEY_CONDA_ENV );
		condaEnv.set( 0 );

		// Add the translator to make a proper cmd line calling conda first.
		setTranslator( condaEnv, s -> {
			final List< String > cmd = new ArrayList<>();
			final String condaPath = CLIUtils.getCondaPath();
			// Conda and executable stuff.
			final String envname = ( String ) s;
			if ( IJ.isWindows() )
			{
				/*
				 * In Windows: Launch a shell, change the conda environment,
				 * runs the command in this environment.
				 */
				cmd.addAll( Arrays.asList( "cmd.exe", "/c" ) );
				cmd.addAll( Arrays.asList( condaPath, "activate", envname ) );
				cmd.add( "&" );
			}
			else
			{
				/*
				 * On Mac: we cannot change the conda environment in the process
				 * builder (I tried very hard). So we use the environment to
				 * retrieve what is the path of the Python executable of this
				 * env, and runs the tool as a module. It won't work if the tool
				 * cannot be run as a module. No escape yet.
				 *
				 * Unsure whether this works in Linux.
				 */
				try
				{
					final String pythonPath = CLIUtils.getEnvMap().get( envname );
					cmd.add( pythonPath );
					cmd.add( "-m" );
				}
				catch ( final IOException e )
				{
					System.err.println( "Could not find the conda executable or change the conda environment.\n"
							+ "Please configure the path to your conda executable in Edit > Options > Configure TrackMate Conda path..." );
					e.printStackTrace();
				}

			}
			// Split by spaces
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
}
