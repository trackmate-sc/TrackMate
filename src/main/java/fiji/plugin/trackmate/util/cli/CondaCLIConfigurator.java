package fiji.plugin.trackmate.util.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CondaCLIConfigurator extends CLIConfigurator
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

		// Don't show the executable arg in the UI: must be set by subclass, and
		// only conda env required configurating.
		getExecutableArg().visible( false );

		// Add the translator to make a proper cmd line calling conda first.
		setTranslator( getExecutableArg(), s -> {
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

	public ChoiceArgument getCondaEnv()
	{
		return condaEnv;
	}

	@Override
	public String check()
	{
		return checkArguments();
	}
}
