package fiji.plugin.trackmate.util.cli;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class CLIConfigurator
{

	private final List< Argument< ? > > arguments = new ArrayList<>();

	private final ExecutablePath executable = new ExecutablePath();

	public List< Argument< ? > > getArguments()
	{
		return arguments;
	}

	/**
	 * Exposes the executable path argument.
	 */
	public ExecutablePath getExecutableArg()
	{
		return executable;
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

		public default void visit( final PathArgument pathArgument )
		{
			throw new UnsupportedOperationException();
		}

		public default void visit( final ExecutablePath executablePath )
		{
			throw new UnsupportedOperationException();
		}
	}

	public Flag addFlag()
	{
		final Flag flag = new Flag();
		arguments.add( flag );
		return flag;
	}

	public StringArgument addStringArgument()
	{
		final StringArgument stringArgument = new StringArgument();
		arguments.add( stringArgument );
		return stringArgument;
	}

	public PathArgument addPathArgument()
	{
		final PathArgument pathArgument = new PathArgument();
		arguments.add( pathArgument );
		return pathArgument;
	}

	public IntArgument addIntArgument()
	{
		final IntArgument intArgument = new IntArgument();
		arguments.add( intArgument );
		return intArgument;
	}

	public DoubleArgument addDoubleArgument()
	{
		final DoubleArgument doubleArgument = new DoubleArgument();
		arguments.add( doubleArgument );
		return doubleArgument;
	}

	public ChoiceArgument addChoiceArgument()
	{
		final ChoiceArgument choiceArgument = new ChoiceArgument();
		arguments.add( choiceArgument );
		return choiceArgument;
	}

	public static class Flag extends Argument< Flag >
	{

		private boolean set = false;

		private boolean defaultValue;

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

		public Flag defaultValue( final boolean defaultValue )
		{
			this.defaultValue = defaultValue;
			return this;
		}

		public boolean getDefaultValue()
		{
			return defaultValue;
		}

		@Override
		public Object getValueObject()
		{
			return Boolean.valueOf( set );
		}

		@Override
		public void setValueObject( final Object val )
		{
			if ( !Boolean.class.isInstance( val ) )
				throw new IllegalArgumentException( "Argument '" + name + "' expects boolean. Got " + val.getClass().getSimpleName() );

			final boolean v = ( ( Boolean ) val );
			set( v );
		}

		@Override
		public void accept( final ArgumentVisitor visitor )
		{
			visitor.visit( this );
		}
	}

	/**
	 * Specialization of {@link StringArgument} to be used in a GUI.
	 */
	public static class PathArgument extends AbstractStringArgument< PathArgument >
	{

		@Override
		public void accept( final ArgumentVisitor visitor )
		{
			visitor.visit( this );
		}
	}

	public static class StringArgument extends AbstractStringArgument< StringArgument >
	{
		@Override
		public void accept( final ArgumentVisitor visitor )
		{
			visitor.visit( this );
		}
	}

	public static abstract class AbstractStringArgument< T extends AbstractStringArgument< T > > extends ValueArgument< T >
	{

		/**
		 * If this is not <code>null</code>, the argument will be added to the
		 * command with the default value, even if the user does not explicitly
		 * call the argument. This emulates Python argparse.ArgumentParser.
		 */
		private String defaultValue = null;

		private String value = null;

		@SuppressWarnings( "unchecked" )
		public T defaultValue( final String defaultValue )
		{
			this.defaultValue = defaultValue;
			return ( T ) this;
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
		public Object getValueObject()
		{
			return value;
		}

		@Override
		public void setValueObject( final Object val )
		{
			if ( !String.class.isInstance( val ) )
				throw new IllegalArgumentException( "Argument '" + name + "' expects a String. Got " + val.getClass().getSimpleName() );
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
		public Object getValueObject()
		{
			return Integer.valueOf( value );
		}

		@Override
		public void setValueObject( final Object val )
		{
			if ( !Number.class.isInstance( val ) )
				throw new IllegalArgumentException( "Argument '" + name + "' expects a Number. Got " + val.getClass().getSimpleName() );

			final int v = ( ( Number ) val ).intValue();
			set( v );
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
		public Object getValueObject()
		{
			return Double.valueOf( value );
		}

		@Override
		public void setValueObject( final Object val )
		{
			if ( !Number.class.isInstance( val ) )
				throw new IllegalArgumentException( "Argument '" + name + "' expects a Number. Got " + val.getClass().getSimpleName() );

			final double v = ( ( Number ) val ).doubleValue();
			set( v );
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

		private int defaultChoice = -1;

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

		public ChoiceArgument defaultValue( final String defaultChoice )
		{
			final int sel = choices.indexOf( defaultChoice );
			if ( sel < 0 )
				throw new IllegalArgumentException( "Unknown selection '" + defaultChoice + "' for parameter '"
						+ name + "'. Must be one of " + StringUtils.join( choices, ", " ) + "." );
			this.defaultChoice = sel;
			return this;
		}

		public ChoiceArgument defaultValue( final int selected )
		{
			if ( selected < 0 || selected >= choices.size() )
				throw new IllegalArgumentException( "Invalid index for selection of parameter '"
						+ name + "'. Must be in scale " + 0 + " to " + ( choices.size() - 1 ) + " in "
						+ StringUtils.join( choices, ", " ) + "." );
			this.defaultChoice = selected;
			return this;
		}

		public boolean hasDefaultValue()
		{
			return defaultChoice >= 0;
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
		public Object getValueObject()
		{
			return getValue();
		}

		@Override
		public void setValueObject( final Object val )
		{
			if ( !String.class.isInstance( val ) )
				throw new IllegalArgumentException( "Argument '" + name + "' expects a String. Got " + val.getClass().getSimpleName() );
		}

		@Override
		public void accept( final ArgumentVisitor visitor )
		{
			visitor.visit( this );
		}
	}

	/**
	 * Base class for arguments that accept a value after the switch.
	 *
	 * @param <T>
	 */
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

	/**
	 * Mother class for executable and argument objects.
	 *
	 * @param <T>
	 */
	@SuppressWarnings( "unchecked" )
	public static abstract class Command< T extends Command< T > >
	{

		protected String name;

		protected String help;

		private String key;

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

		/**
		 * For TrackMate settings de/serialization.
		 *
		 * @param key
		 * @return
		 */
		public T key( final String key )
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

		/**
		 * Returns the value of this argument as a Java object that can be
		 * serialized in a TrackMate settings map.
		 *
		 * @return
		 */
		public abstract Object getValueObject();

		/**
		 * Sets the value of this argument from a Java object. It must be of the
		 * right class.
		 *
		 * @param val
		 */
		public abstract void setValueObject( Object val );

		public abstract void accept( final ArgumentVisitor visitor );

		@Override
		public String toString()
		{
			return this.getClass().getSimpleName() + "\n"
					+ " - name: " + getName() + "\n"
					+ " - help: " + getHelp() + "\n"
					+ " - key: " + getKey() + "\n";
		}
	}

	public static class ExecutablePath extends Command< ExecutablePath >
	{

		private String value = null;

		public void set( final String value )
		{
			this.value = value;
		}

		public boolean isSet()
		{
			return value != null;
		}

		public String getValue()
		{
			return value;
		}

		@Override
		public Object getValueObject()
		{
			return value;
		}

		@Override
		public void setValueObject( final Object val )
		{
			if ( !String.class.isInstance( val ) )
				throw new IllegalArgumentException( "Executable '" + name + "' expects a String. Got " + val.getClass().getSimpleName() );
		}

		@Override
		public void accept( final ArgumentVisitor visitor )
		{
			visitor.visit( this );
		}

		@Override
		public String toString()
		{
			final String str = super.toString();
			return str + " - value: " + getValue() + "\n";
		}
	}

	@SuppressWarnings( "unchecked" )
	public static abstract class Argument< T extends Argument< T > > extends Command< T >
	{

		protected String argument;

		private boolean visible = true;

		public T argument( final String argument )
		{
			this.argument = argument;
			return ( T ) this;
		}

		public String getArgument()
		{
			return argument;
		}

		/**
		 * If <code>false</code>, this argument won't be shown in UIs. It will
		 * be used for the command line builder nonetheless.
		 *
		 * @param visible
		 *            whether this argument should be visible in the UI or not.
		 * @return the argument.
		 */
		public T visible( final boolean visible )
		{
			this.visible = visible;
			return ( T ) this;
		}

		public boolean isVisible()
		{
			return visible;
		}
	}
}
