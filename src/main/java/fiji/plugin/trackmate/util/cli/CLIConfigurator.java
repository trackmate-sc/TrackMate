package fiji.plugin.trackmate.util.cli;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class CLIConfigurator
{

	public class CommandBuilder implements ArgumentVisitor
	{

		private final List< String > tokens = new ArrayList<>();

		@Override
		public String toString()
		{
			return StringUtils.join( tokens, " " );
		}

		private void check( final Argument< ? > arg )
		{
			if ( arg.argument == null )
				throw new IllegalArgumentException( "Incorrect configuration for argument '" + arg.name
						+ "'. The command argument is not set." );
		}

		@Override
		public void visit( final Flag flag )
		{
			check( flag );
			if (flag.set)
				tokens.add( flag.argument );
		}

		@Override
		public void visit( final IntArgument arg )
		{
			check( arg );
			// Is it required and we have not set it? -> error
			if ( arg.required && arg.value == Integer.MIN_VALUE )
				throw new IllegalArgumentException( "Required argument '" + arg.name + "' is not set." );

			// Is not set and we don't have a default value? -> skip
			if ( arg.value == Integer.MIN_VALUE && arg.defaultValue == Integer.MIN_VALUE )
				return;
			
			// We have a default or a value.
			final int val = ( arg.value == Integer.MIN_VALUE )
					? arg.defaultValue
					: arg.value;

			// Test for min & max
			if ( arg.min != Integer.MIN_VALUE && ( val < arg.min ) )
				throw new IllegalArgumentException( "Value " + val + " for argument '" + arg.name + "' is smaller than the min: " + arg.min );
			if ( arg.max != Integer.MAX_VALUE && ( val > arg.max ) )
				throw new IllegalArgumentException( "Value " + val + " for argument '" + arg.name + "' is larger than the max: " + arg.max );

			tokens.add( arg.argument );
			tokens.add( "" + val );
		}

		@Override
		public void visit( final DoubleArgument arg )
		{
			check( arg );
			// Is it required and we have not set it? -> error
			if ( arg.required && Double.isNaN( arg.value ) )
				throw new IllegalArgumentException( "Required argument '" + arg.name + "' is not set." );

			// Is not set and we don't have a default value? -> skip
			if ( Double.isNaN( arg.value ) && Double.isNaN( arg.defaultValue ) )
				return;

			// We have a default or a value.
			final double val = ( Double.isNaN( arg.value ) )
					? arg.defaultValue
					: arg.value;

			// Test for min & max
			if ( !Double.isNaN( arg.min ) && ( val < arg.min ) )
				throw new IllegalArgumentException( "Value " + val + " for argument '" + arg.name + "' is smaller than the min: " + arg.min );
			if ( !Double.isNaN( arg.max ) && ( val > arg.max ) )
				throw new IllegalArgumentException( "Value " + val + " for argument '" + arg.name + "' is larger than the max: " + arg.max );

			tokens.add( arg.argument );
			tokens.add( "" + Double.toString( val ) );
		}

		@Override
		public void visit( final StringArgument arg )
		{
			check( arg );
			// Is it required and we have not set it? -> error
			if ( arg.required && arg.value == null )
				throw new IllegalArgumentException( "Required argument '" + arg.name + "' is not set." );

			// Is not set and we don't have a default value? -> skip
			if ( arg.value == null && arg.defaultValue == null )
				return;

			// We have a default or a value.
			final String val = ( arg.value == null )
					? arg.defaultValue
					: arg.value;

			tokens.add( arg.argument );
			tokens.add( "" + val );
		}

		@Override
		public void visit( final ChoiceArgument arg )
		{
			check( arg );
			// Is it required and we have not set it? -> error
			if ( arg.required && arg.selected < 0 )
				throw new IllegalArgumentException( "Required argument '" + arg.name + "' is not set." );

			// Is not set? -> skip
			if ( arg.selected < 0 )
				return;

			tokens.add( arg.argument );
			tokens.add( arg.choices.get( arg.selected ) );
		}
	}

	private final List< Argument< ? > > arguments = new ArrayList<>();

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
	}

	public Flag addFlag()
	{
		final Flag flagAdder = new Flag();
		arguments.add( flagAdder );
		return flagAdder;
	}

	public StringArgument addStringArgument()
	{
		final StringArgument stringArgumentAdder = new StringArgument();
		arguments.add( stringArgumentAdder );
		return stringArgumentAdder;
	}

	public IntArgument addIntArgument()
	{
		final IntArgument intArgumentAdder = new IntArgument();
		arguments.add( intArgumentAdder );
		return intArgumentAdder;
	}

	public DoubleArgument addDoubleArgument()
	{
		final DoubleArgument doubleArgumentAdder = new DoubleArgument();
		arguments.add( doubleArgumentAdder );
		return doubleArgumentAdder;
	}

	public ChoiceArgument addChoiceArgument()
	{
		final ChoiceArgument choiceArgumentAdder = new ChoiceArgument();
		arguments.add( choiceArgumentAdder );
		return choiceArgumentAdder;
	}

	public static class Flag extends Argument< Flag >
	{

		private boolean set = false;

		public void set()
		{
			this.set = true;
		}

		public void set( final boolean set )
		{
			this.set = set;
		}

		@Override
		public void accept( final ArgumentVisitor visitor )
		{
			visitor.visit( this );
		}
	}

	public static class StringArgument extends ValueArgument< StringArgument >
	{

		/**
		 * If this is not <code>null</code>, the argument will be added to the
		 * command with the default value, even if the user does not explicitly
		 * call the argument. This emulates Python argparse.ArgumentParser.
		 */
		private String defaultValue = null;

		private String value = null;

		public StringArgument defaultValue( final String defaultValue )
		{
			this.defaultValue = defaultValue;
			return this;
		}

		@Override
		public void accept( final ArgumentVisitor visitor )
		{
			visitor.visit( this );
		}

		public void set( final String value )
		{
			this.value = value;
		}
	}

	public static class IntArgument extends ValueArgument< IntArgument >
	{

		/**
		 * If this is not <code>Integer.MIN_VALUE</code>, the argument will be
		 * added to the command with the default value, even if the user does
		 * not explicitly call the argument. This emulates Python
		 * argparse.ArgumentParser.
		 */
		private int defaultValue = Integer.MIN_VALUE;

		/** Considered set if different from <code>Integer.MIN_VALUE</code>. */
		private int min = Integer.MIN_VALUE;

		/** Considered set if different from <code>Integer.MAX_VALUE</code>. */
		private int max = Integer.MAX_VALUE;

		private int value = Integer.MIN_VALUE; // unset value

		public IntArgument defaultValue( final int defaultValue )
		{
			this.defaultValue = defaultValue;
			return this;
		}

		/**
		 * Defines the min.
		 * 
		 * @param min,
		 *            inclusive.
		 * @return the argument.
		 */
		public IntArgument min( final int min )
		{
			this.min = min;
			return this;
		}

		public IntArgument max( final int max )
		{
			this.max = max;
			return this;
		}

		public void set( final int value )
		{
			this.value = value;
		}

		@Override
		public void accept( final ArgumentVisitor visitor )
		{
			visitor.visit( this );
		}
	}

	public static class DoubleArgument extends ValueArgument< DoubleArgument >
	{

		/**
		 * If this is not <code>Double.NaN</code>, the argument will be added to
		 * the command with the default value, even if the user does not
		 * explicitly call the argument. This emulates Python
		 * argparse.ArgumentParser.
		 */
		private double defaultValue = Double.NaN;

		/**
		 * Considered set if different from <code>Double.NaN</code>.
		 */
		private double min = Double.NaN;

		/**
		 * Considered set if different from <code>Double.NaN</code>.
		 */
		private double max = Double.NaN;

		private double value = Double.NaN;

		public DoubleArgument defaultValue( final double defaultValue )
		{
			this.defaultValue = defaultValue;
			return this;
		}

		public DoubleArgument min( final double min )
		{
			this.min = min;
			return this;
		}

		public DoubleArgument max( final double max )
		{
			this.max = max;
			return this;
		}

		public void set( final double value )
		{
			this.value = value;
		}

		@Override
		public void accept( final ArgumentVisitor visitor )
		{
			visitor.visit( this );
		}
	}

	public static class ChoiceArgument extends ValueArgument< ChoiceArgument >
	{

		private final List< String > choices = new ArrayList<>();

		private int selected = -1;

		public ChoiceArgument addChoice( final String choice )
		{
			if ( !choices.contains( choice ) )
				choices.add( choice );
			return this;
		}

		public void set( final String choice )
		{
			final int sel = choices.indexOf( choice );
			if ( sel < 0 )
				throw new IllegalArgumentException( "Unknown selection '" + choice + "' for parameter '"
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
		public void accept( final ArgumentVisitor visitor )
		{
			visitor.visit( this );
		}
	}

	public static abstract class ValueArgument< T extends ValueArgument< T > > extends Argument< T >
	{

		/**
		 * Arguments flagged as not required, but without default value, will be
		 * prompted to the user.
		 */
		protected boolean required = false;

		@SuppressWarnings( "unchecked" )
		public T required( final boolean required )
		{
			this.required = required;
			return ( T ) this;
		}
	}

	@SuppressWarnings( "unchecked" )
	public static abstract class Argument< T extends Argument< T > >
	{

		protected String name;

		protected String help;

		protected String argument;

		public T argument( final String argument )
		{
			this.argument = argument;
			return ( T ) this;
		}

		public T name( final String name )
		{
			this.name = name;
			return ( T ) this;
		}

		public T help( final String help )
		{
			this.help = help;
			return ( T ) this;
		}

		public abstract void accept( final ArgumentVisitor visitor );
	}

	public String command()
	{

		// TODO all the parts from the command itself.

		/*
		 * The arguments.
		 */

		final CommandBuilder commandBuilder = new CommandBuilder();
		arguments.forEach( arg -> arg.accept( commandBuilder ) );
		return commandBuilder.toString();
	}
}
