package fiji.plugin.trackmate;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import ij.IJ;

/**
 * This class is used to log messages occurring during TrackMate execution.
 */
public abstract class Logger extends PrintWriter
{

	public Logger()
	{
		// Call super with a dummy writer
		super( new Writer()
		{
			@Override
			public void write( final char[] cbuf, final int off, final int len ) throws IOException
			{}

			@Override
			public void flush() throws IOException
			{}

			@Override
			public void close() throws IOException
			{}
		} );
		// Replace by a useful writer
		this.out = new Writer()
		{
			@Override
			public void close() throws IOException
			{}

			@Override
			public void flush() throws IOException
			{}

			@Override
			public void write( final char[] cbuf, final int off, final int len ) throws IOException
			{
				String str = "";
				for ( int i = off; i < len; i++ )
					str += cbuf[ i ];
				log( str );
			}
		};
	}

	public static final Color NORMAL_COLOR = Color.BLACK;

	public static final Color ERROR_COLOR = new Color( 0.8f, 0, 0 );

	public static final Color GREEN_COLOR = new Color( 0, 0.6f, 0 );

	public static final Color BLUE_COLOR = new Color( 0, 0, 0.7f );

	/**
	 * Append the message to the logger, with the specified color.
	 *
	 * @param message
	 *            the message to append.
	 * @param color
	 *            the color to use.
	 */
	public abstract void log( String message, Color color );

	/**
	 * Send the message to the error channel of this logger.
	 *
	 * @param message
	 *            the message to send.
	 */
	public abstract void error( String message );

	/**
	 * Append the message to the logger with default black color.
	 *
	 * @param message
	 *            the message to append.
	 */
	public void log( final String message )
	{
		log( message, NORMAL_COLOR );
	}

	/**
	 * Set the progress value of the process logged by this logger. Values
	 * should be between 0 and 1, 1 meaning the process if finished.
	 *
	 * @param val
	 *            the progress value (double from 0 to 1).
	 */
	public abstract void setProgress( double val );

	/**
	 * Set the status to be displayed by this logger.
	 *
	 * @param status
	 *            the status to display.
	 */
	public abstract void setStatus( String status );

	/**
	 * This logger discard any message.
	 */
	public static final Logger VOID_LOGGER = new Logger()
	{

		@Override
		public void setStatus( final String status )
		{}

		@Override
		public void setProgress( final double val )
		{}

		@Override
		public void log( final String message, final Color color )
		{}

		@Override
		public void error( final String message )
		{}
	};

	/**
	 * This {@link Logger} simply outputs to the standard output and standard
	 * error. The {@link #setProgress(double)} method is ignored, the
	 * {@link #setStatus(String)} is sent to the console.
	 */
	public static Logger DEFAULT_LOGGER = new Logger()
	{

		@Override
		public void log( final String message, final Color color )
		{
			System.out.print( message );
		}

		@Override
		public void error( final String message )
		{
			System.err.print( message );
		}

		@Override
		public void setProgress( final double val )
		{}

		@Override
		public void setStatus( final String status )
		{
			System.out.println( status );
		}
	};

	/**
	 * This {@link Logger} outputs to the ImageJ log window, and to the ImageJ
	 * toolbar to report progress. Colors are ignored.
	 */
	public static Logger IJ_LOGGER = new Logger()
	{

		@Override
		public void log( final String message, final Color color )
		{
			IJ.log( message );
		}

		@Override
		public void error( final String message )
		{
			IJ.log( message );
		}

		@Override
		public void setProgress( final double val )
		{
			IJ.showProgress( val );
		}

		@Override
		public void setStatus( final String status )
		{
			IJ.showStatus( status );
		}
	};

	/**
	 * This {@link Logger} outputs everything to the ImageJ toolbar. This is not
	 * optimal for long messages. Colors are ignored.
	 */
	public static Logger IJTOOLBAR_LOGGER = new Logger()
	{
		@Override
		public void log( final String message, final Color color )
		{
			IJ.showStatus( message );
		}

		@Override
		public void error( final String message )
		{
			IJ.showStatus( message );
		}

		@Override
		public void setProgress( final double val )
		{
			IJ.showProgress( val );
		}

		@Override
		public void setStatus( final String status )
		{
			IJ.showStatus( status );
		}
	};

	/**
	 * This {@link Logger} outputs to a StringBuilder given at construction.
	 * Report progress and colors are ignored.
	 */
	public static class StringBuilderLogger extends Logger
	{

		private final StringBuilder sb;

		public StringBuilderLogger( final StringBuilder sb )
		{
			this.sb = sb;
		}

		public StringBuilderLogger()
		{
			this( new StringBuilder() );
		}

		/*
		 * METHODS
		 */

		@Override
		public void log( final String message, final Color color )
		{
			sb.append( message );
		}

		@Override
		public void error( final String message )
		{
			sb.append( message );
		}

		@Override
		public void setProgress( final double val )
		{}

		@Override
		public void setStatus( final String status )
		{
			sb.append( status );
		}

		@Override
		public String toString()
		{
			return sb.toString();
		}
	}

	/**
	 * A logger that wraps a master logger and uses it to echo any received
	 * message. This class is used to report progress of a sub-process. If it is
	 * sent to a subprocess, the master logger can show a progress in a range
	 * and from a starting point that can be specified.
	 *
	 * @author Jean-Yves Tinevez - 2014
	 *
	 */
	public static class SlaveLogger extends Logger
	{

		private final Logger master;

		private final double progressStart;

		private final double progressRange;

		/**
		 * Create a new slave logger that will report progress to its master in
		 * the following way: If a sub-process reports a progress of
		 * <code>val</code>, then the master logger will receive the progress
		 * value <code>progressStart + progressRange * val</code>.
		 *
		 * @param master
		 *            the master {@link Logger}.
		 * @param progressStart
		 *            the progress to start with.
		 * @param progressRange
		 *            the progress range to report.
		 */
		public SlaveLogger( final Logger master, final double progressStart, final double progressRange )
		{
			this.master = master;
			this.progressStart = progressStart;
			this.progressRange = progressRange;
		}

		@Override
		public void log( final String message, final Color color )
		{
			master.log( message, color );
		}

		@Override
		public void error( final String message )
		{
			master.error( message );
		}

		@Override
		public void setProgress( final double val )
		{
			master.setProgress( progressStart + progressRange * val );
		}

		@Override
		public void setStatus( final String status )
		{
			master.setStatus( status );
		}

	}

}
