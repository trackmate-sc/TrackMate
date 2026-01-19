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

import java.io.File;

/**
 * Base class for CLI config that are based on an executable, reachable by a
 * path.
 */
public abstract class CommandCLIConfigurator extends CLIConfigurator
{

	protected final ExecutablePath executable;

	public static class ExecutablePath extends AbstractStringArgument< ExecutablePath >
	{

		@Override
		public ExecutablePath name( final String name )
		{
			return super.name( name );
		}

		@Override
		public ExecutablePath help( final String help )
		{
			return super.help( help );
		}

		@Override
		public ExecutablePath key( final String key )
		{
			return super.key( key );
		}

		@Override
		public void accept( final ArgumentVisitor visitor )
		{
			visitor.visit( this );
		}
	}

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
