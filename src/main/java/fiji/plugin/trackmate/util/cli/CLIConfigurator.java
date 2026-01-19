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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Extends the {@link Configurator} for tools that can be run from the command
 * line.
 *
 * @author Jean-Yves Tinevez
 */
public abstract class CLIConfigurator extends Configurator
{

	/*
	 * COMMAND LINE TRANSLATOR.
	 */

	protected final Map< Argument< ?, ? >, Function< Object, List< String > > > cliTranslators = new HashMap<>();

	/**
	 * Decorates the specified argument with a translator, that will modify the
	 * value of the argument in the command line output. Because we focus on the
	 * command line output, the translator returns a list of tokens.
	 * <p>
	 * This can be used for instance to deal with a diameter expressed in µm
	 * everywhere throughout TrackMate, then translate it to a pixel value in
	 * the command line output.
	 *
	 * @param arg
	 *            the argument to translate.
	 * @param translator
	 *            a function that takes the value of the argument and returns a
	 *            list of strings to be used in the command line output.
	 */
	protected void setCommandTranslator( final Argument< ?, ? > arg, final Function< Object, List< String > > translator )
	{
		cliTranslators.put( arg, translator );
	}

	/**
	 * Returns the command object of this command line tool.
	 *
	 * @return the command object, as an argument.
	 */
	public abstract Argument< ?, ? > getCommandArg();
}
