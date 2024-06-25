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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import fiji.plugin.trackmate.util.cli.CLIConfigurator.AbstractStringArgument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.Argument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.ArgumentVisitor;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.ChoiceArgument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.DoubleArgument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.Flag;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.IntArgument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.PathArgument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.StringArgument;
import fiji.plugin.trackmate.util.cli.CommandCLIConfigurator.ExecutablePath;
import fiji.plugin.trackmate.util.cli.CondaCLIConfigurator.CondaEnvironmentCommand;

public class CommandBuilder implements ArgumentVisitor
{

	private final List< String > tokens = new ArrayList<>();

	private final Map< Argument< ?, ? >, Function< Object, List< String > > > translators;

	protected CommandBuilder( final Map< Argument< ?, ? >, Function< Object, List< String > > > translators )
	{
		this.translators = translators;
	}

	@Override
	public String toString()
	{
		return StringUtils.join( tokens, " " );
	}

	private void check( final Argument< ?, ? > arg )
	{
		if ( arg.isInCLI() && arg.getArgument() == null )
			throw new IllegalArgumentException( "Incorrect configuration for argument '" + arg.getName()
					+ "'. The command argument is not set." );
	}

	@Override
	public void visit( final ExecutablePath executablePath )
	{
		if ( executablePath.getValue() == null )
			throw new IllegalArgumentException( "Executable path is not set." );
		tokens.addAll( translators.getOrDefault( executablePath, v -> Collections.singletonList( "" + v ) ).apply( executablePath.getValue() ) );
	}

	@Override
	public void visit( final CondaEnvironmentCommand condaEnv )
	{
		if ( condaEnv.getValue() == null )
			throw new IllegalArgumentException( "Conda environment is not set." );
		tokens.addAll( translators.getOrDefault( condaEnv, v -> Collections.singletonList( "" + v ) ).apply( condaEnv.getValue() ) );
	}

	@Override
	public void visit( final Flag flag )
	{
		check( flag );
		final boolean val;
		if ( flag.isSet() )
			val = flag.getValue();
		else
			val = flag.getDefaultValue();
		if ( val )
		{
			tokens.add( flag.getArgument() );
			tokens.addAll( translators.getOrDefault( flag, v -> Collections.singletonList( "" + v ) ).apply( val ) );
		}

	}

	@Override
	public void visit( final IntArgument arg )
	{
		check( arg );
		// Is it required and we have not set it? -> error
		if ( arg.isRequired() && !arg.isSet() )
			throw new IllegalArgumentException( "Required argument '" + arg.getName() + "' is not set." );

		// Is not set and we don't have a default value? -> skip
		if ( !arg.isSet() && !arg.hasDefaultValue() )
			return;

		// We have a default or a value.
		final int val = ( !arg.isSet() )
				? arg.getDefaultValue()
				: arg.getValue();

		// Test for min & max
		if ( arg.hasMin() && ( val < arg.getMin() ) )
			throw new IllegalArgumentException( "Value " + val + " for argument '" + arg.getName() + "' is smaller than the min: " + arg.getMin() );
		if ( arg.getMax() != Integer.MAX_VALUE && ( val > arg.getMax() ) )
			throw new IllegalArgumentException( "Value " + val + " for argument '" + arg.getName() + "' is larger than the max: " + arg.getMax() );

		tokens.add( arg.getArgument() );
		tokens.addAll( translators.getOrDefault( arg, v -> Collections.singletonList( "" + v ) ).apply( val ) );
	}

	@Override
	public void visit( final DoubleArgument arg )
	{
		check( arg );
		// Is it required and we have not set it? -> error
		if ( arg.isRequired() && !arg.isSet() )
			throw new IllegalArgumentException( "Required argument '" + arg.getName() + "' is not set." );

		// Is not set and we don't have a default value? -> skip
		if ( !arg.isSet() && !arg.hasDefaultValue() )
			return;

		// We have a default or a value.
		final double val = ( !arg.isSet() )
				? arg.getDefaultValue()
				: arg.getValue();

		// Test for min & max
		if ( arg.hasMin() && ( val < arg.getMin() ) )
			throw new IllegalArgumentException( "Value " + val + " for argument '" + arg.getName()
					+ "' is smaller than the min: " + arg.getMin() );
		if ( arg.hasMax() && ( val > arg.getMax() ) )
			throw new IllegalArgumentException( "Value " + val + " for argument '" + arg.getName()
					+ "' is larger than the max: " + arg.getMax() );

		tokens.add( arg.getArgument() );
		tokens.addAll( translators.getOrDefault( arg, v -> Collections.singletonList( "" + v ) ).apply( val ) );
	}

	private void visitString( final AbstractStringArgument< ? > arg )
	{
		check( arg );
		// Is it required and we have not set it? -> error
		if ( arg.isRequired() && !arg.isSet() )
			throw new IllegalArgumentException( "Required argument '" + arg.getName() + "' is not set." );

		// Is not set and we don't have a default value? -> skip
		if ( !arg.isSet() && !arg.hasDefaultValue() )
			return;

		// We have a default or a value.
		final String val = ( !arg.isSet() )
				? arg.getDefaultValue()
				: arg.getValue();

		tokens.add( arg.getArgument() );
		tokens.addAll( translators.getOrDefault( arg, v -> Collections.singletonList( "" + v ) ).apply( val ) );
	}

	@Override
	public void visit( final StringArgument stringArgument )
	{
		visitString( stringArgument );
	}

	@Override
	public void visit( final PathArgument pathArgument )
	{
		visitString( pathArgument );
	}

	@Override
	public void visit( final ChoiceArgument arg )
	{
		check( arg );
		// Is it required and we have not set it? -> error
		if ( arg.isRequired() && !arg.isSet() )
			throw new IllegalArgumentException( "Required argument '" + arg.getName() + "' is not set." );

		// Is not set? -> skip
		if ( !arg.isSet() )
			return;

		tokens.add( arg.getArgument() );
		tokens.addAll( translators.getOrDefault( arg, v -> Collections.singletonList( "" + v ) ).apply( arg.getValue() ) );
	}

	public static List< String > build( final CLIConfigurator cli )
	{
		final CommandBuilder cb = new CommandBuilder( cli.translators );
		cli.getCommandArg().accept( cb );
		cli.getSelectedArguments()
				.stream()
				.filter( a -> a.isInCLI() )
				.forEach( arg -> arg.accept( cb ) );
		return cb.tokens;
	}
}
