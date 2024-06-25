package fiji.plugin.trackmate.util.cli;

import java.io.File;

/**
 * Base class for CLI config that are based on an executable, reachable by a
 * path.
 */
public abstract class CommandCLIConfigurator extends CLIConfigurator
{

	protected final ExecutablePath executable;

	protected CommandCLIConfigurator()
	{
		this.executable = new ExecutablePath();
	}

	@Override
	public ExecutablePath getCommandArg()
	{
		return executable;
	}

	protected String checkExecutable()
	{
		if ( !executable.isSet() )
		{
			return "Executable path is not set.\n";
		}
		else
		{
			final String path = executable.getValue();
			final File file = new File( path );
			if ( !file.exists() )
				return "Executable path " + path + " does not exist.\n";
			if ( !file.canExecute() )
				return "Executable " + path + " cannot be run.\n";
		}
		return null;
	}

	@Override
	public String check()
	{
		final String out = checkExecutable();
		if ( out != null )
			return out;
		return super.check();
	}

	@Override
	public String toString()
	{
		final StringBuilder str = new StringBuilder();
		str.append( executable.toString() );
		str.append( super.toString() + "\n" );
		return str.toString();
	}
}
