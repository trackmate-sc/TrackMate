package fiji.plugin.trackmate.util.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ij.IJ;

public abstract class CondaCLIConfigurator extends CLIConfigurator
{

	public static final String KEY_CONDA_ENV = "CONDA_ENV";

	private final CondaEnvironmentCommand condaEnv;

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
			if (IJ.isWindows())
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
				final String pythonPath = CLIUtils.getEnvMap().get( envname );

				cmd.add( pythonPath );
				cmd.add( "-m" );
			}
			// Split by spaces
			final String executableCommand = getCommand();
			final String[] split = executableCommand.split( " " );
			cmd.addAll( Arrays.asList( split ) );
			return cmd;
		} );
	}

	@Override
	public Command< ? > getCommandArg()
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
