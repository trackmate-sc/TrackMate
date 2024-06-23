package fiji.plugin.trackmate.util.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CondaCLIConfigurator extends CLIConfigurator
{

	public static final String KEY_CONDA_ENV = "CONDA_ENV";

	private final ChoiceArgument condaEnv;

	protected CondaCLIConfigurator()
	{
		super();

		// Make a UI-only arg configuring the conda env.
		// Default is last one (base is not interesting as a default).
		final List< String > envList = CLIUtils.getEnvList();
		this.condaEnv = addChoiceArgument()
				.name( "Conda environment" )
				.help( "In what conda environment is the tool installed." )
				.addChoiceAll( envList )
				.defaultValue( envList.get( envList.size() - 1 ) )
				.key( KEY_CONDA_ENV )
				.visible( true )
				.inCLI( false )
				.get();

		// Add it first to the list of arguments
		this.arguments.add( condaEnv );

		// Add the translator to make a proper cmd line calling conda first.
		setTranslator( condaEnv, s -> {
			final String executableName = ( String ) s;
			// Split by spaces
			final String[] split = executableName.split( " " );
			final List< String > cmd = new ArrayList<>();
			final String envname = condaEnv.getValue();
			cmd.addAll( Arrays.asList( "cmd.exe", "/c", "conda", "activate", envname ) );
			cmd.add( "&" );
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
