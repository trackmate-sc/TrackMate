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
		setTranslator( condaEnv, s -> {
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
			}
			return null;
		} );
	}

}
