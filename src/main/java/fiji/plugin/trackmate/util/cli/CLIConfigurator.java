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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import fiji.plugin.trackmate.util.cli.CommandCLIConfigurator.ExecutablePath;
import fiji.plugin.trackmate.util.cli.CondaCLIConfigurator.CondaEnvironmentCommand;

/**
 * Base class for CLI configurator tools. The implementation of a CLI
 * configurator is made by subclassing this class (or one of its specialization)
 * and specifying arguments for the CLI component with the <code>addXYX()</code>
 * builders.
 *
 * @author Jean-Yves Tinevez
 */
public abstract class CLIConfigurator
{

	protected final List< Argument< ?, ? > > arguments = new ArrayList<>();

	protected final List< SelectableArguments > selectables = new ArrayList<>();

	protected final Map< Argument< ?, ? >, Function< Object, List< String > > > translators = new HashMap<>();

	/*
	 * GETTERS
	 */

	/**
	 * Returns the list of arguments (plus the command) in this CLI config. All
	 * arguments are present, regardless of whether they are in
	 * {@link SelectableArguments}, {@link Argument#visible} or not,
	 * {@link Argument#inCLI} or not.
	 *
	 * @return the list of arguments.
	 */
	public List< Argument< ?, ? > > getArguments()
	{
		return Collections.unmodifiableList( arguments );
	}

	/**
	 * Returns the list of {@link SelectableArguments} in this CLI config.
	 *
	 * @return the list of {@link SelectableArguments}.
	 */
	public List< SelectableArguments > getSelectables()
	{
		return Collections.unmodifiableList( selectables );
	}

	/**
	 * Returns the list of arguments set in this CLI config. The list contains
	 * only the arguments that are selected if they are in a
	 * {@link SelectableArguments}, and those who are not in a
	 * {@link SelectableArguments}.
	 *
	 * @return the selected arguments.
	 */
	public List< Argument< ?, ? > > getSelectedArguments()
	{
		final List< Argument< ?, ? > > selectedArguments = new ArrayList<>( arguments );
		for ( final SelectableArguments selectable : selectables )
			selectable.filter( selectedArguments );
		return selectedArguments;
	}

	/*
	 * VALUE TRANSLATOR.
	 */

	protected void setTranslator( final Argument< ?, ? > arg, final Function< Object, List< String > > translator )
	{
		translators.put( arg, translator );
	}

	/*
	 * SELECTABLE ARGUMENT GROUPS.
	 */

	/**
	 * Creates a 'one or the other' relationships. The arguments that will be
	 * passed to the {@link SelectableArguments} will be flagged as as not to be
	 * used concurrently in the same command. This will be used when creating
	 * UIs.
	 *
	 * @return
	 */
	protected SelectableArguments addSelectableArguments()
	{
		final SelectableArguments sa = new SelectableArguments();
		selectables.add( sa );
		return sa;
	}

	public static class SelectableArguments
	{

		private final List< Argument< ?, ? > > args = new ArrayList<>();

		private String key;

		private int selected = 0;

		public SelectableArguments add( final Argument< ?, ? > arg )
		{
			if ( !args.contains( arg ) )
				args.add( arg );
			return this;
		}

		public SelectableArguments key( final String key )
		{
			this.key = key;
			return this;
		}

		public String getKey()
		{
			return key;
		}

		private void filter( final List< Argument< ?, ? > > arguments )
		{
			final Set< Argument< ?, ? > > toRemove = new HashSet<>();
			for ( final Argument< ?, ? > arg : arguments )
			{
				if ( !args.contains( arg ) )
					continue; // Unknown of this selectable, keep it.

				if ( arg.equals( getSelection() ) )
					continue; // The one selected, keep it.

				// Not selected, remove it.
				toRemove.add( arg );
			}

			arguments.removeAll( toRemove );
		}

		public void select( final int selection )
		{
			this.selected = Math.max( 0, Math.min( args.size() - 1, selection ) );
		}

		public void select( final Argument< ?, ? > arg )
		{
			final int sel = args.indexOf( arg );
			if ( sel < 0 )
			{
				this.selected = 0;
				return;
			}
			this.selected = sel;
		}

		public void select( final String key )
		{
			for ( int i = 0; i < args.size(); i++ )
			{
				if ( key.equals( args.get( i ).getKey() ) )
				{
					this.selected = i;
					return;
				}
			}
			this.selected = 0;
		}

		public Argument< ?, ? > getSelection()
		{
			return args.get( selected );
		}

		public int getSelected()
		{
			return selected;
		}

		/**
		 * Exposes all members of the selectable.
		 */
		public List< Argument< ?, ? > > getArguments()
		{
			return args;
		}
	}

	/*
	 * VISITOR INTERFACE.
	 */

	public interface ArgumentVisitor
	{
		public default void visit( final Flag flag )
		{
			throw new UnsupportedOperationException();
		}

		public default void visit( final StringArgument stringArgument )
		{
			throw new UnsupportedOperationException();
		}

		public default void visit( final DoubleArgument doubleArgument )
		{
			throw new UnsupportedOperationException();
		}

		public default void visit( final IntArgument intArgument )
		{
			throw new UnsupportedOperationException();
		}

		public default void visit( final ChoiceArgument choiceArgument )
		{
			throw new UnsupportedOperationException();
		}

		public default void visit( final PathArgument pathArgument )
		{
			throw new UnsupportedOperationException();
		}

		public default void visit( final ExecutablePath executablePath )
		{
			throw new UnsupportedOperationException();
		}

		public default void visit( final CondaEnvironmentCommand condaEnvironmentCommand )
		{
			throw new UnsupportedOperationException();
		}
	}

	/*
	 * ADDER CLASSES.
	 */

	@SuppressWarnings( "unchecked" )
	abstract class Adder< A extends Argument< A, O >, T extends Adder< A, T, O >, O >
	{

		protected String name;

		protected String help;

		protected String key;

		protected boolean required;

		protected String units;

		protected O defaultValue;

		protected String argument;

		protected boolean visible = true; // by default

		protected boolean inCLI = true; // by default

		/**
		 * Specifies the argument to use in the CLI.
		 *
		 * @param argument
		 *            the command line argument.
		 * @return this adder.
		 */
		public T argument( final String argument )
		{
			this.argument = argument;
			return ( T ) this;
		}

		/**
		 * Specifies whether this argument will be visible in user interfaces
		 * generated from the configurator.
		 *
		 * @param visible
		 *            UI visibility.
		 * @return this adder.
		 */
		public T visible( final boolean visible )
		{
			this.visible = visible;
			return ( T ) this;
		}

		/**
		 * Specifies a user-friendly name for the argument.
		 *
		 * @param name
		 *            the argument name.
		 * @return this adder.
		 */
		public T name( final String name )
		{
			this.name = name;
			return ( T ) this;
		}

		/**
		 * Specifies a help text for the argument.
		 *
		 * @param help
		 *            the help text.
		 * @return this adder.
		 */
		public T help( final String help )
		{
			this.help = help;
			return ( T ) this;
		}

		/**
		 * Specifies the key to use to serialize this argument in TrackMate XML
		 * file. If <code>null</code>, the argument will not be serialized.
		 *
		 * @param key
		 *            the argument key.
		 * @return this adder.
		 */
		public T key( final String key )
		{
			this.key = key;
			return ( T ) this;
		}

		/**
		 * Specifies whether this argument is required in the CLI. If
		 * <code>true</code>, if not set and if there are no default value, an
		 * error will be thrown.
		 *
		 * @param required
		 *            whether this argument is required.
		 * @return this adder.
		 */
		public T required( final boolean required )
		{
			this.required = required;
			return ( T ) this;
		}

		/**
		 * Specifies units for values accepted by this argument.
		 *
		 * @param units
		 *            argument value units.
		 * @return this adder.
		 */
		public T units( final String units )
		{
			this.units = units;
			return ( T ) this;
		}

		/**
		 * Specifies a default value for this argument. If the argument is not
		 * set, it will appear in the command line with this default value.
		 *
		 * @param defaultValue
		 *            the argument default value.
		 * @return this adder.
		 */
		public T defaultValue( final O defaultValue )
		{
			this.defaultValue = defaultValue;
			return ( T ) this;
		}

		/**
		 * Specifies whether this argument will appear in the command line.
		 *
		 * @param inCLI
		 *            appears in the command line.
		 * @return this adder.
		 */
		public T inCLI( final boolean inCLI )
		{
			this.inCLI = inCLI;
			return ( T ) this;
		}

		/**
		 * Returns the argument created by this builder.
		 *
		 * @return the argument.
		 */
		public abstract A get();
	}

	@SuppressWarnings( "unchecked" )
	private abstract class BoundedAdder< A extends BoundedValueArgument< A, O >, T extends BoundedAdder< A, T, O >, O > extends Adder< A, T, O >
	{
		protected O min;

		protected O max;

		/**
		 * Specifies a min for the values accepted by this argument. If the user
		 * sets a value below this min value, an error is thrown.
		 *
		 * @param min
		 *            the min value.
		 * @return this adder.
		 */
		public T min( final O min )
		{
			this.min = min;
			return ( T ) this;
		}

		/**
		 * Specifies a max for the values accepted by this argument. If the user
		 * sets a value above this max value, an error is thrown.
		 *
		 * @param max
		 *            the max value.
		 * @return this adder.
		 */
		public T max( final O max )
		{
			this.max = max;
			return ( T ) this;
		}
	}

	protected class IntAdder extends BoundedAdder< IntArgument, IntAdder, Integer >
	{
		@Override
		public IntArgument get()
		{
			final IntArgument arg = new IntArgument()
					.name( name )
					.help( help )
					.argument( argument )
					.defaultValue( defaultValue )
					.max( max )
					.min( min )
					.required( required )
					.units( units )
					.visible( visible )
					.inCLI( inCLI )
					.key( key );
			CLIConfigurator.this.arguments.add( arg );
			return arg;
		}
	}

	protected class DoubleAdder extends BoundedAdder< DoubleArgument, DoubleAdder, Double >
	{

		private DoubleAdder()
		{}

		@Override
		public DoubleArgument get()
		{
			final DoubleArgument arg = new DoubleArgument()
					.name( name )
					.help( help )
					.argument( argument )
					.defaultValue( defaultValue )
					.max( max )
					.min( min )
					.required( required )
					.units( units )
					.visible( visible )
					.inCLI( inCLI )
					.key( key );
			CLIConfigurator.this.arguments.add( arg );
			return arg;
		}
	}

	protected class FlagAdder extends Adder< Flag, FlagAdder, Boolean >
	{

		private FlagAdder()
		{}

		@Override
		public Flag get()
		{
			final Flag arg = new Flag()
					.name( name )
					.help( help )
					.argument( argument )
					.defaultValue( defaultValue )
					.required( required )
					.units( units )
					.visible( visible )
					.inCLI( inCLI )
					.key( key );
			CLIConfigurator.this.arguments.add( arg );
			return arg;
		}
	}

	protected class StringAdder extends Adder< StringArgument, StringAdder, String >
	{

		private StringAdder()
		{}

		@Override
		public StringArgument get()
		{
			final StringArgument arg = new StringArgument()
					.name( name )
					.help( help )
					.argument( argument )
					.defaultValue( defaultValue )
					.required( required )
					.units( units )
					.visible( visible )
					.inCLI( inCLI )
					.key( key );
			CLIConfigurator.this.arguments.add( arg );
			return arg;
		}
	}

	protected class PathAdder extends Adder< PathArgument, PathAdder, String >
	{

		private PathAdder()
		{}

		@Override
		public PathArgument get()
		{
			final PathArgument arg = new PathArgument()
					.name( name )
					.help( help )
					.argument( argument )
					.defaultValue( defaultValue )
					.required( required )
					.units( units )
					.visible( visible )
					.inCLI( inCLI )
					.key( key );
			CLIConfigurator.this.arguments.add( arg );
			return arg;
		}
	}

	protected class ChoiceAdder extends Adder< ChoiceArgument, ChoiceAdder, String >
	{

		private ChoiceAdder()
		{}

		private final List< String > choices = new ArrayList<>();

		private final List< String > mappeds = new ArrayList<>();

		/**
		 * Adds a selectable item for this choice argument. The user will be
		 * able to select from the list of choices added by this method.
		 *
		 * @param choice
		 *            the choice to add.
		 * @return this adder.
		 */
		public ChoiceAdder addChoice( final String choice )
		{
			return addChoice( choice, choice );
		}

		/**
		 * Adds a selectable item for this choice argument. The user will be
		 * able to select from the list of choices added by this method. In the
		 * command line, this choice will be mapped to the specified string.
		 * <p>
		 * Example:
		 *
		 * <pre>
		 * addChoice( "Bacteria phase contrast", "bact_phase_omni" )
		 * </pre>
		 *
		 * will display <i>Bacteria phase contrast</i> in the UI and results in
		 * using <i>bact_phase_omni</i> in the generated command line.
		 *
		 * @param choice
		 *            the choice to add.
		 * @param mapped
		 *            the string the choice will be mapped in the command line
		 *            argument.
		 * @return this adder.
		 */
		public ChoiceAdder addChoice( final String choice, final String mapped )
		{
			if ( !choices.contains( choice ) )
			{
				choices.add( choice );
				mappeds.add( mapped );
			}
			return this;
		}

		/**
		 * Adds the specified items for this choice argument. The user will be
		 * able to select from the list of choices added by this method.
		 *
		 * @param c
		 *            the choices to add.
		 * @return this adder.
		 */
		public Adder< ChoiceArgument, ChoiceAdder, String > addChoiceAll( final Collection< String > c )
		{
			for ( final String in : c )
				addChoice( in );
			return this;
		}

		/**
		 * Specifies a default value for this argument. If the argument is not
		 * set, it will appear in the command line with this default value.
		 * <p>
		 * The value specified must belong to the list of choices set with
		 * {@link #addChoice(String)} or {@link #addChoiceAll(Collection)}.
		 *
		 * @param defaultChoice
		 *            the argument default value.
		 * @return this adder.
		 */
		@Override
		public ChoiceAdder defaultValue( final String defaultChoice )
		{
			final int sel = choices.indexOf( defaultChoice );
			if ( sel < 0 )
				throw new IllegalArgumentException( "Unknown selection '" + defaultChoice + "' for parameter '"
						+ name + "'. Must be one of " + StringUtils.join( choices, ", " ) + "." );
			return super.defaultValue( defaultChoice );
		}

		/**
		 * Specifies a default value for this argument, by selecting the
		 * possible value in order or addition. If the argument is not set, it
		 * will appear in the command line with this default value.
		 *
		 * @param selected
		 *            the index of the default value in the list of possible
		 *            choices.
		 * @return this adder.
		 */
		public ChoiceAdder defaultValue( final int selected )
		{
			if ( selected < 0 || selected >= choices.size() )
				throw new IllegalArgumentException( "Invalid index for selection of parameter '"
						+ name + "'. Must be in scale " + 0 + " to " + ( choices.size() - 1 ) + " in "
						+ StringUtils.join( choices, ", " ) + "." );
			return defaultValue( choices.get( selected ) );
		}

		@Override
		public ChoiceArgument get()
		{
			final ChoiceArgument arg = new ChoiceArgument()
					.name( name )
					.help( help )
					.argument( argument )
					.required( required )
					.units( units )
					.visible( visible )
					.inCLI( inCLI )
					.key( key );
			for ( int i = 0; i < choices.size(); i++ )
				arg.addChoice( choices.get( i ), mappeds.get( i ) );

			arg.defaultValue( defaultValue );
			CLIConfigurator.this.arguments.add( arg );
			return arg;
		}
	}

	/*
	 * ADDER METHODS.
	 */

	/**
	 * Adds a boolean flag argument to the CLI, via a builder.
	 * <p>
	 * In the CLI tools we have been trying to implement, they exist in two
	 * flavors, that are both supported.
	 *
	 * If the argument starts with a double-dash <code>--</code>, as in Python
	 * argparse syntax, then it is understood that setting this flag to true
	 * makes it appear in the CLI. For instance: <code>--use-gpu</code>.
	 *
	 * If the arguments ends with a '=' sign (e.g. <code>"save_txt="</code>),
	 * then it expects to receive a 'true' or 'false' value.
	 *
	 * @return a new flag argument builder.
	 */
	protected FlagAdder addFlag()
	{
		return new FlagAdder();
	}

	/**
	 * Adds a string argument to the CLI, via a builder.
	 *
	 * @return new string argument builder.
	 */
	protected StringAdder addStringArgument()
	{
		return new StringAdder();
	}

	/**
	 * Adds a path argument to the CLI, via a builder.
	 *
	 * @return new path argument builder.
	 */
	protected PathAdder addPathArgument()
	{
		return new PathAdder();
	}

	/**
	 * Adds a integer argument to the CLI, via a builder.
	 *
	 * @return new integer argument builder.
	 */
	protected IntAdder addIntArgument()
	{
		return new IntAdder();
	}

	/**
	 * Adds a double argument to the CLI, via a builder.
	 *
	 * @return new double argument builder.
	 */
	protected DoubleAdder addDoubleArgument()
	{
		return new DoubleAdder();
	}

	/**
	 * Adds a choice argument to the CLI, via a builder. Such arguments can
	 * accept a series of discrete values (specified by addChoice() method in
	 * the builder).
	 *
	 * @return new choice argument builder.
	 */
	protected ChoiceAdder addChoiceArgument()
	{
		return new ChoiceAdder();
	}

	/**
	 * Adds an extra argument, defined by other means than the adder methods.
	 *
	 * @param extraArg
	 *            the argument to add to this CLI config.
	 * @return the argument
	 */
	protected < T extends Argument< ?, ? > > T addExtraArgument( final T extraArg )
	{
		this.arguments.add( extraArg );
		return extraArg;
	}

	/*
	 * ARGUMENT CLASSES.
	 */

	public static class Flag extends Argument< Flag, Boolean >
	{
		Flag()
		{}

		/**
		 * Sets this flag argument to <code>true</code>.
		 */
		public void set()
		{
			set( true );
		}

		@Override
		public void accept( final ArgumentVisitor visitor )
		{
			visitor.visit( this );
		}

		@Override
		public void setValueObject( final Object val )
		{
			if ( !Boolean.class.isInstance( val ) )
				throw new IllegalArgumentException( "Argument '" + name + "' expects Boolean. Got " + val.getClass().getSimpleName() );

			final Boolean v = ( ( Boolean ) val );
			set( v );
		}
	}

	/**
	 * Specialization of {@link StringArgument} to be used in a GUI.
	 */
	public static class PathArgument extends AbstractStringArgument< PathArgument >
	{
		private PathArgument()
		{}

		@Override
		public void accept( final ArgumentVisitor visitor )
		{
			visitor.visit( this );
		}
	}

	public static class StringArgument extends AbstractStringArgument< StringArgument >
	{
		private StringArgument()
		{}

		@Override
		public void accept( final ArgumentVisitor visitor )
		{
			visitor.visit( this );
		}
	}

	public static abstract class AbstractStringArgument< T extends AbstractStringArgument< T > > extends Argument< T, String >
	{

		@Override
		public void setValueObject( final Object val )
		{
			if ( !String.class.isInstance( val ) )
				throw new IllegalArgumentException( "Argument '" + name + "' expects String. Got " + val.getClass().getSimpleName() );

			final String v = ( ( String ) val );
			set( v );
		}
	}

	public static class IntArgument extends BoundedValueArgument< IntArgument, Integer >
	{
		IntArgument()
		{}

		@Override
		public void accept( final ArgumentVisitor visitor )
		{
			visitor.visit( this );
		}

		@Override
		public void setValueObject( final Object val )
		{
			if ( !Integer.class.isInstance( val ) )
				throw new IllegalArgumentException( "Argument '" + name + "' expects Integer. Got " + val.getClass().getSimpleName() );

			final Integer v = ( ( Integer ) val );
			set( v );
		}
	}

	public static class DoubleArgument extends BoundedValueArgument< DoubleArgument, Double >
	{
		@Override
		public void accept( final ArgumentVisitor visitor )
		{
			visitor.visit( this );
		}

		@Override
		public void setValueObject( final Object val )
		{
			if ( !Double.class.isInstance( val ) )
				throw new IllegalArgumentException( "Argument '" + name + "' expects Double. Got " + val.getClass().getSimpleName() );

			final Double v = ( ( Double ) val );
			set( v );
		}
	}

	public static class ChoiceArgument extends Argument< ChoiceArgument, String >
	{

		private final List< String > choices = new ArrayList<>();

		private final List< String > displays = new ArrayList<>();

		private int selected = -1; // -1 means no selection.;

		private ChoiceArgument()
		{}

		private ChoiceArgument addChoice( final String choice, final String displayed )
		{
			if ( !choices.contains( choice ) )
			{
				choices.add( choice );
				displays.add( displayed );
			}
			return this;
		}

		/**
		 * The list of the display strings corresponding to the possible
		 * choices.
		 *
		 * @return The list of the display strings.
		 */
		public List< String > getDisplays()
		{
			return displays;
		}

		@Override
		public void accept( final ArgumentVisitor visitor )
		{
			visitor.visit( this );
		}

		@Override
		public boolean isSet()
		{
			return selected >= 0;
		}

		@Override
		public String getValue()
		{
			return choices.get( selected );
		}

		public int getSelectedIndex()
		{
			return selected;
		}

		@Override
		public void set( final String choice )
		{
			final int sel = choices.indexOf( choice );
			if ( sel < 0 )
				throw new IllegalArgumentException( "Unknown selection '" + choices + "' for parameter '"
						+ name + "'. Must be one of " + StringUtils.join( choices, ", " ) + "." );
			this.selected = sel;
		}

		public void set( final int selected )
		{
			if ( selected < 0 || selected >= choices.size() )
				throw new IllegalArgumentException( "Invalid index for selection of parameter '"
						+ name + "'. Must be in scale " + 0 + " to " + ( choices.size() - 1 ) + " in "
						+ StringUtils.join( choices, ", " ) + "." );
			this.selected = selected;
		}

		@Override
		public void setValueObject( final Object val )
		{
			if ( !String.class.isInstance( val ) )
				throw new IllegalArgumentException( "Argument '" + name + "' expects String. Got " + val.getClass().getSimpleName() );

			final String v = ( ( String ) val );
			set( v );
		}

		@Override
		ChoiceArgument defaultValue( final String defaultChoice )
		{
			final int sel = choices.indexOf( defaultChoice );
			if ( sel < 0 )
				throw new IllegalArgumentException( "Unknown selection '" + defaultChoice + "' for parameter '"
						+ name + "'. Must be one of " + StringUtils.join( choices, ", " ) + "." );
			super.defaultValue( defaultChoice );
			return this;
		}

		ChoiceArgument defaultValue( final int selected )
		{
			if ( selected < 0 || selected >= choices.size() )
				throw new IllegalArgumentException( "Invalid index for selection of parameter '"
						+ name + "'. Must be in scale " + 0 + " to " + ( choices.size() - 1 ) + " in "
						+ StringUtils.join( choices, ", " ) + "." );
			super.defaultValue( choices.get( selected ) );
			return this;
		}

		@Override
		public String toString()
		{
			final String str = super.toString();
			return str
					+ " - choices: " + choices + "\n"
					+ " - display strings: " + displays + "\n";
		}


	}

	@SuppressWarnings( "unchecked" )
	public static abstract class BoundedValueArgument< T extends BoundedValueArgument< T, O >, O > extends Argument< T, O >
	{

		private BoundedValueArgument()
		{}

		private O min;

		private O max;

		T min( final O min )
		{
			this.min = min;
			return ( T ) this;
		}

		public O getMax()
		{
			return max;
		}

		T max( final O max )
		{
			this.max = max;
			return ( T ) this;
		}

		public O getMin()
		{
			return min;
		}

		public boolean hasMin()
		{
			return min != null;
		}

		public boolean hasMax()
		{
			return max != null;
		}

		@Override
		public String toString()
		{
			final String str = super.toString();
			return str
					+ " - has min: " + hasMin() + "\n"
					+ ( hasMin()
							? " - min: " + getMin() + "\n"
							: "" )
					+ " - has max: " + hasMax() + "\n"
					+ ( hasMax()
							? " - max: " + getMax() + "\n"
							: "" );
		}
	}

	/**
	 * Mother class for command arguments. Typically in the command line they
	 * appear after the executable name with '--something'.
	 *
	 * @param <T>
	 *            the implementing type of the argument.
	 * @param <O>
	 *            the type of value this argument accepts.
	 */
	@SuppressWarnings( "unchecked" )
	public static abstract class Argument< T extends Argument< T, O >, O >
	{

		protected boolean visible = true;

		protected String name;

		protected String help;

		private String key;

		private String argument;

		private boolean inCLI = true;

		T argument( final String argument )
		{
			this.argument = argument;
			return ( T ) this;
		}

		public String getArgument()
		{
			return argument;
		}

		private O value;

		private O defaultValue;

		/**
		 * Arguments flagged as not required, but without default value, will be
		 * prompted to the user.
		 */
		private boolean required = false;

		private String units;

		T required( final boolean required )
		{
			this.required = required;
			return ( T ) this;
		}

		public boolean isRequired()
		{
			return required;
		}

		T units( final String units )
		{
			this.units = units;
			return ( T ) this;
		}

		public String getUnits()
		{
			return units;
		}

		T defaultValue( final O defaultValue )
		{
			this.defaultValue = defaultValue;
			return ( T ) this;
		}

		public O getDefaultValue()
		{
			return defaultValue;
		}

		public boolean hasDefaultValue()
		{
			return defaultValue != null;
		}

		public void set( final O value )
		{
			this.value = value;
		}

		public O getValue()
		{
			return value;
		}

		public boolean isSet()
		{
			return value != null;
		}

		public Object getValueObject()
		{
			return getValue();
		}


		/**
		 * Sets the value of this argument via the specified object. This is
		 * used when deserializing TrackMate settings map.
		 *
		 * @param val
		 *            the object to set the value from
		 * @see TrackMateSettingsBuilder
		 */
		public abstract void setValueObject( Object val );

		/**
		 * If <code>false</code>, this argument won't be used in the command
		 * line generator. This is useful to add extra parameters to the GUI
		 * that are required by TrackMate but not by the CLI tool.
		 *
		 * @param inCLI
		 *            whether this argument should be used when generating
		 *            commands. By default: <code>true</code>.
		 * @see CommandBuilder
		 * @return the argument.
		 */
		T inCLI( final boolean inCLI )
		{
			this.inCLI = inCLI;
			return ( T ) this;
		}

		public boolean isInCLI()
		{
			return inCLI;
		}

		/**
		 * If <code>false</code>, this argument won't be shown in UIs. It will
		 * be used for the command line builder nonetheless.
		 *
		 * @param visible
		 *            whether this argument should be visible in the UI or not.
		 *            By default: <code>true</code>.
		 * @see CliGuiBuilder
		 * @return the argument.
		 */
		T visible( final boolean visible )
		{
			this.visible = visible;
			return ( T ) this;
		}

		public boolean isVisible()
		{
			return visible;
		}

		T name( final String name )
		{
			this.name = name;
			return ( T ) this;
		}

		T help( final String help )
		{
			this.help = help;
			return ( T ) this;
		}

		/**
		 * Sets the String key to use in TrackMate settings map
		 * de/serialization.
		 *
		 * @param key
		 *            the key to use. By default: the {@link #name} of this
		 *            argument.
		 * @return the argument.
		 * @see TrackMateSettingsBuilder
		 */
		T key( final String key )
		{

			this.key = key;
			return ( T ) this;
		}

		public String getName()
		{
			return name;
		}

		public String getHelp()
		{
			return help;
		}

		public String getKey()
		{
			return key;
		}

		public abstract void accept( final ArgumentVisitor visitor );

		@Override
		public String toString()
		{
			return this.getClass().getSimpleName()
					+ " (" + getName() + ")\n"
					+ " - help: " + getHelp() + "\n"
					+ " - key: " + getKey() + "\n"
					+ " - argument: " + getArgument() + "\n"
					+ " - visible: " + isVisible() + "\n"
					+ " - is set: " + isSet() + "\n"
					+ ( isSet()
							? " - value: " + getValue() + "\n"
							: "" )
					+ " - has default value: " + hasDefaultValue() + "\n"
					+ ( hasDefaultValue()
							? " - default value: " + getDefaultValue() + "\n"
							: "" )
					+ " - required: " + isRequired() + "\n"
					+ " - units: " + getUnits() + "\n";
		}
	}

	@Override
	public String toString()
	{
		final StringBuilder str = new StringBuilder();
		str.append( super.toString() + "\n" );
		arguments.forEach( str::append );
		return str.toString();
	}

	public String check()
	{
		final StringBuilder str = new StringBuilder();
		for ( final Argument< ?, ? > arg : getSelectedArguments() )
		{
			if ( arg.isInCLI() && arg.getArgument() == null )
				str.append( "Argument '" + arg.getName() + "' does not define the argument switch.\n" );

			if ( arg.isRequired() && !arg.isSet() && !arg.hasDefaultValue() )
				str.append( "Argument '" + arg.getName() + "' is required but is not set and does not define a default value.\n" );
		}
		return str.length() == 0 ? null : str.toString();
	}

	/**
	 * Returns the command object of this tool.
	 *
	 * @return the command object, as an argument.
	 */
	public abstract Argument< ?, ? > getCommandArg();
}
