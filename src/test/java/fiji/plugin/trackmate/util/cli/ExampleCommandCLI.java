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

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;

import fiji.plugin.trackmate.util.cli.CliGuiBuilder.CliConfigPanel;

public class ExampleCommandCLI extends CommandCLIConfigurator
{

	private final IntArgument nThreads;

	private final DoubleArgument diameter;

	private final DoubleArgument time;

	public ExampleCommandCLI()
	{
		/*
		 * The executable that will be called by the TrackMate module.
		 * 
		 * CLI configs that inherit from 'CommandCLIConfigurator' are all based
		 * on an actual executable, that is a file with exec rights somewhere on
		 * the user computer. They need to set it themselves, so the config part
		 * only specifies the name, the help and the key of this command.
		 * 
		 * The help and name will be used in the UI.
		 */
		executable
				.name( "Path to the executable." )
				.help( "Browse to the executable location on your computer." )
				.key( "PATH_TO_EXECUTABLE" );

		/*
		 * In this example we will assume that the tool we want to run accepts a
		 * few arguments.
		 * 
		 * The first one is the <code>--nThreads</code> argument, that accept
		 * integer larger than 1 and smaller than 24.
		 * 
		 * The 'argument()' part must be something the tool can understand. This
		 * is what is passed to it before the value.
		 * 
		 * This example argument is not required, but has a default value of 1.
		 * The default value is used only in the command line. If an argument is
		 * not required, is not set, but has a default value, then the argument
		 * will appear in the command line with this default value.
		 * 
		 * Adding arguments is done via 'adder' methods, that are only visible
		 * in inhering classes. The 'get()' method of the adder returns the
		 * created argument. It also adds it to the inner parts of the mother
		 * class, so that it is handled automatically when creating a GUI or a
		 * command line. But it is a good idea to expose it in this concrete
		 * class so that you can expose it to the user and let them set it.
		 */
		this.nThreads = addIntArgument()
				.argument( "--nThreads" ) // arg in the command line
				.name( "N threads" ) // convenient name
				.help( "Sets the number of threads to use for computation." ) // help
				.defaultValue( 1 )
				.min( 1 )
				.max( 24 ) // will be used to create an adequate UI widget
				.key( "N_THREADS" ) // use to serialize to Map<String, Object>
				.get();

		/*
		 * The second argument is a double. It is required, which means that an
		 * error will be thrown when making a command line from this config if
		 * the user forgot to set a value. It also has a unit, which is only
		 * used in the UI.
		 * 
		 * Because it does not specify a min or a max, any numerical value can
		 * be entered in the GUI. The implementation will have to add an extra
		 * check to verify consistency of values.
		 */
		this.diameter = addDoubleArgument()
				.argument( "--diameter" )
				.name( "Diameter" )
				.help( "The diameter of objects to process." )
				.key( "DIAMETER" )
				.defaultValue( 1.5 )
				.units( "microns" )
				.required( true ) // required flag
				.get();

		/*
		 * A double argument that has a min and a max will generate another
		 * widget in the UI: a slider.
		 */
		this.time = addDoubleArgument()
				.argument( "--time" )
				.name( "Time" )
				.help( "Time to wait after processing." )
				.key( "TIME" )
				.min( 1. )
				.max( 100. )
				.units( "seconds" )
				.get();
	}

	public IntArgument nThreads()
	{
		return nThreads;
	}

	public DoubleArgument diameter()
	{
		return diameter;
	}

	public DoubleArgument time()
	{
		return time;
	}

	public static void main( final String[] args )
	{
		final ExampleCommandCLI cli = new ExampleCommandCLI();

		/*
		 * Configure the CLI.
		 */

		cli.getCommandArg().set( "/path/to/my/executable" );

		/*
		 * Play with the command line.
		 */

		/*
		 * Will generate an error because 'diameter' is required and not set.
		 * 
		 * The argument 'nThreads' is not set either, but it has a default value
		 * and is not required -> no error, the command line will use the
		 * default value.
		 * 
		 * The argument 'time' is not set either, does not have a default, but
		 * it is not required -> no error, the command line will just miss the
		 * 'time' argument.
		 */
		try
		{
			final List< String > cmd = CommandBuilder.build( cli );
			System.out.println( "To run: " + cmd ); // error
		}
		catch ( final IllegalArgumentException e )
		{
			System.err.println( e.getMessage() );
		}

		// Set the diameter. Now it should be ok.
		cli.diameter().set( 2.5 );
		try
		{
			final List< String > cmd = CommandBuilder.build( cli );
			System.out.println( "To run: " + cmd );
		}
		catch ( final IllegalArgumentException e )
		{
			System.err.println( e.getMessage() );
		}

		/*
		 * Make a UI that configures the CLI.
		 */

		// The UI cannot be created wit arguments that do not have a value. This
		// will generate an error:
		try
		{
			CliGuiBuilder.build( cli );
		}
		catch ( final IllegalArgumentException e )
		{
			System.err.println( e.getMessage() );
		}

		cli.time().set( 5. );
		cli.nThreads().set( 2 );
		// This should be ok now.
		final CliConfigPanel panel = CliGuiBuilder.build( cli );
		final JFrame frame = new JFrame( "Demo CLI tool" );
		frame.getContentPane().add( panel, BorderLayout.CENTER );
		final JButton btn = new JButton( "echo" );
		btn.addActionListener( e -> System.out.println( CommandBuilder.build( cli ) ) );
		frame.getContentPane().add( btn, BorderLayout.SOUTH );
		frame.setLocationRelativeTo( null );
		frame.pack();
		frame.setVisible( true );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
	}
}
