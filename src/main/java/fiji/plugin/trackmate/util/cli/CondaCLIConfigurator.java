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

import fiji.plugin.trackmate.util.cli.EnvCLIConfigurator.CondaEnvironmentCommand;

/**
 * Backward-compatible base for tools that run exclusively in a conda
 * environment. New tools should prefer {@link EnvCLIConfigurator} which
 * supports both conda and pixi launchers via a {@code Launcher} parameter.
 */
public abstract class CondaCLIConfigurator extends EnvCLIConfigurator
{

	/** Kept for source compatibility; value equals {@link EnvCLIConfigurator#KEY_CONDA_ENV}. */
	public static final String KEY_CONDA_ENV = EnvCLIConfigurator.KEY_CONDA_ENV;

	protected CondaCLIConfigurator()
	{
		super( Launcher.CONDA );
	}

	@Override
	public CondaEnvironmentCommand getCommandArg()
	{
		return ( CondaEnvironmentCommand ) super.getCommandArg();
	}
}
