package fiji.plugin.trackmate.util.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

public abstract class CLIConfigurator
{

	protected final List< Argument< ? > > arguments = new ArrayList<>();

	protected final ExecutablePath executable = new ExecutablePath();

	protected final List< SelectableArguments > selectables = new ArrayList<>();

	protected final Map< Command< ? >, Function< Object, List< String > > > translators = new HashMap<>();

	/*
	 * GETTERS
	 */

	/**
	 * Returns the list of arguments in this CLI config. All arguments are
	 * present, regardless of whether they are in {@link SelectableArguments}.
	 * 
	 * @return the list of arguments.
	 */
	public List< Argument< ? > > getArguments()
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
	public List< Argument< ? > > getSelectedArguments()
	{
		final List< Argument< ? > > selectedArguments = new ArrayList<>( arguments );
		for ( final SelectableArguments selectable : selectables )
			selectable.filter( selectedArguments );
		return selectedArguments;
	}

	/*
	 * EXECUTABLE.
	 */

	/**
	 * Exposes the executable path argument.
	 */
	public ExecutablePath getExecutableArg()
	{
		return executable;
	}

	/*
	 * VALUE TRANSLATOR.
	 */

	protected void setTranslator( final Command< ? > arg, final Function< Object, List< String > > translator )
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

		private final List< Argument< ? > > args = new ArrayList<>();

		private int selected = 0;

		public SelectableArguments add( final Argument< ? > arg )
		{
			if ( !args.contains( arg ) )
				args.add( arg );
			return this;
		}

		private void filter( final List< Argument< ? > > arguments )
		{
			final Set< Argument< ? > > toRemove = new HashSet<>();
			for ( final Argument< ? > arg : arguments )
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

		public void select( final Argument< ? > arg )
		{
			final int sel = args.indexOf( arg );
			if ( sel < 0 )
				throw new IllegalArgumentException( "Unknown argument '" + arg.getName() + "' for this selectable." );
			this.selected = sel;
		}

		public Argument< ? > getSelection()
		{
			return args.get( selected );
		}

		/**
		 * Exposes all members of the selectable.
		 */
		public List< Argument< ? > > getArguments()
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
	}

	/*
	 * ADDER CLASSES.
	 */

	@SuppressWarnings( "unchecked" )
	private abstract class Adder< A extends ValueArgument< A, O >, T extends Adder< A, T, O >, O >
	{

		protected String name;

		protected String help;

		protected String key;

		protected boolean required;

		protected String units;

		protected O defaultValue;

		protected String argument;

		protected boolean visible = true; // by default

		public T argument( final String argument )
		{
			this.argument = argument;
			return ( T ) this;
		}

		public T visible( final boolean visible )
		{
			this.visible = visible;
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

		public T key( final String key )
		{
			this.key = key;
			return ( T ) this;
		}

		public T required( final boolean required )
		{
			this.required = required;
			return ( T ) this;
		}

		public T units( final String units )
		{
			this.units = units;
			return ( T ) this;
		}

		public T defaultValue( final O defaultValue )
		{
			this.defaultValue = defaultValue;
			return ( T ) this;
		}

		public abstract A get();
	}

	@SuppressWarnings( "unchecked" )
	private abstract class BoundedAdder< A extends BoundedValueArgument< A, O >, T extends BoundedAdder< A, T, O >, O > extends Adder< A, T, O >
	{
		protected O min;

		protected O max;

		public T min( final O min )
		{
			this.min = min;
			return ( T ) this;
		}

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

		public ChoiceAdder addChoice( final String choice )
		{
			if ( !choices.contains( choice ) )
				choices.add( choice );
			return this;
		}

		@Override
		public ChoiceAdder defaultValue( final String defaultChoice )
		{
			final int sel = choices.indexOf( defaultChoice );
			if ( sel < 0 )
				throw new IllegalArgumentException( "Unknown selection '" + defaultChoice + "' for parameter '"
						+ name + "'. Must be one of " + StringUtils.join( choices, ", " ) + "." );
			return super.defaultValue( defaultChoice );
		}

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
					.key( key );
			for ( final String choice : choices )
				arg.addChoice( choice );
			arg.defaultValue( defaultValue );
			CLIConfigurator.this.arguments.add( arg );
			return arg;
		}
	}

	/*
	 * ADDER METHODS.
	 */

	protected FlagAdder addFlag()
	{
		return new FlagAdder();
	}

	protected StringAdder addStringArgument()
	{
		return new StringAdder();
	}

	protected PathAdder addPathArgument()
	{
		return new PathAdder();
	}

	protected IntAdder addIntArgument()
	{
		return new IntAdder();
	}

	protected DoubleAdder addDoubleArgument()
	{
		return new DoubleAdder();
	}

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
	protected < T extends Argument< ? > > T addExtraArgument( final T extraArg )
	{
		this.arguments.add( extraArg );
		return extraArg;
	}

	/*
	 * ARGUMENT CLASSES.
	 */

	public static class Flag extends ValueArgument< Flag, Boolean >
	{
		Flag()
		{}

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

	public static abstract class AbstractStringArgument< T extends AbstractStringArgument< T > > extends ValueArgument< T, String >
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

	public static class ChoiceArgument extends AbstractStringArgument< ChoiceArgument >
	{

		private final List< String > choices = new ArrayList<>();

		private ChoiceArgument()
		{}

		ChoiceArgument addChoice( final String choice )
		{
			if ( !choices.contains( choice ) )
				choices.add( choice );
			return this;
		}

		@Override
		public void set( final String choice )
		{
			final int sel = choices.indexOf( choice );
			if ( sel < 0 )
				throw new IllegalArgumentException( "Unknown selection '" + choice + "' for parameter '"
						+ name + "'. Must be one of " + StringUtils.join( choices, ", " ) + "." );
			super.set( choice );
		}

		public void set( final int selected )
		{
			if ( selected < 0 || selected >= choices.size() )
				throw new IllegalArgumentException( "Invalid index for selection of parameter '"
						+ name + "'. Must be in scale " + 0 + " to " + ( choices.size() - 1 ) + " in "
						+ StringUtils.join( choices, ", " ) + "." );
			super.set( choices.get( selected ) );
		}

		public int getSelectedIndex()
		{
			return choices.indexOf( getValue() );
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

		public List< String > getChoices()
		{
			return choices;
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
			return str
					+ " - choices: " + getChoices() + "\n";
		}
	}

	/**
	 * Base class for arguments that accept a value after the switch.
	 *
	 * @param <T>
	 */
	@SuppressWarnings( "unchecked" )
	public static abstract class ValueArgument< T extends ValueArgument< T, O >, O > extends Argument< T >
	{

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

		@Override
		public Object getValueObject()
		{
			return getValue();
		}

		@Override
		public String toString()
		{
			final String str = super.toString();
			return str
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

	@SuppressWarnings( "unchecked" )
	public static abstract class BoundedValueArgument< T extends BoundedValueArgument< T, O >, O > extends ValueArgument< T, O >
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
			return ( key == null ) ? getName() : key;
		}

		public abstract void accept( final ArgumentVisitor visitor );

		@Override
		public String toString()
		{
			return this.getClass().getSimpleName()
					+ " (" + getName() + ")\n"
					+ " - help: " + getHelp() + "\n"
					+ " - key: " + getKey() + "\n";
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
		 * Returns an object built from the value of this argument, if it has
		 * one. This is used in TrackMate settings map serialization.
		 * 
		 * @return the value object.
		 * @see TrackMateSettingsBuilder
		 */
		public abstract Object getValueObject();
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

		@Override
		public String toString()
		{
			final String str = super.toString();
			return str + " - value: " + getValue() + "\n";
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
		public Object getValueObject()
		{
			return getValue();
		}
	}

	/**
	 * Mother class for command arguments. Typically in the command line they
	 * appear after the executable name with '--something'.
	 *
	 * @param <T>
	 */
	@SuppressWarnings( "unchecked" )
	public static abstract class Argument< T extends Argument< T > > extends Command< T >
	{

		private String argument;

		private boolean visible = true;

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

		@Override
		public String toString()
		{
			final String str = super.toString();
			return str
					+ " - argument: " + getArgument() + "\n"
					+ " - visible: " + isVisible() + "\n";
		}
	}

	@Override
	public String toString()
	{
		final StringBuilder str = new StringBuilder();
		str.append( super.toString() + "\n" );
		str.append( executable.toString() );
		arguments.forEach( str::append );
		return str.toString();

	}

	public String check()
	{
		if ( !executable.isSet() )
		{
			return "Executable path is not set.\n" ;
		}
		else
		{
			final String path = executable.getValue();
			final File file = new File(path);
			if ( !file.exists() )
				return "Executable path " + path + " does not exist.\n";
			if ( !file.canExecute() )
				return "Executable " + path + " cannot be run.\n";
		}

		final StringBuilder str = new StringBuilder();
		for ( final Argument< ? > arg : getSelectedArguments() )
		{
			if ( arg.isInCLI() && arg.getArgument() == null )
				str.append( "Argument '" + arg.getName() + "' does not define the argument switch.\n" );

			if ( ValueArgument.class.isInstance( arg ) )
			{
				final ValueArgument< ?, ? > varg = ( ValueArgument< ?, ? > ) arg;
				if ( varg.isRequired() && !varg.isSet() && !varg.hasDefaultValue() )
						str.append( "Argument '" + arg.getName() + "' is required but is not set and does not define a default value.\n" );
			}
		}
		return str.length() == 0 ? null : str.toString();
	}
}
