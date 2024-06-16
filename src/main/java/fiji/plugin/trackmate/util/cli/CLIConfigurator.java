package fiji.plugin.trackmate.util.cli;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class CLIConfigurator
{

	private final List< Argument< ? > > arguments = new ArrayList<>();

	public List< Argument< ? > > getArguments()
	{
		return arguments;
	}

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

		public boolean isSet()
		{
			return set;
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

		public void set( final String value )
		{
			this.value = value;
		}

		public boolean isSet()
		{
			return value != null;
		}

		public boolean hasDefaultValue()
		{
			return defaultValue != null;
		}

		public String getValue()
		{
			return value;
		}

		public String getDefaultValue()
		{
			return defaultValue;
		}

		@Override
		public void accept( final ArgumentVisitor visitor )
		{
			visitor.visit( this );
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

		public boolean isSet()
		{
			return value != Integer.MIN_VALUE;
		}

		public boolean hasDefaultValue()
		{
			return defaultValue != Integer.MIN_VALUE;
		}

		public int getDefaultValue()
		{
			return defaultValue;
		}

		public int getValue()
		{
			return value;
		}

		public int getMin()
		{
			return min;
		}

		public boolean isMinSet()
		{
			return min != Integer.MIN_VALUE;
		}

		public int getMax()
		{
			return max;
		}

		public boolean isMaxSet()
		{
			return max != Integer.MAX_VALUE;
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

		public boolean isSet()
		{
			return !Double.isNaN( value );
		}

		public boolean hasDefaultValue()
		{
			return !Double.isNaN( defaultValue );
		}

		public double getDefaultValue()
		{
			return defaultValue;
		}

		public double getValue()
		{
			return value;
		}

		public double getMax()
		{
			return max;
		}

		public double getMin()
		{
			return min;
		}

		public boolean isMinSet()
		{
			return !Double.isNaN( min );
		}

		public boolean isMaxSet()
		{
			return !Double.isNaN( max );
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

		public boolean isSet()
		{
			return selected > 0;
		}

		public String getValue()
		{
			return choices.get( selected );
		}

		public List< String > getChoices()
		{
			return choices;
		}

		@Override
		public void accept( final ArgumentVisitor visitor )
		{
			visitor.visit( this );
		}
	}

	@SuppressWarnings( "unchecked" )
	public static abstract class ValueArgument< T extends ValueArgument< T > > extends Argument< T >
	{

		/**
		 * Arguments flagged as not required, but without default value, will be
		 * prompted to the user.
		 */
		protected boolean required = false;

		private String units;

		public T required( final boolean required )
		{
			this.required = required;
			return ( T ) this;
		}

		public boolean isRequired()
		{
			return required;
		}

		public T units( final String units )
		{
			this.units = units;
			return ( T ) this;
		}

		public String getUnits()
		{
			return units;
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

		public String getName()
		{
			return name;
		}

		public String getHelp()
		{
			return help;
		}

		public String getArgument()
		{
			return argument;
		}

		public abstract void accept( final ArgumentVisitor visitor );
	}
}
