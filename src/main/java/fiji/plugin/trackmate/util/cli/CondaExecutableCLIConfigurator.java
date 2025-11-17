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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ij.IJ;

/**
 * Mother class for CLI configurator that relies on an <b>executable</b>
 * installed in a conda environment. This happens when the Python code we want
 * to call does not have a Python module (that we could call with 'python -m')
 * or is simply not a Python program. In that case:
 * <ul>
 * <li>on Mac we resolve the env path, and append the executable name to build
 * an absolute path to the executable.
 * <li>on Windows we simply activate the conda environment and call the
 * executable assuming it is on the path.
 * </ul>
 */
public abstract class CondaExecutableCLIConfigurator extends CondaCLIConfigurator
{

	public CondaExecutableCLIConfigurator()
	{
		super();

		// Add the translator to make a proper cmd line calling conda first.
		setCommandTranslator( condaEnv, s -> {
			final List< String > cmd = new ArrayList<>();
			final String condaPath = CLIUtils.getCondaPath();
			// Conda and executable stuff.
			final String envname = ( String ) s;
			if ( IJ.isWindows() )
			{
				cmd.addAll( Arrays.asList( "cmd.exe", "/c" ) );
				cmd.addAll( Arrays.asList( condaPath, "activate", envname ) );
				cmd.add( "&" );
				// Add command name
				final String executableCommand = getCommand();
				// Split by spaces
				final String[] split = executableCommand.split( " " );
				cmd.addAll( Arrays.asList( split ) );
				return cmd;

			}
			else
			{
				try
				{
					final String pythonPath = CLIUtils.getEnvMap().get( envname );
					final int i = pythonPath.lastIndexOf( "python" );
					final String binPath = pythonPath.substring( 0, i );
					final String executablePath = binPath + getCommand();
					final String[] split = executablePath.split( " " );
					cmd.addAll( Arrays.asList( split ) );
					return cmd;
				}
				catch ( final IOException e )
				{
					System.err.println( "Could not find the conda executable or change the conda environment.\n"
							+ "Please configure the path to your conda executable in Edit > Options > Configure TrackMate Conda path..." );
					e.printStackTrace();
				}
				catch ( final Exception e )
				{
					System.err.println( "Error running the conda executable:\n" );
					e.printStackTrace();
				}
			}
			return null;
		} );
	}

}
